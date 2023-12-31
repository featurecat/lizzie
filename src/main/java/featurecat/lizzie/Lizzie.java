package featurecat.lizzie;

import featurecat.lizzie.analysis.EngineManager;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.GtpConsolePane;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.gui.LizzieMain;
import featurecat.lizzie.gui.MainFrame;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.util.Utils;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Main class. */
public class Lizzie {
  public static Config config;
  public static MainFrame frame;
  public static GtpConsolePane gtpConsole;
  public static Board board;
  public static Leelaz leelaz;
  public static String lizzieVersion = "0.7.4";
  private static String[] mainArgs;
  public static EngineManager engineManager;

  /** Launches the game window, and runs the game. */
  public static void main(String[] args) throws IOException {
    setLookAndFeel();
    mainArgs = args;
    config = new Config();
    SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            mainInEDT();
          }
        });
  }

  private static void mainInEDT() {
    frame = config.panelUI ? new LizzieMain() : new LizzieFrame();
    gtpConsole = new GtpConsolePane(frame);
    gtpConsole.setVisible(config.persistedUi.optBoolean("gtp-console-opened", false));
    initializeEngineManager();
  }

  public static void initializeEngineManager() {
    try {
      engineManager = new EngineManager(config);
    } catch (IOException e) {
      frame.openConfigDialog();
      Utils.showMessageDialog(frame, "Please restart Lizzie to apply changes.");
      System.exit(1);
    }

    if (mainArgs.length == 1) {
      frame.loadFile(new File(mainArgs[0]));
    } else if (config.config.getJSONObject("ui").getBoolean("resume-previous-game")) {
      board.resumePreviousGame();
    }
  }

  public static void initializeAfterVersionCheck(Leelaz lz) {
    lz.updateKataGoRule();
    if (config.handicapInsteadOfWinrate) {
      lz.estimatePassWinrate();
    }
    if (lz == leelaz) {
      leelaz.togglePonder();
    }
    Lizzie.engineManager.updateEngineIcon();
  }

  public static void setLookAndFeel() {
    try {
      if (System.getProperty("os.name").contains("Mac")) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      }
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
              frame, "Do you want to save this SGF?", "Save SGF?", JOptionPane.OK_CANCEL_OPTION);
      if (ret == JOptionPane.OK_OPTION) {
        frame.saveFile();
      }
    }
    board.autosaveToMemory();

    try {
      config.save();
      config.persist();
    } catch (IOException e) {
      e.printStackTrace(); // Failed to save config
    }

    if (leelaz != null) leelaz.shutdown();
    System.exit(0);
  }
}
