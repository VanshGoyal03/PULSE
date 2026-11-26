package pulse.service;

import pulse.model.Incident;
import pulse.model.IncidentHistory;
import pulse.model.IncidentStatus;
import pulse.model.Priority;
import pulse.repository.HistoryRepository;
import pulse.repository.IncidentRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Manages incident records (ArrayList) and status audit history (LinkedList).
 */
public class IncidentManager {
    private final IncidentRepository incidentRepository;
    private final HistoryRepository historyRepository;
    private final List<Incident> incidents = new ArrayList<>();
    private final LinkedList<IncidentHistory> historyCache = new LinkedList<>();

    public IncidentManager(IncidentRepository incidentRepository, HistoryRepository historyRepository) {
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
    }

    public synchronized void loadFromDatabase() throws SQLException {
        incidents.clear();
        incidents.addAll(incidentRepository.findAll());

        historyCache.clear();
        historyCache.addAll(historyRepository.findAll());
    }

    public synchronized Incident reportIncident(Incident incident) throws SQLException {
        incidentRepository.save(incident);
        incidents.add(incident);

        IncidentHistory initialHistory = new IncidentHistory(
                incident.getIncidentId(),
                null,
                IncidentStatus.OPEN,
                null,
                "Incident reported at " + incident.getLocation()
        );
        historyRepository.save(initialHistory);
        historyCache.addFirst(initialHistory);
        return incident;
    }

    public synchronized void recordStatusChange(Incident incident, IncidentStatus previousStatus,
                                                IncidentStatus newStatus, Integer technicianId,
                                                String remarks) throws SQLException {
        incident.setStatus(newStatus);
        incidentRepository.updateStatusAndTechnician(incident);

        IncidentHistory entry = new IncidentHistory(
                incident.getIncidentId(),
                previousStatus,
                newStatus,
                technicianId,
                remarks
        );
        historyRepository.save(entry);
        historyCache.addFirst(entry);
    }

    public synchronized List<Incident> getAllIncidents() {
        return new ArrayList<>(incidents);
    }

    public synchronized List<Incident> getIncidentsByPriority(Priority priority) {
        List<Incident> filtered = new ArrayList<>();
        for (Incident incident : incidents) {
            if (incident.getPriority() == priority) {
                filtered.add(incident);
            }
        }
        return filtered;
    }

    public synchronized List<Incident> getIncidentsByStatus(IncidentStatus status) {
        List<Incident> filtered = new ArrayList<>();
        for (Incident incident : incidents) {
            if (incident.getStatus() == status) {
                filtered.add(incident);
            }
        }
        return filtered;
    }

    public synchronized Optional<Incident> findById(int incidentId) {
        for (Incident incident : incidents) {
            if (incident.getIncidentId() == incidentId) {
                return Optional.of(incident);
            }
        }
        return Optional.empty();
    }

    public synchronized List<IncidentHistory> getAllHistory() throws SQLException {
        historyCache.clear();
        historyCache.addAll(historyRepository.findAll());
        return new LinkedList<>(historyCache);
    }

    public synchronized List<IncidentHistory> getHistoryForIncident(int incidentId) throws SQLException {
        return historyRepository.findByIncidentId(incidentId);
    }
}
