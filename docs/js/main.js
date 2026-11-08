import { Engine } from './core/engine.js';
import { loadAssets, preloadStory } from './core/assets.js';
import { audio } from './core/audio.js';
import { loadProgress, resetProgress, saveProgress } from './core/save.js';
import { DescriptionScene, ExitScene, MenuScene } from './scenes/menu.js';
import { StoryAkhirScene, StoryAwalScene, StoryScene } from './scenes/story.js';
import { MapsScene } from './scenes/maps.js';
import { Minigame1 } from './minigames/mg1.js';
import { Minigame2 } from './minigames/mg2.js';
import { Minigame3 } from './minigames/mg3.js';

const range = (from, to) => Array.from({ length: to - from + 1 }, (_, i) => from + i);

const engine = new Engine(document.getElementById('game'));
const go = (scene) => engine.setScene(scene);

/** Layar muat sederhana dengan bilah kemajuan. */
class LoadingScene {
    constructor() {
        this.progress = 0;
        this.ready = false;
        this.failed = false;
    }

    pointerDown() {
        if (this.ready) {
            this.onStart();
        }
    }

    action(name, pressed) {
        if (pressed && name === 'space' && this.ready) {
            this.onStart();
        }
    }

    draw(ctx) {
        ctx.fillStyle = '#000';
        ctx.fillRect(0, 0, 1280, 720);
        ctx.fillStyle = '#fff';
        ctx.textAlign = 'center';
        ctx.font = 'bold 40px Arial';
        ctx.fillText('THE AWAKENING OF SURABAYA', 640, 300);
        if (!this.ready) {
            ctx.fillStyle = '#444';
            ctx.fillRect(440, 350, 400, 12);
            ctx.fillStyle = '#d2b48c';
            ctx.fillRect(440, 350, 400 * this.progress, 12);
            ctx.fillStyle = '#aaa';
            ctx.font = '18px Arial';
            ctx.fillText('Memuat...', 640, 400);
        } else {
            ctx.fillStyle = '#d2b48c';
            ctx.font = 'bold 24px Arial';
            ctx.fillText('SENTUH LAYAR UNTUK MULAI', 640, 380);
        }
        ctx.textAlign = 'left';
    }
}

function showMenu() {
    go(new MenuScene({
        hasProgress: () => loadProgress() !== -1,
        onNewGame: () => {
            resetProgress();
            go(new StoryAwalScene(() => showMaps(0)));
        },
        onContinue: () => showMaps(loadProgress() + 1),
        onDescription: () => go(new DescriptionScene(showMenu)),
        onExit: () => go(new ExitScene(showMenu))
    }));
}

function showMaps(done) {
    go(new MapsScene(done, {
        onBack: showMenu,
        onGame: (index) => {
            if (index === 0) {
                preloadStory(range(1, 15));
                go(new StoryScene(range(1, 10), startMinigame1));
            } else if (index === 1) {
                go(new StoryScene(range(16, 20), startMinigame2));
            } else {
                go(new StoryScene(range(23, 27), startMinigame3));
            }
        }
    }));
}

function startMinigame1() {
    go(new Minigame1({
        onWin: () => saveProgress(0),
        onNext: () => go(new StoryScene(range(11, 15), () => showMaps(1)))
    }));
}

function startMinigame2() {
    go(new Minigame2({
        onWin: () => {
            saveProgress(1);
            go(new StoryScene(range(21, 22), () => showMaps(2)));
        }
    }));
}

function startMinigame3() {
    go(new Minigame3({
        onWin: () => {
            resetProgress();
            go(new StoryAkhirScene(showMenu));
        }
    }));
}

const loading = new LoadingScene();
loading.onStart = () => {
    audio.unlock();
    showMenu();
};
go(loading);
engine.start();
loadAssets((value) => {
    loading.progress = value;
}).then(() => {
    loading.ready = true;
});

// Tombol bantu: suara, layar penuh.
const soundButton = document.getElementById('btn-sound');
const fullButton = document.getElementById('btn-full');
const refreshSound = () => {
    soundButton.textContent = audio.muted ? '\u{1F507}' : '\u{1F50A}';
};
refreshSound();
soundButton.addEventListener('click', () => {
    audio.unlock();
    audio.toggleMute();
    refreshSound();
});
window.addEventListener('keydown', (event) => {
    if (event.code === 'KeyM') {
        audio.toggleMute();
        refreshSound();
    }
});

const root = document.documentElement;
if (!root.requestFullscreen && !root.webkitRequestFullscreen) {
    fullButton.style.display = 'none';
}
fullButton.addEventListener('click', () => {
    const active = document.fullscreenElement || document.webkitFullscreenElement;
    if (active) {
        (document.exitFullscreen || document.webkitExitFullscreen).call(document);
    } else {
        const request = root.requestFullscreen || root.webkitRequestFullscreen;
        const result = request.call(root);
        if (result && result.then) {
            result.then(() => {
                if (screen.orientation && screen.orientation.lock) {
                    screen.orientation.lock('landscape').catch(() => {});
                }
            }).catch(() => {});
        }
    }
});
