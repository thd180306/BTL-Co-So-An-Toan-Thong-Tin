package ransomware.session;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates recoveryId in format RCV-YYYYMMDD-XXXXXX.
 * XXXXXX is 6 uppercase hex characters.
 */
public final class RecoveryIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    private RecoveryIdGenerator() {}

    public static String next() {
        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        return next(utcToday);
    }

    public static String next(LocalDate date) {
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int nibble = RANDOM.nextInt(16);
            hex.append(Character.toUpperCase(Character.forDigit(nibble, 16)));
        }
        return "RCV-" + date.format(DATE_FMT) + "-" + hex;
    }
}