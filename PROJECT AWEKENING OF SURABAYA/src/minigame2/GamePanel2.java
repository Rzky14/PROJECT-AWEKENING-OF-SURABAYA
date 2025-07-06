package minigame2;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GamePanel2 extends JPanel implements ActionListener, KeyListener {

    private BufferedImage background;
    private BufferedImage playerImage;
    private BufferedImage enemyImage;
    private BufferedImage bulletImage;

    private int playerX, playerY;
    private int velX, velY;
    private int leftBoundary, rightBoundary;
    private int enemyLeftBoundary, enemyRightBoundary;
    private int playerHealth = 5;
    private int killCount = 0;
    private boolean gameWon = false;
    private boolean missionComplete = false; // Untuk efek win overlay
    private boolean showGameOver = false;    // Untuk efek kalah overlay
    private static final String SOUND_CLICK = "asset/sounds/click.wav";

    // Overlay animasi
    private int notificationAlpha = 0;
    private int gameOverAlpha = 0;
    private Timer winEffectTimer;
    private Timer gameOverEffectTimer;
    private JButton nextButton;
    private JButton restartButton;

    private Runnable onWin; // Callback ke story selanjutnya

    // Konstanta
    private static final int SPEED = 10;
    private static final int ENEMY_SPEED = 2;
    private static final int SPAWN_INTERVAL = 120;          // enemy spawn frame (≈2 detik @60FPS)
    private static final int BULLET_INTERVAL = 15;          // bullet spawn (~4 per detik)
    private static final int BULLET_SPEED = 10;             // kecepatan pixel/frame
    private static final int ENEMY_BULLET_INTERVAL = 150;   // jeda frame antar tembakan musuh
    private static final int ENEMY_BULLET_SPEED = ENEMY_SPEED + 2;
    private static final int WIN_KILL_COUNT = 5;
    private int paddingLeft = 70;
    private int paddingRight = 70;

    private final Timer timer;

    // Daftar musuh dan peluru
    private final List<Enemy> enemies = new ArrayList<>();
    private int spawnCounter = 0;

    private final List<Bullet> bullets = new ArrayList<>();
    private int bulletCounter = BULLET_INTERVAL;
    private boolean isShooting = false;

    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private int enemyBulletsCounter = 0;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

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

        // Tambahkan padding ke batas kiri dan kanan
        leftBoundary = l + paddingLeft;
        rightBoundary = r - paddingRight - playerImage.getWidth();

        // Hitung batas kiri dan kanan untuk enemy spawn, setelah gambar berhasil dimuat
        enemyLeftBoundary = leftBoundary;
        enemyRightBoundary = rightBoundary - enemyImage.getWidth();

    }

    public GamePanel2() {
        // REMOVED: soundPlayer.fadeOutBackgroundMusic(1000);
        soundPlayer.preloadSound(SOUND_CLICK);
        try {
            background = ImageIO.read(new File("asset/background/background2.png"));
            playerImage = ImageIO.read(new File("asset/player.png"));
            enemyImage = ImageIO.read(new File("asset/enemy.png"));
            bulletImage = ImageIO.read(new File("asset/bullet.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        computeHorizontalBounds();
        centerPlayer();

        // << Tambahkan baris ini >>
        enemyLeftBoundary = leftBoundary;
        enemyRightBoundary = rightBoundary - enemyImage.getWidth();

        setFocusable(true);
        addKeyListener(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (!isShooting) {
                        isShooting = true;
                        soundPlayer.playSound("asset/sounds/shoot.wav"); // HANYA saat awal tekan
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isShooting = false;
                }
            }
        });

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    private void centerPlayer() {
        playerX = background.getWidth() / 2 - playerImage.getWidth() / 2;
        playerY = background.getHeight() / 2 - playerImage.getHeight() / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g.drawImage(background, 0, 0, null); // Asalkan background = ukuran panel
        g.drawImage(playerImage, playerX, playerY, this);

        for (Bullet b : bullets) {
            g.drawImage(bulletImage, b.x, b.y, this);
        }
        for (EnemyBullet eb : enemyBullets) {
            g.drawImage(bulletImage, eb.x, eb.y, this);
        }
        for (Enemy en : enemies) {
            g.drawImage(enemyImage, en.x, en.y, this);
        }

        // UI: health & kill
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        g.drawString("health: " + playerHealth, 10, 20);
        g.drawString("Kills: " + killCount + "/" + WIN_KILL_COUNT, 10, 40);

        // Efek mission complete (win)
        if (missionComplete) {
            g2d.setColor(new Color(0, 0, 0, notificationAlpha));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(new Color(255, 255, 255, notificationAlpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String message = "MISI SELESAI, PERJUANGAN MASIH BERLANJUT!";
            int strWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (getWidth() - strWidth) / 2, getHeight() / 2);
        }

        // Efek game over
        if (showGameOver) {
            g2d.setColor(new Color(0, 0, 0, gameOverAlpha));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(new Color(255, 255, 255, gameOverAlpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 44));
            String gameOverMsg = "TERKALAHKAN";
            int w = g2d.getFontMetrics().stringWidth(gameOverMsg);
            g2d.drawString(gameOverMsg, (getWidth() - w) / 2, getHeight() / 2 - 30);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.setColor(new Color(255, 255, 255, gameOverAlpha));
            String subMsg = "PEJUANG TELAH GUGUR";
            int w2 = g2d.getFontMetrics().stringWidth(subMsg);
            g2d.drawString(subMsg, (getWidth() - w2) / 2, getHeight() / 2 + 10);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (missionComplete || showGameOver) {
            repaint();
            return;
        }

        playerX = Math.max(leftBoundary, Math.min(playerX + velX, rightBoundary));
        if (!gameWon) {
            playerY = Math.max(getHeight() / 2, Math.min(playerY + velY, getHeight() - playerImage.getHeight()));
        } else {
            velX = 0;
            velY = -SPEED;
            playerY += velY;
        }

        if (isShooting) {
            bulletCounter++;
            if (bulletCounter >= BULLET_INTERVAL) {
                bulletCounter = 0;
                spawnBullet();
            }
        } else {
            bulletCounter = BULLET_INTERVAL;
        }

        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            b.y -= BULLET_SPEED;
            boolean hit = false;
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy en = eit.next();
                if (b.x < en.x + enemyImage.getWidth() && b.x + bulletImage.getWidth() > en.x
                        && b.y < en.y + enemyImage.getHeight() && b.y + bulletImage.getHeight() > en.y) {
                    en.health--;
                    bit.remove();
                    hit = true;
                    if (en.health <= 0) {
                        eit.remove();
                        killCount++;
                        win();
                    }
                    break;
                }
            }
            if (!hit && b.y + bulletImage.getHeight() < 0) {
                bit.remove();
            }
        }

        if (++enemyBulletsCounter >= ENEMY_BULLET_INTERVAL) {
            enemyBulletsCounter = 0;
            for (Enemy en : enemies) {
                en.inShootingSequence = true;
                en.shotsFired = 0;
                en.shootDelayCounter = 0;
            }
        }

        // Untuk enemy yang sedang dalam mode menembak berturut
        for (Enemy en : enemies) {
            if (en.inShootingSequence) {
                en.shootDelayCounter++;
                if (en.shootDelayCounter >= 60) { // 1 detik @ 60fps
                    spawnEnemyBullet(en);
                    en.shotsFired++;
                    en.shootDelayCounter = 0;

                    if (en.shotsFired >= 3) {
                        en.inShootingSequence = false;
                    }
                }
            }
        }

        Iterator<EnemyBullet> ebit = enemyBullets.iterator();
        while (ebit.hasNext()) {
            EnemyBullet eb = ebit.next();
            eb.x += eb.dx;
            eb.y += eb.dy;

            if (eb.collidesWith(playerX, playerY, playerImage)) {
                playerHealth--;
                ebit.remove();
                checkGameOver();
            } else if (eb.y > getHeight()) {
                ebit.remove();
            }
        }

        if (++spawnCounter >= SPAWN_INTERVAL) {
            spawnCounter = 0;
            spawnEnemy();
        }
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy en = it.next();
            en.y += en.speed;
            if (en.y > getHeight()) {
                it.remove();
            }
        }

        // when exit complete
        if (gameWon && playerY + playerImage.getHeight() < 0 && !missionComplete) {
            timer.stop();
            startMissionCompleteOverlay();
        }

        repaint();
    }

    // Win overlay logic
    private void startMissionCompleteOverlay() {
        missionComplete = true;
        notificationAlpha = 0;
        if (winEffectTimer != null) {
            winEffectTimer.stop();
        }
        winEffectTimer = new Timer(10, e -> {
            notificationAlpha += 6;
            if (notificationAlpha >= 220) {
                notificationAlpha = 220;
                ((Timer) e.getSource()).stop();
                showNextButton();
            }
            if (nextButton != null) {
                nextButton.setBounds(getWidth() / 2 - 80, getHeight() / 2 + 50, 160, 44);
            }
            repaint();
        });
        winEffectTimer.start();
    }

    private void showNextButton() {
        if (nextButton == null) {
            nextButton = new JButton("NEXT");
            nextButton.setFont(new Font("Arial", Font.BOLD, 20));
            nextButton.setBackground(new Color(210, 180, 140)); // Coklat muda (tan)
            nextButton.setForeground(Color.BLACK);
            nextButton.setFocusPainted(false);
            nextButton.setOpaque(true);

            // Border coklat tua
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2)); // Border: hitam

            // Efek hover: lebih terang saat kursor masuk
            nextButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    nextButton.setBackground(new Color(222, 184, 135)); // Burlywood
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    nextButton.setBackground(new Color(210, 180, 140)); // Tan
                }
            });

            nextButton.setBounds(getWidth() / 2 - 80, getHeight() / 2 + 50, 160, 44);

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

            setLayout(null);
            add(nextButton);
        }
        nextButton.setVisible(true);
    }

    // Setter untuk callback Story setelah win
    public void setOnWin(Runnable onWin) {
        this.onWin = onWin;
    }

    private void win() {
        if (!gameWon && killCount >= WIN_KILL_COUNT) {
            gameWon = true;
        }
    }

    // Game over overlay logic & restart
    private void checkGameOver() {
        if (playerHealth <= 0 && !showGameOver) {
            timer.stop();
            showGameOver = true;
            gameOverAlpha = 0;
            showRestartButton();
            if (gameOverEffectTimer != null) {
                gameOverEffectTimer.stop();
            }
            gameOverEffectTimer = new Timer(10, e -> {
                gameOverAlpha += 6;
                if (gameOverAlpha >= 220) {
                    gameOverAlpha = 220;
                    ((Timer) e.getSource()).stop();
                    if (restartButton != null) {
                        restartButton.setVisible(true);
                    }
                }
                if (restartButton != null) {
                    restartButton.setBounds(getWidth() / 2 - 80, getHeight() / 2 + 40, 160, 50);
                }
                repaint();
            });
            gameOverEffectTimer.start();
        }
    }

    private void showRestartButton() {
        if (restartButton == null) {
            restartButton = new JButton("RESTART");
            restartButton.setFont(new Font("Arial", Font.BOLD, 18));
            restartButton.setBackground(new Color(210, 180, 140)); // Coklat muda (tan)
            restartButton.setForeground(Color.BLACK); // Teks hitam
            restartButton.setFocusPainted(false);
            restartButton.setOpaque(true);
            restartButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2)); // Border hitam
            restartButton.setBounds(getWidth() / 2 - 75, getHeight() / 2 + 40, 150, 40);

            // Efek hover
            restartButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    restartButton.setBackground(new Color(222, 184, 135)); // Burlywood
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    restartButton.setBackground(new Color(210, 180, 140)); // Tan
                }
            });

            restartButton.addActionListener(e -> {
                soundPlayer.playSound(SOUND_CLICK);
                Timer delay = new Timer(150, evt -> resetGame());
                delay.setRepeats(false);
                delay.start();
            });

            setLayout(null);
            add(restartButton);
            restartButton.setVisible(false);
        }
    }

    private void resetGame() {
        // reset state
        showGameOver = false;
        gameOverAlpha = 0;
        missionComplete = false;
        notificationAlpha = 0;
        gameWon = false;
        playerHealth = 5;
        killCount = 0;
        velX = 0;
        velY = 0;
        spawnCounter = 0;
        bulletCounter = BULLET_INTERVAL;
        enemyBulletsCounter = 0;
        enemies.clear();
        bullets.clear();
        enemyBullets.clear();
        centerPlayer();

        // remove overlays and buttons
        if (restartButton != null) {
            remove(restartButton);
            restartButton = null;
        }
        if (nextButton != null) {
            remove(nextButton);
            nextButton = null;
        }

        timer.start();
        repaint();
    }

    // Spawner logic
    private void spawnBullet() {
        bullets.add(new Bullet(
                playerX + playerImage.getWidth() / 2 - bulletImage.getWidth() / 2,
                playerY
        ));
    }

    private void spawnEnemyBullet(Enemy en) {
        int baseX = en.x + enemyImage.getWidth() / 2 - bulletImage.getWidth() / 2;
        int baseY = en.y + enemyImage.getHeight();
        enemyBullets.add(new EnemyBullet(baseX, baseY, 0, ENEMY_BULLET_SPEED));
    }

    private void spawnEnemy() {
        int span = enemyRightBoundary - enemyLeftBoundary;
        Enemy en = new Enemy(
                enemyLeftBoundary + (int) (Math.random() * span + 1),
                -enemyImage.getHeight(),
                ENEMY_SPEED
        );
        en.inShootingSequence = true;
        en.shotsFired = 0;
        en.shootDelayCounter = 0;
        enemies.add(en);
    }

    //input handling
    @Override
    public void keyPressed(KeyEvent e) {
        if (missionComplete || showGameOver) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A ->
                velX = -SPEED;
            case KeyEvent.VK_D ->
                velX = SPEED;
            case KeyEvent.VK_W ->
                velY = -SPEED;
            case KeyEvent.VK_S ->
                velY = SPEED;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (missionComplete || showGameOver) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A, KeyEvent.VK_D ->
                velX = 0;
            case KeyEvent.VK_W, KeyEvent.VK_S ->
                velY = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Inner class: Enemy, Bullet, EnemyBullet (tidak berubah)
    private static class Enemy {

        int x, y, speed, health;
        boolean inShootingSequence = false;
        int shotsFired = 0;
        int shootDelayCounter = 0;

        Enemy(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.health = 3;
        }
    }

    private class Bullet {

        int x, y;

        Bullet(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // boolean collidesWith(Enemy en) {
        //     return x < en.x + enemyImage.getWidth()
        //             && x + bulletImage.getWidth() > en.x
        //             && y < en.y + enemyImage.getHeight()
        //             && y + bulletImage.getHeight() > en.y;
        // }
        // boolean collidesWith(int px, int py, BufferedImage img) {
        //     return x < px + img.getWidth()
        //             && x + bulletImage.getWidth() > px
        //             && y < py + img.getHeight()
        //             && y + bulletImage.getHeight() > py;
        // }
    }

    private class EnemyBullet {

        int x, y;
        int dx, dy;

        EnemyBullet(int x, int y, int dx, int dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        boolean collidesWith(int px, int py, BufferedImage img) {
            return x < px + img.getWidth()
                    && x + bulletImage.getWidth() > px
                    && y < py + img.getHeight()
                    && y + bulletImage.getHeight() > py;
        }
    }

}
