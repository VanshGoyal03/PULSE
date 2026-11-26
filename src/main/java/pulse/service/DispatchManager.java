package pulse.service;

import pulse.model.Incident;
import pulse.model.IncidentStatus;
import pulse.model.Technician;
import pulse.threads.TechnicianWorker;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Coordinates priority-based incident dispatch using a PriorityQueue,
 * a LinkedList waiting queue, and synchronized inter-thread communication.
 */
public class DispatchManager {

    /**
     * Custom Comparator:
     * 1. Higher Priority first (CRITICAL=1 < HIGH=2 < MEDIUM=3 < LOW=4)
     * 2. Older creation timestamp first (FIFO for equal priorities)
     * 3. Lower incident ID as deterministic tie-breaker
     */
    public static final Comparator<Incident> INCIDENT_COMPARATOR = (a, b) -> {
        int priorityCompare = Integer.compare(a.getPriority().getRank(), b.getPriority().getRank());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        int timeCompare = a.getCreatedAt().compareTo(b.getCreatedAt());
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Integer.compare(a.getIncidentId(), b.getIncidentId());
    };

    private final IncidentManager incidentManager;
    private final TechnicianManager technicianManager;
    private final PriorityQueue<Incident> dispatchQueue = new PriorityQueue<>(INCIDENT_COMPARATOR);
    private final LinkedList<Incident> waitingQueue = new LinkedList<>();
    private final Map<Integer, TechnicianWorker> workerThreads = new HashMap<>();

    public DispatchManager(IncidentManager incidentManager, TechnicianManager technicianManager) {
        this.incidentManager = incidentManager;
        this.technicianManager = technicianManager;
    }

    public synchronized void initializeWorkersAndQueues() {
        dispatchQueue.clear();
        waitingQueue.clear();

        for (Incident incident : incidentManager.getAllIncidents()) {
            if (incident.getStatus() == IncidentStatus.OPEN) {
                dispatchQueue.offer(incident);
            }
        }

        for (Technician tech : technicianManager.getAllTechnicians()) {
            startWorkerForTechnician(tech);
        }
    }

    public synchronized void startWorkerForTechnician(Technician tech) {
        if (!workerThreads.containsKey(tech.getTechnicianId())) {
            TechnicianWorker worker = new TechnicianWorker(tech, this);
            workerThreads.put(tech.getTechnicianId(), worker);
            worker.start();
        }
    }

    public synchronized void enqueueIncident(Incident incident) {
        if (incident.getStatus() == IncidentStatus.OPEN) {
            dispatchQueue.offer(incident);
            notifyAll();
        }
    }

    /**
     * Dispatches the highest-priority OPEN incident that has an available matching technician.
     * Incidents without an available technician are held in the LinkedList waitingQueue.
     */
    public synchronized Optional<Incident> dispatchNextIncident() throws SQLException {
        requeueWaitingIncidents();

        List<Incident> deferred = new LinkedList<>();
        Incident dispatched = null;

        while (!dispatchQueue.isEmpty()) {
            Incident candidate = dispatchQueue.poll();
            if (candidate.getStatus() != IncidentStatus.OPEN || candidate.getAssignedTechnicianId() != null) {
                continue;
            }

            Optional<Technician> techOpt = technicianManager
                    .findAvailableBySpecialization(candidate.getRequiredSpecialization());

            if (techOpt.isPresent()) {
                Technician tech = techOpt.get();
                assignIncidentToTechnician(candidate, tech);
                dispatched = candidate;
                break;
            } else {
                deferred.add(candidate);
            }
        }

        waitingQueue.addAll(deferred);
        notifyAll();
        return Optional.ofNullable(dispatched);
    }

    private void assignIncidentToTechnician(Incident incident, Technician technician) throws SQLException {
        IncidentStatus prevStatus = incident.getStatus();
        technician.assignIncident(incident.getIncidentId());
        incident.setAssignedTechnicianId(technician.getTechnicianId());

        technicianManager.updateTechnician(technician);
        incidentManager.recordStatusChange(
                incident,
                prevStatus,
                IncidentStatus.IN_PROGRESS,
                technician.getTechnicianId(),
                "Dispatched to " + technician.getName() + " (" + technician.getSpecialization() + ")"
        );

        TechnicianWorker worker = workerThreads.get(technician.getTechnicianId());
        if (worker != null) {
            worker.assignWork(incident);
        }
    }

    /**
     * Marks an incident as RESOLVED manually or by a worker thread,
     * releases the assigned technician, and notifies waiting dispatchers.
     */
    public synchronized boolean resolveIncident(int incidentId, String remarks) throws SQLException {
        Optional<Incident> incidentOpt = incidentManager.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            return false;
        }

        Incident incident = incidentOpt.get();
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            return false;
        }

        IncidentStatus prevStatus = incident.getStatus();
        Integer techId = incident.getAssignedTechnicianId();

        if (techId != null) {
            TechnicianWorker worker = workerThreads.get(techId);
            if (worker != null) {
                worker.cancelCurrentWorkIfMatching(incidentId);
            }

            Optional<Technician> techOpt = technicianManager.findById(techId);
            if (techOpt.isPresent()) {
                Technician tech = techOpt.get();
                tech.releaseIncident();
                technicianManager.updateTechnician(tech);
            }
        }

        waitingQueue.removeIf(i -> i.getIncidentId() == incidentId);
        dispatchQueue.removeIf(i -> i.getIncidentId() == incidentId);

        incidentManager.recordStatusChange(
                incident,
                prevStatus,
                IncidentStatus.RESOLVED,
                techId,
                remarks
        );

        requeueWaitingIncidents();
        notifyAll();
        return true;
    }

    public void completeIncidentByWorker(int incidentId, int technicianId, String remarks) {
        try {
            boolean resolved = resolveIncident(incidentId, remarks);
            if (resolved) {
                System.out.printf("%n[NOTIFY] Tech #%d completed Incident #%d. Technician is now AVAILABLE.%n",
                        technicianId, incidentId);
            }
        } catch (SQLException e) {
            System.err.printf("%n[ERROR] Worker failed to resolve Incident #%d: %s%n",
                    incidentId, e.getMessage());
        }
    }

    private void requeueWaitingIncidents() {
        while (!waitingQueue.isEmpty()) {
            dispatchQueue.offer(waitingQueue.poll());
        }
    }

    public synchronized int getPendingQueueSize() {
        return dispatchQueue.size() + waitingQueue.size();
    }

    public synchronized int getWaitingQueueSize() {
        return waitingQueue.size();
    }

    public synchronized Map<Integer, Thread.State> getWorkerThreadStates() {
        Map<Integer, Thread.State> states = new HashMap<>();
        for (Map.Entry<Integer, TechnicianWorker> entry : workerThreads.entrySet()) {
            states.put(entry.getKey(), entry.getValue().getThreadState());
        }
        return states;
    }

    public synchronized void shutdownAllWorkers() {
        for (TechnicianWorker worker : workerThreads.values()) {
            worker.shutdown();
        }
        notifyAll();
    }
}
