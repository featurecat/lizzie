package featurecat.lizzie;

import org.json.JSONArray;
import org.json.JSONException;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.plugin.PluginManager;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.gui.LizzieFrame;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

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

        // Check that user has installed leela zero
        JSONObject leelazconfig = Lizzie.config.config.getJSONObject("leelaz");
        ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");
        String startfolder = leelazconfig.optString("engine-start-location", ".");

        // Check if engine is present
        File lef = new File(startfolder + '/' + "leelaz");
        if (!lef.exists()) {
            File leexe = new File(startfolder + '/' + "leelaz.exe");
            if (!leexe.exists()) {
                JOptionPane.showMessageDialog(null, resourceBundle.getString("LizzieFrame.display.leelaz-missing"), "Lizzie - Error!", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        PluginManager.loadPlugins();

        board = new Board();

        frame = new LizzieFrame();

        new Thread( () -> {
            try {
                leelaz = new Leelaz();
                if(config.handicapInsteadOfWinrate) {
                    leelaz.estimatePassWinrate();
                }
                if (args.length == 1) {
                    frame.loadFile(new File(args[0]));
                } else if (config.config.getJSONObject("ui").getBoolean("resume-previous-game")) {
                    board.resumePreviousGame();
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
                LizzieFrame.saveFile();
            }
        }
        if (board != null) {
            board.autosaveToMemory();
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

    /**
     * Switch the Engine by index number
     * @param index engine index
     */
    public static void switchEngine(int index) {

        String commandLine = null;
        if (index == 0) {
            commandLine = Lizzie.config.leelazConfig.getString("engine-command");
            commandLine = commandLine.replaceAll("%network-file", Lizzie.config.leelazConfig.getString("network-file"));
        } else {
            JSONArray commandList = Lizzie.config.leelazConfig.getJSONArray("engine-command-list");
            if (commandList != null && commandList.length() >= index) {
                commandLine = commandList.getString(index - 1);
            } else {
                index = -1;
            }
        }
        if (index < 0 || commandLine == null || commandLine.trim().isEmpty() || index == Lizzie.leelaz.currentEngineN()) {
            return;
        }

        // Workaround for leelaz cannot exit when restarting
        if (leelaz.isThinking) {
            if (Lizzie.frame.isPlayingAgainstLeelaz) {
                Lizzie.frame.isPlayingAgainstLeelaz = false;
                Lizzie.leelaz.togglePonder(); // we must toggle twice for it to restart pondering
                Lizzie.leelaz.isThinking = false;
            }
            Lizzie.leelaz.togglePonder();
        }

        board.saveMoveNumber();
        try {
            leelaz.restartEngine(commandLine, index);
            board.restoreMoveNumber();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
