import { HEIGHT, WIDTH, drawCentered, inside } from '../core/engine.js';
import { img } from '../core/assets.js';
import { audio } from '../core/audio.js';

const SOUNDTRACK = { name: 'soundtrack', volume: 1 };

const GRID = { x: 150, y: 250, width: 980, height: 220, gap: 30 };
const CELL_WIDTH = (GRID.width - GRID.gap * 2) / 3;

const DIALOG = { width: 640, height: 240 };
DIALOG.x = (WIDTH - DIALOG.width) / 2;
DIALOG.y = (HEIGHT - DIALOG.height) / 2;
const DIALOG_BUTTONS = [
    { image: 'btn_Pbaru', action: 'new', x: DIALOG.x + 80, y: DIALOG.y + 115 },
    { image: 'btn_lanjut', action: 'continue', x: DIALOG.x + 330, y: DIALOG.y + 115 }
];

export class MenuScene {
    constructor({ onNewGame, onContinue, onDescription, onExit, hasProgress }) {
        this.onNewGame = onNewGame;
        this.onContinue = onContinue;
        this.onDescription = onDescription;
        this.onExit = onExit;
        this.hasProgress = hasProgress;
        this.music = SOUNDTRACK;
        this.dialogOpen = false;
        this.notice = '';
        this.noticeTimer = 0;
    }

    cell(index) {
        return { x: GRID.x + index * (CELL_WIDTH + GRID.gap), y: GRID.y, w: CELL_WIDTH, h: GRID.height };
    }

    pointerDown(x, y) {
        if (this.dialogOpen) {
            this.dialogPointer(x, y);
            return;
        }
        const actions = [
            () => {
                this.dialogOpen = true;
                this.notice = '';
            },
            this.onDescription,
            this.onExit
        ];
        for (let i = 0; i < 3; i++) {
            const c = this.cell(i);
            if (inside(x, y, c.x, c.y, c.w, c.h)) {
                audio.play('click');
                actions[i]();
                return;
            }
        }
    }

    dialogPointer(x, y) {
        for (const item of DIALOG_BUTTONS) {
            if (inside(x, y, item.x, item.y, 230, 50)) {
                audio.play('click');
                if (item.action === 'new') {
                    this.dialogOpen = false;
                    this.onNewGame();
                } else if (this.hasProgress()) {
                    this.dialogOpen = false;
                    this.onContinue();
                } else {
                    this.notice = 'Belum ada progres tersimpan.';
                    this.noticeTimer = 150;
                }
                return;
            }
        }
        if (!inside(x, y, DIALOG.x, DIALOG.y, DIALOG.width, DIALOG.height)) {
            this.dialogOpen = false;
        }
    }

    action(name, pressed) {
        if (pressed && name === 'space' && !this.dialogOpen) {
            audio.play('click');
            this.dialogOpen = true;
        }
    }

    update() {
        if (this.noticeTimer > 0 && --this.noticeTimer === 0) {
            this.notice = '';
        }
    }

    draw(ctx) {
        const background = img('BGMenu');
        if (background) {
            ctx.drawImage(background, 0, 0, WIDTH, HEIGHT);
        }
        ['Mulai', 'Deskripsi', 'Keluar'].forEach((name, i) => {
            const c = this.cell(i);
            const icon = img(name);
            if (icon) {
                ctx.drawImage(icon, c.x + (c.w - 272) / 2, c.y + (c.h - 75) / 2, 272, 75);
            }
        });

        if (this.dialogOpen) {
            ctx.fillStyle = 'rgba(0,0,0,0.55)';
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
            const bg = img('BGDialog');
            if (bg) {
                ctx.drawImage(bg, DIALOG.x, DIALOG.y, DIALOG.width, DIALOG.height);
            }
            DIALOG_BUTTONS.forEach((item) => {
                const image = img(item.image);
                if (image) {
                    ctx.drawImage(image, item.x, item.y, 230, 50);
                }
            });
            if (this.notice) {
                ctx.fillStyle = '#fff';
                ctx.font = 'bold 20px Arial';
                drawCentered(ctx, this.notice, DIALOG.y + DIALOG.height - 28);
            }
        }
    }
}

export class DescriptionScene {
    constructor(onBack) {
        this.onBack = onBack;
        this.music = SOUNDTRACK;
    }

    pointerDown(x, y) {
        if (inside(x, y, 40, 620, 80, 80)) {
            audio.play('click');
            this.onBack();
        }
    }

    action(name, pressed) {
        if (pressed && name === 'space') {
            this.onBack();
        }
    }

    draw(ctx) {
        const background = img('BGDeskripsi');
        if (background) {
            ctx.drawImage(background, 0, 0, WIDTH, HEIGHT);
        }
        const back = img('Back');
        if (back) {
            ctx.drawImage(back, 40, 620, 80, 80);
        }
    }
}

/** Layar penutup karena halaman web tidak bisa menutup dirinya seperti aplikasi desktop. */
export class ExitScene {
    constructor(onBack) {
        this.onBack = onBack;
        this.music = { name: 'soundtrack', volume: 0 };
    }

    pointerDown() {
        audio.play('click');
        this.onBack();
    }

    action(name, pressed) {
        if (pressed) {
            this.onBack();
        }
    }

    draw(ctx) {
        ctx.fillStyle = '#000';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 36px Arial';
        drawCentered(ctx, 'TERIMA KASIH TELAH BERMAIN', HEIGHT / 2 - 10);
        ctx.font = '20px Arial';
        ctx.fillStyle = '#d2b48c';
        drawCentered(ctx, 'Sentuh layar untuk kembali ke menu', HEIGHT / 2 + 36);
    }
}
