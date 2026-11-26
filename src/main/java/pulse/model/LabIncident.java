package pulse.model;

import java.time.LocalDateTime;

/**
 * Represents a faulty laboratory equipment incident.
 */
public class LabIncident extends Incident {
    public static final String TYPE = "LAB";

    private String labName;
    private String equipmentId;
    private boolean safetyHazard;

    public LabIncident(String title, String description, String location,
                       Priority priority, String reportedBy,
                       String labName, String equipmentId, boolean safetyHazard) {
        super(title, description, location, priority, reportedBy);
        this.labName = labName == null ? "General Lab" : labName.trim();
        this.equipmentId = equipmentId == null ? "N/A" : equipmentId.trim();
        this.safetyHazard = safetyHazard;
    }

    public LabIncident(int incidentId, String title, String description, String location,
                       Priority priority, IncidentStatus status, String reportedBy,
                       Integer assignedTechnicianId, LocalDateTime createdAt,
                       LocalDateTime updatedAt, LocalDateTime resolvedAt,
                       String labName, String equipmentId, boolean safetyHazard) {
        super(incidentId, title, description, location, priority, status, reportedBy,
                assignedTechnicianId, createdAt, updatedAt, resolvedAt);
        this.labName = labName;
        this.equipmentId = equipmentId;
        this.safetyHazard = safetyHazard;
    }

    @Override
    public String getIncidentType() {
        return TYPE;
    }

    @Override
    public String getRequiredSpecialization() {
        return TYPE;
    }

    @Override
    public int getEstimatedResolutionMinutes() {
        return safetyHazard ? 45 : 30;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Lab: %s | Equipment ID: %s | Safety Hazard: %s",
                labName, equipmentId, safetyHazard ? "YES" : "NO");
    }

    @Override
    public String serializeSpecificData() {
        return labName + ";;" + equipmentId + ";;" + safetyHazard;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public boolean isSafetyHazard() {
        return safetyHazard;
    }

    public void setSafetyHazard(boolean safetyHazard) {
        this.safetyHazard = safetyHazard;
    }
}
