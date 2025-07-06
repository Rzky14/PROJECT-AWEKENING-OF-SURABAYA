package minigame3;

import java.awt.*;
import javax.swing.ImageIcon;

public class EnemyBullet {

    public int x, y;
    public int speed = 5; // Sesuaikan nilai ini untuk mengubah kecepatan peluru enemy
    public boolean active = true;

    public final int width = 20;
    public final int height = 20;

    private static final Image bulletImage;

    static {
        bulletImage = new ImageIcon("asset/bullet/peluru.png")
                .getImage()
                .getScaledInstance(20, 20, Image.SCALE_SMOOTH);
    }

    public EnemyBullet(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        x -= speed;
        if (x < 0) {
            active = false;
        }
    }

    public void draw(Graphics g, int cameraX) {
        g.drawImage(bulletImage, x - cameraX, y, null);
    }
}
