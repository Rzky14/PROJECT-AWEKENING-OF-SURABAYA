package deskripsi;

import audio.SoundPlayer;
import java.awt.*;
import javax.swing.*;

public class Deskripsi extends JPanel {

    private static final String BG_DESC = "asset/background/BGDeskripsi.png";
    private static final String TOMBOL_KEMBALI = "asset/Back.png";
    private static final String SOUND_CLICK = "asset/sounds/click.wav";

    private final Image backgroundImage;
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    public Deskripsi(Runnable onBack) {
        setLayout(null);
        backgroundImage = new ImageIcon(BG_DESC).getImage();
        soundPlayer.preloadSound(SOUND_CLICK);

        JButton backButton = new JButton(new ImageIcon(
                new ImageIcon(TOMBOL_KEMBALI).getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        backButton.setBounds(40, 620, 80, 80);
        setupButton(backButton);
        backButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            onBack.run();
        });

        add(backButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
    }

    private void setupButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
    }
}
