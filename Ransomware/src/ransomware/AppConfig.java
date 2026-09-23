package ransomware;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads app.properties from the application directory.
 * Allows runtime configuration of attacker host/port
 * without recompiling — supports both 1-machine and 2-machine demo setups.
 *
 * <p>Search order:
 * <ol>
 *   <li>app.properties next to the running JAR (working directory)</li>
 *   <li>app.properties on the classpath</li>
 *   <li>Built-in defaults (localhost)</li>
 * </ol>
 */
public final class AppConfig {

    // ── Property keys ──────────────────────────────────────────────────────
    public static final String KEY_ATTACKER_HOST            = "attacker.host";
    public static final String KEY_CONTROLLER_PORT          = "attacker.controller.port";
    public static final String KEY_PORTAL_PORT              = "attacker.portal.port";
    public static final String KEY_SANDBOX_ROOT             = "sandbox.root";
    public static final String KEY_DB_PATH                  = "db.path";

    // ── Defaults ───────────────────────────────────────────────────────────
    private static final String DEFAULT_ATTACKER_HOST       = "127.0.0.1";
    private static final String DEFAULT_CONTROLLER_PORT     = "5000";
    private static final String DEFAULT_PORTAL_PORT         = "8080";
    private static final String DEFAULT_SANDBOX_ROOT        =
        "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Lab_sandbox";
    private static final String DEFAULT_DB_PATH             =
        "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Ransomware\\sessions.db";

    private static final Properties props = new Properties();

    static {
        loadProperties();
    }

    private AppConfig() {}

    // ── Typed accessors ────────────────────────────────────────────────────

    /** Attacker host IP or hostname. Default: 127.0.0.1 */
    public static String attackerHost() {
        return get(KEY_ATTACKER_HOST, DEFAULT_ATTACKER_HOST);
    }

    /** Full controller base URL, e.g. http://192.168.1.100:5000 */
    public static String controllerBaseUrl() {
        return "http://" + attackerHost() + ":" + get(KEY_CONTROLLER_PORT, DEFAULT_CONTROLLER_PORT);
    }

    /** Full portal URL shown to victim, e.g. http://192.168.1.100:8080/ */
    public static String portalUrl() {
        return "http://" + attackerHost() + ":" + get(KEY_PORTAL_PORT, DEFAULT_PORTAL_PORT) + "/";
    }

    /** Sandbox root path (victim local). */
    public static String sandboxRoot() {
        String configured = get(KEY_SANDBOX_ROOT, null);
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                return p.toString();
            }
        }
        // 1. Fallback for standalone installer deployment (app runs from $INSTDIR/app, data in app/data)
        Path localData = Paths.get("data").toAbsolutePath().normalize();
        if (Files.isDirectory(localData)) {
            return localData.toString();
        }
        // 2. Fallback for dev repo where Lab_sandbox exists relative to working dir
        Path devSandbox = Paths.get("Lab_sandbox").toAbsolutePath().normalize();
        if (Files.isDirectory(devSandbox)) {
            return devSandbox.toString();
        }
        // 3. Fallback for absolute default path
        Path defaultPath = Paths.get(DEFAULT_SANDBOX_ROOT);
        if (Files.isDirectory(defaultPath)) {
            return defaultPath.toAbsolutePath().normalize().toString();
        }
        return localData.toString();
    }

    /** sessions.db path (attacker side — only meaningful when running on attacker). */
    public static String dbPath() {
        String configured = get(KEY_DB_PATH, null);
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured).toAbsolutePath().normalize();
            return p.toString();
        }
        // 1. Check if sessions.db exists in current working directory (Installer layout)
        Path localDb = Paths.get("sessions.db").toAbsolutePath().normalize();
        if (Files.exists(localDb)) {
            return localDb.toString();
        }
        // 2. Check dev repo Ransomware/sessions.db
        Path devDb = Paths.get("Ransomware", "sessions.db").toAbsolutePath().normalize();
        if (Files.exists(devDb) || (devDb.getParent() != null && Files.isDirectory(devDb.getParent()))) {
            return devDb.toString();
        }
        // 3. Fallback for absolute default path
        Path defaultPath = Paths.get(DEFAULT_DB_PATH);
        if (defaultPath.getParent() != null && Files.isDirectory(defaultPath.getParent())) {
            return defaultPath.toAbsolutePath().normalize().toString();
        }
        return localDb.toString();
    }

    // ── Raw accessor ───────────────────────────────────────────────────────

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    // ── Internal loader ────────────────────────────────────────────────────

    private static void loadProperties() {
        // 1. Try working directory (next to JAR)
        Path localFile = Paths.get("app.properties");
        if (Files.isReadable(localFile)) {
            try (java.io.Reader r = new java.io.InputStreamReader(
                    Files.newInputStream(localFile),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(r);
                System.out.println("[AppConfig] Loaded: " + localFile.toAbsolutePath());
                return;
            } catch (IOException e) {
                System.err.println("[AppConfig] Failed to load " + localFile + ": " + e.getMessage());
            }
        }

        // 2. Try classpath resource
        try (InputStream raw = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (raw != null) {
                try (java.io.Reader r = new java.io.InputStreamReader(
                        raw, java.nio.charset.StandardCharsets.UTF_8)) {
                    props.load(r);
                    System.out.println("[AppConfig] Loaded from classpath: app.properties");
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("[AppConfig] Classpath load failed: " + e.getMessage());
        }

        // 3. Use built-in defaults silently
        System.out.println("[AppConfig] Using default config (localhost)");
    }
}
