package pulse.model;

/**
 * Represents a campus maintenance technician with a specific specialization.
 */
public class Technician {
    private int technicianId;
    private String name;
    private String specialization;
    private boolean available;
    private Integer currentIncidentId;
    private int resolvedCount;

    public Technician(String name, String specialization) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Technician name cannot be empty.");
        }
        if (specialization == null || specialization.isBlank()) {
            throw new IllegalArgumentException("Technician specialization cannot be empty.");
        }
        this.name = name.trim();
        this.specialization = specialization.trim().toUpperCase();
        this.available = true;
        this.currentIncidentId = null;
        this.resolvedCount = 0;
    }

    public Technician(int technicianId, String name, String specialization,
                      boolean available, Integer currentIncidentId, int resolvedCount) {
        this.technicianId = technicianId;
        this.name = name;
        this.specialization = specialization.toUpperCase();
        this.available = available;
        this.currentIncidentId = currentIncidentId;
        this.resolvedCount = resolvedCount;
    }

    public synchronized void assignIncident(int incidentId) {
        if (!available) {
            throw new IllegalStateException("Technician " + name + " is already busy.");
        }
        this.available = false;
        this.currentIncidentId = incidentId;
    }

    public synchronized void releaseIncident() {
        this.available = true;
        this.currentIncidentId = null;
        this.resolvedCount++;
    }

    public int getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(int technicianId) {
        this.technicianId = technicianId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization.toUpperCase();
    }

    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    public synchronized Integer getCurrentIncidentId() {
        return currentIncidentId;
    }

    public synchronized void setCurrentIncidentId(Integer currentIncidentId) {
        this.currentIncidentId = currentIncidentId;
    }

    public synchronized int getResolvedCount() {
        return resolvedCount;
    }

    public synchronized void setResolvedCount(int resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    @Override
    public String toString() {
        String statusText = available
                ? "AVAILABLE"
                : "BUSY (Incident #" + currentIncidentId + ")";
        return String.format("[Tech #%d] %-18s | Spec: %-10s | %-24s | Resolved: %d",
                technicianId, name, specialization, statusText, resolvedCount);
    }
}
