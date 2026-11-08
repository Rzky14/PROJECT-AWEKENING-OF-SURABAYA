package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import util.Assets;

public class Player3 {

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    int x, y;
    int dx;
    int speed = 6;
    int health = 100;
    boolean isAlive = true;

    private boolean isHaltedByGame = false;

    private static final int PLAYER_WIDTH = 140;
    private static final int PLAYER_HEIGHT = 160;
    public static final int MAX_HEALTH = 100;
    public static final int MAX_X = 1240;                 // batas kanan dunia (tepat di sisi layar saat kamera di ujung)
    private static final int INVULNERABLE_FRAMES = 50;     // kebal singkat setelah terkena serangan
    private static final int JUMP_BUFFER_FRAMES = 8;       // lompat yang ditekan sesaat sebelum mendarat tetap dihitung

    private static final Image idleImage = Assets.image("asset/player/player1.png", PLAYER_WIDTH, PLAYER_HEIGHT);
    private static final Image jumpImage = Assets.image("asset/player/playerloncat.png", PLAYER_WIDTH, PLAYER_HEIGHT);
    private static final Image image1 = idleImage;
    private static final Image image2 = Assets.image("asset/player/player2.png", PLAYER_WIDTH, PLAYER_HEIGHT);

    private static final Image[] walkRightImages = new Image[]{
        image2, image1, image2, image1, image2, image1
    };
    private static final Image[] walkLeftImages = new Image[]{
        image1, image2, image1, image2, image1, image2
    };

    Image currentImage = idleImage;

    int animationFrame = 0;
    int animationCounter = 0;
    int animationSpeed = 8;

    boolean jumping = false;
    private double positionY;
    private double jumpVelocity = 0;
    private int jumpBuffer = 0;
    private int invulnerable = 0;
    final double gravity = 0.9;
    final double jumpStrength = 19;
    final int groundY = 470;
    final int topLimit = 15;
    final int bottomLimit = groundY;

    boolean walkSoundPlaying = false;

    private long lastHitSoundTime = 0;
    private final long hitSoundCooldown = 300;

    private long lastWalkSoundTime = 0;
    private final long walkSoundCooldown = 250;

    public Player3(int startX, int startY) {
        x = startX;
        y = startY + 40;
        positionY = y;
    }

    /** Area tubuh yang bisa terkena peluru atau tabrakan, lebih kecil dari gambar karena ada ruang kosong. */
    public Rectangle getHitbox() {
        return new Rectangle(x + 35, y + 25, 70, 120);
    }

    public boolean isInvulnerable() {
        return invulnerable > 0;
    }

    public void update() {
        if (!isAlive) {
            stopWalkSoundIfPlaying();
            return;
        }

        if (invulnerable > 0) {
            invulnerable--;
        }
        if (isHaltedByGame) {
            dx = 0;
        }

        x += dx;
        if (x < 0) {
            x = 0;
        }
        if (x > MAX_X) {
            x = MAX_X;
        }

        if (jumping) {
            positionY -= jumpVelocity;
            jumpVelocity -= gravity;

            if (positionY <= topLimit) {
                positionY = topLimit;
                jumpVelocity = Math.min(jumpVelocity, 0);
            }

            if (positionY >= bottomLimit) {
                positionY = bottomLimit;
                jumping = false;
                jumpVelocity = 0;
                if (jumpBuffer > 0 && !isHaltedByGame) {
                    startJump();
                }
            }
        }
        if (jumpBuffer > 0) {
            jumpBuffer--;
        }
        y = (int) Math.round(positionY);

        if (jumping) {
            currentImage = jumpImage;
            stopWalkSoundIfPlaying();
        } else if (dx > 0) {
            animateWalk(walkRightImages);
            playWalkSound();
        } else if (dx < 0) {
            animateWalk(walkLeftImages);
            playWalkSound();
        } else {
            currentImage = idleImage;
            animationFrame = 0;
            animationCounter = 0;
            stopWalkSoundIfPlaying();
        }
    }

    private void animateWalk(Image[] frames) {
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            animationCounter = 0;
            animationFrame = (animationFrame + 1) % frames.length;
        }
        currentImage = frames[animationFrame];
    }

    private void playWalkSound() {
        long now = System.currentTimeMillis();
        if (!isHaltedByGame && dx != 0 && (!walkSoundPlaying || now - lastWalkSoundTime > walkSoundCooldown)) {
            soundPlayer.playSound("asset/sounds/walk.wav");
            walkSoundPlaying = true;
            lastWalkSoundTime = now;
        } else if (dx == 0 || isHaltedByGame) {
            stopWalkSoundIfPlaying();
        }
    }

    private void stopWalkSoundIfPlaying() {
        if (walkSoundPlaying) {
            soundPlayer.stopSound("asset/sounds/walk.wav");
            walkSoundPlaying = false;
        }
    }

    public void draw(Graphics g, int cameraX) {
        if (isAlive) {
            boolean visible = invulnerable == 0 || (invulnerable / 4) % 2 == 0;
            if (visible) {
                g.drawImage(currentImage, x - cameraX, y, null);
            }
            drawHealthBar(g, cameraX);
        } else {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, 1280, 720);

            // Judul "KAMU TELAH MATI!"
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.setColor(new Color(255, 255, 255));
            String title = "KAMU TELAH MATI!";
            int titleWidth = g2d.getFontMetrics().stringWidth(title);
            g2d.drawString(title, (1280 - titleWidth) / 2, 380);

            // Subjudul
            g2d.setFont(new Font("Serif", Font.ITALIC, 24));
            String sub = "PERLAWANAN PENJAJAH GAGAL!";
            int subWidth = g2d.getFontMetrics().stringWidth(sub);
            g2d.drawString(sub, (1280 - subWidth) / 2, 420);

            // Instruksi restart
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String info = "PENCET TOMBOL \"R\" UNTUK RESTART GAME";
            int infoWidth = g2d.getFontMetrics().stringWidth(info);
            g2d.drawString(info, (1280 - infoWidth) / 2, 455);
        }
    }

    private void drawHealthBar(Graphics g, int cameraX) {
        int barWidth = 60;
        int barHeight = 6;
        int fillWidth = (int) ((health / (double) MAX_HEALTH) * barWidth);

        int barX = x - cameraX + (PLAYER_WIDTH - barWidth) / 2;
        int barY = y + 10;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(health > 30 ? new Color(80, 220, 90) : new Color(230, 70, 60));
        g.fillRect(barX, barY, fillWidth, barHeight);
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }

    public void setDirection(int direction) {
        if (!isHaltedByGame) {
            dx = direction * speed;
        } else {
            dx = 0;
        }
    }

    /** @return true bila lompatan benar-benar dimulai (bukan hanya disimpan di buffer). */
    public boolean jump() {
        if (isHaltedByGame || !isAlive) {
            return false;
        }
        if (jumping) {
            jumpBuffer = JUMP_BUFFER_FRAMES;
            return false;
        }
        startJump();
        return true;
    }

    private void startJump() {
        jumping = true;
        jumpVelocity = jumpStrength;
        jumpBuffer = 0;
        soundPlayer.playSound("asset/sounds/jump.wav");
    }

    /** Tombol lompat dilepas: lompatan dipendekkan agar tinggi lompatan bisa diatur. */
    public void releaseJump() {
        if (jumping && jumpVelocity > 6) {
            jumpVelocity = 6;
        }
        jumpBuffer = 0;
    }

    /**
     * Menerima kerusakan dengan masa kebal singkat.
     *
     * @return true bila kerusakan benar-benar diterima
     */
    public boolean hurt(int damage) {
        if (!isAlive || invulnerable > 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastHitSoundTime > hitSoundCooldown) {
            soundPlayer.playSound("asset/sounds/hitHurt.wav");
            lastHitSoundTime = now;
        }
        invulnerable = INVULNERABLE_FRAMES;
        takeDamage(damage);
        return true;
    }

    public void takeDamage(int damage) {
        if (isAlive) {
            health -= damage;
            if (health <= 0) {
                health = 0;
                die();
            }
        }
    }

    public void heal(int amount) {
        if (isAlive) {
            health += amount;
            if (health > MAX_HEALTH) {
                health = MAX_HEALTH;
            }
        }
    }

    public void hitByEnemyBullet() {
        hurt(7);
    }

    public void die() {
        isAlive = false;
        isHaltedByGame = true;
        dx = 0;
        stopWalkSoundIfPlaying();
        soundPlayer.playSound("asset/sounds/enemy_death.wav");
    }

    public void stop() {
        this.dx = 0;
        this.isHaltedByGame = true;
        stopWalkSoundIfPlaying();
        this.currentImage = idleImage;
        this.animationFrame = 0;
        this.animationCounter = 0;
    }

    public void reset() {
        x = 0;
        y = groundY;
        positionY = groundY;
        health = MAX_HEALTH;
        isAlive = true;
        dx = 0;
        jumping = false;
        jumpVelocity = 0;
        jumpBuffer = 0;
        invulnerable = 0;
        walkSoundPlaying = false;
        lastHitSoundTime = 0;
        lastWalkSoundTime = 0;
        isHaltedByGame = false;
        currentImage = idleImage;
    }
}
