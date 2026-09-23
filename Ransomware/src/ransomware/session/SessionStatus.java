package ransomware.session;

/**
 * Canonical session state enum — CONTRACTS.md §2.
 *
 * Values are declared in canonical order (§2.1).
 * Transition table is encoded exactly from §2.2.
 */
public enum SessionStatus {

    CREATED,
    LOCKED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    RECOVERY_AVAILABLE,
    RESTORING,
    VERIFYING,
    COMPLETED,
    EXPIRED,
    DATA_UNAVAILABLE;

    // ── Terminal states ────────────────────────────────────────────────────

    /**
     * Returns {@code true} for terminal states (COMPLETED, DATA_UNAVAILABLE).
     * Terminal states have no legal outgoing transitions.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DATA_UNAVAILABLE;
    }

    // ── Transition table (CONTRACTS.md §2.2) ──────────────────────────────

    /**
     * Returns {@code true} iff transitioning from {@code this} to {@code next}
     * is a legal state transition per CONTRACTS.md §2.2.
     *
     * Illegal transitions (not in the table) return {@code false}.
     */
    public boolean canTransitionTo(SessionStatus next) {
        if (next == null) return false;
        switch (this) {
            case CREATED:
                return next == LOCKED;

            case LOCKED:
                return next == PAYMENT_PENDING;

            case PAYMENT_PENDING:
                return next == PAYMENT_CONFIRMED
                    || next == EXPIRED;

            case PAYMENT_CONFIRMED:
                return next == RECOVERY_AVAILABLE;

            case RECOVERY_AVAILABLE:
                return next == RESTORING;

            case RESTORING:
                return next == VERIFYING;

            case VERIFYING:
                return next == COMPLETED
                    || next == RESTORING;       // retry on hash mismatch

            case EXPIRED:
                return next == DATA_UNAVAILABLE;

            case COMPLETED:
            case DATA_UNAVAILABLE:
                return false;                   // terminal — no outgoing transitions

            default:
                return false;
        }
    }
}
