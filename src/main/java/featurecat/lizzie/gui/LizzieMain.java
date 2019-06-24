package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.rules.GIBParser;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.util.WindowPosition;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class LizzieMain extends MainFrame {
  public static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");

  public static Input input;
  public static BasicInfoPane basicInfoPane;
  private static final String DEFAULT_TITLE = resourceBundle.getString("LizzieFrame.title");

  public static BoardPane boardPane;
  public static SubBoardPane subBoardPane;
  public static WinratePane winratePane;
  public static VariationTreePane variationTreePane;
  public static CommentPane commentPane;
  public static boolean designMode;
  private LizzieLayout layout;

  private static final BufferedImage emptyImage = new BufferedImage(1, 1, TYPE_INT_ARGB);

  public BufferedImage cachedBackground;
  private BufferedImage cachedBasicInfoContainer = emptyImage;
  private BufferedImage cachedWinrateContainer = emptyImage;
  private BufferedImage cachedVariationContainer = emptyImage;

  private BufferedImage cachedWallpaperImage = emptyImage;
  private int cachedBackgroundWidth = 0, cachedBackgroundHeight = 0;
  private boolean redrawBackgroundAnyway = false;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;

  // Save the player title
  private String playerTitle = "";

  // Show the playouts in the title
  private ScheduledExecutorService showPlayouts = Executors.newScheduledThreadPool(1);
  private long lastPlayouts = 0;
  private String visitsString = "";
  public boolean isDrawVisitsInTitle = true;

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

  /** Creates a window */
  public LizzieMain() {
    super(DEFAULT_TITLE);

    // TODO
    //    setMinimumSize(new Dimension(640, 400));
    boolean persisted = Lizzie.config.persistedUi != null;
    if (persisted)
      boardPositionProportion =
          Lizzie.config.persistedUi.optInt("board-position-proportion", boardPositionProportion);
    JSONArray pos = WindowPosition.mainWindowPos();
    if (pos != null) {
      this.setBounds(pos.getInt(0), pos.getInt(1), pos.getInt(2), pos.getInt(3));
    } else {
      setSize(960, 600);
      setLocationRelativeTo(null); // Start centered, needs to be called *after* setSize...
    }

    // Allow change font in the config
    if (Lizzie.config.uiFontName != null) {
      uiFont = new Font(Lizzie.config.uiFontName, Font.PLAIN, 12);
    }
    if (Lizzie.config.winrateFontName != null) {
      winrateFont = new Font(Lizzie.config.winrateFontName, Font.BOLD, 12);
    }

    if (Lizzie.config.startMaximized && !persisted) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    } else if (persisted && Lizzie.config.persistedUi.getBoolean("window-maximized")) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    JPanel panel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            if (g instanceof Graphics2D) {
              int width = getWidth();
              int height = getHeight();
              Optional<Graphics2D> backgroundG;
              if (cachedBackgroundWidth != width
                  || cachedBackgroundHeight != height
                  || redrawBackgroundAnyway) {
                backgroundG = Optional.of(createBackground(width, height));
              } else {
                backgroundG = Optional.empty();
              }
              // draw the image
              Graphics2D bsGraphics = (Graphics2D) g; // bs.getDrawGraphics();
              bsGraphics.setRenderingHint(
                  RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
              bsGraphics.drawImage(cachedBackground, 0, 0, null);
            }
          }
        };
    setContentPane(panel);

    layout = new LizzieLayout();
    getContentPane().setLayout(layout);
    basicInfoPane = new BasicInfoPane(this);
    boardPane = new BoardPane(this);
    subBoardPane = new SubBoardPane(this);
    winratePane = new WinratePane(this);
    variationTreePane = new VariationTreePane(this);
    commentPane = new CommentPane(this);
    getContentPane().add(boardPane, LizzieLayout.MAIN_BOARD);
    getContentPane().add(basicInfoPane, LizzieLayout.BASIC_INFO);
    getContentPane().add(winratePane, LizzieLayout.WINRATE);
    getContentPane().add(subBoardPane, LizzieLayout.SUB_BOARD);
    getContentPane().add(variationTreePane, LizzieLayout.VARIATION);
    getContentPane().add(commentPane, LizzieLayout.COMMENT);
    WindowPosition.restorePane(Lizzie.config.persistedUi, boardPane);
    WindowPosition.restorePane(Lizzie.config.persistedUi, basicInfoPane);
    WindowPosition.restorePane(Lizzie.config.persistedUi, winratePane);
    WindowPosition.restorePane(Lizzie.config.persistedUi, subBoardPane);
    WindowPosition.restorePane(Lizzie.config.persistedUi, variationTreePane);
    WindowPosition.restorePane(Lizzie.config.persistedUi, commentPane);

    try {
      this.setIconImage(ImageIO.read(getClass().getResourceAsStream("/assets/logo.png")));
    } catch (IOException e) {
      e.printStackTrace();
    }

    setVisible(true);

    input = new Input();
    //  addMouseListener(input);
    addKeyListener(input);
    addMouseWheelListener(input);
    //    addMouseMotionListener(input);

    // When the window is closed: save the SGF file, then run shutdown()
    this.addWindowListener(
        new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            Lizzie.shutdown();
          }
        });

    // Show the playouts in the title
    showPlayouts.scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            if (!isDrawVisitsInTitle) {
              visitsString = "";
              return;
            }
            if (Lizzie.leelaz == null) return;
            try {
              int totalPlayouts = MoveData.getPlayouts(Lizzie.leelaz.getBestMoves());
              if (totalPlayouts <= 0) return;
              visitsString =
                  String.format(
                      " %d visits/second",
                      (totalPlayouts > lastPlayouts) ? totalPlayouts - lastPlayouts : 0);
              updateTitle();
              lastPlayouts = totalPlayouts;
            } catch (Exception e) {
            }
          }
        },
        1,
        1,
        TimeUnit.SECONDS);

    setFocusable(true);
    setFocusTraversalKeysEnabled(false);
  }

  /**
   * temporary measure to refresh background. ideally we shouldn't need this (but we want to release
   * Lizzie 0.5 today, not tomorrow!). Refactor me out please! (you need to get blurring to work
   * properly on startup).
   */
  public void refreshBackground() {
    redrawBackgroundAnyway = true;
  }

  public BufferedImage getWallpaper() {
    if (cachedWallpaperImage == emptyImage) {
      cachedWallpaperImage = Lizzie.config.theme.background();
    }
    return cachedWallpaperImage;
  }

  private Graphics2D createBackground(int width, int height) {
    cachedBackground = new BufferedImage(width, height, TYPE_INT_RGB);
    cachedBackgroundWidth = cachedBackground.getWidth();
    cachedBackgroundHeight = cachedBackground.getHeight();

    redrawBackgroundAnyway = false;

    Graphics2D g = cachedBackground.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    BufferedImage wallpaper = getWallpaper();
    int drawWidth = max(wallpaper.getWidth(), width);
    int drawHeight = max(wallpaper.getHeight(), height);
    // Support seamless texture
    drawTextureImage(g, wallpaper, 0, 0, drawWidth, drawHeight);

    return g;
  }

  private GaussianFilter filter20 = new GaussianFilter(20);

  public BufferedImage getBasicInfoContainer(LizziePane pane) {
    if (cachedBackground == null
        || (cachedBasicInfoContainer != null
            && cachedBasicInfoContainer.getWidth() == pane.getWidth()
            && cachedBasicInfoContainer.getHeight() == pane.getHeight())) {
      return cachedBasicInfoContainer;
    }
    int vx = pane.getX();
    int vy = pane.getY();
    int vw = pane.getWidth();
    int vh = pane.getHeight();
    BufferedImage result = cachedBackground.getSubimage(vx, vy, vw, vh);
    //    BufferedImage result = new BufferedImage(vw, vh, TYPE_INT_ARGB);
    //    filter10.filter(cachedBackground.getSubimage(vx, vy, vw, vh), result);
    cachedBasicInfoContainer = result;
    return result;
  }

  public BufferedImage getWinrateContainer(LizziePane pane) {
    if (cachedBackground == null
        || (cachedWinrateContainer != null
            && cachedWinrateContainer.getWidth() == pane.getWidth()
            && cachedWinrateContainer.getHeight() == pane.getHeight())) {
      return cachedWinrateContainer;
    }
    int vx = pane.getX();
    int vy = pane.getY();
    int vw = pane.getWidth();
    int vh = pane.getHeight();
    BufferedImage result = new BufferedImage(vw, vh, TYPE_INT_ARGB);
    filter20.filter(cachedBackground.getSubimage(vx, vy, vw, vh), result);
    cachedWinrateContainer = result;
    return result;
  }

  public BufferedImage getVariationContainer(LizziePane pane) {
    if (cachedBackground == null
        || (cachedVariationContainer != null
            && cachedVariationContainer.getWidth() == pane.getWidth()
            && cachedVariationContainer.getHeight() == pane.getHeight())) {
      return cachedVariationContainer;
    }
    int vx = pane.getX();
    int vy = pane.getY();
    int vw = pane.getWidth();
    int vh = pane.getHeight();
    BufferedImage result = new BufferedImage(vw, vh, TYPE_INT_ARGB);
    filter20.filter(cachedBackground.getSubimage(vx, vy, vw, vh), result);
    cachedVariationContainer = result;
    return result;
  }

  public void drawContainer(Graphics g, int vx, int vy, int vw, int vh) {
    if (vw <= 0
        || vh <= 0
        || vx < cachedBackground.getMinX()
        || vx + vw > cachedBackground.getMinX() + cachedBackground.getWidth()
        || vy < cachedBackground.getMinY()
        || vy + vh > cachedBackground.getMinY() + cachedBackground.getHeight()) {
      return;
    }
    redrawBackgroundAnyway = false;
    BufferedImage result = new BufferedImage(vw, vh, TYPE_INT_ARGB);
    filter20.filter(cachedBackground.getSubimage(vx, vy, vw, vh), result);
    g.drawImage(result, vx, vy, null);
  }

  /** Draw texture image */
  public void drawTextureImage(
      Graphics2D g, BufferedImage img, int x, int y, int width, int height) {
    TexturePaint paint =
        new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
    g.setPaint(paint);
    g.fill(new Rectangle(x, y, width, height));
  }

  @Override
  public boolean isDesignMode() {
    return designMode;
  }

  @Override
  public void toggleDesignMode() {
    this.designMode = !this.designMode;
    //    boardPane.setDesignMode(designMode);
    basicInfoPane.setDesignMode(designMode);
    winratePane.setDesignMode(designMode);
    subBoardPane.setDesignMode(designMode);
    variationTreePane.setDesignMode(designMode);
    commentPane.setDesignMode(designMode);
  }

  @Override
  public void updateBasicInfo(String bTime, String wTime) {
    if (basicInfoPane != null) {
      basicInfoPane.bTime = bTime;
      basicInfoPane.wTime = wTime;
      basicInfoPane.repaint();
    }
  }

  @Override
  public void updateBasicInfo() {
    if (basicInfoPane != null) {
      basicInfoPane.repaint();
    }
  }

  public void invalidLayout() {
    // TODO
    layout.layoutContainer(getContentPane());
    layout.invalidateLayout(getContentPane());
    repaint();
  }

  @Override
  public void refresh() {
    refresh(0);
  }

  @Override
  public void refresh(int type) {
    if (type == 2) {
      invalidLayout();
    } else {
      boardPane.repaint();
      if (type != 1) {
        updateStatus();
      }
    }
  }

  public void repaintSub() {
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showSubBoard && !subBoardPane.isVisible()) {
        subBoardPane.setVisible(true);
      }
      if (Lizzie.config.showWinrate && !winratePane.isVisible()) {
        winratePane.setVisible(true);
      }
    }
    subBoardPane.repaint();
    winratePane.repaint();
  }

  public void updateStatus() {
    //    basicInfoPane.revalidate();
    basicInfoPane.repaint();
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showVariationGraph && !variationTreePane.isVisible()) {
        variationTreePane.setVisible(true);
      }
    }
    //    variationTreePane.revalidate();
    variationTreePane.repaint();
    commentPane.drawComment();
    //    commentPane.revalidate();
    commentPane.repaint();
    invalidLayout();
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

  public void openAvoidMoveDialog() {
    AvoidMoveDialog avoidMoveDialog = new AvoidMoveDialog();
    avoidMoveDialog.setVisible(true);
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

  @Override
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
    Lizzie.leelaz.sendCommand("komi " + gameInfo.getKomi());

    Lizzie.leelaz.time_settings();
    Lizzie.frame.playerIsBlack = playerIsBlack;
    Lizzie.frame.isNewGame = isNewGame;
    Lizzie.frame.isPlayingAgainstLeelaz = true;

    boolean isHandicapGame = gameInfo.getHandicap() != 0;
    if (isNewGame) {
      Lizzie.board.getHistory().setGameInfo(gameInfo);
      if (isHandicapGame) {
        Lizzie.board.getHistory().getData().blackToPlay = false;
        Lizzie.leelaz.sendCommand("fixed_handicap " + gameInfo.getHandicap());
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

  public void openFile() {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf or *.gib", "SGF", "GIB");
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-folder"));

    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showOpenDialog(null);
    if (result == JFileChooser.APPROVE_OPTION) loadFile(chooser.getSelectedFile());
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

  public void setPlayers(String whitePlayer, String blackPlayer) {
    playerTitle = String.format("(%s [W] vs %s [B])", whitePlayer, blackPlayer);
    updateTitle();
  }

  public void updateTitle() {
    StringBuilder sb = new StringBuilder(DEFAULT_TITLE);
    sb.append(playerTitle);
    sb.append(" [" + Lizzie.leelaz.engineCommand() + "]");
    sb.append(visitsString);
    setTitle(sb.toString());
  }

  public void resetTitle() {
    playerTitle = "";
    updateTitle();
  }

  @Override
  public void drawControls() {
    boardPane.drawControls();
  }

  @Override
  public void replayBranch() {
    boardPane.replayBranch();
  }

  @Override
  public boolean isMouseOver(int x, int y) {
    return boardPane.isMouseOver(x, y);
  }

  @Override
  public void onClicked(int x, int y) {
    boardPane.onClicked(x, y);
  }

  @Override
  public void onDoubleClicked(int x, int y) {
    boardPane.onDoubleClicked(x, y);
  }

  @Override
  public void onMouseDragged(int x, int y) {
    winratePane.onMouseDragged(x, y);
  }

  @Override
  public void onMouseMoved(int x, int y) {
    boardPane.onMouseMoved(x, y);
  }

  @Override
  public void startRawBoard() {
    boardPane.startRawBoard();
  }

  @Override
  public void stopRawBoard() {
    boardPane.stopRawBoard();
  }

  @Override
  public boolean incrementDisplayedBranchLength(int n) {
    return boardPane.incrementDisplayedBranchLength(n);
  }

  @Override
  public void increaseMaxAlpha(int k) {
    boardPane.increaseMaxAlpha(k);
  }

  @Override
  public void copySgf() {
    boardPane.copySgf();
  }

  @Override
  public void pasteSgf() {
    boardPane.pasteSgf();
  }

  @Override
  public boolean playCurrentVariation() {
    return boardPane.playCurrentVariation();
  }

  @Override
  public void playBestMove() {
    boardPane.playBestMove();
  }

  @Override
  public void clear() {
    boardPane.clear();
  }
}
