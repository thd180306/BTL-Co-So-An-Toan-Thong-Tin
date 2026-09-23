package ransomware.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the lifecycle of a per-session sandbox workspace.
 *
 * <p>One instance is created per {@code recoveryId}. Responsibilities:
 * <ul>
 *   <li>Copy {@code baseline\} → {@code working\<recoveryId>\} (prepareWorkspace)</li>
 *   <li>Provide typed path accessors for working, restored, and manifest dirs</li>
 *   <li>List files in the working directory</li>
 *   <li>Clean up (delete) only {@code working\<recoveryId>\}</li>
 * </ul>
 *
 * <p>All paths are validated by {@link SandboxGuard} before use.
 * All I/O uses {@code java.nio.file.*} exclusively.
 */
public class SandboxManager {

    private final String recoveryId;
    private final Path workingDir;
    private final Path restoredDir;
    private final Path manifestDir;
    private final Path baselineDir;

    /**
     * Creates a SandboxManager for the given recoveryId.
     *
     * @param recoveryId session identifier, format RCV-YYYYMMDD-XXXXXX
     * @throws SandboxException if recoveryId fails input validation
     */
    public SandboxManager(String recoveryId) {
        // Validate recoveryId via SandboxGuard input rules
        SandboxGuard.validateInput(recoveryId, "recoveryId");

        this.recoveryId  = recoveryId;
        this.baselineDir = SandboxGuard.resolveSandboxRoot().resolve("baseline");
        this.workingDir  = SandboxGuard.resolveSandboxRoot().resolve("working").resolve(recoveryId);
        this.restoredDir = SandboxGuard.resolveSandboxRoot().resolve("restored").resolve(recoveryId);
        this.manifestDir = SandboxGuard.resolveSandboxRoot().resolve("manifest");
    }

    // ── Workspace lifecycle ───────────────────────────────────────────────

    /**
     * Copies all files from {@code baseline\} into {@code working\<recoveryId>\}.
     * Creates the working directory if it does not exist.
     * baseline\ is never modified — files are copied, not moved.
     *
     * @return the working directory Path
     * @throws SandboxException if baseline is missing or a copy fails
     */
    public Path prepareWorkspace() {
        if (!Files.isDirectory(baselineDir)) {
            throw new SandboxException(
                    "baseline\\ directory does not exist or is not a directory: " + baselineDir);
        }

        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            throw new SandboxException("Cannot create working directory: " + workingDir, e);
        }

        // Copy each regular file from baseline\ → working\<recoveryId>\
        try (Stream<Path> stream = Files.list(baselineDir)) {
            List<Path> baselineFiles = stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            if (baselineFiles.isEmpty()) {
                throw new SandboxException(
                        "baseline\\ contains no files — cannot prepare workspace");
            }

            for (Path src : baselineFiles) {
                // Validate source is truly inside sandbox (it should be, but verify)
                if (!SandboxGuard.isInsideSandbox(src)) {
                    throw new SandboxException("baseline file outside sandbox: " + src);
                }
                Path dst = workingDir.resolve(src.getFileName());
                // dst is implicitly inside sandbox because workingDir already is
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new SandboxException("Error preparing workspace for " + recoveryId, e);
        }

        return workingDir;
    }

    // ── Path accessors ────────────────────────────────────────────────────

    /** @return {@code working\<recoveryId>\} path (may not yet exist on disk) */
    public Path getWorkingPath() {
        return workingDir;
    }

    /** @return {@code restored\<recoveryId>\} path (may not yet exist on disk) */
    public Path getRestoredPath() {
        return restoredDir;
    }

    /** @return {@code manifest\} path */
    public Path getManifestPath() {
        return manifestDir;
    }

    // ── File listing ──────────────────────────────────────────────────────

    /**
     * Returns a list of regular files in {@code working\<recoveryId>\}.
     * Returns an empty list if the workspace does not yet exist.
     *
     * @return list of file Paths; never null
     * @throws SandboxException on I/O error
     */
    public List<Path> listWorkspaceFiles() {
        if (!Files.isDirectory(workingDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(workingDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SandboxException("Cannot list workspace files: " + workingDir, e);
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /**
     * Deletes {@code working\<recoveryId>\} and all its contents.
     * NEVER touches baseline\, restored\, or manifest\.
     *
     * <p>If the directory does not exist, this method returns silently.
     *
     * @throws SandboxException if deletion fails or path escapes sandbox
     */
    public void cleanupWorkspace() {
        if (!SandboxGuard.isInsideSandbox(workingDir)) {
            throw new SandboxException(
                    "cleanupWorkspace: workingDir is outside sandbox — refusing to delete: " + workingDir);
        }
        if (!Files.exists(workingDir)) {
            return; // nothing to clean
        }
        deleteRecursive(workingDir);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void deleteRecursive(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {
            // Sort in reverse order so files are deleted before their parent dirs
            List<Path> paths = stream
                    .sorted(java.util.Comparator.reverseOrder())
                    .collect(Collectors.toList());

            for (Path p : paths) {
                // Hard safety check on every path before deletion
                if (!SandboxGuard.isInsideSandbox(p)) {
                    throw new SandboxException(
                            "deleteRecursive: refusing to delete path outside sandbox: " + p);
                }
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            throw new SandboxException("Error during workspace cleanup: " + dir, e);
        }
    }
}
