package ransomware.session.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class AuditRepository {

    public void log(long timestamp, String recoveryId,
                    String eventType, String status, String metadataJson)
            throws SQLException {
        String sql = """
            INSERT INTO audit_events
              (timestamp, recovery_id, event_type, status, metadata_json)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.setString(2, recoveryId);
            ps.setString(3, eventType);
            ps.setString(4, status);
            ps.setString(5, metadataJson);
            ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM audit_events");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}