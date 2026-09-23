package ransomware;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main class - Giải mã file .locked
 */
public class Decryption {

    private static int fileCount = 0;
    private static int errorCount = 0;

    public static void main(String[] args) {
        try {
            System.out.println("=".repeat(60));
            System.out.println("DECRYPTION LAB");
            System.out.println("=".repeat(60));

            String privPath = Config.KEY_DIR + "private.key";
            if (!Files.exists(Paths.get(privPath))) {
                System.err.println("[!] Không tìm thấy: " + privPath);
                return;
            }

            System.out.println("[1] Đọc private key: " + privPath);
            PrivateKey priv = CryptoUtils.loadPrivateKey(privPath);
            System.out.println("    ✓ " + priv.getEncoded().length + " bytes");
            System.out.println();

            List<File> files;
            try (Stream<Path> paths = Files.walk(Paths.get(Config.TARGET_DIR))) {
                files = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString()
                                .endsWith(Config.ENCRYPTED_EXT))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            }

            System.out.println("[2] Tìm thấy " + files.size() + " file .locked");
            System.out.println();

            System.out.println("[3] Giải mã:");
            for (File f : files) {
                try {
                    CryptoUtils.decryptFile(f, priv);
                    fileCount++;
                    System.out.println("    [OK] " + f.getName());
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("    [ERR] " + f.getName() + ": " + e.getMessage());
                }
            }

            System.out.println();
            System.out.println("=".repeat(60));
            System.out.println("HOÀN THÀNH");
            System.out.println("=".repeat(60));
            System.out.println("Files decrypted: " + fileCount);
            System.out.println("Errors:          " + errorCount);
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
        }
    }
}