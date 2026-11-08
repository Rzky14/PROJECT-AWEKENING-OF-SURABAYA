const KEY = 'aos.progress';

export function saveProgress(value) {
    try {
        localStorage.setItem(KEY, String(value));
    } catch (e) {
        /* penyimpanan tidak tersedia */
    }
}

export function loadProgress() {
    try {
        const value = parseInt(localStorage.getItem(KEY), 10);
        return Number.isNaN(value) ? -1 : value;
    } catch (e) {
        return -1;
    }
}

export function resetProgress() {
    try {
        localStorage.removeItem(KEY);
    } catch (e) {
        /* penyimpanan tidak tersedia */
    }
}
