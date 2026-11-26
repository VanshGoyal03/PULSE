package pulse.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized JDBC connection provider.
 * Reads credentials from external application.properties (never hardcoded).
 */
public final class DatabaseConnection {
    private static final String[] PROPERTY_PATHS = {
            "src/main/resources/application.properties",
            "application.properties"
    };

    private static final Properties properties = new Properties();
    private static boolean configured = false;

    static {
        loadConfiguration();
    }

    private DatabaseConnection() {
    }

    private static void loadConfiguration() {
        for (String path : PROPERTY_PATHS) {
            try (InputStream fis = new FileInputStream(path)) {
                properties.load(fis);
                configured = true;
                return;
            } catch (IOException ignored) {
                // Try next location
            }
        }

        try (InputStream cp = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (cp != null) {
                properties.load(cp);
                configured = true;
            }
        } catch (IOException ignored) {
            // Handled in getConnection
        }
    }

    public static boolean isConfigured() {
        return configured
                && properties.getProperty("db.url") != null
                && properties.getProperty("db.username") != null
                && properties.getProperty("db.password") != null;
    }

    public static Connection getConnection() throws SQLException {
        if (!isConfigured()) {
            throw new SQLException(
                    "Database configuration not found. Copy src/main/resources/application.properties.example "
                    + "to src/main/resources/application.properties and set your MySQL credentials.");
        }
        String url = properties.getProperty("db.url").trim();
        String username = properties.getProperty("db.username").trim();
        String password = properties.getProperty("db.password").trim();

        return DriverManager.getConnection(url, username, password);
    }
}
