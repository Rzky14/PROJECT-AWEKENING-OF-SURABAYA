package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;

public class GamePanel3 extends JPanel implements KeyListener, MouseListener {

    private Thread gameThread;
    private volatile boolean runningGame = true;
    private Player3 player;
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Enemy3> enemies = new ArrayList<>();
    private ArrayList<EnemyBullet> enemyBullets = new ArrayList<>();

    Image background;

    int stage = 1;
    int cameraX = 0;

    boolean showTransitionText = false;
    String transitionText = "";
    int transitionTextTimer = 0;

    float transitionAlpha = 0.0f;
    boolean fadingIn = true;

    boolean isShooting = false;
    long lastShotTime = 0;
    long shotDelay = 500;

    Flag flag;
    boolean gameFinished = false;

    boolean exitRight = false;
    boolean showVictoryPopup = false;
    long exitStartTime = 0;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    ArrayList<Cloud> clouds = new ArrayList<>();
    Image cloudImage;

    private long flagReachedTime = 0;
    private long exitReachedTime = 0;

    private float fadeOutAlpha = 0.0f;
    private boolean startFadeOut = false;

    private Runnable onWin;

    public GamePanel3() {
        setFocusable(true);
        setPreferredSize(new Dimension(1280, 720));

        background = new ImageIcon("asset/background/background3.png").getImage();
        cloudImage = new ImageIcon("asset/background/cloud.png").getImage();
        clouds.add(new Cloud(500, 0, 1, cloudImage, 150, 100));
        clouds.add(new Cloud(200, 0, 2, cloudImage, 150, 100));
        clouds.add(new Cloud(100, 0, 1, cloudImage, 150, 100));

        player = new Player3(0, 430);
        SoundPlayer.getInstance().playBackgroundMusic("asset/sounds/backsound.wav");
    }

    @Override
    public void addNotify() {
        super.addNotify();
        addKeyListener(this);
        addMouseListener(this);
        spawnEnemiesForStage(stage);
        startGameLoop();
    }

    private void startGameLoop() {
        gameThread = new Thread(() -> {
            final int targetFPS = 60;
            final long frameDuration = 1000 / targetFPS;

            // MODIFIKASI: Loop berdasarkan 'runningGame'
            while (runningGame) {
                long start = System.currentTimeMillis();

                updateGameLogic();
                repaint();

                long duration = System.currentTimeMillis() - start;
                long sleepTime = frameDuration - duration;

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        System.err.println("Thread interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        runningGame = false;
                    }
                }
            }
            System.out.println("GamePanel3 game loop stopped.");
        });
        gameThread.start();
    }

    public void updateGameLogic() {
        if (!player.isAlive) {
            return;
        }

        player.update();
        if (player.x < 0) {
            player.x = 0;
        }
        cameraX = Math.max(0, Math.min(player.x - 400, 100));

        bullets.removeIf(b -> !b.isActive());
        for (Bullet b : bullets) {
            b.update();
        }

        // Logika menembak ini tidak perlu diubah, karena hanya bergantung pada 'isShooting'
        if (isShooting && System.currentTimeMillis() - lastShotTime >= shotDelay) {
            bullets.add(new Bullet(player.x + 125, player.y + 64, 15));
            soundPlayer.playSound("asset/sounds/shootplayer.wav");
            lastShotTime = System.currentTimeMillis();
        }

        Iterator<Enemy3> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy3 enemy = enemyIterator.next();
            enemy.update();

            for (EnemyBullet newBullet : enemy.getBullets()) {
                if (!enemyBullets.contains(newBullet)) {
                    enemyBullets.add(newBullet);
                }
            }

            for (Bullet b : bullets) {
                if (enemy.isHit(b)) {
                    enemy.hit();
                    b.active = false;
                }
            }

            if (!enemy.alive) {
                enemyIterator.remove();
                if (enemies.isEmpty()) {
                    switch (stage) {
                        case 1 ->
                            startStageTransition("MUSUH BERIKUTNYA LEBIH KUAT");
                        case 2 ->
                            startStageTransition("BOS AKAN DATANG!");
                        case 3 -> {
                            startStageTransition("AMBIL BENDERA UNTUK MENANG!");
                            flag = new Flag(1290, 630);
                            flag.isWaving = true;
                        }
                    }
                }
            }
        }

        enemyBullets.removeIf(b -> !b.active);
        Iterator<EnemyBullet> enemyBulletIterator = enemyBullets.iterator();
        while (enemyBulletIterator.hasNext()) {
            EnemyBullet bullet = enemyBulletIterator.next();
            bullet.update();

            if (bullet.x >= player.x && bullet.x <= player.x + 100
                    && bullet.y >= player.y && bullet.y <= player.y + 100) {
                player.hitByEnemyBullet();
                bullet.active = false;
            }
        }

        if (flag != null && !gameFinished) {
            if (!flag.isTaken && player.x + 50 >= flag.x) {
                if (flagReachedTime == 0) {
                    flagReachedTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - flagReachedTime >= 10) {
                    flag.isTaken = true;
                    flag.raising = true;
                    player.stop();
                }
            }

            if (flag.raising) {
                flag.raiseY += 2;
                if (flag.raiseY >= 50) {
                    flag.raising = false;
                    showVictoryPopup = true;
                    gameFinished = true;
                    exitReachedTime = System.currentTimeMillis();
                }
            }
        }

        if (gameFinished && exitReachedTime > 0) {
            long timeSinceFinish = System.currentTimeMillis() - exitReachedTime;
            if (timeSinceFinish >= 2000) {
                startFadeOut = true;
            }
        }

        if (startFadeOut && fadeOutAlpha < 1.0f) {
            fadeOutAlpha += 0.02f;
            if (fadeOutAlpha >= 1.0f) {
                fadeOutAlpha = 1.0f;
                runningGame = false;
                showCutscene();
                exitReachedTime = 0;
            }
        }

        for (Cloud cloud : clouds) {
            cloud.update();
        }
    }

    public void startStageTransition(String message) {
        transitionText = message;
        showTransitionText = true;
        transitionTextTimer = 90;
        transitionAlpha = 0.0f;
        fadingIn = true;

        Timer transitionTimer = new Timer(16, null);
        final int[] localTimer = {0};

        transitionTimer.addActionListener(e -> {
            if (fadingIn) {
                transitionAlpha += 0.02f;
                if (transitionAlpha >= 1.0f) {
                    transitionAlpha = 1.0f;
                    fadingIn = false;
                }
            } else {
                transitionTextTimer--;
                if (transitionTextTimer < 30) {
                    transitionAlpha -= 0.03f;
                    if (transitionAlpha <= 0.0f) {
                        transitionAlpha = 0.0f;
                    }
                }
            }

            localTimer[0]++;

            if (localTimer[0] >= 90) {
                transitionTimer.stop();
                switch (stage) {
                    case 1 -> {
                        stage = 2;
                        new Timer(1000, evt -> {
                            spawnEnemiesForStage(stage);
                            showTransitionText = false;
                            ((Timer) evt.getSource()).stop();
                        }).start();
                    }
                    case 2 -> {
                        stage = 3;
                        spawnEnemiesForStage(stage);
                        showTransitionText = false;
                    }
                    case 3 -> {
                        flag = new Flag(1290, 630);
                        flag.isWaving = true;
                        showTransitionText = false;
                    }
                }
            }
        });

        transitionTimer.start();
    }

    public void spawnEnemiesForStage(int stage) {
        enemies.clear();
        enemyBullets.clear();
        int y = 300;

        switch (stage) {
            case 1 -> {
                for (int i = 0; i < 2; i++) {
                    enemies.add(new Enemy3(1400 + i * 300, y, 10, 2, 60, 100, 120, false));
                }
            }
            case 2 -> {
                for (int i = 0; i < 2; i++) {
                    enemies.add(new Enemy3(1400 + i * 300, y, 14, 3, 30, 60, 100, false));
                }
            }
            case 3 -> {
                enemies.add(new Enemy3(1400, y, 50, 5, 20, 40, 90, true));
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother text and graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int bgWidth = background.getWidth(null);
        for (int x = -cameraX % bgWidth - bgWidth; x < getWidth(); x += bgWidth) {
            g2d.drawImage(background, x, 0, null);
        }

        for (Cloud c : clouds) {
            c.draw(g2d, cameraX);
        }
        player.draw(g2d, cameraX);
        for (Bullet b : bullets) {
            b.draw(g2d, cameraX);
        }
        for (Enemy3 e : enemies) {
            e.draw(g2d, cameraX);
        }
        for (EnemyBullet eb : enemyBullets) {
            eb.draw(g2d, cameraX);
        }

        if (flag != null) {
            flag.draw(g2d, cameraX);
        }

        // --- Drawing Transition Text with Shadow ---
        if (showTransitionText) {
            Font transitionFont = new Font("Arial", Font.BOLD, 35);
            g2d.setFont(transitionFont);
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(transitionText);
            int x = (getWidth() - textWidth) / 2;
            int y = 380; // Y-coordinate for the text

            // Apply alpha for fading effect
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitionAlpha));

            // Draw the black shadow (offset slightly)
            g2d.setColor(new Color(0, 0, 0, 150)); // Semi-transparent black for shadow
            int shadowOffset = 3; // Adjust shadow offset as needed
            g2d.drawString(transitionText, x + shadowOffset, y + shadowOffset);

            // Draw the white main text
            g2d.setColor(Color.WHITE);
            g2d.drawString(transitionText, x, y);

            // Reset composite
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // --- Drawing Victory Popup Text with Shadow ---
        if (showVictoryPopup) {
            Font victoryFont = new Font("Arial", Font.BOLD, 50);
            g2d.setFont(victoryFont);
            String victory = "KEMENANGAN TELAH BERHASIL";
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(victory);
            int x = (getWidth() - textWidth) / 2;
            int y = 300; // Y-coordinate for the text

            // Draw the black shadow (offset slightly)
            g2d.setColor(new Color(0, 0, 0, 200)); // Slightly more opaque black for victory shadow
            int shadowOffset = 4; // Adjust shadow offset as needed
            g2d.drawString(victory, x + shadowOffset, y + shadowOffset);

            // Draw the white main text
            g2d.setColor(Color.WHITE);
            g2d.drawString(victory, x, y);
        }

        // Drawing fade-out overlay
        if (startFadeOut) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeOutAlpha));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (!player.isAlive && code == KeyEvent.VK_R) {
            resetGame();
        }

        if (!gameFinished) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                player.setDirection(-1);
            }

            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                player.setDirection(1);
            }

            if ((code == KeyEvent.VK_UP || code == KeyEvent.VK_W)) {
                if (!player.jumping) {
                    player.jump();
                    soundPlayer.playSound("asset/sounds/jump.wav");
                }
            }

            if (code == KeyEvent.VK_SPACE) {
                isShooting = true;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (!gameFinished) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_A || code == KeyEvent.VK_D) {
                player.setDirection(0);
            }

            if (code == KeyEvent.VK_SPACE) {
                isShooting = false;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!gameFinished) {
            isShooting = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!gameFinished) {
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
        runningGame = true;
        player.reset();
        enemies.clear();
        enemyBullets.clear();
        stage = 1;
        cameraX = 0;
        gameFinished = false;
        flag = null;
        showTransitionText = false;
        showVictoryPopup = false;
        exitRight = false;
        spawnEnemiesForStage(stage);

        fadeOutAlpha = 0.0f;
        startFadeOut = false;
        flagReachedTime = 0;
        exitReachedTime = 0;
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
