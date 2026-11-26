package pulse.repository;

import pulse.database.DatabaseConnection;
import pulse.model.Technician;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for Technician persistence.
 */
public class TechnicianRepository {

    public Technician save(Technician technician) throws SQLException {
        String sql = "INSERT INTO technicians (name, specialization, available, current_incident_id, resolved_count) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, technician.getName());
            ps.setString(2, technician.getSpecialization());
            ps.setBoolean(3, technician.isAvailable());
            if (technician.getCurrentIncidentId() != null) {
                ps.setInt(4, technician.getCurrentIncidentId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, technician.getResolvedCount());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    technician.setTechnicianId(keys.getInt(1));
                }
            }
        }
        return technician;
    }

    public void updateAssignment(Technician technician) throws SQLException {
        String sql = "UPDATE technicians SET available = ?, current_incident_id = ?, resolved_count = ? "
                + "WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, technician.isAvailable());
            if (technician.getCurrentIncidentId() != null) {
                ps.setInt(2, technician.getCurrentIncidentId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, technician.getResolvedCount());
            ps.setInt(4, technician.getTechnicianId());
            ps.executeUpdate();
        }
    }

    public List<Technician> findAll() throws SQLException {
        String sql = "SELECT technician_id, name, specialization, available, current_incident_id, resolved_count "
                + "FROM technicians ORDER BY technician_id ASC";
        List<Technician> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Optional<Technician> findById(int technicianId) throws SQLException {
        String sql = "SELECT technician_id, name, specialization, available, current_incident_id, resolved_count "
                + "FROM technicians WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private Technician mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("technician_id");
        String name = rs.getString("name");
        String specialization = rs.getString("specialization");
        boolean available = rs.getBoolean("available");
        int rawIncidentId = rs.getInt("current_incident_id");
        Integer currentIncidentId = rs.wasNull() ? null : rawIncidentId;
        int resolvedCount = rs.getInt("resolved_count");
        return new Technician(id, name, specialization, available, currentIncidentId, resolvedCount);
    }
}
