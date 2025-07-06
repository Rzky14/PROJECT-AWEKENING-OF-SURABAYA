// minigame1/Player1.java
package minigame1;

import audio.SoundPlayer;
import java.awt.Image;
import java.util.HashSet;

public class Player1 extends Block {

    private int lives = 3;
    private int score = 0;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private final long walkSoundCooldown = 250;
    private final long hitSoundCooldown = 300;

    private long lastWalkSoundTime = 0;
    private long lastHitSoundTime = 0;

    public Player1(Image image, int x, int y, int width, int height, HashSet<Block> walls, int tileSize) {
        super(image, x, y, width, height, walls, tileSize);
    }

    public void updateDirection(char dir) {
        int speed = tileSize / 4;

        velocityX = 0;
        velocityY = 0;

        switch (dir) {
            case 'L' ->
                velocityX = -speed;
            case 'R' ->
                velocityX = speed;
            case 'U' ->
                velocityY = -speed;
            case 'D' ->
                velocityY = speed;
        }

        if (velocityX == 0 && velocityY == 0) {
            stopWalkSound();
        } else {
            playWalkSound();
        }
    }

    private void playWalkSound() {
        long now = System.currentTimeMillis();
        if (now - lastWalkSoundTime > walkSoundCooldown) {
            soundPlayer.playSound("asset/sounds/walk.wav");
            lastWalkSoundTime = now;
        }
    }

    private void stopWalkSound() {
        soundPlayer.stopSound("asset/sounds/walk.wav");
    }

    public void stopMoving() {
        velocityX = 0;
        velocityY = 0;
        stopWalkSound();
    }

    public void playPickRadioSound() {
        soundPlayer.playSound("asset/sounds/pickradio.wav");
    }

    public void playHurtSound() {
        long now = System.currentTimeMillis();
        if (now - lastHitSoundTime > hitSoundCooldown) {
            soundPlayer.playSound("asset/sounds/Hurt.wav");
            lastHitSoundTime = now;
        }
    }

    @Override
    public void reset() {
        super.reset();
        lives = 3;
        score = 0;
        stopWalkSound();
        lastWalkSoundTime = 0;
        lastHitSoundTime = 0;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
