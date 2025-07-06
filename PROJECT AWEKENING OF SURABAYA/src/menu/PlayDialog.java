package menu;

import audio.SoundPlayer;
import java.awt.*;
import javax.swing.*;

public class PlayDialog extends JDialog {

    private static final int DIALOG_WIDTH = 640;
    private static final int DIALOG_HEIGHT = 240;
    private static final String SOUND_CLICK = "asset/sounds/click.wav";
    private final SoundPlayer mySoundPlayer = SoundPlayer.getInstance();

    public PlayDialog(JFrame parent, Runnable onNewGame, Runnable onContinue) {
        super(parent, "", true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(parent);
        mySoundPlayer.preloadSound(SOUND_CLICK);

        Image bg = loadImage("asset/background/BGDialog.png", DIALOG_WIDTH, DIALOG_HEIGHT);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 40, 40, 40));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createButton("asset/btn_Pbaru.png", onNewGame));
        buttonPanel.add(createButton("asset/btn_lanjut.png", onContinue));

        panel.add(Box.createVerticalStrut(55));
        panel.add(buttonPanel);
        setContentPane(panel);
    }

    private JButton createButton(String imgPath, Runnable action) {
        JButton button = new JButton(new ImageIcon(loadImage(imgPath, 230, 50)));
        button.setPreferredSize(new Dimension(230, 50));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.addActionListener(e -> {
            mySoundPlayer.playSound(SOUND_CLICK);
            action.run();
            dispose();
        });
        return button;
    }

    private Image loadImage(String path, int width, int height) {
        return new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}
