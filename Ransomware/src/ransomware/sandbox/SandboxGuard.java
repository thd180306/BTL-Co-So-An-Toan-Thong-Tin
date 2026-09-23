package ransomware.sandbox;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single-responsibility path validator for the Lab_sandbox boundary.
 *
 * <p>ALL file I/O in the system MUST call a resolve method here before
 * opening, reading, writing, or deleting any file.
 *
 * <p>Rules enforced on every call:
 * <ol>
 *   <li>Reject null or blank input.</li>
 *   <li>Reject inputs containing "..", ":", "\\\\", drive letters (X:),
 *       or UNC prefixes ("\\\\server").</li>
 *   <li>Resolve against sandbox root → toAbsolutePath() → normalize().</li>
 *   <li>Canonical path must start with SANDBOX_ROOT string.</li>
 *   <li>If any check fails → throw SandboxException (never silently fail).</li>
 * </ol>
 *
 * <p>This class is final with a private constructor; all methods are static.
 */
public final class SandboxGuard {

    // ── Sandbox root constant ─────────────────────────────────────────────
    // CONTRACTS.md §8: Authorized root.
    // This is the ONLY place this path is defined in the sandbox package.
    public static final String SANDBOX_ROOT_STRING =
            ransomware.AppConfig.sandboxRoot();

    private static final Path SANDBOX_ROOT = Paths.get(SANDBOX_ROOT_STRING)
            .toAbsolutePath()
            .normalize();

    private SandboxGuard() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the resolved, normalized sandbox root Path.
     */
    public static Path resolveSandboxRoot() {
        return SANDBOX_ROOT;
    }

    /**
     * Resolves an arbitrary relative path segment under the sandbox root.
     * Use this only when none of the typed helpers (baseline/working/restored/manifest) apply.
     *
     * @param relativePath path relative to sandbox root, must not be blank or dangerous
     * @return validated absolute Path inside sandbox
     * @throws SandboxException if path escapes sandbox or contains forbidden patterns
     */
    public static Path resolveSafePath(String relativePath) {
        validateInput(relativePath, "relativePath");
        return resolve(SANDBOX_ROOT, relativePath);
    }

    /**
     * Resolves a filename under {@code baseline\}.
     * Callers may ONLY read from this path — baseline is IMMUTABLE.
     *
     * @param filename plain filename (no directory separators)
     * @return validated absolute Path inside baseline\
     */
    public static Path resolveBaselinePath(String filename) {
        validateInput(filename, "filename");
        return resolve(SANDBOX_ROOT.resolve("baseline"), filename);
    }

    /**
     * Resolves a filename under {@code working\<recoveryId>\}.
     *
     * @param recoveryId validated recoveryId (format RCV-YYYYMMDD-XXXXXX)
     * @param filename   plain filename
     * @return validated absolute Path inside working\<recoveryId>\
     */
    public static Path resolveWorkingPath(String recoveryId, String filename) {
        validateInput(recoveryId, "recoveryId");
        validateInput(filename, "filename");
        Path workingBase = SANDBOX_ROOT.resolve("working").resolve(recoveryId);
        return resolve(workingBase, filename);
    }

    /**
     * Resolves a filename under {@code restored\<recoveryId>\}.
     *
     * @param recoveryId validated recoveryId
     * @param filename   plain filename
     * @return validated absolute Path inside restored\<recoveryId>\
     */
    public static Path resolveRestoredPath(String recoveryId, String filename) {
        validateInput(recoveryId, "recoveryId");
        validateInput(filename, "filename");
        Path restoredBase = SANDBOX_ROOT.resolve("restored").resolve(recoveryId);
        return resolve(restoredBase, filename);
    }

    /**
     * Resolves a filename under {@code manifest\}.
     *
     * @param filename plain filename (e.g. "RCV-20260921-A81F42.json")
     * @return validated absolute Path inside manifest\
     */
    public static Path resolveManifestPath(String filename) {
        validateInput(filename, "filename");
        return resolve(SANDBOX_ROOT.resolve("manifest"), filename);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /**
     * Core resolution + boundary check.
     * Resolves {@code segment} against {@code base}, normalizes, then
     * verifies the result is inside SANDBOX_ROOT.
     */
    private static Path resolve(Path base, String segment) {
        Path candidate = base.resolve(segment).toAbsolutePath().normalize();
        if (!isInsideSandbox(candidate)) {
            throw new SandboxException(
                    "Path escapes sandbox boundary.\n" +
                    "  Resolved : " + candidate + "\n" +
                    "  Boundary : " + SANDBOX_ROOT
            );
        }
        return candidate;
    }

    /**
     * Returns true iff {@code path} is equal to or a child of SANDBOX_ROOT.
     */
    static boolean isInsideSandbox(Path path) {
        // normalize both to ensure consistent separators / case on Windows
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.startsWith(SANDBOX_ROOT);
    }

    /**
     * Rejects null, blank, and dangerously-formed input strings.
     */
    static void validateInput(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new SandboxException(
                    paramName + " must not be null or blank");
        }
        // Reject path traversal
        if (value.contains("..")) {
            throw new SandboxException(
                    paramName + " contains forbidden traversal sequence '..': " + value);
        }
        // Reject drive-letter colon (e.g. "C:", "D:")
        if (value.matches(".*[A-Za-z]:.*")) {
            throw new SandboxException(
                    paramName + " contains a drive letter reference: " + value);
        }
        // Reject UNC prefix
        if (value.startsWith("\\\\") || value.startsWith("//")) {
            throw new SandboxException(
                    paramName + " contains a UNC path prefix: " + value);
        }
        // Reject null bytes
        if (value.contains("\0")) {
            throw new SandboxException(
                    paramName + " contains a null byte: " + value);
        }
    }
}
