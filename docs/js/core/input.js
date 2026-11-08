const KEY_MAP = {
    KeyA: 'L', ArrowLeft: 'L',
    KeyD: 'R', ArrowRight: 'R',
    KeyW: 'U', ArrowUp: 'U',
    KeyS: 'D', ArrowDown: 'D',
    Space: 'space', Enter: 'space',
    KeyR: 'reload'
};

const STICK_DEAD_ZONE = 0.28;

/**
 * Menyatukan keyboard dan kontrol sentuh menjadi aksi yang sama:
 * L, R, U, D, space, reload. Scene menerima aksi lewat engine.
 */
export class Input {
    constructor(onAction) {
        this.onAction = onAction;
        this.down = new Set();
        this.stickDiagonal = false;
        this.touchSeen = false;
        this.bindKeyboard();
        this.bindTouchControls();
    }

    set(action, pressed) {
        if (pressed === this.down.has(action)) {
            return;
        }
        if (pressed) {
            this.down.add(action);
        } else {
            this.down.delete(action);
        }
        this.onAction(action, pressed);
    }

    releaseAll() {
        [...this.down].forEach((action) => this.set(action, false));
    }

    bindKeyboard() {
        window.addEventListener('keydown', (event) => {
            const action = KEY_MAP[event.code];
            if (!action) {
                return;
            }
            event.preventDefault();
            if (!event.repeat) {
                this.set(action, true);
            }
        });
        window.addEventListener('keyup', (event) => {
            const action = KEY_MAP[event.code];
            if (action) {
                event.preventDefault();
                this.set(action, false);
            }
        });
        window.addEventListener('blur', () => this.releaseAll());
        document.addEventListener('visibilitychange', () => this.releaseAll());
    }

    bindTouchControls() {
        document.querySelectorAll('#touch button[data-action]').forEach((button) => {
            const action = button.dataset.action === 'shoot' ? 'space' : button.dataset.action === 'left' ? 'L'
                : button.dataset.action === 'right' ? 'R' : button.dataset.action === 'up' ? 'U' : button.dataset.action;
            const press = (event) => {
                event.preventDefault();
                button.setPointerCapture(event.pointerId);
                button.classList.add('down');
                this.set(action, true);
            };
            const release = (event) => {
                event.preventDefault();
                button.classList.remove('down');
                this.set(action, false);
            };
            button.addEventListener('pointerdown', press);
            button.addEventListener('pointerup', release);
            button.addEventListener('pointercancel', release);
            button.addEventListener('lostpointercapture', release);
            button.addEventListener('contextmenu', (event) => event.preventDefault());
        });

        const stick = document.getElementById('stick');
        const knob = document.getElementById('knob');
        let activeId = null;

        const update = (event) => {
            const rect = stick.getBoundingClientRect();
            const radius = rect.width / 2;
            let dx = (event.clientX - (rect.left + radius)) / radius;
            let dy = (event.clientY - (rect.top + radius)) / radius;
            const length = Math.hypot(dx, dy);
            if (length > 1) {
                dx /= length;
                dy /= length;
            }
            knob.style.transform = `translate(${dx * radius * 0.6}px, ${dy * radius * 0.6}px)`;

            const ax = Math.abs(dx);
            const ay = Math.abs(dy);
            let horizontal = 0;
            let vertical = 0;
            if (Math.max(ax, ay) >= STICK_DEAD_ZONE) {
                if (this.stickDiagonal) {
                    horizontal = ax >= STICK_DEAD_ZONE && ax >= ay * 0.5 ? Math.sign(dx) : 0;
                    vertical = ay >= STICK_DEAD_ZONE && ay >= ax * 0.5 ? Math.sign(dy) : 0;
                } else if (ax >= ay) {
                    horizontal = Math.sign(dx);
                } else {
                    vertical = Math.sign(dy);
                }
            }
            this.set('L', horizontal < 0);
            this.set('R', horizontal > 0);
            this.set('U', vertical < 0);
            this.set('D', vertical > 0);
        };

        const end = (event) => {
            if (event.pointerId !== activeId) {
                return;
            }
            activeId = null;
            knob.style.transform = '';
            this.set('L', false);
            this.set('R', false);
            this.set('U', false);
            this.set('D', false);
        };

        stick.addEventListener('pointerdown', (event) => {
            event.preventDefault();
            activeId = event.pointerId;
            stick.setPointerCapture(event.pointerId);
            update(event);
        });
        stick.addEventListener('pointermove', (event) => {
            if (event.pointerId === activeId) {
                update(event);
            }
        });
        stick.addEventListener('pointerup', end);
        stick.addEventListener('pointercancel', end);
        stick.addEventListener('lostpointercapture', end);
    }

    /** Atur tombol sentuh yang tampil untuk scene saat ini. */
    configureTouch(config) {
        const show = (id, visible, display = 'block') => {
            document.getElementById(id).style.display = visible ? display : 'none';
        };
        const touch = document.getElementById('touch');
        const any = Boolean(config);
        touch.hidden = !(any && this.touchSeen);
        this.touchConfig = config;
        const c = config || {};
        show('stick', c.stick);
        show('dpad', c.dpad, 'flex');
        show('btn-shoot', c.shoot);
        show('btn-jump', c.jump);
        show('btn-reload', c.reload);
        this.stickDiagonal = Boolean(c.diagonal);
        this.releaseAll();
    }

    markTouchSeen() {
        if (this.touchSeen) {
            return;
        }
        this.touchSeen = true;
        const touch = document.getElementById('touch');
        touch.hidden = !this.touchConfig;
    }
}
