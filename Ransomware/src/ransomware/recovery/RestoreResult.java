package ransomware.recovery;

import java.util.ArrayList;
import java.util.List;

public class RestoreResult {

    public final String recoveryId;
    public boolean success;
    public int verifiedCount;
    public int failedCount;
    public long durationMs;
    public final List<String> verifiedFiles = new ArrayList<>();
    public final List<String> failedFiles   = new ArrayList<>();
    public final List<String> mismatched    = new ArrayList<>();

    public RestoreResult(String recoveryId) {
        this.recoveryId = recoveryId;
    }

    @Override
    public String toString() {
        return "RestoreResult{" +
               "recoveryId=" + recoveryId +
               ", verified=" + verifiedCount +
               ", failed=" + failedCount +
               ", duration=" + durationMs + "ms}";
    }
}