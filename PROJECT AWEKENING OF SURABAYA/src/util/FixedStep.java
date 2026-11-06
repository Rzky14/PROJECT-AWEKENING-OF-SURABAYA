package util;

/**
 * Penghitung langkah waktu tetap (60 pembaruan per detik) agar kecepatan permainan
 * sama di semua komputer, meskipun timer Swing berjalan tidak rata.
 */
public final class FixedStep {

    public static final int STEPS_PER_SECOND = 60;
    private static final long STEP_NANOS = 1_000_000_000L / STEPS_PER_SECOND;
    private static final long MAX_ACCUMULATED = STEP_NANOS * 5;

    private long lastTime = System.nanoTime();
    private long accumulated = 0;

    /** Mulai ulang pengukuran, dipanggil saat permainan dimulai atau dilanjutkan. */
    public void reset() {
        lastTime = System.nanoTime();
        accumulated = 0;
    }

    /** Jumlah langkah pembaruan yang perlu dijalankan sejak pemanggilan terakhir. */
    public int consume() {
        long now = System.nanoTime();
        accumulated = Math.min(accumulated + (now - lastTime), MAX_ACCUMULATED);
        lastTime = now;
        int steps = 0;
        while (accumulated >= STEP_NANOS) {
            accumulated -= STEP_NANOS;
            steps++;
        }
        return steps;
    }
}
