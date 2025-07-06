package story;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import menu.MenuComponent;

public class StoryAkhir extends JPanel {

    private final ImageIcon[] gambarStory = {
        new ImageIcon("asset/story/28.png"),
        new ImageIcon("asset/story/29.png"),
        new ImageIcon("asset/story/30.png")
    };
    private Image currentImage;
    private int index = 0;

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private static final String SOUND_CLICK = "asset/sounds/click.wav";

    private float fadeAlpha = 0.0f;
    private boolean fadingOut = false;
    private boolean showEndScreen = false;
    private Timer fadeTimer;
    private JButton selesaiButton;

    public StoryAkhir() {
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));
        setBackground(Color.BLACK);

        soundPlayer.preloadSound(SOUND_CLICK);

        tampilkanGambar(); // Panggil ini untuk mengatur currentImage, sekarang dengan MediaTracker

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    soundPlayer.playSound(SOUND_CLICK);
                    index++;
                    if (index >= gambarStory.length) {
                        mulaiFadeOut();
                    } else {
                        tampilkanGambar();
                    }
                }
            }
        });

        setFocusable(true);
        // MODIFIKASI: Gunakan Timer untuk menunda permintaan fokus sedikit
        Timer focusTimer = new Timer(50, e -> { // Menunda 50 milidetik
            requestFocusInWindow();
            ((Timer)e.getSource()).stop(); // Hentikan timer setelah berjalan sekali
        });
        focusTimer.setRepeats(false); // Pastikan timer hanya berjalan sekali
        focusTimer.start(); // Mulai timer
    }

    private void tampilkanGambar() {
        try {
            Image img = gambarStory[index].getImage().getScaledInstance(1280, 720, Image.SCALE_SMOOTH);

            // Gunakan MediaTracker untuk memastikan gambar dimuat sepenuhnya
            MediaTracker tracker = new MediaTracker(this); // 'this' merujuk ke JPanel
            tracker.addImage(img, 0); // Menambahkan gambar ke tracker dengan ID 0
            tracker.waitForID(0); // Menunggu hingga gambar dengan ID 0 selesai dimuat

            currentImage = img;
        } catch (InterruptedException e) {
            System.err.println("Pemuatan gambar StoryAkhir terinterupsi: " + e.getMessage());
            Thread.currentThread().interrupt(); // Set kembali status interupsi
            currentImage = null; // Set gambar ke null jika terjadi kesalahan
        } catch (Exception e) {
            System.err.println("Gagal memuat gambar untuk StoryAkhir: " + gambarStory[index] + " -> " + e.getMessage());
            currentImage = null; // Set gambar ke null jika terjadi kesalahan
        }
        repaint(); // Pastikan panel digambar ulang setelah gambar dimuat
    }

    private void mulaiFadeOut() {
        fadingOut = true;
        fadeAlpha = 0.0f;

        fadeTimer = new Timer(30, e -> {
            fadeAlpha = Math.min(fadeAlpha + 0.02f, 1.0f);
            repaint();
            if (fadeAlpha >= 1.0f) {
                fadeTimer.stop();
                showEndScreen = true;
                tampilkanTeksDanTombol();
            }
        });
        fadeTimer.start();
    }

    private void tampilkanTeksDanTombol() {
        removeAll();  // Bersihkan panel
        // Set layout dan background lagi karena removeAll bisa meresetnya
        setLayout(null);
        setBackground(Color.BLACK);

        // Teks misi selesai
        JLabel labelText = new JLabel("PERJUANGAN RAKYAT SURABAYA TELAH BERHASIL", SwingConstants.CENTER);
        labelText.setFont(new Font("Arial", Font.BOLD, 28));
        labelText.setForeground(new Color(255, 255, 255));
        labelText.setBounds(0, 260, 1280, 40);
        add(labelText);

        // Tombol NEXT
        // Tombol NEXT (disamakan seperti di GamePanel1)
        selesaiButton = new JButton("TAMAT");
        selesaiButton.setBounds(540, 320, 200, 40);

        // Warna coklat muda dan teks hitam
        selesaiButton.setBackground(new Color(210, 180, 140)); // Tan
        selesaiButton.setForeground(Color.BLACK);
        selesaiButton.setFocusPainted(false);

        // Gaya font
        selesaiButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Border melengkung
        selesaiButton.setBorder(BorderFactory.createLineBorder(new Color(160, 82, 45), 2)); // Sienna

        // Hilangkan background default
        selesaiButton.setContentAreaFilled(true);
        selesaiButton.setOpaque(true);

        // Efek hover (ubah warna saat mouse masuk/keluar)
        selesaiButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                selesaiButton.setBackground(new Color(222, 184, 135)); // Burlywood (lebih terang)
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                selesaiButton.setBackground(new Color(210, 180, 140)); // Tan (awal)
            }
        });

        // Aksi saat ditekan (kembali ke menu)
        selesaiButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK); // Tambahkan efek suara juga jika ingin mirip
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            frame.setContentPane(new MenuComponent((menu.BaseFrame) frame));
            frame.revalidate();
            frame.repaint();
        });

        add(selesaiButton);
        revalidate();
        repaint();
        selesaiButton.requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (currentImage != null) {
            g2d.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
        }

        if (fadingOut || showEndScreen) {
            float alpha = Math.min(fadeAlpha, 1.0f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
}