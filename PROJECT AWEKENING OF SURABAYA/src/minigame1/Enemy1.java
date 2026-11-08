// minigame1/Enemy1.java
package minigame1;

import java.awt.Image;
import java.util.HashMap;
import java.util.HashSet;

public class Enemy1 extends Block {

    /** Peran musuh: masing-masing mengincar sasaran yang berbeda agar pemain tidak hanya dikejar dari satu arah. */
    public enum Role {
        CHASER, // mengejar langsung ke posisi pemain
        AMBUSHER, // menghadang di depan arah gerak pemain
        GUARD, // menjaga radio dan pintu keluar
        FLANKER // mengejar dari jauh, mundur saat terlalu dekat
    }

    public static final int SPEED = 2; // harus membagi ukuran tile agar posisi selalu sejajar grid

    private final int enemyTileSize;
    private final HashMap<Character, Image> directionImages = new HashMap<>();

    public char enemyDirection = 'U';
    private Role role = Role.CHASER;
    private int releaseDelay = 0; // jeda (frame) setelah pemain mulai bergerak sebelum musuh keluar

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getReleaseDelay() {
        return releaseDelay;
    }

    public void setReleaseDelay(int releaseDelay) {
        this.releaseDelay = releaseDelay;
    }

    public void updateDirection(char direction) {
        this.enemyDirection = direction;

        velocityX = 0;
        velocityY = 0;

        switch (direction) {
            case 'U' ->
                velocityY = -SPEED;
            case 'D' ->
                velocityY = SPEED;
            case 'L' ->
                velocityX = -SPEED;
            case 'R' ->
                velocityX = SPEED;
        }
        Image directionImage = directionImages.get(direction);
        if (directionImage != null) {
            image = directionImage;
        }
    }

    /**
     * Bergerak satu langkah menuju tile sasaran. Keputusan arah hanya diambil di tengah tile,
     * dan hanya pada jalur yang terbuka sehingga musuh tidak pernah menabrak atau tersangkut dinding.
     */
    public void update(boolean[][] wallGrid, int targetCol, int targetRow) {
        if (x % enemyTileSize == 0 && y % enemyTileSize == 0) {
            updateDirection(chooseDirection(wallGrid, targetCol, targetRow));
        }
        x += velocityX;
        y += velocityY;
    }

    private char chooseDirection(boolean[][] wallGrid, int targetCol, int targetRow) {
        int col = x / enemyTileSize;
        int row = y / enemyTileSize;
        char reverse = opposite(enemyDirection);

        char best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (char dir : new char[]{'U', 'L', 'D', 'R'}) {
            if (dir == reverse || isBlocked(wallGrid, col, row, dir)) {
                continue;
            }
            int nextCol = col + dx(dir);
            int nextRow = row + dy(dir);
            long distance = (long) (nextCol - targetCol) * (nextCol - targetCol)
                    + (long) (nextRow - targetRow) * (nextRow - targetRow);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = dir;
            }
        }
        if (best == 0) {
            // jalan buntu: balik arah
            best = isBlocked(wallGrid, col, row, reverse) ? enemyDirection : reverse;
        }
        return best;
    }

    private static boolean isBlocked(boolean[][] wallGrid, int col, int row, char dir) {
        int nextCol = col + dx(dir);
        int nextRow = row + dy(dir);
        if (nextRow < 0 || nextRow >= wallGrid.length || nextCol < 0 || nextCol >= wallGrid[0].length) {
            return true;
        }
        return wallGrid[nextRow][nextCol];
    }

    private static int dx(char dir) {
        return dir == 'L' ? -1 : dir == 'R' ? 1 : 0;
    }

    private static int dy(char dir) {
        return dir == 'U' ? -1 : dir == 'D' ? 1 : 0;
    }

    private static char opposite(char dir) {
        return switch (dir) {
            case 'U' ->
                'D';
            case 'D' ->
                'U';
            case 'L' ->
                'R';
            default ->
                'L';
        };
    }

    @Override
    public void reset() {
        super.reset();
        enemyDirection = 'U';
    }
}
