package featurecat.lizzie;

import featurecat.lizzie.analysis.EngineManager;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.GtpConsolePane;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.gui.LizzieMain;
import featurecat.lizzie.gui.MainFrame;
import featurecat.lizzie.rules.Board;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Main class. */
public class Lizzie {
  public static Config config;
  public static MainFrame frame;
  public static GtpConsolePane gtpConsole;
  public static Board board;
  public static Leelaz leelaz;
  public static String lizzieVersion = "0.7";
  private static String[] mainArgs;
  public static EngineManager engineManager;

  /** Launches the game window, and runs the game. */
  public static void main(String[] args) throws IOException {
    setLookAndFeel();
    mainArgs = args;
    config = new Config();
    frame = config.panelUI ? new LizzieMain() : new LizzieFrame();
    gtpConsole = new GtpConsolePane(frame);
    gtpConsole.setVisible(config.leelazConfig.optBoolean("print-comms", false));
    try {
      engineManager = new EngineManager(config);

      if (config.handicapInsteadOfWinrate) {
        leelaz.estimatePassWinrate();
      }
      if (mainArgs.length == 1) {
        frame.loadFile(new File(mainArgs[0]));
      } else if (config.config.getJSONObject("ui").getBoolean("resume-previous-game")) {
        board.resumePreviousGame();
      }
      leelaz.togglePonder();
    } catch (IOException e) {
      frame.openConfigDialog();
      System.exit(1);
    }
  }

  public static void setLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }
  }

  public static void shutdown() {
    if (config.config.getJSONObject("ui").getBoolean("confirm-exit")) {
      int ret =
          JOptionPane.showConfirmDialog(
              null, "Do you want to save this SGF?", "Save SGF?", JOptionPane.OK_CANCEL_OPTION);
      if (ret == JOptionPane.OK_OPTION) {
        frame.saveFile();
      }
    }
    board.autosaveToMemory();

    try {
      config.persist();
    } catch (IOException e) {
      e.printStackTrace(); // Failed to save config
    }

    if (leelaz != null) leelaz.shutdown();
    System.exit(0);
  }
}
