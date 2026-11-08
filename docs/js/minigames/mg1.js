import { HEIGHT, WIDTH, button, inside } from '../core/engine.js';
import { img } from '../core/assets.js';
import { audio } from '../core/audio.js';

const ROWS = 21;
const COLS = 19;
const TILE = 32;
const BOARD_W = COLS * TILE;
const BOARD_H = ROWS * TILE;
const OFFSET_X = (WIDTH - BOARD_W) / 2;
const OFFSET_Y = (HEIGHT - BOARD_H) / 2;

const TURN_ASSIST = 16;
const RADIO_COUNT = 3;
const INVULNERABLE_FRAMES = 150;
const DANGER_RANGE = 128;
const MAX_LIVES = 3;
const PLAYER_SPEED = 3;
const ENEMY_SPEED = 2;
const AUTO_WALK_DISTANCE = 370;
const AUTO_WALK_SPEED = 3;
const START_MESSAGE = 'Lari Dari Kejaran Dan Dapatkan Minimal 1 Radio Untuk Keluar';
const MESSAGE_HOLD_FRAMES = 90;

const TILE_MAP = [
    'XXXXXXXXXXXXXXXXXXX',
    'Y        Y        B',
    'X YXYXYX X YYXXYY X',
    'X                 X',
    'X XX BBBXXXX X BB X',
    'X    X       X YY X',
    'XXXXXX XX XX X BB X',
    'B      XX XX X YY X',
    'B BBBXXXXrXX X    X',
    'B       bpo  X XXXX',
    'XXXX X BBBBB X XXXX',
    '     B       B    F',
    'XXXX X YYYYY X XXXX',
    'X        X   X    X',
    'X YY XXX X YYYYYY X',
    'X YY     P      X X',
    'X YY XXXYXXXBXX Y X',
    'Y        X      X X',
    'Y XXBBXX X XXYYXX X',
    'X       ad        X',
    'XXBBYYYXBBBXXXXXXXX'
];

const ROLES = { b: ['CHASER', 30], o: ['AMBUSHER', 120], d: ['GUARD', 240], r: ['FLANKER', 360] };
const TILE_IMAGE = { X: 'Genteng', Y: 'Genteng2', B: 'Genteng1' };

const dxOf = (dir) => (dir === 'L' ? -1 : dir === 'R' ? 1 : 0);
const dyOf = (dir) => (dir === 'U' ? -1 : dir === 'D' ? 1 : 0);
const opposite = (dir) => ({ U: 'D', D: 'U', L: 'R' }[dir] || 'L');
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

export class Minigame1 {
    constructor({ onWin, onNext }) {
        this.onWin = onWin;
        this.onNext = onNext;
        this.music = { name: 'Bs_Minigame1', volume: 1 };
        this.touch = { stick: true };
        this.state = 'INTRO';
        this.frame = 0;
        this.held = [];
        this.overlayAlpha = 1;
        this.notificationAlpha = 0;
        this.displayed = '';
        this.messageIndex = 0;
        this.messageTick = 0;
        this.messageHold = 0;
        this.banner = { text: '', timer: 0, duration: 1 };
        this.hitFlash = 0;
        this.exitHintCooldown = 0;
        this.floating = [];
        this.winTimer = 0;
        this.lastWalk = 0;
        this.lastHurt = 0;
        this.hover = '';
        this.buildMap();
        this.lives = MAX_LIVES;
    }

    leave() {
        audio.stop('walk');
    }

    // ---------- peta ----------

    buildMap() {
        this.walls = [];
        this.grid = Array.from({ length: ROWS }, () => new Array(COLS).fill(false));
        this.enemies = [];
        this.finishX = 0;
        this.finishY = 0;

        for (let r = 0; r < ROWS; r++) {
            for (let c = 0; c < COLS; c++) {
                const tile = TILE_MAP[r][c];
                const x = c * TILE;
                const y = r * TILE;
                if (TILE_IMAGE[tile]) {
                    this.walls.push({ x, y, image: TILE_IMAGE[tile] });
                    this.grid[r][c] = true;
                } else if (ROLES[tile]) {
                    this.enemies.push({
                        x, y, startX: x, startY: y, vx: 0, vy: 0, dir: 'U', image: 'enemy_belakang',
                        role: ROLES[tile][0], delay: ROLES[tile][1]
                    });
                } else if (tile === 'F') {
                    this.finishX = x;
                    this.finishY = y;
                }
            }
        }
        this.player = { x: 0, y: this.finishY, image: 'player_samping_kanan' };
        this.facing = 'R';
        this.resetPositions();
        this.generateRadios();
        this.playerStarted = false;
        this.enemyClock = 0;
        this.invulnerable = 0;
        this.danger = 0;
        this.radio = 0;
    }

    resetPositions() {
        this.player.x = 0;
        this.player.y = this.finishY;
        this.player.image = 'player_samping_kanan';
        this.facing = 'R';
        this.enemies.forEach((enemy) => {
            enemy.x = enemy.startX;
            enemy.y = enemy.startY;
            this.setEnemyDirection(enemy, 'U');
            enemy.vx = 0;
            enemy.vy = 0;
        });
    }

    generateRadios() {
        this.radios = [];
        const candidates = [];
        const startRow = this.finishY / TILE;
        for (let r = 0; r < ROWS; r++) {
            for (let c = 0; c < COLS; c++) {
                if (this.grid[r][c] || this.isEnemyStart(c, r) || (c === this.finishX / TILE && r === startRow)) {
                    continue;
                }
                candidates.push([c, r]);
            }
        }
        for (let i = candidates.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [candidates[i], candidates[j]] = [candidates[j], candidates[i]];
        }
        for (let minGap = 5; minGap >= 0 && this.radios.length < RADIO_COUNT; minGap--) {
            const startDistance = Math.max(minGap + 1, 6);
            for (const [c, r] of candidates) {
                if (this.radios.length >= RADIO_COUNT) {
                    break;
                }
                if (Math.abs(c) + Math.abs(r - startRow) < startDistance) {
                    continue;
                }
                const far = this.radios.every((o) => Math.abs(c - o.x / TILE) + Math.abs(r - o.y / TILE) >= minGap);
                const exists = this.radios.some((o) => o.x / TILE === c && o.y / TILE === r);
                if (far && !exists) {
                    this.radios.push({ x: c * TILE, y: r * TILE });
                }
            }
        }
    }

    isEnemyStart(col, row) {
        return this.enemies.some((e) => e.startX / TILE === col && e.startY / TILE === row);
    }

    // ---------- musuh ----------

    setEnemyDirection(enemy, dir) {
        enemy.dir = dir;
        enemy.vx = dxOf(dir) * ENEMY_SPEED;
        enemy.vy = dyOf(dir) * ENEMY_SPEED;
        enemy.image = { U: 'enemy_belakang', D: 'enemy_depan', L: 'enemy_samping_kiri', R: 'enemy_samping_kanan' }[dir];
    }

    blocked(col, row, dir) {
        const nc = col + dxOf(dir);
        const nr = row + dyOf(dir);
        if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
            return true;
        }
        return this.grid[nr][nc];
    }

    updateEnemy(enemy, tc, tr) {
        if (enemy.x % TILE === 0 && enemy.y % TILE === 0) {
            this.setEnemyDirection(enemy, this.chooseDirection(enemy, tc, tr));
        }
        enemy.x += enemy.vx;
        enemy.y += enemy.vy;
    }

    chooseDirection(enemy, tc, tr) {
        const col = enemy.x / TILE;
        const row = enemy.y / TILE;
        const reverse = opposite(enemy.dir);
        let best = '';
        let bestDistance = Infinity;
        for (const dir of ['U', 'L', 'D', 'R']) {
            if (dir === reverse || this.blocked(col, row, dir)) {
                continue;
            }
            const nc = col + dxOf(dir);
            const nr = row + dyOf(dir);
            const distance = (nc - tc) * (nc - tc) + (nr - tr) * (nr - tr);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = dir;
            }
        }
        if (!best) {
            best = this.blocked(col, row, reverse) ? enemy.dir : reverse;
        }
        return best;
    }

    targetFor(enemy) {
        const p = this.player;
        const pc = Math.trunc((p.x + TILE / 2) / TILE);
        const pr = Math.trunc((p.y + TILE / 2) / TILE);
        const ec = Math.trunc((enemy.x + TILE / 2) / TILE);
        const er = Math.trunc((enemy.y + TILE / 2) / TILE);
        const distance = Math.abs(pc - ec) + Math.abs(pr - er);

        switch (enemy.role) {
            case 'AMBUSHER':
                return [clamp(pc + dxOf(this.facing) * 3, 0, COLS - 1), clamp(pr + dyOf(this.facing) * 3, 0, ROWS - 1)];
            case 'GUARD': {
                if (this.radio >= 1) {
                    return [this.finishX / TILE, this.finishY / TILE];
                }
                if (distance < 6 || this.radios.length === 0) {
                    return [pc, pr];
                }
                let nearest = this.radios[0];
                let best = Infinity;
                this.radios.forEach((r) => {
                    const d = Math.abs(r.x / TILE - ec) + Math.abs(r.y / TILE - er);
                    if (d < best) {
                        best = d;
                        nearest = r;
                    }
                });
                return [nearest.x / TILE, nearest.y / TILE];
            }
            case 'FLANKER':
                return distance > 8 ? [pc, pr] : [1, ROWS - 2];
            default:
                return [pc, pr];
        }
    }

    // ---------- pemain ----------

    isFree(px, py) {
        if (px < 0 || py < 0) {
            return false;
        }
        const firstCol = Math.trunc(px / TILE);
        const lastCol = Math.trunc((px + TILE - 1) / TILE);
        const firstRow = Math.trunc(py / TILE);
        const lastRow = Math.trunc((py + TILE - 1) / TILE);
        if (lastCol >= COLS || lastRow >= ROWS) {
            return false;
        }
        for (let r = firstRow; r <= lastRow; r++) {
            for (let c = firstCol; c <= lastCol; c++) {
                if (this.grid[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    alignToTile(value) {
        return Math.round(value / TILE) * TILE;
    }

    movePlayer(dir) {
        const p = this.player;
        const dx = dxOf(dir);
        const dy = dyOf(dir);
        const step = PLAYER_SPEED;

        if (this.isFree(p.x + dx * step, p.y + dy * step)) {
            p.x += dx * step;
            p.y += dy * step;
            return true;
        }
        if (dx !== 0) {
            const aligned = this.alignToTile(p.y);
            const offset = aligned - p.y;
            if (offset !== 0 && Math.abs(offset) <= TURN_ASSIST && this.isFree(p.x + dx * step, aligned)) {
                p.y += Math.sign(offset) * Math.min(Math.abs(offset), step);
                return true;
            }
        } else {
            const aligned = this.alignToTile(p.x);
            const offset = aligned - p.x;
            if (offset !== 0 && Math.abs(offset) <= TURN_ASSIST && this.isFree(aligned, p.y + dy * step)) {
                p.x += Math.sign(offset) * Math.min(Math.abs(offset), step);
                return true;
            }
        }
        for (let s = step - 1; s >= 1; s--) {
            if (this.isFree(p.x + dx * s, p.y + dy * s)) {
                p.x += dx * s;
                p.y += dy * s;
                return true;
            }
        }
        return false;
    }

    overlaps(enemy, inset) {
        const p = this.player;
        return p.x + inset < enemy.x + TILE - inset && p.x + TILE - inset > enemy.x + inset
            && p.y + inset < enemy.y + TILE - inset && p.y + TILE - inset > enemy.y + inset;
    }

    playWalk() {
        const now = performance.now();
        if (now - this.lastWalk > 250) {
            audio.play('walk');
            this.lastWalk = now;
        }
    }

    // ---------- pembaruan ----------

    update() {
        this.frame++;
        this.updateEffects();
        switch (this.state) {
            case 'INTRO': this.updateIntro(); break;
            case 'PLAYING': this.updatePlaying(); break;
            case 'ESCAPING': this.updateEscaping(); break;
            case 'MISSION_TEXT': this.updateMissionText(); break;
            case 'WON': this.updateWon(); break;
            default: break;
        }
    }

    updateEffects() {
        if (this.banner.timer > 0) {
            this.banner.timer--;
        }
        if (this.hitFlash > 0) {
            this.hitFlash--;
        }
        if (this.exitHintCooldown > 0) {
            this.exitHintCooldown--;
        }
        this.floating = this.floating.filter((t) => --t.life > 0);
        this.floating.forEach((t) => { t.y -= 0.7; });
    }

    updateIntro() {
        this.messageTick++;
        if (this.messageIndex < START_MESSAGE.length) {
            if (this.messageTick >= 2) {
                this.messageTick = 0;
                this.displayed += START_MESSAGE[this.messageIndex++];
            }
        } else if (++this.messageHold >= MESSAGE_HOLD_FRAMES) {
            this.finishIntro();
        }
    }

    finishIntro() {
        this.state = 'PLAYING';
        this.showBanner('Gerak: W A S D, tombol panah, atau joystick', 180);
    }

    showBanner(text, frames) {
        this.banner = { text, timer: frames, duration: Math.max(1, frames) };
    }

    updatePlaying() {
        const p = this.player;
        const want = this.held.length ? this.held[this.held.length - 1] : '';
        let moved = false;
        if (want) {
            this.facing = want;
            p.image = { U: 'player_belakang', D: 'player_depan', L: 'player_samping_kiri', R: 'player_samping_kanan' }[want];
            moved = this.movePlayer(want);
        }
        if (moved) {
            this.playWalk();
            this.playerStarted = true;
        } else {
            audio.stop('walk');
        }

        if (p.x >= this.finishX && Math.abs(p.y - this.finishY) <= TURN_ASSIST) {
            if (this.radio >= 1) {
                this.startEscape();
                return;
            }
            if (this.exitHintCooldown === 0) {
                this.showBanner('Ambil minimal 1 radio dulu sebelum keluar!', 150);
                this.exitHintCooldown = 150;
            }
        }

        const picked = this.radios.find((r) => Math.hypot(r.x - p.x, r.y - p.y) < 24);
        if (picked) {
            this.radios = this.radios.filter((r) => r !== picked);
            this.radio++;
            audio.play('pickradio');
            this.floating.push({
                text: '+1 RADIO', x: picked.x + OFFSET_X + TILE / 2, y: picked.y + OFFSET_Y,
                color: [255, 230, 120], life: 70, maxLife: 70
            });
            if (this.radio === 1) {
                this.showBanner('Radio didapat! Cari pintu keluar di sisi kanan peta', 200);
            } else {
                this.showBanner(`Radio: ${this.radio}`, 90);
            }
        }

        if (this.playerStarted) {
            this.enemyClock++;
            this.enemies.forEach((enemy) => {
                if (this.enemyClock >= enemy.delay) {
                    const [tc, tr] = this.targetFor(enemy);
                    this.updateEnemy(enemy, tc, tr);
                }
            });
        }

        if (this.invulnerable > 0) {
            this.invulnerable--;
        } else if (this.enemies.some((enemy) => this.overlaps(enemy, 8))) {
            this.onCaught();
            return;
        }
        this.updateDanger();
    }

    updateDanger() {
        let nearest = Infinity;
        this.enemies.forEach((e) => {
            nearest = Math.min(nearest, Math.hypot(e.x - this.player.x, e.y - this.player.y));
        });
        const target = nearest < DANGER_RANGE ? 1 - nearest / DANGER_RANGE : 0;
        this.danger += (target - this.danger) * 0.15;
    }

    onCaught() {
        this.lives--;
        const now = performance.now();
        if (now - this.lastHurt > 300) {
            audio.play('Hurt');
            this.lastHurt = now;
        }
        audio.stop('walk');
        this.hitFlash = 40;
        this.danger = 0;

        if (this.lives <= 0) {
            this.state = 'GAME_OVER';
            return;
        }
        this.showBanner(`Tertangkap! Nyawa tersisa: ${this.lives}`, 150);
        this.floating.push({
            text: '-1 NYAWA', x: this.player.x + OFFSET_X + TILE / 2, y: this.player.y + OFFSET_Y,
            color: [255, 110, 110], life: 70, maxLife: 70
        });
        this.resetPositions();
        this.invulnerable = INVULNERABLE_FRAMES;
        this.playerStarted = false;
        this.enemyClock = 0;
    }

    startEscape() {
        this.state = 'ESCAPING';
        this.autoWalk = 0;
        audio.stop('walk');
        this.player.image = 'player_samping_kanan';
        this.showBanner('Berhasil keluar!', 120);
    }

    updateEscaping() {
        this.overlayAlpha = Math.max(0, this.overlayAlpha - 0.02);
        this.player.x += AUTO_WALK_SPEED;
        this.autoWalk += AUTO_WALK_SPEED;
        if (this.autoWalk >= AUTO_WALK_DISTANCE) {
            this.overlayAlpha = 1;
            this.notificationAlpha = 0;
            this.banner.timer = 0;
            this.state = 'MISSION_TEXT';
        }
    }

    updateMissionText() {
        this.notificationAlpha = Math.min(255, this.notificationAlpha + 3);
        if (this.notificationAlpha >= 255) {
            this.state = 'WON';
            this.winTimer = 0;
        }
    }

    updateWon() {
        if (this.winTimer >= 0 && ++this.winTimer >= 24) {
            this.winTimer = -1;
            if (this.onWin) {
                this.onWin();
            }
        }
    }

    restart() {
        this.state = 'PLAYING';
        this.notificationAlpha = 0;
        this.overlayAlpha = 1;
        this.autoWalk = 0;
        this.hitFlash = 0;
        this.banner.timer = 0;
        this.floating = [];
        this.held = [];
        this.buildMap();
        this.lives = MAX_LIVES;
    }

    // ---------- input ----------

    action(name, pressed) {
        if (name === 'U' || name === 'D' || name === 'L' || name === 'R') {
            if (pressed) {
                if (this.state === 'INTRO') {
                    this.finishIntro();
                }
                this.held = this.held.filter((d) => d !== name);
                this.held.push(name);
            } else {
                this.held = this.held.filter((d) => d !== name);
            }
        } else if (pressed && this.state === 'INTRO') {
            this.finishIntro();
        }
    }

    pointerMove(x, y) {
        this.hover = '';
        if (this.state === 'GAME_OVER' && inside(x, y, WIDTH / 2 - 75, HEIGHT / 2 + 40, 150, 40)) {
            this.hover = 'restart';
        } else if (this.state === 'WON' && inside(x, y, WIDTH / 2 - 75, HEIGHT / 2 + 80, 150, 40)) {
            this.hover = 'next';
        }
    }

    pointerDown(x, y) {
        if (this.state === 'INTRO') {
            this.finishIntro();
        } else if (this.state === 'GAME_OVER' && inside(x, y, WIDTH / 2 - 75, HEIGHT / 2 + 40, 150, 40)) {
            audio.play('click');
            this.restart();
        } else if (this.state === 'WON' && inside(x, y, WIDTH / 2 - 75, HEIGHT / 2 + 80, 150, 40)) {
            audio.play('click');
            if (this.onNext) {
                this.onNext();
            }
        }
    }

    // ---------- gambar ----------

    draw(ctx) {
        const bg = img('BGMinigame1');
        if (bg) {
            ctx.drawImage(bg, 0, 0, WIDTH, HEIGHT);
        }
        this.drawObjects(ctx);

        const p = this.player;
        const missionComplete = this.state === 'MISSION_TEXT' || this.state === 'WON';

        if (!missionComplete) {
            const cx = p.x + OFFSET_X + TILE / 2;
            const cy = p.y + OFFSET_Y + TILE / 2;
            const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, 125);
            gradient.addColorStop(0, 'rgba(0,0,0,0)');
            gradient.addColorStop(1, `rgba(0,0,0,${this.overlayAlpha})`);
            ctx.fillStyle = gradient;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            const a = this.notificationAlpha / 255;
            ctx.fillStyle = `rgba(0,0,0,${a})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
            ctx.fillStyle = `rgba(255,255,255,${a})`;
            ctx.font = 'bold 36px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('MISI SELESAI, BERHASIL KABUR DARI PENJAJAH', WIDTH / 2, HEIGHT / 2);
            ctx.textAlign = 'left';
        }

        const visible = this.invulnerable === 0 || Math.floor(this.invulnerable / 6) % 2 === 0;
        if (visible) {
            const image = img(p.image);
            if (image) {
                ctx.drawImage(image, p.x + OFFSET_X, p.y + OFFSET_Y, TILE, TILE);
            }
        }

        this.drawDanger(ctx);
        this.drawExitHint(ctx);
        this.drawMiniMap(ctx);
        this.drawHud(ctx);
        this.drawFloating(ctx);
        this.drawBanner(ctx);

        if (this.hitFlash > 0) {
            ctx.fillStyle = `rgba(220,30,30,${Math.min(150, this.hitFlash * 5) / 255})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        }

        if (this.state === 'GAME_OVER') {
            ctx.fillStyle = 'rgba(0,0,0,0.7)';
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
            ctx.fillStyle = '#fff';
            ctx.textAlign = 'center';
            ctx.font = 'bold 36px Arial';
            ctx.fillText('TERTANGKAP...', WIDTH / 2, HEIGHT / 2 - 20);
            ctx.font = 'bold 24px Arial';
            ctx.fillText('PERLARIAN BUNG TOMO GAGAL', WIDTH / 2, HEIGHT / 2 + 20);
            ctx.textAlign = 'left';
            button(ctx, 'RESTART', WIDTH / 2 - 75, HEIGHT / 2 + 40, 150, 40, '#000', 18, this.hover === 'restart');
        }
        if (this.state === 'WON') {
            button(ctx, 'NEXT', WIDTH / 2 - 75, HEIGHT / 2 + 80, 150, 40, '#a0522d', 16, this.hover === 'next');
        }

        if (this.state === 'INTRO') {
            ctx.font = 'bold 28px Arial';
            ctx.textAlign = 'center';
            ctx.fillStyle = 'rgba(0,0,0,0.78)';
            ctx.fillText(this.displayed, WIDTH / 2 + 2, HEIGHT / 2 + 2);
            ctx.fillStyle = '#fff';
            ctx.fillText(this.displayed, WIDTH / 2, HEIGHT / 2);
            ctx.textAlign = 'left';
        }
    }

    drawObjects(ctx) {
        this.enemies.forEach((enemy) => {
            const image = img(enemy.image);
            if (image) {
                ctx.drawImage(image, enemy.x + OFFSET_X, enemy.y + OFFSET_Y, TILE, TILE);
            }
        });
        this.walls.forEach((wall) => {
            const image = img(wall.image);
            if (image) {
                ctx.drawImage(image, wall.x + OFFSET_X, wall.y + OFFSET_Y, TILE, TILE);
            }
        });
        const radio = img('Radio');
        this.radios.forEach((r) => {
            const bob = Math.round(Math.sin(this.frame * 0.1 + r.x) * 2);
            if (radio) {
                ctx.drawImage(radio, r.x + OFFSET_X, r.y + OFFSET_Y + bob, TILE, TILE);
            }
        });
    }

    drawDanger(ctx) {
        if (this.danger <= 0.02 || this.state !== 'PLAYING') {
            return;
        }
        const radius = WIDTH * 0.65;
        const gradient = ctx.createRadialGradient(WIDTH / 2, HEIGHT / 2, radius * 0.55, WIDTH / 2, HEIGHT / 2, radius);
        gradient.addColorStop(0, 'rgba(200,0,0,0)');
        gradient.addColorStop(1, `rgba(200,0,0,${Math.floor(this.danger * 130) / 255})`);
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
    }

    drawExitHint(ctx) {
        if (this.state !== 'PLAYING' || this.radio < 1) {
            return;
        }
        const pulse = Math.trunc(Math.sin(this.frame * 0.15) * 5);
        const x = OFFSET_X + BOARD_W + 8 + pulse;
        const y = OFFSET_Y + this.finishY + TILE / 2;
        ctx.fillStyle = 'rgb(120,255,140)';
        ctx.beginPath();
        ctx.moveTo(x, y - 12);
        ctx.lineTo(x + 18, y);
        ctx.lineTo(x, y + 12);
        ctx.closePath();
        ctx.fill();
        ctx.font = 'bold 14px Arial';
        ctx.fillText('KELUAR', x - 8, y - 18);
    }

    drawMiniMap(ctx) {
        const size = 200;
        const mx = WIDTH - size - 10;
        const my = 10;
        const map = img('Peta');
        if (map) {
            ctx.globalAlpha = 0.8;
            ctx.drawImage(map, mx, my, size, size);
            ctx.globalAlpha = 1;
        }
        const p = this.player;
        const dx = mx + Math.trunc((p.x + TILE / 2) / BOARD_W * size);
        const dy = my + Math.trunc((p.y + TILE / 2) / BOARD_H * size);
        const blink = Math.floor(this.frame / 15) % 2 === 0;
        ctx.fillStyle = blink ? 'rgb(255,220,60)' : 'rgb(255,120,0)';
        ctx.beginPath();
        ctx.arc(dx, dy, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 1;
        ctx.stroke();
    }

    drawHud(ctx) {
        for (let i = 0; i < MAX_LIVES; i++) {
            drawHeart(ctx, 20 + i * 30, 18, 22, i < this.lives);
        }
        ctx.font = 'bold 20px Arial';
        const text = this.radio === 0 ? 'Radio: 0  (minimal 1)' : `Radio: ${this.radio}`;
        ctx.fillStyle = 'rgba(0,0,0,0.67)';
        ctx.fillText(text, 22, 72);
        ctx.fillStyle = this.radio === 0 ? '#fff' : 'rgb(255,230,120)';
        ctx.fillText(text, 20, 70);
    }

    drawFloating(ctx) {
        ctx.font = 'bold 18px Arial';
        this.floating.forEach((t) => {
            const alpha = clamp(t.life / t.maxLife, 0, 1);
            const w = ctx.measureText(t.text).width;
            ctx.fillStyle = `rgba(0,0,0,${alpha})`;
            ctx.fillText(t.text, t.x - w / 2 + 1, t.y + 1);
            ctx.fillStyle = `rgba(${t.color[0]},${t.color[1]},${t.color[2]},${alpha})`;
            ctx.fillText(t.text, t.x - w / 2, t.y);
        });
    }

    drawBanner(ctx) {
        const { text, timer, duration } = this.banner;
        if (timer <= 0 || !text) {
            return;
        }
        const fade = Math.min(1, Math.min(timer, duration - timer + 10) / 15);
        const alpha = Math.max(0, fade);
        ctx.font = 'bold 22px Arial';
        const w = ctx.measureText(text).width;
        const boxW = w + 40;
        const x = (WIDTH - boxW) / 2;
        const y = HEIGHT - 70;
        ctx.fillStyle = `rgba(0,0,0,${alpha * 170 / 255})`;
        ctx.beginPath();
        ctx.roundRect(x, y, boxW, 40, 16);
        ctx.fill();
        ctx.fillStyle = `rgba(255,255,255,${alpha})`;
        ctx.fillText(text, x + 20, y + 27);
    }
}

function drawHeart(ctx, x, y, s, filled) {
    ctx.beginPath();
    ctx.moveTo(x + s / 2, y + s);
    ctx.bezierCurveTo(x - s * 0.25, y + s * 0.45, x + s * 0.05, y - s * 0.2, x + s / 2, y + s * 0.28);
    ctx.bezierCurveTo(x + s * 0.95, y - s * 0.2, x + s * 1.25, y + s * 0.45, x + s / 2, y + s);
    ctx.closePath();
    ctx.fillStyle = filled ? 'rgb(225,40,50)' : 'rgba(60,60,60,0.75)';
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.stroke();
}
