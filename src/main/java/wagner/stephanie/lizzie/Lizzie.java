package wagner.stephanie.lizzie;

import org.json.JSONException;
import wagner.stephanie.lizzie.analysis.Leelaz;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.gui.LizzieFrame;

import javax.swing.*;
import java.io.IOException;

/**
 * Main class.
 */
public class Lizzie {
    public static LizzieFrame frame;
    public static Leelaz leelaz;
    public static Board board;
    public static Config config;
    public static String lizzieVersion = "0.3";

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException, JSONException {
        config = new Config();
        leelaz = new Leelaz();
        leelaz.togglePonder();

        board = new Board();

        frame = new LizzieFrame();
    }

    public static void shutdown() {
        int ret = JOptionPane.showConfirmDialog(null, "Do you want to save this SGF?", "Save SGF?", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            LizzieFrame.saveSgf();
        }

        leelaz.shutdown();
        System.exit(0);
    }

}
