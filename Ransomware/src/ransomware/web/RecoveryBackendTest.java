package ransomware.web;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Smoke test — assumes BOTH DemoController (:5000) and RecoveryBackend (:8080)
 * are running.
 */
public class RecoveryBackendTest {

    public static void main(String[] args) throws Exception {
        int passed = 0, failed = 0;

        // T1 — backend reachable + serves index.html
        String html = get("http://127.0.0.1:8080/");
        if (html.contains("PIXEL CHASER") && html.contains("Recovery Portal")) {
            System.out.println("[PASS] T1. Backend serves index.html");
            passed++;
        } else {
            System.out.println("[FAIL] T1. Backend did not serve expected HTML");
            failed++;
        }

        // T2 — CSS served
        String css = get("http://127.0.0.1:8080/style.css");
        if (css.contains(".header")) {
            System.out.println("[PASS] T2. Serves style.css");
            passed++;
        } else {
            System.out.println("[FAIL] T2. style.css not served");
            failed++;
        }

        // T3 — JS served
        String js = get("http://127.0.0.1:8080/app.js");
        if (js.contains("fetchLatestSession")) {
            System.out.println("[PASS] T3. Serves app.js");
            passed++;
        } else {
            System.out.println("[FAIL] T3. app.js not served");
            failed++;
        }

        // T4 — proxy /api/session/latest to :5000
        String api = get("http://127.0.0.1:8080/api/session/latest");
        if (api.contains("\"found\"")) {
            System.out.println("[PASS] T4. Proxies /api/session/latest");
            passed++;
        } else {
            System.out.println("[FAIL] T4. Proxy failed: " + api);
            failed++;
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("PASSED: " + passed + " / " + (passed + failed));
        System.out.println("FAILED: " + failed);
        System.out.println("========================================");

        if (failed > 0) System.exit(1);
    }

    private static String get(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(2000);
        c.setReadTimeout(5000);
        try (Scanner sc = new Scanner(c.getInputStream())) {
            sc.useDelimiter("\\A");
            return sc.hasNext() ? sc.next() : "";
        }
    }
}