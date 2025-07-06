package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.ImageIcon;

public class Enemy3 {

    int x, y;
    int maxHealth;
    int hitCount = 0;
    boolean alive = true;

    private static final int ENEMY_WIDTH = 180;
    private static final int ENEMY_HEIGHT = 200;
    // GROUND_Y sekarang akan merepresentasikan posisi KAKI musuh
    private static final int GROUND_Y = 647;
    private static final Image[] enemyFrames = new Image[4];
    private static final Image[] bossFrames = new Image[4];

    static {
        enemyFrames[0] = loadScaledImage("asset/enemy/enemy1.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[1] = loadScaledImage("asset/enemy/enemy2.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[2] = loadScaledImage("asset/enemy/enemy3.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[3] = loadScaledImage("asset/enemy/enemy1.png", ENEMY_WIDTH, ENEMY_HEIGHT);

        // Gunakan dimensi ini untuk memuat gambar bos agar konsisten dengan actualEnemyHeight/Width
        // Jika Anda ingin ukuran bos berbeda, ubah nilai di bawah DAN di konstruktor.
        int visualBossWidth = ENEMY_WIDTH + 4;  // Sesuai dengan pemuatan gambar Anda
        int visualBossHeight = ENEMY_HEIGHT + 4; // Sesuai dengan pemuatan gambar Anda

        bossFrames[0] = loadScaledImage("asset/enemy/boss1.png", visualBossWidth, visualBossHeight);
        bossFrames[1] = loadScaledImage("asset/enemy/boss2.png", visualBossWidth, visualBossHeight);
        bossFrames[2] = loadScaledImage("asset/enemy/boss3.png", visualBossWidth, visualBossHeight);
        bossFrames[3] = loadScaledImage("asset/enemy/boss1.png", visualBossWidth, visualBossHeight);
    }

    private static Image loadScaledImage(String path, int width, int height) {
        return new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    int currentFrame = 0;
    int animationTimer = 0;
    int animationInterval = 10;

    ArrayList<EnemyBullet> bullets = new ArrayList<>();

    int leftBound = 500;
    int rightBound = 1200;

    int dx;
    int moveTimer = 0;
    int moveInterval;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private boolean isWalking = false;
    private final String walkSoundFile = "asset/sounds/walk.wav";

    private long lastHitSoundTime = 0;
    private final long hitSoundCooldown = 350;

    private long lastWalkSoundTime = 0;
    private final long walkSoundCooldown = 250;

    private long lastShootTime = 0;
    private final long shootCooldown = 3000;

    private boolean isBoss = false;
    private int actualEnemyWidth;
    private int actualEnemyHeight;

    public void setMovementBounds(int left, int right) {
        this.leftBound = left;
        this.rightBound = right;
    }

    public Enemy3(int startX, int startY, int health, int speed, int shootMin, int shootMax, int moveInterval, boolean isBoss) {
        this.x = startX;
        // startY dari GamePanel3 tidak digunakan secara langsung untuk y akhir, karena kita pakai GROUND_Y

        this.maxHealth = health;
        this.dx = speed; // Kecepatan awal
        this.moveInterval = moveInterval;
        this.isBoss = isBoss;

        // --- AWAL PERUBAHAN: Atur actualEnemyWidth dan actualEnemyHeight ---
        // Pastikan nilai ini SAMA dengan yang digunakan saat loadScaledImage untuk bossFrames
        if (isBoss) {
            // Menggunakan dimensi dari pemuatan gambar bossFrames di blok static
            this.actualEnemyWidth = ENEMY_WIDTH + 4;  // Lebar visual bos
            this.actualEnemyHeight = ENEMY_HEIGHT + 4; // Tinggi visual bos
        } else {
            this.actualEnemyWidth = ENEMY_WIDTH;
            this.actualEnemyHeight = ENEMY_HEIGHT;
        }
        // --- AKHIR PERUBAHAN ---

        // --- AWAL PERUBAHAN: Hitung y agar kaki berada di GROUND_Y ---
        // this.y adalah koordinat y untuk sudut kiri ATAS gambar saat digambar.
        // Jadi, y = GROUND_Y (posisi kaki) - tinggi_musuh_aktual.
        this.y = GROUND_Y - this.actualEnemyHeight;
        // --- AKHIR PERUBAHAN ---
    }

    public void update() {
        if (!alive) {
            stopWalkSound();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastShootTime >= shootCooldown) {
            shoot();
            lastShootTime = now;
        }

        moveTimer++;
        if (moveTimer >= moveInterval) {
            dx = new Random().nextInt(5) - 2; // -2 hingga 2
            moveTimer = 0;
        }

        x += dx;
        if (x <= leftBound) {
            x = leftBound;
            dx = Math.abs(dx);
        } else if (x >= rightBound) {
            x = rightBound;
            dx = -Math.abs(dx);
        }

        if (dx != 0) {
            playWalkSound();
            animateWalking();
        } else {
            stopWalkSound();
            currentFrame = 0; // Kembali ke frame idle jika berhenti
        }

        for (EnemyBullet bullet : bullets) {
            bullet.update();
        }
        bullets.removeIf(b -> !b.active);
    }

    private void playWalkSound() {
        long now = System.currentTimeMillis();
        if (!isWalking || now - lastWalkSoundTime > walkSoundCooldown) {
            soundPlayer.playLoopingSound("walk" + this.hashCode(), walkSoundFile);
            isWalking = true;
            lastWalkSoundTime = now;
        }
    }

    private void stopWalkSound() {
        if (isWalking) {
            soundPlayer.stopLoopingSound("walk" + this.hashCode());
            isWalking = false;
        }
    }

    private void animateWalking() {
        animationTimer++;
        if (animationTimer >= animationInterval) {
            animationTimer = 0;
            currentFrame = (currentFrame + 1);
            Image[] framesToUse = isBoss ? bossFrames : enemyFrames;
            if (currentFrame >= framesToUse.length) {
                // Jika animasi jalan seharusnya loop dari frame tertentu, sesuaikan di sini
                // Misalnya, jika frame 0 adalah idle, dan jalan mulai dari 1.
                currentFrame = (framesToUse == enemyFrames && enemyFrames.length > 1) ? 1 : 0;
                if (framesToUse == bossFrames && bossFrames.length > 1) {
                    currentFrame = 1; // Sesuaikan jika perlu

                }
                if (framesToUse.length <= 1) {
                    currentFrame = 0; // fallback

                }
            }
        }
    }

    public void draw(Graphics g, int cameraX) {
        Image[] framesToUse = isBoss ? bossFrames : enemyFrames;
        // Gambar digambar dari this.x dan this.y (sudut kiri atas)
        g.drawImage(framesToUse[currentFrame], x - cameraX, y, null);

        if (alive) {
            int barWidth = 50;
            int barHeight = 6;
            int healthBar = (int) (((double) (maxHealth - hitCount) / maxHealth) * barWidth);

            int barX = x - cameraX + (actualEnemyWidth - barWidth) / 2;
            // Posisi Y health bar sedikit di atas kepala musuh
            int barY = y - 15; // Naikkan healthbar sedikit (misal 15px di atas y musuh)

            g.setColor(Color.RED);
            g.fillRect(barX, barY, barWidth, barHeight);
            g.setColor(Color.GREEN);
            g.fillRect(barX, barY, healthBar, barHeight);
            g.setColor(Color.BLACK);
            g.drawRect(barX, barY, barWidth, barHeight);
        }
    }

    public boolean isHit(Bullet bullet) {
        int offsetX = 65;
        int offsetY = 40;
        int hitboxWidth = 90;

        // Menggunakan saran sebelumnya untuk hitbox dinamis (Opsi 1 dari jawaban sebelumnya)
        int currentHitboxHeight;
        if (isBoss) {
            currentHitboxHeight = (int) ((150.0 / ENEMY_HEIGHT) * actualEnemyHeight);
        } else {
            currentHitboxHeight = 150;
        }

        Rectangle enemyBox = new Rectangle(x + offsetX, y + offsetY, hitboxWidth, currentHitboxHeight);
        Rectangle bulletBox = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);
        return alive && enemyBox.intersects(bulletBox);
    }

    public void hit() {
        if (alive) {
            long now = System.currentTimeMillis();
            if (now - lastHitSoundTime > hitSoundCooldown) {
                soundPlayer.playSound("asset/sounds/hitHurt.wav");
                lastHitSoundTime = now;
            }

            hitCount += 2;
            if (hitCount >= maxHealth) {
                die();
            }
        }
    }

    public void die() {
        alive = false;
        stopWalkSound();
        soundPlayer.playSound("asset/sounds/enemy_death.wav");
        lastShootTime = 0; // Agar tidak langsung menembak jika di-reset/muncul lagi
    }

    public void shoot() {
        if (bullets.size() < 5) {
            int offsetX;
            int offsetY;

            if (isBoss) {
                // Nilai ini HARUS Anda sesuaikan melalui trial-and-error
                // agar pas dengan gambar bos Anda.
                offsetX = 44;  // GANTI NILAI INI (Geser Kanan/Kiri)
                offsetY = 92; // GANTI NILAI INI (Geser Atas/Bawah)
            } else {
                // Nilai untuk musuh biasa (sudah pas)
                offsetX = 44;
                offsetY = 92;
            }
            
            int bulletSpawnX = this.x + offsetX;
            int bulletSpawnY = this.y + offsetY;
            
            bullets.add(new EnemyBullet(bulletSpawnX, bulletSpawnY));
            soundPlayer.playSound("asset/sounds/shootenemy.wav");
        }
    }
    
    public ArrayList<EnemyBullet> getBullets() {
        return bullets;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getDx() {
        return dx;
    }
}
