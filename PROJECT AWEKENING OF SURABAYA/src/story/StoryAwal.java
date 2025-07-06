package story;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public class StoryAwal extends JPanel {

    private static final String TEKS = "PERLAWANAN RAKYAT SURABAYA MELAWAN PARA PENJAJAH UNTUK MERDEKA";
    private static final String BG_IMAGE = "asset/story/BGTeks.png";
    private static final String SOUND_CLICK = "asset/sounds/click.wav";
    private static final String SOUND_TYPE = "asset/sounds/type.wav";

    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private final JTextPane narasiArea = new JTextPane();
    private Timer typingTimer;
    private int charIndex = 0;

    public StoryAwal(Runnable onSelesai) {
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));
        setBackground(Color.BLACK);

        JLabel background = new JLabel(new ImageIcon(new ImageIcon(BG_IMAGE).getImage().getScaledInstance(1280, 720, Image.SCALE_SMOOTH)));
        background.setBounds(0, 0, 1280, 720);
        add(background);

        narasiArea.setBounds(140, 285, 1000, 200);
        narasiArea.setOpaque(false);
        narasiArea.setForeground(new Color(0x906b4c));
        narasiArea.setFont(new Font("Arial", Font.BOLD, 36));
        narasiArea.setEditable(false);
        narasiArea.setFocusable(false);
        centerText(narasiArea);
        add(narasiArea);
        setComponentZOrder(narasiArea, 0);

        soundPlayer.preloadSound(SOUND_CLICK);
        soundPlayer.preloadSound(SOUND_TYPE);

        startTypingEffect();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && !typingTimer.isRunning()) {
                    soundPlayer.playSound(SOUND_CLICK);
                    onSelesai.run();
                }
            }
        });

        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void centerText(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setAlignment(attr, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), attr, false);
    }

    private void startTypingEffect() {
        narasiArea.setText("");
        charIndex = 0;
        final int TYPING_SPEED = 70;
        final int SOUND_INTERVAL = 450;
        final long[] lastSoundTime = {0};

        typingTimer = new Timer(TYPING_SPEED, e -> {
            if (charIndex < TEKS.length()) {
                try {
                    narasiArea.getDocument().insertString(narasiArea.getDocument().getLength(),
                            String.valueOf(TEKS.charAt(charIndex)), null);
                    long now = System.currentTimeMillis();
                    if (now - lastSoundTime[0] >= SOUND_INTERVAL) {
                        soundPlayer.playSound(SOUND_TYPE);
                        lastSoundTime[0] = now;
                    }
                    charIndex++;
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            } else {
                typingTimer.stop();
                soundPlayer.stopSound(SOUND_TYPE);
            }
        });
        typingTimer.start();
    }
}
