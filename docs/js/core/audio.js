const SOUNDS = [
    'Bs_Minigame1', 'Hurt', 'backsound', 'click', 'enemy_death', 'hitHurt', 'jump', 'laserShoot',
    'pickradio', 'shoot', 'shootenemy', 'shootplayer', 'soundtrack', 'soundtrack01', 'type', 'walk', 'walkenemy'
];

const FADE_SECONDS = 0.5;
const STORE_KEY = 'aos.muted';

class AudioManager {
    constructor() {
        this.ctx = null;
        this.buffers = new Map();
        this.pending = new Map();
        this.active = new Map();
        this.loops = new Map();
        this.music = null;
        this.musicName = '';
        this.musicTarget = 1;
        this.muted = false;
        try {
            this.muted = localStorage.getItem(STORE_KEY) === '1';
        } catch (e) {
            this.muted = false;
        }
    }

    /** Dipanggil dari gestur pengguna pertama (wajib di iOS/Chrome). */
    unlock() {
        if (!this.ctx) {
            const Ctx = window.AudioContext || window.webkitAudioContext;
            if (!Ctx) {
                return;
            }
            this.ctx = new Ctx();
            this.master = this.ctx.createGain();
            this.master.gain.value = this.muted ? 0 : 1;
            this.master.connect(this.ctx.destination);
            SOUNDS.forEach((name) => this.fetchBuffer(name));
        }
        if (this.ctx.state === 'suspended') {
            this.ctx.resume();
        }
        if (this.musicName && !this.music) {
            this.music = null;
            this.startMusic(this.musicName, this.musicTarget);
        }
    }

    fetchBuffer(name) {
        if (this.buffers.has(name)) {
            return Promise.resolve(this.buffers.get(name));
        }
        if (this.pending.has(name)) {
            return this.pending.get(name);
        }
        const request = fetch(`assets/snd/${name}.mp3`)
            .then((response) => response.arrayBuffer())
            .then((data) => new Promise((resolve, reject) => this.ctx.decodeAudioData(data, resolve, reject)))
            .then((buffer) => {
                this.buffers.set(name, buffer);
                return buffer;
            })
            .catch(() => null);
        this.pending.set(name, request);
        return request;
    }

    play(name, volume = 1) {
        if (!this.ctx) {
            return;
        }
        const buffer = this.buffers.get(name);
        if (!buffer) {
            return;
        }
        const source = this.ctx.createBufferSource();
        const gain = this.ctx.createGain();
        gain.gain.value = volume;
        source.buffer = buffer;
        source.connect(gain).connect(this.master);
        const list = this.active.get(name) || [];
        list.push(source);
        this.active.set(name, list);
        source.onended = () => {
            const current = this.active.get(name) || [];
            this.active.set(name, current.filter((s) => s !== source));
        };
        source.start();
    }

    /** Suara berulang yang dikenali dengan id (misalnya langkah musuh). */
    playLoop(id, name, volume = 1) {
        if (!this.ctx || this.loops.has(id)) {
            return;
        }
        const buffer = this.buffers.get(name);
        if (!buffer) {
            return;
        }
        const source = this.ctx.createBufferSource();
        const gain = this.ctx.createGain();
        gain.gain.value = volume;
        source.buffer = buffer;
        source.loop = true;
        source.connect(gain).connect(this.master);
        source.start();
        this.loops.set(id, source);
    }

    stopLoop(id) {
        const source = this.loops.get(id);
        if (source) {
            try {
                source.stop();
            } catch (e) {
                /* sudah berhenti */
            }
            this.loops.delete(id);
        }
    }

    stop(name) {
        (this.active.get(name) || []).forEach((source) => {
            try {
                source.stop();
            } catch (e) {
                /* sudah berhenti */
            }
        });
        this.active.set(name, []);
    }

    /** Musik latar: ganti lagu bila berbeda, lalu fade ke volume tujuan. */
    setMusic(name, volume) {
        this.musicTarget = volume;
        if (name !== this.musicName) {
            this.stopMusic();
            this.musicName = name;
            this.startMusic(name, volume);
            return;
        }
        this.fadeMusic(volume);
    }

    startMusic(name, volume) {
        if (!this.ctx) {
            return;
        }
        this.fetchBuffer(name).then((buffer) => {
            if (!buffer || this.musicName !== name || this.music) {
                return;
            }
            const source = this.ctx.createBufferSource();
            const gain = this.ctx.createGain();
            source.buffer = buffer;
            source.loop = true;
            gain.gain.value = 0;
            source.connect(gain).connect(this.master);
            source.start();
            this.music = { source, gain };
            this.fadeMusic(this.musicTarget);
        });
    }

    fadeMusic(volume) {
        this.musicTarget = volume;
        if (!this.music) {
            return;
        }
        const { gain } = this.music;
        const now = this.ctx.currentTime;
        gain.gain.cancelScheduledValues(now);
        gain.gain.setValueAtTime(gain.gain.value, now);
        gain.gain.linearRampToValueAtTime(volume, now + FADE_SECONDS);
    }

    stopMusic() {
        if (this.music) {
            try {
                this.music.source.stop();
            } catch (e) {
                /* sudah berhenti */
            }
            this.music = null;
        }
    }

    toggleMute() {
        this.muted = !this.muted;
        if (this.master) {
            this.master.gain.value = this.muted ? 0 : 1;
        }
        try {
            localStorage.setItem(STORE_KEY, this.muted ? '1' : '0');
        } catch (e) {
            /* penyimpanan tidak tersedia */
        }
        return this.muted;
    }
}

export const audio = new AudioManager();
