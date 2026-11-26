package pulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents an audit log entry tracking incident status transitions.
 */
public class IncidentHistory {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int historyId;
    private int incidentId;
    private IncidentStatus previousStatus;
    private IncidentStatus newStatus;
    private Integer technicianId;
    private String remarks;
    private LocalDateTime changedAt;

    public IncidentHistory(int incidentId, IncidentStatus previousStatus,
                           IncidentStatus newStatus, Integer technicianId, String remarks) {
        this.incidentId = incidentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.technicianId = technicianId;
        this.remarks = remarks;
        this.changedAt = LocalDateTime.now();
    }

    public IncidentHistory(int historyId, int incidentId, IncidentStatus previousStatus,
                           IncidentStatus newStatus, Integer technicianId,
                           String remarks, LocalDateTime changedAt) {
        this.historyId = historyId;
        this.incidentId = incidentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.technicianId = technicianId;
        this.remarks = remarks;
        this.changedAt = changedAt;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getIncidentId() {
        return incidentId;
    }

    public IncidentStatus getPreviousStatus() {
        return previousStatus;
    }

    public IncidentStatus getNewStatus() {
        return newStatus;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    @Override
    public String toString() {
        String prev = previousStatus != null ? previousStatus.name() : "NONE";
        String tech = technicianId != null ? "Tech #" + technicianId : "System";
        String time = changedAt != null ? changedAt.format(FORMATTER) : "N/A";
        return String.format("[%s] Incident #%d: %-11s -> %-11s | %-10s | %s",
                time, incidentId, prev, newStatus, tech, remarks);
    }
}
