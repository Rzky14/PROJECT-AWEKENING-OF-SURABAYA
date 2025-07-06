package menu;

import java.io.*;

public class SaveGameManager {

    private static final String SAVE_FILE = "save.dat";

    public static void saveProgress(int progress) {
        try (PrintWriter out = new PrintWriter(SAVE_FILE)) {
            out.println(progress);
        } catch (IOException e) {
            System.err.println("Gagal menyimpan progres: " + e.getMessage());
        }
    }

    public static int loadProgress() {
        try (BufferedReader in = new BufferedReader(new FileReader(SAVE_FILE))) {
            return Integer.parseInt(in.readLine());
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    public static void resetProgress() {
        new File(SAVE_FILE).delete();
    }
}
