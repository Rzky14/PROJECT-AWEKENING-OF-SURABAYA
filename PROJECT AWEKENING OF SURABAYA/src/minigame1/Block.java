package minigame1;

import java.awt.*;
import java.util.HashSet;

public class Block {
    public int x, y, width, height;
    public Image image;
    public int startX, startY;
    public char direction = 'U';
    public int velocityX = 0, velocityY = 0;

    protected HashSet<Block> walls;
    protected int tileSize;

    // Konstruktor utama
    public Block(Image image, int x, int y, int width, int height, HashSet<Block> walls, int tileSize) {
        this.image = image;
        this.x = this.startX = x;
        this.y = this.startY = y;
        this.width = width;
        this.height = height;
        this.walls = walls;
        this.tileSize = tileSize;
    }

    // Konstruktor tanpa tembok
    public Block(Image image, int x, int y, int width, int height) {
        this(image, x, y, width, height, null, 0);
    }

    // Cek tabrakan antar dua objek
    public static boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x &&
               a.y < b.y + b.height && a.y + a.height > b.y;
    }

    // Cek apakah bisa pindah ke koordinat tertentu
    public boolean canMove(int newX, int newY) {
        if (walls == null) return true;
        Rectangle next = new Rectangle(newX, newY, width, height);
        return walls.stream().noneMatch(wall ->
            next.intersects(new Rectangle(wall.x, wall.y, wall.width, wall.height)));
    }

    // Gerakkan jika bisa, kalau tidak, hentikan
    public void move() {
        int newX = x + velocityX, newY = y + velocityY;
        if (canMove(newX, newY)) {
            x = newX;
            y = newY;
        } else {
            velocityX = 0;
            velocityY = 0;
        }
    }

    // Reset ke posisi awal
    public void reset() {
        x = startX;
        y = startY;
        velocityX = 0;
        velocityY = 0;
    }
}
