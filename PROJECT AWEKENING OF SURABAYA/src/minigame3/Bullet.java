package minigame3;

import java.awt.*;
import javax.swing.ImageIcon;

public class Bullet {

    public int x, y;
    public boolean active = true;
    public int speed = 25; // Sesuaikan nilai ini untuk mengubah kecepatan peluru player
    public final int width = 20;
    public final int height = 20;

    private static final Image bulletImage;

    // Static block: hanya dilakukan sekali untuk seluruh instance Bullet
    static {
        bulletImage = new ImageIcon("asset/bullet/peluru.png")
                .getImage()
                .getScaledInstance(20, 20, Image.SCALE_SMOOTH);
    }

    public Bullet(int startX, int startY, int speed) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
    }

    public void update() {
        x += speed;
        if (x > 10000) {
            active = false;
        }
    }

    public void draw(Graphics g, int cameraX) {
        g.drawImage(bulletImage, x - cameraX, y, null);
    }

    public boolean isActive() {
        return active;
    }
}
