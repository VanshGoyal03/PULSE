package pulse.model;

/**
 * Defines incident priority levels in strict dispatch order:
 * CRITICAL (1) > HIGH (2) > MEDIUM (3) > LOW (4).
 */
public enum Priority {
    CRITICAL(1, "Critical"),
    HIGH(2, "High"),
    MEDIUM(3, "Medium"),
    LOW(4, "Low");

    private final int rank;
    private final String displayName;

    Priority(int rank, String displayName) {
        this.rank = rank;
        this.displayName = displayName;
    }

    public int getRank() {
        return rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Priority cannot be null or blank.");
        }
        return Priority.valueOf(value.trim().toUpperCase());
    }
}
