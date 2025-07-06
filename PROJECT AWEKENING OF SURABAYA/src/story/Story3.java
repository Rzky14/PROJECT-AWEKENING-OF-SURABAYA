package story;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class Story3 extends JPanel {

    private final String[] imgStory = {
        "asset/story/16.png", "asset/story/17.png", "asset/story/18.png",
        "asset/story/19.png", "asset/story/20.png"
    };

    private int index = 0;
    private final JLabel label = new JLabel();
    private static final String SOUND_CLICK = "asset/sounds/click.wav";
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    public Story3(Runnable onSelesai) {
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));
        setBackground(Color.BLACK);

        soundPlayer.preloadSound(SOUND_CLICK);
        label.setBounds(0, 0, 1280, 720);
        add(label);
        tampilkanGambar();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    soundPlayer.playSound(SOUND_CLICK);
                    if (++index >= imgStory.length) {
                        onSelesai.run();
                    } else {
                        tampilkanGambar();
                    }
                }
            }
        });

        setFocusable(true);
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void tampilkanGambar() {
        Image img = new ImageIcon(imgStory[index]).getImage().getScaledInstance(1280, 720, Image.SCALE_SMOOTH);
        label.setIcon(new ImageIcon(img));
    }
}
