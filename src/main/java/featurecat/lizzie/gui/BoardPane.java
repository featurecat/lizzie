package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.SGFParser;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** The window used to display the game. */
public class BoardPane extends LizziePane {
  private static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");

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
    resourceBundle.getString("LizzieFrame.commands.keyHome"),
    resourceBundle.getString("LizzieFrame.commands.keyEnd"),
    resourceBundle.getString("LizzieFrame.commands.keyControl"),
    resourceBundle.getString("LizzieFrame.commands.keyDelete"),
    resourceBundle.getString("LizzieFrame.commands.keyBackspace"),
  };
  private static final String DEFAULT_TITLE = "Lizzie - Leela Zero Interface";
  private static BoardRenderer boardRenderer;

  public static Font uiFont;
  public static Font winrateFont;

  //  private final BufferStrategy bs;
  private static boolean started = false;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;
  public boolean showControls = false;
  public boolean isPlayingAgainstLeelaz = false;
  public boolean playerIsBlack = true;
  public int winRateGridLines = 3;
  public int BoardPositionProportion = 4;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;

  // Save the player title
  private String playerTitle = "";

  public boolean isNewGame = false;

  static {
    // load fonts
    try {
      uiFont =
          Font.createFont(
              Font.TRUETYPE_FONT,
              Thread.currentThread()
                  .getContextClassLoader()
                  .getResourceAsStream("fonts/OpenSans-Regular.ttf"));
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

  LizzieMain owner;
  /** Creates a window */
  public BoardPane(LizzieMain owner) {
    this.owner = owner;

    //    setBackground(new Color(0, 0, 0, 0));
    //    JPanel panel = new JPanel() {
    //        @Override
    //        protected void paintComponent(Graphics g) {
    //            if (g instanceof Graphics2D) {
    //                final int R = 240;
    //                final int G = 240;
    //                final int B = 240;
    //
    //                Paint p =
    //                    new GradientPaint(0.0f, 0.0f, new Color(R, G, B, 0),
    //                        0.0f, getHeight(), new Color(R, G, B, 0), true);
    //                Graphics2D g2d = (Graphics2D)g;
    //                g2d.setPaint(p);
    //                g2d.fillRect(0, 0, getWidth(), getHeight());
    //            }
    //        }
    //    };
    //    setContentPane(panel);
    //  getContentPane().setBackground(new Color(0, 100, 100, 0));

    boardRenderer = new BoardRenderer(true);

    // Allow change font in the config
    if (Lizzie.config.uiFontName != null) {
      uiFont = new Font(Lizzie.config.uiFontName, Font.PLAIN, 12);
    }
    if (Lizzie.config.winrateFontName != null) {
      winrateFont = new Font(Lizzie.config.winrateFontName, Font.BOLD, 12);
    }

    // TODO BufferStrategy does not support transparent background?
    //    createBufferStrategy(2);
    //    bs = getBufferStrategy();

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            onClicked(e.getX(), e.getY());
          }
        });
    addMouseMotionListener(
        new MouseAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            onMouseMoved(e.getX(), e.getY());
          }
        });
  }

  /** Clears related status from empty board. */
  public void clear() {
    if (LizzieMain.winratePane != null) {
      LizzieMain.winratePane.clear();
    }
    Lizzie.main.updateStatus();
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
  private int cachedBoardPositionProportion = BoardPositionProportion;

  /**
   * Draws the game board and interface
   *
   * @param g0 not used
   */
  @Override
  protected void paintComponent(Graphics g0) {
    super.paintComponent(g0);
    autosaveMaybe();

    int width = getWidth();
    int height = getHeight();

    if (!showControls) {
      // layout parameters

      int topInset = this.getInsets().top;
      int leftInset = this.getInsets().left;
      int rightInset = this.getInsets().right;
      int bottomInset = this.getInsets().bottom;
      int maxBound = Math.max(width, height);

      // board
      int maxSize = (int) (min(width - leftInset - rightInset, height - topInset - bottomInset));
      maxSize = max(maxSize, Board.boardSize + 5); // don't let maxWidth become too small
      int boardX = (width - maxSize) / 8 * BoardPositionProportion;
      int boardY = topInset + (height - topInset - bottomInset - maxSize) / 2;

      // initialize

      cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
      Graphics2D g = (Graphics2D) cachedImage.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      boardRenderer.setLocation(boardX, boardY);
      boardRenderer.setBoardLength(maxSize);
      boardRenderer.draw(g);

      owner.repaintSub();

      if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded() && !started) {
        started = true;
        if (Lizzie.config.showVariationGraph || Lizzie.config.showComment) {
          owner.updateStatus();
        }
      }

      // cleanup
      g.dispose();
    }

    // draw the image
    // TODO BufferStrategy does not support transparent background?
    Graphics2D bsGraphics = (Graphics2D) g0; // bs.getDrawGraphics();
    bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    //    bsGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
    //        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    bsGraphics.drawImage(cachedImage, 0, 0, null);

    // cleanup
    bsGraphics.dispose();
    // TODO BufferStrategy does not support transparent background?
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

  private Graphics2D createBackground() {
    cachedBackground = new BufferedImage(getWidth(), getHeight(), TYPE_INT_ARGB);
    cachedBackgroundWidth = cachedBackground.getWidth();
    cachedBackgroundHeight = cachedBackground.getHeight();
    cachedBackgroundShowControls = showControls;
    cachedShowWinrate = Lizzie.config.showWinrate;
    cachedShowVariationGraph = Lizzie.config.showVariationGraph;
    cachedShowLargeSubBoard = Lizzie.config.showLargeSubBoard();
    cachedLargeWinrate = Lizzie.config.showLargeWinrate();
    cachedShowComment = Lizzie.config.showComment;
    cachedBoardPositionProportion = BoardPositionProportion;

    redrawBackgroundAnyway = false;

    Graphics2D g = cachedBackground.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    //    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
    //        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    //
    //    final int R = 240;
    //    final int G = 240;
    //    final int B = 240;
    //    Paint p =
    //        new GradientPaint(0.0f, 0.0f, new Color(R, G, B, 0),
    //            0.0f, getHeight(), new Color(R, G, B, 0), false);
    //  g.setPaint(p);
    //    AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);
    //    g.setComposite(composite);
    //    g.setColor(new Color(0, 0, 0, 0));
    //    g.fillRect(0, 0, getWidth(), getHeight());

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

    BufferedImage result = new BufferedImage(vw, vh, TYPE_INT_ARGB);
    filter20.filter(cachedBackground.getSubimage(vx, vy, vw, vh), result);
    g.drawImage(result, vx, vy, null);
  }

  private void drawPonderingState(Graphics2D g, String text, int x, int y, double size) {
    int fontSize = (int) (max(getWidth(), getHeight()) * size);
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    FontMetrics fm = g.getFontMetrics(font);
    int stringWidth = fm.stringWidth(text);
    // Truncate too long text when display switching prompt
    if (Lizzie.leelaz.isLoaded()) {
      int mainBoardX = boardRenderer.getLocation().x;
      if (getWidth() > getHeight() && (mainBoardX > x) && stringWidth > (mainBoardX - x)) {
        text = truncateStringByWidth(text, fm, mainBoardX - x);
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
    // commenting this out for now... always causing an exception on startup. will
    // fix in the
    // upcoming refactoring
    // filter20.filter(cachedBackground.getSubimage(x, y, result.getWidth(),
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

  /**
   * @return a shorter, rounded string version of playouts. e.g. 345 -> 345, 1265 -> 1.3k, 44556 ->
   *     45k, 133523 -> 134k, 1234567 -> 1.2m
   */
  public String getPlayoutsString(int playouts) {
    if (playouts >= 1_000_000) {
      double playoutsDouble = (double) playouts / 100_000; // 1234567 -> 12.34567
      return round(playoutsDouble) / 10.0 + "m";
    } else if (playouts >= 10_000) {
      double playoutsDouble = (double) playouts / 1_000; // 13265 -> 13.265
      return round(playoutsDouble) + "k";
    } else if (playouts >= 1_000) {
      double playoutsDouble = (double) playouts / 100; // 1265 -> 12.65
      return round(playoutsDouble) / 10.0 + "k";
    } else {
      return String.valueOf(playouts);
    }
  }

  /**
   * Truncate text that is too long for the given width
   *
   * @param line
   * @param fm
   * @param fitWidth
   * @return fitted
   */
  private static String truncateStringByWidth(String line, FontMetrics fm, int fitWidth) {
    if (line.isEmpty()) {
      return "";
    }
    int width = fm.stringWidth(line);
    if (width > fitWidth) {
      int guess = line.length() * fitWidth / width;
      String before = line.substring(0, guess).trim();
      width = fm.stringWidth(before);
      if (width > fitWidth) {
        int diff = width - fitWidth;
        int i = 0;
        for (; (diff > 0 && i < 5); i++) {
          diff = diff - fm.stringWidth(line.substring(guess - i - 1, guess - i));
        }
        return line.substring(0, guess - i).trim();
      } else {
        return before;
      }
    } else {
      return line;
    }
  }

  private GaussianFilter filter20 = new GaussianFilter(20);
  private GaussianFilter filter10 = new GaussianFilter(10);

  /** Display the controls */
  void drawControls() {
    userAlreadyKnowsAboutCommandString = true;

    cachedImage = new BufferedImage(getWidth(), getHeight(), TYPE_INT_ARGB);

    // redraw background
    //    createBackground();

    List<String> commandsToShow = new ArrayList<>(Arrays.asList(commands));
    if (Lizzie.leelaz.getDynamicKomi().isPresent()) {
      commandsToShow.add(resourceBundle.getString("LizzieFrame.commands.keyD"));
    }

    Graphics2D g = cachedImage.createGraphics();

    int maxSize = min(getWidth(), getHeight());
    int fontSize = (int) (maxSize * min(0.034, 0.80 / commandsToShow.size()));
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, fontSize);
    g.setFont(font);

    FontMetrics metrics = g.getFontMetrics(font);
    int maxCmdWidth = commandsToShow.stream().mapToInt(c -> metrics.stringWidth(c)).max().orElse(0);
    int lineHeight = (int) (font.getSize() * 1.15);

    int boxWidth = min((int) (maxCmdWidth * 1.4), getWidth());
    int boxHeight =
        min(commandsToShow.size() * lineHeight, getHeight() - getInsets().top - getInsets().bottom);

    int commandsX = min(getWidth() / 2 - boxWidth / 2, getWidth());
    int top = this.getInsets().top;
    int commandsY = top + min((getHeight() - top) / 2 - boxHeight / 2, getHeight() - top);

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

    //    refreshBackground();
  }

  private boolean userAlreadyKnowsAboutCommandString = false;

  private void drawCommandString(Graphics2D g) {
    if (userAlreadyKnowsAboutCommandString) return;

    int maxSize = (int) (min(getWidth(), getHeight()) * 0.98);

    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, (int) (maxSize * 0.03));
    String commandString = resourceBundle.getString("LizzieFrame.prompt.showControlsHint");
    int strokeRadius = Lizzie.config.showBorder ? 2 : 0;

    int showCommandsHeight = (int) (font.getSize() * 1.1);
    int showCommandsWidth = g.getFontMetrics(font).stringWidth(commandString) + 4 * strokeRadius;
    int showCommandsX = this.getInsets().left;
    int showCommandsY = getHeight() - showCommandsHeight - this.getInsets().bottom;
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

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {
    // Check for board click
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (Lizzie.board.inAnalysisMode()) Lizzie.board.toggleAnalysis();
      if (!isPlayingAgainstLeelaz || (playerIsBlack == Lizzie.board.getData().blackToPlay))
        Lizzie.board.place(coords[0], coords[1]);
      //      repaint();
      //      owner.updateStatus();
    }
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

  private final Consumer<String> placeVariation =
      v -> Board.asCoordinates(v).ifPresent(c -> Lizzie.board.place(c[0], c[1]));

  public boolean playCurrentVariation() {
    boardRenderer.variationOpt.ifPresent(vs -> vs.forEach(placeVariation));
    return boardRenderer.variationOpt.isPresent();
  }

  public void playBestMove() {
    boardRenderer.bestMoveCoordinateName().ifPresent(placeVariation);
  }

  public boolean isMouseOver(int x, int y) {
    return mouseOverCoordinate[0] == x && mouseOverCoordinate[1] == y;
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

  public double lastWinrateDiff(BoardHistoryNode node) {

    // Last winrate
    Optional<BoardData> lastNode = node.previous().flatMap(n -> Optional.of(n.getData()));
    boolean validLastWinrate = lastNode.map(d -> d.getPlayouts() > 0).orElse(false);
    double lastWR = validLastWinrate ? lastNode.get().winrate : 50;

    // Current winrate
    BoardData data = node.getData();
    boolean validWinrate = false;
    double curWR = 50;
    if (data == Lizzie.board.getHistory().getData()) {
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
      curWR = stats.maxWinrate;
      validWinrate = (stats.totalPlayouts > 0);
      if (isPlayingAgainstLeelaz
          && playerIsBlack == !Lizzie.board.getHistory().getData().blackToPlay) {
        validWinrate = false;
      }
    } else {
      validWinrate = (data.getPlayouts() > 0);
      curWR = validWinrate ? data.winrate : 100 - lastWR;
    }

    // Last move difference winrate
    if (validLastWinrate && validWinrate) {
      return 100 - lastWR - curWR;
    } else {
      return 0;
    }
  }

  public Color getBlunderNodeColor(BoardHistoryNode node) {
    if (Lizzie.config.nodeColorMode == 1 && node.getData().blackToPlay
        || Lizzie.config.nodeColorMode == 2 && !node.getData().blackToPlay) {
      return Color.WHITE;
    }
    double diffWinrate = lastWinrateDiff(node);
    Optional<Double> st =
        diffWinrate >= 0
            ? Lizzie.config.blunderWinrateThresholds.flatMap(
                l -> l.stream().filter(t -> (t > 0 && t <= diffWinrate)).reduce((f, s) -> s))
            : Lizzie.config.blunderWinrateThresholds.flatMap(
                l -> l.stream().filter(t -> (t < 0 && t >= diffWinrate)).reduce((f, s) -> f));
    if (st.isPresent()) {
      return Lizzie.config.blunderNodeColors.map(m -> m.get(st.get())).get();
    } else {
      return Color.WHITE;
    }
  }

  public void replayBranch() {
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
            if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
          }
        };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  public void updateStatus() {
    owner.updateStatus();
  }
}
