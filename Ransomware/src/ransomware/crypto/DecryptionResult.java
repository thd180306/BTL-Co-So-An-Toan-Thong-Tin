package ransomware.crypto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured result of a decryption run.
 * Fields are mutable during the run, then frozen once complete.
 */
public class DecryptionResult {

    public final String recoveryId;

    public boolean success;
    public int restoredCount;
    public int failedCount;
    public long durationMs;

    public final List<String> restoredFiles = new ArrayList<>();
    public final List<String> failedFiles   = new ArrayList<>();
    public final Map<String, String> restoredHashes = new HashMap<>();

    public DecryptionResult(String recoveryId) {
        this.recoveryId = recoveryId;
    }

    @Override
    public String toString() {
        return "DecryptionResult{" +
               "recoveryId=" + recoveryId +
               ", restored=" + restoredCount +
               ", failed=" + failedCount +
               ", duration=" + durationMs + "ms}";
    }
}