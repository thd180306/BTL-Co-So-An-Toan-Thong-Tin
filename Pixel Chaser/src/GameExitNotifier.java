package javaapplication1;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Sends a GAME_CLOSED event to the Demo Controller.
 * Runs on a background thread so game shutdown is not blocked.
 * Idempotent — safe to call multiple times, will POST only once.
 *
 * <p>Controller URL is read from app.properties (next to JAR) so the
 * victim machine can point to the attacker's real IP in 2-machine demos.
 */
public final class GameExitNotifier {

    private static final String CONTROLLER_URL;

    static {
        // Read attacker host from app.properties
        // Search order: working directory → classpath → default localhost
        String host = "127.0.0.1";
        String port = "5000";

        Properties p = new Properties();
        try {
            // 1. Working directory
            if (Files.isReadable(Paths.get("app.properties"))) {
                try (InputStream in = Files.newInputStream(Paths.get("app.properties"))) {
                    p.load(in);
                }
            } else {
                // 2. Classpath
                try (InputStream in = GameExitNotifier.class
                        .getResourceAsStream("/app.properties")) {
                    if (in != null) p.load(in);
                }
            }
            host = p.getProperty("attacker.host", host);
            port = p.getProperty("attacker.controller.port", port);
        } catch (Exception ignored) {}

        CONTROLLER_URL = "http://" + host + ":" + port + "/api/game/closed";
        System.out.println("[GameExitNotifier] Controller: " + CONTROLLER_URL);
    }

    private static final AtomicBoolean FIRED = new AtomicBoolean(false);

    private static final String INSTANCE_ID =
        UUID.randomUUID().toString();

    private GameExitNotifier() {}

    public static String getInstanceId() {
        return INSTANCE_ID;
    }

    /**
     * Fire-and-forget: sends GAME_CLOSED once, never blocks caller.
     * After the HTTP attempt (success or fail), invokes onComplete.run().
     */
    public static void notifyGameClosed(Runnable onComplete) {
        if (!FIRED.compareAndSet(false, true)) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Thread t = new Thread(() -> {
            try {
                sendHttpEvent();
            } catch (Exception e) {
                System.err.println("[GameExitNotifier] POST failed: "
                    + e.getMessage());
            } finally {
                if (onComplete != null) onComplete.run();
            }
        }, "game-exit-notifier");
        t.setDaemon(true);
        t.start();
    }

    private static void sendHttpEvent() throws Exception {
        String json = String.format(
            "{\"game\":\"PixelChaser\",\"instanceId\":\"%s\",\"timestamp\":\"%s\"}",
            INSTANCE_ID, Instant.now().toString());

        HttpURLConnection conn = null;
        try {
            URL url = new URL(CONTROLLER_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                "application/json; charset=UTF-8");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("[GameExitNotifier] POST " + CONTROLLER_URL
                + " → HTTP " + code);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}