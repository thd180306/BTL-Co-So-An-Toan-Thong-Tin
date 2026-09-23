package ransomware.session;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Plain main() test for the session domain model.
 * No JUnit dependency.
 */
public class DomainTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        // T1
        check("T1. CREATED->LOCKED allowed",
              SessionStatus.CREATED.canTransitionTo(SessionStatus.LOCKED));

        // T2
        check("T2. CREATED->COMPLETED rejected",
              !SessionStatus.CREATED.canTransitionTo(SessionStatus.COMPLETED));

        // T3
        check("T3. PAYMENT_PENDING->EXPIRED allowed",
              SessionStatus.PAYMENT_PENDING.canTransitionTo(SessionStatus.EXPIRED));

        // T4
        check("T4. COMPLETED.isTerminal()",
              SessionStatus.COMPLETED.isTerminal());

        // T5
        check("T5. RESTORING.isTerminal() == false",
              !SessionStatus.RESTORING.isTerminal());

        // T6
        check("T6. PaymentStatus.CONFIRMED.isTerminal()",
              PaymentStatus.CONFIRMED.isTerminal());

        // T7
        SessionIdGenerator.reset(1);
        String first = SessionIdGenerator.next();
        check("T7. First sessionId == SES-000001",
              "SES-000001".equals(first));

        // T8
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(SessionIdGenerator.next());
        }
        check("T8. 1000 sequential sessionIds unique",
              ids.size() == 1000);

        // T9
        String recoveryId = RecoveryIdGenerator.next();
        check("T9. recoveryId matches regex (got: " + recoveryId + ")",
              recoveryId.matches("RCV-\\d{8}-[0-9A-F]{6}"));

        // T10
        DemoSession s1 = new DemoSession(
            "SES-000001", "RCV-20260923-A81F42",
            "f7f1f0f0-0000-0000-0000-000000000001",
            1000L, 2000L,
            SessionStatus.CREATED, PaymentStatus.PENDING);
        DemoSession s2 = s1.withStatus(SessionStatus.LOCKED);
        check("T10. withStatus returns new instance",
              s1 != s2 && s2.getStatus() == SessionStatus.LOCKED);

        // T11
        boolean threw = false;
        try {
            s1.withStatus(SessionStatus.COMPLETED);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("T11. withStatus rejects illegal transition", threw);

        // T12
        check("T12. isExpired() works with past/future",
              s1.isExpired(2500L) && !s1.isExpired(500L));

        System.out.println();
        System.out.println("========================================");
        System.out.println("PASSED: " + passed + " / " + (passed + failed));
        System.out.println("FAILED: " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + label);
            passed++;
        } else {
            System.out.println("[FAIL] " + label);
            failed++;
        }
    }
}