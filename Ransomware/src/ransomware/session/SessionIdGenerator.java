package ransomware.session;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates sessionId in format SES-NNNNNN.
 */
public final class SessionIdGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private SessionIdGenerator() {}

    public static String next() {
        int n = COUNTER.getAndIncrement();
        return String.format("SES-%06d", n);
    }

    /** For testing or seeding. */
    public static void reset(int start) {
        COUNTER.set(start);
    }

    /**
     * Initializes counter to max(session_id) + 1 from database
     * so restarting controller never causes UNIQUE constraint collision.
     */
    public static void initFromDb(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT session_id FROM sessions")) {
            int max = 0;
            while (rs.next()) {
                String sid = rs.getString("session_id");
                if (sid != null && sid.startsWith("SES-")) {
                    try {
                        int num = Integer.parseInt(sid.substring(4));
                        if (num > max) max = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
            COUNTER.set(max + 1);
        } catch (Exception ignored) {}
    }
}