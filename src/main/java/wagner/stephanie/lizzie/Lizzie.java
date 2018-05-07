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
                if( config.config.getJSONObject("ui").getBoolean("handicap-instead-of-winrate") ) {
                	estimatePassWinrate();
                }
                leelaz.togglePonder();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        
    }

    public static void estimatePassWinrate() {
    	leelaz.playMove(Stone.BLACK, "A1"); // we use A1 instead of pass, because networks have some experience with this due to early randomness but none with pass. hence the network has reasonably converged and don't need lots of playouts for reasonable estimate.
    	leelaz.togglePonder();
    	WinrateStats stats=leelaz.getWinrateStats();
    	while( stats.totalPlayouts < 1 ) {
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new Error(e);
			}
    		stats=leelaz.getWinrateStats();
    	}
    	mHandicapWinrate=stats.maxWinrate;
    	System.out.println("handicap winrate set to " + stats.maxWinrate + " after " + stats.totalPlayouts + " playouts." );
    	leelaz.togglePonder();
    	leelaz.undo();
    	board.clear();
    }
    
    public static double mHandicapWinrate=25;
    
    /**
     * Convert winrate to handicap stones, by normalizing winrate by first move pass winrate (one stone handicap).
     */
    public static double winrateToHandicap(double pWinrate) {
        return Math.signum(0.5-(pWinrate/100))*Math.log(1-Math.abs(1-(pWinrate/100)*2))/-Math.log(1-Math.abs(1-(mHandicapWinrate/100)*2));
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
