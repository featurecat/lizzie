package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.SGFParser;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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
import java.awt.event.MouseMotionListener;
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

  //  private final BufferStrategy bs;
  private static boolean started = false;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;

  LizzieMain owner;
  /** Creates a window */
  public BoardPane(LizzieMain owner) {
    super(owner);
    this.owner = owner;

    boardRenderer = new BoardRenderer(true);

    //    createBufferStrategy(2);
    //    bs = getBufferStrategy();

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) { // left click
              if (e.getClickCount() == 2) { // TODO: Maybe need to delay check
                onDoubleClicked(e.getX(), e.getY());
              } else {
                onClicked(e.getX(), e.getY());
              }
            } else if (e.getButton() == MouseEvent.BUTTON3) { // right click
              Input.undo();
            }
          }
        });
    addMouseMotionListener(
        new MouseMotionListener() {
          @Override
          public void mouseMoved(MouseEvent e) {
            onMouseMoved(e.getX(), e.getY());
          }

          @Override
          public void mouseDragged(MouseEvent e) {}
        });
  }

  /** Clears related status from empty board. */
  public void clear() {
    if (LizzieMain.winratePane != null) {
      LizzieMain.winratePane.clear();
    }
    started = false;
    owner.updateStatus();
  }

  private BufferedImage cachedImage;

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

    if (!owner.showControls) {
      // layout parameters
      // initialize
      cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
      Graphics2D g = (Graphics2D) cachedImage.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      boardRenderer.setLocation(0, 0);
      if (boardParams == null) {
        boardParams =
            boardRenderer.availableLength(
                max(width, Board.boardWidth + 5),
                max(height, Board.boardHeight + 5),
                Lizzie.config.showCoordinates);
      }
      boardRenderer.setBoardParam(boardParams);
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
    Graphics2D bsGraphics = (Graphics2D) g0; // bs.getDrawGraphics();
    bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    //    bsGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
    //        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    bsGraphics.drawImage(cachedImage, 0, 0, null);

    // cleanup
    bsGraphics.dispose();
    //    bs.show();
  }

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
        owner.cachedBackground.getSubimage(commandsX, commandsY, boxWidth, boxHeight), result);
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
      if (!owner.isPlayingAgainstLeelaz
          || (owner.playerIsBlack == Lizzie.board.getData().blackToPlay))
        Lizzie.board.place(coords[0], coords[1]);
      //      repaint();
      //      owner.updateStatus();
    }
  }

  public void onDoubleClicked(int x, int y) {
    // Check for board double click
    Optional<int[]> boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      int[] coords = boardCoordinates.get();
      if (!owner.isPlayingAgainstLeelaz) {
        int moveNumber = Lizzie.board.moveNumberByCoord(coords);
        if (moveNumber > 0) {
          Lizzie.board.goToMoveNumberBeyondBranch(moveNumber);
        }
      }
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
            if (!Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
          }
        };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  public void updateStatus() {
    owner.updateStatus();
  }
}
