package minigame1;

import java.awt.Image;
import java.util.HashSet;
import java.util.Random;

public class GameEngine {

    private final int rowCount, columnCount, tileSize;
    private final int boardWidth, boardHeight;
    private final String[] tileMap;

    private final HashSet<Block> walls = new HashSet<>();
    private final HashSet<Block> points = new HashSet<>();
    private final HashSet<Enemy1> enemies = new HashSet<>();
    private final Random random = new Random();
    private final char[] directions = {'U', 'D', 'L', 'R'};

    private Player1 player;
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;

    private final Image wallImage;
    private final Image[] enemyImages;
    private final Image[] playerImages;

    public GameEngine(int rowCount, int columnCount, int tileSize,
            String[] tileMap,
            Image wallImage,
            Image[] enemy1Frames, Image enemy2, Image enemy3, Image enemy4,
            Image playerUp, Image playerDown, Image playerLeft, Image playerRight,
            Image radioImage) {

        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.tileSize = tileSize;
        this.boardWidth = columnCount * tileSize;
        this.boardHeight = rowCount * tileSize;
        this.tileMap = tileMap;
        this.wallImage = wallImage;
        this.enemyImages = new Image[]{enemy1Frames[0], enemy2, enemy3, enemy4};
        this.playerImages = new Image[]{playerUp, playerDown, playerLeft, playerRight};

        loadMap();
    }

    public void update() {
        if (gameOver) {
            return;
        }

        // Update player position
        player.x += player.velocityX;
        player.y += player.velocityY;

        // Cek tabrakan dengan musuh
        for (Enemy1 enemy : enemies) {
            if (Block.collision(player, enemy)) {
                if (--lives <= 0) {
                    gameOver = true;
                    return;
                }
                resetPositions();
                break;
            }
        }

        // Update musuh
        for (Enemy1 enemy : enemies) {
            enemy.x += enemy.velocityX;
            enemy.y += enemy.velocityY;

            if (checkCollision(enemy)) {
                enemy.x -= enemy.velocityX;
                enemy.y -= enemy.velocityY;
                enemy.updateDirection(randomDirection());
            }
        }

        Block ambil = points.stream()
                .filter(point -> Block.collision(player, point))
                .findFirst()
                .orElse(null);

        if (ambil != null) {
            points.remove(ambil);
            points.add(ambil); // Tampilkan sebagai point
            score += 10;
        }

        // Jika semua makanan habis, reset map
        if (points.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    public final void loadMap() {
        walls.clear();
        points.clear();
        enemies.clear();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char ch = tileMap[r].charAt(c);
                int x = c * tileSize, y = r * tileSize;

                switch (ch) {
                    case 'X' ->
                        walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                    case 'b' ->
                        enemies.add(createEnemy(enemyImages[0], x, y));
                    case 'o' ->
                        enemies.add(createEnemy(enemyImages[1], x, y));
                    case 'p' ->
                        enemies.add(createEnemy(enemyImages[2], x, y));
                    case 'r' ->
                        enemies.add(createEnemy(enemyImages[3], x, y));
                    case 'P' ->
                        player = new Player1(playerImages[3], x, y, tileSize, tileSize, walls, tileSize);
                    case ' ' ->
                        points.add(new Block(null, x + 14, y + 14, 4, 4));
                }
            }
        }
    }

    public void resetPositions() {
        player.reset();
        player.velocityX = 0;
        player.velocityY = 0;
        for (Enemy1 e : enemies) {
            e.reset();
            e.updateDirection(randomDirection());
        }
    }

    public void restartGame() {
        score = 0;
        lives = 3;
        gameOver = false;
        loadMap();
        resetPositions();
    }

    // Helpers
    private Enemy1 createEnemy(Image img, int x, int y) {
        return new Enemy1(img, x, y, tileSize, tileSize, walls, tileSize);
    }

    private boolean checkCollision(Enemy1 enemy) {
        return walls.stream().anyMatch(wall -> Block.collision(enemy, wall))
                || enemy.x < 0 || enemy.y < 0
                || enemy.x + enemy.width >= boardWidth
                || enemy.y + enemy.height >= boardHeight;
    }

    public char randomDirection() {
        return directions[random.nextInt(directions.length)];
    }

    // Getters
    public Player1 getPlayer() {
        return player;
    }

    public HashSet<Block> getWalls() {
        return walls;
    }

    public HashSet<Block> getFoods() {
        return points;
    }

    public HashSet<Enemy1> getEnemies() {
        return enemies;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Image getPlayerUpImage() {
        return playerImages[0];
    }

    public Image getPlayerDownImage() {
        return playerImages[1];
    }

    public Image getPlayerLeftImage() {
        return playerImages[2];
    }

    public Image getPlayerRightImage() {
        return playerImages[3];
    }
}
