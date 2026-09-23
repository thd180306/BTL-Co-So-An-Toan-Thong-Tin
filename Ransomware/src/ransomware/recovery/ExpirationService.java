package ransomware.recovery;

import ransomware.session.DemoSession;
import ransomware.session.PaymentStatus;
import ransomware.session.SessionManager;
import ransomware.session.SessionStatus;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Checks sessions for expiry.
 * On expiry: PAYMENT_PENDING → EXPIRED → DATA_UNAVAILABLE.
 * DO NOT delete any file.
 */
public final class ExpirationService {

    private final SessionManager sessionMgr;

    public ExpirationService() {
        this.sessionMgr = new SessionManager();
    }

    /**
     * If the session's expiresAt has passed and payment still PENDING,
     * transitions the session to EXPIRED then DATA_UNAVAILABLE.
     * Returns true if any transition occurred.
     */
    public boolean checkAndExpire(String recoveryId) throws SQLException {
        Optional<DemoSession> opt = sessionMgr.findByRecoveryId(recoveryId);
        if (opt.isEmpty()) return false;

        DemoSession s = opt.get();

        if (s.getStatus() != SessionStatus.PAYMENT_PENDING) return false;
        if (s.getPaymentStatus() != PaymentStatus.PENDING) return false;

        long now = Instant.now().getEpochSecond();
        if (now < s.getExpiresAt()) return false;

        // Expire
        sessionMgr.transition(recoveryId, SessionStatus.EXPIRED);
        sessionMgr.transition(recoveryId, SessionStatus.DATA_UNAVAILABLE);

        System.out.println("[ExpirationService] Expired: " + recoveryId);
        return true;
    }
}