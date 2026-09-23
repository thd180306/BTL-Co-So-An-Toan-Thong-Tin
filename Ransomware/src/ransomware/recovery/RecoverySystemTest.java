package ransomware.recovery;

import ransomware.crypto.CryptoAdapter;
import ransomware.crypto.EncryptionResult;
import ransomware.session.DemoSession;
import ransomware.session.SessionManager;
import ransomware.session.SessionStatus;
import ransomware.session.db.DatabaseManager;
import ransomware.sandbox.SandboxManager;

import java.util.Map;

public class RecoverySystemTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        DatabaseManager.deleteDatabase();
        DatabaseManager.initSchema();

        SessionManager mgr = new SessionManager();
        SandboxManager sandbox;

        // ============================================================
        // TEST SUITE 1 — RESTORE
        // ============================================================
        System.out.println("=== SUITE 1: Restore ===");

        DemoSession s1 = mgr.createSession("inst-restore");
        String rid1 = s1.getRecoveryId();

        sandbox = new SandboxManager(rid1);
        sandbox.prepareWorkspace();

        CryptoAdapter adapter = new CryptoAdapter();
        EncryptionResult enc = adapter.encrypt(rid1);
        check("S1.T1. Encrypt: " + enc.processedCount + " files",
              enc.processedCount > 0);

        // Save original hashes
        Map<String, String> originalHashes = enc.originalHashes;
        check("S1.T2. Original hashes recorded: " + originalHashes.size(),
              originalHashes.size() == 4);

        // Store private key in DB (simulates controller storing key in attacker DB)
        new ransomware.session.db.KeyRepository().insert(
            rid1, "KEY-" + rid1, enc.privateKeyB64, System.currentTimeMillis() / 1000);

        // Simulate payment confirmed + state transitions
        mgr.transition(rid1, SessionStatus.LOCKED);
        mgr.transition(rid1, SessionStatus.PAYMENT_PENDING);
        mgr.confirmPayment(rid1);
        mgr.transition(rid1, SessionStatus.RECOVERY_AVAILABLE);
        mgr.transition(rid1, SessionStatus.RESTORING);

        // Restore
        RestoreService rs = new RestoreService();
        RestoreResult rres = rs.restore(rid1, originalHashes);

        check("S1.T3. Restore: " + rres.verifiedCount + " verified",
              rres.verifiedCount == 4);
        check("S1.T4. Restore: 0 failed",
              rres.failedCount == 0);
        check("S1.T5. Restore success = true",
              rres.success);

        // Baseline still intact
        var baselineFiles = new java.io.File(
            "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\baseline")
            .listFiles();
        check("S1.T6. Baseline 4 files intact",
              baselineFiles != null && baselineFiles.length == 4);

        // ============================================================
        // TEST SUITE 2 — EXPIRATION
        // ============================================================
        System.out.println();
        System.out.println("=== SUITE 2: Expiration ===");

        // Create a session with past expiry (by passing TTL=1 and waiting)
        DemoSession s2 = mgr.createSession("inst-expire", 1);
        String rid2 = s2.getRecoveryId();
        mgr.transition(rid2, SessionStatus.LOCKED);
        mgr.transition(rid2, SessionStatus.PAYMENT_PENDING);

        // Wait 2 seconds for expiry
        Thread.sleep(2000);

        ExpirationService exp = new ExpirationService();
        boolean didExpire = exp.checkAndExpire(rid2);
        check("S2.T1. Expiration triggered", didExpire);

        DemoSession afterExpire = mgr.findByRecoveryId(rid2).get();
        check("S2.T2. Status = DATA_UNAVAILABLE",
              afterExpire.getStatus() == SessionStatus.DATA_UNAVAILABLE);

        // Baseline still intact (no destruction)
        baselineFiles = new java.io.File(
            "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\baseline")
            .listFiles();
        check("S2.T3. Baseline still 4 files (no destruction)",
              baselineFiles != null && baselineFiles.length == 4);

        // ============================================================
        // TEST SUITE 3 — IDEMPOTENCY
        // ============================================================
        System.out.println();
        System.out.println("=== SUITE 3: Idempotency ===");

        boolean secondTry = exp.checkAndExpire(rid2);
        check("S3.T1. Second expire attempt returns false (idempotent)",
              !secondTry);

        // Cleanup
        new SandboxManager(rid1).cleanupWorkspace();

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