package pulse.model;

/**
 * Represents the lifecycle states of a campus incident:
 * OPEN -> IN_PROGRESS -> RESOLVED.
 */
public enum IncidentStatus {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static IncidentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IncidentStatus cannot be null or blank.");
        }
        return IncidentStatus.valueOf(value.trim().toUpperCase());
    }
}
