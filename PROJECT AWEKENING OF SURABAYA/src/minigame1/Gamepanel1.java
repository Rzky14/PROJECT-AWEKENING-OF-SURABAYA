package minigame1;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class Gamepanel1 extends JPanel implements KeyListener, ActionListener {
    // Frame dan Papan Permainan
    private final int frameWidth = 1280;
    private final int frameHeight = 720;
    private final int rowCount = 21;
    private final int columnCount = 19;
    private final int tileSize = 32;
    private final int boardWidth = columnCount * tileSize;
    private final int boardHeight = rowCount * tileSize;
    private final int offsetX = (frameWidth - boardWidth) / 2;
    private final int offsetY = (frameHeight - boardHeight) / 2;
    private final SoundPlayer soundPlayer = SoundPlayer.getInstance();
    private static final String SOUND_CLICK = "asset/sounds/click.wav";

    // Timer dan Random
    private final Timer gameLoop;
    private final Random random = new Random();

    // Koleksi dan Entitas
    private HashSet<Block> walls;
    private HashSet<Block> radiox;
    private HashSet<Enemy1> enemies;
    private Player1 player;

    // Tombol dan Transisi
    private JButton restartButton;
    private final JButton nextButton;
    private Runnable onWin;
    private final Runnable onNext;
    private boolean startedBrightTransition = false;
    private boolean isTransitioningToBright = false;
    private float overlayAlpha = 1.0f;

    // Pesan Awal Game
    private final String startMessage = "Lari Dari Kejaran Dan Dapatkan Minimal 1 Radio Untuk Keluar";
    private String displayedStartMessage = "";
    private int startMessageIndex = 0;
    private boolean isShowingStartMessage = true;
    private final int startMessageDelay = 1; // jumlah frame delay sebelum tambah karakter
    private int startMessageDelayCounter = 0;
    private int messageDisplayHoldCounter = 0;  // penahan setelah pesan selesai
    private final int messageDisplayHoldFrames = 30;

    // Status Game
    private boolean showGameOverScreen = false;
    private boolean missionComplete = false;
    private int radio = 0;
    private int notificationAlpha = 0;

    // Auto Walk
    private boolean isAutoWalking = false;
    private final int autoWalkDistance = 370; // jarak auto walk dalam pixel
    private int autoWalkProgress = 0;
    private final int autoWalkSpeed = 6; // kecepatan auto walk (pixel/frame)

    // Transisi Teks Misi Selesai
    private boolean isMissionTextFadingIn = false;

    // Titik Finish
    private int finishX; // posisi X titik F
    private int finishY; // posisi Y titik F

    // Arah
    private final char[] directions = {'U', 'D', 'L', 'R'};

    // Gambar / Assets
    private Image wallImage;
    private Image mapsImage;
    private Image building1Image;
    private Image buildingImage;
    private Image RadioImage;
    private Image playerUpImage;
    private Image playerDownImage;
    private Image playerLeftImage;
    private Image playerRightImage;
    private Image backgroundImage;
    private Image enemy1UpImage;
    private Image enemy1RightImage;
    private Image enemy1DownImage;
    private Image enemy1LeftImage;
    private Image enemy2UpImage;
    private Image enemy2RightImage;
    private Image enemy2DownImage;
    private Image enemy2LeftImage;
    private Image enemy3UpImage;
    private Image enemy3RightImage;
    private Image enemy3DownImage;
    private Image enemy3LeftImage;
    private Image enemy4UpImage;
    private Image enemy4RightImage;
    private Image enemy4DownImage;
    private Image enemy4LeftImage;

    public Gamepanel1(Runnable onNext) {
        soundPlayer.preloadSound(SOUND_CLICK);
        this.onNext = onNext;
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        setFocusable(true);
        setLayout(null);

        loadResources();
        loadMap();
        resetPositions();
        restartButton = new JButton("RESTART");
        restartButton.setBounds(frameWidth / 2 - 75, frameHeight / 2 + 40, 150, 40);
        restartButton.setFont(new Font("Arial", Font.BOLD, 18));
        restartButton.setBackground(new Color(210, 180, 140)); // Warna dasar: Tan (coklat muda)
        restartButton.setForeground(Color.BLACK); // Teks hitam agar kontras
        restartButton.setFocusPainted(false);
        restartButton.setOpaque(true);
        restartButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2)); // Border: hitam

// Efek hover agar tombol terasa hidup
        restartButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                restartButton.setBackground(new Color(222, 184, 135)); // Burlywood
            }

            @Override
            public void mouseExited(MouseEvent e) {
                restartButton.setBackground(new Color(210, 180, 140)); // Kembali ke Tan
            }
        });

        restartButton.setVisible(false); // Hanya tampil saat game over
        restartButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            restartGame();                  // Panggil method restart game
            showGameOverScreen = false;
            restartButton.setVisible(false);
            requestFocusInWindow();
        });
        add(restartButton);

        nextButton = new JButton("NEXT");
        nextButton.setBounds(frameWidth / 2 - 75, frameHeight / 2 + 80, 150, 40);

        // Warna coklat muda dan teks hitam
        nextButton.setBackground(new Color(210, 180, 140)); // Tan
        nextButton.setForeground(Color.BLACK);
        nextButton.setFocusPainted(false);

        // Gaya font
        nextButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Border melengkung
        nextButton.setBorder(BorderFactory.createLineBorder(new Color(160, 82, 45), 2)); // Sienna

        // Hilangkan background default
        nextButton.setContentAreaFilled(true);
        nextButton.setOpaque(true);

        // Efek hover (ubah warna saat mouse masuk/keluar)
        nextButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nextButton.setBackground(new Color(222, 184, 135)); // Burlywood (lebih terang)
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nextButton.setBackground(new Color(210, 180, 140)); // Tan (awal)
            }
        });

        nextButton.setVisible(false);
        nextButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            if (onNext != null) {
                onNext.run();
            }
        });
        add(nextButton);

        SoundPlayer.getInstance().playBackgroundMusic("asset/sounds/Bs_Minigame1.wav"); // MODIFIED
        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        addKeyListener(this);
    }

    private void loadResources() {
        wallImage = new ImageIcon("asset/Properti/Genteng.png").getImage();
        buildingImage = new ImageIcon("asset/Properti/Genteng1.png").getImage();
        building1Image = new ImageIcon("asset/Properti/Genteng2.png").getImage();

        enemy1UpImage = new ImageIcon("asset/enemy/Belakang.png").getImage();
        enemy1DownImage = new ImageIcon("asset/enemy/Depan.png").getImage();
        enemy1RightImage = new ImageIcon("asset/enemy/Samping kanan.png").getImage();
        enemy1LeftImage = new ImageIcon("asset/enemy/Samping kiri.png").getImage();

        enemy2UpImage = new ImageIcon("asset/enemy/Belakang.png").getImage();
        enemy2DownImage = new ImageIcon("asset/enemy/Depan.png").getImage();
        enemy2RightImage = new ImageIcon("asset/enemy/Samping kanan.png").getImage();
        enemy2LeftImage = new ImageIcon("asset/enemy/Samping kiri.png").getImage();

        enemy3UpImage = new ImageIcon("asset/enemy/Belakang.png").getImage();
        enemy3DownImage = new ImageIcon("asset/enemy/Depan.png").getImage();
        enemy3RightImage = new ImageIcon("asset/enemy/Samping kanan.png").getImage();
        enemy3LeftImage = new ImageIcon("asset/enemy/Samping kiri.png").getImage();

        enemy4UpImage = new ImageIcon("asset/enemy/Belakang.png").getImage();
        enemy4DownImage = new ImageIcon("asset/enemy/Depan.png").getImage();
        enemy4RightImage = new ImageIcon("asset/enemy/Samping kanan.png").getImage();
        enemy4LeftImage = new ImageIcon("asset/enemy/Samping kiri.png").getImage();

        playerUpImage = new ImageIcon("asset/player/Belakang.png").getImage();
        playerDownImage = new ImageIcon("asset/player/Depan.png").getImage();
        playerLeftImage = new ImageIcon("asset/player/Samping Kiri.png").getImage();
        playerRightImage = new ImageIcon("asset/player/Samping Kanan.png").getImage();

        backgroundImage = new ImageIcon("asset/background/BGMinigame1.png").getImage();
        mapsImage = new ImageIcon("asset/Properti/Peta.png").getImage();
        RadioImage = new ImageIcon("asset/Properti/Radio.png").getImage();
    }

    private void loadMap() {
        walls = new HashSet<>();
        radiox = new HashSet<>();
        enemies = new HashSet<>();

        String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "Y        Y        B",
            "X YXYXYX X YYXXYY X",
            "X                 X",
            "X XX BBBXXXX X BB X",
            "X    X       X YY X",
            "XXXXXX XX XX X BB X",
            "B      XX XX X YY X",
            "B BBBXXXXrXX X    X",
            "B       bpo  X XXXX", // Added a space here
            "XXXX X BBBBB X XXXX", // Added a space here
            "     B       B    F",
            "XXXX X YYYYY X XXXX",
            "X        X   X    X",
            "X YY XXX X YYYYYY X",
            "X YY     P      X X",
            "X YY XXXYXXXBXX Y X",
            "Y        X      X X",
            "Y XXBBXX X XXYYXX X",
            "X       ad        X",
            "XXBBYYYXBBBXXXXXXXX",};

        finishX = 0;
        finishY = 0;

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char tile = tileMap[r].charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                switch (tile) {
                    case 'X' ->
                        walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                    case 'Y' ->
                        walls.add(new Block(building1Image, x, y, tileSize, tileSize));
                    case 'B' ->
                        walls.add(new Block(buildingImage, x, y, tileSize, tileSize));
                    case 'b' ->
                        enemies.add(new Enemy1(enemy1UpImage, enemy1DownImage, enemy1LeftImage, enemy1RightImage,
                                x, y, tileSize, tileSize, walls, tileSize));
                    case 'o' ->
                        enemies.add(new Enemy1(enemy2UpImage, enemy2DownImage, enemy2LeftImage, enemy2RightImage,
                                x, y, tileSize, tileSize, walls, tileSize));
                    case 'd' ->
                        enemies.add(new Enemy1(enemy3UpImage, enemy3DownImage, enemy3LeftImage, enemy3RightImage,
                                x, y, tileSize, tileSize, walls, tileSize));
                    case 'r' ->
                        enemies.add(new Enemy1(enemy4UpImage, enemy4DownImage, enemy4LeftImage, enemy4RightImage,
                                x, y, tileSize, tileSize, walls, tileSize));
                    case 'P' ->
                        player = new Player1(playerRightImage, x, y, tileSize, tileSize, walls, tileSize);
                    case 'F' -> {
                        finishX = x;
                        finishY = y;
                    }
                    case ' ' ->
                        radiox.add(new Block(RadioImage, x, y, tileSize, tileSize));
                }
            }
        }
        generateRandomRadiox(3);
    }

    private void generateRandomRadiox(int count) {
        radiox.clear();

        int tries = 0;
        while (radiox.size() < count && tries < 1000) {
            int c = random.nextInt(columnCount);
            int r = random.nextInt(rowCount);
            int x = c * tileSize;
            int y = r * tileSize;

            Block radioBlock = new Block(RadioImage, x, y, tileSize, tileSize);
            boolean valid = true;

            // Cek tabrakan dengan dinding
            for (Block wall : walls) {
                if (Block.collision(radioBlock, wall)) {
                    valid = false;
                    break;
                }
            }

            // Cek tabrakan dengan player
            if (player != null && Block.collision(radioBlock, player)) {
                valid = false;
            }

            // Cek tabrakan dengan musuh
            for (Enemy1 enemy : enemies) {
                if (Block.collision(radioBlock, enemy)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                radiox.add(radioBlock);
            }

            tries++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // 1. Gambar background utama game
        g2d.drawImage(backgroundImage, 0, 0, frameWidth, frameHeight, null);

        // 2. Gambar map dan objek lain
        drawMapAndObjects(g2d);

        // 3. Efek spotlight / overlay gelap saat misi belum selesai
        if (!missionComplete) {
            int centerX = player.x + offsetX + player.width / 2;
            int centerY = player.y + offsetY + player.height / 2;
            int spotlightRadius = 125;

            float[] dist = {0.0f, 1.0f};
            Color[] colors = {
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, (int) (overlayAlpha * 255))
            };
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point(centerX, centerY),
                    spotlightRadius,
                    dist,
                    colors,
                    MultipleGradientPaint.CycleMethod.NO_CYCLE
            );
            g2d.setPaint(rgp);
            g2d.fillRect(0, 0, frameWidth, frameHeight);
        } else {
            // Overlay gelap penuh untuk efek mission complete
            g2d.setColor(new Color(0, 0, 0, notificationAlpha));
            g2d.fillRect(0, 0, frameWidth, frameHeight);

            // Pesan misi selesai
            g2d.setColor(new Color(255, 255, 255, notificationAlpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String message = "MISI SELESAI, BERHASIL KABUR DARI PENJAJAH";
            int strWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (frameWidth - strWidth) / 2, frameHeight / 2);
        }

        // 4. Gambar player di atas map
        if (player != null) {
            g2d.drawImage(player.image, player.x + offsetX, player.y + offsetY, player.width, player.height, null);
        }

        // 5. Gambar peta kecil (mini map) di pojok kanan atas
        int mapWidth = 200;
        int mapHeight = 200;
        int mapX = frameWidth - mapWidth - 10;
        int mapY = 10;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2d.drawImage(mapsImage, mapX, mapY, mapWidth, mapHeight, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // 6. Tampilkan jumlah radio yang sudah diambil
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String radioText = "Radio = " + radio;
        int xRadio = 20;
        int yRadio = 40;
        g2d.drawString(radioText, xRadio, yRadio);

        if (showGameOverScreen) {
            // Latar semi-transparan gelap
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, frameWidth, frameHeight);

            // Judul "TERTANGKAP..." dengan warna coklat tua
            g2d.setColor(new Color(255, 255, 255)); // SaddleBrown
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String gameOverMsg = "TERTANGKAP...";
            int w = g2d.getFontMetrics().stringWidth(gameOverMsg);
            g2d.drawString(gameOverMsg, (frameWidth - w) / 2, frameHeight / 2 - 20);

            // Subjudul "PERLARIAN BUNG TOMO GAGAL" dengan warna coklat muda
            g2d.setColor(new Color(255, 255, 255)); // SaddleBrown
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String subMsg = "PERLARIAN BUNG TOMO GAGAL";
            int w2 = g2d.getFontMetrics().stringWidth(subMsg);
            g2d.drawString(subMsg, (frameWidth - w2) / 2, frameHeight / 2 + 20);
        }

        // 8. TERAKHIR: Tampilkan pesan overlay (instruksi) DI ATAS SEMUA ELEMEN
        if (isShowingStartMessage) {
            g2d.setColor(new Color(255, 255, 255));
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            int msgWidth = g2d.getFontMetrics().stringWidth(displayedStartMessage);
            int x = (frameWidth - msgWidth) / 2;
            int y = frameHeight / 2;
            g2d.drawString(displayedStartMessage, x, y);
            // Tidak ada return!
        }

        g2d.dispose();
    }

    private void drawMapAndObjects(Graphics2D g2d) {
        g2d.drawImage(backgroundImage, 0, 0, frameWidth, frameHeight, null);

        for (Enemy1 enemy : enemies) {
            g2d.drawImage(enemy.image, enemy.x + offsetX, enemy.y + offsetY, enemy.width, enemy.height, null);
        }

        for (Block wall : walls) {
            if (wall.image != null) {
                g2d.drawImage(wall.image, wall.x + offsetX, wall.y + offsetY, wall.width, wall.height, null);
            } else {
                g2d.setColor(Color.BLUE);
                g2d.fillRect(wall.x + offsetX, wall.y + offsetY, wall.width, wall.height);
            }
        }

        for (Block radioObj : radiox) {
            if (radioObj.image != null) {
                g2d.drawImage(radioObj.image, radioObj.x + offsetX, radioObj.y + offsetY, radioObj.width, radioObj.height, null);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRect(radioObj.x + offsetX, radioObj.y + offsetY, radioObj.width, radioObj.height);
            }
        }

    }

    public void setOnWin(Runnable onWin) {
        this.onWin = onWin;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    private void updateGame() {
        if (player == null) {
            return;
        }

        // Efek teks pesan mulai game
        if (isShowingStartMessage) {
            startMessageDelayCounter++;
            if (startMessageDelayCounter >= startMessageDelay) {
                startMessageDelayCounter = 0;
                if (startMessageIndex < startMessage.length()) {
                    displayedStartMessage += startMessage.charAt(startMessageIndex);
                    startMessageIndex++;
                } else {
                    // Tambahkan jeda 2 detik (40 frame) setelah semua teks tampil
                    messageDisplayHoldCounter++;
                    if (messageDisplayHoldCounter >= messageDisplayHoldFrames) {
                        isShowingStartMessage = false;
                    }
                }
            }
            repaint();
            return;  // hentikan update lain selama teks masih muncul
        }

        if (showGameOverScreen) {
            return;
        }

        // Kode lainnya untuk gameplay...
        // Jika player sampai finish tapi belum ambil radio (radio == 0)
        if (player.x >= finishX && player.y >= finishY && radio == 0) {
            // Batasi player agar tidak melewati garis finish (kecuali radio sudah diambil)
            if (player.x > finishX) {
                player.x = finishX;
            }
            if (player.y > finishY) {
                player.y = finishY;
            }
            // Tidak return supaya player masih bisa bergerak di sisa frame lain
        }

        // Mulai proses finish jika radio sudah diambil minimal 1
        if (!startedBrightTransition && player.x >= finishX && player.y >= finishY && radio >= 1) {
            startedBrightTransition = true;
        }

        // Transisi terang menurun
        if (startedBrightTransition && overlayAlpha > 0f) {
            overlayAlpha -= 0.05f;
            if (overlayAlpha < 0f) {
                overlayAlpha = 0f;
            }
        }

        // AUTO WALK
        if (isAutoWalking) {
            player.velocityX = autoWalkSpeed;
            player.velocityY = 0;
            player.x += player.velocityX;
            autoWalkProgress += player.velocityX;

            if (autoWalkProgress >= autoWalkDistance) {
                isAutoWalking = false;
                player.velocityX = 0;
                player.velocityY = 0;
                overlayAlpha = 1.0f;  // langsung gelap
                missionComplete = true;
                notificationAlpha = 0;
                isMissionTextFadingIn = true;
                // REMOVED: SoundPlayer.getInstance().stopBackgroundMusic();
            }
            return;
        }

        // TRANSISI BRIGHT KE MISSION COMPLETE
        if (isTransitioningToBright) {
            overlayAlpha -= 0.08f;
            if (overlayAlpha <= 0f) {
                overlayAlpha = 0f;
                isTransitioningToBright = false;
                missionComplete = true;
                notificationAlpha = 0;
                isMissionTextFadingIn = true;
                // Kunci posisi player
                player.velocityX = 0;
                player.velocityY = 0;
                // Return di sini juga untuk pastikan tidak lanjut ke bawah
                return;
            }
            return;
        }

        // FADING IN TEKS MISSION COMPLETE
        if (isMissionTextFadingIn) {
            if (notificationAlpha < 255) {
                notificationAlpha += 5;
            } else {
                notificationAlpha = 255;
                isMissionTextFadingIn = false;
                nextButton.setVisible(true);
                gameLoop.stop();

                // PANGGIL callback kalau ada!
                if (onWin != null) {
                    // Delay sebentar supaya user sempat baca pesan misi selesai
                    Timer t = new Timer(400, e -> onWin.run());
                    t.setRepeats(false);
                    t.start();
                }
            }
            repaint();
            return;
        }

        int oldX = player.x;
        int oldY = player.y;

        player.x += player.velocityX;
        player.y += player.velocityY;

        if (player.x < 0) {
            player.x = 0;
        }
        if (player.y < 0) {
            player.y = 0;
        }
        if (player.y + player.height > frameHeight) {
            player.y = frameHeight - player.height;
        }

        // Mulai auto walking saat player sudah finish dan radio >= 1
        if (player.x >= finishX && player.y >= finishY && !isAutoWalking && !missionComplete && radio >= 1) {
            isAutoWalking = true;
            autoWalkProgress = 0;
            player.updateDirection('R');
            player.image = playerRightImage;
            return;
        }

        // Cek tabrakan dengan dinding
        for (Block wall : walls) {
            if (Block.collision(player, wall)) {
                player.x = oldX;
                player.y = oldY;
                break;
            }
        }

        for (Enemy1 enemy : enemies) {
            if (Block.collision(player, enemy)) {
                player.playHurtSound();              // <-- Panggil suara di sini!
                radio = 0;
                showGameOverScreen = true;
                restartButton.setVisible(true);
                gameLoop.stop();
                repaint();
                return;
            }
        }

        // Update posisi musuh
        for (Enemy1 enemy : enemies) {
            enemy.x += enemy.velocityX;
            enemy.y += enemy.velocityY;

            boolean collided = false;
            for (Block wall : walls) {
                if (Block.collision(enemy, wall)) {
                    collided = true;
                    break;
                }
            }

            if (collided || enemy.x <= 0 || enemy.x + enemy.width >= boardWidth
                    || enemy.y <= 0 || enemy.y + enemy.height >= boardHeight) {
                enemy.x -= enemy.velocityX;
                enemy.y -= enemy.velocityY;
                enemy.updateDirection(directions[random.nextInt(directions.length)]);
            }
        }

        // Ambil radio jika bersentuhan dengan player
        Block foundRadio = null;
        for (Block r : radiox) {
            if (Block.collision(player, r)) {
                foundRadio = r;
                radio += 1;
                player.playPickRadioSound();
                break;
            }
        }
        if (foundRadio != null) {
            radiox.remove(foundRadio);
        }

        // Reload map jika radio habis dan kurang dari 1 radio sudah diambil
        if (radiox.isEmpty() && radio < 1) {
            loadMap();
            resetPositions();
        }
    }

    public void restartGame() {
        radio = 0;
        missionComplete = false;
        showGameOverScreen = false;
        notificationAlpha = 0;
        isTransitioningToBright = false;
        isMissionTextFadingIn = false;
        overlayAlpha = 1.0f;
        isAutoWalking = false;
        autoWalkProgress = 0;
        restartButton.setVisible(false);
        nextButton.setVisible(false);
        loadMap();
        resetPositions();
        repaint();
        gameLoop.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (player == null) {
            return;
        }
        if (isTransitioningToBright || missionComplete || isAutoWalking) {
            return;
        }

        player.velocityX = 0;
        player.velocityY = 0;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> {
                player.updateDirection('U');
                player.image = playerUpImage;
            }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
                player.updateDirection('D');
                player.image = playerDownImage;
            }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
                player.updateDirection('L');
                player.image = playerLeftImage;
            }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
                player.updateDirection('R');
                player.image = playerRightImage;
            }
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (player == null) {
            return;
        }
        if (isTransitioningToBright || missionComplete || isAutoWalking) {
            return;
        }

        player.stopMoving();
        player.velocityX = 0;
        player.velocityY = 0;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void resetPositions() {
        if (player != null) {
            player.x = 0;
            player.y = (frameHeight / 2) - (player.height / 2);
            player.velocityX = 0;
            player.velocityY = 0;
        }

        for (Enemy1 enemy : enemies) {
            enemy.reset();
            enemy.updateDirection(randomDirection());
        }
    }

    private char randomDirection() {
        return directions[new Random().nextInt(directions.length)];
    }
}
