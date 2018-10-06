package featurecat.lizzie;

import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.rules.Board;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import org.json.JSONException;

/** Main class. */
public class Lizzie {
  public static LizzieFrame frame;
  public static Leelaz leelaz;
  public static Board board;
  public static Config config;
  public static String lizzieVersion = "0.5";

  /** Launches the game window, and runs the game. */
  public static void main(String[] args)
      throws IOException, JSONException, ClassNotFoundException, UnsupportedLookAndFeelException,
          InstantiationException, IllegalAccessException, InterruptedException {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    config = new Config();
    board = new Board();
    frame = new LizzieFrame();

    new Thread(
            () -> {
              try {
                leelaz = new Leelaz();
                if (config.handicapInsteadOfWinrate) {
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
                System.exit(-1);
              }
            })
        .start();
  }

  public static void shutdown() {
    if (board != null && config.config.getJSONObject("ui").getBoolean("confirm-exit")) {
      int ret =
          JOptionPane.showConfirmDialog(
              null, "Do you want to save this SGF?", "Save SGF?", JOptionPane.OK_CANCEL_OPTION);
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

    if (leelaz != null) leelaz.shutdown();
    System.exit(0);
  }
}
