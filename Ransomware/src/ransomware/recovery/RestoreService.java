package ransomware.recovery;

import ransomware.crypto.CryptoAdapter;
import ransomware.crypto.DecryptionResult;
import ransomware.crypto.EncryptionResult;
import ransomware.session.SessionManager;
import ransomware.session.db.FileRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Restores .locked files and verifies SHA-256 against original manifest.
 */
public final class RestoreService {

    private final SessionManager sessionMgr;
    private final FileRepository fileRepo;

    public RestoreService() {
        this.sessionMgr = new SessionManager();
        this.fileRepo = new FileRepository();
    }

    /**
     * Full restore pipeline:
     * 1. decrypt working\<recoveryId>\
     * 2. compute hashes of restored files
     * 3. compare vs original hashes (from encrypt phase)
     * 4. return result
     *
     * Note: caller must have already validated payment + key + state.
     */
    public RestoreResult restore(String recoveryId,
                                 Map<String, String> originalHashes)
            throws Exception {
        String key = new ransomware.session.db.KeyRepository()
            .findPrivateKey(recoveryId)
            .orElse(null);
        return restore(recoveryId, key, originalHashes);
    }

    public RestoreResult restore(String recoveryId,
                                 String privateKeyB64,
                                 Map<String, String> originalHashes)
            throws Exception {
        long start = System.currentTimeMillis();
        RestoreResult result = new RestoreResult(recoveryId);

        CryptoAdapter adapter = new CryptoAdapter();
        DecryptionResult dec;
        if (privateKeyB64 != null && !privateKeyB64.isBlank()) {
            dec = adapter.decryptWithKey(recoveryId, privateKeyB64);
        } else {
            dec = adapter.decrypt(recoveryId);
        }

        // Compare each original hash to restored hash
        for (Map.Entry<String, String> entry : originalHashes.entrySet()) {
            String name = entry.getKey();
            String originalHash = entry.getValue();
            String restoredHash = dec.restoredHashes.get(name);

            if (restoredHash == null) {
                result.failedFiles.add(name);
                result.mismatched.add(name + " (missing)");
                result.failedCount++;
            } else if (!originalHash.equals(restoredHash)) {
                result.failedFiles.add(name);
                result.mismatched.add(name + " (hash mismatch)");
                result.failedCount++;
            } else {
                result.verifiedFiles.add(name);
                result.verifiedCount++;
                // Update manifest
                try {
                    fileRepo.updateRestoreStatus(
                        recoveryId, name, restoredHash, "VERIFIED");
                } catch (Exception ignored) { }
            }
        }

        result.durationMs = System.currentTimeMillis() - start;
        result.success = result.failedCount == 0 && result.verifiedCount > 0;
        return result;
    }
}