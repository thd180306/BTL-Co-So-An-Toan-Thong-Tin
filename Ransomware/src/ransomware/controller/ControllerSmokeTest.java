package ransomware.controller;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Smoke test: hits /api/game/closed, then verifies session exists.
 * Assumes DemoController is already running on :5000.
 */
public class ControllerSmokeTest {

    public static void main(String[] args) throws Exception {
        int passed = 0, failed = 0;

        // Wait for server (up to 5s)
        if (!waitForServer(5000)) {
            System.out.println("[FAIL] Controller not reachable on :5000");
            System.exit(1);
        }
        System.out.println("[PASS] Controller reachable");
        passed++;

        // T1: POST /api/game/closed
        String body = "{\"game\":\"PixelChaser\","
            + "\"instanceId\":\"smoke-test-001\","
            + "\"timestamp\":\"2026-09-23T12:00:00Z\"}";
        String resp1 = post("http://127.0.0.1:5000/api/game/closed", body);
        System.out.println("  Response: " + resp1);
        if (resp1.contains("\"success\":true") && resp1.contains("RCV-")) {
            System.out.println("[PASS] POST /api/game/closed");
            passed++;
        } else {
            System.out.println("[FAIL] POST /api/game/closed");
            failed++;
        }

        // T2: GET /api/session/latest
        String resp2 = get("http://127.0.0.1:5000/api/session/latest");
        System.out.println("  Response: " + resp2);
        if (resp2.contains("\"found\":true")) {
            System.out.println("[PASS] GET /api/session/latest");
            passed++;
        } else {
            System.out.println("[FAIL] GET /api/session/latest");
            failed++;
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("PASSED: " + passed + " / " + (passed + failed));
        System.out.println("FAILED: " + failed);
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }

    private static boolean waitForServer(int timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                URL url = new URL("http://127.0.0.1:5000/api/session/latest");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(500);
                c.setRequestMethod("GET");
                int code = c.getResponseCode();
                if (code == 200) return true;
            } catch (Exception ignored) { }
            try { Thread.sleep(200); } catch (InterruptedException e) { }
        }
        return false;
    }

    private static String post(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        c.setConnectTimeout(2000);
        c.setReadTimeout(5000);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(c);
    }

    private static String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(2000);
        c.setReadTimeout(5000);
        return readResponse(c);
    }

    private static String readResponse(HttpURLConnection c) throws Exception {
        try (Scanner sc = new Scanner(
                c.getInputStream(), StandardCharsets.UTF_8)) {
            sc.useDelimiter("\\A");
            return sc.hasNext() ? sc.next() : "";
        }
    }
}