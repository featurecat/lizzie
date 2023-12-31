package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jhlabs.image.GaussianFilter;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.util.Utils;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONObject;

/** The window used to display the game. */
public class BoardPane extends LizziePane {
  private static final ResourceBundle resourceBundle = MainFrame.resourceBundle;

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
    // scoreMode has no keyboard binding in 0.7.4.
    // resourceBundle.getString("LizzieFrame.commands.keyPeriod"),
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
    // This is keyK actually in 0.7.4.
    // resourceBundle.getString("LizzieFrame.commands.keyE"),
  };

  private static BoardRenderer boardRenderer;

  //  private final BufferStrategy bs;
  private static boolean started = false;

  private static final int[] outOfBoundCoordinate = new int[] {-1, -1};
  public int[] mouseOverCoordinate = outOfBoundCoordinate;

  private long lastAutosaveTime = System.currentTimeMillis();
  private boolean isReplayVariation = false;
  private boolean isPonderingBeforeReplayVariation = false;

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
            if (e.isAltDown() && e.getButton() == MouseEvent.BUTTON1) {
              owner.input.startSettingRegionOfInterest(e);
              Lizzie.frame.refresh();
              return;
            }
            if (e.getButton() == MouseEvent.BUTTON1) { // left click
              if (e.getClickCount() == 2) { // TODO: Maybe need to delay check
                onDoubleClicked(e.getX(), e.getY());
              } else {
                onClicked(e.getX(), e.getY());
              }
            } else if (e.getButton() == MouseEvent.BUTTON3) { // right click
              // if (Lizzie.frame.isMouseOver) {
              //  Lizzie.frame.addSuggestionAsBranch();
              // } else {
              Lizzie.frame.onRightClicked(e.getX(), e.getY());
              // }
            }
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            owner.input.mouseReleased(e);
          }

          @Override
          public void mouseExited(MouseEvent e) {
            onMouseExited(e.getX(), e.getY());
          }
        });

    addMouseMotionListener(
        new MouseMotionListener() {
          @Override
          public void mouseMoved(MouseEvent e) {
            onMouseMoved(e.getX(), e.getY());
            if (Lizzie.config.showSubBoard) {
              Lizzie.frame.clearIsMouseOverSub();
            }
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            owner.input.dragRegionOfInterest(e);
          }
        });
  }

  protected void checkRightClick(MouseEvent e) {
    // cancel default behavior of LizziePane
  }

  /** Clears related status from empty board. */
  public void clear() {
    Utils.mustBeEventDispatchThread();
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
    Utils.mustBeEventDispatchThread();
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
                Lizzie.config.showCoordinates,
                true);
      }
      boardRenderer.setBoardParam(boardParams);
      try {
        boardRenderer.draw(g);
      } catch (Exception ex) {
      }

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
    Utils.mustBeEventDispatchThread();
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
      repaint(); // for #772
      owner.updateStatus();
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

  public void clearMoved() {
    isReplayVariation = false;
    if (Lizzie.frame != null && Lizzie.frame.isMouseOver) {
      Lizzie.frame.isMouseOver = false;
      boardRenderer.startNormalBoard();
    }
  }

  public void onMouseExited(int x, int y) {
    if (Lizzie.frame != null && Lizzie.frame.isShowingRightMenu) return;
    mouseOverCoordinate = outOfBoundCoordinate;
    clearMoved();
  }

  public void onMouseMoved(int x, int y) {
    if (Lizzie.frame != null && Lizzie.frame.isShowingRightMenu) return;
    mouseOverCoordinate = outOfBoundCoordinate;
    Optional<int[]> coords = boardRenderer.convertScreenToCoordinates(x, y);
    coords
        .filter(c -> !isMouseOver(c[0], c[1]))
        .ifPresent(
            c -> {
              clearMoved();
              repaint();
            });
    coords.ifPresent(
        c -> {
          mouseOverCoordinate = c;
          if (Lizzie.frame != null) {
            Lizzie.frame.isMouseOver = boardRenderer.isShowingBranch();
          }
        });
    if (!coords.isPresent() && boardRenderer.isShowingBranch()) {
      clearMoved();
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

  public void setDisplayedBranchLength(int n) {
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

  public void doBranch(int moveTo) {
    boardRenderer.doBranch(moveTo);
  }

  public void addSuggestionAsBranch() {
    boardRenderer.addSuggestionAsBranch();
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

  private String gifPath = null;

  public void replayBranch(boolean generateGif) {
    if (isReplayVariation) return;
    int replaySteps = boardRenderer.getReplayBranch();
    if (replaySteps <= 0) return; // Bad steps or no branch
    List<BufferedImage> frames = new ArrayList<BufferedImage>();
    gifPath = null;
    if (generateGif) {
      FileNameExtensionFilter filter = new FileNameExtensionFilter("*.gif", "GIF");
      JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
      JFileChooser chooser = new JFileChooser(filesystem.getString("last-image-folder"));
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileFilter(filter);
      chooser.setMultiSelectionEnabled(false);
      int result = chooser.showSaveDialog(Lizzie.frame);
      if (result == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        filesystem.put("last-image-folder", file.getParent());
        if (file.exists()) {
          int ret =
              JOptionPane.showConfirmDialog(
                  Lizzie.frame,
                  resourceBundle.getString("LizzieFrame.prompt.fileExists"),
                  "Warning",
                  JOptionPane.OK_CANCEL_OPTION);
          if (ret == JOptionPane.YES_OPTION) {
            gifPath = file.getAbsolutePath() + (!file.getPath().endsWith(".gif") ? ".gif" : "");
          }
        } else {
          gifPath = file.getAbsolutePath() + (!file.getPath().endsWith(".gif") ? ".gif" : "");
        }
      }
    }
    int width = this.getWidth();
    int height = this.getHeight();
    int oriBranchLength = boardRenderer.getDisplayedBranchLength();
    isReplayVariation = true;
    isPonderingBeforeReplayVariation = Lizzie.leelaz.isPondering();
    if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
    Runnable runnable =
        new Runnable() {
          public void run() {
            int secs = (int) (Lizzie.config.replayBranchIntervalSeconds * 1000);
            for (int i = 1; i < replaySteps + 1; i++) {
              if (!isReplayVariation) break;
              setDisplayedBranchLength(i);
              repaint();
              if (gifPath != null) {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D cg = img.createGraphics();
                paintAll(cg);
                frames.add(img);
              }
              try {
                Thread.sleep(secs);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
            if (gifPath != null) {
              Utils.toGif(
                  gifPath, frames, (int) (Lizzie.config.replayBranchIntervalSeconds * 100), true);
            }
            boardRenderer.setDisplayedBranchLength(oriBranchLength);
            isReplayVariation = false;
            if (isPonderingBeforeReplayVariation && !Lizzie.leelaz.isPondering())
              Lizzie.leelaz.togglePonder();
          }
        };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  public void updateStatus() {
    owner.updateStatus();
  }

  public void removeEstimateRect() {
    boardRenderer.removeEstimateRect();
  }

  public void drawEstimateRect(ArrayList<Double> estimateArray, boolean isZen) {
    boardRenderer.drawEstimateRect(estimateArray, isZen);
  }

  public void resetImages() {
    boardRenderer.resetImages();
  }

  public void saveImage() {
    JSONObject filesystem = Lizzie.config.persisted.getJSONObject("filesystem");
    JFileChooser chooser = new JFileChooser(filesystem.getString("last-image-folder"));
    chooser.setAcceptAllFileFilterUsed(false);
    //    String writerNames[] = ImageIO.getWriterFormatNames();
    FileNameExtensionFilter filter1 = new FileNameExtensionFilter("*.png", "PNG");
    FileNameExtensionFilter filter2 = new FileNameExtensionFilter("*.jpg", "JPG", "JPEG");
    FileNameExtensionFilter filter3 = new FileNameExtensionFilter("*.gif", "GIF");
    FileNameExtensionFilter filter4 = new FileNameExtensionFilter("*.bmp", "BMP");
    chooser.addChoosableFileFilter(filter1);
    chooser.addChoosableFileFilter(filter2);
    chooser.addChoosableFileFilter(filter3);
    chooser.addChoosableFileFilter(filter4);
    chooser.setMultiSelectionEnabled(false);
    int result = chooser.showSaveDialog(Lizzie.frame);
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      filesystem.put("last-image-folder", file.getParent());
      String ext =
          chooser.getFileFilter() instanceof FileNameExtensionFilter
              ? ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0].toLowerCase()
              : "";
      if (!Utils.isBlank(ext)) {
        if (!chooser.getFileFilter().accept(file)) {
          file = new File(file.getPath() + "." + ext);
        }
      }
      if (file.exists()) {
        int ret =
            JOptionPane.showConfirmDialog(
                Lizzie.frame,
                resourceBundle.getString("LizzieFrame.prompt.fileExists"),
                "Warning",
                JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      // Never use ARGB here for supporting JPG!
      // (cf.)
      // https://stackoverflow.com/questions/57673051/writing-jpg-or-jpeg-image-with-imageio-write-does-not-create-image-file
      BufferedImage bImg =
          new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D cg = bImg.createGraphics();
      paintAll(cg);
      try {
        boolean supported = ImageIO.write(bImg, ext, file);
        if (!supported) {
          String displayedMessage =
              String.format("Failed to save \"%s\".\n(unsupported image format?)", file.getName());
          JOptionPane.showMessageDialog(
              Lizzie.frame, displayedMessage, "Lizzie - Error!", JOptionPane.ERROR_MESSAGE);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public Optional<int[]> convertScreenToCoordinates(int x, int y) {
    return boardRenderer.convertScreenToCoordinates(x, y);
  }
}
