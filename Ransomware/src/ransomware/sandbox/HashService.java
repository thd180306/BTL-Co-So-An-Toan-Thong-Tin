package ransomware.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SHA-256 hash utilities for file integrity verification.
 *
 * <p>Used by:
 * <ul>
 *   <li>Installer — builds the baseline manifest</li>
 *   <li>RestoreService — verifies restored files against manifest</li>
 * </ul>
 *
 * <p>This class is final with a private constructor; all methods are static.
 */
public final class HashService {

    private static final String ALGORITHM = "SHA-256";
    private static final int    BUFFER_SIZE = 8192;

    private HashService() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Computes the SHA-256 hex digest of a single file.
     *
     * @param file path to the file; must exist and be readable
     * @return lowercase hex string (64 characters)
     * @throws SandboxException if the file cannot be read or the algorithm is unavailable
     */
    public static String sha256Hex(Path file) {
        if (file == null) {
            throw new SandboxException("sha256Hex: file path must not be null");
        }
        if (!Files.isRegularFile(file)) {
            throw new SandboxException("sha256Hex: not a regular file: " + file);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream in = Files.newInputStream(file)) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return bytesToHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by Java spec — should never happen
            throw new SandboxException("SHA-256 algorithm not available on this JVM", e);
        } catch (IOException e) {
            throw new SandboxException("Cannot read file for hashing: " + file, e);
        }
    }

    /**
     * Computes SHA-256 for every regular file in a directory (non-recursive).
     * Files are processed in sorted order for deterministic output.
     *
     * @param dir directory to scan; must exist
     * @return ordered map of {@code filename → sha256hex}
     * @throws SandboxException on I/O error
     */
    public static Map<String, String> sha256ForDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            throw new SandboxException(
                    "sha256ForDirectory: not a directory: " + dir);
        }

        Map<String, String> result = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(Collectors.toList());

            for (Path f : files) {
                result.put(f.getFileName().toString(), sha256Hex(f));
            }
        } catch (IOException e) {
            throw new SandboxException("Cannot list directory for hashing: " + dir, e);
        }

        return result;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
