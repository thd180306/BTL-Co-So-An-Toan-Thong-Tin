package ransomware.session.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite connection manager + schema bootstrap.
 * Owner: SessionManager only. Web_Server MUST NOT use this class.
 */
public final class DatabaseManager {

    private static final String DB_PATH =
        ransomware.AppConfig.dbPath();

    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public static String getDbPath() {
        return DB_PATH;
    }

    /**
     * Creates all 5 tables if they do not exist.
     * Idempotent — safe to call multiple times.
     */
    public static void initSchema() throws SQLException {
        try (Connection c = getConnection();
             Statement st = c.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    session_id      TEXT PRIMARY KEY,
                    recovery_id     TEXT UNIQUE NOT NULL,
                    instance_id     TEXT NOT NULL,
                    created_at      INTEGER NOT NULL,
                    expires_at      INTEGER NOT NULL,
                    status          TEXT NOT NULL,
                    payment_status  TEXT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS recovery_keys (
                    recovery_id     TEXT PRIMARY KEY,
                    key_id          TEXT NOT NULL,
                    private_key_b64 TEXT NOT NULL,
                    status          TEXT NOT NULL,
                    created_at      INTEGER NOT NULL,
                    released_at     INTEGER
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS file_manifest (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    recovery_id     TEXT NOT NULL,
                    original_name   TEXT NOT NULL,
                    original_sha256 TEXT NOT NULL,
                    current_name    TEXT NOT NULL,
                    current_sha256  TEXT,
                    restore_status  TEXT NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS payment_events (
                    payment_id      TEXT PRIMARY KEY,
                    recovery_id     TEXT NOT NULL,
                    amount_demo     INTEGER NOT NULL,
                    status          TEXT NOT NULL,
                    created_at      INTEGER NOT NULL,
                    confirmed_at    INTEGER
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS audit_events (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp       INTEGER NOT NULL,
                    recovery_id     TEXT,
                    event_type      TEXT NOT NULL,
                    status          TEXT NOT NULL,
                    metadata_json   TEXT
                )
            """);
        }
    }

    public static void deleteDatabase() throws Exception {
        Path p = Paths.get(DB_PATH);
        if (Files.exists(p)) {
            Files.delete(p);
        }
    }
}