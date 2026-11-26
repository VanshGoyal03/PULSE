package pulse.threads;

import pulse.model.Incident;
import pulse.model.Technician;
import pulse.service.DispatchManager;

/**
 * Worker thread representing a campus technician.
 * Demonstrates Java thread lifecycle states (NEW, RUNNABLE, WAITING, TIMED_WAITING, TERMINATED),
 * synchronized coordination, wait()/notifyAll(), and proper interruption handling.
 */
public class TechnicianWorker implements Runnable {
    private static final long REPAIR_DURATION_MS = 12000L;

    private final Technician technician;
    private final DispatchManager dispatchManager;
    private volatile boolean running = true;
    private Incident assignedIncident;
    private Thread workerThread;

    public TechnicianWorker(Technician technician, DispatchManager dispatchManager) {
        this.technician = technician;
        this.dispatchManager = dispatchManager;
    }

    public synchronized void start() {
        if (workerThread == null) {
            workerThread = new Thread(this, "TechWorker-" + technician.getTechnicianId());
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    public synchronized void assignWork(Incident incident) {
        this.assignedIncident = incident;
        notifyAll();
    }

    public synchronized void cancelCurrentWorkIfMatching(int incidentId) {
        if (assignedIncident != null && assignedIncident.getIncidentId() == incidentId) {
            assignedIncident = null;
            if (workerThread != null) {
                workerThread.interrupt();
            }
        }
    }

    public synchronized void shutdown() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        notifyAll();
    }

    public Thread.State getThreadState() {
        return workerThread != null ? workerThread.getState() : Thread.State.NEW;
    }

    public String getThreadName() {
        return workerThread != null ? workerThread.getName() : "TechWorker-" + technician.getTechnicianId();
    }

    public Technician getTechnician() {
        return technician;
    }

    @Override
    public void run() {
        while (running) {
            Incident taskToProcess;
            synchronized (this) {
                while (running && assignedIncident == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        if (!running) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        // Clear interrupt status if woken up for a new task or manual resolution
                    }
                }
                if (!running) {
                    return;
                }
                taskToProcess = assignedIncident;
            }

            try {
                // Simulate on-site technician work (TIMED_WAITING state)
                Thread.sleep(REPAIR_DURATION_MS);

                boolean stillAssigned;
                synchronized (this) {
                    stillAssigned = running
                            && assignedIncident != null
                            && assignedIncident.getIncidentId() == taskToProcess.getIncidentId();
                    if (stillAssigned) {
                        assignedIncident = null;
                    }
                }

                if (stillAssigned) {
                    dispatchManager.completeIncidentByWorker(
                            taskToProcess.getIncidentId(),
                            technician.getTechnicianId(),
                            "Automatically resolved by technician worker thread."
                    );
                }
            } catch (InterruptedException e) {
                // Either manually resolved or shutting down
                synchronized (this) {
                    if (!running) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
