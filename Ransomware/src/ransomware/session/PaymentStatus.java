package ransomware.session;

/**
 * Payment state for a demo session.
 * Canonical values per CONTRACTS.md Section 3.
 */
public enum PaymentStatus {

    PENDING,
    CONFIRMED,
    FAILED;

    /**
     * Terminal states cannot transition further.
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == FAILED;
    }
}