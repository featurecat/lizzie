package featurecat.lizzie;

import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.rules.Board;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javax.swing.*;
import org.json.JSONArray;

/** Main class. */
public class Lizzie {
  public static Config config;
  public static LizzieFrame frame;
  public static Board board;
  public static Leelaz leelaz;
  public static String lizzieVersion = "0.6";
  private static String[] mainArgs;

  /** Launches the game window, and runs the game. */
  public static void main(String[] args) throws IOException {
    setLookAndFeel();
    mainArgs = args;
    config = new Config();
    board = new Board();
    frame = new LizzieFrame();
    leelaz = new Leelaz();

    if (config.handicapInsteadOfWinrate) {
      leelaz.estimatePassWinrate();
    }
    if (mainArgs.length == 1) {
      frame.loadFile(new File(mainArgs[0]));
    } else if (config.config.getJSONObject("ui").getBoolean("resume-previous-game")) {
      board.resumePreviousGame();
    }
    leelaz.togglePonder();
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
        LizzieFrame.saveFile();
      }
    }
    board.autosaveToMemory();

    try {
      config.persist();
    } catch (IOException e) {
      e.printStackTrace(); // Failed to save config
    }

    leelaz.shutdown();
    System.exit(0);
  }

  /**
   * Switch the Engine by index number
   *
   * @param index engine index
   */
  public static void switchEngine(int index) {
    String commandLine;
    if (index == 0) {
      String networkFile = Lizzie.config.leelazConfig.getString("network-file");
      commandLine = Lizzie.config.leelazConfig.getString("engine-command");
      commandLine = commandLine.replaceAll("%network-file", networkFile);
    } else {
      Optional<JSONArray> enginesOpt =
          Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-command-list"));
      if (enginesOpt.map(e -> e.length() < index).orElse(true)) {
        return;
      }
      commandLine = enginesOpt.get().getString(index - 1);
    }
    if (commandLine.trim().isEmpty() || index == Lizzie.leelaz.currentEngineN()) {
      return;
    }

    // Workaround for leelaz no exiting when restarting
    if (leelaz.isThinking) {
      if (Lizzie.frame.isPlayingAgainstLeelaz) {
        Lizzie.frame.isPlayingAgainstLeelaz = false;
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
