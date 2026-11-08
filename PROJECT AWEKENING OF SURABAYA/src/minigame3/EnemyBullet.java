package minigame3;

import java.awt.*;
import util.Assets;

public class EnemyBullet {

    public int x, y;
    public int speed = 5; // Sesuaikan nilai ini untuk mengubah kecepatan peluru enemy
    public boolean active = true;

    public final int width = 20;
    public final int height = 20;

    /** Peluru tinggi lewat di atas kepala pemain yang berdiri, tetapi mengenai pemain yang sedang melompat. */
    public final boolean high;
    public final int damage;

    private static final Image bulletImage = Assets.image("asset/bullet/peluru.png", 20, 20);

    public EnemyBullet(int startX, int startY) {
        this(startX, startY, 5, false, 7);
    }

    public EnemyBullet(int startX, int startY, int speed, boolean high, int damage) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.high = high;
        this.damage = damage;
    }

    public void update() {
        x -= speed;
        if (x < -width) {
            active = false;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics g, int cameraX) {
        int screenX = x - cameraX;
        // Cahaya pembeda: merah untuk peluru setinggi badan (hindari dengan melompat),
        // kuning untuk peluru tinggi (hindari dengan tetap di tanah)
        g.setColor(high ? new Color(255, 210, 40, 120) : new Color(255, 50, 50, 110));
        g.fillOval(screenX - 5, y - 5, width + 10, height + 10);
        g.drawImage(bulletImage, screenX, y, null);
    }
}
