package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.analysis.YaZenGtp;
import featurecat.lizzie.rules.GIBParser;
import featurecat.lizzie.rules.SGFParser;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.HeadlessException;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONObject;

public abstract class MainFrame extends JFrame {
  public static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");

  private static final String DEFAULT_TITLE = resourceBundle.getString("LizzieFrame.title");
  public boolean isPlayingAgainstLeelaz = false;
  public boolean playerIsBlack = true;
  public boolean isNewGame = false;
  public int boardPositionProportion = Lizzie.config.boardPositionProportion;
  public int winRateGridLines = 3;
  public boolean showControls = false;
  public static Font uiFont;
  public static Font winrateFont;
  public YaZenGtp zen;
  public static CountResults countResults;
  public boolean isEstimating = false;
  public boolean isFirstCount = true;
  public boolean isAutoEstimating = false;

  static {
    // load fonts
    try {
      uiFont = new Font("SansSerif", Font.TRUETYPE_FONT, 12);
      //          Font.createFont(
      //              Font.TRUETYPE_FONT,
      //              Thread.currentThread()
      //                  .getContextClassLoader()
      //                  .getResourceAsStream("fonts/OpenSans-Regular.ttf"));
      winrateFont =
          Font.createFont(
              Font.TRUETYPE_FONT,
              Thread.currentThread()
                  .getContextClassLoader()
                  .getResourceAsStream("fonts/OpenSans-Semibold.ttf"));
    } catch (IOException | FontFormatException e) {
      e.printStackTrace();
    }
  }

  // Save the player title
  private String playerTitle = "";
  protected String visitsString = "";
  // Force refresh board
  private boolean forceRefresh;
  public boolean isMouseOver = false;
  public OnlineDialog onlineDialog = null;

  public MainFrame() throws HeadlessException {
    super(DEFAULT_TITLE);
  }

  public boolean isDesignMode() {
    return false;
  }

  public void toggleDesignMode() {}

  public void updateBasicInfo() {}

  public void updateBasicInfo(String bTime, String wTime) {}

  public void refresh() {
    repaint();
  }

  /**
   * Refresh
   *
   * @param type: 0-All, 1-Only Board, 2-Invalid Layout
   */
  public void refresh(int type) {
    repaint();
  }

  public boolean isForceRefresh() {
    return forceRefresh;
  }

  public void setForceRefresh(boolean forceRefresh) {
    this.forceRefresh = forceRefresh;
  }

  public boolean processCommentMouseWheelMoved(MouseWheelEvent e) {
    return false;
  }

  public abstract void removeEstimateRect();

  public abstract void drawEstimateRectKata(ArrayList<Double> esitmateArray);

  public abstract void drawControls();

  public abstract void replayBranch(boolean generateGif);

  public abstract void refreshBackground();

  public abstract void clear();

  public abstract boolean isMouseOver(int x, int y);

  public abstract void onClicked(int x, int y);

  public abstract void onDoubleClicked(int x, int y);

  public abstract void onMouseDragged(int x, int y);

  public abstract void onMouseMoved(int x, int y);

  public abstract void startRawBoard();

  public abstract void stopRawBoard();

  public abstract boolean incrementDisplayedBranchLength(int n);

  public void doBranch(int moveTo) {}

  public void addSuggestionAsBranch() {}

  public abstract void increaseMaxAlpha(int k);

  public abstract void copySgf();

  public abstract void pasteSgf();

  public void setPlayers(String whitePlayer, String blackPlayer) {
    playerTitle = String.format("(%s [W] vs %s [B])", whitePlayer, blackPlayer);
    updateTitle();
  }

  public void updateTitle() {
    StringBuilder sb = new StringBuilder(DEFAULT_TITLE);
    if (Lizzie.watcher != null) {
      File file = Lizzie.watcher.getFile();
      if (file != null) {
        sb.append(" [watching: " + file.getPath() + "]");
      }
    }
    setTitle(sb.toString());
  }

  public void resetTitle() {
    playerTitle = "";
    updateTitle();
  }

  public void openConfigDialog() {
    ConfigDialog configDialog = new ConfigDialog();
    configDialog.setVisible(true);
    //    configDialog.dispose();
  }

  public void openChangeMoveDialog() {
    ChangeMoveDialog changeMoveDialog = new ChangeMoveDialog();
    changeMoveDialog.setVisible(true);
  }

  public void toggleGtpConsole() {
    Lizzie.leelaz.toggleGtpConsole();
    if (Lizzie.gtpConsole != null) {
      Lizzie.gtpConsole.setVisible(!Lizzie.gtpConsole.isVisible());
    } else {
      Lizzie.gtpConsole = new GtpConsolePane(this);
      Lizzie.gtpConsole.setVisible(true);
    }
  }

  public void openOnlineDialog() {
    if (onlineDialog == null) {
      onlineDialog = new OnlineDialog();
    }
    onlineDialog.setVisible(true);
  }

  public void startGame() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    NewGameDialog gameDialog = new NewGameDialog();
    gameDialog.setGameInfo(gameInfo);
    gameDialog.setVisible(true);
    boolean playerIsBlack = gameDialog.playerIsBlack();
    boolean isNewGame = gameDialog.isNewGame();
    //    gameDialog.dispose();
    if (gameDialog.isCancelled()) return;

    if (isNewGame) {
      Lizzie.board.clear();
    }
    Lizzie.leelaz.komi(gameInfo.getKomi());

    Lizzie.leelaz.time_settings();
    Lizzie.frame.playerIsBlack = playerIsBlack;
    Lizzie.frame.isNewGame = isNewGame;
    Lizzie.frame.isPlayingAgainstLeelaz = true;

    boolean isHandicapGame = gameInfo.getHandicap() != 0;
    if (isNewGame) {
      Lizzie.board.getHistory().setGameInfo(gameInfo);
      if (isHandicapGame) {
        Lizzie.board.getHistory().getData().blackToPlay = false;
        Lizzie.leelaz.handicap(gameInfo.getHandicap());
        if (playerIsBlack) Lizzie.leelaz.genmove("W");
      } else if (!playerIsBlack) {
        Lizzie.leelaz.genmove("B");
      }
    } else {
      Lizzie.board.getHistory().setGameInfo(gameInfo);
      if (Lizzie.frame.playerIsBlack != Lizzie.board.getData().blackToPlay) {
        if (!Lizzie.leelaz.isThinking) {
          Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "B" : "W"));
        }
      }
    }
  }

  public void editGameInfo() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    GameInfoDialog gameInfoDialog = new GameInfoDialog();
    gameInfoDialog.setGameInfo(gameInfo);
    gameInfoDialog.setVisible(true);

    gameInfoDialog.dispose();
  }

  public void saveFile() {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf", "SGF");
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-folder"));
    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showSaveDialog(null);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (file.exists()) {
        int ret =
            JOptionPane.showConfirmDialog(
                null,
                resourceBundle.getString("LizzieFrame.prompt.sgfExists"),
                "Warning",
                JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      if (!file.getPath().endsWith(".sgf")) {
        file = new File(file.getPath() + ".sgf");
      }
      try {
        SGFParser.save(Lizzie.board, file.getPath());
        filesystem.put("last-folder", file.getParent());
      } catch (IOException err) {
        JOptionPane.showConfirmDialog(
            null,
            resourceBundle.getString("LizzieFrame.prompt.failedTosaveFile"),
            "Error",
            JOptionPane.ERROR);
      }
    }
  }

  public File chooseFile() {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf or *.gib", "SGF", "GIB");
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-folder"));

    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showOpenDialog(null);
    if (result == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile();
    }

    return null;
  }

  public void openFile() {
    File file = chooseFile();
    if (file != null) loadFile(file);
  }

  public void loadFile(File file) {
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    if (!(file.getPath().endsWith(".sgf") || file.getPath().endsWith(".gib"))) {
      file = new File(file.getPath() + ".sgf");
    }
    try {
      System.out.println(file.getPath());
      if (file.getPath().endsWith(".sgf")) {
        SGFParser.load(file.getPath());
      } else {
        GIBParser.load(file.getPath());
      }
      filesystem.put("last-folder", file.getParent());
    } catch (IOException err) {
      JOptionPane.showConfirmDialog(
          null,
          resourceBundle.getString("LizzieFrame.prompt.failedToOpenFile"),
          "Error",
          JOptionPane.ERROR);
    }
  }

  public abstract boolean playCurrentVariation();

  public abstract void playBestMove();

  public abstract void estimateByZen();

  public abstract void noAutoEstimateByZen();

  public abstract void noEstimateByZen();

  public abstract void drawEstimateRectZen(ArrayList<Integer> esitmateArray);

  public void saveImage() {};
}
