package minigame1;

import audio.SoundPlayer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import util.Assets;
import util.FixedStep;

public class Gamepanel1 extends JPanel implements KeyListener, ActionListener {

    private enum State {
        INTRO, PLAYING, ESCAPING, MISSION_TEXT, WON, GAME_OVER
    }

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

    // Pengaturan permainan
    private static final int TURN_ASSIST = 16;            // toleransi (px) saat berbelok di persimpangan
    private static final int RADIO_COUNT = 3;
    private static final int INVULNERABLE_FRAMES = 150;   // kebal singkat setelah tertangkap
    private static final int DANGER_RANGE = 128;          // jarak (px) musuh mulai memberi peringatan

    // Waktu: pembaruan tetap 60 kali per detik
    private final Timer gameLoop;
    private final FixedStep clock = new FixedStep();
    private final Random random = new Random();
    private int frame = 0;

    // Koleksi dan Entitas
    private HashSet<Block> walls;
    private boolean[][] wallGrid;
    private final List<Block> radiox = new ArrayList<>();
    private HashSet<Enemy1> enemies;
    private Player1 player;

    // Tombol dan Transisi
    private final JButton restartButton;
    private final JButton nextButton;
    private Runnable onWin;
    private final Runnable onNext;
    private float overlayAlpha = 1.0f;

    // Status Game
    private State state = State.INTRO;
    private int radio = 0;
    private int notificationAlpha = 0;
    private boolean playerStarted = false;   // musuh baru keluar setelah pemain mulai bergerak
    private int enemyClock = 0;
    private int invulnerable = 0;
    private char playerFacing = 'R';
    private float danger = 0f;

    // Pesan Awal Game
    private final String startMessage = "Lari Dari Kejaran Dan Dapatkan Minimal 1 Radio Untuk Keluar";
    private String displayedStartMessage = "";
    private int startMessageIndex = 0;
    private int startMessageTick = 0;
    private int messageHoldCounter = 0;
    private final int messageHoldFrames = 90;

    // Umpan balik visual
    private String bannerText = "";
    private int bannerTimer = 0;
    private int bannerDuration = 1;
    private int hitFlash = 0;
    private int exitHintCooldown = 0;
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // Auto Walk (adegan keluar)
    private final int autoWalkDistance = 370;
    private final int autoWalkSpeed = 3;
    private int autoWalkProgress = 0;

    // Titik Finish
    private int finishX;
    private int finishY;

    // Kontrol: arah yang sedang ditahan, yang terakhir ditekan diprioritaskan
    private final LinkedList<Character> heldDirs = new LinkedList<>();

    // Gambar / Assets
    private Image wallImage;
    private Image mapsImage;
    private Image building1Image;
    private Image buildingImage;
    private Image radioImage;
    private Image playerUpImage;
    private Image playerDownImage;
    private Image playerLeftImage;
    private Image playerRightImage;
    private Image backgroundImage;
    private Image enemyUpImage;
    private Image enemyRightImage;
    private Image enemyDownImage;
    private Image enemyLeftImage;

    private static class FloatingText {

        final String text;
        final Color color;
        float x;
        float y;
        int life;
        final int maxLife;

        FloatingText(String text, float x, float y, Color color, int life) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }
    }

    public Gamepanel1(Runnable onNext) {
        soundPlayer.preloadSound(SOUND_CLICK);
        this.onNext = onNext;
        setPreferredSize(new Dimension(frameWidth, frameHeight));
        setFocusable(true);
        setLayout(null);
        addKeyListener(this);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                heldDirs.clear();
            }
        });

        loadResources();
        loadMap();

        restartButton = createButton("RESTART", frameWidth / 2 - 75, frameHeight / 2 + 40, 150, 40, Color.BLACK, 18);
        restartButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            restartGame();
            requestFocusInWindow();
        });
        add(restartButton);

        nextButton = createButton("NEXT", frameWidth / 2 - 75, frameHeight / 2 + 80, 150, 40, new Color(160, 82, 45), 16);
        nextButton.addActionListener(e -> {
            soundPlayer.playSound(SOUND_CLICK);
            if (onNext != null) {
                onNext.run();
            }
        });
        add(nextButton);

        SoundPlayer.getInstance().playBackgroundMusic("asset/sounds/Bs_Minigame1.wav");
        gameLoop = new Timer(8, this);
        clock.reset();
        gameLoop.start();
    }

    private JButton createButton(String text, int x, int y, int w, int h, Color border, int fontSize) {
        final Color normal = new Color(210, 180, 140);     // Tan
        final Color hover = new Color(222, 184, 135);      // Burlywood
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.setFont(new Font("Arial", Font.BOLD, fontSize));
        button.setBackground(normal);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(border, 2));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });
        button.setVisible(false);
        return button;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (gameLoop != null && !gameLoop.isRunning() && state != State.WON && state != State.GAME_OVER) {
            clock.reset();
            gameLoop.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (gameLoop != null) {
            gameLoop.stop();
        }
        player.stopMoving();
    }

    private void loadResources() {
        wallImage = Assets.image("asset/Properti/Genteng.png");
        buildingImage = Assets.image("asset/Properti/Genteng1.png");
        building1Image = Assets.image("asset/Properti/Genteng2.png");

        enemyUpImage = Assets.image("asset/enemy/Belakang.png");
        enemyDownImage = Assets.image("asset/enemy/Depan.png");
        enemyRightImage = Assets.image("asset/enemy/Samping Kanan.png");
        enemyLeftImage = Assets.image("asset/enemy/Samping Kiri.png");

        playerUpImage = Assets.image("asset/player/Belakang.png");
        playerDownImage = Assets.image("asset/player/Depan.png");
        playerLeftImage = Assets.image("asset/player/Samping kiri.png");
        playerRightImage = Assets.image("asset/player/Samping Kanan.png");

        backgroundImage = Assets.image("asset/background/BGMinigame1.png", frameWidth, frameHeight);
        mapsImage = Assets.image("asset/Properti/Peta.png");
        radioImage = Assets.image("asset/Properti/Radio.png");
    }

    private void loadMap() {
        walls = new HashSet<>();
        enemies = new HashSet<>();
        wallGrid = new boolean[rowCount][columnCount];
        radiox.clear();

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
            "B       bpo  X XXXX",
            "XXXX X BBBBB X XXXX",
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
                    case 'X' -> {
                        walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                        wallGrid[r][c] = true;
                    }
                    case 'Y' -> {
                        walls.add(new Block(building1Image, x, y, tileSize, tileSize));
                        wallGrid[r][c] = true;
                    }
                    case 'B' -> {
                        walls.add(new Block(buildingImage, x, y, tileSize, tileSize));
                        wallGrid[r][c] = true;
                    }
                    case 'b' ->
                        enemies.add(createEnemy(x, y, Enemy1.Role.CHASER, 30));
                    case 'o' ->
                        enemies.add(createEnemy(x, y, Enemy1.Role.AMBUSHER, 120));
                    case 'd' ->
                        enemies.add(createEnemy(x, y, Enemy1.Role.GUARD, 240));
                    case 'r' ->
                        enemies.add(createEnemy(x, y, Enemy1.Role.FLANKER, 360));
                    case 'P' ->
                        player = new Player1(playerRightImage, x, y, tileSize, tileSize, walls, tileSize);
                    case 'F' -> {
                        finishX = x;
                        finishY = y;
                    }
                    default -> {
                    }
                }
            }
        }
        resetPositions();
        generateRadios();
    }

    private Enemy1 createEnemy(int x, int y, Enemy1.Role role, int releaseDelay) {
        Enemy1 enemy = new Enemy1(enemyUpImage, enemyDownImage, enemyLeftImage, enemyRightImage,
                x, y, tileSize, tileSize, walls, tileSize);
        enemy.setRole(role);
        enemy.setReleaseDelay(releaseDelay);
        return enemy;
    }

    /**
     * Meletakkan radio di jalur terbuka yang tersebar: tidak di titik awal, pintu keluar,
     * kotak awal musuh, maupun terlalu dekat dengan pemain atau radio lain.
     */
    private void generateRadios() {
        radiox.clear();

        List<int[]> candidates = new ArrayList<>();
        int startCol = 0;
        int startRow = finishY / tileSize;
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                if (wallGrid[r][c] || isEnemyStartTile(c, r) || (c == finishX / tileSize && r == startRow)) {
                    continue;
                }
                candidates.add(new int[]{c, r});
            }
        }
        Collections.shuffle(candidates, random);

        for (int minGap = 5; minGap >= 0 && radiox.size() < RADIO_COUNT; minGap--) {
            int startDistance = Math.max(minGap + 1, 6);
            for (int[] tile : candidates) {
                if (radiox.size() >= RADIO_COUNT) {
                    break;
                }
                if (Math.abs(tile[0] - startCol) + Math.abs(tile[1] - startRow) < startDistance) {
                    continue;
                }
                boolean farEnough = true;
                for (Block other : radiox) {
                    int distance = Math.abs(tile[0] - other.x / tileSize) + Math.abs(tile[1] - other.y / tileSize);
                    if (distance < minGap) {
                        farEnough = false;
                        break;
                    }
                }
                if (farEnough && !alreadyHasRadio(tile[0], tile[1])) {
                    radiox.add(new Block(radioImage, tile[0] * tileSize, tile[1] * tileSize, tileSize, tileSize));
                }
            }
        }
    }

    private boolean alreadyHasRadio(int col, int row) {
        for (Block block : radiox) {
            if (block.x / tileSize == col && block.y / tileSize == row) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnemyStartTile(int col, int row) {
        for (Enemy1 enemy : enemies) {
            if (enemy.startX / tileSize == col && enemy.startY / tileSize == row) {
                return true;
            }
        }
        return false;
    }

    // ===================== PEMBARUAN PERMAINAN =====================

    @Override
    public void actionPerformed(ActionEvent e) {
        int steps = clock.consume();
        for (int i = 0; i < steps && gameLoop.isRunning(); i++) {
            update();
        }
        if (steps > 0) {
            repaint();
        }
    }

    private void update() {
        frame++;
        updateEffects();

        switch (state) {
            case INTRO ->
                updateIntro();
            case PLAYING ->
                updatePlaying();
            case ESCAPING ->
                updateEscaping();
            case MISSION_TEXT ->
                updateMissionText();
            default -> {
            }
        }
    }

    private void updateEffects() {
        if (bannerTimer > 0) {
            bannerTimer--;
        }
        if (hitFlash > 0) {
            hitFlash--;
        }
        if (exitHintCooldown > 0) {
            exitHintCooldown--;
        }
        floatingTexts.removeIf(t -> --t.life <= 0);
        for (FloatingText text : floatingTexts) {
            text.y -= 0.7f;
        }
    }

    private void updateIntro() {
        startMessageTick++;
        if (startMessageIndex < startMessage.length()) {
            if (startMessageTick >= 2) {
                startMessageTick = 0;
                displayedStartMessage += startMessage.charAt(startMessageIndex);
                startMessageIndex++;
            }
        } else if (++messageHoldCounter >= messageHoldFrames) {
            finishIntro();
        }
    }

    private void finishIntro() {
        state = State.PLAYING;
        showBanner("Gerak: W A S D atau tombol panah", 180);
    }

    private void updatePlaying() {
        // 1. Gerak pemain
        char want = heldDirs.isEmpty() ? 0 : heldDirs.getLast();
        boolean moved = false;
        if (want != 0) {
            playerFacing = want;
            player.image = imageForDirection(want);
            moved = movePlayer(want);
        }
        if (moved) {
            player.updateDirection(want);
            playerStarted = true;
        } else {
            player.stopMoving();
        }

        // 2. Pintu keluar
        if (player.x >= finishX && Math.abs(player.y - finishY) <= TURN_ASSIST) {
            if (radio >= 1) {
                startEscape();
                return;
            }
            if (exitHintCooldown == 0) {
                showBanner("Ambil minimal 1 radio dulu sebelum keluar!", 150);
                exitHintCooldown = 150;
            }
        }

        // 3. Ambil radio
        Block picked = null;
        for (Block r : radiox) {
            if (Math.hypot(r.x - player.x, r.y - player.y) < 24) {
                picked = r;
                break;
            }
        }
        if (picked != null) {
            radiox.remove(picked);
            radio++;
            player.playPickRadioSound();
            floatingTexts.add(new FloatingText("+1 RADIO", picked.x + offsetX + tileSize / 2f,
                    picked.y + offsetY, new Color(255, 230, 120), 70));
            if (radio == 1) {
                showBanner("Radio didapat! Cari pintu keluar di sisi kanan peta", 200);
            } else {
                showBanner("Radio: " + radio, 90);
            }
        }

        // 4. Musuh
        if (playerStarted) {
            enemyClock++;
            for (Enemy1 enemy : enemies) {
                if (enemyClock >= enemy.getReleaseDelay()) {
                    int[] target = targetFor(enemy);
                    enemy.update(wallGrid, target[0], target[1]);
                }
            }
        }

        // 5. Tertangkap
        if (invulnerable > 0) {
            invulnerable--;
        } else {
            for (Enemy1 enemy : enemies) {
                if (overlaps(player, enemy, 8)) {
                    onCaught();
                    return;
                }
            }
        }

        updateDanger();
    }

    /** Sasaran tiap musuh sesuai perannya; keputusan arah diambil di Enemy1. */
    private int[] targetFor(Enemy1 enemy) {
        int playerCol = (player.x + tileSize / 2) / tileSize;
        int playerRow = (player.y + tileSize / 2) / tileSize;
        int enemyCol = (enemy.x + tileSize / 2) / tileSize;
        int enemyRow = (enemy.y + tileSize / 2) / tileSize;
        int distance = Math.abs(playerCol - enemyCol) + Math.abs(playerRow - enemyRow);

        switch (enemy.getRole()) {
            case AMBUSHER -> {
                int ahead = 3;
                int col = Math.max(0, Math.min(columnCount - 1, playerCol + dxOf(playerFacing) * ahead));
                int row = Math.max(0, Math.min(rowCount - 1, playerRow + dyOf(playerFacing) * ahead));
                return new int[]{col, row};
            }
            case GUARD -> {
                if (radio >= 1) {
                    return new int[]{finishX / tileSize, finishY / tileSize};
                }
                if (distance < 6 || radiox.isEmpty()) {
                    return new int[]{playerCol, playerRow};
                }
                Block nearest = radiox.get(0);
                int best = Integer.MAX_VALUE;
                for (Block r : radiox) {
                    int d = Math.abs(r.x / tileSize - enemyCol) + Math.abs(r.y / tileSize - enemyRow);
                    if (d < best) {
                        best = d;
                        nearest = r;
                    }
                }
                return new int[]{nearest.x / tileSize, nearest.y / tileSize};
            }
            case FLANKER -> {
                if (distance > 8) {
                    return new int[]{playerCol, playerRow};
                }
                return new int[]{1, rowCount - 2};
            }
            default -> {
                return new int[]{playerCol, playerRow};
            }
        }
    }

    private void updateDanger() {
        float nearest = Float.MAX_VALUE;
        for (Enemy1 enemy : enemies) {
            float d = (float) Math.hypot(enemy.x - player.x, enemy.y - player.y);
            nearest = Math.min(nearest, d);
        }
        float target = nearest < DANGER_RANGE ? 1f - nearest / DANGER_RANGE : 0f;
        danger += (target - danger) * 0.15f;
    }

    private void onCaught() {
        player.setLives(player.getLives() - 1);
        player.playHurtSound();
        player.stopMoving();
        hitFlash = 40;
        danger = 0f;

        if (player.getLives() <= 0) {
            state = State.GAME_OVER;
            restartButton.setVisible(true);
            gameLoop.stop();
            repaint();
            return;
        }

        showBanner("Tertangkap! Nyawa tersisa: " + player.getLives(), 150);
        floatingTexts.add(new FloatingText("-1 NYAWA", player.x + offsetX + tileSize / 2f,
                player.y + offsetY, new Color(255, 110, 110), 70));
        respawn();
    }

    /** Pemain kembali ke titik awal; musuh kembali ke kotak awal dan menunggu pemain bergerak. */
    private void respawn() {
        resetPositions();
        invulnerable = INVULNERABLE_FRAMES;
        playerStarted = false;
        enemyClock = 0;
    }

    private void startEscape() {
        state = State.ESCAPING;
        autoWalkProgress = 0;
        player.stopMoving();
        player.image = playerRightImage;
        showBanner("Berhasil keluar!", 120);
    }

    private void updateEscaping() {
        overlayAlpha = Math.max(0f, overlayAlpha - 0.02f);
        player.x += autoWalkSpeed;
        autoWalkProgress += autoWalkSpeed;

        if (autoWalkProgress >= autoWalkDistance) {
            overlayAlpha = 1.0f;
            notificationAlpha = 0;
            bannerTimer = 0;
            state = State.MISSION_TEXT;
        }
    }

    private void updateMissionText() {
        if (notificationAlpha < 255) {
            notificationAlpha = Math.min(255, notificationAlpha + 3);
        }
        if (notificationAlpha >= 255) {
            state = State.WON;
            nextButton.setVisible(true);
            gameLoop.stop();
            repaint();

            if (onWin != null) {
                // Jeda sebentar supaya pemain sempat membaca pesan misi selesai
                Timer t = new Timer(400, e -> onWin.run());
                t.setRepeats(false);
                t.start();
            }
        }
    }

    // ===================== GERAK PEMAIN =====================

    /**
     * Menggerakkan pemain satu langkah. Lorong selebar tepat satu tile, jadi di persimpangan
     * pemain yang meleset beberapa piksel dibantu bergeser ke jalur yang sejajar.
     */
    private boolean movePlayer(char dir) {
        int dx = dxOf(dir);
        int dy = dyOf(dir);
        int step = Player1.SPEED;

        if (isFree(player.x + dx * step, player.y + dy * step)) {
            player.x += dx * step;
            player.y += dy * step;
            return true;
        }

        // Bantuan berbelok: sejajarkan sumbu tegak lurus bila jalur sebenarnya terbuka
        if (dx != 0) {
            int aligned = alignToTile(player.y);
            int offset = aligned - player.y;
            if (offset != 0 && Math.abs(offset) <= TURN_ASSIST && isFree(player.x + dx * step, aligned)) {
                player.y += Integer.signum(offset) * Math.min(Math.abs(offset), step);
                return true;
            }
        } else {
            int aligned = alignToTile(player.x);
            int offset = aligned - player.x;
            if (offset != 0 && Math.abs(offset) <= TURN_ASSIST && isFree(aligned, player.y + dy * step)) {
                player.x += Integer.signum(offset) * Math.min(Math.abs(offset), step);
                return true;
            }
        }

        // Rapatkan ke dinding dengan langkah yang lebih kecil
        for (int s = step - 1; s >= 1; s--) {
            if (isFree(player.x + dx * s, player.y + dy * s)) {
                player.x += dx * s;
                player.y += dy * s;
                return true;
            }
        }
        return false;
    }

    private int alignToTile(int value) {
        return Math.round(value / (float) tileSize) * tileSize;
    }

    private boolean isFree(int px, int py) {
        if (px < 0 || py < 0) {
            return false;
        }
        int firstCol = px / tileSize;
        int lastCol = (px + tileSize - 1) / tileSize;
        int firstRow = py / tileSize;
        int lastRow = (py + tileSize - 1) / tileSize;
        if (lastCol >= columnCount || lastRow >= rowCount) {
            return false;
        }
        for (int r = firstRow; r <= lastRow; r++) {
            for (int c = firstCol; c <= lastCol; c++) {
                if (wallGrid[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean overlaps(Block a, Block b, int inset) {
        return a.x + inset < b.x + b.width - inset && a.x + a.width - inset > b.x + inset
                && a.y + inset < b.y + b.height - inset && a.y + a.height - inset > b.y + inset;
    }

    private static int dxOf(char dir) {
        return dir == 'L' ? -1 : dir == 'R' ? 1 : 0;
    }

    private static int dyOf(char dir) {
        return dir == 'U' ? -1 : dir == 'D' ? 1 : 0;
    }

    private Image imageForDirection(char dir) {
        return switch (dir) {
            case 'U' ->
                playerUpImage;
            case 'D' ->
                playerDownImage;
            case 'L' ->
                playerLeftImage;
            default ->
                playerRightImage;
        };
    }

    private void showBanner(String text, int frames) {
        bannerText = text;
        bannerTimer = frames;
        bannerDuration = Math.max(1, frames);
    }

    // ===================== PENGGAMBARAN =====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // 1. Latar dan objek peta
        g2d.drawImage(backgroundImage, 0, 0, frameWidth, frameHeight, null);
        drawMapAndObjects(g2d);

        boolean missionComplete = state == State.MISSION_TEXT || state == State.WON;

        // 2. Spotlight gelap selama misi berjalan
        if (!missionComplete) {
            int centerX = player.x + offsetX + player.width / 2;
            int centerY = player.y + offsetY + player.height / 2;
            int spotlightRadius = 125;

            float[] dist = {0.0f, 1.0f};
            Color[] colors = {
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, (int) (overlayAlpha * 255))
            };
            g2d.setPaint(new RadialGradientPaint(new Point(centerX, centerY), spotlightRadius,
                    dist, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g2d.fillRect(0, 0, frameWidth, frameHeight);
        } else {
            g2d.setColor(new Color(0, 0, 0, notificationAlpha));
            g2d.fillRect(0, 0, frameWidth, frameHeight);

            g2d.setColor(new Color(255, 255, 255, notificationAlpha));
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String message = "MISI SELESAI, BERHASIL KABUR DARI PENJAJAH";
            int strWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (frameWidth - strWidth) / 2, frameHeight / 2);
        }

        // 3. Pemain di atas peta (berkedip saat kebal)
        boolean visible = invulnerable == 0 || (invulnerable / 6) % 2 == 0;
        if (visible) {
            g2d.drawImage(player.image, player.x + offsetX, player.y + offsetY, player.width, player.height, null);
        }

        // 4. Peringatan musuh dekat dan panah pintu keluar (di atas kegelapan)
        drawDangerVignette(g2d);
        drawExitHint(g2d);

        // 5. Peta kecil
        drawMiniMap(g2d);

        // 6. HUD, pesan, dan efek
        drawHud(g2d);
        drawFloatingTexts(g2d);
        drawBanner(g2d);

        if (hitFlash > 0) {
            g2d.setColor(new Color(220, 30, 30, Math.min(150, hitFlash * 5)));
            g2d.fillRect(0, 0, frameWidth, frameHeight);
        }

        if (state == State.GAME_OVER) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, frameWidth, frameHeight);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String gameOverMsg = "TERTANGKAP...";
            int w = g2d.getFontMetrics().stringWidth(gameOverMsg);
            g2d.drawString(gameOverMsg, (frameWidth - w) / 2, frameHeight / 2 - 20);

            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String subMsg = "PERLARIAN BUNG TOMO GAGAL";
            int w2 = g2d.getFontMetrics().stringWidth(subMsg);
            g2d.drawString(subMsg, (frameWidth - w2) / 2, frameHeight / 2 + 20);
        }

        // 7. Pesan pengantar paling atas
        if (state == State.INTRO) {
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            int msgWidth = g2d.getFontMetrics().stringWidth(displayedStartMessage);
            int x = (frameWidth - msgWidth) / 2;
            int y = frameHeight / 2;
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.drawString(displayedStartMessage, x + 2, y + 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString(displayedStartMessage, x, y);
        }

        g2d.dispose();
    }

    private void drawMapAndObjects(Graphics2D g2d) {
        for (Enemy1 enemy : enemies) {
            g2d.drawImage(enemy.image, enemy.x + offsetX, enemy.y + offsetY, enemy.width, enemy.height, null);
        }

        for (Block wall : walls) {
            g2d.drawImage(wall.image, wall.x + offsetX, wall.y + offsetY, wall.width, wall.height, null);
        }

        for (Block radioObj : radiox) {
            int bob = (int) Math.round(Math.sin(frame * 0.1 + radioObj.x) * 2);
            g2d.drawImage(radioObj.image, radioObj.x + offsetX, radioObj.y + offsetY + bob,
                    radioObj.width, radioObj.height, null);
        }
    }

    private void drawDangerVignette(Graphics2D g2d) {
        if (danger <= 0.02f || state != State.PLAYING) {
            return;
        }
        float[] dist = {0.55f, 1.0f};
        Color[] colors = {new Color(200, 0, 0, 0), new Color(200, 0, 0, (int) (danger * 130))};
        g2d.setPaint(new RadialGradientPaint(new Point(frameWidth / 2, frameHeight / 2), frameWidth * 0.65f,
                dist, colors, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2d.fillRect(0, 0, frameWidth, frameHeight);
    }

    private void drawExitHint(Graphics2D g2d) {
        if (state != State.PLAYING || radio < 1) {
            return;
        }
        int pulse = (int) (Math.sin(frame * 0.15) * 5);
        int x = offsetX + boardWidth + 8 + pulse;
        int y = offsetY + finishY + tileSize / 2;
        g2d.setColor(new Color(120, 255, 140));
        Polygon arrow = new Polygon(new int[]{x, x + 18, x}, new int[]{y - 12, y, y + 12}, 3);
        g2d.fillPolygon(arrow);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("KELUAR", x - 8, y - 18);
    }

    private void drawMiniMap(Graphics2D g2d) {
        int mapWidth = 200;
        int mapHeight = 200;
        int mapX = frameWidth - mapWidth - 10;
        int mapY = 10;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2d.drawImage(mapsImage, mapX, mapY, mapWidth, mapHeight, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Posisi pemain di peta kecil
        int dotX = mapX + (int) ((player.x + tileSize / 2f) / boardWidth * mapWidth);
        int dotY = mapY + (int) ((player.y + tileSize / 2f) / boardHeight * mapHeight);
        boolean blink = (frame / 15) % 2 == 0;
        g2d.setColor(blink ? new Color(255, 220, 60) : new Color(255, 120, 0));
        g2d.fillOval(dotX - 4, dotY - 4, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(dotX - 4, dotY - 4, 8, 8);
    }

    private void drawHud(Graphics2D g2d) {
        // Nyawa
        for (int i = 0; i < Player1.MAX_LIVES; i++) {
            drawHeart(g2d, 20 + i * 30, 18, 22, i < player.getLives());
        }

        // Jumlah radio
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String radioText = radio == 0 ? "Radio: 0  (minimal 1)" : "Radio: " + radio;
        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.drawString(radioText, 22, 72);
        g2d.setColor(radio == 0 ? Color.WHITE : new Color(255, 230, 120));
        g2d.drawString(radioText, 20, 70);
    }

    private void drawHeart(Graphics2D g2d, int x, int y, int s, boolean filled) {
        Path2D.Float heart = new Path2D.Float();
        heart.moveTo(x + s / 2f, y + s);
        heart.curveTo(x - s * 0.25f, y + s * 0.45f, x + s * 0.05f, y - s * 0.2f, x + s / 2f, y + s * 0.28f);
        heart.curveTo(x + s * 0.95f, y - s * 0.2f, x + s * 1.25f, y + s * 0.45f, x + s / 2f, y + s);
        heart.closePath();
        if (filled) {
            g2d.setColor(new Color(225, 40, 50));
            g2d.fill(heart);
        } else {
            g2d.setColor(new Color(60, 60, 60, 190));
            g2d.fill(heart);
        }
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(heart);
    }

    private void drawFloatingTexts(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        for (FloatingText text : floatingTexts) {
            int alpha = Math.max(0, Math.min(255, text.life * 255 / text.maxLife));
            int width = g2d.getFontMetrics().stringWidth(text.text);
            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.drawString(text.text, text.x - width / 2f + 1, text.y + 1);
            g2d.setColor(new Color(text.color.getRed(), text.color.getGreen(), text.color.getBlue(), alpha));
            g2d.drawString(text.text, text.x - width / 2f, text.y);
        }
    }

    private void drawBanner(Graphics2D g2d) {
        if (bannerTimer <= 0 || bannerText.isEmpty()) {
            return;
        }
        float fade = Math.min(1f, Math.min(bannerTimer, bannerDuration - bannerTimer + 10) / 15f);
        int alpha = (int) (Math.max(0f, fade) * 255);
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(bannerText);
        int boxW = w + 40;
        int boxH = 40;
        int x = (frameWidth - boxW) / 2;
        int y = frameHeight - 70;
        g2d.setColor(new Color(0, 0, 0, alpha * 170 / 255));
        g2d.fillRoundRect(x, y, boxW, boxH, 16, 16);
        g2d.setColor(new Color(255, 255, 255, alpha));
        g2d.drawString(bannerText, x + 20, y + 27);
    }

    // ===================== API PUBLIK =====================

    public void setOnWin(Runnable onWin) {
        this.onWin = onWin;
    }

    public void restartGame() {
        state = State.PLAYING;
        radio = 0;
        notificationAlpha = 0;
        overlayAlpha = 1.0f;
        autoWalkProgress = 0;
        invulnerable = 0;
        enemyClock = 0;
        playerStarted = false;
        danger = 0f;
        hitFlash = 0;
        bannerTimer = 0;
        floatingTexts.clear();
        heldDirs.clear();
        restartButton.setVisible(false);
        nextButton.setVisible(false);
        loadMap();
        player.setLives(Player1.MAX_LIVES);
        repaint();
        clock.reset();
        gameLoop.start();
    }

    // ===================== INPUT =====================

    private static char directionOf(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W, KeyEvent.VK_UP ->
                'U';
            case KeyEvent.VK_S, KeyEvent.VK_DOWN ->
                'D';
            case KeyEvent.VK_A, KeyEvent.VK_LEFT ->
                'L';
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT ->
                'R';
            default ->
                0;
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state == State.INTRO) {
            finishIntro(); // tombol apa pun melewati teks pengantar
        }
        char dir = directionOf(e.getKeyCode());
        if (dir != 0) {
            heldDirs.remove(Character.valueOf(dir));
            heldDirs.addLast(dir);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        char dir = directionOf(e.getKeyCode());
        if (dir != 0) {
            heldDirs.remove(Character.valueOf(dir));
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void resetPositions() {
        if (player != null) {
            // Mulai tepat di lorong keluar-masuk (baris titik finish) agar sejajar dengan dinding
            player.x = 0;
            player.y = finishY;
            player.velocityX = 0;
            player.velocityY = 0;
            player.image = playerRightImage;
            playerFacing = 'R';
        }

        for (Enemy1 enemy : enemies) {
            enemy.reset();
            enemy.updateDirection('U');
            enemy.velocityX = 0;
            enemy.velocityY = 0;
        }
    }
}
