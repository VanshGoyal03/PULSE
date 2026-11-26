package pulse.repository;

import pulse.database.DatabaseConnection;
import pulse.model.IncidentHistory;
import pulse.model.IncidentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * JDBC repository for incident status change audit logs.
 */
public class HistoryRepository {

    public void save(IncidentHistory entry) throws SQLException {
        String sql = "INSERT INTO incident_history (incident_id, previous_status, new_status, technician_id, remarks, changed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entry.getIncidentId());
            if (entry.getPreviousStatus() != null) {
                ps.setString(2, entry.getPreviousStatus().name());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, entry.getNewStatus().name());
            if (entry.getTechnicianId() != null) {
                ps.setInt(4, entry.getTechnicianId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, entry.getRemarks());
            ps.setTimestamp(6, Timestamp.valueOf(entry.getChangedAt()));
            ps.executeUpdate();
        }
    }

    public List<IncidentHistory> findAll() throws SQLException {
        String sql = "SELECT history_id, incident_id, previous_status, new_status, technician_id, remarks, changed_at "
                + "FROM incident_history ORDER BY changed_at DESC, history_id DESC";
        List<IncidentHistory> historyList = new LinkedList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                historyList.add(mapRow(rs));
            }
        }
        return historyList;
    }

    public List<IncidentHistory> findByIncidentId(int incidentId) throws SQLException {
        String sql = "SELECT history_id, incident_id, previous_status, new_status, technician_id, remarks, changed_at "
                + "FROM incident_history WHERE incident_id = ? ORDER BY changed_at ASC, history_id ASC";
        List<IncidentHistory> historyList = new LinkedList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historyList.add(mapRow(rs));
                }
            }
        }
        return historyList;
    }

    private IncidentHistory mapRow(ResultSet rs) throws SQLException {
        int historyId = rs.getInt("history_id");
        int incidentId = rs.getInt("incident_id");

        String prevRaw = rs.getString("previous_status");
        IncidentStatus prevStatus = rs.wasNull() ? null : IncidentStatus.fromString(prevRaw);

        IncidentStatus newStatus = IncidentStatus.fromString(rs.getString("new_status"));

        int rawTechId = rs.getInt("technician_id");
        Integer techId = rs.wasNull() ? null : rawTechId;

        String remarks = rs.getString("remarks");
        Timestamp ts = rs.getTimestamp("changed_at");
        LocalDateTime changedAt = ts != null ? ts.toLocalDateTime() : null;

        return new IncidentHistory(historyId, incidentId, prevStatus, newStatus, techId, remarks, changedAt);
    }
}
