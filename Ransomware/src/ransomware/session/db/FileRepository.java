package ransomware.session.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class FileRepository {

    public void insert(String recoveryId, String originalName,
                       String originalSha256, String currentName)
            throws SQLException {
        String sql = """
            INSERT INTO file_manifest
              (recovery_id, original_name, original_sha256,
               current_name, current_sha256, restore_status)
            VALUES (?, ?, ?, ?, NULL, 'PENDING')
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recoveryId);
            ps.setString(2, originalName);
            ps.setString(3, originalSha256);
            ps.setString(4, currentName);
            ps.executeUpdate();
        }
    }

    public void updateRestoreStatus(String recoveryId, String originalName,
                                    String currentSha256, String status)
            throws SQLException {
        String sql = """
            UPDATE file_manifest
            SET current_sha256 = ?, restore_status = ?
            WHERE recovery_id = ? AND original_name = ?
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, currentSha256);
            ps.setString(2, status);
            ps.setString(3, recoveryId);
            ps.setString(4, originalName);
            ps.executeUpdate();
        }
    }

    public List<String> listOriginalNames(String recoveryId)
            throws SQLException {
        String sql = "SELECT original_name FROM file_manifest WHERE recovery_id = ?";
        List<String> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recoveryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("original_name"));
                }
            }
        }
        return out;
    }
}