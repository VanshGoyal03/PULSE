package pulse.model;

import java.time.LocalDateTime;

/**
 * Represents a campus network or Wi-Fi disruption incident.
 */
public class NetworkIncident extends Incident {
    public static final String TYPE = "NETWORK";

    private String buildingZone;
    private String networkSsid;
    private boolean affectsMultipleUsers;

    public NetworkIncident(String title, String description, String location,
                           Priority priority, String reportedBy,
                           String buildingZone, String networkSsid, boolean affectsMultipleUsers) {
        super(title, description, location, priority, reportedBy);
        this.buildingZone = buildingZone == null ? "Campus Block" : buildingZone.trim();
        this.networkSsid = networkSsid == null ? "Campus-WiFi" : networkSsid.trim();
        this.affectsMultipleUsers = affectsMultipleUsers;
    }

    public NetworkIncident(int incidentId, String title, String description, String location,
                           Priority priority, IncidentStatus status, String reportedBy,
                           Integer assignedTechnicianId, LocalDateTime createdAt,
                           LocalDateTime updatedAt, LocalDateTime resolvedAt,
                           String buildingZone, String networkSsid, boolean affectsMultipleUsers) {
        super(incidentId, title, description, location, priority, status, reportedBy,
                assignedTechnicianId, createdAt, updatedAt, resolvedAt);
        this.buildingZone = buildingZone;
        this.networkSsid = networkSsid;
        this.affectsMultipleUsers = affectsMultipleUsers;
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
        return affectsMultipleUsers ? 40 : 25;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Zone: %s | SSID: %s | Multi-User Impact: %s",
                buildingZone, networkSsid, affectsMultipleUsers ? "YES" : "NO");
    }

    @Override
    public String serializeSpecificData() {
        return buildingZone + ";;" + networkSsid + ";;" + affectsMultipleUsers;
    }

    public String getBuildingZone() {
        return buildingZone;
    }

    public void setBuildingZone(String buildingZone) {
        this.buildingZone = buildingZone;
    }

    public String getNetworkSsid() {
        return networkSsid;
    }

    public void setNetworkSsid(String networkSsid) {
        this.networkSsid = networkSsid;
    }

    public boolean isAffectsMultipleUsers() {
        return affectsMultipleUsers;
    }

    public void setAffectsMultipleUsers(boolean affectsMultipleUsers) {
        this.affectsMultipleUsers = affectsMultipleUsers;
    }
}
