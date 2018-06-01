package featurecat.lizzie;

import org.json.JSONException;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.plugin.PluginManager;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.gui.LizzieFrame;

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
    public static String lizzieVersion = "0.5";

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException, JSONException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, InterruptedException {
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
        if (board != null && config.config.getJSONObject("ui").getBoolean("confirm-exit")) {
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

        if (leelaz != null)
            leelaz.shutdown();
        System.exit(0);
    }

}
