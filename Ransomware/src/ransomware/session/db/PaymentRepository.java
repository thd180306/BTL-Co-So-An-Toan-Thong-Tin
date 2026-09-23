package ransomware.session.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class PaymentRepository {

    public String insertPending(String recoveryId, int amountDemo, long createdAt)
            throws SQLException {
        String paymentId = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO payment_events
              (payment_id, recovery_id, amount_demo, status, created_at, confirmed_at)
            VALUES (?, ?, ?, 'PENDING', ?, NULL)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            ps.setString(2, recoveryId);
            ps.setInt(3, amountDemo);
            ps.setLong(4, createdAt);
            ps.executeUpdate();
        }
        return paymentId;
    }

    public void confirm(String recoveryId, long confirmedAt)
            throws SQLException {
        String sql = """
            UPDATE payment_events
            SET status = 'CONFIRMED', confirmed_at = ?
            WHERE recovery_id = ? AND status = 'PENDING'
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, confirmedAt);
            ps.setString(2, recoveryId);
            ps.executeUpdate();
        }
    }
}