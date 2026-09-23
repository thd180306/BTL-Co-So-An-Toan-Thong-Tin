package ransomware.session.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class KeyRepository {

    public void insert(String recoveryId, String keyId,
                       String privateKeyB64, long createdAt)
            throws SQLException {
        String sql = """
            INSERT INTO recovery_keys
              (recovery_id, key_id, private_key_b64, status, created_at, released_at)
            VALUES (?, ?, ?, 'AVAILABLE', ?, NULL)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recoveryId);
            ps.setString(2, keyId);
            ps.setString(3, privateKeyB64);
            ps.setLong(4, createdAt);
            ps.executeUpdate();
        }
    }

    public Optional<String> findPrivateKey(String recoveryId)
            throws SQLException {
        String sql = "SELECT private_key_b64 FROM recovery_keys WHERE recovery_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recoveryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                    ? Optional.of(rs.getString("private_key_b64"))
                    : Optional.empty();
            }
        }
    }

    public void markUsed(String recoveryId, long releasedAt)
            throws SQLException {
        String sql = """
            UPDATE recovery_keys
            SET status = 'USED', released_at = ?
            WHERE recovery_id = ?
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, releasedAt);
            ps.setString(2, recoveryId);
            ps.executeUpdate();
        }
    }
}