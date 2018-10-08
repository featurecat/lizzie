package featurecat.lizzie;

import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.rules.Board;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import org.json.JSONArray;

/** Main class. */
public class Lizzie {
  public static LizzieFrame frame;
  public static Leelaz leelaz;
  public static Board board;
  public static Config config;
  public static String lizzieVersion = "0.5";
  private static String[] mainArgs;

  /** Launches the game window, and runs the game. */
  public static void main(String[] args) throws IOException {
    setLookAndFeel();
    mainArgs = args;
    config = new Config();
    board = new Board();
    frame = new LizzieFrame();
    new Thread(Lizzie::run).start();
  }

  public static void run() {
    try {
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
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
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
    } catch (IOException e) {
      e.printStackTrace(); // Failed to save config
    }

    if (leelaz != null) leelaz.shutdown();
    System.exit(0);
  }

  /**
   * Switch the Engine by index number
   *
   * @param index engine index
   */
  public static void switchEngine(int index) {

    String commandLine = null;
    if (index == 0) {
      commandLine = Lizzie.config.leelazConfig.getString("engine-command");
      commandLine =
          commandLine.replaceAll(
              "%network-file", Lizzie.config.leelazConfig.getString("network-file"));
    } else {
      JSONArray commandList = Lizzie.config.leelazConfig.getJSONArray("engine-command-list");
      if (commandList != null && commandList.length() >= index) {
        commandLine = commandList.getString(index - 1);
      } else {
        index = -1;
      }
    }
    if (index < 0
        || commandLine == null
        || commandLine.trim().isEmpty()
        || index == Lizzie.leelaz.currentEngineN()) {
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
