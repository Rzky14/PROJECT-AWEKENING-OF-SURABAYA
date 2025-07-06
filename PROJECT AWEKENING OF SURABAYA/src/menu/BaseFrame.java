package menu;

import audio.SoundPlayer;
import java.awt.*;
import java.io.File;
import javax.swing.*;

public class BaseFrame extends JFrame {

    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;

    public BaseFrame(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setIconImage(new ImageIcon(new File("asset/AoS.png").getAbsolutePath()).getImage());

        JPanel dummy = new JPanel();
        dummy.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setContentPane(dummy);
        pack();
        setLocationRelativeTo(null);

        SoundPlayer.getInstance().playBackgroundMusic("asset/sounds/soundtrack.wav", 0.0f);
        SoundPlayer.getInstance().fadeBackgroundMusic(1.0f, 1000);

    }

    public void setContent(JComponent component) {
        component.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setContentPane(component);
        pack();
        revalidate();
        repaint();

        String currentBackgroundMusicPath = null;
        float targetVolume = 1.0f;

        if (component instanceof menu.MenuComponent || // Main Menu
            component instanceof deskripsi.Deskripsi || // Deskripsi
            component instanceof maps.Maps || // Maps
            component instanceof story.Story1 || // Story 1
            component instanceof story.Story2 || // Story 2
            component instanceof story.Story3 || // Story 3
            component instanceof story.Story4 || // Story 4
            component instanceof story.Story5 || // Story 5
            component instanceof story.StoryAkhir) { // Story Akhir
            currentBackgroundMusicPath = "asset/sounds/soundtrack.wav";
            targetVolume = 1.0f; // Full volume for soundtrack

        } else if (component instanceof story.StoryAwal) { // Story Awal
            currentBackgroundMusicPath = "asset/sounds/soundtrack.wav";
            targetVolume = 0.0f; // Mute it
        } else if (component instanceof minigame1.Gamepanel1) { // Minigame 1
            currentBackgroundMusicPath = "asset/sounds/Bs_Minigame1.wav";
            targetVolume = 1.0f;
        } else if (component instanceof minigame2.GamePanel2) { // Minigame 2
            currentBackgroundMusicPath = "asset/sounds/soundtrack01.wav";
            targetVolume = 1.0f;
        } else if (component instanceof minigame3.GamePanel3) { // Minigame 3
            currentBackgroundMusicPath = "asset/sounds/backsound.wav";
            targetVolume = 1.0f;
        }


        // Manage background music transitions
        if (currentBackgroundMusicPath != null) {
            if (!SoundPlayer.getInstance().getCurrentBackgroundMusicPath().equals(currentBackgroundMusicPath)) {
                SoundPlayer.getInstance().playBackgroundMusic(currentBackgroundMusicPath, 0.0f);
            }
            SoundPlayer.getInstance().fadeBackgroundMusic(targetVolume, 500);
        } else {
            SoundPlayer.getInstance().fadeBackgroundMusic(0.0f, 500);
        }
    }
}