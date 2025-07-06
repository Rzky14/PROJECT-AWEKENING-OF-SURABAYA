package menu;

import audio.SoundPlayer;
import deskripsi.Deskripsi;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import maps.Maps;
import minigame1.Gamepanel1;
import minigame2.GamePanel2;
import minigame3.GamePanel3;
import story.*;

public class MenuComponent extends JComponent {

    private final BaseFrame frame;
    private final JPanel mainPanel;
    private final Image backgroundImage;
    private static final String BG_MENU = "asset/background/BGMenu.png";
    private static final String SOUND_CLICK = "asset/sounds/click.wav";
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();

    public MenuComponent(BaseFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        backgroundImage = new ImageIcon(BG_MENU).getImage();
        soundPlayer.preloadSound(SOUND_CLICK);

        mainPanel = new JPanel(new GridLayout(1, 3, 30, 10));
        mainPanel.setBorder(new EmptyBorder(250, 150, 250, 150));
        mainPanel.setOpaque(false);

        mainPanel.add(createMenuButton("asset/Mulai.png", this::showPlayDialog));
        mainPanel.add(createMenuButton("asset/Deskripsi.png", () -> frame.setContent(new Deskripsi(() -> frame.setContent(new MenuComponent(frame))))));
        mainPanel.add(createMenuButton("asset/Keluar.png", () -> System.exit(0)));

        add(mainPanel, BorderLayout.CENTER);
    }

    private void showPlayDialog() {
        PlayDialog dialog = new PlayDialog(frame,
                () -> {
                    SaveGameManager.resetProgress();
                    Maps[] maps = new Maps[1];
                    maps[0] = createMaps(maps);
                    frame.setContent(new StoryAwal(() -> frame.setContent(maps[0])));
                },
                () -> {
                    int last = SaveGameManager.loadProgress();
                    if (last == -1) {
                        JOptionPane.showMessageDialog(null, "Belum ada progres tersimpan.");
                        return;
                    }
                    Maps[] maps = new Maps[1];
                    maps[0] = createMaps(maps);
                    for (int i = 0; i <= last; i++) {
                        maps[0].markGameDone(i);
                    }
                    frame.setContent(maps[0]);
                }
        );
        dialog.setVisible(true);
    }

    private JButton createMenuButton(String imgPath, Runnable action) {
        JButton button = new JButton(new ImageIcon(imgPath));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                soundPlayer.playSound(SOUND_CLICK);
            }
        });
        button.addActionListener(e -> action.run());
        return button;
    }

    private Maps createMaps(Maps[] mapsPanel) {
        return new Maps(
                () -> frame.setContent(new Story1(() -> {
                    Gamepanel1 game1 = new Gamepanel1(() -> {
                        mapsPanel[0].markGameDone(0);
                        SaveGameManager.saveProgress(0);
                        frame.setContent(new Story2(() -> rebuildMapsAfter(0)));
                    });
                    show(game1);
                })),
                () -> frame.setContent(new Story3(() -> {
                    GamePanel2 game2 = new GamePanel2();
                    game2.setOnWin(() -> {
                        mapsPanel[0].markGameDone(1);
                        SaveGameManager.saveProgress(1);
                        frame.setContent(new Story4(() -> rebuildMapsAfter(1)));
                    });
                    show(game2);
                })),
                () -> frame.setContent(new Story5(() -> {
                    GamePanel3 game3 = new GamePanel3();
                    game3.setOnWin(() -> {
                        SaveGameManager.resetProgress();
                        mapsPanel[0].markGameDone(2);
                        frame.setContent(new StoryAkhir()); 
                    });
                    show(game3);
                })),
                () -> frame.setContent(new MenuComponent(frame))
        );
    }

    private void rebuildMapsAfter(int index) {
        Maps[] updated = new Maps[1];
        updated[0] = createMaps(updated);
        for (int i = 0; i <= index; i++) {
            updated[0].markGameDone(i);
        }
        frame.setContent(updated[0]);
    }

    private void show(JComponent component) {
        frame.setContent(component);
        SwingUtilities.invokeLater(component::requestFocusInWindow);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}
