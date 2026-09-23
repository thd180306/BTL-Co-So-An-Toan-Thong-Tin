package ransomware.recovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Per-session countdown timer.
 * Emits EXPIRED by calling the provided callback when time is up.
 * Does NOT delete files.
 */
public final class CountdownService {

    public interface ExpiryCallback {
        void onExpired(String recoveryId);
    }

    private static final ScheduledExecutorService SCHEDULER =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "countdown-svc");
            t.setDaemon(true);
            return t;
        });

    private final Map<String, ScheduledFuture<?>> jobs =
        new ConcurrentHashMap<>();

    /**
     * Start countdown for a session. If already started, no-op.
     * Callback runs on scheduler thread.
     */
    public void start(String recoveryId, long durationSeconds,
                      ExpiryCallback callback) {
        if (jobs.containsKey(recoveryId)) return;

        ScheduledFuture<?> f = SCHEDULER.schedule(() -> {
            try {
                System.out.println("[CountdownService] EXPIRED: " + recoveryId);
                if (callback != null) callback.onExpired(recoveryId);
            } finally {
                jobs.remove(recoveryId);
            }
        }, durationSeconds, TimeUnit.SECONDS);

        jobs.put(recoveryId, f);
    }

    public void cancel(String recoveryId) {
        ScheduledFuture<?> f = jobs.remove(recoveryId);
        if (f != null) f.cancel(false);
    }

    public boolean isRunning(String recoveryId) {
        return jobs.containsKey(recoveryId);
    }
}