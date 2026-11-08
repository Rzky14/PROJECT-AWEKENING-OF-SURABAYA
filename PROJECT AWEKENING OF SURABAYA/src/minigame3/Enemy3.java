package minigame3;

import audio.SoundPlayer;
import java.awt.*;
import java.util.List;
import java.util.Random;
import util.Assets;

public class Enemy3 {

    /** Tahap perilaku musuh; setiap serangan diawali isyarat (TELEGRAPH) agar pemain sempat bereaksi. */
    private enum Mode {
        ENTER, MOVE, TELEGRAPH, SHOOT, CHARGE, RECOVER, RETURN
    }

    /** Pola serangan: 0 = peluru setinggi badan (hindari dengan melompat), 1 = peluru tinggi (hindari dengan tetap di tanah). */
    private static final int[][] PATTERN_SINGLE = {{0}};
    private static final int[][] PATTERN_DOUBLE = {{0, 1}};
    private static final int[][] PATTERN_TRIPLE = {{0, 1, 0}};
    private static final int PATTERN_CHARGE = -1;
    private static final int SHOT_GAP = 70;                 // jeda antar peluru dalam satu rentetan (frame)

    int x, y;
    int maxHealth;
    int hitCount = 0;
    boolean alive = true;

    private static final int ENEMY_WIDTH = 180;
    private static final int ENEMY_HEIGHT = 200;
    // GROUND_Y merepresentasikan posisi KAKI musuh
    private static final int GROUND_Y = 647;
    private static final int ENTER_SPEED = 4;
    private static final Image[] enemyFrames = new Image[4];
    private static final Image[] bossFrames = new Image[4];

    static {
        enemyFrames[0] = Assets.image("asset/enemy/enemy1.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[1] = Assets.image("asset/enemy/enemy2.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[2] = Assets.image("asset/enemy/enemy3.png", ENEMY_WIDTH, ENEMY_HEIGHT);
        enemyFrames[3] = enemyFrames[0];

        int visualBossWidth = ENEMY_WIDTH + 4;
        int visualBossHeight = ENEMY_HEIGHT + 4;

        bossFrames[0] = Assets.image("asset/enemy/boss1.png", visualBossWidth, visualBossHeight);
        bossFrames[1] = Assets.image("asset/enemy/boss2.png", visualBossWidth, visualBossHeight);
        bossFrames[2] = Assets.image("asset/enemy/boss3.png", visualBossWidth, visualBossHeight);
        bossFrames[3] = bossFrames[0];
    }

    int currentFrame = 0;
    int animationTimer = 0;
    int animationInterval = 10;

    int leftBound = 500;
    int rightBound = 1200;

    private final Random random = new Random();
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private final String walkSoundId = "walk" + hashCode();
    private boolean isWalking = false;
    private final String walkSoundFile = "asset/sounds/walk.wav";

    private long lastHitSoundTime = 0;
    private final long hitSoundCooldown = 350;

    private final boolean isBoss;
    private final int actualEnemyWidth;
    private final int actualEnemyHeight;
    private final double moveSpeed;
    private final int shootIntervalMin;
    private final int shootIntervalMax;
    private final int bulletSpeed;
    private final int bulletDamage;

    private Mode mode = Mode.ENTER;
    private double posX;
    private int targetX;
    private int timer = 0;           // hitung mundur untuk tahap yang sedang berjalan (frame)
    private int cooldown = 0;        // jeda sebelum serangan berikutnya
    private int pattern = 0;         // indeks pola serangan berikutnya (untuk bos)
    private int[] shotQueue = null;
    private int shotIndex = 0;
    private int currentAttack = 0;
    private int hitFlash = 0;
    private boolean moving = false;

    public void setMovementBounds(int left, int right) {
        this.leftBound = left;
        this.rightBound = right;
    }

    /**
     * @param startX posisi awal di luar layar sebelah kanan
     * @param health jumlah tembakan yang dibutuhkan untuk mengalahkan musuh
     * @param speed kecepatan jalan (piksel per frame)
     * @param shootMin jeda tembak tercepat (frame); untuk bos, jeda antar serangan
     * @param shootMax jeda tembak terlama (frame)
     * @param bulletSpeed kecepatan peluru
     * @param bulletDamage kerusakan peluru ke pemain
     */
    public Enemy3(int startX, int health, double speed, int shootMin, int shootMax,
            int bulletSpeed, int bulletDamage, boolean isBoss) {
        this.x = startX;
        this.posX = startX;
        this.maxHealth = health;
        this.moveSpeed = speed;
        this.shootIntervalMin = shootMin;
        this.shootIntervalMax = shootMax;
        this.bulletSpeed = bulletSpeed;
        this.bulletDamage = bulletDamage;
        this.isBoss = isBoss;

        if (isBoss) {
            this.actualEnemyWidth = ENEMY_WIDTH + 4;
            this.actualEnemyHeight = ENEMY_HEIGHT + 4;
        } else {
            this.actualEnemyWidth = ENEMY_WIDTH;
            this.actualEnemyHeight = ENEMY_HEIGHT;
        }
        // y adalah sudut kiri atas gambar: kaki berada di GROUND_Y
        this.y = GROUND_Y - this.actualEnemyHeight;
    }

    public boolean isBoss() {
        return isBoss;
    }

    /** Musuh yang masih berjalan masuk dari luar layar tidak bisa ditembak maupun menembak. */
    public boolean isEntering() {
        return mode == Mode.ENTER;
    }

    /** Fase bos mengikuti sisa nyawa: 1 (>66%), 2 (>33%), 3 (sisanya). */
    private int phase() {
        double ratio = (maxHealth - hitCount) / (double) maxHealth;
        return ratio > 0.66 ? 1 : ratio > 0.33 ? 2 : 3;
    }

    public double healthRatio() {
        return Math.max(0, (maxHealth - hitCount) / (double) maxHealth);
    }

    public void update(List<EnemyBullet> sink) {
        if (!alive) {
            stopWalkSound();
            return;
        }
        if (hitFlash > 0) {
            hitFlash--;
        }
        moving = false;

        switch (mode) {
            case ENTER -> {
                posX -= ENTER_SPEED;
                moving = true;
                if (posX <= rightBound) {
                    posX = rightBound;
                    mode = Mode.MOVE;
                    cooldown = 45 + random.nextInt(60);
                    pickTarget();
                }
            }
            case MOVE -> {
                moveTowardTarget();
                if (--cooldown <= 0) {
                    beginAttack();
                }
            }
            case TELEGRAPH -> {
                if (--timer <= 0) {
                    startAttack();
                }
            }
            case SHOOT ->
                runShooting(sink);
            case CHARGE -> {
                moving = true;
                posX -= chargeSpeed();
                if (posX <= chargeEndX()) {
                    posX = chargeEndX();
                    mode = Mode.RECOVER;
                    timer = 50;
                }
            }
            case RECOVER -> {
                if (--timer <= 0) {
                    if (currentAttack == PATTERN_CHARGE) {
                        mode = Mode.RETURN;
                    } else {
                        mode = Mode.MOVE;
                        scheduleNextAttack();
                        pickTarget();
                    }
                }
            }
            case RETURN -> {
                moving = true;
                posX += 6;
                if (posX >= rightBound) {
                    posX = rightBound;
                    mode = Mode.MOVE;
                    scheduleNextAttack();
                    pickTarget();
                }
            }
        }

        if (mode != Mode.ENTER && mode != Mode.CHARGE && mode != Mode.RETURN) {
            posX = Math.max(leftBound, Math.min(posX, rightBound));
        }
        x = (int) Math.round(posX);

        if (moving) {
            playWalkSound();
            animateWalking();
        } else {
            stopWalkSound();
            currentFrame = 0;
            animationTimer = 0;
        }
    }

    private void moveTowardTarget() {
        double diff = targetX - posX;
        if (Math.abs(diff) <= moveSpeed) {
            posX = targetX;
            if (random.nextInt(90) == 0) {
                pickTarget();
            }
            return;
        }
        posX += Math.signum(diff) * moveSpeed;
        moving = true;
    }

    private void pickTarget() {
        targetX = leftBound + random.nextInt(Math.max(1, rightBound - leftBound + 1));
    }

    private void scheduleNextAttack() {
        int min = isBoss && phase() == 3 ? shootIntervalMin * 2 / 3 : shootIntervalMin;
        int max = isBoss && phase() == 3 ? shootIntervalMax * 2 / 3 : shootIntervalMax;
        cooldown = min + random.nextInt(Math.max(1, max - min + 1));
    }

    private void beginAttack() {
        currentAttack = nextAttack();
        mode = Mode.TELEGRAPH;
        timer = isBoss ? (phase() == 3 ? 32 : 42) : 28;
    }

    private int nextAttack() {
        if (!isBoss) {
            return 0;
        }
        int[] order = switch (phase()) {
            case 1 ->
                new int[]{0, 0, 3};
            case 2 ->
                new int[]{1, 0, 3, 1};
            default ->
                new int[]{2, 3, 1, 3};
        };
        int pick = order[pattern % order.length];
        pattern++;
        // 0 = tunggal, 1 = ganda, 2 = tiga, 3 = serudukan
        return pick == 3 ? PATTERN_CHARGE : pick;
    }

    private void startAttack() {
        if (currentAttack == PATTERN_CHARGE) {
            mode = Mode.CHARGE;
            return;
        }
        shotQueue = switch (currentAttack) {
            case 1 ->
                PATTERN_DOUBLE[0];
            case 2 ->
                PATTERN_TRIPLE[0];
            default ->
                PATTERN_SINGLE[0];
        };
        shotIndex = 0;
        timer = 0;
        mode = Mode.SHOOT;
    }

    private void runShooting(List<EnemyBullet> sink) {
        if (timer > 0) {
            timer--;
            return;
        }
        fire(sink, shotQueue[shotIndex] == 1);
        shotIndex++;
        if (shotIndex >= shotQueue.length) {
            mode = Mode.RECOVER;
            timer = 35;
            // setelah RECOVER kembali ke MOVE (bukan RETURN)
            currentAttack = 0;
        } else {
            timer = SHOT_GAP;
        }
    }

    private int chargeSpeed() {
        return phase() == 3 ? 9 : 8;
    }

    private int chargeEndX() {
        return 120;
    }

    private void fire(List<EnemyBullet> sink, boolean high) {
        int speed = bulletSpeed + (isBoss && phase() == 3 ? 1 : 0);
        int spawnX = x + 44;
        int spawnY = high ? y + 4 : y + 92;
        sink.add(new EnemyBullet(spawnX, spawnY, speed, high, bulletDamage));
        soundPlayer.playSound("asset/sounds/shootenemy.wav");
    }

    private void playWalkSound() {
        if (!isWalking) {
            soundPlayer.playLoopingSound(walkSoundId, walkSoundFile);
            isWalking = true;
        }
    }

    public void stopWalkSound() {
        if (isWalking) {
            soundPlayer.stopLoopingSound(walkSoundId);
            isWalking = false;
        }
    }

    private void animateWalking() {
        animationTimer++;
        if (animationTimer >= animationInterval) {
            animationTimer = 0;
            Image[] framesToUse = isBoss ? bossFrames : enemyFrames;
            currentFrame++;
            if (currentFrame >= framesToUse.length) {
                currentFrame = 1;
            }
        }
    }

    public void draw(Graphics g, int cameraX) {
        Graphics2D g2d = (Graphics2D) g;
        Image[] framesToUse = isBoss ? bossFrames : enemyFrames;
        int screenX = x - cameraX;

        if (mode == Mode.TELEGRAPH && alive) {
            // Isyarat serangan: cahaya berdenyut dan tanda seru di atas kepala
            int pulse = 70 + (timer % 8) * 12;
            g2d.setColor(new Color(255, 60, 40, Math.min(200, pulse)));
            g2d.fillOval(screenX + 10, y + 20, actualEnemyWidth - 20, actualEnemyHeight - 30);
        }

        g2d.drawImage(framesToUse[currentFrame], screenX, y, null);

        if (hitFlash > 0 && alive) {
            g2d.setColor(new Color(255, 255, 255, 110));
            g2d.fillOval(screenX + 40, y + 40, actualEnemyWidth - 80, actualEnemyHeight - 80);
        }

        if (alive) {
            if (mode == Mode.TELEGRAPH) {
                g2d.setFont(new Font("Arial", Font.BOLD, 34));
                g2d.setColor(new Color(0, 0, 0, 170));
                g2d.drawString("!", screenX + actualEnemyWidth / 2 - 6, y - 14);
                g2d.setColor(currentAttack == PATTERN_CHARGE ? new Color(255, 140, 30) : new Color(255, 70, 60));
                g2d.drawString("!", screenX + actualEnemyWidth / 2 - 8, y - 16);
            }

            if (!isBoss) {
                int barWidth = 50;
                int barHeight = 6;
                int healthBar = (int) (healthRatio() * barWidth);

                int barX = screenX + (actualEnemyWidth - barWidth) / 2;
                int barY = y + 10;

                g2d.setColor(new Color(150, 30, 30));
                g2d.fillRect(barX, barY, barWidth, barHeight);
                g2d.setColor(new Color(80, 220, 90));
                g2d.fillRect(barX, barY, healthBar, barHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(barX, barY, barWidth, barHeight);
            }
        }
    }

    /** Area yang bisa terkena peluru pemain. */
    public Rectangle getHitbox() {
        int offsetX = 65;
        int offsetY = 40;
        int hitboxWidth = 90;
        int hitboxHeight = isBoss ? (int) ((150.0 / ENEMY_HEIGHT) * actualEnemyHeight) : 150;
        return new Rectangle(x + offsetX, y + offsetY, hitboxWidth, hitboxHeight);
    }

    /** Area badan untuk tabrakan dengan pemain; lebih sempit agar melompati serudukan bos masih adil. */
    public Rectangle getContactBox() {
        return new Rectangle(x + 75, y + 60, 70, 120);
    }

    public int getContactDamage() {
        return mode == Mode.CHARGE ? 15 : 8;
    }

    public boolean isHit(Bullet bullet) {
        Rectangle bulletBox = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);
        return alive && mode != Mode.ENTER && getHitbox().intersects(bulletBox);
    }

    public void hit() {
        if (alive) {
            long now = System.currentTimeMillis();
            if (now - lastHitSoundTime > hitSoundCooldown) {
                soundPlayer.playSound("asset/sounds/hitHurt.wav");
                lastHitSoundTime = now;
            }

            hitFlash = 6;
            hitCount++;
            if (hitCount >= maxHealth) {
                die();
            }
        }
    }

    public void die() {
        alive = false;
        stopWalkSound();
        soundPlayer.playSound("asset/sounds/enemy_death.wav");
    }

    public boolean isAlive() {
        return alive;
    }
}
