package wagner.stephanie.lizzie;

import org.json.JSONException;
import wagner.stephanie.lizzie.analysis.Leelaz;
import wagner.stephanie.lizzie.analysis.Leelaz.WinrateStats;
import wagner.stephanie.lizzie.plugin.PluginManager;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.Stone;
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
    public static String lizzieVersion = "0.4";

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException, JSONException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        config = new Config();

        PluginManager.loadPlugins();

        board = new Board();

        frame = new LizzieFrame();
        
        new Thread( () -> {
            try {
                leelaz = new Leelaz();
                if(config.handicapInsteadOfWinrate) {
                	leelaz.estimatePassWinrate();
                }
                leelaz.togglePonder();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        
    }

    public static void shutdown() {
        PluginManager.onShutdown();
        if (config.config.getJSONObject("ui").getBoolean("confirm-exit")) {
            int ret = JOptionPane.showConfirmDialog(null, "Do you want to save this SGF?", "Save SGF?", JOptionPane.OK_CANCEL_OPTION);
            if (ret == JOptionPane.OK_OPTION) {
                LizzieFrame.saveSgf();
            }
        }

        try {
            config.persist();
        } catch (IOException err) {
            // Failed to save config
        }

        leelaz.shutdown();
        System.exit(0);
    }

}
