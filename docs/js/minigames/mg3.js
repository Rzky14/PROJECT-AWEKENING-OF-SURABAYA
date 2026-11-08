import { HEIGHT, WIDTH, button, inside } from '../core/engine.js';
import { img } from '../core/assets.js';
import { audio } from '../core/audio.js';

const MAGAZINE_SIZE = 10;
const SHOT_DELAY = 18;
const RELOAD_FRAMES = 75;
const STAGE_CLEAR_HEAL = 25;
const CONTACT_KNOCKBACK = 70;

const PLAYER_W = 140;
const PLAYER_H = 160;
const MAX_HEALTH = 100;
const MAX_X = 1240;
const PLAYER_INVULNERABLE = 50;
const JUMP_BUFFER_FRAMES = 8;
const GRAVITY = 0.9;
const JUMP_STRENGTH = 19;
const GROUND_Y = 470;
const TOP_LIMIT = 15;

const ENEMY_W = 180;
const ENEMY_H = 200;
const ENEMY_GROUND = 647;
const ENTER_SPEED = 4;
const SHOT_GAP = 70;
const PATTERN_CHARGE = -1;

const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));
const rnd = (n) => Math.floor(Math.random() * n);
const intersects = (a, b) => a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;

class Player {
    constructor() {
        this.reset();
    }

    reset() {
        this.x = 0;
        this.y = GROUND_Y;
        this.positionY = GROUND_Y;
        this.health = MAX_HEALTH;
        this.alive = true;
        this.dx = 0;
        this.speed = 6;
        this.jumping = false;
        this.jumpVelocity = 0;
        this.jumpBuffer = 0;
        this.invulnerable = 0;
        this.halted = false;
        this.walking = false;
        this.lastWalk = 0;
        this.lastHit = 0;
        this.image = 'player1';
        this.frame = 0;
        this.counter = 0;
    }

    hitbox() {
        return { x: this.x + 35, y: this.y + 25, w: 70, h: 120 };
    }

    update() {
        if (!this.alive) {
            this.stopWalk();
            return;
        }
        if (this.invulnerable > 0) {
            this.invulnerable--;
        }
        if (this.halted) {
            this.dx = 0;
        }
        this.x = clamp(this.x + this.dx, 0, MAX_X);

        if (this.jumping) {
            this.positionY -= this.jumpVelocity;
            this.jumpVelocity -= GRAVITY;
            if (this.positionY <= TOP_LIMIT) {
                this.positionY = TOP_LIMIT;
                this.jumpVelocity = Math.min(this.jumpVelocity, 0);
            }
            if (this.positionY >= GROUND_Y) {
                this.positionY = GROUND_Y;
                this.jumping = false;
                this.jumpVelocity = 0;
                if (this.jumpBuffer > 0 && !this.halted) {
                    this.startJump();
                }
            }
        }
        if (this.jumpBuffer > 0) {
            this.jumpBuffer--;
        }
        this.y = Math.round(this.positionY);

        if (this.jumping) {
            this.image = 'playerloncat';
            this.stopWalk();
        } else if (this.dx !== 0) {
            this.animate(this.dx > 0);
            this.playWalk();
        } else {
            this.image = 'player1';
            this.frame = 0;
            this.counter = 0;
            this.stopWalk();
        }
    }

    animate(right) {
        if (++this.counter >= 8) {
            this.counter = 0;
            this.frame = (this.frame + 1) % 6;
        }
        const odd = this.frame % 2 === 1;
        // Kanan: player2, player1, ...  Kiri: player1, player2, ...
        const second = right ? !odd : odd;
        this.image = second ? 'player2' : 'player1';
    }

    playWalk() {
        const now = performance.now();
        if (!this.halted && this.dx !== 0 && (!this.walking || now - this.lastWalk > 250)) {
            audio.play('walk');
            this.walking = true;
            this.lastWalk = now;
        } else if (this.dx === 0 || this.halted) {
            this.stopWalk();
        }
    }

    stopWalk() {
        if (this.walking) {
            audio.stop('walk');
            this.walking = false;
        }
    }

    setDirection(direction) {
        this.dx = this.halted ? 0 : direction * this.speed;
    }

    jump() {
        if (this.halted || !this.alive) {
            return false;
        }
        if (this.jumping) {
            this.jumpBuffer = JUMP_BUFFER_FRAMES;
            return false;
        }
        this.startJump();
        return true;
    }

    startJump() {
        this.jumping = true;
        this.jumpVelocity = JUMP_STRENGTH;
        this.jumpBuffer = 0;
        audio.play('jump');
    }

    releaseJump() {
        if (this.jumping && this.jumpVelocity > 6) {
            this.jumpVelocity = 6;
        }
        this.jumpBuffer = 0;
    }

    hurt(damage) {
        if (!this.alive || this.invulnerable > 0) {
            return false;
        }
        const now = performance.now();
        if (now - this.lastHit > 300) {
            audio.play('hitHurt');
            this.lastHit = now;
        }
        this.invulnerable = PLAYER_INVULNERABLE;
        this.health -= damage;
        if (this.health <= 0) {
            this.health = 0;
            this.die();
        }
        return true;
    }

    heal(amount) {
        if (this.alive) {
            this.health = Math.min(MAX_HEALTH, this.health + amount);
        }
    }

    die() {
        this.alive = false;
        this.halted = true;
        this.dx = 0;
        this.stopWalk();
        audio.play('enemy_death');
    }

    stop() {
        this.dx = 0;
        this.halted = true;
        this.stopWalk();
        this.image = 'player1';
        this.frame = 0;
        this.counter = 0;
    }

    draw(ctx, cameraX) {
        if (!this.alive) {
            return;
        }
        const visible = this.invulnerable === 0 || Math.floor(this.invulnerable / 4) % 2 === 0;
        const image = img(this.image);
        if (visible && image) {
            ctx.drawImage(image, this.x - cameraX, this.y, PLAYER_W, PLAYER_H);
        }
        const barW = 60;
        const barX = this.x - cameraX + (PLAYER_W - barW) / 2;
        const barY = this.y + 10;
        ctx.fillStyle = 'rgba(0,0,0,0.59)';
        ctx.fillRect(barX, barY, barW, 6);
        ctx.fillStyle = this.health > 30 ? 'rgb(80,220,90)' : 'rgb(230,70,60)';
        ctx.fillRect(barX, barY, Math.trunc(this.health / MAX_HEALTH * barW), 6);
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 1;
        ctx.strokeRect(barX + 0.5, barY + 0.5, barW, 6);
    }
}

class EnemyBullet {
    constructor(x, y, speed, high, damage) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.high = high;
        this.damage = damage;
        this.active = true;
    }

    update() {
        this.x -= this.speed;
        if (this.x < -20) {
            this.active = false;
        }
    }

    bounds() {
        return { x: this.x, y: this.y, w: 20, h: 20 };
    }

    draw(ctx, cameraX) {
        const sx = this.x - cameraX;
        ctx.fillStyle = this.high ? 'rgba(255,210,40,0.47)' : 'rgba(255,50,50,0.43)';
        ctx.beginPath();
        ctx.ellipse(sx + 10, this.y + 10, 15, 15, 0, 0, Math.PI * 2);
        ctx.fill();
        const image = img('peluru');
        if (image) {
            ctx.drawImage(image, sx, this.y, 20, 20);
        }
    }
}

let enemyIds = 0;

class Enemy {
    constructor(startX, health, speed, shootMin, shootMax, bulletSpeed, bulletDamage, isBoss) {
        this.id = `enemy${++enemyIds}`;
        this.x = startX;
        this.posX = startX;
        this.maxHealth = health;
        this.hitCount = 0;
        this.alive = true;
        this.moveSpeed = speed;
        this.shootMin = shootMin;
        this.shootMax = shootMax;
        this.bulletSpeed = bulletSpeed;
        this.bulletDamage = bulletDamage;
        this.isBoss = isBoss;
        this.w = isBoss ? ENEMY_W + 4 : ENEMY_W;
        this.h = isBoss ? ENEMY_H + 4 : ENEMY_H;
        this.y = ENEMY_GROUND - this.h;
        this.leftBound = 500;
        this.rightBound = 1200;
        this.mode = 'ENTER';
        this.timer = 0;
        this.cooldown = 0;
        this.pattern = 0;
        this.shotQueue = null;
        this.shotIndex = 0;
        this.currentAttack = 0;
        this.hitFlash = 0;
        this.moving = false;
        this.walking = false;
        this.currentFrame = 0;
        this.animationTimer = 0;
        this.lastHit = 0;
        this.targetX = 0;
    }

    setBounds(left, right) {
        this.leftBound = left;
        this.rightBound = right;
    }

    entering() {
        return this.mode === 'ENTER';
    }

    phase() {
        const ratio = (this.maxHealth - this.hitCount) / this.maxHealth;
        return ratio > 0.66 ? 1 : ratio > 0.33 ? 2 : 3;
    }

    healthRatio() {
        return Math.max(0, (this.maxHealth - this.hitCount) / this.maxHealth);
    }

    update(sink) {
        if (!this.alive) {
            this.stopWalk();
            return;
        }
        if (this.hitFlash > 0) {
            this.hitFlash--;
        }
        this.moving = false;

        switch (this.mode) {
            case 'ENTER':
                this.posX -= ENTER_SPEED;
                this.moving = true;
                if (this.posX <= this.rightBound) {
                    this.posX = this.rightBound;
                    this.mode = 'MOVE';
                    this.cooldown = 45 + rnd(60);
                    this.pickTarget();
                }
                break;
            case 'MOVE':
                this.moveToward();
                if (--this.cooldown <= 0) {
                    this.beginAttack();
                }
                break;
            case 'TELEGRAPH':
                if (--this.timer <= 0) {
                    this.startAttack();
                }
                break;
            case 'SHOOT':
                this.runShooting(sink);
                break;
            case 'CHARGE':
                this.moving = true;
                this.posX -= this.phase() === 3 ? 9 : 8;
                if (this.posX <= 120) {
                    this.posX = 120;
                    this.mode = 'RECOVER';
                    this.timer = 50;
                }
                break;
            case 'RECOVER':
                if (--this.timer <= 0) {
                    if (this.currentAttack === PATTERN_CHARGE) {
                        this.mode = 'RETURN';
                    } else {
                        this.mode = 'MOVE';
                        this.scheduleNext();
                        this.pickTarget();
                    }
                }
                break;
            case 'RETURN':
                this.moving = true;
                this.posX += 6;
                if (this.posX >= this.rightBound) {
                    this.posX = this.rightBound;
                    this.mode = 'MOVE';
                    this.scheduleNext();
                    this.pickTarget();
                }
                break;
            default:
                break;
        }

        if (this.mode !== 'ENTER' && this.mode !== 'CHARGE' && this.mode !== 'RETURN') {
            this.posX = Math.max(this.leftBound, Math.min(this.posX, this.rightBound));
        }
        this.x = Math.round(this.posX);

        if (this.moving) {
            this.playWalk();
            if (++this.animationTimer >= 10) {
                this.animationTimer = 0;
                if (++this.currentFrame >= 4) {
                    this.currentFrame = 1;
                }
            }
        } else {
            this.stopWalk();
            this.currentFrame = 0;
            this.animationTimer = 0;
        }
    }

    moveToward() {
        const diff = this.targetX - this.posX;
        if (Math.abs(diff) <= this.moveSpeed) {
            this.posX = this.targetX;
            if (rnd(90) === 0) {
                this.pickTarget();
            }
            return;
        }
        this.posX += Math.sign(diff) * this.moveSpeed;
        this.moving = true;
    }

    pickTarget() {
        this.targetX = this.leftBound + rnd(Math.max(1, this.rightBound - this.leftBound + 1));
    }

    scheduleNext() {
        const fast = this.isBoss && this.phase() === 3;
        const min = fast ? Math.trunc(this.shootMin * 2 / 3) : this.shootMin;
        const max = fast ? Math.trunc(this.shootMax * 2 / 3) : this.shootMax;
        this.cooldown = min + rnd(Math.max(1, max - min + 1));
    }

    beginAttack() {
        this.currentAttack = this.nextAttack();
        this.mode = 'TELEGRAPH';
        this.timer = this.isBoss ? (this.phase() === 3 ? 32 : 42) : 28;
    }

    nextAttack() {
        if (!this.isBoss) {
            return 0;
        }
        const phase = this.phase();
        const order = phase === 1 ? [0, 0, 3] : phase === 2 ? [1, 0, 3, 1] : [2, 3, 1, 3];
        const pick = order[this.pattern % order.length];
        this.pattern++;
        return pick === 3 ? PATTERN_CHARGE : pick;
    }

    startAttack() {
        if (this.currentAttack === PATTERN_CHARGE) {
            this.mode = 'CHARGE';
            return;
        }
        this.shotQueue = this.currentAttack === 1 ? [0, 1] : this.currentAttack === 2 ? [0, 1, 0] : [0];
        this.shotIndex = 0;
        this.timer = 0;
        this.mode = 'SHOOT';
    }

    runShooting(sink) {
        if (this.timer > 0) {
            this.timer--;
            return;
        }
        this.fire(sink, this.shotQueue[this.shotIndex] === 1);
        this.shotIndex++;
        if (this.shotIndex >= this.shotQueue.length) {
            this.mode = 'RECOVER';
            this.timer = 35;
            this.currentAttack = 0;
        } else {
            this.timer = SHOT_GAP;
        }
    }

    fire(sink, high) {
        const speed = this.bulletSpeed + (this.isBoss && this.phase() === 3 ? 1 : 0);
        sink.push(new EnemyBullet(this.x + 44, high ? this.y + 4 : this.y + 92, speed, high, this.bulletDamage));
        audio.play('shootenemy');
    }

    playWalk() {
        if (!this.walking) {
            audio.playLoop(this.id, 'walk');
            this.walking = true;
        }
    }

    stopWalk() {
        if (this.walking) {
            audio.stopLoop(this.id);
            this.walking = false;
        }
    }

    hitbox() {
        const h = this.isBoss ? Math.trunc(150 / ENEMY_H * this.h) : 150;
        return { x: this.x + 65, y: this.y + 40, w: 90, h };
    }

    contactBox() {
        return { x: this.x + 75, y: this.y + 60, w: 70, h: 120 };
    }

    contactDamage() {
        return this.mode === 'CHARGE' ? 15 : 8;
    }

    isHit(bullet) {
        return this.alive && this.mode !== 'ENTER'
            && intersects(this.hitbox(), { x: bullet.x, y: bullet.y, w: 20, h: 20 });
    }

    hit() {
        if (!this.alive) {
            return;
        }
        const now = performance.now();
        if (now - this.lastHit > 350) {
            audio.play('hitHurt');
            this.lastHit = now;
        }
        this.hitFlash = 6;
        this.hitCount++;
        if (this.hitCount >= this.maxHealth) {
            this.die();
        }
    }

    die() {
        this.alive = false;
        this.stopWalk();
        audio.play('enemy_death');
    }

    draw(ctx, cameraX) {
        const sx = this.x - cameraX;
        if (this.mode === 'TELEGRAPH' && this.alive) {
            const pulse = 70 + (this.timer % 8) * 12;
            ctx.fillStyle = `rgba(255,60,40,${Math.min(200, pulse) / 255})`;
            ctx.beginPath();
            ctx.ellipse(sx + 10 + (this.w - 20) / 2, this.y + 20 + (this.h - 30) / 2, (this.w - 20) / 2, (this.h - 30) / 2, 0, 0, Math.PI * 2);
            ctx.fill();
        }
        const names = this.isBoss ? ['boss1', 'boss2', 'boss3', 'boss1'] : ['enemy1', 'enemy2', 'enemy3', 'enemy1'];
        const image = img(names[this.currentFrame]);
        if (image) {
            ctx.drawImage(image, sx, this.y, this.w, this.h);
        }
        if (this.hitFlash > 0 && this.alive) {
            ctx.fillStyle = 'rgba(255,255,255,0.43)';
            ctx.beginPath();
            ctx.ellipse(sx + 40 + (this.w - 80) / 2, this.y + 40 + (this.h - 80) / 2, (this.w - 80) / 2, (this.h - 80) / 2, 0, 0, Math.PI * 2);
            ctx.fill();
        }
        if (!this.alive) {
            return;
        }
        if (this.mode === 'TELEGRAPH') {
            ctx.font = 'bold 34px Arial';
            ctx.fillStyle = 'rgba(0,0,0,0.67)';
            ctx.fillText('!', sx + this.w / 2 - 6, this.y - 14);
            ctx.fillStyle = this.currentAttack === PATTERN_CHARGE ? 'rgb(255,140,30)' : 'rgb(255,70,60)';
            ctx.fillText('!', sx + this.w / 2 - 8, this.y - 16);
        }
        if (!this.isBoss) {
            const barW = 50;
            const barX = sx + (this.w - barW) / 2;
            const barY = this.y + 10;
            ctx.fillStyle = 'rgb(150,30,30)';
            ctx.fillRect(barX, barY, barW, 6);
            ctx.fillStyle = 'rgb(80,220,90)';
            ctx.fillRect(barX, barY, Math.trunc(this.healthRatio() * barW), 6);
            ctx.strokeStyle = '#000';
            ctx.lineWidth = 1;
            ctx.strokeRect(barX + 0.5, barY + 0.5, barW, 6);
        }
    }
}

class Flag {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.height = 150;
        this.raiseY = 0;
        this.waving = true;
        this.taken = false;
        this.raising = false;
        this.waveOffset = 0;
    }

    draw(ctx, cameraX) {
        const sx = this.x - cameraX;
        const sy = this.y - this.height - this.raiseY;
        ctx.fillStyle = '#404040';
        ctx.fillRect(sx, sy, 5, this.height);
        if (this.waving || this.raising) {
            this.waveOffset += 0.15;
            if (this.waveOffset > 2 * Math.PI) {
                this.waveOffset -= 2 * Math.PI;
            }
            for (let i = 0; i < 60; i += 10) {
                const wave = Math.trunc(Math.sin(this.waveOffset + i * 0.5) * 5);
                ctx.fillStyle = '#f00';
                ctx.fillRect(sx + 5 + i, sy, 10, 20 + wave);
                ctx.fillStyle = '#fff';
                ctx.fillRect(sx + 5 + i, sy + 20 + wave, 10, 20 - wave);
            }
        }
    }
}

export class Minigame3 {
    constructor({ onWin }) {
        this.onWin = onWin;
        this.music = { name: 'backsound', volume: 1 };
        this.touch = { dpad: true, jump: true, shoot: true, reload: true };
        this.player = new Player();
        this.clouds = [
            { x: 500, speed: 1 },
            { x: 200, speed: 2 },
            { x: 100, speed: 1 }
        ];
        this.hover = false;
        this.resetGame();
    }

    leave() {
        this.stopEnemySounds();
        this.player.stopWalk();
    }

    resetGame() {
        this.stopEnemySounds();
        this.player.reset();
        this.enemies = [];
        this.enemyBullets = [];
        this.bullets = [];
        this.effects = [];
        this.stage = 1;
        this.cameraX = 0;
        this.frame = 0;
        this.gameFinished = false;
        this.flag = null;
        this.showTransition = false;
        this.transitionText = '';
        this.transitionAlpha = 0;
        this.transitionFrames = 0;
        this.transitionLength = 0;
        this.nextStage = 0;
        this.showVictory = false;
        this.left = false;
        this.right = false;
        this.shooting = false;
        this.shotCooldown = 0;
        this.ammo = MAGAZINE_SIZE;
        this.reloadTimer = 0;
        this.hitFlash = 0;
        this.fadeOutAlpha = 0;
        this.startFadeOut = false;
        this.winTriggered = false;
        this.finishedFrames = 0;
        this.spawnEnemies(1);
    }

    stopEnemySounds() {
        (this.enemies || []).forEach((e) => e.stopWalk());
    }

    spawnEnemies(stage) {
        this.enemies = [];
        this.enemyBullets = [];
        if (stage === 1) {
            const a = new Enemy(1400, 7, 1.6, 150, 230, 5, 8, false);
            a.setBounds(900, 1200);
            const b = new Enemy(1560, 7, 1.6, 170, 250, 5, 8, false);
            b.setBounds(560, 900);
            this.enemies.push(a, b);
        } else if (stage === 2) {
            const a = new Enemy(1400, 10, 2.2, 100, 160, 6, 8, false);
            a.setBounds(880, 1200);
            const b = new Enemy(1560, 10, 2.2, 120, 180, 6, 8, false);
            b.setBounds(520, 900);
            this.enemies.push(a, b);
        } else if (stage === 3) {
            const boss = new Enemy(1400, 40, 2.4, 70, 120, 6, 10, true);
            boss.setBounds(700, 1200);
            this.enemies.push(boss);
        }
    }

    // ---------- pembaruan ----------

    update() {
        this.frame++;
        this.effects = this.effects.filter((e) => ++e.age < e.life);
        if (this.hitFlash > 0) {
            this.hitFlash--;
        }
        if (!this.player.alive) {
            this.stopEnemySounds();
            return;
        }
        this.updateTransition();
        this.updatePlayer();
        this.updateShooting();
        this.updateEnemies();
        this.updateEnemyBullets();
        this.updateFlag();
        this.updateVictory();
        this.clouds.forEach((c) => {
            c.x -= c.speed;
            if (c.x + 150 < 0) {
                c.x = 1280 + Math.trunc(Math.random() * 300);
            }
        });
    }

    updatePlayer() {
        if (!this.gameFinished) {
            this.player.setDirection((this.right ? 1 : 0) - (this.left ? 1 : 0));
        }
        this.player.update();
        this.cameraX = Math.max(0, Math.min(this.player.x - 400, 100));
    }

    effect(x, y, size, color, life) {
        this.effects.push({ x, y, size, color, life, text: null, age: 0 });
    }

    textEffect(text, x, y, color) {
        this.effects.push({ x, y, size: 0, color, life: 50, text, age: 0 });
    }

    updateShooting() {
        const p = this.player;
        if (this.shotCooldown > 0) {
            this.shotCooldown--;
        }
        if (this.reloadTimer > 0) {
            if (--this.reloadTimer === 0) {
                this.ammo = MAGAZINE_SIZE;
            }
        } else if (this.shooting && !this.gameFinished && this.shotCooldown === 0 && this.ammo > 0) {
            this.bullets.push({ x: p.x + 125, y: p.y + 64, speed: 15, active: true });
            audio.play('shootplayer');
            this.effect(p.x + 135, p.y + 72, 16, [255, 230, 140], 8);
            this.shotCooldown = SHOT_DELAY;
            this.ammo--;
            if (this.ammo === 0) {
                this.startReload();
            }
        }
        this.bullets.forEach((b) => {
            b.x += b.speed;
            if (b.x > 10000) {
                b.active = false;
            }
        });
        this.bullets = this.bullets.filter((b) => b.active && b.x - this.cameraX <= WIDTH);
    }

    startReload() {
        if (this.reloadTimer === 0 && this.ammo < MAGAZINE_SIZE) {
            this.reloadTimer = RELOAD_FRAMES;
        }
    }

    updateEnemies() {
        const p = this.player;
        const remaining = [];
        for (const enemy of this.enemies) {
            enemy.update(this.enemyBullets);

            for (const b of this.bullets) {
                if (b.active && enemy.isHit(b)) {
                    enemy.hit();
                    b.active = false;
                    this.effect(b.x + 10, b.y + 10, 22, [255, 200, 90], 12);
                    if (!enemy.alive) {
                        break;
                    }
                }
            }
            this.bullets = this.bullets.filter((b) => b.active);

            if (enemy.alive && !enemy.entering() && intersects(enemy.contactBox(), p.hitbox())) {
                const damage = enemy.contactDamage();
                if (p.hurt(damage)) {
                    this.onPlayerHurt(damage);
                    p.x = Math.max(0, p.x - CONTACT_KNOCKBACK);
                }
            }

            if (!enemy.alive) {
                this.effect(enemy.x + 90, enemy.y + 100, enemy.isBoss ? 120 : 70, [255, 140, 40], 26);
                continue;
            }
            remaining.push(enemy);
        }
        const hadEnemies = this.enemies.length > 0;
        this.enemies = remaining;
        if (hadEnemies && remaining.length === 0) {
            this.onStageCleared();
        }
    }

    onStageCleared() {
        this.enemyBullets = [];
        if (this.stage === 1) {
            this.healAfterStage();
            this.startTransition('MUSUH BERIKUTNYA LEBIH KUAT', 2, 150);
        } else if (this.stage === 2) {
            this.healAfterStage();
            this.startTransition('BOS AKAN DATANG!', 3, 110);
        } else if (this.stage === 3) {
            this.startTransition('AMBIL BENDERA UNTUK MENANG!', 0, 110);
            this.flag = new Flag(1290, 630);
        }
    }

    healAfterStage() {
        const before = this.player.health;
        this.player.heal(STAGE_CLEAR_HEAL);
        if (this.player.health > before) {
            this.textEffect(`+${this.player.health - before} NYAWA`, this.player.x + 70, this.player.y, [120, 255, 140]);
        }
    }

    updateEnemyBullets() {
        const p = this.player;
        const remaining = [];
        for (const bullet of this.enemyBullets) {
            bullet.update();
            if (!bullet.active) {
                continue;
            }
            if (intersects(bullet.bounds(), p.hitbox()) && p.hurt(bullet.damage)) {
                this.onPlayerHurt(bullet.damage);
                continue;
            }
            remaining.push(bullet);
        }
        this.enemyBullets = remaining;
    }

    onPlayerHurt(damage) {
        this.hitFlash = 14;
        this.textEffect(`-${damage}`, this.player.x + 70, this.player.y, [255, 100, 100]);
    }

    updateFlag() {
        const flag = this.flag;
        if (!flag || this.gameFinished) {
            return;
        }
        if (!flag.taken && this.player.x + 100 >= flag.x) {
            flag.taken = true;
            flag.raising = true;
            this.player.stop();
            this.left = false;
            this.right = false;
        }
        if (flag.raising) {
            flag.raiseY += 2;
            if (flag.raiseY >= 50) {
                flag.raising = false;
                this.showVictory = true;
                this.gameFinished = true;
                this.finishedFrames = 0;
            }
        }
    }

    updateVictory() {
        if (this.gameFinished && ++this.finishedFrames >= 120) {
            this.startFadeOut = true;
        }
        if (this.startFadeOut && this.fadeOutAlpha < 1) {
            this.fadeOutAlpha += 0.02;
            if (this.fadeOutAlpha >= 1) {
                this.fadeOutAlpha = 1;
                if (!this.winTriggered) {
                    this.winTriggered = true;
                    if (this.onWin) {
                        this.onWin();
                    }
                }
            }
        }
    }

    startTransition(message, stageAfter, length) {
        this.transitionText = message;
        this.showTransition = true;
        this.transitionAlpha = 0;
        this.transitionFrames = length;
        this.transitionLength = length;
        this.nextStage = stageAfter;
    }

    updateTransition() {
        if (this.transitionFrames <= 0) {
            return;
        }
        this.transitionFrames--;
        const elapsed = this.transitionLength - this.transitionFrames;
        const span = Math.min(this.transitionLength, 100);
        if (elapsed >= span) {
            this.showTransition = false;
        } else {
            this.transitionAlpha = Math.min(1, Math.min(elapsed, span - elapsed) / 20);
        }
        if (this.transitionFrames === 0) {
            this.showTransition = false;
            if (this.nextStage !== 0) {
                this.stage = this.nextStage;
                this.spawnEnemies(this.stage);
                this.nextStage = 0;
            }
        }
    }

    // ---------- input ----------

    action(name, pressed) {
        const p = this.player;
        if (!p.alive) {
            if (pressed && name === 'reload') {
                this.resetGame();
            }
            return;
        }
        if (this.gameFinished) {
            return;
        }
        if (name === 'L') {
            this.left = pressed;
        } else if (name === 'R') {
            this.right = pressed;
        } else if (name === 'U') {
            if (pressed) {
                p.jump();
            } else {
                p.releaseJump();
            }
        } else if (name === 'space') {
            this.shooting = pressed;
        } else if (name === 'reload' && pressed) {
            this.startReload();
        }
    }

    pointerMove(x, y) {
        this.hover = !this.player.alive && inside(x, y, WIDTH / 2 - 75, 485, 150, 40);
    }

    pointerDown(x, y) {
        if (!this.player.alive) {
            if (inside(x, y, WIDTH / 2 - 75, 485, 150, 40)) {
                audio.play('click');
                this.resetGame();
            }
            return;
        }
        if (!this.gameFinished) {
            this.shooting = true;
        }
    }

    pointerUp() {
        this.shooting = false;
    }

    // ---------- gambar ----------

    draw(ctx) {
        const bg = img('background3');
        const cam = this.cameraX;
        if (bg) {
            for (let x = -(cam % WIDTH) - WIDTH; x < WIDTH; x += WIDTH) {
                ctx.drawImage(bg, x, 0, WIDTH, HEIGHT);
            }
        }
        const cloud = img('cloud');
        if (cloud) {
            this.clouds.forEach((c) => ctx.drawImage(cloud, c.x - cam, 0, 150, 100));
        }
        this.enemies.forEach((e) => e.draw(ctx, cam));
        this.player.draw(ctx, cam);
        if (!this.player.alive) {
            this.drawDeath(ctx);
        }
        const bulletImage = img('peluru');
        this.bullets.forEach((b) => bulletImage && ctx.drawImage(bulletImage, b.x - cam, b.y, 20, 20));
        this.enemyBullets.forEach((b) => b.draw(ctx, cam));
        if (this.flag) {
            this.flag.draw(ctx, cam);
        }
        this.effects.forEach((e) => this.drawEffect(ctx, e, cam));
        this.drawHud(ctx);

        if (this.hitFlash > 0 && this.player.alive) {
            ctx.fillStyle = `rgba(220,30,30,${Math.min(100, this.hitFlash * 8) / 255})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (this.showTransition) {
            ctx.globalAlpha = clamp(this.transitionAlpha, 0, 1);
            ctx.font = 'bold 35px Arial';
            ctx.textAlign = 'center';
            ctx.fillStyle = 'rgba(0,0,0,0.59)';
            ctx.fillText(this.transitionText, WIDTH / 2 + 3, 383);
            ctx.fillStyle = '#fff';
            ctx.fillText(this.transitionText, WIDTH / 2, 380);
            ctx.textAlign = 'left';
            ctx.globalAlpha = 1;
        }
        if (this.showVictory) {
            ctx.font = 'bold 50px Arial';
            ctx.textAlign = 'center';
            ctx.fillStyle = 'rgba(0,0,0,0.78)';
            ctx.fillText('KEMENANGAN TELAH BERHASIL', WIDTH / 2 + 4, 304);
            ctx.fillStyle = '#fff';
            ctx.fillText('KEMENANGAN TELAH BERHASIL', WIDTH / 2, 300);
            ctx.textAlign = 'left';
        }
        if (this.startFadeOut) {
            ctx.fillStyle = `rgba(0,0,0,${Math.min(1, this.fadeOutAlpha)})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    drawDeath(ctx) {
        ctx.fillStyle = 'rgba(0,0,0,0.71)';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        ctx.fillStyle = '#fff';
        ctx.textAlign = 'center';
        ctx.font = 'bold 36px Arial';
        ctx.fillText('KAMU TELAH MATI!', WIDTH / 2, 380);
        ctx.font = 'italic 24px Georgia, serif';
        ctx.fillText('PERLAWANAN PENJAJAH GAGAL!', WIDTH / 2, 420);
        ctx.font = '20px Arial, sans-serif';
        ctx.fillText('PENCET TOMBOL "R" ATAU RESTART UNTUK MENGULANG', WIDTH / 2, 455);
        ctx.textAlign = 'left';
        button(ctx, 'RESTART', WIDTH / 2 - 75, 485, 150, 40, '#000', 18, this.hover);
    }

    drawEffect(ctx, e, cam) {
        const t = e.age / e.life;
        const alpha = Math.max(0, 230 * (1 - t)) / 255;
        const [r, g, b] = e.color;
        const sx = e.x - cam;
        if (e.text) {
            ctx.font = 'bold 22px Arial';
            const w = ctx.measureText(e.text).width;
            ctx.fillStyle = `rgba(0,0,0,${alpha})`;
            ctx.fillText(e.text, sx - w / 2 + 1, e.y - e.age + 1);
            ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`;
            ctx.fillText(e.text, sx - w / 2, e.y - e.age);
            return;
        }
        const radius = Math.trunc(e.size * (0.3 + t));
        ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`;
        ctx.beginPath();
        ctx.arc(sx, e.y, radius, 0, Math.PI * 2);
        ctx.fill();
    }

    shadowText(ctx, text, x, y, color) {
        ctx.fillStyle = 'rgba(0,0,0,0.75)';
        ctx.fillText(text, x + 1, y + 1);
        ctx.fillStyle = color;
        ctx.fillText(text, x, y);
    }

    drawBar(ctx, x, y, w, h, ratio, fill, label) {
        ctx.fillStyle = 'rgba(0,0,0,0.67)';
        ctx.beginPath();
        ctx.roundRect(x - 3, y - 3, w + 6, h + 6, 10);
        ctx.fill();
        ctx.fillStyle = 'rgb(60,60,60)';
        ctx.beginPath();
        ctx.roundRect(x, y, w, h, 8);
        ctx.fill();
        const filled = Math.trunc(w * clamp(ratio, 0, 1));
        if (filled > 0) {
            ctx.fillStyle = fill;
            ctx.beginPath();
            ctx.roundRect(x, y, filled, h, 8);
            ctx.fill();
        }
        ctx.font = `bold ${Math.max(11, h - 6)}px Arial`;
        const tw = ctx.measureText(label).width;
        this.shadowText(ctx, label, x + (w - tw) / 2, y + h - 4, '#fff');
    }

    drawHud(ctx) {
        const p = this.player;
        if (!p.alive) {
            return;
        }
        this.drawBar(ctx, 20, 20, 260, 20, p.health / MAX_HEALTH,
            p.health > 30 ? 'rgb(80,220,90)' : 'rgb(230,70,60)', `NYAWA  ${p.health} / ${MAX_HEALTH}`);

        ctx.font = 'bold 16px Arial';
        if (this.reloadTimer > 0) {
            this.drawBar(ctx, 20, 48, 260, 14, 1 - this.reloadTimer / RELOAD_FRAMES, 'rgb(240,200,80)', 'MENGISI ULANG...');
        } else {
            const low = this.ammo <= 3;
            const text = `PELURU  ${this.ammo} / ${MAGAZINE_SIZE}${low ? '   (R: isi ulang)' : ''}`;
            this.shadowText(ctx, text, 22, 62, low ? 'rgb(255,150,120)' : '#fff');
            for (let i = 0; i < MAGAZINE_SIZE; i++) {
                ctx.fillStyle = i < this.ammo ? 'rgb(255,215,90)' : 'rgba(70,70,70,0.75)';
                ctx.beginPath();
                ctx.roundRect(22 + i * 14, 68, 10, 14, 4);
                ctx.fill();
            }
        }

        ctx.font = 'bold 18px Arial';
        const stageText = `TAHAP ${Math.min(this.stage, 3)} / 3`;
        const sw = ctx.measureText(stageText).width;
        this.shadowText(ctx, stageText, WIDTH - sw - 24, 36, '#fff');

        this.enemies.forEach((e) => {
            if (e.isBoss && e.alive && !e.entering()) {
                const w = 520;
                this.drawBar(ctx, (WIDTH - w) / 2, 24, w, 22, e.healthRatio(), 'rgb(210,50,50)', 'BOS PENJAJAH');
            }
        });

        if (this.frame < 360 && this.stage === 1) {
            ctx.font = '16px Arial';
            const hint = 'Gerak: A D / panah   |   Lompat: W / panah atas   |   Tembak: Spasi / klik   |   R: isi ulang';
            const hw = ctx.measureText(hint).width;
            this.shadowText(ctx, hint, (WIDTH - hw) / 2, HEIGHT - 20, '#fff');
        }
    }
}
