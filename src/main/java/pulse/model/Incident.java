package pulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Abstract base class for all campus infrastructure incidents.
 * Demonstrates Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class Incident {
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int incidentId;
    private String title;
    private String description;
    private String location;
    private Priority priority;
    private IncidentStatus status;
    private String reportedBy;
    private Integer assignedTechnicianId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    /**
     * Constructor for newly reported incidents.
     * Automatically sets initial status to OPEN and initializes timestamps.
     */
    protected Incident(String title, String description, String location,
                       Priority priority, String reportedBy) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Incident title cannot be empty.");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Incident location cannot be empty.");
        }
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.location = location.trim();
        this.priority = Objects.requireNonNull(priority, "Priority is required.");
        this.reportedBy = (reportedBy == null || reportedBy.isBlank()) ? "Anonymous" : reportedBy.trim();
        this.status = IncidentStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Full constructor used when reconstructing an incident from the database.
     */
    protected Incident(int incidentId, String title, String description, String location,
                       Priority priority, IncidentStatus status, String reportedBy,
                       Integer assignedTechnicianId, LocalDateTime createdAt,
                       LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        this.incidentId = incidentId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.priority = priority;
        this.status = status;
        this.reportedBy = reportedBy;
        this.assignedTechnicianId = assignedTechnicianId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    /**
     * Returns the canonical category code (e.g., LAB, ELECTRICAL, PROJECTOR, NETWORK).
     */
    public abstract String getIncidentType();

    /**
     * Returns the technician specialization required to handle this incident.
     */
    public abstract String getRequiredSpecialization();

    /**
     * Returns estimated resolution time in minutes based on incident category.
     */
    public abstract int getEstimatedResolutionMinutes();

    /**
     * Returns subclass-specific details formatted for display.
     */
    public abstract String getSpecificDetails();

    /**
     * Serializes subclass-specific fields for database persistence.
     */
    public abstract String serializeSpecificData();

    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident ID      : #").append(incidentId).append('\n');
        sb.append("Type             : ").append(getIncidentType()).append('\n');
        sb.append("Title            : ").append(title).append('\n');
        sb.append("Description      : ").append(description).append('\n');
        sb.append("Location         : ").append(location).append('\n');
        sb.append("Priority         : ").append(priority).append('\n');
        sb.append("Status           : ").append(status).append('\n');
        sb.append("Reported By      : ").append(reportedBy).append('\n');
        sb.append("Assigned Tech ID : ").append(assignedTechnicianId != null ? "#" + assignedTechnicianId : "Unassigned").append('\n');
        sb.append("Est. Resolution  : ").append(getEstimatedResolutionMinutes()).append(" mins\n");
        sb.append("Specific Details : ").append(getSpecificDetails()).append('\n');
        sb.append("Created At       : ").append(formatTime(createdAt)).append('\n');
        sb.append("Updated At       : ").append(formatTime(updatedAt)).append('\n');
        sb.append("Resolved At      : ").append(resolvedAt != null ? formatTime(resolvedAt) : "Not yet resolved");
        return sb.toString();
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_FORMATTER) : "N/A";
    }

    public int getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(int incidentId) {
        this.incidentId = incidentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "Priority cannot be null.");
        this.updatedAt = LocalDateTime.now();
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null.");
        this.updatedAt = LocalDateTime.now();
        if (status == IncidentStatus.RESOLVED && this.resolvedAt == null) {
            this.resolvedAt = this.updatedAt;
        }
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Integer getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Integer assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public String toString() {
        return String.format("[#%d] %-10s | %-8s | %-11s | %s (%s)",
                incidentId, getIncidentType(), priority, status, title, location);
    }
}
