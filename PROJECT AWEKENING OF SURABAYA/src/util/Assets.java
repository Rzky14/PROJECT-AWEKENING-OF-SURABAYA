package util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Pemuat gambar yang aman: jika berkas tidak ada atau rusak, game tetap berjalan
 * dengan gambar pengganti dan pesan peringatan di konsol (satu kali per berkas).
 */
public final class Assets {

    private static final Set<String> reported = new HashSet<>();

    private Assets() {
    }

    /** Memuat gambar dengan ukuran aslinya. */
    public static Image image(String path) {
        return image(path, 0, 0);
    }

    /** Memuat gambar dan menskalakannya; width/height 0 berarti ukuran asli. */
    public static Image image(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        boolean loaded = icon.getImageLoadStatus() == MediaTracker.COMPLETE
                && icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
        if (!loaded) {
            report(path);
            return placeholder(width > 0 ? width : 64, height > 0 ? height : 64);
        }
        BufferedImage source = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = source.createGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.dispose();
        if (width > 0 && height > 0) {
            return scale(source, width, height);
        }
        return source;
    }

    /** Skala halus yang langsung jadi (tidak menunggu pemuatan asinkron), turun bertahap agar tetap tajam. */
    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();
        while (w > width * 2 || h > height * 2) {
            w = Math.max(width, w / 2);
            h = Math.max(height, h / 2);
            current = resample(current, w, h);
        }
        return resample(current, width, height);
    }

    private static BufferedImage resample(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return target;
    }

    /** Memuat gambar sebagai BufferedImage (untuk membaca piksel). */
    public static BufferedImage buffered(String path, int fallbackWidth, int fallbackHeight) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img != null) {
                return img;
            }
        } catch (IOException e) {
            // dilaporkan di bawah
        }
        report(path);
        return placeholder(fallbackWidth, fallbackHeight);
    }

    private static void report(String path) {
        if (reported.add(path)) {
            System.err.println("Aset tidak dapat dimuat: " + path);
        }
    }

    private static BufferedImage placeholder(int width, int height) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(120, 120, 120, 160));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(new Color(255, 255, 255, 200));
        g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
        g.drawLine(0, 0, img.getWidth() - 1, img.getHeight() - 1);
        g.drawLine(img.getWidth() - 1, 0, 0, img.getHeight() - 1);
        g.dispose();
        return img;
    }
}
