package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.analysis.YaZenGtp;
import featurecat.lizzie.util.Utils;
import featurecat.lizzie.util.WindowPosition;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.json.JSONArray;

public class LizzieMain extends MainFrame {

  public static Input input;
  public static BasicInfoPane basicInfoPane;
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

  // Show the playouts in the title
  private ScheduledExecutorService showPlayouts = Executors.newScheduledThreadPool(1);
  private long lastPlayouts = 0;
  public boolean isDrawVisitsInTitle = true;
  RightClickMenu rightClickMenu;

  /** Creates a window */
  public LizzieMain() {
    super();

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
              bsGraphics.setRenderingHint(
                  RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              bsGraphics.drawImage(cachedBackground, 0, 0, null);

              // pondering message
              int maxBound = Math.max(width, height);
              double ponderingSize = .02;
              int ponderingX = 0;
              int ponderingY = height - 2 - (int) (maxBound * ponderingSize);

              // dynamic komi
              double dynamicKomiSize = .02;
              int dynamicKomiX = 0;
              int dynamicKomiY = ponderingY - (int) (maxBound * dynamicKomiSize);
              int dynamicKomiLabelX = 0;
              int dynamicKomiLabelY = dynamicKomiY - (int) (maxBound * dynamicKomiSize);

              // loading message;
              double loadingSize = 0.03;
              int loadingX = ponderingX;
              int loadingY = ponderingY - (int) (maxBound * (loadingSize - ponderingSize));

              if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
                if (Lizzie.config.showStatus) {
                  String statusKey =
                      "LizzieFrame.display." + (Lizzie.leelaz.isPondering() ? "on" : "off");
                  String statusText = resourceBundle.getString(statusKey);
                  String ponderingText = resourceBundle.getString("LizzieFrame.display.pondering");
                  String switching = resourceBundle.getString("LizzieFrame.prompt.switching");
                  String switchingText = Lizzie.leelaz.switching() ? switching : "";
                  String weightText = Lizzie.leelaz.currentWeight();
                  String text =
                      ponderingText + " " + statusText + " " + weightText + " " + switchingText;
                  drawPonderingState(bsGraphics, text, ponderingX, ponderingY, ponderingSize);
                }

                Optional<String> dynamicKomi = Lizzie.leelaz.getDynamicKomi();
                if (Lizzie.config.showDynamicKomi && dynamicKomi.isPresent()) {
                  String text = resourceBundle.getString("LizzieFrame.display.dynamic-komi");
                  drawPonderingState(
                      bsGraphics, text, dynamicKomiLabelX, dynamicKomiLabelY, dynamicKomiSize);
                  drawPonderingState(
                      bsGraphics, dynamicKomi.get(), dynamicKomiX, dynamicKomiY, dynamicKomiSize);
                }
              } else if (Lizzie.config.showStatus) {
                String loadingText = resourceBundle.getString("LizzieFrame.display.loading");
                drawPonderingState(bsGraphics, loadingText, loadingX, loadingY, loadingSize);
              }
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
    countResults = new CountResults();
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
      if (System.getProperty("os.name").contains("Mac")
          && Utils.classExists("com.apple.eawt.Application")) {
        try {
          Class c = Class.forName("com.apple.eawt.Application");
          Method m = c.getDeclaredMethod("getApplication");
          Object o = m.invoke(null);
          Method mset = c.getDeclaredMethod("setDockIconImage", Image.class);
          mset.invoke(o, ImageIO.read(getClass().getResourceAsStream("/assets/logo.png")));
        } catch (ClassNotFoundException e1) {
        } catch (NoSuchMethodException e1) {
        } catch (SecurityException e1) {
        } catch (IllegalAccessException e1) {
        } catch (IllegalArgumentException e1) {
        } catch (InvocationTargetException e1) {
        }
      } else {
        this.setIconImage(ImageIO.read(getClass().getResourceAsStream("/assets/logo.png")));
      }
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

  private void drawPonderingState(Graphics2D g, String text, int x, int y, double size) {
    int fontSize = (int) (max(getWidth(), getHeight()) * size);
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    FontMetrics fm = g.getFontMetrics(font);
    int stringWidth = fm.stringWidth(text);
    // Truncate too long text when display switching prompt
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      int mainBoardX = boardPane.getLocation().x;
      if (getWidth() > getHeight() && (mainBoardX > x) && stringWidth > (mainBoardX - x)) {
        text = Utils.truncateStringByWidth(text, fm, mainBoardX - x);
        stringWidth = fm.stringWidth(text);
      }
    }
    // Do nothing when no text
    if (stringWidth <= 0) {
      return;
    }
    int stringHeight = fm.getAscent() - fm.getDescent();
    int width = max(stringWidth, 1);
    int height = max((int) (stringHeight * 1.7), 1);

    BufferedImage result = new BufferedImage(width, height, TYPE_INT_ARGB);
    // commenting this out for now... always causing an exception on startup. will fix in the
    // upcoming refactoring
    //        filter20.filter(cachedBackground.getSubimage(x, y, result.getWidth(),
    // result.getHeight()), result);
    g.drawImage(result, x, y, null);

    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(x, y, width, height);
    g.drawRect(x, y, width, height);

    g.setColor(Color.white);
    g.setFont(font);
    g.drawString(
        text, x + (width - stringWidth) / 2, y + stringHeight + (height - stringHeight) / 2);
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

  @Override
  public void drawControls() {
    boardPane.drawControls();
  }

  @Override
  public void replayBranch(boolean generateGif) {
    boardPane.replayBranch(generateGif);
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
  public void doBranch(int moveTo) {
    boardPane.doBranch(moveTo);
  }

  public void addSuggestionAsBranch() {
    boardPane.addSuggestionAsBranch();
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

  public void removeEstimateRect() {
    boardPane.removeEstimateRect();
    if (Lizzie.config.showSubBoard) {
      subBoardPane.removeEstimateRect();
    }
  }

  public void drawEstimateRectKata(ArrayList<Double> esitmateArray) {
    if (Lizzie.config.showSubBoard && Lizzie.config.showKataGoEstimateOnSubbord) {
      subBoardPane.drawEstimateRectKata(esitmateArray);
    }
    if (Lizzie.config.showKataGoEstimateOnMainbord) {
      boardPane.drawEstimateRectKata(esitmateArray);
    }
  }

  @Override
  public void estimateByZen() {
    if (Lizzie.board.boardHeight != Lizzie.board.boardWidth) return;
    if (isFirstCount) {
      try {
        zen = new YaZenGtp();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
      isFirstCount = false;
    } else if (!zen.process.isAlive()) {
      try {
        zen = new YaZenGtp();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
    zen.noRead = false;
    zen.syncBoradStat();
    zen.countStones();
    isEstimating = true;
  }

  @Override
  public void noAutoEstimateByZen() {
    // TODO Auto-generated method stub
    this.isAutoEstimating = false;
    removeEstimateRect();
    Lizzie.frame.repaint();
    countResults.button2.setText(
        resourceBundle.getString("CountDialog.autoEstimateButton.clickone"));
  }

  @Override
  public void noEstimateByZen() {
    // TODO Auto-generated method stub
    removeEstimateRect();
    isEstimating = false;
    countResults.button.setText(resourceBundle.getString("CountDialog.estimateButton.clickone"));
  }

  @Override
  public void drawEstimateRectZen(ArrayList<Integer> esitmateArray) {
    // TODO Auto-generated method stub
    if (!Lizzie.frame.isAutoEstimating) boardPane.drawEstimateRectZen(esitmateArray);
    else {
      if (Lizzie.config.showSubBoard) {
        try {
          subBoardPane.drawEstimateRectZen(esitmateArray);
        } catch (Exception e) {
        }
      } else boardPane.drawEstimateRectZen(esitmateArray);
    }
  }

  @Override
  public void saveImage() {
    boardPane.saveImage();
  };

  public Optional<int[]> convertScreenToCoordinates(int x, int y) {
    return boardPane.convertScreenToCoordinates(x, y);
  }

  public boolean openRightClickMenu(int x, int y) {
    Optional<int[]> boardCoordinates = convertScreenToCoordinates(x, y);
    if (!boardCoordinates.isPresent()) {
      return false;
    }
    if (isPlayingAgainstLeelaz) {
      return false;
    }
    if (Lizzie.leelaz.isPondering()) {
      Lizzie.leelaz.sendCommand("name");
    }
    isShowingRightMenu = true;

    rightClickMenu = new RightClickMenu();

    rightClickMenu.storeXY(x, y);
    Timer timer = new Timer();
    timer.schedule(
        new TimerTask() {
          public void run() {
            showMenu(x, y);
            this.cancel();
          }
        },
        50);
    return true;
  }

  private void showMenu(int x, int y) {
    rightClickMenu.show(boardPane, x, y);
  }
}
