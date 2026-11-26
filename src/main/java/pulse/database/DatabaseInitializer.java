package pulse.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ensures required database tables exist and seeds default technicians if empty.
 */
public class DatabaseInitializer {

    private static final String CREATE_TECHNICIANS_SQL = """
            CREATE TABLE IF NOT EXISTS technicians (
                technician_id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                specialization ENUM('LAB', 'ELECTRICAL', 'PROJECTOR', 'NETWORK') NOT NULL,
                available BOOLEAN NOT NULL DEFAULT TRUE,
                current_incident_id INT NULL,
                resolved_count INT NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_technician_name_spec (name, specialization)
            )
            """;

    private static final String CREATE_INCIDENTS_SQL = """
            CREATE TABLE IF NOT EXISTS incidents (
                incident_id INT AUTO_INCREMENT PRIMARY KEY,
                incident_type ENUM('LAB', 'ELECTRICAL', 'PROJECTOR', 'NETWORK') NOT NULL,
                title VARCHAR(150) NOT NULL,
                description TEXT NOT NULL,
                location VARCHAR(150) NOT NULL,
                priority ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL,
                status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
                reported_by VARCHAR(100) NOT NULL,
                assigned_technician_id INT NULL,
                specific_data VARCHAR(500) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                resolved_at TIMESTAMP NULL,
                CONSTRAINT fk_incident_technician
                    FOREIGN KEY (assigned_technician_id)
                    REFERENCES technicians(technician_id)
                    ON DELETE SET NULL
            )
            """;

    private static final String CREATE_HISTORY_SQL = """
            CREATE TABLE IF NOT EXISTS incident_history (
                history_id INT AUTO_INCREMENT PRIMARY KEY,
                incident_id INT NOT NULL,
                previous_status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NULL,
                new_status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL,
                technician_id INT NULL,
                remarks VARCHAR(255) NOT NULL,
                changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_history_incident
                    FOREIGN KEY (incident_id)
                    REFERENCES incidents(incident_id)
                    ON DELETE CASCADE,
                CONSTRAINT fk_history_technician
                    FOREIGN KEY (technician_id)
                    REFERENCES technicians(technician_id)
                    ON DELETE SET NULL
            )
            """;

    public void initialize() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TECHNICIANS_SQL);
            stmt.execute(CREATE_INCIDENTS_SQL);
            stmt.execute(CREATE_HISTORY_SQL);
            seedDefaultTechnicians(conn);
        }
    }

    private void seedDefaultTechnicians(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM technicians";
        try (PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = "INSERT IGNORE INTO technicians (name, specialization, available, resolved_count) VALUES (?, ?, TRUE, 0)";
        Object[][] defaults = {
                {"Aarav Sharma", "LAB"},
                {"Priya Nair", "ELECTRICAL"},
                {"Rohan Verma", "PROJECTOR"},
                {"Sneha Kulkarni", "NETWORK"}
        };

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Object[] tech : defaults) {
                ps.setString(1, (String) tech[0]);
                ps.setString(2, (String) tech[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
