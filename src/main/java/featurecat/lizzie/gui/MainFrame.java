package featurecat.lizzie.gui;

import java.awt.HeadlessException;
import java.io.File;
import javax.swing.JFrame;

public abstract class MainFrame extends JFrame {

  public boolean isPlayingAgainstLeelaz = false;
  public boolean playerIsBlack = true;
  public boolean isNewGame = false;
  public int boardPositionProportion;
  public int winRateGridLines = 3;
  public boolean showControls = false;

  public MainFrame(String title) throws HeadlessException {
    super(title);
  }

  public boolean isDesignMode() {
    return false;
  }

  public void toggleDesignMode() {}

  public void updateBasicInfo() {}

  public void refresh() {
    repaint();
  };

  public abstract void drawControls();

  public abstract void replayBranch();

  public abstract void refreshBackground();

  public abstract void updateTitle();

  public abstract void setPlayers(String whitePlayer, String blackPlayer);

  public abstract void resetTitle();

  public abstract void clear();

  public abstract boolean isMouseOver(int x, int y);

  public abstract void onClicked(int x, int y);

  public abstract void startRawBoard();

  public abstract void stopRawBoard();

  public abstract boolean incrementDisplayedBranchLength(int n);

  public abstract void increaseMaxAlpha(int k);

  public abstract void loadFile(File file);

  public abstract void openFile();

  public abstract void saveFile();

  public abstract void copySgf();

  public abstract void pasteSgf();

  public abstract void openConfigDialog();

  public abstract void toggleGtpConsole();

  public abstract void openAvoidMoveDialog();

  public abstract void startNewGame();

  public abstract void editGameInfo();

  public abstract void openChangeMoveDialog();

  public abstract boolean playCurrentVariation();

  public abstract void playBestMove();
}
