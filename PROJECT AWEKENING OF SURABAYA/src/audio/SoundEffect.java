package audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class SoundEffect {

    public static void play(String filepath) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(filepath));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Gagal memutar suara: " + filepath + " → " + e.getMessage());
        }
    }
}
