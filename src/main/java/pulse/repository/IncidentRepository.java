package pulse.repository;

import pulse.database.DatabaseConnection;
import pulse.model.ElectricalIncident;
import pulse.model.Incident;
import pulse.model.IncidentStatus;
import pulse.model.LabIncident;
import pulse.model.NetworkIncident;
import pulse.model.Priority;
import pulse.model.ProjectorIncident;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for polymorphic Incident persistence.
 */
public class IncidentRepository {

    public Incident save(Incident incident) throws SQLException {
        String sql = "INSERT INTO incidents (incident_type, title, description, location, priority, status, "
                + "reported_by, assigned_technician_id, specific_data, created_at, updated_at, resolved_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, incident.getIncidentType());
            ps.setString(2, incident.getTitle());
            ps.setString(3, incident.getDescription());
            ps.setString(4, incident.getLocation());
            ps.setString(5, incident.getPriority().name());
            ps.setString(6, incident.getStatus().name());
            ps.setString(7, incident.getReportedBy());
            if (incident.getAssignedTechnicianId() != null) {
                ps.setInt(8, incident.getAssignedTechnicianId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, incident.serializeSpecificData());
            ps.setTimestamp(10, Timestamp.valueOf(incident.getCreatedAt()));
            ps.setTimestamp(11, Timestamp.valueOf(incident.getUpdatedAt()));
            if (incident.getResolvedAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(incident.getResolvedAt()));
            } else {
                ps.setNull(12, Types.TIMESTAMP);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    incident.setIncidentId(keys.getInt(1));
                }
            }
        }
        return incident;
    }

    public void updateStatusAndTechnician(Incident incident) throws SQLException {
        String sql = "UPDATE incidents SET status = ?, assigned_technician_id = ?, updated_at = ?, resolved_at = ? "
                + "WHERE incident_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, incident.getStatus().name());
            if (incident.getAssignedTechnicianId() != null) {
                ps.setInt(2, incident.getAssignedTechnicianId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setTimestamp(3, Timestamp.valueOf(incident.getUpdatedAt()));
            if (incident.getResolvedAt() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(incident.getResolvedAt()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setInt(5, incident.getIncidentId());
            ps.executeUpdate();
        }
    }

    public List<Incident> findAll() throws SQLException {
        String sql = "SELECT incident_id, incident_type, title, description, location, priority, status, "
                + "reported_by, assigned_technician_id, specific_data, created_at, updated_at, resolved_at "
                + "FROM incidents ORDER BY incident_id ASC";
        List<Incident> incidents = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                incidents.add(mapRow(rs));
            }
        }
        return incidents;
    }

    public Optional<Incident> findById(int incidentId) throws SQLException {
        String sql = "SELECT incident_id, incident_type, title, description, location, priority, status, "
                + "reported_by, assigned_technician_id, specific_data, created_at, updated_at, resolved_at "
                + "FROM incidents WHERE incident_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private Incident mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("incident_id");
        String type = rs.getString("incident_type");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String location = rs.getString("location");
        Priority priority = Priority.fromString(rs.getString("priority"));
        IncidentStatus status = IncidentStatus.fromString(rs.getString("status"));
        String reportedBy = rs.getString("reported_by");

        int rawTechId = rs.getInt("assigned_technician_id");
        Integer assignedTechId = rs.wasNull() ? null : rawTechId;

        String specificData = rs.getString("specific_data");
        LocalDateTime createdAt = toLocalDateTime(rs.getTimestamp("created_at"));
        LocalDateTime updatedAt = toLocalDateTime(rs.getTimestamp("updated_at"));
        Timestamp resolvedTs = rs.getTimestamp("resolved_at");
        LocalDateTime resolvedAt = rs.wasNull() ? null : toLocalDateTime(resolvedTs);

        String[] parts = specificData != null ? specificData.split(";;", -1) : new String[0];
        String p0 = parts.length > 0 ? parts[0] : "";
        String p1 = parts.length > 1 ? parts[1] : "";
        String p2 = parts.length > 2 ? parts[2] : "";

        return switch (type) {
            case LabIncident.TYPE -> new LabIncident(
                    id, title, description, location, priority, status, reportedBy,
                    assignedTechId, createdAt, updatedAt, resolvedAt,
                    p0, p1, Boolean.parseBoolean(p2));
            case ElectricalIncident.TYPE -> new ElectricalIncident(
                    id, title, description, location, priority, status, reportedBy,
                    assignedTechId, createdAt, updatedAt, resolvedAt,
                    p0, p1, Boolean.parseBoolean(p2));
            case ProjectorIncident.TYPE -> new ProjectorIncident(
                    id, title, description, location, priority, status, reportedBy,
                    assignedTechId, createdAt, updatedAt, resolvedAt,
                    p0, p1, p2);
            case NetworkIncident.TYPE -> new NetworkIncident(
                    id, title, description, location, priority, status, reportedBy,
                    assignedTechId, createdAt, updatedAt, resolvedAt,
                    p0, p1, Boolean.parseBoolean(p2));
            default -> throw new SQLException("Unknown incident type in database: " + type);
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
