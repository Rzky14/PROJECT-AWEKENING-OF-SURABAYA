package minigame2;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import util.Assets;
import util.FixedStep;

public class GamePanel2 extends JPanel implements ActionListener, KeyListener {

    private enum State {
        PLAYING, WINNING, WON, LOST
    }

    // Konstanta permainan
    private static final int PLAYER_SPEED = 8;
    private static final int PLAYER_MAX_HEALTH = 5;
    private static final int INVULNERABLE_FRAMES = 75;      // kebal singkat setelah terkena tembakan
    private static final int BULLET_INTERVAL = 12;          // jeda antar tembakan pemain (frame)
    private static final int BULLET_SPEED = 12;
    private static final int WIN_KILL_COUNT = 8;
    private static final int ENEMY_HEALTH = 3;
    private static final int ENEMY_VOLLEY_PAUSE = 150;      // jeda antar rentetan tembakan musuh
    private static final int HITBOX_INSET = 10;             // hitbox pemain lebih kecil dari gambarnya
    private static final String SOUND_CLICK = "asset/sounds/click.wav";

    // Tingkat kesulitan per gelombang (gelombang naik tiap 3 musuh dikalahkan)
    private static final int[] SPAWN_INTERVAL = {110, 90, 75};
    private static final double[] ENEMY_SPEED = {1.6, 2.0, 2.4};
    private static final int[] MAX_ENEMIES = {2, 3, 3};
    private static final int[] SHOTS_PER_VOLLEY = {2, 3, 3};
    private static final int[] SHOT_GAP = {55, 45, 40};
    private static final double[] BULLET_SPEED_ENEMY = {4.0, 4.5, 5.0};
    private static final double[] DRIFT = {0.0, 1.0, 1.5};
    private static final boolean[] AIMED = {false, false, true};

    private final BufferedImage background;
    private final BufferedImage playerImage;
    private final BufferedImage enemyImage;
    private final BufferedImage bulletImage;

    private final int screenWidth;
    private final int screenHeight;
    private int leftBoundary;
    private int rightBoundary;
    private int enemyLeftBoundary;
    private int enemyRightBoundary;
    private final int paddingLeft = 70;
    private final int paddingRight = 70;

    // Status
    private State state = State.PLAYING;
    private int playerX;
    private int playerY;
    private int playerHealth = PLAYER_MAX_HEALTH;
    private int killCount = 0;
    private int invulnerable = 0;
    private int hitFlash = 0;
    private int frame = 0;
    private int lastStage = 0;
    private int stageBannerTimer = 0;

    // Input
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private boolean mouseShooting;
    private boolean keyShooting;
    private int shootCooldown = 0;
    private long lastShootSound = 0;

    // Daftar objek
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final List<Burst> bursts = new ArrayList<>();
    private int spawnCounter = 0;

    // Overlay dan tombol
    private int overlayAlpha = 0;
    private final JButton nextButton;
    private final JButton restartButton;
    private Runnable onWin;

    private final Timer timer;
    private final FixedStep clock = new FixedStep();
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    public GamePanel2() {
        soundPlayer.preloadSound(SOUND_CLICK);

        background = Assets.buffered("asset/background/background2.png", 1280, 720);
        playerImage = Assets.buffered("asset/player.png", 60, 90);
        enemyImage = Assets.buffered("asset/enemy.png", 60, 90);
        bulletImage = Assets.buffered("asset/bullet.png", 30, 32);
        screenWidth = background.getWidth();
        screenHeight = background.getHeight();

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setLayout(null);
        setFocusable(true);
        addKeyListener(this);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                releaseInput();
            }
        });

        computeHorizontalBounds();
        centerPlayer();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    requestFocusInWindow();
                    mouseShooting = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseShooting = false;
                }
            }
        });

        restartButton = createButton("RESTART", screenWidth / 2 - 75, screenHeight / 2 + 60, 150, 44);
        restartButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            resetGame();
            requestFocusInWindow();
        });
        add(restartButton);

        nextButton = createButton("NEXT", screenWidth / 2 - 80, screenHeight / 2 + 60, 160, 44);
        nextButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            Timer delay = new Timer(150, evt -> {
                if (onWin != null) {
                    onWin.run();
                }
            });
            delay.setRepeats(false);
            delay.start();
        });
        add(nextButton);

        timer = new Timer(8, this);
        clock.reset();
        timer.start();
    }

    private JButton createButton(String text, int x, int y, int w, int h) {
        final Color normal = new Color(210, 180, 140);     // Tan
        final Color hover = new Color(222, 184, 135);      // Burlywood
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(normal);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });
        button.setVisible(false);
        return button;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (timer != null && !timer.isRunning()) {
            clock.reset();
            timer.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (timer != null) {
            timer.stop();
        }
        releaseInput();
    }

    private void computeHorizontalBounds() {
        int imgW = background.getWidth();
        int imgH = background.getHeight();
        int midY = imgH / 2;
        int waterRGB = background.getRGB(0, midY);

        int l = 0;
        for (int x = 0; x < imgW; x++) {
            if (background.getRGB(x, midY) != waterRGB) {
                l = x;
                break;
            }
        }
        int r = imgW - 1;
        for (int x = imgW - 1; x >= 0; x--) {
            if (background.getRGB(x, midY) != waterRGB) {
                r = x;
                break;
            }
        }

        leftBoundary = l + paddingLeft;
        rightBoundary = r - paddingRight - playerImage.getWidth();
        if (rightBoundary - leftBoundary < 200) {
            // Tepi tidak terdeteksi dengan wajar: gunakan seluruh lebar layar
            leftBoundary = paddingLeft;
            rightBoundary = imgW - paddingRight - playerImage.getWidth();
        }

        enemyLeftBoundary = leftBoundary;
        enemyRightBoundary = rightBoundary + playerImage.getWidth() - enemyImage.getWidth();
    }

    private void centerPlayer() {
        playerX = screenWidth / 2 - playerImage.getWidth() / 2;
        playerY = screenHeight - playerImage.getHeight() - 60;
    }

    private void releaseInput() {
        left = false;
        right = false;
        up = false;
        down = false;
        mouseShooting = false;
        keyShooting = false;
    }

    // ===================== PEMBARUAN PERMAINAN =====================

    @Override
    public void actionPerformed(ActionEvent e) {
        int steps = clock.consume();
        for (int i = 0; i < steps; i++) {
            update();
        }
        if (steps > 0) {
            repaint();
        }
    }

    private int stage() {
        return Math.min(SPAWN_INTERVAL.length - 1, killCount / 3);
    }

    private void update() {
        frame++;
        if (invulnerable > 0) {
            invulnerable--;
        }
        if (hitFlash > 0) {
            hitFlash--;
        }
        if (stageBannerTimer > 0) {
            stageBannerTimer--;
        }
        updateBursts();

        switch (state) {
            case PLAYING -> {
                updatePlayer();
                updateShooting();
                updatePlayerBullets();
                updateEnemies();
                updateEnemyBullets();
                checkStageChange();
            }
            case WINNING ->
                updateWinning();
            case WON, LOST -> {
                overlayAlpha = Math.min(210, overlayAlpha + 4);
                if (overlayAlpha >= 150) {
                    (state == State.WON ? nextButton : restartButton).setVisible(true);
                }
            }
        }
    }

    private void updatePlayer() {
        int dx = (right ? 1 : 0) - (left ? 1 : 0);
        int dy = (down ? 1 : 0) - (up ? 1 : 0);
        double scale = (dx != 0 && dy != 0) ? 0.7071 : 1.0;
        playerX = Math.max(leftBoundary, Math.min(playerX + (int) Math.round(dx * PLAYER_SPEED * scale), rightBoundary));
        playerY = Math.max(screenHeight / 2, Math.min(playerY + (int) Math.round(dy * PLAYER_SPEED * scale),
                screenHeight - playerImage.getHeight()));
    }

    private void updateShooting() {
        if (shootCooldown > 0) {
            shootCooldown--;
        }
        if ((mouseShooting || keyShooting) && shootCooldown == 0) {
            shootCooldown = BULLET_INTERVAL;
            bullets.add(new Bullet(
                    playerX + playerImage.getWidth() / 2 - bulletImage.getWidth() / 2,
                    playerY - bulletImage.getHeight() / 2));
            long now = System.currentTimeMillis();
            if (now - lastShootSound > 180) {
                soundPlayer.playSound("asset/sounds/shoot.wav");
                lastShootSound = now;
            }
        }
    }

    private void updatePlayerBullets() {
        int enemyW = enemyImage.getWidth();
        int enemyH = enemyImage.getHeight();
        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            b.y -= BULLET_SPEED;
            boolean hit = false;
            for (Iterator<Enemy> eit = enemies.iterator(); eit.hasNext();) {
                Enemy en = eit.next();
                if (b.x < en.x + enemyW && b.x + bulletImage.getWidth() > en.x
                        && b.y < en.y + enemyH && b.y + bulletImage.getHeight() > en.y) {
                    en.health--;
                    en.flash = 6;
                    hit = true;
                    bursts.add(new Burst(b.x + bulletImage.getWidth() / 2f, b.y, 14, new Color(255, 220, 120)));
                    if (en.health <= 0) {
                        eit.remove();
                        killCount++;
                        bursts.add(new Burst((float) (en.x + enemyW / 2f), (float) (en.y + enemyH / 2f), 46, new Color(255, 140, 40)));
                        soundPlayer.playSound("asset/sounds/enemy_death.wav");
                    } else {
                        soundPlayer.playSound("asset/sounds/shootenemy.wav");
                    }
                    break;
                }
            }
            if (hit || b.y + bulletImage.getHeight() < 0) {
                bit.remove();
            }
        }
        if (killCount >= WIN_KILL_COUNT) {
            startWinning();
        }
    }

    private void updateEnemies() {
        int s = stage();
        if (++spawnCounter >= SPAWN_INTERVAL[s] && enemies.size() < MAX_ENEMIES[s] && killCount < WIN_KILL_COUNT) {
            spawnCounter = 0;
            spawnEnemy(s);
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy en = it.next();
            en.y += en.speedY;
            en.x += en.speedX;
            if (en.x < enemyLeftBoundary || en.x > enemyRightBoundary) {
                en.speedX = -en.speedX;
                en.x = Math.max(enemyLeftBoundary, Math.min(en.x, enemyRightBoundary));
            }
            if (en.flash > 0) {
                en.flash--;
            }
            updateEnemyFire(en, s);

            if (overlapsPlayer((int) en.x, (int) en.y, enemyImage.getWidth(), enemyImage.getHeight(), 0)) {
                it.remove();
                bursts.add(new Burst((float) (en.x + enemyImage.getWidth() / 2f), (float) (en.y + enemyImage.getHeight() / 2f), 46,
                        new Color(255, 140, 40)));
                damagePlayer();
                continue;
            }
            if (en.y > screenHeight) {
                it.remove();
            }
        }
    }

    private void updateEnemyFire(Enemy en, int s) {
        // Musuh baru menembak setelah seluruh badannya masuk layar
        if (en.y < 10) {
            return;
        }
        if (en.volleyShots > 0) {
            if (--en.shotTimer <= 0) {
                fireEnemyBullet(en, s);
                en.volleyShots--;
                en.shotTimer = SHOT_GAP[s];
                if (en.volleyShots == 0) {
                    en.volleyPause = ENEMY_VOLLEY_PAUSE;
                }
            }
        } else if (--en.volleyPause <= 0) {
            en.volleyShots = SHOTS_PER_VOLLEY[s];
            en.shotTimer = 20; // jeda singkat sebagai tanda musuh akan menembak
            en.warning = 20;
        }
        if (en.warning > 0) {
            en.warning--;
        }
    }

    private void fireEnemyBullet(Enemy en, int s) {
        double speed = BULLET_SPEED_ENEMY[s];
        double startX = en.x + enemyImage.getWidth() / 2.0 - bulletImage.getWidth() / 2.0;
        double startY = en.y + enemyImage.getHeight() - 10;
        double dx = 0;
        double dy = speed;
        if (AIMED[s]) {
            double tx = playerX + playerImage.getWidth() / 2.0 - (startX + bulletImage.getWidth() / 2.0);
            double ty = playerY + playerImage.getHeight() / 2.0 - startY;
            double len = Math.hypot(tx, ty);
            if (len > 1 && ty > 0) {
                dx = tx / len * speed;
                dy = Math.max(speed * 0.6, ty / len * speed);
                dx = Math.max(-speed * 0.8, Math.min(speed * 0.8, dx));
            }
        }
        enemyBullets.add(new EnemyBullet(startX, startY, dx, dy));
        if (System.currentTimeMillis() - lastEnemyShotSound > 250) {
            lastEnemyShotSound = System.currentTimeMillis();
            soundPlayer.playSound("asset/sounds/shootplayer.wav");
        }
    }

    private long lastEnemyShotSound = 0;

    private void updateEnemyBullets() {
        Iterator<EnemyBullet> ebit = enemyBullets.iterator();
        while (ebit.hasNext()) {
            EnemyBullet eb = ebit.next();
            eb.x += eb.dx;
            eb.y += eb.dy;

            if (overlapsPlayer((int) eb.x + 4, (int) eb.y + 4, bulletImage.getWidth() - 8, bulletImage.getHeight() - 8,
                    HITBOX_INSET)) {
                ebit.remove();
                bursts.add(new Burst((float) eb.x + bulletImage.getWidth() / 2f, (float) eb.y + bulletImage.getHeight() / 2f,
                        18, new Color(255, 80, 80)));
                damagePlayer();
                if (state != State.PLAYING) {
                    return;
                }
            } else if (eb.y > screenHeight || eb.x < -bulletImage.getWidth() || eb.x > screenWidth) {
                ebit.remove();
            }
        }
    }

    /** Tabrakan dengan hitbox pemain yang sedikit lebih kecil dari gambarnya. */
    private boolean overlapsPlayer(int x, int y, int w, int h, int inset) {
        int px = playerX + inset;
        int py = playerY + inset;
        int pw = playerImage.getWidth() - 2 * inset;
        int ph = playerImage.getHeight() - 2 * inset;
        return x < px + pw && x + w > px && y < py + ph && y + h > py;
    }

    private void damagePlayer() {
        if (invulnerable > 0 || state != State.PLAYING) {
            return;
        }
        playerHealth--;
        invulnerable = INVULNERABLE_FRAMES;
        hitFlash = 24;
        soundPlayer.playSound("asset/sounds/hitHurt.wav");
        if (playerHealth <= 0) {
            playerHealth = 0;
            state = State.LOST;
            overlayAlpha = 0;
            enemyBullets.clear();
            releaseInput();
            bursts.add(new Burst(playerX + playerImage.getWidth() / 2f, playerY + playerImage.getHeight() / 2f, 60,
                    new Color(255, 120, 40)));
        }
    }

    private void checkStageChange() {
        int s = stage();
        if (s != lastStage) {
            lastStage = s;
            stageBannerTimer = 150;
        }
    }

    private void startWinning() {
        if (state != State.PLAYING) {
            return;
        }
        state = State.WINNING;
        for (Enemy en : enemies) {
            bursts.add(new Burst((float) (en.x + enemyImage.getWidth() / 2f), (float) (en.y + enemyImage.getHeight() / 2f), 40,
                    new Color(255, 140, 40)));
        }
        enemies.clear();
        enemyBullets.clear();
        bullets.clear();
        releaseInput();
        stageBannerTimer = 0;
    }

    /** Pemain terbang ke atas sebagai penutup misi; tidak bisa lagi terkena tembakan. */
    private void updateWinning() {
        playerY -= PLAYER_SPEED;
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext();) {
            Bullet b = it.next();
            b.y -= BULLET_SPEED;
            if (b.y < -bulletImage.getHeight()) {
                it.remove();
            }
        }
        if (playerY + playerImage.getHeight() < 0) {
            state = State.WON;
            overlayAlpha = 0;
        }
    }

    private void updateBursts() {
        Iterator<Burst> it = bursts.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            if (++b.age >= b.life) {
                it.remove();
            }
        }
    }

    private void spawnEnemy(int s) {
        int span = Math.max(1, enemyRightBoundary - enemyLeftBoundary);
        Enemy en = new Enemy(
                enemyLeftBoundary + (int) (Math.random() * span),
                -enemyImage.getHeight(),
                ENEMY_SPEED[s],
                DRIFT[s] == 0 ? 0 : (Math.random() < 0.5 ? -DRIFT[s] : DRIFT[s]));
        en.volleyPause = 25 + (int) (Math.random() * 50);
        enemies.add(en);
    }

    // ===================== API PUBLIK =====================

    public void setOnWin(Runnable onWin) {
        this.onWin = onWin;
    }

    private void resetGame() {
        state = State.PLAYING;
        overlayAlpha = 0;
        playerHealth = PLAYER_MAX_HEALTH;
        killCount = 0;
        invulnerable = 0;
        hitFlash = 0;
        lastStage = 0;
        stageBannerTimer = 0;
        spawnCounter = 0;
        shootCooldown = 0;
        releaseInput();
        enemies.clear();
        bullets.clear();
        enemyBullets.clear();
        bursts.clear();
        centerPlayer();
        restartButton.setVisible(false);
        nextButton.setVisible(false);
        clock.reset();
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    // ===================== PENGGAMBARAN =====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.drawImage(background, 0, 0, null);

        for (Enemy en : enemies) {
            drawEnemy(g2d, en);
        }
        for (Bullet b : bullets) {
            g2d.drawImage(bulletImage, b.x, b.y, null);
        }
        for (EnemyBullet eb : enemyBullets) {
            drawEnemyBullet(g2d, eb);
        }
        if (state != State.LOST && (invulnerable == 0 || (invulnerable / 5) % 2 == 0 || state == State.WINNING)) {
            g2d.drawImage(playerImage, playerX, playerY, null);
        }
        for (Burst b : bursts) {
            drawBurst(g2d, b);
        }

        drawHud(g2d);

        if (hitFlash > 0) {
            g2d.setColor(new Color(220, 30, 30, Math.min(110, hitFlash * 5)));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }

        if (state == State.WON) {
            drawOverlay(g2d, "MISI SELESAI, PERJUANGAN MASIH BERLANJUT!", null, 36);
        } else if (state == State.LOST) {
            drawOverlay(g2d, "TERKALAHKAN", "PEJUANG TELAH GUGUR", 44);
        }
        g2d.dispose();
    }

    private void drawEnemy(Graphics2D g2d, Enemy en) {
        int x = (int) en.x;
        int y = (int) en.y;
        if (en.warning > 0) {
            // Isyarat singkat sebelum musuh menembak
            g2d.setColor(new Color(255, 70, 70, 90 + (en.warning % 6) * 12));
            g2d.fillOval(x - 6, y - 6, enemyImage.getWidth() + 12, enemyImage.getHeight() + 12);
        }
        g2d.drawImage(enemyImage, x, y, null);
        if (en.flash > 0) {
            g2d.setColor(new Color(255, 255, 255, 120));
            g2d.fillRect(x, y, enemyImage.getWidth(), enemyImage.getHeight());
        }
        // Bilah nyawa musuh
        if (en.health < ENEMY_HEALTH) {
            int w = enemyImage.getWidth();
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRect(x, y - 8, w, 5);
            g2d.setColor(new Color(230, 70, 60));
            g2d.fillRect(x, y - 8, w * en.health / ENEMY_HEALTH, 5);
        }
    }

    private void drawEnemyBullet(Graphics2D g2d, EnemyBullet eb) {
        int x = (int) eb.x;
        int y = (int) eb.y;
        int w = bulletImage.getWidth();
        int h = bulletImage.getHeight();
        g2d.setColor(new Color(255, 40, 40, 110));
        g2d.fillOval(x - 4, y - 4, w + 8, h + 8);
        AffineTransform old = g2d.getTransform();
        g2d.rotate(Math.PI, x + w / 2.0, y + h / 2.0);
        g2d.drawImage(bulletImage, x, y, null);
        g2d.setTransform(old);
    }

    private void drawBurst(Graphics2D g2d, Burst b) {
        float t = b.age / (float) b.life;
        int radius = (int) (b.size * (0.3f + t));
        int alpha = (int) (200 * (1f - t));
        g2d.setColor(new Color(b.color.getRed(), b.color.getGreen(), b.color.getBlue(), Math.max(0, alpha)));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawOval((int) b.x - radius, (int) b.y - radius, radius * 2, radius * 2);
        g2d.fillOval((int) b.x - radius / 2, (int) b.y - radius / 2, radius, radius);
    }

    private void drawHud(Graphics2D g2d) {
        // Nyawa
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        drawShadowText(g2d, "NYAWA", 20, 28, Color.WHITE);
        for (int i = 0; i < PLAYER_MAX_HEALTH; i++) {
            drawHeart(g2d, 20 + i * 30, 36, 22, i < playerHealth);
        }

        // Progres misi
        int barW = 360;
        int barH = 18;
        int barX = (screenWidth - barW) / 2;
        int barY = 20;
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRoundRect(barX - 3, barY - 3, barW + 6, barH + 6, 10, 10);
        g2d.setColor(new Color(70, 70, 70));
        g2d.fillRoundRect(barX, barY, barW, barH, 8, 8);
        int filled = barW * Math.min(killCount, WIN_KILL_COUNT) / WIN_KILL_COUNT;
        g2d.setColor(new Color(230, 190, 70));
        g2d.fillRoundRect(barX, barY, filled, barH, 8, 8);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String progress = "MUSUH DIKALAHKAN " + Math.min(killCount, WIN_KILL_COUNT) + " / " + WIN_KILL_COUNT;
        int tw = g2d.getFontMetrics().stringWidth(progress);
        drawShadowText(g2d, progress, barX + (barW - tw) / 2, barY + 14, Color.WHITE);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String wave = "GELOMBANG " + (stage() + 1);
        int ww = g2d.getFontMetrics().stringWidth(wave);
        drawShadowText(g2d, wave, (screenWidth - ww) / 2, barY + barH + 24, new Color(255, 230, 150));

        if (stageBannerTimer > 0 && state == State.PLAYING) {
            int alpha = Math.min(255, stageBannerTimer * 4);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String banner = "GELOMBANG " + (stage() + 1);
            int bw = g2d.getFontMetrics().stringWidth(banner);
            g2d.setColor(new Color(0, 0, 0, alpha / 2));
            g2d.drawString(banner, (screenWidth - bw) / 2 + 2, screenHeight / 2 - 100 + 2);
            g2d.setColor(new Color(255, 230, 150, alpha));
            g2d.drawString(banner, (screenWidth - bw) / 2, screenHeight / 2 - 100);
        }

        if (state == State.PLAYING && frame < 360) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String hint = "Gerak: W A S D / panah   |   Tembak: klik kiri / Spasi";
            int hw = g2d.getFontMetrics().stringWidth(hint);
            drawShadowText(g2d, hint, (screenWidth - hw) / 2, screenHeight - 20, Color.WHITE);
        }
    }

    private void drawShadowText(Graphics2D g2d, String text, int x, int y, Color color) {
        g2d.setColor(new Color(0, 0, 0, 190));
        g2d.drawString(text, x + 1, y + 1);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    private void drawHeart(Graphics2D g2d, int x, int y, int s, boolean filled) {
        Path2D.Float heart = new Path2D.Float();
        heart.moveTo(x + s / 2f, y + s);
        heart.curveTo(x - s * 0.25f, y + s * 0.45f, x + s * 0.05f, y - s * 0.2f, x + s / 2f, y + s * 0.28f);
        heart.curveTo(x + s * 0.95f, y - s * 0.2f, x + s * 1.25f, y + s * 0.45f, x + s / 2f, y + s);
        heart.closePath();
        g2d.setColor(filled ? new Color(225, 40, 50) : new Color(60, 60, 60, 190));
        g2d.fill(heart);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(heart);
    }

    private void drawOverlay(Graphics2D g2d, String title, String subtitle, int titleSize) {
        g2d.setColor(new Color(0, 0, 0, overlayAlpha));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        int textAlpha = Math.min(255, overlayAlpha * 255 / 210);
        g2d.setColor(new Color(255, 255, 255, textAlpha));
        g2d.setFont(new Font("Arial", Font.BOLD, titleSize));
        int w = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (screenWidth - w) / 2, screenHeight / 2 - 30);
        if (subtitle != null) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            int w2 = g2d.getFontMetrics().stringWidth(subtitle);
            g2d.drawString(subtitle, (screenWidth - w2) / 2, screenHeight / 2 + 10);
        }
    }

    // ===================== INPUT =====================

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                right = true;
            case KeyEvent.VK_W, KeyEvent.VK_UP ->
                up = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                down = true;
            case KeyEvent.VK_SPACE ->
                keyShooting = true;
            case KeyEvent.VK_R -> {
                if (state == State.LOST) {
                    resetGame();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                right = false;
            case KeyEvent.VK_W, KeyEvent.VK_UP ->
                up = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                down = false;
            case KeyEvent.VK_SPACE ->
                keyShooting = false;
            default -> {
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ===================== OBJEK PERMAINAN =====================

    private static class Enemy {

        double x;
        double y;
        double speedY;
        double speedX;
        int health = ENEMY_HEALTH;
        int flash = 0;
        int warning = 0;
        int volleyShots = 0;
        int volleyPause = 0;
        int shotTimer = 0;

        Enemy(double x, double y, double speedY, double speedX) {
            this.x = x;
            this.y = y;
            this.speedY = speedY;
            this.speedX = speedX;
        }
    }

    private static class Bullet {

        int x;
        int y;

        Bullet(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class EnemyBullet {

        double x;
        double y;
        double dx;
        double dy;

        EnemyBullet(double x, double y, double dx, double dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static class Burst {

        final float x;
        final float y;
        final int size;
        final Color color;
        final int life = 22;
        int age = 0;

        Burst(float x, float y, int size, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
    }
}
