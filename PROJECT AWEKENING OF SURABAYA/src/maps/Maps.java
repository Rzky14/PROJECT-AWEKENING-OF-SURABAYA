package maps;

import audio.SoundPlayer;
import java.awt.*;
import javax.swing.*;

public class Maps extends JPanel {

    private final JButton[] gameButtons = new JButton[3];
    private final String[] status = {"active", "locked", "locked"};
    private final Image backgroundImage;
    private static final String SOUND_CLICK = "asset/sounds/click.wav";
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    public Maps(Runnable onGame1, Runnable onGame2, Runnable onGame3, Runnable onBack) {
        setLayout(null);
        backgroundImage = new ImageIcon("asset/background/BGMaps.png").getImage();
        soundPlayer.preloadSound(SOUND_CLICK);

        Runnable[] actions = {onGame1, onGame2, onGame3};
        int[][] pos = {{335, 170}, {590, 350}, {910, 425}};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JButton btn = new JButton();
            btn.setBounds(pos[i][0], pos[i][1], 100, 100);
            setupButton(btn);
            btn.addActionListener(e -> {
                if ("active".equals(status[idx])) {
                    soundPlayer.playSound(SOUND_CLICK);
                    actions[idx].run();
                }
            });
            gameButtons[i] = btn;
            add(btn);
        }

        JButton backBtn = new JButton(new ImageIcon(scaleImage("asset/Back.png", 80, 80)));
        backBtn.setBounds(40, 620, 80, 80);
        setupButton(backBtn);
        backBtn.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            onBack.run();
        });
        add(backBtn);

        refreshIcons();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
    }

    public void markGameDone(int index) {
        if (index >= 0 && index < status.length) {
            status[index] = "done";
            if (index + 1 < status.length) {
                status[index + 1] = "active";
            }
            refreshIcons();
        }
    }

    private void refreshIcons() {
        for (int i = 0; i < gameButtons.length; i++) {
            String icon = switch (status[i]) {
                case "done" ->
                    "asset/Done.png";
                case "active" ->
                    "asset/Point.png";
                default ->
                    "asset/Lock.png";
            };
            gameButtons[i].setIcon(new ImageIcon(scaleImage(icon, 60, 60)));
        }
    }

    private void setupButton(JButton button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
    }

    private Image scaleImage(String path, int w, int h) {
        return new ImageIcon(path).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }
}
