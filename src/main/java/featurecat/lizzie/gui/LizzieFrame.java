package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.analysis.YaZenGtp;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.util.Utils;
import java.awt.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import org.json.JSONArray;

/** The window used to display the game. */
public class LizzieFrame extends MainFrame {

  private static final String[] commands = {
    resourceBundle.getString("LizzieFrame.commands.keyN"),
    resourceBundle.getString("LizzieFrame.commands.keyEnter"),
    resourceBundle.getString("LizzieFrame.commands.keySpace"),
    resourceBundle.getString("LizzieFrame.commands.keyUpArrow"),
    resourceBundle.getString("LizzieFrame.commands.keyDownArrow"),
    resourceBundle.getString("LizzieFrame.commands.rightClick"),
    resourceBundle.getString("LizzieFrame.commands.mouseWheelScroll"),
    resourceBundle.getString("LizzieFrame.commands.keyC"),
    resourceBundle.getString("LizzieFrame.commands.keyP"),
    resourceBundle.getString("LizzieFrame.commands.keyPeriod"),
    resourceBundle.getString("LizzieFrame.commands.keyA"),
    resourceBundle.getString("LizzieFrame.commands.keyM"),
    resourceBundle.getString("LizzieFrame.commands.keyI"),
    resourceBundle.getString("LizzieFrame.commands.keyO"),
    resourceBundle.getString("LizzieFrame.commands.keyS"),
    resourceBundle.getString("LizzieFrame.commands.keyAltC"),
    resourceBundle.getString("LizzieFrame.commands.keyAltV"),
    resourceBundle.getString("LizzieFrame.commands.keyF"),
    resourceBundle.getString("LizzieFrame.commands.keyV"),
    resourceBundle.getString("LizzieFrame.commands.keyW"),
    resourceBundle.getString("LizzieFrame.commands.keyCtrlW"),
    resourceBundle.getString("LizzieFrame.commands.keyG"),
    resourceBundle.getString("LizzieFrame.commands.keyR"),
    resourceBundle.getString("LizzieFrame.commands.keyBracket"),
    resourceBundle.getString("LizzieFrame.commands.keyT"),
    resourceBundle.getString("LizzieFrame.commands.keyCtrlT"),
    resourceBundle.getString("LizzieFrame.commands.keyY"),
    resourceBundle.getString("LizzieFrame.commands.keyZ"),
    resourceBundle.getString("LizzieFrame.commands.keyShiftZ"),
    resourceBundle.getString("LizzieFrame.commands.keyHome"),
    resourceBundle.getString("LizzieFrame.commands.keyEnd"),
    resourceBundle.getString("LizzieFrame.commands.keyControl"),
    resourceBundle.getString("LizzieFrame.commands.keyDelete"),
    resourceBundle.getString("LizzieFrame.commands.keyBackspace"),
    resourceBundle.getString("LizzieFrame.commands.keyE"),
  };
  private static BoardRenderer boardRenderer;
  private static BoardRenderer subBoardRenderer;
  private static VariationTree variationTree;
  private static WinrateGraph winrateGraph;
  private static Menu menu;
  private JPanel mainPanel;

  private final BufferStrategy bs;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;

  // Display Comment
  private HTMLDocument htmlDoc;
  private LizziePane.HtmlKit htmlKit;
  private StyleSheet htmlStyle;
  private JScrollPane scrollPane;
  private JTextPane commentPane;
  private BufferedImage cachedCommentImage = new BufferedImage(1, 1, TYPE_INT_ARGB);
  private String cachedComment;
  private Rectangle commentRect;

  // Show the playouts in the title
  private ScheduledExecutorService showPlayouts = Executors.newScheduledThreadPool(1);
  private long lastPlayouts = 0;
  public boolean isDrawVisitsInTitle = true;
  RightClickMenu rightClickMenu;

  /** Creates a window */
  public LizzieFrame() {
    super();

    boardRenderer = new BoardRenderer(true);
    subBoardRenderer = new BoardRenderer(false);
    variationTree = new VariationTree();
    winrateGraph = new WinrateGraph();
    countResults = new CountResults();
    menu = new Menu();
    toolBar = new ToolBar();
    add(toolBar, BorderLayout.PAGE_END);
    setMinimumSize(new Dimension(640, 400));
    boolean persisted = Lizzie.config.persistedUi != null;
    if (persisted
        && Lizzie.config.persistedUi.optJSONArray("main-window-position") != null
        && Lizzie.config.persistedUi.optJSONArray("main-window-position").length() == 4) {
      JSONArray pos = Lizzie.config.persistedUi.getJSONArray("main-window-position");
      this.setBounds(pos.getInt(0), pos.getInt(1), pos.getInt(2), pos.getInt(3));
      this.boardPositionProportion =
          Lizzie.config.persistedUi.optInt(
              "board-position-proportion", this.boardPositionProportion);
    } else {
      setSize(960, 600);
      setLocationRelativeTo(null); // Start centered, needs to be called *after* setSize...
    }
    mainPanel =
        new JPanel(true) {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            paintMianPanel(g);
          }
        };
    getContentPane().add(mainPanel);
    setJMenuBar(menu);
    mainPanel.setFocusable(true);
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

    htmlKit = new LizziePane.HtmlKit();
    htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
    htmlStyle = htmlKit.getStyleSheet();
    String style =
        "body {background:#"
            + String.format(
                "%02x%02x%02x",
                Lizzie.config.commentBackgroundColor.getRed(),
                Lizzie.config.commentBackgroundColor.getGreen(),
                Lizzie.config.commentBackgroundColor.getBlue())
            + "; color:#"
            + String.format(
                "%02x%02x%02x",
                Lizzie.config.commentFontColor.getRed(),
                Lizzie.config.commentFontColor.getGreen(),
                Lizzie.config.commentFontColor.getBlue())
            + "; font-family:"
            + Lizzie.config.fontName
            + ", Consolas, Menlo, Monaco, 'Ubuntu Mono', monospace;"
            + (Lizzie.config.commentFontSize > 0
                ? "font-size:" + Lizzie.config.commentFontSize
                : "")
            + "}";
    htmlStyle.addRule(style);
    commentPane = new JTextPane();
    commentPane.setBorder(BorderFactory.createEmptyBorder());
    commentPane.setEditorKit(htmlKit);
    commentPane.setDocument(htmlDoc);
    commentPane.setEditable(false);
    scrollPane = new JScrollPane();
    scrollPane.setViewportView(commentPane);
    scrollPane.setBorder(null);
    scrollPane.setVerticalScrollBarPolicy(
        javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    commentRect = new Rectangle(0, 0, 0, 0);

    try {
      this.setIconImage(ImageIO.read(getClass().getResourceAsStream("/assets/logo.png")));
    } catch (IOException e) {
      e.printStackTrace();
    }

    setVisible(true);

    createBufferStrategy(2);
    bs = getBufferStrategy();

    Input input = new Input();

    mainPanel.addMouseListener(input);
    mainPanel.addKeyListener(input);
    mainPanel.addMouseWheelListener(input);
    mainPanel.addMouseMotionListener(input);

    // necessary for Windows users - otherwise Lizzie shows a blank white screen on startup until
    // updates occur.
    repaint();

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
  }

  /** Clears related status from empty board. */
  public void clear() {
    if (winrateGraph != null) {
      winrateGraph.clear();
    }
  }

  private BufferedImage cachedImage;

  private BufferedImage cachedBackground;
  private int cachedBackgroundWidth = 0, cachedBackgroundHeight = 0;
  private boolean cachedBackgroundShowControls = false;
  private boolean cachedShowWinrate = true;
  private boolean cachedShowVariationGraph = true;
  private boolean cachedShowLargeSubBoard = true;
  private boolean cachedLargeWinrate = true;
  private boolean cachedShowComment = true;
  private boolean redrawBackgroundAnyway = false;
  private int cachedBoardPositionProportion = boardPositionProportion;

  /**
   * Draws the game board and interface
   *
   * @param g0 not used
   */
  public void paintMianPanel(Graphics g0) {
    autosaveMaybe();

    int width = mainPanel.getWidth();
    int height = mainPanel.getHeight();

    Optional<Graphics2D> backgroundG;
    if (cachedBackgroundWidth != width
        || cachedBackgroundHeight != height
        || cachedBoardPositionProportion != boardPositionProportion
        || redrawBackgroundAnyway) {
      backgroundG = Optional.of(createBackground(mainPanel.getWidth(), mainPanel.getHeight()));
    } else {
      backgroundG = Optional.empty();
    }

    if (!showControls) {
      // layout parameters

      int topInset = mainPanel.getInsets().top;
      int leftInset = mainPanel.getInsets().left;
      int rightInset = mainPanel.getInsets().right;
      int bottomInset = mainPanel.getInsets().bottom;
      int maxBound = Math.max(width, height);

      boolean noWinrate = !Lizzie.config.showWinrate;
      boolean noVariation = !Lizzie.config.showVariationGraph;
      boolean noBasic = !Lizzie.config.showCaptured;
      boolean noSubBoard = !Lizzie.config.showSubBoard;
      boolean noComment = !Lizzie.config.showComment;
      // board
      int maxSize = (int) (min(width - leftInset - rightInset, height - topInset - bottomInset));
      maxSize =
          max(
              maxSize,
              max(Board.boardWidth, Board.boardHeight) + 5); // don't let maxWidth become too small
      int boardX = (width - maxSize) / 8 * boardPositionProportion;
      if (noBasic && noWinrate && noSubBoard) {
        boardX = leftInset;
      } else if (noVariation && noComment) {
        boardX = (width - maxSize);
      }
      int boardY = topInset + (height - topInset - bottomInset - maxSize) / 2;

      int panelMargin = (int) (maxSize * 0.02);

      // captured stones
      int capx = leftInset;
      int capy = topInset;
      int capw = boardX - panelMargin - leftInset;
      int caph = boardY + maxSize / 8 - topInset;

      // move statistics (winrate bar)
      // boardX equals width of space on each side
      int statx = capx;
      int staty = capy + caph;
      int statw = capw;
      int stath = maxSize / 10;

      // winrate graph
      int grx = statx;
      int gry = staty + stath;
      int grw = statw;
      int grh = maxSize / 3;

      // variation tree container
      int vx = boardX + maxSize + panelMargin;
      int vy = capy;
      int vw = width - vx - rightInset;
      int vh = height - vy - bottomInset;

      // pondering message
      double ponderingSize = .02;
      int ponderingX = leftInset;
      int ponderingY =
          height - bottomInset - (int) (maxSize * 0.033) - (int) (maxBound * ponderingSize);

      // dynamic komi
      double dynamicKomiSize = .02;
      int dynamicKomiX = leftInset;
      int dynamicKomiY = ponderingY - (int) (maxBound * dynamicKomiSize);
      int dynamicKomiLabelX = leftInset;
      int dynamicKomiLabelY = dynamicKomiY - (int) (maxBound * dynamicKomiSize);

      // loading message;
      double loadingSize = 0.03;
      int loadingX = ponderingX;
      int loadingY = ponderingY - (int) (maxBound * (loadingSize - ponderingSize));

      // subboard
      int subBoardY = gry + grh;
      int subBoardWidth = grw;
      int subBoardHeight = ponderingY - subBoardY;
      int subBoardLength = min(subBoardWidth, subBoardHeight);
      int subBoardX = statx + (statw - subBoardLength) / 2;

      if (width >= height) {
        // Landscape mode
        if (Lizzie.config.showLargeSubBoard() && !noSubBoard) {
          boardX = width - maxSize - panelMargin;
          int spaceW = boardX - panelMargin - leftInset;
          int spaceH = height - topInset - bottomInset;
          int panelW = spaceW / 2;
          int panelH = spaceH / 4;

          // captured stones
          capw = (noVariation && noComment) ? spaceW : panelW;
          caph = (int) (panelH * 0.2);
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = (int) (panelH * 0.4);
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = panelH - caph - stath;
          // variation tree container
          vx = statx + statw;
          vw = panelW;
          vh = panelH;
          // subboard
          subBoardY = gry + grh;
          subBoardWidth = spaceW;
          subBoardHeight = ponderingY - subBoardY;
          subBoardLength = Math.min(subBoardWidth, subBoardHeight);
          subBoardX = statx + (spaceW - subBoardLength) / 2;
        } else if (Lizzie.config.showLargeWinrate()) {
          boardX = width - maxSize - panelMargin;
          int spaceW = boardX - panelMargin - leftInset;
          int spaceH = height - topInset - bottomInset;
          int panelW = spaceW / 2;
          int panelH = spaceH / 4;

          // captured stones
          capy = topInset + panelH + 1;
          capw = spaceW;
          caph = (int) ((ponderingY - topInset - panelH) * 0.15);
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = caph;
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = ponderingY - gry;
          // variation tree container
          vx = leftInset + panelW;
          vw = panelW;
          vh = panelH;
          // subboard
          subBoardY = topInset;
          subBoardWidth = panelW - leftInset;
          subBoardHeight = panelH;
          subBoardLength = Math.min(subBoardWidth, subBoardHeight);
          subBoardX = statx + (vw - subBoardLength) / 2;
        }
      } else {
        // Portrait mode
        if (Lizzie.config.showLargeSubBoard() && !noSubBoard) {
          // board
          maxSize = (int) (maxSize * 0.8);
          boardY = height - maxSize - bottomInset;
          int spaceW = width - leftInset - rightInset;
          int spaceH = boardY - panelMargin - topInset;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;
          boardX = (spaceW - maxSize) / 2 + leftInset;

          // captured stones
          capw = panelW / 2;
          caph = panelH / 2;
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = caph;
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = spaceH - caph - stath;
          // variation tree container
          vx = capx + capw;
          vw = panelW / 2;
          vh = spaceH;
          // subboard
          subBoardX = vx + vw;
          subBoardWidth = panelW;
          subBoardHeight = boardY - topInset;
          subBoardLength = Math.min(subBoardWidth, subBoardHeight);
          subBoardY = capy + (gry + grh - capy - subBoardLength) / 2;
          // pondering message
          ponderingY = height;
        } else if (Lizzie.config.showLargeWinrate() && !noWinrate) {
          // board
          maxSize = (int) (maxSize * 0.8);
          boardY = height - maxSize - bottomInset;
          int spaceW = width - leftInset - rightInset;
          int spaceH = boardY - panelMargin - topInset;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;
          boardX = (spaceW - maxSize) / 2 + leftInset;

          // captured stones
          capw = panelW / 2;
          caph = panelH / 4;
          // move statistics (winrate bar)
          statx = capx + capw;
          staty = capy;
          statw = capw;
          stath = caph;
          // winrate graph
          gry = staty + stath;
          grw = spaceW;
          grh = boardY - gry - 1;
          // variation tree container
          vx = statx + statw;
          vy = capy;
          vw = panelW / 2;
          vh = caph;
          // subboard
          subBoardY = topInset;
          subBoardWidth = panelW / 2;
          subBoardHeight = gry - topInset;
          subBoardLength = Math.min(subBoardWidth, subBoardHeight);
          subBoardX = vx + vw;
          // pondering message
          ponderingY = height;
        } else {
          // Normal
          // board
          boardY = (height - maxSize + topInset - bottomInset) / 2;
          int spaceW = width - leftInset - rightInset;
          int spaceH = boardY - panelMargin - topInset;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;

          // captured stones
          capw = panelW * 3 / 4;
          caph = panelH / 2;
          // move statistics (winrate bar)
          statx = capx + capw;
          staty = capy;
          statw = capw;
          stath = caph;
          // winrate graph
          grx = capx;
          gry = staty + stath;
          grw = capw + statw;
          grh = boardY - gry;
          // subboard
          subBoardX = grx + grw;
          subBoardWidth = panelW / 2;
          subBoardHeight = boardY - topInset;
          subBoardLength = Math.min(subBoardWidth, subBoardHeight);
          subBoardY = capy + (boardY - topInset - subBoardLength) / 2;
          // variation tree container
          vx = leftInset + panelW;
          vy = boardY + maxSize;
          vw = panelW;
          vh = height - vy - bottomInset;
        }
      }

      // graph container
      int contx = statx;
      int conty = staty;
      int contw = statw;
      int conth = stath + grh;
      if (width < height) {
        contw = grw;
        if (Lizzie.config.showLargeWinrate()) {
          contx = grx;
          conty = gry;
          conth = grh;
        } else {
          contx = capx;
          conty = capy;
          conth = stath + grh;
        }
      }

      // variation tree
      int treex = vx;
      int treey = vy;
      int treew = vw;
      int treeh = vh;

      // comment panel
      int cx = vx, cy = vy, cw = vw, ch = vh;
      if (Lizzie.config.showComment) {
        if (width >= height) {
          if (Lizzie.config.showVariationGraph) {
            treeh = vh / 2;
            cy = vy + treeh;
            ch = treeh;
          }
        } else {
          if (Lizzie.config.showVariationGraph) {
            if (Lizzie.config.showLargeSubBoard()) {
              treeh = vh / 2;
              cy = vy + treeh;
              ch = treeh;
            } else {
              treew = vw / 2;
              cx = vx + treew;
              cw = treew;
            }
          }
        }
      }

      // initialize

      cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
      Graphics2D g = (Graphics2D) cachedImage.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      if (Lizzie.config.showStatus) drawCommandString(g);

      boardRenderer.setLocation(boardX, boardY);
      boardRenderer.setBoardLength(maxSize, maxSize); // TODO boardSize
      boardRenderer.setupSizeParameters();
      boardRenderer.draw(g);

      if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
        if (Lizzie.config.showStatus) {
          String statusKey = "LizzieFrame.display." + (Lizzie.leelaz.isPondering() ? "on" : "off");
          String statusText = resourceBundle.getString(statusKey);
          String ponderingText = resourceBundle.getString("LizzieFrame.display.pondering");
          String switching = resourceBundle.getString("LizzieFrame.prompt.switching");
          String switchingText = Lizzie.leelaz.switching() ? switching : "";
          String weightText = Lizzie.leelaz.currentWeight();
          String text = ponderingText + " " + statusText + " " + weightText + " " + switchingText;
          drawPonderingState(g, text, ponderingX, ponderingY, ponderingSize);
        }

        Optional<String> dynamicKomi = Lizzie.leelaz.getDynamicKomi();
        if (Lizzie.config.showDynamicKomi && dynamicKomi.isPresent()) {
          String text = resourceBundle.getString("LizzieFrame.display.dynamic-komi");
          drawPonderingState(g, text, dynamicKomiLabelX, dynamicKomiLabelY, dynamicKomiSize);
          drawPonderingState(g, dynamicKomi.get(), dynamicKomiX, dynamicKomiY, dynamicKomiSize);
        }

        // Todo: Make board move over when there is no space beside the board
        if (Lizzie.config.showWinrate) {
          if (backgroundG.isPresent()) {
            drawContainer(backgroundG.get(), contx, conty, contw, conth);
          }
          drawMoveStatistics(g, statx, staty, statw, stath);
          winrateGraph.draw(g, grx, gry, grw, grh);
        }

        if (Lizzie.config.showVariationGraph || Lizzie.config.showComment) {
          if (backgroundG.isPresent()) {
            drawContainer(backgroundG.get(), vx, vy, vw, vh);
          }
          if (Lizzie.config.showVariationGraph) {
            variationTree.draw(g, treex, treey, treew, treeh);
          }
          if (Lizzie.config.showComment) {
            drawComment(g, cx, cy, cw, ch);
          }
        }

        if (Lizzie.config.showSubBoard) {
          try {
            subBoardRenderer.setLocation(subBoardX, subBoardY);
            subBoardRenderer.setBoardLength(subBoardLength, subBoardLength); // TODO boardSize
            subBoardRenderer.setupSizeParameters();
            subBoardRenderer.draw(g);
          } catch (Exception e) {
            // This can happen when no space is left for subboard.
          }
        }
      } else if (Lizzie.config.showStatus) {
        String loadingText = resourceBundle.getString("LizzieFrame.display.loading");
        drawPonderingState(g, loadingText, loadingX, loadingY, loadingSize);
      }

      if (Lizzie.config.showCaptured) drawCaptured(g, capx, capy, capw, caph);

      // cleanup
      g.dispose();
    }
    g0.drawImage(cachedBackground, 0, 0, null);
    g0.drawImage(cachedImage, 0, 0, null);
    // draw the image
    //    Graphics2D bsGraphics = (Graphics2D) bs.getDrawGraphics();
    //    bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING,
    // RenderingHints.VALUE_RENDER_QUALITY);
    //    bsGraphics.drawImage(cachedBackground, 0, 0, null);
    //    bsGraphics.drawImage(cachedImage, 0, 0, null);
    //
    //    // cleanup
    g0.dispose();
    //    bs.show();
  }

  /**
   * temporary measure to refresh background. ideally we shouldn't need this (but we want to release
   * Lizzie 0.5 today, not tomorrow!). Refactor me out please! (you need to get blurring to work
   * properly on startup).
   */
  public void refreshBackground() {
    redrawBackgroundAnyway = true;
  }

  private Graphics2D createBackground(int width, int hight) {
    cachedBackground = new BufferedImage(width, hight, TYPE_INT_RGB);
    cachedBackgroundWidth = cachedBackground.getWidth();
    cachedBackgroundHeight = cachedBackground.getHeight();
    cachedBackgroundShowControls = showControls;
    cachedShowWinrate = Lizzie.config.showWinrate;
    cachedShowVariationGraph = Lizzie.config.showVariationGraph;
    cachedShowLargeSubBoard = Lizzie.config.showLargeSubBoard();
    cachedLargeWinrate = Lizzie.config.showLargeWinrate();
    cachedShowComment = Lizzie.config.showComment;
    cachedBoardPositionProportion = boardPositionProportion;

    //    redrawBackgroundAnyway = false;

    Graphics2D g = cachedBackground.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    BufferedImage wallpaper = boardRenderer.getWallpaper();
    int drawWidth = max(wallpaper.getWidth(), mainPanel.getWidth());
    int drawHeight = max(wallpaper.getHeight(), mainPanel.getHeight());
    // Support seamless texture
    boardRenderer.drawTextureImage(g, wallpaper, 0, 0, drawWidth, drawHeight);

    return g;
  }

  private void drawContainer(Graphics g, int vx, int vy, int vw, int vh) {
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

  private void drawPonderingState(Graphics2D g, String text, int x, int y, double size) {
    int fontSize = (int) (max(mainPanel.getWidth(), mainPanel.getHeight()) * size);
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    FontMetrics fm = g.getFontMetrics(font);
    int stringWidth = fm.stringWidth(text);
    // Truncate too long text when display switching prompt
    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      int mainBoardX = boardRenderer.getLocation().x;
      if (mainPanel.getWidth() > mainPanel.getHeight()
          && (mainBoardX > x)
          && stringWidth > (mainBoardX - x)) {
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
    int height = max((int) (stringHeight * 1.2), 1);

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
  private GaussianFilter filter10 = new GaussianFilter(10);

  /** Display the controls */
  public void drawControls() {
    userAlreadyKnowsAboutCommandString = true;

    cachedImage = new BufferedImage(mainPanel.getWidth(), mainPanel.getHeight(), TYPE_INT_ARGB);

    // redraw background
    createBackground(mainPanel.getWidth(), mainPanel.getHeight());

    List<String> commandsToShow = new ArrayList<>(Arrays.asList(commands));
    if (Lizzie.leelaz.getDynamicKomi().isPresent()) {
      commandsToShow.add(resourceBundle.getString("LizzieFrame.commands.keyD"));
    }

    Graphics2D g = cachedImage.createGraphics();

    int maxSize = min(mainPanel.getWidth(), mainPanel.getHeight());
    int fontSize = (int) (maxSize * min(0.034, 0.80 / commandsToShow.size()));
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    g.setFont(font);

    FontMetrics metrics = g.getFontMetrics(font);
    int maxCmdWidth = commandsToShow.stream().mapToInt(c -> metrics.stringWidth(c)).max().orElse(0);
    int lineHeight = (int) (font.getSize() * 1.15);

    int boxWidth = min((int) (maxCmdWidth * 1.4), mainPanel.getWidth());
    int boxHeight =
        min(
            commandsToShow.size() * lineHeight,
            mainPanel.getHeight() - getInsets().top - getInsets().bottom);

    int commandsX = min(mainPanel.getWidth() / 2 - boxWidth / 2, mainPanel.getWidth());
    int top = this.getInsets().top;
    int commandsY =
        top + min((mainPanel.getHeight() - top) / 2 - boxHeight / 2, mainPanel.getHeight() - top);

    BufferedImage result = new BufferedImage(boxWidth, boxHeight, TYPE_INT_ARGB);
    filter10.filter(
        cachedBackground.getSubimage(commandsX, commandsY, boxWidth, boxHeight), result);
    g.drawImage(result, commandsX, commandsY, null);

    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(commandsX, commandsY, boxWidth, boxHeight);
    int strokeRadius = Lizzie.config.showBorder ? 2 : 1;
    g.setStroke(new BasicStroke(strokeRadius == 1 ? strokeRadius : 2 * strokeRadius));
    if (Lizzie.config.showBorder) {
      g.setColor(new Color(0, 0, 0, 60));
      g.drawRect(
          commandsX + strokeRadius,
          commandsY + strokeRadius,
          boxWidth - 2 * strokeRadius,
          boxHeight - 2 * strokeRadius);
    }
    int verticalLineX = (int) (commandsX + boxWidth * 0.3);
    g.setColor(new Color(0, 0, 0, 60));
    g.drawLine(
        verticalLineX,
        commandsY + 2 * strokeRadius,
        verticalLineX,
        commandsY + boxHeight - 2 * strokeRadius);

    g.setStroke(new BasicStroke(1));

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g.setColor(Color.WHITE);
    int lineOffset = commandsY;
    for (String command : commandsToShow) {
      String[] split = command.split("\\|");
      g.drawString(
          split[0],
          verticalLineX - metrics.stringWidth(split[0]) - strokeRadius * 4,
          font.getSize() + lineOffset);
      g.drawString(split[1], verticalLineX + strokeRadius * 4, font.getSize() + lineOffset);
      lineOffset += lineHeight;
    }

    refreshBackground();
  }

  private boolean userAlreadyKnowsAboutCommandString = false;

  private void drawCommandString(Graphics2D g) {
    if (userAlreadyKnowsAboutCommandString) return;

    int maxSize = (int) (min(mainPanel.getWidth(), mainPanel.getHeight()) * 0.98);

    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, (int) (maxSize * 0.03));
    String commandString = resourceBundle.getString("LizzieFrame.prompt.showControlsHint");
    int strokeRadius = Lizzie.config.showBorder ? 2 : 0;

    int showCommandsHeight = (int) (font.getSize() * 1.1);
    int showCommandsWidth = g.getFontMetrics(font).stringWidth(commandString) + 4 * strokeRadius;
    int showCommandsX = mainPanel.getInsets().left;
    int showCommandsY = mainPanel.getHeight() - showCommandsHeight - mainPanel.getInsets().bottom;
    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(showCommandsX, showCommandsY, showCommandsWidth, showCommandsHeight);
    if (Lizzie.config.showBorder) {
      g.setStroke(new BasicStroke(2 * strokeRadius));
      g.setColor(new Color(0, 0, 0, 60));
      g.drawRect(
          showCommandsX + strokeRadius,
          showCommandsY + strokeRadius,
          showCommandsWidth - 2 * strokeRadius,
          showCommandsHeight - 2 * strokeRadius);
    }
    g.setStroke(new BasicStroke(1));

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    g.setFont(font);
    g.drawString(commandString, showCommandsX + 2 * strokeRadius, showCommandsY + font.getSize());
  }

  private void drawMoveStatistics(Graphics2D g, int posX, int posY, int width, int height) {
    if (width < 0 || height < 0) return; // we don't have enough space

    double lastWR = 50; // winrate the previous move
    boolean validLastWinrate = false; // whether it was actually calculated
    Optional<BoardData> previous = Lizzie.board.getHistory().getPrevious();
    if (previous.isPresent() && previous.get().getPlayouts() > 0) {
      lastWR = previous.get().winrate;
      validLastWinrate = true;
    }

    Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
    double curWR = stats.maxWinrate; // winrate on this move
    double curSM = stats.maxScoreMean; // mean score on this move
    boolean validWinrate = (stats.totalPlayouts > 0); // and whether it was actually calculated
    if (!validWinrate) {
      curWR = Lizzie.board.getHistory().getData().winrate;
      validWinrate = Lizzie.board.getHistory().getData().getPlayouts() > 0;
    }
    if (isPlayingAgainstLeelaz
        && playerIsBlack == !Lizzie.board.getHistory().getData().blackToPlay) {
      validWinrate = false;
    }

    if (!validWinrate) {
      curWR = 100 - lastWR; // display last move's winrate for now (with color difference)
    }
    double whiteWR, blackWR, blackSM;
    if (Lizzie.board.getData().blackToPlay) {
      blackWR = curWR;
      blackSM = curSM;
    } else {
      blackWR = 100 - curWR;
      blackSM = -curSM;
    }

    whiteWR = 100 - blackWR;

    // Background rectangle
    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(posX, posY, width, height);

    // border. does not include bottom edge
    int strokeRadius = Lizzie.config.showBorder ? 3 : 1;
    g.setStroke(new BasicStroke(strokeRadius == 1 ? strokeRadius : 2 * strokeRadius));
    g.drawLine(
        posX + strokeRadius, posY + strokeRadius,
        posX - strokeRadius + width, posY + strokeRadius);
    if (Lizzie.config.showBorder) {
      g.drawLine(
          posX + strokeRadius, posY + 3 * strokeRadius,
          posX + strokeRadius, posY - strokeRadius + height);
      g.drawLine(
          posX - strokeRadius + width, posY + 3 * strokeRadius,
          posX - strokeRadius + width, posY - strokeRadius + height);
    }

    // resize the box now so it's inside the border
    posX += 2 * strokeRadius;
    posY += 2 * strokeRadius;
    width -= 4 * strokeRadius;
    height -= 4 * strokeRadius;

    // Title
    strokeRadius = 2;
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    setPanelFont(g, (int) (min(width, height) * 0.2));

    String text = "";
    if (Lizzie.leelaz.isKataGo) {
      double score = Lizzie.leelaz.scoreMean;
      if (Lizzie.board.getHistory().isBlacksTurn()) {
        if (Lizzie.config.showKataGoBoardScoreMean) {
          score = score + Lizzie.board.getHistory().getGameInfo().getKomi();
        }
      } else {
        if (Lizzie.config.showKataGoBoardScoreMean) {
          score = score - Lizzie.board.getHistory().getGameInfo().getKomi();
        }
        if (Lizzie.config.kataGoScoreMeanAlwaysBlack) {
          score = -score;
        }
      }
      text =
          resourceBundle.getString("LizzieFrame.katago.scoreMean")
              + ": "
              + String.format("%.1f", score)
              + " ";
      text =
          text
              + resourceBundle.getString("LizzieFrame.katago.scoreStdev")
              + ": "
              + String.format("%.1f", Lizzie.leelaz.scoreStdev)
              + " ";
    }
    // Last move
    if (validLastWinrate && validWinrate) {

      if (Lizzie.config.handicapInsteadOfWinrate) {
        double currHandicapedWR = Lizzie.leelaz.winrateToHandicap(100 - curWR);
        double lastHandicapedWR = Lizzie.leelaz.winrateToHandicap(lastWR);
        text =
            text
                + resourceBundle.getString("LizzieFrame.display.lastMove")
                + String.format(": %.2f", currHandicapedWR - lastHandicapedWR);
      } else {
        text =
            text
                + resourceBundle.getString("LizzieFrame.display.lastMove")
                + String.format(": %.1f%%", 100 - lastWR - curWR);
      }
      g.drawString(
          text, posX + 2 * strokeRadius, posY + height - 2 * strokeRadius); // - font.getSize());
    } else {
      // I think it's more elegant to just not display anything when we don't have
      // valid data --dfannius
      // g.drawString(resourceBundle.getString("LizzieFrame.display.lastMove") + ": ?%",
      //              posX + 2 * strokeRadius, posY + height - 2 * strokeRadius);
    }

    if (validWinrate || validLastWinrate) {
      int maxBarwidth = (int) (width);
      int barWidthB = (int) (blackWR * maxBarwidth / 100);
      int barWidthW = (int) (whiteWR * maxBarwidth / 100);
      int barPosY = posY + height / 3;
      int barPosxB = (int) (posX);
      int barPosxW = barPosxB + barWidthB;
      int barHeight = height / 3;

      // Draw winrate bars
      g.fillRect(barPosxW, barPosY, barWidthW, barHeight);
      g.setColor(Color.BLACK);
      g.fillRect(barPosxB, barPosY, barWidthB, barHeight);

      // Show percentage above bars
      g.setColor(Color.WHITE);
      g.drawString(
          String.format("%.1f%%", blackWR),
          barPosxB + 2 * strokeRadius,
          posY + barHeight - 2 * strokeRadius);
      String winString = String.format("%.1f%%", whiteWR);
      int sw = g.getFontMetrics().stringWidth(winString);
      g.drawString(
          winString,
          barPosxB + maxBarwidth - sw - 2 * strokeRadius,
          posY + barHeight - 2 * strokeRadius);
      if (Lizzie.leelaz.isKataGo) {
        String scoreString = String.format("%.1f", blackSM);
        sw = g.getFontMetrics().stringWidth(scoreString);
        g.drawString(
            scoreString,
            barPosxB + maxBarwidth / 2 - sw / 2 - strokeRadius,
            posY + barHeight - 2 * strokeRadius);
      }

      g.setColor(Color.GRAY);
      Stroke oldstroke = g.getStroke();
      Stroke dashed =
          new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {4}, 0);
      g.setStroke(dashed);

      for (int i = 1; i <= winRateGridLines; i++) {
        int x = barPosxB + (int) (i * (maxBarwidth / (winRateGridLines + 1)));
        g.drawLine(x, barPosY, x, barPosY + barHeight);
      }
      g.setStroke(oldstroke);
    }
  }

  private void drawCaptured(Graphics2D g, int posX, int posY, int width, int height) {
    // Draw border
    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(posX, posY, width, height);

    // border. does not include bottom edge
    int strokeRadius = Lizzie.config.showBorder ? 3 : 1;
    g.setStroke(new BasicStroke(strokeRadius == 1 ? strokeRadius : 2 * strokeRadius));
    if (Lizzie.config.showBorder) {
      g.drawLine(
          posX + strokeRadius,
          posY + strokeRadius,
          posX - strokeRadius + width,
          posY + strokeRadius);
      g.drawLine(
          posX + strokeRadius,
          posY + 3 * strokeRadius,
          posX + strokeRadius,
          posY - strokeRadius + height);
      g.drawLine(
          posX - strokeRadius + width,
          posY + 3 * strokeRadius,
          posX - strokeRadius + width,
          posY - strokeRadius + height);
    }

    // Draw middle line
    g.drawLine(
        posX - strokeRadius + width / 2,
        posY + 3 * strokeRadius,
        posX - strokeRadius + width / 2,
        posY - strokeRadius + height);
    g.setColor(Color.white);

    // Draw black and white "stone"
    int diam = height / 3;
    int smallDiam = diam / 2;
    int bdiam = diam, wdiam = diam;
    if (Lizzie.board != null) {
      if (Lizzie.board.inScoreMode() || isEstimating) {
        // do nothing
      } else if (Lizzie.board.getHistory().isBlacksTurn()) {
        wdiam = smallDiam;
      } else {
        bdiam = smallDiam;
      }
    } else {
      bdiam = smallDiam;
    }
    g.setColor(Color.black);
    g.fillOval(
        posX + width / 4 - bdiam / 2, posY + height * 3 / 8 + (diam - bdiam) / 2, bdiam, bdiam);

    g.setColor(Color.WHITE);
    g.fillOval(
        posX + width * 3 / 4 - wdiam / 2, posY + height * 3 / 8 + (diam - wdiam) / 2, wdiam, wdiam);

    // Draw captures
    String bval = "", wval = "";
    setPanelFont(g, (float) (height * 0.18));
    if (Lizzie.board == null) {
      return;
    }
    if (Lizzie.board.inScoreMode()) {
      double score[] = Lizzie.board.getScore(Lizzie.board.scoreStones());
      bval = String.format("%.0f", score[0]);
      wval = String.format("%.1f", score[1]);
    } else if (isEstimating || isAutoEstimating) {
      bval = String.format("%d", countResults.allBlackCounts);
      wval = String.format("%d", countResults.allWhiteCounts);
    } else {
      bval = String.format("%d", Lizzie.board.getData().blackCaptures);
      wval = String.format("%d", Lizzie.board.getData().whiteCaptures);
    }

    g.setColor(Color.WHITE);
    int bw = g.getFontMetrics().stringWidth(bval);
    int ww = g.getFontMetrics().stringWidth(wval);
    boolean largeSubBoard = Lizzie.config.showLargeSubBoard();
    int bx = (largeSubBoard ? diam : -bw / 2);
    int wx = (largeSubBoard ? bx : -ww / 2);

    g.drawString(bval, posX + width / 4 + bx, posY + height * 7 / 8);
    g.drawString(wval, posX + width * 3 / 4 + wx, posY + height * 7 / 8);
  }

  private void setPanelFont(Graphics2D g, float size) {
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, (int) size);
    g.setFont(font);
  }

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {
    // Check for board click
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
    int moveNumber = winrateGraph.moveNumber(x, y);

    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
      if (!isPlayingAgainstLeelaz || (playerIsBlack == Lizzie.board.getData().blackToPlay))
        Lizzie.board.place(coords[0], coords[1]);
    }
    if (Lizzie.config.showWinrate && moveNumber >= 0) {
      isPlayingAgainstLeelaz = false;
      Lizzie.board.goToMoveNumberBeyondBranch(moveNumber);
    }
    if (Lizzie.config.showSubBoard && subBoardRenderer.isInside(x, y)) {
      Lizzie.config.toggleLargeSubBoard();
    }
    if (Lizzie.config.showVariationGraph) {
      variationTree.onClicked(x, y);
    }
    repaint();
  }

  public void onDoubleClicked(int x, int y) {
    // Check for board double click
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (!isPlayingAgainstLeelaz) {
        int moveNumber = Lizzie.board.moveNumberByCoord(coords);
        if (moveNumber > 0) {
          Lizzie.board.goToMoveNumberBeyondBranch(moveNumber);
        }
      }
    }
  }

  private final Consumer<String> placeVariation =
      v -> Board.asCoordinates(v).ifPresent(c -> Lizzie.board.place(c[0], c[1]));

  public boolean playCurrentVariation() {
    boardRenderer.variationOpt.ifPresent(vs -> vs.forEach(placeVariation));
    return boardRenderer.variationOpt.isPresent();
  }

  public void playBestMove() {
    boardRenderer.bestMoveCoordinateName().ifPresent(placeVariation);
  }

  public void onMouseMoved(int x, int y) {
    mouseOverCoordinate = outOfBoundCoordinate;
    Optional<int[]> coords = boardRenderer.convertScreenToCoordinates(x, y);
    coords.filter(c -> !isMouseOver(c[0], c[1])).ifPresent(c -> repaint());
    coords.ifPresent(
        c -> {
          mouseOverCoordinate = c;
          isReplayVariation = false;
        });
    if (!coords.isPresent() && boardRenderer.isShowingBranch()) {
      repaint();
    }
  }

  public boolean isMouseOver(int x, int y) {
    return mouseOverCoordinate[0] == x && mouseOverCoordinate[1] == y;
  }

  public void onMouseDragged(int x, int y) {
    int moveNumber = winrateGraph.moveNumber(x, y);
    if (Lizzie.config.showWinrate && moveNumber >= 0) {
      if (Lizzie.board.goToMoveNumberWithinBranch(moveNumber)) {
        repaint();
      }
    }
  }

  /**
   * Process Comment Mouse Wheel Moved
   *
   * @return true when the scroll event was processed by this method
   */
  @Override
  public boolean processCommentMouseWheelMoved(MouseWheelEvent e) {
    if (Lizzie.config.showComment && commentRect.contains(e.getX(), e.getY())) {
      scrollPane.dispatchEvent(e);
      createCommentImage(true, commentRect.width, commentRect.height);
      getGraphics()
          .drawImage(
              cachedCommentImage,
              commentRect.x,
              commentRect.y,
              commentRect.width,
              commentRect.height,
              null);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Create comment cached image
   *
   * @param forceRefresh
   * @param w
   * @param h
   */
  public void createCommentImage(boolean forceRefresh, int w, int h) {
    if (forceRefresh || scrollPane.getWidth() != w || scrollPane.getHeight() != h) {
      if (w > 0 && h > 0) {
        scrollPane.setSize(w, h);
        cachedCommentImage =
            new BufferedImage(scrollPane.getWidth(), scrollPane.getHeight(), TYPE_INT_ARGB);
        Graphics2D g2 = cachedCommentImage.createGraphics();
        scrollPane.doLayout();
        scrollPane.addNotify();
        scrollPane.validate();
        scrollPane.printAll(g2);
        g2.dispose();
      }
    }
  }

  private void autosaveMaybe() {
    int interval =
        Lizzie.config.config.getJSONObject("ui").getInt("autosave-interval-seconds") * 1000;
    long currentTime = System.currentTimeMillis();
    if (interval > 0 && currentTime - lastAutosaveTime >= interval) {
      Lizzie.board.autosave();
      lastAutosaveTime = currentTime;
    }
  }

  private void setDisplayedBranchLength(int n) {
    boardRenderer.setDisplayedBranchLength(n);
  }

  public void startRawBoard() {
    boolean onBranch = boardRenderer.isShowingBranch();
    int n = (onBranch ? 1 : BoardRenderer.SHOW_RAW_BOARD);
    boardRenderer.setDisplayedBranchLength(n);
  }

  public void stopRawBoard() {
    boardRenderer.setDisplayedBranchLength(BoardRenderer.SHOW_NORMAL_BOARD);
  }

  public boolean incrementDisplayedBranchLength(int n) {
    return boardRenderer.incrementDisplayedBranchLength(n);
  }

  public void copySgf() {
    try {
      // Get sgf content from game
      String sgfContent = SGFParser.saveToString();

      // Save to clipboard
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable transferableString = new StringSelection(sgfContent);
      clipboard.setContents(transferableString, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void pasteSgf() {
    // Get string from clipboard
    String sgfContent =
        Optional.ofNullable(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null))
            .filter(cc -> cc.isDataFlavorSupported(DataFlavor.stringFlavor))
            .flatMap(
                cc -> {
                  try {
                    return Optional.of((String) cc.getTransferData(DataFlavor.stringFlavor));
                  } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                  return Optional.empty();
                })
            .orElse("");

    // Load game contents from sgf string
    if (!sgfContent.isEmpty()) {
      SGFParser.loadFromString(sgfContent);
    }
  }

  public void increaseMaxAlpha(int k) {
    boardRenderer.increaseMaxAlpha(k);
  }

  /**
   * Draw the Comment of the Sgf file
   *
   * @param g
   * @param x
   * @param y
   * @param w
   * @param h
   */
  private void drawComment(Graphics2D g, int x, int y, int w, int h) {
    String comment = Lizzie.board.getHistory().getData().comment;
    int fontSize = (int) (min(mainPanel.getWidth(), mainPanel.getHeight()) * 0.0294);
    if (Lizzie.config.commentFontSize > 0) {
      fontSize = Lizzie.config.commentFontSize;
    } else if (fontSize < 16) {
      fontSize = 16;
    }
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    commentPane.setFont(font);
    comment = comment.replaceAll("(\r\n)|(\n)", "<br />").replaceAll(" ", "&nbsp;");
    commentPane.setText(comment);
    commentPane.setSize(w, h);
    createCommentImage(!comment.equals(this.cachedComment), w, h);
    commentRect = new Rectangle(x, y, scrollPane.getWidth(), scrollPane.getHeight());
    g.drawImage(
        cachedCommentImage,
        commentRect.x,
        commentRect.y,
        commentRect.width,
        commentRect.height,
        null);
    cachedComment = comment;
  }

  public void replayBranch(boolean generateGif) {
    if (isReplayVariation) return;
    int replaySteps = boardRenderer.getReplayBranch();
    if (replaySteps <= 0) return; // Bad steps or no branch
    int oriBranchLength = boardRenderer.getDisplayedBranchLength();
    isReplayVariation = true;
    if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
    Runnable runnable =
        new Runnable() {
          public void run() {
            int secs = (int) (Lizzie.config.replayBranchIntervalSeconds * 1000);
            for (int i = 1; i < replaySteps + 1; i++) {
              if (!isReplayVariation) break;
              setDisplayedBranchLength(i);
              repaint();
              try {
                Thread.sleep(secs);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            boardRenderer.setDisplayedBranchLength(oriBranchLength);
            isReplayVariation = false;
            if (!Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
          }
        };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  public void removeEstimateRect() {
    boardRenderer.removeEstimateRect();
    if (Lizzie.config.showSubBoard) {
      subBoardRenderer.removeEstimateRect();
    }
  }

  public void drawEstimateRectKata(ArrayList<Double> esitmateArray) {
    if (!Lizzie.config.showKataGoEstimate) {
      return;
    }
    if (Lizzie.config.showKataGoEstimateBySize) {
      if (Lizzie.config.showSubBoard && Lizzie.config.showKataGoEstimateOnSubbord) {
        subBoardRenderer.drawEstimateRectKataBySize(esitmateArray);
      }
      if (Lizzie.config.showKataGoEstimateOnMainbord) {
        boardRenderer.drawEstimateRectKataBySize(esitmateArray);
      }
    } else {
      if (Lizzie.config.showSubBoard && Lizzie.config.showKataGoEstimateOnSubbord) {
        subBoardRenderer.drawEstimateRectKata(esitmateArray);
      }
      if (Lizzie.config.showKataGoEstimateOnMainbord) {
        boardRenderer.drawEstimateRectKata(esitmateArray);
      }
    }
  }

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

  public void drawEstimateRectZen(ArrayList<Integer> esitmateArray) {
    if (!Lizzie.frame.isAutoEstimating) boardRenderer.drawEstimateRectZen(esitmateArray);
    else {
      if (Lizzie.config.showSubBoard) {
        try {
          subBoardRenderer.drawEstimateRectZen(esitmateArray);
        } catch (Exception e) {
        }
      } else boardRenderer.drawEstimateRectZen(esitmateArray);
    }
  }

  public void noAutoEstimateByZen() {
    this.isAutoEstimating = false;
    removeEstimateRect();
    Lizzie.frame.repaint();
    countResults.button2.setText(
        resourceBundle.getString("CountDialog.autoEstimateButton.clickone"));
  }

  public void noEstimateByZen(boolean byToolBar) {
    removeEstimateRect();
    isEstimating = false;
    countResults.button.setText(resourceBundle.getString("CountDialog.estimateButton.clickone"));
    if (byToolBar) countResults.setVisible(false);
  }

  public void updateEngineMenu(List<Leelaz> engineList) {
    menu.updateEngineMenu(engineList);
  }

  public void updateEngineIcon(List<Leelaz> engineList, int currentEngineNo) {
    menu.updateEngineIcon(engineList, currentEngineNo);
  }

  public Optional<int[]> convertScreenToCoordinates(int x, int y) {
    return boardRenderer.convertScreenToCoordinates(x, y);
  }

  public boolean openRightClickMenu(int x, int y) {
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
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
    rightClickMenu.show(mainPanel, x, y);
  }
}
