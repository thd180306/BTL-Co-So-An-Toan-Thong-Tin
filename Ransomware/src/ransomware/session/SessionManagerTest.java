package ransomware.session;

import ransomware.session.db.DatabaseManager;

import java.util.Optional;

public class SessionManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        DatabaseManager.deleteDatabase();
        DatabaseManager.initSchema();

        SessionManager mgr = new SessionManager();

        // T1: create session
        DemoSession s1 = mgr.createSession("inst-aaa");
        check("T1. createSession returns CREATED",
              s1.getStatus() == SessionStatus.CREATED);
        check("T2. sessionId format",
              s1.getSessionId().matches("SES-\\d{6}"));
        check("T3. recoveryId format",
              s1.getRecoveryId().matches("RCV-\\d{8}-[0-9A-F]{6}"));

        // T4: unique IDs across 100 sessions
        String firstRecovery = s1.getRecoveryId();
        boolean uniqueOK = true;
        for (int i = 0; i < 100; i++) {
            DemoSession sn = mgr.createSession("inst-" + i);
            if (sn.getRecoveryId().equals(firstRecovery)) {
                uniqueOK = false;
                break;
            }
        }
        check("T4. unique recoveryId across 100 sessions", uniqueOK);

        // T5: lookup by recoveryId
        Optional<DemoSession> found = mgr.findByRecoveryId(s1.getRecoveryId());
        check("T5. findByRecoveryId works", found.isPresent());

        // T6: findLatest returns newest
        Optional<DemoSession> latest = mgr.findLatest();
        check("T6. findLatest returns a session", latest.isPresent());

        // T7: legal transition CREATED -> LOCKED
        DemoSession t = mgr.transition(s1.getRecoveryId(), SessionStatus.LOCKED);
        check("T7. transition CREATED->LOCKED", t.getStatus() == SessionStatus.LOCKED);

        // T8: illegal transition LOCKED -> COMPLETED
        boolean threw = false;
        try {
            mgr.transition(s1.getRecoveryId(), SessionStatus.COMPLETED);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("T8. illegal transition rejected", threw);

        // T9: LOCKED -> PAYMENT_PENDING
        t = mgr.transition(s1.getRecoveryId(), SessionStatus.PAYMENT_PENDING);
        check("T9. transition LOCKED->PAYMENT_PENDING",
              t.getStatus() == SessionStatus.PAYMENT_PENDING);

        // T10: confirmPayment
        t = mgr.confirmPayment(s1.getRecoveryId());
        check("T10. confirmPayment → PAYMENT_CONFIRMED",
              t.getStatus() == SessionStatus.PAYMENT_CONFIRMED
              && t.getPaymentStatus() == PaymentStatus.CONFIRMED);

        // T11: storeKey + getPrivateKey
        mgr.storeKey(s1.getRecoveryId(), "B64KEY==");
        Optional<String> key = mgr.getPrivateKey(s1.getRecoveryId());
        check("T11. storeKey + getPrivateKey",
              key.isPresent() && "B64KEY==".equals(key.get()));

        // T12: confirmPayment on already-confirmed session throws
        boolean threw2 = false;
        try {
            mgr.confirmPayment(s1.getRecoveryId());
        } catch (IllegalStateException e) {
            threw2 = true;
        }
        check("T12. double confirmPayment rejected", threw2);

        System.out.println();
        System.out.println("========================================");
        System.out.println("PASSED: " + passed + " / " + (passed + failed));
        System.out.println("FAILED: " + failed);
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }

    private static void check(String label, boolean cond) {
        if (cond) {
            System.out.println("[PASS] " + label);
            passed++;
        } else {
            System.out.println("[FAIL] " + label);
            failed++;
        }
    }
}