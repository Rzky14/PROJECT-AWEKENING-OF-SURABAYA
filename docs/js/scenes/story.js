import { HEIGHT, WIDTH, button, drawCentered, inside } from '../core/engine.js';
import { img, story } from '../core/assets.js';
import { audio } from '../core/audio.js';

const SOUNDTRACK = { name: 'soundtrack', volume: 1 };

/** Rangkaian gambar cerita; Spasi atau sentuhan untuk lanjut. */
export class StoryScene {
    constructor(numbers, onDone) {
        this.numbers = numbers;
        this.onDone = onDone;
        this.index = 0;
        this.music = SOUNDTRACK;
        this.done = false;
    }

    enter() {
        this.numbers.slice(0, 3).forEach(story);
    }

    advance() {
        if (this.done) {
            return;
        }
        audio.play('click');
        this.index++;
        if (this.index >= this.numbers.length) {
            this.done = true;
            this.onDone();
            return;
        }
        story(this.numbers[Math.min(this.index + 1, this.numbers.length - 1)]);
    }

    pointerDown() {
        this.advance();
    }

    action(name, pressed) {
        if (pressed && name === 'space') {
            this.advance();
        }
    }

    draw(ctx) {
        ctx.fillStyle = '#000';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        const image = story(this.numbers[Math.min(this.index, this.numbers.length - 1)]);
        if (image) {
            ctx.drawImage(image, 0, 0, WIDTH, HEIGHT);
        }
    }
}

const TEKS = 'PERLAWANAN RAKYAT SURABAYA MELAWAN PARA PENJAJAH UNTUK MERDEKA';
const TYPING_MS = 70;
const SOUND_INTERVAL_MS = 450;
const BOX = { x: 140, y: 285, width: 1000 };

export class StoryAwalScene {
    constructor(onDone) {
        this.onDone = onDone;
        this.music = { name: 'soundtrack', volume: 0 };
        this.count = 0;
        this.nextChar = 0;
        this.lastSound = -Infinity;
        this.finished = false;
    }

    enter() {
        this.nextChar = performance.now() + TYPING_MS;
    }

    update() {
        const now = performance.now();
        if (this.count < TEKS.length && now >= this.nextChar) {
            this.count++;
            this.nextChar += TYPING_MS;
            if (now - this.lastSound >= SOUND_INTERVAL_MS) {
                audio.play('type');
                this.lastSound = now;
            }
            if (this.count === TEKS.length) {
                audio.stop('type');
            }
        }
    }

    typingDone() {
        return this.count >= TEKS.length;
    }

    advance() {
        if (this.typingDone() && !this.finished) {
            this.finished = true;
            audio.play('click');
            this.onDone();
        }
    }

    pointerDown() {
        this.advance();
    }

    action(name, pressed) {
        if (pressed && name === 'space') {
            this.advance();
        }
    }

    draw(ctx) {
        ctx.fillStyle = '#000';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        const background = img('BGTeks');
        if (background) {
            ctx.drawImage(background, 0, 0, WIDTH, HEIGHT);
        }
        ctx.font = 'bold 36px Arial';
        ctx.fillStyle = '#906b4c';
        const lines = wrap(ctx, TEKS.slice(0, this.count), BOX.width);
        lines.forEach((line, i) => drawCentered(ctx, line, BOX.y + 33 + i * 41, BOX.x + BOX.width / 2));
    }
}

function wrap(ctx, text, maxWidth) {
    const words = text.split(' ');
    const lines = [];
    let line = '';
    words.forEach((word, i) => {
        const trial = line ? `${line} ${word}` : word;
        if (ctx.measureText(trial).width > maxWidth && line) {
            lines.push(line);
            line = word;
        } else {
            line = trial;
        }
        if (i === words.length - 1) {
            lines.push(line);
        }
    });
    return lines;
}

const FADE_PER_STEP = 0.02 * (1000 / 60) / 30;
const END_BUTTON = { x: 540, y: 320, w: 200, h: 40 };

export class StoryAkhirScene {
    constructor(onDone) {
        this.onDone = onDone;
        this.music = SOUNDTRACK;
        this.numbers = [28, 29, 30];
        this.index = 0;
        this.fadeAlpha = 0;
        this.fadingOut = false;
        this.showEnd = false;
        this.hover = false;
    }

    enter() {
        this.numbers.forEach(story);
    }

    update() {
        if (this.fadingOut && !this.showEnd) {
            this.fadeAlpha = Math.min(1, this.fadeAlpha + FADE_PER_STEP);
            if (this.fadeAlpha >= 1) {
                this.showEnd = true;
            }
        }
    }

    advance() {
        if (this.fadingOut) {
            return;
        }
        audio.play('click');
        this.index++;
        if (this.index >= this.numbers.length) {
            this.fadingOut = true;
            this.fadeAlpha = 0;
        }
    }

    pointerMove(x, y) {
        this.hover = this.showEnd && inside(x, y, END_BUTTON.x, END_BUTTON.y, END_BUTTON.w, END_BUTTON.h);
    }

    pointerDown(x, y) {
        if (this.showEnd) {
            if (inside(x, y, END_BUTTON.x, END_BUTTON.y, END_BUTTON.w, END_BUTTON.h)) {
                audio.play('click');
                this.onDone();
            }
            return;
        }
        this.advance();
    }

    action(name, pressed) {
        if (pressed && name === 'space' && !this.showEnd) {
            this.advance();
        }
    }

    draw(ctx) {
        ctx.fillStyle = '#000';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        const image = story(this.numbers[Math.min(this.index, this.numbers.length - 1)]);
        if (image) {
            ctx.drawImage(image, 0, 0, WIDTH, HEIGHT);
        }
        if (this.fadingOut) {
            ctx.fillStyle = `rgba(0,0,0,${Math.min(1, this.fadeAlpha)})`;
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (this.showEnd) {
            ctx.fillStyle = '#000';
            ctx.fillRect(0, 0, WIDTH, HEIGHT);
            ctx.fillStyle = '#fff';
            ctx.font = 'bold 28px Arial';
            ctx.textBaseline = 'middle';
            drawCentered(ctx, 'PERJUANGAN RAKYAT SURABAYA TELAH BERHASIL', 280);
            ctx.textBaseline = 'alphabetic';
            button(ctx, 'TAMAT', END_BUTTON.x, END_BUTTON.y, END_BUTTON.w, END_BUTTON.h, '#a0522d', 16, this.hover);
        }
    }
}
