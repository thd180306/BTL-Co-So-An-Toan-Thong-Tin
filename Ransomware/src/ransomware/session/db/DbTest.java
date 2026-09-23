package ransomware.session.db;

import ransomware.session.DemoSession;
import ransomware.session.PaymentStatus;
import ransomware.session.SessionStatus;

import java.util.Optional;

public class DbTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {

        // Clean slate
        DatabaseManager.deleteDatabase();
        DatabaseManager.initSchema();

        SessionRepository sessionRepo = new SessionRepository();
        KeyRepository keyRepo = new KeyRepository();
        FileRepository fileRepo = new FileRepository();
        PaymentRepository payRepo = new PaymentRepository();
        AuditRepository auditRepo = new AuditRepository();

        // T1: insert + lookup
        DemoSession s = new DemoSession(
            "SES-000001", "RCV-20260923-AAAAAA",
            "f7f1f0f0-0000-0000-0000-000000000001",
            1000L, 3000L,
            SessionStatus.CREATED, PaymentStatus.PENDING);
        sessionRepo.insert(s);

        Optional<DemoSession> found = sessionRepo.findByRecoveryId("RCV-20260923-AAAAAA");
        check("T1. Insert + lookup",
              found.isPresent() && "SES-000001".equals(found.get().getSessionId()));

        // T2: latest
        Optional<DemoSession> latest = sessionRepo.findLatest();
        check("T2. findLatest", latest.isPresent());

        // T3: update status
        sessionRepo.updateStatus("RCV-20260923-AAAAAA", SessionStatus.LOCKED);
        found = sessionRepo.findByRecoveryId("RCV-20260923-AAAAAA");
        check("T3. updateStatus",
              found.isPresent() && found.get().getStatus() == SessionStatus.LOCKED);

        // T4: update payment
        sessionRepo.updatePaymentStatus("RCV-20260923-AAAAAA", PaymentStatus.CONFIRMED);
        found = sessionRepo.findByRecoveryId("RCV-20260923-AAAAAA");
        check("T4. updatePaymentStatus",
              found.isPresent() && found.get().getPaymentStatus() == PaymentStatus.CONFIRMED);

        // T5: key insert + lookup
        keyRepo.insert("RCV-20260923-AAAAAA", "KEY-001", "B64PRIVATEKEY==", 1000L);
        Optional<String> key = keyRepo.findPrivateKey("RCV-20260923-AAAAAA");
        check("T5. key insert + lookup",
              key.isPresent() && "B64PRIVATEKEY==".equals(key.get()));

        // T6: key markUsed
        keyRepo.markUsed("RCV-20260923-AAAAAA", 2000L);
        Optional<String> stillKey = keyRepo.findPrivateKey("RCV-20260923-AAAAAA");
        check("T6. key markUsed (still retrievable)", stillKey.isPresent());

        // T7: file manifest insert
        fileRepo.insert("RCV-20260923-AAAAAA",
                        "document.txt", "HASH_A", "document.txt.locked");
        var names = fileRepo.listOriginalNames("RCV-20260923-AAAAAA");
        check("T7. file insert + list",
              names.size() == 1 && "document.txt".equals(names.get(0)));

        // T8: file update restore status
        fileRepo.updateRestoreStatus(
            "RCV-20260923-AAAAAA", "document.txt", "HASH_A", "VERIFIED");
        names = fileRepo.listOriginalNames("RCV-20260923-AAAAAA");
        check("T8. file update restore status", names.size() == 1);

        // T9: payment insert + confirm
        payRepo.insertPending("RCV-20260923-AAAAAA", 100, 1000L);
        payRepo.confirm("RCV-20260923-AAAAAA", 2000L);
        check("T9. payment insert + confirm", true);

        // T10: audit log
        auditRepo.log(1000L, "RCV-20260923-AAAAAA", "GAME_CLOSED", "OK", "{}");
        auditRepo.log(2000L, "RCV-20260923-AAAAAA", "PAYMENT_CONFIRMED", "OK", "{}");
        check("T10. audit log count", auditRepo.count() == 2);

        // T11: unknown recoveryId
        Optional<DemoSession> missing = sessionRepo.findByRecoveryId("RCV-99999999-XXXXXX");
        check("T11. unknown recoveryId returns empty", missing.isEmpty());

        // T12: DB path contains "sessions.db"
        check("T12. DB path is sessions.db",
              DatabaseManager.getDbPath().endsWith("sessions.db"));

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