import { HEIGHT, WIDTH, button, inside } from '../core/engine.js';
import { img } from '../core/assets.js';
import { audio } from '../core/audio.js';

const PLAYER_SPEED = 8;
const PLAYER_MAX_HEALTH = 5;
const INVULNERABLE_FRAMES = 75;
const BULLET_INTERVAL = 12;
const BULLET_SPEED = 12;
const WIN_KILL_COUNT = 8;
const ENEMY_HEALTH = 3;
const ENEMY_VOLLEY_PAUSE = 150;
const HITBOX_INSET = 10;

const SPAWN_INTERVAL = [110, 90, 75];
const ENEMY_SPEED = [1.6, 2.0, 2.4];
const MAX_ENEMIES = [2, 3, 3];
const SHOTS_PER_VOLLEY = [2, 3, 3];
const SHOT_GAP = [55, 45, 40];
const BULLET_SPEED_ENEMY = [4.0, 4.5, 5.0];
const DRIFT = [0.0, 1.0, 1.5];
const AIMED = [false, false, true];

const PLAYER_W = 60;
const PLAYER_H = 90;
const ENEMY_W = 60;
const ENEMY_H = 90;
const BULLET_W = 30;
const BULLET_H = 32;

// Batas kiri-kanan tepi sungai pada latar (dihitung dari gambar latar 1280x720).
const LEFT_BOUNDARY = 123;
const RIGHT_BOUNDARY = 1096;
const ENEMY_LEFT = LEFT_BOUNDARY;
const ENEMY_RIGHT = RIGHT_BOUNDARY + PLAYER_W - ENEMY_W;

const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

export class Minigame2 {
    constructor({ onWin }) {
        this.onWin = onWin;
        this.music = { name: 'soundtrack01', volume: 1 };
        this.touch = { stick: true, diagonal: true, shoot: true };
        this.hover = '';
        this.nextPending = false;
        this.reset();
    }

    leave() {
        this.releaseInput();
    }

    reset() {
        this.state = 'PLAYING';
        this.overlayAlpha = 0;
        this.health = PLAYER_MAX_HEALTH;
        this.kills = 0;
        this.invulnerable = 0;
        this.hitFlash = 0;
        this.frame = 0;
        this.lastStage = 0;
        this.stageBanner = 0;
        this.spawnCounter = 0;
        this.shootCooldown = 0;
        this.lastShootSound = 0;
        this.lastEnemyShotSound = 0;
        this.enemies = [];
        this.bullets = [];
        this.enemyBullets = [];
        this.bursts = [];
        this.releaseInput();
        this.playerX = WIDTH / 2 - PLAYER_W / 2;
        this.playerY = HEIGHT - PLAYER_H - 60;
    }

    releaseInput() {
        this.left = false;
        this.right = false;
        this.up = false;
        this.down = false;
        this.pointerShooting = false;
        this.keyShooting = false;
    }

    stage() {
        return Math.min(SPAWN_INTERVAL.length - 1, Math.floor(this.kills / 3));
    }

    // ---------- pembaruan ----------

    update() {
        this.frame++;
        if (this.invulnerable > 0) {
            this.invulnerable--;
        }
        if (this.hitFlash > 0) {
            this.hitFlash--;
        }
        if (this.stageBanner > 0) {
            this.stageBanner--;
        }
        this.bursts = this.bursts.filter((b) => ++b.age < b.life);

        if (this.state === 'PLAYING') {
            this.updatePlayer();
            this.updateShooting();
            this.updatePlayerBullets();
            this.updateEnemies();
            this.updateEnemyBullets();
            const s = this.stage();
            if (s !== this.lastStage) {
                this.lastStage = s;
                this.stageBanner = 150;
            }
        } else if (this.state === 'WINNING') {
            this.updateWinning();
        } else {
            this.overlayAlpha = Math.min(210, this.overlayAlpha + 4);
        }
    }

    updatePlayer() {
        const dx = (this.right ? 1 : 0) - (this.left ? 1 : 0);
        const dy = (this.down ? 1 : 0) - (this.up ? 1 : 0);
        const scale = dx !== 0 && dy !== 0 ? 0.7071 : 1.0;
        this.playerX = clamp(this.playerX + Math.round(dx * PLAYER_SPEED * scale), LEFT_BOUNDARY, RIGHT_BOUNDARY);
        this.playerY = clamp(this.playerY + Math.round(dy * PLAYER_SPEED * scale), HEIGHT / 2, HEIGHT - PLAYER_H);
    }

    updateShooting() {
        if (this.shootCooldown > 0) {
            this.shootCooldown--;
        }
        if ((this.pointerShooting || this.keyShooting) && this.shootCooldown === 0) {
            this.shootCooldown = BULLET_INTERVAL;
            this.bullets.push({ x: this.playerX + PLAYER_W / 2 - BULLET_W / 2, y: this.playerY - BULLET_H / 2 });
            const now = performance.now();
            if (now - this.lastShootSound > 180) {
                audio.play('shoot');
                this.lastShootSound = now;
            }
        }
    }

    updatePlayerBullets() {
        const remaining = [];
        for (const b of this.bullets) {
            b.y -= BULLET_SPEED;
            let hit = false;
            for (let i = 0; i < this.enemies.length; i++) {
                const en = this.enemies[i];
                if (b.x < en.x + ENEMY_W && b.x + BULLET_W > en.x && b.y < en.y + ENEMY_H && b.y + BULLET_H > en.y) {
                    en.health--;
                    en.flash = 6;
                    hit = true;
                    this.burst(b.x + BULLET_W / 2, b.y, 14, [255, 220, 120]);
                    if (en.health <= 0) {
                        this.enemies.splice(i, 1);
                        this.kills++;
                        this.burst(en.x + ENEMY_W / 2, en.y + ENEMY_H / 2, 46, [255, 140, 40]);
                        audio.play('enemy_death');
                    } else {
                        audio.play('shootenemy');
                    }
                    break;
                }
            }
            if (!hit && b.y + BULLET_H >= 0) {
                remaining.push(b);
            }
        }
        this.bullets = remaining;
        if (this.kills >= WIN_KILL_COUNT) {
            this.startWinning();
        }
    }

    burst(x, y, size, color) {
        this.bursts.push({ x, y, size, color, life: 22, age: 0 });
    }

    updateEnemies() {
        const s = this.stage();
        if (++this.spawnCounter >= SPAWN_INTERVAL[s] && this.enemies.length < MAX_ENEMIES[s] && this.kills < WIN_KILL_COUNT) {
            this.spawnCounter = 0;
            this.spawnEnemy(s);
        }

        const remaining = [];
        for (const en of this.enemies) {
            en.y += en.speedY;
            en.x += en.speedX;
            if (en.x < ENEMY_LEFT || en.x > ENEMY_RIGHT) {
                en.speedX = -en.speedX;
                en.x = clamp(en.x, ENEMY_LEFT, ENEMY_RIGHT);
            }
            if (en.flash > 0) {
                en.flash--;
            }
            this.updateEnemyFire(en, s);

            if (this.overlapsPlayer(Math.trunc(en.x), Math.trunc(en.y), ENEMY_W, ENEMY_H, 0)) {
                this.burst(en.x + ENEMY_W / 2, en.y + ENEMY_H / 2, 46, [255, 140, 40]);
                this.damagePlayer();
                continue;
            }
            if (en.y > HEIGHT) {
                continue;
            }
            remaining.push(en);
        }
        this.enemies = remaining;
    }

    updateEnemyFire(en, s) {
        if (en.y < 10) {
            return;
        }
        if (en.volleyShots > 0) {
            if (--en.shotTimer <= 0) {
                this.fireEnemyBullet(en, s);
                en.volleyShots--;
                en.shotTimer = SHOT_GAP[s];
                if (en.volleyShots === 0) {
                    en.volleyPause = ENEMY_VOLLEY_PAUSE;
                }
            }
        } else if (--en.volleyPause <= 0) {
            en.volleyShots = SHOTS_PER_VOLLEY[s];
            en.shotTimer = 20;
            en.warning = 20;
        }
        if (en.warning > 0) {
            en.warning--;
        }
    }

    fireEnemyBullet(en, s) {
        const speed = BULLET_SPEED_ENEMY[s];
        const startX = en.x + ENEMY_W / 2 - BULLET_W / 2;
        const startY = en.y + ENEMY_H - 10;
        let dx = 0;
        let dy = speed;
        if (AIMED[s]) {
            const tx = this.playerX + PLAYER_W / 2 - (startX + BULLET_W / 2);
            const ty = this.playerY + PLAYER_H / 2 - startY;
            const len = Math.hypot(tx, ty);
            if (len > 1 && ty > 0) {
                dx = tx / len * speed;
                dy = Math.max(speed * 0.6, ty / len * speed);
                dx = Math.max(-speed * 0.8, Math.min(speed * 0.8, dx));
            }
        }
        this.enemyBullets.push({ x: startX, y: startY, dx, dy });
        const now = performance.now();
        if (now - this.lastEnemyShotSound > 250) {
            this.lastEnemyShotSound = now;
            audio.play('shootplayer');
        }
    }

    updateEnemyBullets() {
        const remaining = [];
        for (const eb of this.enemyBullets) {
            eb.x += eb.dx;
            eb.y += eb.dy;
            if (this.overlapsPlayer(Math.trunc(eb.x) + 4, Math.trunc(eb.y) + 4, BULLET_W - 8, BULLET_H - 8, HITBOX_INSET)) {
                this.burst(eb.x + BULLET_W / 2, eb.y + BULLET_H / 2, 18, [255, 80, 80]);
                this.damagePlayer();
                if (this.state !== 'PLAYING') {
                    this.enemyBullets = [];
                    return;
                }
            } else if (!(eb.y > HEIGHT || eb.x < -BULLET_W || eb.x > WIDTH)) {
                remaining.push(eb);
            }
        }
        this.enemyBullets = remaining;
    }

    overlapsPlayer(x, y, w, h, inset) {
        const px = this.playerX + inset;
        const py = this.playerY + inset;
        const pw = PLAYER_W - 2 * inset;
        const ph = PLAYER_H - 2 * inset;
        return x < px + pw && x + w > px && y < py + ph && y + h > py;
    }

    damagePlayer() {
        if (this.invulnerable > 0 || this.state !== 'PLAYING') {
            return;
        }
        this.health--;
        this.invulnerable = INVULNERABLE_FRAMES;
        this.hitFlash = 24;
        audio.play('hitHurt');
        if (this.health <= 0) {
            this.health = 0;
            this.state = 'LOST';
            this.overlayAlpha = 0;
            this.enemyBullets = [];
            this.releaseInput();
            this.burst(this.playerX + PLAYER_W / 2, this.playerY + PLAYER_H / 2, 60, [255, 120, 40]);
        }
    }

    startWinning() {
        if (this.state !== 'PLAYING') {
            return;
        }
        this.state = 'WINNING';
        this.enemies.forEach((en) => this.burst(en.x + ENEMY_W / 2, en.y + ENEMY_H / 2, 40, [255, 140, 40]));
        this.enemies = [];
        this.enemyBullets = [];
        this.bullets = [];
        this.releaseInput();
        this.stageBanner = 0;
    }

    updateWinning() {
        this.playerY -= PLAYER_SPEED;
        if (this.playerY + PLAYER_H < 0) {
            this.state = 'WON';
            this.overlayAlpha = 0;
        }
    }

    spawnEnemy(s) {
        const span = Math.max(1, ENEMY_RIGHT - ENEMY_LEFT);
        const drift = DRIFT[s] === 0 ? 0 : (Math.random() < 0.5 ? -DRIFT[s] : DRIFT[s]);
        this.enemies.push({
            x: ENEMY_LEFT + Math.trunc(Math.random() * span),
            y: -ENEMY_H,
            speedY: ENEMY_SPEED[s],
            speedX: drift,
            health: ENEMY_HEALTH,
            flash: 0,
            warning: 0,
            volleyShots: 0,
            volleyPause: 25 + Math.trunc(Math.random() * 50),
            shotTimer: 0
        });
    }

    // ---------- input ----------

    action(name, pressed) {
        if (this.state === 'LOST' && pressed && name === 'reload') {
            this.reset();
            return;
        }
        if (name === 'L') this.left = pressed;
        else if (name === 'R') this.right = pressed;
        else if (name === 'U') this.up = pressed;
        else if (name === 'D') this.down = pressed;
        else if (name === 'space') this.keyShooting = pressed;
    }

    buttonRect() {
        return this.state === 'WON'
            ? { x: WIDTH / 2 - 80, y: HEIGHT / 2 + 60, w: 160, h: 44 }
            : { x: WIDTH / 2 - 75, y: HEIGHT / 2 + 60, w: 150, h: 44 };
    }

    buttonVisible() {
        return (this.state === 'WON' || this.state === 'LOST') && this.overlayAlpha >= 150;
    }

    pointerMove(x, y) {
        const r = this.buttonRect();
        this.hover = this.buttonVisible() && inside(x, y, r.x, r.y, r.w, r.h) ? 'button' : '';
    }

    pointerDown(x, y) {
        if (this.buttonVisible()) {
            const r = this.buttonRect();
            if (inside(x, y, r.x, r.y, r.w, r.h)) {
                audio.play('click');
                if (this.state === 'WON') {
                    if (!this.nextPending) {
                        this.nextPending = true;
                        setTimeout(() => this.onWin && this.onWin(), 150);
                    }
                } else {
                    this.reset();
                }
                return;
            }
        }
        this.pointerShooting = true;
    }

    pointerUp() {
        this.pointerShooting = false;
    }

    // ---------- gambar ----------

    draw(ctx) {
        const bg = img('background2');
        if (bg) {
            ctx.drawImage(bg, 0, 0, WIDTH, HEIGHT);
        }
        const enemyImage = img('mg2_enemy');
        const bulletImage = img('mg2_bullet');
        const playerImage = img('mg2_player');

        this.enemies.forEach((en) => this.drawEnemy(ctx, en, enemyImage));
        this.bullets.forEach((b) => bulletImage && ctx.drawImage(bulletImage, b.x, b.y, BULLET_W, BULLET_H));
        this.enemyBullets.forEach((eb) => this.drawEnemyBullet(ctx, eb, bulletImage));

        const blinkVisible = this.invulnerable === 0 || Math.floor(this.invulnerable / 5) % 2 === 0 || this.state === 'WINNING';
        if (this.state !== 'LOST' && blinkVisible && playerImage) {
            ctx.drawImage(playerImage, this.playerX, this.playerY, PLAYER_W, PLAYER_H);
        }
        this.bursts.forEach((b) => this.drawBurst(ctx, b));
        this.drawHud(ctx);

        if (this.hitFlash > 0) {
            ctx.fillStyle = `rgba(220,30,30,${Math.min(110, this.hitFlash * 5) / 255})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (this.state === 'WON') {
            this.drawOverlay(ctx, 'MISI SELESAI, PERJUANGAN MASIH BERLANJUT!', null, 36);
            if (this.buttonVisible()) {
                const r = this.buttonRect();
                button(ctx, 'NEXT', r.x, r.y, r.w, r.h, '#000', 20, this.hover === 'button');
            }
        } else if (this.state === 'LOST') {
            this.drawOverlay(ctx, 'TERKALAHKAN', 'PEJUANG TELAH GUGUR', 44);
            if (this.buttonVisible()) {
                const r = this.buttonRect();
                button(ctx, 'RESTART', r.x, r.y, r.w, r.h, '#000', 20, this.hover === 'button');
            }
        }
    }

    drawEnemy(ctx, en, image) {
        const x = Math.trunc(en.x);
        const y = Math.trunc(en.y);
        if (en.warning > 0) {
            ctx.fillStyle = `rgba(255,70,70,${(90 + (en.warning % 6) * 12) / 255})`;
            ctx.beginPath();
            ctx.ellipse(x + ENEMY_W / 2, y + ENEMY_H / 2, ENEMY_W / 2 + 6, ENEMY_H / 2 + 6, 0, 0, Math.PI * 2);
            ctx.fill();
        }
        if (image) {
            ctx.drawImage(image, x, y, ENEMY_W, ENEMY_H);
        }
        if (en.flash > 0) {
            ctx.fillStyle = 'rgba(255,255,255,0.47)';
            ctx.fillRect(x, y, ENEMY_W, ENEMY_H);
        }
        if (en.health < ENEMY_HEALTH) {
            ctx.fillStyle = 'rgba(0,0,0,0.67)';
            ctx.fillRect(x, y - 8, ENEMY_W, 5);
            ctx.fillStyle = 'rgb(230,70,60)';
            ctx.fillRect(x, y - 8, Math.trunc(ENEMY_W * en.health / ENEMY_HEALTH), 5);
        }
    }

    drawEnemyBullet(ctx, eb, image) {
        const x = Math.trunc(eb.x);
        const y = Math.trunc(eb.y);
        ctx.fillStyle = 'rgba(255,40,40,0.43)';
        ctx.beginPath();
        ctx.ellipse(x + BULLET_W / 2, y + BULLET_H / 2, BULLET_W / 2 + 4, BULLET_H / 2 + 4, 0, 0, Math.PI * 2);
        ctx.fill();
        if (image) {
            ctx.save();
            ctx.translate(x + BULLET_W / 2, y + BULLET_H / 2);
            ctx.rotate(Math.PI);
            ctx.drawImage(image, -BULLET_W / 2, -BULLET_H / 2, BULLET_W, BULLET_H);
            ctx.restore();
        }
    }

    drawBurst(ctx, b) {
        const t = b.age / b.life;
        const radius = Math.trunc(b.size * (0.3 + t));
        const alpha = Math.max(0, 200 * (1 - t)) / 255;
        const [r, g, bl] = b.color;
        ctx.strokeStyle = `rgba(${r},${g},${bl},${alpha})`;
        ctx.fillStyle = `rgba(${r},${g},${bl},${alpha})`;
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(b.x, b.y, radius, 0, Math.PI * 2);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(b.x, b.y, radius / 2, 0, Math.PI * 2);
        ctx.fill();
    }

    shadowText(ctx, text, x, y, color) {
        ctx.fillStyle = 'rgba(0,0,0,0.75)';
        ctx.fillText(text, x + 1, y + 1);
        ctx.fillStyle = color;
        ctx.fillText(text, x, y);
    }

    drawHud(ctx) {
        ctx.font = 'bold 16px Arial';
        this.shadowText(ctx, 'NYAWA', 20, 28, '#fff');
        for (let i = 0; i < PLAYER_MAX_HEALTH; i++) {
            drawHeart(ctx, 20 + i * 30, 36, 22, i < this.health);
        }

        const barW = 360;
        const barH = 18;
        const barX = (WIDTH - barW) / 2;
        const barY = 20;
        ctx.fillStyle = 'rgba(0,0,0,0.63)';
        ctx.beginPath();
        ctx.roundRect(barX - 3, barY - 3, barW + 6, barH + 6, 10);
        ctx.fill();
        ctx.fillStyle = 'rgb(70,70,70)';
        ctx.beginPath();
        ctx.roundRect(barX, barY, barW, barH, 8);
        ctx.fill();
        const filled = Math.trunc(barW * Math.min(this.kills, WIN_KILL_COUNT) / WIN_KILL_COUNT);
        if (filled > 0) {
            ctx.fillStyle = 'rgb(230,190,70)';
            ctx.beginPath();
            ctx.roundRect(barX, barY, filled, barH, 8);
            ctx.fill();
        }
        ctx.font = 'bold 14px Arial';
        const progress = `MUSUH DIKALAHKAN ${Math.min(this.kills, WIN_KILL_COUNT)} / ${WIN_KILL_COUNT}`;
        const tw = ctx.measureText(progress).width;
        this.shadowText(ctx, progress, barX + (barW - tw) / 2, barY + 14, '#fff');

        ctx.font = 'bold 16px Arial';
        const wave = `GELOMBANG ${this.stage() + 1}`;
        const ww = ctx.measureText(wave).width;
        this.shadowText(ctx, wave, (WIDTH - ww) / 2, barY + barH + 24, 'rgb(255,230,150)');

        if (this.stageBanner > 0 && this.state === 'PLAYING') {
            const alpha = Math.min(255, this.stageBanner * 4);
            ctx.font = 'bold 40px Arial';
            const bw = ctx.measureText(wave).width;
            ctx.fillStyle = `rgba(0,0,0,${alpha / 2 / 255})`;
            ctx.fillText(wave, (WIDTH - bw) / 2 + 2, HEIGHT / 2 - 100 + 2);
            ctx.fillStyle = `rgba(255,230,150,${alpha / 255})`;
            ctx.fillText(wave, (WIDTH - bw) / 2, HEIGHT / 2 - 100);
        }

        if (this.state === 'PLAYING' && this.frame < 360) {
            ctx.font = '16px Arial';
            const hint = 'Gerak: W A S D / panah / joystick   |   Tembak: klik kiri / Spasi / tombol TEMBAK';
            const hw = ctx.measureText(hint).width;
            this.shadowText(ctx, hint, (WIDTH - hw) / 2, HEIGHT - 20, '#fff');
        }
    }

    drawOverlay(ctx, title, subtitle, titleSize) {
        ctx.fillStyle = `rgba(0,0,0,${this.overlayAlpha / 255})`;
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        const textAlpha = Math.min(255, this.overlayAlpha * 255 / 210) / 255;
        ctx.fillStyle = `rgba(255,255,255,${textAlpha})`;
        ctx.textAlign = 'center';
        ctx.font = `bold ${titleSize}px Arial`;
        ctx.fillText(title, WIDTH / 2, HEIGHT / 2 - 30);
        if (subtitle) {
            ctx.font = '24px Arial';
            ctx.fillText(subtitle, WIDTH / 2, HEIGHT / 2 + 10);
        }
        ctx.textAlign = 'left';
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
