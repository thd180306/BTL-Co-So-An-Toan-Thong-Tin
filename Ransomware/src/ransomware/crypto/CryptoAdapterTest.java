package ransomware.crypto;

import ransomware.sandbox.HashService;
import ransomware.sandbox.SandboxManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class CryptoAdapterTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final Path SANDBOX_ROOT =
        Paths.get("D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox");
    private static final Path BASELINE =
        SANDBOX_ROOT.resolve("baseline");
    private static final Path MANIFEST_KEYS =
        SANDBOX_ROOT.resolve("manifest").resolve("keys");

    public static void main(String[] args) throws Exception {
        String rid = "RCV-20260923-TESTAD";

        // Prepare workspace (baseline → working)
        SandboxManager mgr = new SandboxManager(rid);
        Path working = mgr.prepareWorkspace();

        // Snapshot baseline before
        Map<String, String> baselineBefore =
            HashService.sha256ForDirectory(BASELINE);
        System.out.println("[INFO] Baseline file count: " + baselineBefore.size());

        // Adapter encrypt
        CryptoAdapter adapter = new CryptoAdapter();
        EncryptionResult enc = adapter.encrypt(rid);

        check("T1. encrypt() processed > 0",
              enc.processedCount > 0);
        check("T2. encrypt() success = true",
              enc.success);
        check("T3. originalHashes recorded (" + enc.originalHashes.size() + ")",
              enc.originalHashes.size() == enc.processedCount);

        // Verify .locked files exist in working
        long lockedCount;
        try (var stream = Files.list(working)) {
            lockedCount = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".locked"))
                .count();
        }
        check("T4. .locked files in working (" + lockedCount + ")",
              lockedCount == enc.processedCount);

        // Verify baseline unchanged
        Map<String, String> baselineAfter =
            HashService.sha256ForDirectory(BASELINE);
        check("T5. baseline hash unchanged",
              baselineBefore.equals(baselineAfter));

        check("T6. baseline file count unchanged",
              baselineAfter.size() == baselineBefore.size());

        // Adapter decrypt
        DecryptionResult dec = adapter.decryptWithKey(rid, enc.privateKeyB64);
        check("T7. decrypt() restored > 0",
              dec.restoredCount > 0);
        check("T8. decrypt() success = true",
              dec.success);
        check("T9. restoredHashes recorded (" + dec.restoredHashes.size() + ")",
              dec.restoredHashes.size() > 0);

        // Verify restored hash == original hash for each file
        boolean hashesMatch = true;
        for (Map.Entry<String, String> e : enc.originalHashes.entrySet()) {
            String origHash = e.getValue();
            String restoredHash = dec.restoredHashes.get(e.getKey());
            if (restoredHash == null || !origHash.equals(restoredHash)) {
                hashesMatch = false;
                System.out.println("  [MISMATCH] " + e.getKey()
                    + " orig=" + origHash.substring(0, 12) + "..."
                    + " restored=" + (restoredHash == null ? "null"
                        : restoredHash.substring(0, 12) + "..."));
            }
        }
        check("T10. all restored hashes match originals", hashesMatch);

        // Verify private key file is deleted from victim disk (lives only in C2 DB)
        Path privKey = MANIFEST_KEYS.resolve(rid + "_private.key");
        check("T11. private key file deleted from disk for security",
              !Files.exists(privKey) && enc.privateKeyB64 != null && !enc.privateKeyB64.isBlank());

        // Verify public key file exists
        Path pubKey = MANIFEST_KEYS.resolve(rid + "_public.key");
        check("T12. public key file exists",
              Files.exists(pubKey));

        // Cleanup working after test
        mgr.cleanupWorkspace();

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