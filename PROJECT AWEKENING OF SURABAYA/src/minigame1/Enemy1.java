// minigame1/Enemy1.java
package minigame1;

import java.awt.Image;
import java.util.HashMap;
import java.util.HashSet;

public class Enemy1 extends Block {

    private final int enemyTileSize;
    private final HashMap<Character, Image> directionImages = new HashMap<>();

    public char enemyDirection = 'U';
    private final int animationSpeed = 5;
    private int animationCounter = 0;

    public Enemy1(Image defaultImage, int x, int y, int width, int height,
            HashSet<Block> walls, int tileSize) {
        super(defaultImage, x, y, width, height);
        this.enemyTileSize = tileSize;
        for (char dir : new char[]{'U', 'D', 'L', 'R'}) {
            directionImages.put(dir, defaultImage);
        }
    }

    public Enemy1(Image up, Image down, Image left, Image right,
            int x, int y, int width, int height,
            HashSet<Block> walls, int tileSize) {
        super(up, x, y, width, height, walls, tileSize);
        this.enemyTileSize = tileSize;

        directionImages.put('U', up);
        directionImages.put('D', down);
        directionImages.put('L', left);
        directionImages.put('R', right);
    }

    public void updateDirection(char direction) {
        this.enemyDirection = direction;
        int speed = enemyTileSize / 5;

        velocityX = 0;
        velocityY = 0;

        switch (direction) {
            case 'U' ->
                velocityY = -speed;
            case 'D' ->
                velocityY = speed;
            case 'L' ->
                velocityX = -speed;
            case 'R' ->
                velocityX = speed;
        }
        image = directionImages.get(direction);
    }

    public void updateAnimation() {
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            animationCounter = 0;
        }
    }

    @Override
    public void reset() {
        super.reset();
    }
}
