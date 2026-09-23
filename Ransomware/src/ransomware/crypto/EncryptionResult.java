package ransomware.crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured result of an encryption run.
 * Fields are mutable during the run, then frozen once complete.
 */
public class EncryptionResult {

    public final String recoveryId;
    public final String publicKeyPath;
    public final String privateKeyPath;
    public String privateKeyB64;

    public boolean success;
    public int processedCount;
    public int failedCount;
    public long durationMs;

    public final List<String> processedFiles = new ArrayList<>();
    public final List<String> failedFiles    = new ArrayList<>();
    public final Map<String, String> originalHashes = new HashMap<>();

    public EncryptionResult(String recoveryId,
                            String publicKeyPath,
                            String privateKeyPath) {
        this.recoveryId = recoveryId;
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public String toString() {
        return "EncryptionResult{" +
               "recoveryId=" + recoveryId +
               ", processed=" + processedCount +
               ", failed=" + failedCount +
               ", duration=" + durationMs + "ms}";
    }
}