package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;
import util.Assets;
import util.FixedStep;

public class GamePanel3 extends JPanel implements KeyListener, MouseListener, ActionListener {

    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;
    private static final int MAGAZINE_SIZE = 10;
    private static final int SHOT_DELAY = 18;              // jeda antar tembakan (frame)
    private static final int RELOAD_FRAMES = 75;           // lama mengisi ulang (frame)
    private static final int STAGE_CLEAR_HEAL = 25;
    private static final int CONTACT_KNOCKBACK = 70;

    private final Timer gameTimer;
    private final FixedStep clock = new FixedStep();
    private final Player3 player;
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy3> enemies = new ArrayList<>();
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final JButton restartButton;

    private Image background;
    private int backgroundWidth;

    int stage = 1;
    int cameraX = 0;
    private int frame = 0;

    // Teks transisi antar tahap
    private boolean showTransitionText = false;
    private String transitionText = "";
    private float transitionAlpha = 0.0f;
    private int transitionFrames = 0;
    private int transitionLength = 0;
    private int nextStage = 0;      // tahap yang dimunculkan setelah transisi; 0 = tidak ada

    // Input dan peluru
    private boolean left;
    private boolean right;
    private boolean isShooting = false;
    private int shotCooldown = 0;
    private int ammo = MAGAZINE_SIZE;
    private int reloadTimer = 0;
    private int hitFlash = 0;

    Flag flag;
    boolean gameFinished = false;
    boolean showVictoryPopup = false;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    ArrayList<Cloud> clouds = new ArrayList<>();
    Image cloudImage;

    private int flagReachedFrames = 0;
    private int finishedFrames = 0;
    private float fadeOutAlpha = 0.0f;
    private boolean startFadeOut = false;
    private boolean winTriggered = false;

    private Runnable onWin;

    public GamePanel3() {
        setFocusable(true);
        setLayout(null);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));

        background = Assets.image("asset/background/background3.png", SCREEN_WIDTH, SCREEN_HEIGHT);
        backgroundWidth = background.getWidth(null);
        if (backgroundWidth <= 0) {
            backgroundWidth = SCREEN_WIDTH;
        }
        cloudImage = Assets.image("asset/background/cloud.png");
        clouds.add(new Cloud(500, 0, 1, cloudImage, 150, 100));
        clouds.add(new Cloud(200, 0, 2, cloudImage, 150, 100));
        clouds.add(new Cloud(100, 0, 1, cloudImage, 150, 100));

        player = new Player3(0, 430);
        SoundPlayer.getInstance().playBackgroundMusic("asset/sounds/backsound.wav");

        addKeyListener(this);
        addMouseListener(this);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                releaseInput();
            }
        });

        restartButton = new JButton("RESTART");
        restartButton.setBounds(SCREEN_WIDTH / 2 - 75, 485, 150, 40);
        restartButton.setFont(new Font("Arial", Font.BOLD, 18));
        restartButton.setBackground(new Color(210, 180, 140));
        restartButton.setForeground(Color.BLACK);
        restartButton.setFocusPainted(false);
        restartButton.setOpaque(true);
        restartButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        restartButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                restartButton.setBackground(new Color(222, 184, 135));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                restartButton.setBackground(new Color(210, 180, 140));
            }
        });
        restartButton.addActionListener(e -> {
            soundPlayer.playSound("asset/sounds/click.wav");
            resetGame();
            requestFocusInWindow();
        });
        restartButton.setVisible(false);
        add(restartButton);

        spawnEnemiesForStage(stage);
        gameTimer = new Timer(8, this);
        clock.reset();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!gameTimer.isRunning() && !winTriggered) {
            clock.reset();
            gameTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        gameTimer.stop();
        releaseInput();
        player.setDirection(0);
        stopEnemySounds();
    }

    private void releaseInput() {
        left = false;
        right = false;
        isShooting = false;
    }

    private void stopEnemySounds() {
        for (Enemy3 enemy : enemies) {
            enemy.stopWalkSound();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int steps = clock.consume();
        for (int i = 0; i < steps && gameTimer.isRunning(); i++) {
            updateGameLogic();
        }
        if (steps > 0) {
            repaint();
        }
    }

    // ===================== PEMBARUAN PERMAINAN =====================

    public void updateGameLogic() {
        frame++;
        updateEffects();
        if (hitFlash > 0) {
            hitFlash--;
        }

        if (!player.isAlive) {
            if (!restartButton.isVisible()) {
                restartButton.setVisible(true);
                stopEnemySounds();
            }
            return;
        }

        updateTransition();
        updatePlayer();
        updateShooting();
        updateEnemies();
        updateEnemyBullets();
        updateFlag();
        updateVictory();

        for (Cloud cloud : clouds) {
            cloud.update();
        }
    }

    private void updatePlayer() {
        if (!gameFinished) {
            player.setDirection((right ? 1 : 0) - (left ? 1 : 0));
        }
        player.update();
        cameraX = Math.max(0, Math.min(player.x - 400, 100));
    }

    private void updateShooting() {
        if (shotCooldown > 0) {
            shotCooldown--;
        }
        if (reloadTimer > 0) {
            if (--reloadTimer == 0) {
                ammo = MAGAZINE_SIZE;
            }
        } else if (isShooting && !gameFinished && shotCooldown == 0 && ammo > 0) {
            bullets.add(new Bullet(player.x + 125, player.y + 64, 15));
            soundPlayer.playSound("asset/sounds/shootplayer.wav");
            effects.add(new Effect(player.x + 135, player.y + 72, 16, new Color(255, 230, 140), 8));
            shotCooldown = SHOT_DELAY;
            ammo--;
            if (ammo == 0) {
                startReload();
            }
        }

        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update();
            // Peluru yang sudah keluar layar dihapus
            if (!b.isActive() || b.x - cameraX > SCREEN_WIDTH) {
                it.remove();
            }
        }
    }

    private void startReload() {
        if (reloadTimer == 0 && ammo < MAGAZINE_SIZE) {
            reloadTimer = RELOAD_FRAMES;
        }
    }

    private void updateEnemies() {
        Iterator<Enemy3> enemyIterator = enemies.iterator();
        boolean cleared = false;
        while (enemyIterator.hasNext()) {
            Enemy3 enemy = enemyIterator.next();
            enemy.update(enemyBullets);

            for (Bullet b : bullets) {
                if (b.active && enemy.isHit(b)) {
                    enemy.hit();
                    b.active = false;
                    effects.add(new Effect(b.x + 10, b.y + 10, 22, new Color(255, 200, 90), 12));
                    if (!enemy.alive) {
                        break;
                    }
                }
            }
            bullets.removeIf(b -> !b.active);

            if (enemy.alive && !enemy.isEntering()
                    && enemy.getContactBox().intersects(player.getHitbox())) {
                if (player.hurt(enemy.getContactDamage())) {
                    onPlayerHurt(enemy.getContactDamage());
                    player.x = Math.max(0, player.x - CONTACT_KNOCKBACK);
                }
            }

            if (!enemy.alive) {
                effects.add(new Effect(enemy.x + 90, enemy.y + 100, enemy.isBoss() ? 120 : 70, new Color(255, 140, 40), 26));
                enemyIterator.remove();
                cleared = enemies.isEmpty();
            }
        }
        if (cleared) {
            onStageCleared();
        }
    }

    private void onStageCleared() {
        enemyBullets.clear();
        switch (stage) {
            case 1 -> {
                healAfterStage();
                startStageTransition("MUSUH BERIKUTNYA LEBIH KUAT", 2, 150);
            }
            case 2 -> {
                healAfterStage();
                startStageTransition("BOS AKAN DATANG!", 3, 110);
            }
            case 3 -> {
                startStageTransition("AMBIL BENDERA UNTUK MENANG!", 0, 110);
                flag = new Flag(1290, 630);
                flag.isWaving = true;
            }
            default -> {
            }
        }
    }

    private void healAfterStage() {
        int before = player.health;
        player.heal(STAGE_CLEAR_HEAL);
        if (player.health > before) {
            effects.add(Effect.text("+" + (player.health - before) + " NYAWA", player.x + 70, player.y,
                    new Color(120, 255, 140)));
        }
    }

    private void updateEnemyBullets() {
        Iterator<EnemyBullet> iterator = enemyBullets.iterator();
        while (iterator.hasNext()) {
            EnemyBullet bullet = iterator.next();
            bullet.update();
            if (!bullet.active) {
                iterator.remove();
                continue;
            }
            if (bullet.getBounds().intersects(player.getHitbox())) {
                if (player.hurt(bullet.damage)) {
                    onPlayerHurt(bullet.damage);
                    bullet.active = false;
                    iterator.remove();
                }
            }
        }
    }

    private void onPlayerHurt(int damage) {
        hitFlash = 14;
        effects.add(Effect.text("-" + damage, player.x + 70, player.y, new Color(255, 100, 100)));
    }

    private void updateFlag() {
        if (flag != null && !gameFinished) {
            if (!flag.isTaken && player.x + 100 >= flag.x) {
                if (++flagReachedFrames >= 1) {
                    flag.isTaken = true;
                    flag.raising = true;
                    player.stop();
                    left = false;
                    right = false;
                }
            }

            if (flag.raising) {
                flag.raiseY += 2;
                if (flag.raiseY >= 50) {
                    flag.raising = false;
                    showVictoryPopup = true;
                    gameFinished = true;
                    finishedFrames = 0;
                }
            }
        }
    }

    private void updateVictory() {
        if (gameFinished) {
            if (++finishedFrames >= 120) {
                startFadeOut = true;
            }
        }
        if (startFadeOut && fadeOutAlpha < 1.0f) {
            fadeOutAlpha += 0.02f;
            if (fadeOutAlpha >= 1.0f) {
                fadeOutAlpha = 1.0f;
                if (!winTriggered) {
                    winTriggered = true;
                    gameTimer.stop();
                    showCutscene();
                }
            }
        }
    }

    // ===================== TRANSISI TAHAP =====================

    public void startStageTransition(String message, int stageAfter, int length) {
        transitionText = message;
        showTransitionText = true;
        transitionAlpha = 0.0f;
        transitionFrames = length;
        transitionLength = length;
        nextStage = stageAfter;
    }

    private void updateTransition() {
        if (transitionFrames <= 0) {
            return;
        }
        transitionFrames--;
        int elapsed = transitionLength - transitionFrames;
        int textSpan = Math.min(transitionLength, 100);
        if (elapsed >= textSpan) {
            showTransitionText = false;
        } else {
            transitionAlpha = Math.min(1f, Math.min(elapsed, textSpan - elapsed) / 20f);
        }
        if (transitionFrames == 0) {
            showTransitionText = false;
            if (nextStage != 0) {
                stage = nextStage;
                spawnEnemiesForStage(stage);
                nextStage = 0;
            }
        }
    }

    public void spawnEnemiesForStage(int stage) {
        enemies.clear();
        enemyBullets.clear();

        // Enemy3(posisi awal, tembakan untuk dikalahkan, kecepatan jalan,
        //        jeda tembak min/maks (frame), kecepatan peluru, kerusakan peluru, bos?)
        switch (stage) {
            case 1 -> {
                Enemy3 a = new Enemy3(1400, 7, 1.6, 150, 230, 5, 8, false);
                a.setMovementBounds(900, 1200);
                Enemy3 b = new Enemy3(1560, 7, 1.6, 170, 250, 5, 8, false);
                b.setMovementBounds(560, 900);
                enemies.add(a);
                enemies.add(b);
            }
            case 2 -> {
                Enemy3 a = new Enemy3(1400, 10, 2.2, 100, 160, 6, 8, false);
                a.setMovementBounds(880, 1200);
                Enemy3 b = new Enemy3(1560, 10, 2.2, 120, 180, 6, 8, false);
                b.setMovementBounds(520, 900);
                enemies.add(a);
                enemies.add(b);
            }
            case 3 -> {
                Enemy3 boss = new Enemy3(1400, 40, 2.4, 70, 120, 6, 10, true);
                boss.setMovementBounds(700, 1200);
                enemies.add(boss);
            }
            default -> {
            }
        }
    }

    public void setOnWin(Runnable onWin) {
        this.onWin = onWin;
    }

    public void showCutscene() {
        if (onWin != null) {
            onWin.run();
        }
    }

    // ===================== PENGGAMBARAN =====================

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int x = -(cameraX % backgroundWidth) - backgroundWidth; x < getWidth(); x += backgroundWidth) {
            g2d.drawImage(background, x, 0, null);
        }

        for (Cloud c : clouds) {
            c.draw(g2d, cameraX);
        }
        for (Enemy3 e : enemies) {
            e.draw(g2d, cameraX);
        }
        player.draw(g2d, cameraX);
        for (Bullet b : bullets) {
            b.draw(g2d, cameraX);
        }
        for (EnemyBullet eb : enemyBullets) {
            eb.draw(g2d, cameraX);
        }
        if (flag != null) {
            flag.draw(g2d, cameraX);
        }
        for (Effect effect : effects) {
            effect.draw(g2d, cameraX);
        }

        drawHud(g2d);

        if (hitFlash > 0 && player.isAlive) {
            g2d.setColor(new Color(220, 30, 30, Math.min(100, hitFlash * 8)));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        if (showTransitionText) {
            g2d.setFont(new Font("Arial", Font.BOLD, 35));
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(transitionText);
            int x = (getWidth() - textWidth) / 2;
            int y = 380;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, transitionAlpha))));
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(transitionText, x + 3, y + 3);
            g2d.setColor(Color.WHITE);
            g2d.drawString(transitionText, x, y);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        if (showVictoryPopup) {
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            String victory = "KEMENANGAN TELAH BERHASIL";
            int textWidth = g2d.getFontMetrics().stringWidth(victory);
            int x = (getWidth() - textWidth) / 2;
            int y = 300;
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.drawString(victory, x + 4, y + 4);
            g2d.setColor(Color.WHITE);
            g2d.drawString(victory, x, y);
        }

        if (startFadeOut) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fadeOutAlpha)));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        g2d.dispose();
    }

    private void drawHud(Graphics2D g2d) {
        if (!player.isAlive) {
            return;
        }
        // Nyawa pemain
        drawBar(g2d, 20, 20, 260, 20, player.health / (double) Player3.MAX_HEALTH,
                player.health > 30 ? new Color(80, 220, 90) : new Color(230, 70, 60),
                "NYAWA  " + player.health + " / " + Player3.MAX_HEALTH);

        // Peluru dan isi ulang
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        if (reloadTimer > 0) {
            drawBar(g2d, 20, 48, 260, 14, 1.0 - reloadTimer / (double) RELOAD_FRAMES,
                    new Color(240, 200, 80), "MENGISI ULANG...");
        } else {
            String ammoText = "PELURU  " + ammo + " / " + MAGAZINE_SIZE + (ammo <= 3 ? "   (R: isi ulang)" : "");
            drawShadowText(g2d, ammoText, 22, 62, ammo <= 3 ? new Color(255, 150, 120) : Color.WHITE);
            for (int i = 0; i < MAGAZINE_SIZE; i++) {
                g2d.setColor(i < ammo ? new Color(255, 215, 90) : new Color(70, 70, 70, 190));
                g2d.fillRoundRect(22 + i * 14, 68, 10, 14, 4, 4);
            }
        }

        // Tahap
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String stageText = "TAHAP " + Math.min(stage, 3) + " / 3";
        int sw = g2d.getFontMetrics().stringWidth(stageText);
        drawShadowText(g2d, stageText, SCREEN_WIDTH - sw - 24, 36, Color.WHITE);

        // Nyawa bos
        for (Enemy3 e : enemies) {
            if (e.isBoss() && e.alive && !e.isEntering()) {
                int w = 520;
                drawBar(g2d, (SCREEN_WIDTH - w) / 2, 24, w, 22, e.healthRatio(), new Color(210, 50, 50), "BOS PENJAJAH");
            }
        }

        if (frame < 360 && stage == 1) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String hint = "Gerak: A D / panah   |   Lompat: W / panah atas   |   Tembak: Spasi / klik   |   R: isi ulang";
            int hw = g2d.getFontMetrics().stringWidth(hint);
            drawShadowText(g2d, hint, (SCREEN_WIDTH - hw) / 2, SCREEN_HEIGHT - 20, Color.WHITE);
        }
    }

    private void drawBar(Graphics2D g2d, int x, int y, int w, int h, double ratio, Color fill, String label) {
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRoundRect(x - 3, y - 3, w + 6, h + 6, 10, 10);
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(fill);
        g2d.fillRoundRect(x, y, (int) (w * Math.max(0, Math.min(1, ratio))), h, 8, 8);
        g2d.setFont(new Font("Arial", Font.BOLD, Math.max(11, h - 6)));
        int tw = g2d.getFontMetrics().stringWidth(label);
        drawShadowText(g2d, label, x + (w - tw) / 2, y + h - 4, Color.WHITE);
    }

    private void drawShadowText(Graphics2D g2d, String text, int x, int y, Color color) {
        g2d.setColor(new Color(0, 0, 0, 190));
        g2d.drawString(text, x + 1, y + 1);
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    // ===================== INPUT =====================

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (!player.isAlive) {
            if (code == KeyEvent.VK_R) {
                resetGame();
            }
            return;
        }
        if (gameFinished) {
            return;
        }

        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A ->
                left = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D ->
                right = true;
            case KeyEvent.VK_UP, KeyEvent.VK_W ->
                player.jump();
            case KeyEvent.VK_SPACE ->
                isShooting = true;
            case KeyEvent.VK_R ->
                startReload();
            default -> {
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A ->
                left = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D ->
                right = false;
            case KeyEvent.VK_UP, KeyEvent.VK_W ->
                player.releaseJump();
            case KeyEvent.VK_SPACE ->
                isShooting = false;
            default -> {
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        if (!gameFinished && SwingUtilities.isLeftMouseButton(e)) {
            isShooting = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            isShooting = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public void resetGame() {
        stopEnemySounds();
        player.reset();
        enemies.clear();
        enemyBullets.clear();
        bullets.clear();
        effects.clear();
        stage = 1;
        cameraX = 0;
        gameFinished = false;
        flag = null;
        showTransitionText = false;
        transitionFrames = 0;
        nextStage = 0;
        showVictoryPopup = false;
        left = false;
        right = false;
        isShooting = false;
        shotCooldown = 0;
        ammo = MAGAZINE_SIZE;
        reloadTimer = 0;
        hitFlash = 0;
        spawnEnemiesForStage(stage);

        fadeOutAlpha = 0.0f;
        startFadeOut = false;
        winTriggered = false;
        flagReachedFrames = 0;
        finishedFrames = 0;
        restartButton.setVisible(false);
        clock.reset();
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
    }

    // ===================== OBJEK PENDUKUNG =====================

    /** Lingkaran ledakan singkat atau teks melayang untuk umpan balik. */
    private static class Effect {

        final int x;
        int y;
        final int size;
        final Color color;
        final int life;
        final String text;
        int age = 0;

        Effect(int x, int y, int size, Color color, int life) {
            this(x, y, size, color, life, null);
        }

        private Effect(int x, int y, int size, Color color, int life, String text) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.life = life;
            this.text = text;
        }

        static Effect text(String text, int x, int y, Color color) {
            return new Effect(x, y, 0, color, 50, text);
        }

        void draw(Graphics2D g2d, int cameraX) {
            float t = age / (float) life;
            int alpha = Math.max(0, (int) (230 * (1f - t)));
            Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            int sx = x - cameraX;
            if (text != null) {
                g2d.setFont(new Font("Arial", Font.BOLD, 22));
                int w = g2d.getFontMetrics().stringWidth(text);
                g2d.setColor(new Color(0, 0, 0, alpha));
                g2d.drawString(text, sx - w / 2 + 1, y - age + 1);
                g2d.setColor(c);
                g2d.drawString(text, sx - w / 2, y - age);
                return;
            }
            int radius = (int) (size * (0.3f + t));
            g2d.setColor(c);
            g2d.fillOval(sx - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private void updateEffects() {
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect effect = it.next();
            if (++effect.age >= effect.life) {
                it.remove();
            }
        }
    }

    public class Cloud {

        int x, y, speed, width, height;
        Image image;

        public Cloud(int x, int y, int speed, Image image, int width, int height) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.image = image;
            this.width = width;
            this.height = height;
        }

        public void update() {
            x -= speed;
            if (x + width < 0) {
                x = 1280 + (int) (Math.random() * 300);
            }
        }

        public void draw(Graphics g, int cameraX) {
            g.drawImage(image, x - cameraX, y, width, height, null);
        }
    }

    public class Flag {

        private int x;
        private final int y;
        int height = 150, raiseY = 0;
        boolean isWaving = false, isTaken = false, raising = false;
        private double waveOffset = 0;
        private final double waveSpeed = 0.15;
        private final int waveAmplitude = 5;

        public Flag(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics g, int cameraX) {
            int screenX = x - cameraX;
            int screenY = y - height - raiseY;
            g.setColor(Color.DARK_GRAY);
            g.fillRect(screenX, screenY, 5, height);
            if (isWaving || raising) {
                int flagWidth = 60, flagHeight = 40;
                waveOffset += waveSpeed;
                if (waveOffset > 2 * Math.PI) {
                    waveOffset -= 2 * Math.PI;
                }
                for (int i = 0; i < flagWidth; i += 10) {
                    int waveY = (int) (Math.sin(waveOffset + i * 0.5) * waveAmplitude);
                    g.setColor(Color.RED);
                    g.fillRect(screenX + 5 + i, screenY, 10, flagHeight / 2 + waveY);
                    g.setColor(Color.WHITE);
                    g.fillRect(screenX + 5 + i, screenY + flagHeight / 2 + waveY, 10, flagHeight / 2 - waveY);
                }
            }
        }
    }
}
