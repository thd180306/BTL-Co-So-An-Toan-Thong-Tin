package ransomware.session;

/**
 * Immutable domain model for one demo session.
 * All state transitions produce a new instance.
 */
public final class DemoSession {

    private final String sessionId;
    private final String recoveryId;
    private final String instanceId;
    private final long createdAt;
    private final long expiresAt;
    private final SessionStatus status;
    private final PaymentStatus paymentStatus;

    public DemoSession(String sessionId,
                       String recoveryId,
                       String instanceId,
                       long createdAt,
                       long expiresAt,
                       SessionStatus status,
                       PaymentStatus paymentStatus) {
        this.sessionId = sessionId;
        this.recoveryId = recoveryId;
        this.instanceId = instanceId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public String getSessionId()          { return sessionId; }
    public String getRecoveryId()         { return recoveryId; }
    public String getInstanceId()         { return instanceId; }
    public long getCreatedAt()            { return createdAt; }
    public long getExpiresAt()            { return expiresAt; }
    public SessionStatus getStatus()      { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    /**
     * Returns a new DemoSession with the status replaced.
     * Throws IllegalArgumentException if the transition is illegal.
     */
    public DemoSession withStatus(SessionStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Illegal transition: " + this.status + " -> " + newStatus);
        }
        return new DemoSession(
            sessionId, recoveryId, instanceId,
            createdAt, expiresAt, newStatus, paymentStatus);
    }

    public DemoSession withPaymentStatus(PaymentStatus newPayment) {
        return new DemoSession(
            sessionId, recoveryId, instanceId,
            createdAt, expiresAt, status, newPayment);
    }

    public boolean isExpired(long nowEpochSeconds) {
        return nowEpochSeconds >= expiresAt;
    }

    @Override
    public String toString() {
        return "DemoSession{" +
               "recoveryId=" + recoveryId +
               ", status=" + status +
               ", payment=" + paymentStatus +
               '}';
    }
}