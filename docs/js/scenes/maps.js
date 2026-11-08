import { HEIGHT, WIDTH, inside } from '../core/engine.js';
import { img } from '../core/assets.js';
import { audio } from '../core/audio.js';

const POSITIONS = [[335, 170], [590, 350], [910, 425]];

export class MapsScene {
    /** done: jumlah minigame yang sudah selesai (0..3). */
    constructor(done, { onGame, onBack }) {
        this.status = ['active', 'locked', 'locked'];
        for (let i = 0; i < done && i < 3; i++) {
            this.status[i] = 'done';
            if (i + 1 < 3) {
                this.status[i + 1] = 'active';
            }
        }
        this.onGame = onGame;
        this.onBack = onBack;
        this.music = { name: 'soundtrack', volume: 1 };
    }

    pointerDown(x, y) {
        for (let i = 0; i < 3; i++) {
            const [bx, by] = POSITIONS[i];
            if (inside(x, y, bx, by, 100, 100) && this.status[i] === 'active') {
                audio.play('click');
                this.onGame(i);
                return;
            }
        }
        if (inside(x, y, 40, 620, 80, 80)) {
            audio.play('click');
            this.onBack();
        }
    }

    draw(ctx) {
        const background = img('BGMaps');
        if (background) {
            ctx.drawImage(background, 0, 0, WIDTH, HEIGHT);
        }
        const icons = { done: 'Done', active: 'Point', locked: 'Lock' };
        POSITIONS.forEach(([bx, by], i) => {
            const icon = img(icons[this.status[i]]);
            if (icon) {
                ctx.drawImage(icon, bx + 20, by + 20, 60, 60);
            }
        });
        const back = img('Back');
        if (back) {
            ctx.drawImage(back, 40, 620, 80, 80);
        }
    }
}
