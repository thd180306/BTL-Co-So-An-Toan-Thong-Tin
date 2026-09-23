package ransomware.session.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

/**
 * Command-line Database Inspector for sessions.db.
 * Allows presenters and evaluators to interactively inspect
 * sessions, RSA-4096 recovery keys, and SHA-256 file manifests.
 */
public class DbInspector {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            ATTACKER C2 DATABASE LIVE INSPECTOR (sessions.db)");
        System.out.println("================================================================================");

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {

            // 1. SESSIONS TABLE
            System.out.println("\n[TABLE: sessions]");
            System.out.printf("%-16s | %-22s | %-18s | %-12s | %s%n",
                "SESSION_ID", "RECOVERY_ID", "STATUS", "PAYMENT", "CREATED_AT");
            System.out.println("-".repeat(88));
            try (ResultSet rs = st.executeQuery("SELECT * FROM sessions ORDER BY created_at DESC LIMIT 5")) {
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    System.out.printf("%-16s | %-22s | %-18s | %-12s | %s%n",
                        rs.getString("session_id"),
                        rs.getString("recovery_id"),
                        rs.getString("status"),
                        rs.getString("payment_status"),
                        new Date(rs.getLong("created_at") * 1000));
                }
                if (!hasData) System.out.println("  (No session records found)");
            }

            // 2. RECOVERY KEYS TABLE
            System.out.println("\n[TABLE: recovery_keys (RSA-4096 Asymmetric Master Keys)]");
            System.out.printf("%-22s | %-12s | %-10s | %-10s | %s%n",
                "RECOVERY_ID", "KEY_ID", "STATUS", "KEY_LEN", "PRIVATE_KEY_PREVIEW");
            System.out.println("-".repeat(88));
            try (ResultSet rs = st.executeQuery("SELECT recovery_id, key_id, status, private_key_b64 FROM recovery_keys ORDER BY created_at DESC LIMIT 5")) {
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    String k = rs.getString("private_key_b64");
                    String prev = (k != null && k.length() > 24) ? k.substring(0, 24) + "..." : k;
                    System.out.printf("%-22s | %-12s | %-10s | %-10d | %s%n",
                        rs.getString("recovery_id"),
                        rs.getString("key_id"),
                        rs.getString("status"),
                        (k != null ? k.length() : 0),
                        prev);
                }
                if (!hasData) System.out.println("  (No key records found)");
            }

            // 3. FILE MANIFEST TABLE
            System.out.println("\n[TABLE: file_manifest (Cryptographic Integrity & SHA-256)]");
            System.out.printf("%-22s | %-14s | %-20s | %-10s | %s%n",
                "RECOVERY_ID", "ORIGINAL_FILE", "CURRENT_FILE", "STATUS", "SHA-256 (ORIGINAL)");
            System.out.println("-".repeat(88));
            try (ResultSet rs = st.executeQuery("SELECT * FROM file_manifest ORDER BY id DESC LIMIT 10")) {
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    String hash = rs.getString("original_sha256");
                    String hPrev = (hash != null && hash.length() > 16) ? hash.substring(0, 16) + "..." : hash;
                    System.out.printf("%-22s | %-14s | %-20s | %-10s | %s%n",
                        rs.getString("recovery_id"),
                        rs.getString("original_name"),
                        rs.getString("current_name"),
                        rs.getString("restore_status"),
                        hPrev);
                }
                if (!hasData) System.out.println("  (No manifest records found)");
            }

            System.out.println("\n" + "=".repeat(88));

        } catch (Exception e) {
            System.err.println("[ERROR] Database inspection error: " + e.getMessage());
        }
    }
}
