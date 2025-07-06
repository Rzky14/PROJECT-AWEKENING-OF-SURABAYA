package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import javax.swing.ImageIcon;

public class Player3 {

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    int x, y;
    int dx;
    int speed = 5;
    int health = 100;
    boolean isAlive = true;

    private boolean isHaltedByGame = false;

    private static final int PLAYER_WIDTH = 140;
    private static final int PLAYER_HEIGHT = 160;
    private static final Image idleImage = loadImage("asset/player/Player1.png");
    private static final Image jumpImage = loadImage("asset/player/PlayerLoncat.png");
    private static final Image image1 = loadImage("asset/player/Player1.png");
    private static final Image image2 = loadImage("asset/player/Player2.png");

    private static final Image[] walkRightImages = new Image[]{
        image2, image1, image2, image1, image2, image1
    };
    private static final Image[] walkLeftImages = new Image[]{
        image1, image2, image1, image2, image1, image2
    };

    private static Image loadImage(String path) {
        return new ImageIcon(path).getImage().getScaledInstance(PLAYER_WIDTH, PLAYER_HEIGHT, Image.SCALE_SMOOTH);
    }

    Image currentImage = idleImage;

    int animationFrame = 0;
    int animationCounter = 0;
    int animationSpeed = 10;

    boolean jumping = false;
    int jumpVelocity = 0;
    final int gravity = 2;
    final int jumpStrength = 20;
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
    }

    public void update() {
        if (!isAlive) {
            stopWalkSoundIfPlaying();
            return;
        }

        if (isHaltedByGame) {
            dx = 0;
        }

        x += dx;
        if (x < 0) {
            x = 0;
        }

        if (jumping) {
            y -= jumpVelocity;
            jumpVelocity -= gravity;

            if (y <= topLimit) {
                y = topLimit;
                jumpVelocity = 0;
            }

            if (y >= bottomLimit) {
                y = bottomLimit;
                jumping = false;
                jumpVelocity = 0;
            }
        }

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
            g.drawImage(currentImage, x - cameraX, y, null);
            drawHealthBar(g, cameraX);
        } else {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, 1280, 720);

            // Hapus baris ini:
            // g2d.drawImage(currentImage, x - cameraX, y, null);
            // Judul "KAMU TELAH MATI!"
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.setColor(new Color(255, 255, 255));
            String title = "KAMU TELAH MATI!";
            int titleWidth = g2d.getFontMetrics().stringWidth(title);
            g2d.drawString(title, (1280 - titleWidth) / 2, 380);

            // Subjudul "PERLARIAN BUNG TOMO GAGAL"
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
        int barWidth = 50;
        int barHeight = 5;
        int fillWidth = (int) ((health / 100.0) * barWidth);

        int barX = x - cameraX + (PLAYER_WIDTH - barWidth) / 3;
        int barY = y - 20;

        g.setColor(Color.GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.GREEN);
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

    public void jump() {
        if (!jumping && !isHaltedByGame) {
            jumping = true;
            jumpVelocity = jumpStrength;
        }
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
            if (health > 100) {
                health = 100;
            }
        }
    }

    public void hitByEnemyBullet() {
        if (isAlive) {
            long now = System.currentTimeMillis();
            if (now - lastHitSoundTime > hitSoundCooldown) {
                soundPlayer.playSound("asset/sounds/hitHurt.wav");
                lastHitSoundTime = now;
            }
            takeDamage(7);
        }
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
        health = 100;
        isAlive = true;
        dx = 0;
        jumping = false;
        jumpVelocity = 0;
        walkSoundPlaying = false;
        lastHitSoundTime = 0;
        lastWalkSoundTime = 0;
        isHaltedByGame = false;
        currentImage = idleImage;
    }
}
