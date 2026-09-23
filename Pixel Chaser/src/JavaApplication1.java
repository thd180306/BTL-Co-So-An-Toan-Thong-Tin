/*
 * Decompiled with CFR 0.152.
 */
package javaapplication1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class JavaApplication1
extends JPanel
implements Runnable,
KeyListener,
MouseListener,
MouseMotionListener {
    private javax.swing.JFrame hostWindow;
    public void setHostWindow(javax.swing.JFrame window) {
        this.hostWindow = window;
    }
    public static final int TILE_SIZE = 16;
    public static final int SCALE = 3;
    public static final int VIEW_WIDTH = 320;
    public static final int VIEW_HEIGHT = 240;
    public static final int WORLD_WIDTH = 640;
    public static final int WORLD_HEIGHT = 480;
    public static final int MINIMAP_DIV = 8;
    private Thread gameThread;
    private boolean running = false;
    private Random random = new Random();
    private GameState state = GameState.MENU;
    private boolean gameInProgress = false;
    private int score = 0;
    private int highScore = 0;
    private Lang currentLang = Lang.VI;
    private int selectedMenuIndex = 0;
    private Rectangle[] menuButtons = new Rectangle[4];
    private int selectedPauseIndex = 0;
    private Rectangle[] pauseButtons = new Rectangle[3];
    private Rectangle backButton;
    private int mouseX = 0;
    private int mouseY = 0;
    private Font gameFont;
    private double totalTime = 0.0;
    private double uiTime = 0.0;
    private double camX = 0.0;
    private double camY = 0.0;
    private double playerX;
    private double playerY;
    private double playerSpeed = 150.0;
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean spacePressed;
    private double stamina = 100.0;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double enemyX;
    private double enemyY;
    private double enemySpeed = 80.0;
    private int coinX;
    private int coinY;
    private ArrayList<Particle> particles = new ArrayList();
    private ArrayList<FloatingText> floatingTexts = new ArrayList();
    private double shakeTimer = 0.0;
    private Clip sfxClickClip;
    private Clip sfxCoinClip;
    private Clip sfxDashClip;
    private Clip sfxOverClip;
    private long lastDashSoundTime = 0L;

    public JavaApplication1() {
        this.setPreferredSize(new Dimension(960, 720));
        this.setBackground(new Color(20, 20, 28));
        this.setDoubleBuffered(true);
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.gameFont = new Font("Monospaced", 1, 12);
        int btnWidth = 160;
        int btnHeight = 28;
        int gap = 10;
        int totalMenuHeight = 4 * btnHeight + 3 * gap;
        int startY = 120 - totalMenuHeight / 2 + 20;
        for (int i = 0; i < 4; ++i) {
            this.menuButtons[i] = new Rectangle(160 - btnWidth / 2, startY + (btnHeight + gap) * i, btnWidth, btnHeight);
        }
        int totalPauseHeight = 3 * btnHeight + 2 * gap;
        int pStartY = 120 - totalPauseHeight / 2 + 10;
        for (int i = 0; i < 3; ++i) {
            this.pauseButtons[i] = new Rectangle(160 - btnWidth / 2, pStartY + (btnHeight + gap) * i, btnWidth, btnHeight);
        }
        this.backButton = new Rectangle(110, 205, 100, 28);
        this.playerX = 320.0;
        this.playerY = 240.0;
        this.camX = this.playerX - 160.0;
        this.camY = this.playerY - 120.0;
        this.enemyX = 50.0;
        this.enemyY = 50.0;
        this.spawnCoin();
        this.initAudio();
    }

    public void start() {
        this.gameThread = new Thread(this);
        this.running = true;
        this.gameThread.start();
    }

    private void initAudio() {
        this.sfxClickClip = this.createTone(880.0, 40, 0.15);
        this.sfxDashClip = this.createTone(150.0, 150, 0.2);
        this.sfxCoinClip = this.createCoinSound();
        this.sfxOverClip = this.createGameOverSound();
    }

    private Clip createTone(double freq, int durationMs, double vol) {
        try {
            float sampleRate = 44100.0f;
            int frames = (int)((float)durationMs * sampleRate / 1000.0f);
            byte[] buf = new byte[frames];
            for (int i = 0; i < frames; ++i) {
                double t = (float)i / sampleRate;
                double val = Math.sin(t * freq * 2.0 * Math.PI);
                double env = 1.0 - (double)i / (double)frames;
                buf[i] = (byte)(val * 127.0 * vol * env);
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(af, buf, 0, buf.length);
            return clip;
        }
        catch (Exception e) {
            return null;
        }
    }

    private Clip createCoinSound() {
        try {
            double val;
            int i;
            float sampleRate = 44100.0f;
            int f1 = (int)(60.0f * sampleRate / 1000.0f);
            int f2 = (int)(150.0f * sampleRate / 1000.0f);
            byte[] buf = new byte[f1 + f2];
            for (i = 0; i < f1; ++i) {
                val = Math.sin((double)((float)i / sampleRate * 987.0f) * 2.0 * Math.PI);
                buf[i] = (byte)(val * 127.0 * 0.15 * (1.0 - (double)i / (double)f1));
            }
            for (i = 0; i < f2; ++i) {
                val = Math.sin((double)((float)i / sampleRate * 1318.0f) * 2.0 * Math.PI);
                buf[f1 + i] = (byte)(val * 127.0 * 0.15 * (1.0 - (double)i / (double)f2));
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(af, buf, 0, buf.length);
            return clip;
        }
        catch (Exception e) {
            return null;
        }
    }

    private Clip createGameOverSound() {
        try {
            int i;
            float sampleRate = 44100.0f;
            int d1 = (int)(150.0f * sampleRate / 1000.0f);
            int d2 = (int)(400.0f * sampleRate / 1000.0f);
            byte[] buf = new byte[d1 * 2 + d2];
            for (i = 0; i < d1; ++i) {
                buf[i] = (byte)(Math.sin((double)((float)i / sampleRate * 300.0f * 2.0f) * Math.PI) * 127.0 * 0.2 * (1.0 - (double)i / (double)d1));
            }
            for (i = 0; i < d1; ++i) {
                buf[d1 + i] = (byte)(Math.sin((double)((float)i / sampleRate * 250.0f * 2.0f) * Math.PI) * 127.0 * 0.2 * (1.0 - (double)i / (double)d1));
            }
            for (i = 0; i < d2; ++i) {
                buf[d1 * 2 + i] = (byte)(Math.sin((double)((float)i / sampleRate * 200.0f * 2.0f) * Math.PI) * 127.0 * 0.2 * (1.0 - (double)i / (double)d2));
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(af, buf, 0, buf.length);
            return clip;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double targetFPS = 60.0;
        double nsPerTick = 1.0E9 / targetFPS;
        while (this.running) {
            long now = System.nanoTime();
            double dt = (double)(now - lastTime) / 1.0E9;
            lastTime = now;
            this.update(dt);
            this.repaint();
            long sleepTime = (long)(((double)(lastTime - System.nanoTime()) + nsPerTick) / 1000000.0);
            if (sleepTime <= 0L) continue;
            try {
                Thread.sleep(sleepTime);
            }
            catch (InterruptedException interruptedException) {}
        }
    }

    public void update(double dt) {
        double currentSpeed;
        this.uiTime += dt;
        Iterator<Particle> it = this.particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (p.isAlive()) continue;
            it.remove();
        }
        Iterator<FloatingText> fit = this.floatingTexts.iterator();
        while (fit.hasNext()) {
            FloatingText ft = fit.next();
            ft.update(dt);
            if (ft.isAlive()) continue;
            fit.remove();
        }
        if ((this.state == GameState.MENU || this.state == GameState.HOWTOPLAY) && this.random.nextDouble() < 0.1) {
            this.particles.add(new Particle((double)this.random.nextInt(320) + this.camX, (double)this.random.nextInt(240) + this.camY, new Color(255, 255, 255, 100)));
        }
        if (this.state != GameState.PLAYING) {
            return;
        }
        this.totalTime += dt;
        if (this.shakeTimer > 0.0) {
            this.shakeTimer -= dt;
        }
        boolean isDashing = this.spacePressed && this.stamina > 20.0;
        double d = currentSpeed = isDashing ? this.playerSpeed * 2.5 : this.playerSpeed;
        if (isDashing) {
            this.stamina -= 50.0 * dt;
            if (this.random.nextDouble() > 0.6) {
                this.createParticles(this.playerX, this.playerY, Color.CYAN, 1);
            }
            if (System.currentTimeMillis() - this.lastDashSoundTime > 200L) {
                this.playSound(this.sfxDashClip);
                this.lastDashSoundTime = System.currentTimeMillis();
            }
        } else if (this.stamina < 100.0) {
            this.stamina += 15.0 * dt;
        }
        double dx = 0.0;
        double dy = 0.0;
        if (this.upPressed) {
            dy -= currentSpeed * dt;
        }
        if (this.downPressed) {
            dy += currentSpeed * dt;
        }
        if (this.leftPressed) {
            dx -= currentSpeed * dt;
        }
        if (this.rightPressed) {
            dx += currentSpeed * dt;
        }
        this.playerX = Math.max(0.0, Math.min(624.0, this.playerX + dx));
        this.playerY = Math.max(0.0, Math.min(464.0, this.playerY + dy));
        this.scaleX += (1.0 - this.scaleX) * 12.0 * dt;
        this.scaleY += (1.0 - this.scaleY) * 12.0 * dt;
        double edx = this.playerX - this.enemyX;
        double edy = this.playerY - this.enemyY;
        double dist = Math.sqrt(edx * edx + edy * edy);
        if (dist > 0.0) {
            this.enemyX += edx / dist * this.enemySpeed * dt;
            this.enemyY += edy / dist * this.enemySpeed * dt;
        }
        double targetCamX = Math.max(0.0, Math.min(320.0, this.playerX - 160.0));
        double targetCamY = Math.max(0.0, Math.min(240.0, this.playerY - 120.0));
        this.camX += (targetCamX - this.camX) * 5.0 * dt;
        this.camY += (targetCamY - this.camY) * 5.0 * dt;
        Rectangle playerBox = new Rectangle((int)this.playerX, (int)this.playerY, 16, 16);
        if (playerBox.intersects(new Rectangle(this.coinX, this.coinY, 16, 16))) {
            this.playSound(this.sfxCoinClip);
            this.score += 10;
            this.enemySpeed += 5.0;
            this.createParticles(this.coinX, this.coinY, Color.YELLOW, 25);
            this.createParticles(this.coinX, this.coinY, Color.WHITE, 5);
            this.floatingTexts.add(new FloatingText("+10", this.coinX, this.coinY, Color.YELLOW));
            this.spawnCoin();
            this.shakeTimer = 0.15;
            this.scaleX = 1.5;
            this.scaleY = 0.5;
        }
        if (!isDashing && playerBox.intersects(new Rectangle((int)this.enemyX + 4, (int)this.enemyY + 4, 8, 8))) {
            this.playSound(this.sfxOverClip);
            this.state = GameState.GAMEOVER;
            this.gameInProgress = false;
            this.shakeTimer = 0.3;
            this.createParticles(this.playerX, this.playerY, Color.RED, 60);
            if (this.score > this.highScore) {
                this.highScore = this.score;
            }
        }
    }

    private void spawnCoin() {
        this.coinX = this.random.nextInt(38) * 16 + 16;
        this.coinY = this.random.nextInt(28) * 16 + 16;
    }

    private void resetPositions() {
        this.playerX = 320.0;
        this.playerY = 240.0;
        this.camX = this.playerX - 160.0;
        this.camY = this.playerY - 120.0;
        this.enemyX = 50.0;
        this.enemyY = 50.0;
        this.enemySpeed = 90.0;
        this.stamina = 100.0;
        this.score = 0;
        this.particles.clear();
        this.floatingTexts.clear();
        this.spawnCoin();
        this.shakeTimer = 0.0;
    }

    private void createParticles(double x, double y, Color color, int amount) {
        for (int i = 0; i < amount; ++i) {
            this.particles.add(new Particle(x + 8.0, y + 8.0, color));
        }
    }

    private String getText(String key) {
        if (this.currentLang == Lang.VI) {
            switch (key) {
                case "PLAY": {
                    return "CH\u01a0I M\u1edaI";
                }
                case "CONTINUE": {
                    return "CH\u01a0I TI\u1ebeP";
                }
                case "LANG": {
                    return "NG\u00d4N NG\u1eee: VI\u1ec6T NAM";
                }
                case "HELP": {
                    return "H\u01af\u1edaNG D\u1eaaN";
                }
                case "EXIT": {
                    return "THO\u00c1T GAME";
                }
                case "MAIN_MENU": {
                    return "MENU CH\u00cdNH";
                }
                case "RESUME": {
                    return "TI\u1ebeP T\u1ee4C";
                }
                case "SCORE": {
                    return "\u0110I\u1ec2M: ";
                }
                case "HIGH": {
                    return "CAO NH\u1ea4T: ";
                }
                case "PAUSED": {
                    return "T\u1ea0M D\u1eeaNG";
                }
                case "GAMEOVER": {
                    return "B\u1ea0N \u0110\u00c3 CH\u1ebeT!";
                }
                case "RESTART": {
                    return "Nh\u1ea5n ENTER \u0111\u1ec3 ch\u01a1i l\u1ea1i";
                }
                case "BACK": {
                    return "QUAY L\u1ea0I";
                }
                case "HELP_1": {
                    return "Di chuy\u1ec3n: C\u00e1c ph\u00edm W, A, S, D";
                }
                case "HELP_2": {
                    return "L\u01b0\u1edbt (Dash): Nh\u1ea5n gi\u1eef SPACE";
                }
                case "HELP_3": {
                    return "M\u1ee5c ti\u00eau: \u0102n \u0111\u1ed3ng xu v\u00e0ng";
                }
                case "HELP_4": {
                    return "Sinh t\u1ed3n: N\u00e9 k\u1ebb \u0111\u1ecbch m\u00e0u \u0111\u1ecf";
                }
            }
        } else if (this.currentLang == Lang.EN) {
            switch (key) {
                case "PLAY": {
                    return "NEW GAME";
                }
                case "CONTINUE": {
                    return "CONTINUE";
                }
                case "LANG": {
                    return "LANGUAGE: ENGLISH";
                }
                case "HELP": {
                    return "TUTORIAL";
                }
                case "EXIT": {
                    return "EXIT GAME";
                }
                case "MAIN_MENU": {
                    return "MAIN MENU";
                }
                case "RESUME": {
                    return "RESUME";
                }
                case "SCORE": {
                    return "SCORE: ";
                }
                case "HIGH": {
                    return "HIGH SCORE: ";
                }
                case "PAUSED": {
                    return "GAME PAUSED";
                }
                case "GAMEOVER": {
                    return "WASTED!";
                }
                case "RESTART": {
                    return "Press ENTER to Restart";
                }
                case "BACK": {
                    return "BACK";
                }
                case "HELP_1": {
                    return "Move: W, A, S, D Keys";
                }
                case "HELP_2": {
                    return "Dash: Hold SPACE Bar";
                }
                case "HELP_3": {
                    return "Goal: Collect gold coins";
                }
                case "HELP_4": {
                    return "Survive: Avoid the red enemy";
                }
            }
        } else if (this.currentLang == Lang.ZH) {
            switch (key) {
                case "PLAY": {
                    return "\u5f00\u59cb\u6e38\u620f";
                }
                case "CONTINUE": {
                    return "\u7ee7\u7eed\u6e38\u620f";
                }
                case "LANG": {
                    return "\u8bed\u8a00: \u4e2d\u6587";
                }
                case "HELP": {
                    return "\u6e38\u620f\u6559\u7a0b";
                }
                case "EXIT": {
                    return "\u9000\u51fa\u6e38\u620f";
                }
                case "MAIN_MENU": {
                    return "\u4e3b\u83dc\u5355";
                }
                case "RESUME": {
                    return "\u6062\u590d\u6e38\u620f";
                }
                case "SCORE": {
                    return "\u5f97\u5206: ";
                }
                case "HIGH": {
                    return "\u6700\u9ad8\u5206: ";
                }
                case "PAUSED": {
                    return "\u6e38\u620f\u6682\u505c";
                }
                case "GAMEOVER": {
                    return "\u6e38\u620f\u7ed3\u675f!";
                }
                case "RESTART": {
                    return "\u6309 ENTER \u91cd\u65b0\u5f00\u59cb";
                }
                case "BACK": {
                    return "\u8fd4\u56de";
                }
                case "HELP_1": {
                    return "\u79fb\u52a8: W, A, S, D";
                }
                case "HELP_2": {
                    return "\u51b2\u523a: \u6309\u4f4f SPACE";
                }
                case "HELP_3": {
                    return "\u76ee\u6807: \u6536\u96c6\u91d1\u5e01";
                }
                case "HELP_4": {
                    return "\u751f\u5b58: \u907f\u5f00\u7ea2\u8272\u654c\u4eba";
                }
            }
        }
        return "";
    }

    @Override
    public void paintComponent(Graphics g) {
        int intensity;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setFont(this.gameFont);
        g2.scale(3.0, 3.0);
        if (this.shakeTimer > 0.0 && this.state == GameState.PLAYING && (intensity = (int)(this.shakeTimer * 20.0)) > 0) {
            g2.translate(this.random.nextInt(intensity) - intensity / 2, this.random.nextInt(intensity) - intensity / 2);
        }
        g2.translate(-this.camX, -this.camY);
        g2.setColor(new Color(28, 28, 38));
        g2.fillRect(0, 0, 640, 480);
        g2.setColor(new Color(34, 34, 46));
        for (int i = 0; i < 640; i += 32) {
            for (int j = 0; j < 480; j += 32) {
                if ((i / 32 + j / 32) % 2 != 0) continue;
                g2.fillRect(i, j, 32, 32);
            }
        }
        if (this.state == GameState.PLAYING || this.state == GameState.GAMEOVER || this.state == GameState.PAUSED) {
            int eyeOffX = 0;
            double bobbing = Math.sin(this.totalTime * 4.0) * 4.0;
            double spin = Math.cos(this.totalTime * 5.0);
            int cWidth = (int)Math.max(2.0, 16.0 * Math.abs(spin));
            int cX = this.coinX + (16 - cWidth) / 2;
            int cY = (int)((double)this.coinY + bobbing);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval(this.coinX + 2, this.coinY + 14, 12, 4);
            g2.setColor(Color.ORANGE);
            g2.fillOval(cX, cY, cWidth, 16);
            g2.setColor(Color.YELLOW);
            g2.fillOval(cX + 1, cY + 1, Math.max(1, cWidth - 2), 14);
            if (Math.abs(spin) > 0.9) {
                g2.setColor(Color.WHITE);
                int gleamLen = (int)((Math.abs(spin) - 0.9) * 80.0);
                g2.fillRect(this.coinX + 8 - gleamLen / 2, cY + 8 - 1, gleamLen, 2);
                g2.fillRect(this.coinX + 8 - 1, cY + 8 - gleamLen / 2, 2, gleamLen);
            }
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval((int)this.enemyX + 2, (int)this.enemyY + 14, 12, 4);
            g2.setColor(new Color(220, 20, 60));
            g2.fillRoundRect((int)this.enemyX, (int)this.enemyY, 16, 16, 4, 4);
            g2.setColor(Color.WHITE);
            g2.fillRect((int)this.enemyX + 2, (int)this.enemyY + 4, 4, 3);
            g2.fillRect((int)this.enemyX + 10, (int)this.enemyY + 4, 4, 3);
            g2.setColor(Color.BLACK);
            g2.fillRect((int)this.enemyX + 3, (int)this.enemyY + 5, 2, 2);
            g2.fillRect((int)this.enemyX + 11, (int)this.enemyY + 5, 2, 2);
            g2.setColor(new Color(139, 0, 0));
            g2.drawLine((int)this.enemyX + 1, (int)this.enemyY + 3, (int)this.enemyX + 6, (int)this.enemyY + 5);
            g2.drawLine((int)this.enemyX + 14, (int)this.enemyY + 3, (int)this.enemyX + 9, (int)this.enemyY + 5);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval((int)this.playerX + 2, (int)this.playerY + 14, 12, 4);
            AffineTransform oldTx = g2.getTransform();
            g2.translate(this.playerX + 8.0, this.playerY + 16.0);
            g2.scale(this.scaleX, this.scaleY);
            g2.setColor(this.spacePressed && this.stamina > 20.0 ? Color.CYAN : new Color(50, 205, 50));
            g2.fillRoundRect(-8, -16, 16, 16, 4, 4);
            int n = this.rightPressed ? 2 : (eyeOffX = this.leftPressed ? -2 : 0);
            int eyeOffY = this.downPressed ? 2 : (this.upPressed ? -2 : 0);
            g2.setColor(Color.WHITE);
            g2.fillRect(-6 + eyeOffX, -12 + eyeOffY, 4, 6);
            g2.fillRect(2 + eyeOffX, -12 + eyeOffY, 4, 6);
            g2.setColor(Color.BLACK);
            g2.fillRect(-5 + eyeOffX + (this.rightPressed ? 1 : (this.leftPressed ? -1 : 0)), -10 + eyeOffY + (this.downPressed ? 1 : (this.upPressed ? -1 : 0)), 2, 2);
            g2.fillRect(3 + eyeOffX + (this.rightPressed ? 1 : (this.leftPressed ? -1 : 0)), -10 + eyeOffY + (this.downPressed ? 1 : (this.upPressed ? -1 : 0)), 2, 2);
            g2.setTransform(oldTx);
        }
        for (Particle p : this.particles) {
            g2.setColor(p.color);
            g2.fillRect((int)p.x, (int)p.y, p.size, p.size);
        }
        for (FloatingText ft : this.floatingTexts) {
            g2.setFont(this.gameFont);
            g2.setColor(new Color(0, 0, 0, ft.alpha));
            g2.drawString(ft.text, (int)ft.x + 1, (int)ft.y + 1);
            g2.setColor(new Color(ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(), ft.alpha));
            g2.drawString(ft.text, (int)ft.x, (int)ft.y);
        }
        g2.translate(this.camX, this.camY);
        g2.setFont(this.gameFont);
        RadialGradientPaint vignette = new RadialGradientPaint(new Point2D.Float(160.0f, 120.0f), 266.66666f, new float[]{0.0f, 1.0f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, 320, 240);
        if (this.state == GameState.PLAYING || this.state == GameState.PAUSED) {
            int mapW = 80;
            int mapH = 60;
            int mapX = 320 - mapW - 5;
            int mapY = 5;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(mapX, mapY, mapW, mapH, 2, 2);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawRoundRect(mapX, mapY, mapW, mapH, 2, 2);
            g2.setColor(Color.YELLOW);
            g2.fillRect(mapX + this.coinX / 8, mapY + this.coinY / 8, 2, 2);
            g2.setColor(Color.RED);
            g2.fillRect(mapX + (int)this.enemyX / 8, mapY + (int)this.enemyY / 8, 2, 2);
            g2.setColor(Color.GREEN);
            g2.fillRect(mapX + (int)this.playerX / 8, mapY + (int)this.playerY / 8, 2, 2);
        }
        if (this.state == GameState.MENU) {
            int titleYOffset = (int)(Math.sin(this.uiTime * 2.0) * 4.0);
            g2.setFont(this.gameFont.deriveFont(1, 30.0f));
            String title = "PIXEL CHASER";
            int tX = 160 - g2.getFontMetrics().stringWidth(title) / 2;
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(title, tX + 2, 42 + titleYOffset);
            Point2D.Float start = new Point2D.Float(tX, 20 + titleYOffset);
            Point2D.Float end = new Point2D.Float(tX, 50 + titleYOffset);
            float[] dist = new float[]{0.0f, 1.0f};
            Color[] colors = new Color[]{new Color(255, 220, 50), new Color(255, 140, 0)};
            g2.setPaint(new LinearGradientPaint(start, end, dist, colors));
            g2.drawString(title, tX, 40 + titleYOffset);
            if (this.random.nextDouble() < 0.2) {
                this.particles.add(new Particle((double)this.random.nextInt(320) + this.camX, (double)this.random.nextInt(240) + this.camY, new Color(255, 255, 255, 100)));
            }
            g2.setFont(this.gameFont);
            String playText = this.gameInProgress ? this.getText("CONTINUE") : this.getText("PLAY");
            String[] menuTexts = new String[]{playText, this.getText("LANG"), this.getText("HELP"), this.getText("EXIT")};
            for (int i = 0; i < 4; ++i) {
                boolean isLangBtn = i == 1;
                this.drawStoneButton(g2, this.menuButtons[i], menuTexts[i], this.selectedMenuIndex == i, isLangBtn);
            }
            g2.setColor(Color.GRAY);
            g2.drawString("Beta 0.0.0.1", 5, 235);
        } else if (this.state == GameState.HOWTOPLAY) {
            g2.setColor(new Color(0, 0, 0, 220));
            g2.fillRect(0, 0, 320, 240);
            g2.setFont(this.gameFont.deriveFont(1, 18.0f));
            g2.setColor(new Color(255, 220, 50));
            g2.drawString(this.getText("HELP"), 160 - g2.getFontMetrics().stringWidth(this.getText("HELP")) / 2, 35);
            g2.setFont(this.gameFont.deriveFont(0, 12.0f));
            g2.setColor(Color.WHITE);
            g2.drawString("- " + this.getText("HELP_1"), 30, 70);
            g2.drawString("- " + this.getText("HELP_2"), 30, 95);
            g2.drawString("- " + this.getText("HELP_3"), 30, 120);
            g2.drawString("- " + this.getText("HELP_4"), 30, 145);
            this.drawStoneButton(g2, this.backButton, this.getText("BACK"), this.backButton.contains(this.mouseX, this.mouseY), false);
        } else {
            g2.setColor(Color.BLACK);
            g2.drawString(this.getText("SCORE") + this.score, 6, 13);
            g2.drawString(this.getText("HIGH") + this.highScore, 6, 25);
            g2.setColor(Color.WHITE);
            g2.drawString(this.getText("SCORE") + this.score, 5, 12);
            g2.setColor(Color.YELLOW);
            g2.drawString(this.getText("HIGH") + this.highScore, 5, 24);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(136, 6, 50, 4);
            g2.setColor(this.stamina > 20.0 ? Color.CYAN : Color.RED);
            g2.fillRect(135, 5, (int)(this.stamina / 2.0), 4);
            g2.setColor(Color.WHITE);
            g2.drawRect(135, 5, 50, 4);
            if (this.state == GameState.GAMEOVER) {
                this.drawOverlay(g2, this.getText("GAMEOVER"), this.getText("RESTART"));
            } else if (this.state == GameState.PAUSED) {
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, 320, 240);
                int panelW = 200;
                int panelH = 140;
                int panelX = 160 - panelW / 2;
                int panelY = 120 - panelH / 2;
                g2.setColor(new Color(30, 30, 35, 240));
                g2.fillRoundRect(panelX, panelY, panelW, panelH, 4, 4);
                g2.setColor(new Color(20, 20, 25));
                g2.drawRoundRect(panelX, panelY, panelW, panelH, 4, 4);
                g2.setFont(this.gameFont.deriveFont(1, 18.0f));
                String pTitle = this.getText("PAUSED");
                g2.setColor(Color.BLACK);
                g2.drawString(pTitle, 160 - g2.getFontMetrics().stringWidth(pTitle) / 2 + 2, panelY + 27);
                g2.setColor(new Color(255, 220, 50));
                g2.drawString(pTitle, 160 - g2.getFontMetrics().stringWidth(pTitle) / 2, panelY + 25);
                g2.setFont(this.gameFont);
                String[] pTexts = new String[]{this.getText("RESUME"), this.getText("MAIN_MENU"), this.getText("EXIT")};
                for (int i = 0; i < 3; ++i) {
                    this.drawStoneButton(g2, this.pauseButtons[i], pTexts[i], this.selectedPauseIndex == i, false);
                }
            }
        }
        g2.dispose();
    }

    private void drawStoneButton(Graphics2D g2, Rectangle btn, String text, boolean isHovered, boolean isLangBtn) {
        Color base = isHovered ? new Color(150, 140, 130) : new Color(130, 120, 110);
        Color shadow = new Color(70, 60, 50);
        Color light = new Color(180, 170, 160);
        g2.setColor(new Color(10, 10, 10));
        g2.fillRoundRect(btn.x - 2, btn.y - 2, btn.width + 4, btn.height + 4, 2, 2);
        g2.setColor(shadow);
        g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 2, 2);
        int pressOffset = isHovered ? 2 : 0;
        g2.setColor(base);
        g2.fillRoundRect(btn.x, btn.y + pressOffset, btn.width, btn.height - 4, 2, 2);
        g2.setColor(light);
        g2.drawLine(btn.x + 1, btn.y + pressOffset + 1, btn.x + btn.width - 2, btn.y + pressOffset + 1);
        Random r = new Random((long)btn.x * 100L + (long)btn.y);
        g2.setColor(new Color(90, 80, 70, 180));
        for (int i = 0; i < 15; ++i) {
            int tx = btn.x + 2 + r.nextInt(btn.width - 4);
            int ty = btn.y + pressOffset + 2 + r.nextInt(btn.height - 8);
            g2.fillRect(tx, ty, r.nextInt(3) + 1, 1);
        }
        int strW = g2.getFontMetrics().stringWidth(text);
        int tX = btn.x + (btn.width - strW) / 2;
        int tY = btn.y + 18 + pressOffset;
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(text, tX + 1, tY + 1);
        g2.setColor(isHovered ? new Color(255, 250, 240) : Color.WHITE);
        g2.drawString(text, tX, tY);
        if (isLangBtn && isHovered) {
            g2.setColor(new Color(255, 220, 50));
            int anim = (int)(Math.sin(this.uiTime * 15.0) * 2.0);
            int[] lx = new int[]{btn.x - 14 + anim, btn.x - 6 + anim, btn.x - 6 + anim};
            int[] ly = new int[]{btn.y + btn.height / 2, btn.y + 6, btn.y + btn.height - 6};
            g2.fillPolygon(lx, ly, 3);
            int[] rx = new int[]{btn.x + btn.width + 14 - anim, btn.x + btn.width + 6 - anim, btn.x + btn.width + 6 - anim};
            int[] ry = new int[]{btn.y + btn.height / 2, btn.y + 6, btn.y + btn.height - 6};
            g2.fillPolygon(rx, ry, 3);
            g2.setColor(Color.BLACK);
            g2.drawPolygon(lx, ly, 3);
            g2.drawPolygon(rx, ry, 3);
        }
    }

    private void drawOverlay(Graphics2D g2, String title, String sub) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, 320, 240);
        g2.setFont(this.gameFont.deriveFont(1, 18.0f));
        int tX = 160 - g2.getFontMetrics().stringWidth(title) / 2;
        int tY = 110;
        g2.setColor(Color.BLACK);
        g2.drawString(title, tX + 2, tY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, tX, tY);
        g2.setFont(this.gameFont);
        int sX = 160 - g2.getFontMetrics().stringWidth(sub) / 2;
        int sY = 135;
        g2.setColor(Color.BLACK);
        g2.drawString(sub, sX + 1, sY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(sub, sX, sY);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int oldIndex = this.selectedMenuIndex;
        int oldPause = this.selectedPauseIndex;
        this.mouseX = e.getX() / 3;
        this.mouseY = e.getY() / 3;
        if (this.state == GameState.MENU) {
            for (int i = 0; i < 4; ++i) {
                if (!this.menuButtons[i].contains(this.mouseX, this.mouseY)) continue;
                this.selectedMenuIndex = i;
            }
            if (oldIndex != this.selectedMenuIndex) {
                this.playSound(this.sfxClickClip);
            }
        } else if (this.state == GameState.PAUSED) {
            for (int i = 0; i < 3; ++i) {
                if (!this.pauseButtons[i].contains(this.mouseX, this.mouseY)) continue;
                this.selectedPauseIndex = i;
            }
            if (oldPause != this.selectedPauseIndex) {
                this.playSound(this.sfxClickClip);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX() / 3;
        int my = e.getY() / 3;
        if (this.state == GameState.MENU) {
            for (int i = 0; i < 4; ++i) {
                if (!this.menuButtons[i].contains(mx, my)) continue;
                this.executeMenuAction(i);
            }
        } else if (this.state == GameState.PAUSED) {
            for (int i = 0; i < 3; ++i) {
                if (!this.pauseButtons[i].contains(mx, my)) continue;
                this.executePauseAction(i);
            }
        } else if (this.state == GameState.HOWTOPLAY && this.backButton.contains(mx, my)) {
            this.playSound(this.sfxClickClip);
            this.state = GameState.MENU;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (this.state == GameState.MENU) {
            if (code == 38 || code == 87) {
                this.playSound(this.sfxClickClip);
                --this.selectedMenuIndex;
                if (this.selectedMenuIndex < 0) {
                    this.selectedMenuIndex = 3;
                }
            }
            if (code == 40 || code == 83) {
                this.playSound(this.sfxClickClip);
                ++this.selectedMenuIndex;
                if (this.selectedMenuIndex > 3) {
                    this.selectedMenuIndex = 0;
                }
            }
            if (code == 10) {
                this.executeMenuAction(this.selectedMenuIndex);
            }
        } else if (this.state == GameState.PAUSED) {
            if (code == 38 || code == 87) {
                this.playSound(this.sfxClickClip);
                --this.selectedPauseIndex;
                if (this.selectedPauseIndex < 0) {
                    this.selectedPauseIndex = 2;
                }
            }
            if (code == 40 || code == 83) {
                this.playSound(this.sfxClickClip);
                ++this.selectedPauseIndex;
                if (this.selectedPauseIndex > 2) {
                    this.selectedPauseIndex = 0;
                }
            }
            if (code == 10) {
                this.executePauseAction(this.selectedPauseIndex);
            }
            if (code == 27) {
                this.playSound(this.sfxClickClip);
                this.state = GameState.PLAYING;
            }
        } else if (this.state == GameState.HOWTOPLAY) {
            if (code == 27 || code == 10) {
                this.playSound(this.sfxClickClip);
                this.state = GameState.MENU;
            }
        } else {
            if (code == 27 && this.state == GameState.PLAYING) {
                this.playSound(this.sfxClickClip);
                this.state = GameState.PAUSED;
                this.selectedPauseIndex = 0;
            }
            if (this.state == GameState.PLAYING) {
                if (!(this.upPressed || code != 87 && code != 38)) {
                    this.upPressed = true;
                    this.scaleX = 0.7;
                    this.scaleY = 1.3;
                }
                if (!(this.downPressed || code != 83 && code != 40)) {
                    this.downPressed = true;
                    this.scaleX = 0.7;
                    this.scaleY = 1.3;
                }
                if (!(this.leftPressed || code != 65 && code != 37)) {
                    this.leftPressed = true;
                    this.scaleX = 1.3;
                    this.scaleY = 0.7;
                }
                if (!(this.rightPressed || code != 68 && code != 39)) {
                    this.rightPressed = true;
                    this.scaleX = 1.3;
                    this.scaleY = 0.7;
                }
                if (code == 32) {
                    this.spacePressed = true;
                }
            } else if (code == 10 && this.state == GameState.GAMEOVER) {
                this.playSound(this.sfxClickClip);
                this.resetPositions();
                this.state = GameState.PLAYING;
                this.gameInProgress = true;
            }
        }
    }

    private void executeMenuAction(int index) {
        this.playSound(this.sfxClickClip);
        if (index == 0) {
            if (!this.gameInProgress) {
                this.resetPositions();
            }
            this.state = GameState.PLAYING;
            this.gameInProgress = true;
        } else if (index == 1) {
            this.currentLang = this.currentLang == Lang.VI ? Lang.EN : (this.currentLang == Lang.EN ? Lang.ZH : Lang.VI);
        } else if (index == 2) {
            this.state = GameState.HOWTOPLAY;
        } else if (index == 3) {
            GameExitNotifier.notifyGameClosed(() -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (this.hostWindow != null) this.hostWindow.dispose();
                });
            });
        }
    }

    private void executePauseAction(int index) {
        this.playSound(this.sfxClickClip);
        if (index == 0) {
            this.state = GameState.PLAYING;
        } else if (index == 1) {
            this.state = GameState.MENU;
            this.selectedMenuIndex = 0;
        } else if (index == 2) {
            GameExitNotifier.notifyGameClosed(() -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (this.hostWindow != null) this.hostWindow.dispose();
                });
            });
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == 87 || code == 38) {
            this.upPressed = false;
        }
        if (code == 83 || code == 40) {
            this.downPressed = false;
        }
        if (code == 65 || code == 37) {
            this.leftPressed = false;
        }
        if (code == 68 || code == 39) {
            this.rightPressed = false;
        }
        if (code == 32) {
            this.spacePressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame window = new JFrame("Pixel Chaser");
        try {
            java.io.InputStream is = JavaApplication1.class.getResourceAsStream("/icon.png");
            if (is != null) {
                window.setIconImage(javax.imageio.ImageIO.read(is));
            }
        } catch (Exception ignored) {}
        window.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new java.awt.event.WindowAdapter() {
    @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        GameExitNotifier.notifyGameClosed(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> window.dispose());
        });
    }
});
        window.setResizable(false);
        JavaApplication1 game = new JavaApplication1();
        game.setHostWindow(window);
        window.add(game);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        game.start();
    }

    class Particle {
        double x;
        double y;
        double dx;
        double dy;
        double life;
        double maxLife;
        int size;
        Color color;

        public Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            double angle = JavaApplication1.this.random.nextDouble() * Math.PI * 2.0;
            double speed = JavaApplication1.this.random.nextDouble() * 100.0 + 50.0;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
            this.life = this.maxLife = JavaApplication1.this.random.nextDouble() * 0.4 + 0.2;
            this.size = JavaApplication1.this.random.nextInt(3) + 1;
        }

        public void update(double dt) {
            this.x += this.dx * dt;
            this.y += this.dy * dt;
            this.life -= dt;
            int alpha = Math.max(0, (int)(255.0 * (this.life / this.maxLife)));
            this.color = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha);
        }

        public boolean isAlive() {
            return this.life > 0.0;
        }
    }

    class FloatingText {
        double x;
        double y;
        String text;
        Color color;
        double life;
        double maxLife;
        int alpha = 255;

        public FloatingText(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = this.maxLife = 1.0;
        }

        public void update(double dt) {
            this.y -= 20.0 * dt;
            this.life -= dt;
            this.alpha = Math.max(0, (int)(255.0 * (this.life / this.maxLife)));
        }

        public boolean isAlive() {
            return this.life > 0.0;
        }
    }

    static enum Lang {
        VI,
        EN,
        ZH;

    }

    static enum GameState {
        MENU,
        HOWTOPLAY,
        PLAYING,
        PAUSED,
        GAMEOVER;

    }
}

