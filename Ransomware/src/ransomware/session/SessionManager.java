package ransomware.session;

import ransomware.session.db.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Central coordinator for session lifecycle.
 * Owns sessions.db via repositories.
 * Called by DemoController only.
 */
public final class SessionManager {

    private static final long DEFAULT_TTL_SECONDS = 36 * 60; // 36 minutes

    private final SessionRepository sessionRepo;
    private final KeyRepository keyRepo;
    private final FileRepository fileRepo;
    private final PaymentRepository payRepo;
    private final AuditRepository auditRepo;

    public SessionManager() {
        this.sessionRepo = new SessionRepository();
        this.keyRepo = new KeyRepository();
        this.fileRepo = new FileRepository();
        this.payRepo = new PaymentRepository();
        this.auditRepo = new AuditRepository();
    }

    /**
     * Creates a new session for a game instance.
     * Returns the freshly created DemoSession in CREATED state.
     */
    public DemoSession createSession(String instanceId) throws SQLException {
        return createSession(instanceId, DEFAULT_TTL_SECONDS);
    }

    public DemoSession createSession(String instanceId, long ttlSeconds)
            throws SQLException {

        String sessionId = SessionIdGenerator.next();
        String recoveryId = RecoveryIdGenerator.next();
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + ttlSeconds;

        DemoSession s = new DemoSession(
            sessionId,
            recoveryId,
            instanceId,
            now,
            expiresAt,
            SessionStatus.CREATED,
            PaymentStatus.PENDING
        );

        sessionRepo.insert(s);
        auditRepo.log(now, recoveryId, "SESSION_CREATED", "OK",
                      "{\"instanceId\":\"" + instanceId + "\"}");

        return s;
    }

    /**
     * Retrieves a session by recoveryId.
     */
    public Optional<DemoSession> findByRecoveryId(String recoveryId)
            throws SQLException {
        return sessionRepo.findByRecoveryId(recoveryId);
    }

    public Optional<DemoSession> findLatest() throws SQLException {
        return sessionRepo.findLatest();
    }

    /**
     * Transitions session state. Validates transition legality
     * via DemoSession.withStatus().
     */
    public DemoSession transition(String recoveryId, SessionStatus newStatus)
            throws SQLException {

        DemoSession current = sessionRepo.findByRecoveryId(recoveryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Session not found: " + recoveryId));

        DemoSession updated = current.withStatus(newStatus); // throws if illegal
        sessionRepo.updateStatus(recoveryId, newStatus);
        auditRepo.log(Instant.now().getEpochSecond(),
                      recoveryId, "STATE_" + newStatus.name(), "OK", null);

        return updated;
    }

    /**
     * Confirms payment (mock). Advances session to PAYMENT_CONFIRMED.
     */
    public DemoSession confirmPayment(String recoveryId) throws SQLException {
        DemoSession s = sessionRepo.findByRecoveryId(recoveryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Session not found: " + recoveryId));

        if (s.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                "Payment already " + s.getPaymentStatus());
        }

        long now = Instant.now().getEpochSecond();
        payRepo.confirm(recoveryId, now);
        sessionRepo.updatePaymentStatus(recoveryId, PaymentStatus.CONFIRMED);

        DemoSession updated = sessionRepo.findByRecoveryId(recoveryId).get();
        updated = updated.withStatus(SessionStatus.PAYMENT_CONFIRMED);
        sessionRepo.updateStatus(recoveryId, SessionStatus.PAYMENT_CONFIRMED);

        auditRepo.log(now, recoveryId, "PAYMENT_CONFIRMED", "OK", null);
        return updated;
    }

    public void storeKey(String recoveryId, String privateKeyB64)
            throws SQLException {
        long now = Instant.now().getEpochSecond();
        String keyId = UUID.randomUUID().toString();
        keyRepo.insert(recoveryId, keyId, privateKeyB64, now);
        auditRepo.log(now, recoveryId, "KEY_AVAILABLE", "OK", null);
    }

    public Optional<String> getPrivateKey(String recoveryId)
            throws SQLException {
        return keyRepo.findPrivateKey(recoveryId);
    }

    public SessionRepository sessions() { return sessionRepo; }
    public KeyRepository keys()         { return keyRepo; }
    public FileRepository files()       { return fileRepo; }
    public PaymentRepository payments() { return payRepo; }
    public AuditRepository audit()      { return auditRepo; }
}