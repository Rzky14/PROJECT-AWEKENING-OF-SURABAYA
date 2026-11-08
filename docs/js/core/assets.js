const IMG = 'assets/img/';
const STORY = 'assets/story/';

const IMAGE_FILES = [
    'BGMenu', 'BGDialog', 'BGMaps', 'BGDeskripsi', 'BGMinigame1', 'background2', 'background3',
    'Mulai', 'Deskripsi', 'Keluar', 'btn_Pbaru', 'btn_lanjut', 'Back', 'Point', 'Done', 'Lock',
    'cloud', 'Peta', 'Genteng', 'Genteng1', 'Genteng2', 'Radio',
    'enemy_belakang', 'enemy_depan', 'enemy_samping_kanan', 'enemy_samping_kiri',
    'player_belakang', 'player_depan', 'player_samping_kanan', 'player_samping_kiri',
    'player1', 'player2', 'playerloncat', 'enemy1', 'enemy2', 'enemy3', 'boss1', 'boss2', 'boss3',
    'mg2_player', 'mg2_enemy', 'mg2_bullet', 'peluru'
];

const images = new Map();

function load(src) {
    return new Promise((resolve) => {
        const image = new Image();
        image.onload = () => resolve(image);
        image.onerror = () => resolve(null);
        image.src = src;
    });
}

/** Memuat semua gambar utama; gambar cerita dimuat saat dibutuhkan. */
export async function loadAssets(onProgress) {
    let done = 0;
    const total = IMAGE_FILES.length + 1;
    const tick = () => onProgress && onProgress(++done / total);

    await Promise.all(IMAGE_FILES.map(async (name) => {
        images.set(name, await load(`${IMG}${name}.webp`));
        tick();
    }));
    images.set('BGTeks', await load(`${STORY}BGTeks.webp`));
    tick();
}

export function img(name) {
    return images.get(name) || null;
}

/** Gambar cerita bernomor 1..30. Dimuat sekali lalu disimpan. */
export function story(number) {
    const key = `story${number}`;
    if (!images.has(key)) {
        images.set(key, null);
        load(`${STORY}${number}.webp`).then((image) => images.set(key, image));
    }
    return images.get(key);
}

export function preloadStory(numbers) {
    numbers.forEach(story);
}
