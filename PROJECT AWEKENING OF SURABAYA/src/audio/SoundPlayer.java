package audio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.*;
import javax.sound.sampled.*;

public class SoundPlayer {

    private static SoundPlayer instance;
    private final HashMap<String, Clip> clips = new HashMap<>();
    private final HashMap<String, Clip> loopingClips = new HashMap<>();
    private Clip backgroundClip;
    private String currentBackgroundMusicPath = "";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private SoundPlayer() {
    }

    public static SoundPlayer getInstance() {
        if (instance == null) {
            instance = new SoundPlayer();
        }
        return instance;
    }

    public void preloadSound(String path) {
        if (!clips.containsKey(path)) {
            Clip clip = loadClip(path);
            if (clip != null) {
                clips.put(path, clip);
            }
        }
    }

    public void playSound(String path) {
        Clip clip = clips.get(path);
        if (clip == null || !clip.isOpen()) {
            clip = loadClip(path);
            if (clip == null) {
                return;
            }
            clips.put(path, clip);
        }
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    public void stopSound(String path) {
        Clip clip = clips.remove(path);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        }
    }

    public void playLoopingSound(String key, String path) {
        if (loopingClips.containsKey(key) && loopingClips.get(key).isRunning()) {
            return;
        }

        Clip clip = loadClip(path);
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            loopingClips.put(key, clip);
        }
    }

    public void stopLoopingSound(String key) {
        Clip clip = loopingClips.remove(key);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        }
    }

    public void playBackgroundMusic(String path) {
        playBackgroundMusic(path, 1.0f);
    }

    public void playBackgroundMusic(String path, float volume) {
        if (backgroundClip != null && backgroundClip.isRunning() && currentBackgroundMusicPath.equals(path)) {
            setBackgroundMusicVolume(volume);
            return;
        }

        stopBackgroundMusic();
        backgroundClip = loadClip(path);
        if (backgroundClip != null) {
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
            currentBackgroundMusicPath = path;
            setBackgroundMusicVolume(volume);
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundClip != null) {
            if (backgroundClip.isRunning()) {
                backgroundClip.stop();
            }
            backgroundClip.close();
            backgroundClip = null;
            currentBackgroundMusicPath = "";
        }
    }

    public String getCurrentBackgroundMusicPath() {
        return currentBackgroundMusicPath;
    }

    public void fadeInBackgroundMusic(String path, int durationMs) {
        stopBackgroundMusic();
        backgroundClip = loadClip(path);
        if (backgroundClip != null) {
            try {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = 0.0f;
                int steps = 15;
                int interval = durationMs / steps;

                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundClip.start();

                for (int i = 0; i <= steps; i++) {
                    final int step = i;
                    scheduler.schedule(() -> {
                        float gain = min + (max - min) * step / steps;
                        gainControl.setValue(gain);
                    }, step * interval, TimeUnit.MILLISECONDS);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println("Fade in gagal: " + e.getMessage());
            }
        }
    }

    public void fadeOutBackgroundMusic(int durationMs) {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            try {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = gainControl.getValue();
                int steps = 15;
                int interval = durationMs / steps;

                for (int i = steps; i >= 0; i--) {
                    final int step = i;
                    scheduler.schedule(() -> {
                        float gain = min + (max - min) * step / steps;
                        gainControl.setValue(gain);
                        if (step == 0) {
                            backgroundClip.stop();
                            backgroundClip.close();
                            backgroundClip = null;
                        }
                    }, (steps - step) * interval, TimeUnit.MILLISECONDS);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println("Fade out gagal: " + e.getMessage());
            }
        }
    }

    public void setBackgroundMusicVolume(float volume) {
        if (backgroundClip != null && backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = 0.0f;
            float gain = min + (max - min) * volume;
            gainControl.setValue(gain);
        }
    }

    public void fadeBackgroundMusic(float targetVolume, int durationMs) {
        if (backgroundClip != null && backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = 0.0f;
                float currentGain = gainControl.getValue();
                float currentVolume = (currentGain - min) / (max - min);
                int steps = 20;
                int interval = durationMs / steps;

                for (int i = 0; i <= steps; i++) {
                    final int step = i;
                    scheduler.schedule(() -> {
                        float vol = currentVolume + (targetVolume - currentVolume) * step / steps;
                        float gain = min + (max - min) * vol;
                        gainControl.setValue(gain);
                    }, step * interval, TimeUnit.MILLISECONDS);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println("Gagal fade background music: " + e.getMessage());
            }
        }
    }

    private Clip loadClip(String path) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(path));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Gagal load suara: " + path + " → " + e.getMessage());
            return null;
        }
    }
}
