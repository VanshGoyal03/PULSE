package pulse.model;

import java.time.LocalDateTime;

/**
 * Represents an electrical failure or power disruption incident.
 */
public class ElectricalIncident extends Incident {
    public static final String TYPE = "ELECTRICAL";

    private String panelOrCircuit;
    private String voltageLevel;
    private boolean powerOutage;

    public ElectricalIncident(String title, String description, String location,
                              Priority priority, String reportedBy,
                              String panelOrCircuit, String voltageLevel, boolean powerOutage) {
        super(title, description, location, priority, reportedBy);
        this.panelOrCircuit = panelOrCircuit == null ? "Main Panel" : panelOrCircuit.trim();
        this.voltageLevel = voltageLevel == null ? "230V" : voltageLevel.trim();
        this.powerOutage = powerOutage;
    }

    public ElectricalIncident(int incidentId, String title, String description, String location,
                              Priority priority, IncidentStatus status, String reportedBy,
                              Integer assignedTechnicianId, LocalDateTime createdAt,
                              LocalDateTime updatedAt, LocalDateTime resolvedAt,
                              String panelOrCircuit, String voltageLevel, boolean powerOutage) {
        super(incidentId, title, description, location, priority, status, reportedBy,
                assignedTechnicianId, createdAt, updatedAt, resolvedAt);
        this.panelOrCircuit = panelOrCircuit;
        this.voltageLevel = voltageLevel;
        this.powerOutage = powerOutage;
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
        return powerOutage ? 60 : 35;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Panel/Circuit: %s | Voltage: %s | Power Outage: %s",
                panelOrCircuit, voltageLevel, powerOutage ? "YES" : "NO");
    }

    @Override
    public String serializeSpecificData() {
        return panelOrCircuit + ";;" + voltageLevel + ";;" + powerOutage;
    }

    public String getPanelOrCircuit() {
        return panelOrCircuit;
    }

    public void setPanelOrCircuit(String panelOrCircuit) {
        this.panelOrCircuit = panelOrCircuit;
    }

    public String getVoltageLevel() {
        return voltageLevel;
    }

    public void setVoltageLevel(String voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    public boolean isPowerOutage() {
        return powerOutage;
    }

    public void setPowerOutage(boolean powerOutage) {
        this.powerOutage = powerOutage;
    }
}
