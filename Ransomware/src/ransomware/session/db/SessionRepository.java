package ransomware.session.db;

import ransomware.session.DemoSession;
import ransomware.session.PaymentStatus;
import ransomware.session.SessionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class SessionRepository {

    public void insert(DemoSession s) throws SQLException {
        String sql = """
            INSERT INTO sessions
              (session_id, recovery_id, instance_id, created_at,
               expires_at, status, payment_status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getSessionId());
            ps.setString(2, s.getRecoveryId());
            ps.setString(3, s.getInstanceId());
            ps.setLong(4, s.getCreatedAt());
            ps.setLong(5, s.getExpiresAt());
            ps.setString(6, s.getStatus().name());
            ps.setString(7, s.getPaymentStatus().name());
            ps.executeUpdate();
        }
    }

    public Optional<DemoSession> findByRecoveryId(String recoveryId)
            throws SQLException {
        String sql = "SELECT * FROM sessions WHERE recovery_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recoveryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<DemoSession> findLatest() throws SQLException {
        String sql = "SELECT * FROM sessions ORDER BY created_at DESC LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        }
    }

    public void updateStatus(String recoveryId, SessionStatus newStatus)
            throws SQLException {
        String sql = "UPDATE sessions SET status = ? WHERE recovery_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, recoveryId);
            ps.executeUpdate();
        }
    }

    public void updatePaymentStatus(String recoveryId, PaymentStatus newPayment)
            throws SQLException {
        String sql = "UPDATE sessions SET payment_status = ? WHERE recovery_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPayment.name());
            ps.setString(2, recoveryId);
            ps.executeUpdate();
        }
    }

    private DemoSession map(ResultSet rs) throws SQLException {
        return new DemoSession(
            rs.getString("session_id"),
            rs.getString("recovery_id"),
            rs.getString("instance_id"),
            rs.getLong("created_at"),
            rs.getLong("expires_at"),
            SessionStatus.valueOf(rs.getString("status")),
            PaymentStatus.valueOf(rs.getString("payment_status"))
        );
    }
}