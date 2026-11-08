import { audio } from './audio.js';
import { Input } from './input.js';

export const WIDTH = 1280;
export const HEIGHT = 720;

const STEP = 1000 / 60;
const MAX_ACCUMULATED = STEP * 5;

/**
 * Mesin permainan: kanvas 1280x720 yang diskalakan ke layar, loop 60 langkah per detik,
 * dan pengantar input pointer/keyboard/sentuh ke scene aktif.
 */
export class Engine {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.scene = null;
        this.last = 0;
        this.accumulated = 0;
        this.input = new Input((action, pressed) => this.dispatchAction(action, pressed));
        this.resize = this.resize.bind(this);
        this.frame = this.frame.bind(this);
        this.bindPointer();
        window.addEventListener('resize', this.resize);
        window.addEventListener('orientationchange', this.resize);
        this.resize();
    }

    start() {
        this.last = performance.now();
        requestAnimationFrame(this.frame);
    }

    setScene(scene) {
        if (this.scene && this.scene.leave) {
            this.scene.leave();
        }
        this.input.releaseAll();
        this.scene = scene;
        this.input.configureTouch(scene.touch || null);
        if (scene.music) {
            audio.setMusic(scene.music.name, scene.music.volume);
        }
        if (scene.enter) {
            scene.enter();
        }
        this.accumulated = 0;
    }

    dispatchAction(action, pressed) {
        audio.unlock();
        if (this.scene && this.scene.action) {
            this.scene.action(action, pressed);
        }
    }

    bindPointer() {
        const toGame = (event) => {
            const rect = this.canvas.getBoundingClientRect();
            return {
                x: (event.clientX - rect.left) * WIDTH / rect.width,
                y: (event.clientY - rect.top) * HEIGHT / rect.height
            };
        };
        this.canvas.addEventListener('pointerdown', (event) => {
            event.preventDefault();
            if (event.pointerType === 'touch') {
                this.input.markTouchSeen();
            }
            audio.unlock();
            const point = toGame(event);
            if (this.scene && this.scene.pointerDown) {
                this.scene.pointerDown(point.x, point.y);
            }
        });
        const release = (event) => {
            const point = toGame(event);
            if (this.scene && this.scene.pointerUp) {
                this.scene.pointerUp(point.x, point.y);
            }
        };
        this.canvas.addEventListener('pointerup', release);
        this.canvas.addEventListener('pointercancel', release);
        this.canvas.addEventListener('pointermove', (event) => {
            const point = toGame(event);
            this.pointer = point;
            if (this.scene && this.scene.pointerMove) {
                this.scene.pointerMove(point.x, point.y);
            }
        });
        this.canvas.addEventListener('contextmenu', (event) => event.preventDefault());
        window.addEventListener('touchstart', () => this.input.markTouchSeen(), { passive: true });
    }

    resize() {
        const width = window.innerWidth;
        const height = window.innerHeight;
        const scale = Math.min(width / WIDTH, height / HEIGHT);
        this.canvas.style.width = `${Math.floor(WIDTH * scale)}px`;
        this.canvas.style.height = `${Math.floor(HEIGHT * scale)}px`;
    }

    frame(now) {
        this.accumulated = Math.min(this.accumulated + (now - this.last), MAX_ACCUMULATED);
        this.last = now;
        const scene = this.scene;
        if (scene) {
            while (this.accumulated >= STEP) {
                this.accumulated -= STEP;
                if (scene.update) {
                    scene.update();
                }
                if (this.scene !== scene) {
                    break;
                }
            }
            this.ctx.clearRect(0, 0, WIDTH, HEIGHT);
            if (this.scene.draw) {
                this.scene.draw(this.ctx);
            }
        }
        requestAnimationFrame(this.frame);
    }
}

/** Bantuan menggambar yang dipakai berbagai scene. */
export function drawCentered(ctx, text, y, x = WIDTH / 2) {
    ctx.textAlign = 'center';
    ctx.fillText(text, x, y);
    ctx.textAlign = 'left';
}

export function inside(px, py, x, y, w, h) {
    return px >= x && px <= x + w && py >= y && py <= y + h;
}

export function button(ctx, label, x, y, w, h, border, fontSize, hover = false) {
    ctx.fillStyle = hover ? '#deb887' : '#d2b48c';
    ctx.fillRect(x, y, w, h);
    ctx.strokeStyle = border;
    ctx.lineWidth = 2;
    ctx.strokeRect(x + 1, y + 1, w - 2, h - 2);
    ctx.fillStyle = '#000';
    ctx.font = `bold ${fontSize}px Arial`;
    ctx.textBaseline = 'middle';
    drawCentered(ctx, label, y + h / 2 + 1, x + w / 2);
    ctx.textBaseline = 'alphabetic';
}
