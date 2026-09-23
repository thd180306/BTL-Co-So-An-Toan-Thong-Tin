package ransomware.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Integration test suite for the sandbox package.
 * No JUnit dependency — plain main() with pass/fail counters.
 *
 * Test cases:
 *   T1  SandboxGuard rejects ".." traversal
 *   T2  SandboxGuard rejects drive letter reference
 *   T3  SandboxGuard rejects UNC prefix
 *   T4  SandboxGuard rejects blank input
 *   T5  SandboxGuard accepts valid relative filename
 *   T6  SandboxManager.prepareWorkspace() copies baseline files
 *   T7  SandboxManager.listWorkspaceFiles() returns same count
 *   T8  SandboxManager.cleanupWorkspace() removes working dir
 *  [T9] HashService.sha256Hex() returns 64-char hex string
 *  [T10] HashService.sha256ForDirectory() returns one entry per baseline file
 */
public class SandboxTest {

    // ── Test state ────────────────────────────────────────────────────────
    private static int passed = 0;
    private static int failed = 0;

    // Unique recoveryId for this test run so parallel runs don't collide
    private static final String TEST_RECOVERY_ID =
            "RCV-TEST00-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();

    // ── Entry point ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("SandboxTest — Lab Sandbox Manager");
        System.out.println("recoveryId: " + TEST_RECOVERY_ID);
        System.out.println("=".repeat(60));

        runT1_RejectTraversal();
        runT2_RejectDriveLetter();
        runT3_RejectUNC();
        runT4_RejectBlank();
        runT5_AcceptValidFilename();
        runT6_PrepareWorkspace();
        runT7_ListWorkspaceFiles();
        runT8_CleanupWorkspace();
        runT9_HashFile();
        runT10_HashDirectory();

        System.out.println("=".repeat(60));
        System.out.printf("RESULTS: %d passed, %d failed%n", passed, failed);
        System.out.println("=".repeat(60));
        if (failed > 0) System.exit(1);
    }

    // ── T1: Reject ".." traversal ─────────────────────────────────────────
    private static void runT1_RejectTraversal() {
        String label = "T1 SandboxGuard rejects '..' traversal";
        try {
            SandboxGuard.resolveSafePath("../some/escape");
            fail(label, "Expected SandboxException but none was thrown");
        } catch (SandboxException e) {
            pass(label, "Rejected: " + e.getMessage().split("\n")[0]);
        } catch (Exception e) {
            fail(label, "Wrong exception type: " + e.getClass().getName());
        }
    }

    // ── T2: Reject drive letter ───────────────────────────────────────────
    private static void runT2_RejectDriveLetter() {
        String label = "T2 SandboxGuard rejects drive letter";
        try {
            SandboxGuard.resolveSafePath("C:\\Windows\\System32");
            fail(label, "Expected SandboxException but none was thrown");
        } catch (SandboxException e) {
            pass(label, "Rejected: " + e.getMessage().split("\n")[0]);
        } catch (Exception e) {
            fail(label, "Wrong exception type: " + e.getClass().getName());
        }
    }

    // ── T3: Reject UNC prefix ─────────────────────────────────────────────
    private static void runT3_RejectUNC() {
        String label = "T3 SandboxGuard rejects UNC prefix";
        try {
            SandboxGuard.resolveSafePath("\\\\server\\share\\file.txt");
            fail(label, "Expected SandboxException but none was thrown");
        } catch (SandboxException e) {
            pass(label, "Rejected: " + e.getMessage().split("\n")[0]);
        } catch (Exception e) {
            fail(label, "Wrong exception type: " + e.getClass().getName());
        }
    }

    // ── T4: Reject blank input ────────────────────────────────────────────
    private static void runT4_RejectBlank() {
        String label = "T4 SandboxGuard rejects blank input";
        try {
            SandboxGuard.resolveSafePath("   ");
            fail(label, "Expected SandboxException but none was thrown");
        } catch (SandboxException e) {
            pass(label, "Rejected blank: " + e.getMessage());
        } catch (Exception e) {
            fail(label, "Wrong exception type: " + e.getClass().getName());
        }
    }

    // ── T5: Accept valid filename ─────────────────────────────────────────
    private static void runT5_AcceptValidFilename() {
        String label = "T5 SandboxGuard accepts valid relative filename";
        try {
            Path p = SandboxGuard.resolveBaselinePath("document.txt");
            boolean inside = SandboxGuard.isInsideSandbox(p);
            if (inside) {
                pass(label, "Resolved to: " + p);
            } else {
                fail(label, "Path resolved but is outside sandbox: " + p);
            }
        } catch (Exception e) {
            fail(label, "Unexpected exception: " + e.getMessage());
        }
    }

    // ── T6: prepareWorkspace copies baseline files ────────────────────────
    private static void runT6_PrepareWorkspace() {
        String label = "T6 SandboxManager.prepareWorkspace() copies baseline";
        try {
            SandboxManager mgr = new SandboxManager(TEST_RECOVERY_ID);
            Path workDir = mgr.prepareWorkspace();

            // baseline\ has 4 files; working\ should have the same count
            long baselineCount = Files.list(
                    SandboxGuard.resolveSandboxRoot().resolve("baseline"))
                    .filter(Files::isRegularFile).count();
            long workingCount  = Files.list(workDir)
                    .filter(Files::isRegularFile).count();

            if (workingCount == baselineCount && workingCount > 0) {
                pass(label, "Copied " + workingCount + " files to " + workDir);
            } else {
                fail(label, "baseline=" + baselineCount + " working=" + workingCount);
            }
        } catch (Exception e) {
            fail(label, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── T7: listWorkspaceFiles returns same count ─────────────────────────
    private static void runT7_ListWorkspaceFiles() {
        String label = "T7 SandboxManager.listWorkspaceFiles() correct count";
        try {
            SandboxManager mgr   = new SandboxManager(TEST_RECOVERY_ID);
            List<Path> files     = mgr.listWorkspaceFiles();
            long baselineCount   = Files.list(
                    SandboxGuard.resolveSandboxRoot().resolve("baseline"))
                    .filter(Files::isRegularFile).count();

            if (files.size() == baselineCount) {
                pass(label, "Listed " + files.size() + " file(s)");
            } else {
                fail(label, "Expected " + baselineCount + ", got " + files.size());
            }
        } catch (Exception e) {
            fail(label, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── T8: cleanupWorkspace removes working dir ──────────────────────────
    private static void runT8_CleanupWorkspace() {
        String label = "T8 SandboxManager.cleanupWorkspace() removes working dir";
        try {
            SandboxManager mgr = new SandboxManager(TEST_RECOVERY_ID);
            Path workDir = mgr.getWorkingPath();

            // Ensure it exists first (T6 created it)
            if (!Files.isDirectory(workDir)) {
                // Re-prepare in case T6 was skipped
                mgr.prepareWorkspace();
            }

            mgr.cleanupWorkspace();

            if (!Files.exists(workDir)) {
                pass(label, "Working dir removed: " + workDir);
            } else {
                fail(label, "Working dir still exists after cleanup: " + workDir);
            }
        } catch (Exception e) {
            fail(label, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── T9: sha256Hex returns 64-char lowercase hex ───────────────────────
    private static void runT9_HashFile() {
        String label = "T9 HashService.sha256Hex() returns 64-char hex";
        try {
            Path baselineFile = SandboxGuard.resolveSandboxRoot()
                    .resolve("baseline").resolve("document.txt");
            if (!Files.isRegularFile(baselineFile)) {
                fail(label, "baseline\\document.txt not found — cannot test hash");
                return;
            }
            String hex = HashService.sha256Hex(baselineFile);
            if (hex != null && hex.length() == 64 && hex.matches("[0-9a-f]+")) {
                pass(label, "SHA-256: " + hex.substring(0, 16) + "…");
            } else {
                fail(label, "Unexpected hash output: " + hex);
            }
        } catch (Exception e) {
            fail(label, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── T10: sha256ForDirectory returns one entry per baseline file ───────
    private static void runT10_HashDirectory() {
        String label = "T10 HashService.sha256ForDirectory() baseline";
        try {
            Path baselineDir = SandboxGuard.resolveSandboxRoot().resolve("baseline");
            Map<String, String> hashes = HashService.sha256ForDirectory(baselineDir);
            long fileCount = Files.list(baselineDir).filter(Files::isRegularFile).count();

            if (hashes.size() == fileCount && fileCount > 0) {
                pass(label, hashes.size() + " entries: " + hashes.keySet());
            } else {
                fail(label, "Expected " + fileCount + " entries, got " + hashes.size());
            }
        } catch (Exception e) {
            fail(label, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static void pass(String label, String detail) {
        passed++;
        System.out.printf("  [PASS] %s%n         %s%n", label, detail);
    }

    private static void fail(String label, String detail) {
        failed++;
        System.out.printf("  [FAIL] %s%n         %s%n", label, detail);
    }
}
