package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;

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
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class LizzieMain extends JFrame {
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

  private int originX;
  private int originY;
  private int originW;
  private int originH;

  public static Font uiFont;
  public static Font winrateFont;

  private final BufferStrategy bs;

  private static final BufferedImage emptyImage = new BufferedImage(1, 1, TYPE_INT_ARGB);
  private BufferedImage cachedImage;

  private BufferedImage cachedBackground;

  private BufferedImage cachedWallpaperImage = emptyImage;
  private int cachedBackgroundWidth = 0, cachedBackgroundHeight = 0;
  private boolean cachedBackgroundShowControls = false;
  private boolean cachedShowWinrate = true;
  private boolean cachedShowVariationGraph = true;
  private boolean cachedShowLargeSubBoard = true;
  private boolean cachedLargeWinrate = true;
  private boolean cachedShowComment = true;
  private boolean redrawBackgroundAnyway = false;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;
  public boolean showControls = false;
  public boolean isPlayingAgainstLeelaz = false;
  public boolean playerIsBlack = true;
  public boolean isNewGame = false;
  public int winRateGridLines = 3;
  public int BoardPositionProportion = Lizzie.config.boardPositionProportion;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;

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
      BoardPositionProportion =
          Lizzie.config.persistedUi.optInt("board-postion-propotion", BoardPositionProportion);
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
                backgroundG = Optional.of(createBackground());
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
    // TODO
    Lizzie.frame = boardPane;
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
    setVisible(true);

    createBufferStrategy(2);
    bs = getBufferStrategy();

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
   * Draws the game board and interface
   *
   * @param g0 not used
   */
  public void paint(Graphics g0) {
    super.paintComponents(g0);
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

  private Graphics2D createBackground() {
    cachedBackground = new BufferedImage(getWidth(), getHeight(), TYPE_INT_RGB);
    cachedBackgroundWidth = cachedBackground.getWidth();
    cachedBackgroundHeight = cachedBackground.getHeight();
    //    cachedBackgroundShowControls = showControls;
    cachedShowWinrate = Lizzie.config.showWinrate;
    cachedShowVariationGraph = Lizzie.config.showVariationGraph;
    cachedShowLargeSubBoard = Lizzie.config.showLargeSubBoard();
    cachedLargeWinrate = Lizzie.config.showLargeWinrate();
    cachedShowComment = Lizzie.config.showComment;

    redrawBackgroundAnyway = false;

    Graphics2D g = cachedBackground.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    BufferedImage wallpaper = getWallpaper();
    int drawWidth = max(wallpaper.getWidth(), getWidth());
    int drawHeight = max(wallpaper.getHeight(), getHeight());
    // Support seamless texture
    drawTextureImage(g, wallpaper, 0, 0, drawWidth, drawHeight);

    return g;
  }

  /** Draw texture image */
  public void drawTextureImage(
      Graphics2D g, BufferedImage img, int x, int y, int width, int height) {
    TexturePaint paint =
        new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
    g.setPaint(paint);
    g.fill(new Rectangle(x, y, width, height));
  }

  public void toggleDesignMode() {
    this.designMode = !this.designMode;
    //    boardPane.setDesignMode(designMode);
    basicInfoPane.setDesignMode(designMode);
    winratePane.setDesignMode(designMode);
    subBoardPane.setDesignMode(designMode);
    variationTreePane.setDesignMode(designMode);
    commentPane.setDesignMode(designMode);
  }

  public void invalidLayout() {
    // TODO
    layout.layoutContainer(getContentPane());
    layout.invalidateLayout(getContentPane());
    repaint();
  }

  public void refresh(boolean all) {
    boardPane.repaint();
    if (all) {
      updateStatus();
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
    basicInfoPane.revalidate();
    basicInfoPane.repaint();
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showVariationGraph && !variationTreePane.isVisible()) {
        variationTreePane.setVisible(true);
      }
    }
    variationTreePane.revalidate();
    variationTreePane.repaint();
    commentPane.drawComment();
    commentPane.revalidate();
    commentPane.repaint();
    invalidLayout();
  }

  public static void openConfigDialog() {
    ConfigDialog configDialog = new ConfigDialog();
    configDialog.setVisible(true);
    //    configDialog.dispose();
  }

  public static void openChangeMoveDialog() {
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

  public static void startNewGame() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    NewGameDialog newGameDialog = new NewGameDialog();
    newGameDialog.setGameInfo(gameInfo);
    newGameDialog.setVisible(true);
    boolean playerIsBlack = newGameDialog.playerIsBlack();
    newGameDialog.dispose();
    if (newGameDialog.isCancelled()) return;

    Lizzie.board.clear();
    Lizzie.board.getHistory().setGameInfo(gameInfo);
    Lizzie.leelaz.sendCommand("komi " + gameInfo.getKomi());

    Lizzie.leelaz.time_settings();
    Lizzie.frame.playerIsBlack = playerIsBlack;
    Lizzie.frame.isPlayingAgainstLeelaz = true;

    boolean isHandicapGame = gameInfo.getHandicap() != 0;
    if (isHandicapGame) {
      Lizzie.board.getHistory().getData().blackToPlay = false;
      Lizzie.leelaz.sendCommand("fixed_handicap " + gameInfo.getHandicap());
      if (playerIsBlack) Lizzie.leelaz.genmove("W");
    } else if (!playerIsBlack) {
      Lizzie.leelaz.genmove("B");
    }
  }

  public static void editGameInfo() {
    GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

    GameInfoDialog gameInfoDialog = new GameInfoDialog();
    gameInfoDialog.setGameInfo(gameInfo);
    gameInfoDialog.setVisible(true);

    gameInfoDialog.dispose();
  }

  public static void saveFile() {
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

  public static void openFile() {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf or *.gib", "SGF", "GIB");
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-folder"));

    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showOpenDialog(null);
    if (result == JFileChooser.APPROVE_OPTION) loadFile(chooser.getSelectedFile());
  }

  public static void loadFile(File file) {
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
}
