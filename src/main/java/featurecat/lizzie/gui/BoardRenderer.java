package featurecat.lizzie.gui;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Branch;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.rules.Stone;
import featurecat.lizzie.rules.Zobrist;
import featurecat.lizzie.util.Utils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BoardRenderer {
  // Percentage of the boardLength to offset before drawing black lines
  private static final double MARGIN = 0.03;
  private static final double MARGIN_WITH_COORDINATES = 0.06;
  private static final double STARPOINT_DIAMETER = 0.015;
  private static final BufferedImage emptyImage = new BufferedImage(1, 1, TYPE_INT_ARGB);

  private static boolean emptyName = false;
  private static boolean changedName = false;

  private int x, y;
  private int boardWidth, boardHeight;
  private int shadowRadius;

  private JSONObject uiConfig, uiPersist;
  private int scaledMarginWidth, availableWidth, squareWidth, stoneRadius;
  private int scaledMarginHeight, availableHeight, squareHeight;
  private Optional<Branch> branchOpt = Optional.empty();
  private List<MoveData> bestMoves;

  private BufferedImage cachedBackgroundImage = emptyImage;
  private boolean cachedBackgroundImageHasCoordinatesEnabled = false;
  private int cachedX, cachedY;
  private int cachedBoardWidth = 0, cachedBoardHeight = 0;
  private BufferedImage cachedStonesImage = emptyImage;
  private BufferedImage cachedBoardImage = emptyImage;
  private BufferedImage cachedWallpaperImage = emptyImage;
  private BufferedImage cachedStonesShadowImage = emptyImage;
  private Zobrist cachedZhash = new Zobrist(); // defaults to an empty board

  private BufferedImage cachedBlackStoneImage = emptyImage;
  private BufferedImage cachedWhiteStoneImage = emptyImage;

  private BufferedImage branchStonesImage = emptyImage;
  private BufferedImage branchStonesShadowImage;
  private BufferedImage cachedEstimateLargeRectImage = emptyImage;
  private BufferedImage cachedEstimateSmallRectImage = emptyImage;

  private boolean lastInScoreMode = false;

  public Optional<List<String>> variationOpt;

  // special values of displayedBranchLength
  public static final int SHOW_RAW_BOARD = -1;
  public static final int SHOW_NORMAL_BOARD = -2;

  private int displayedBranchLength = SHOW_NORMAL_BOARD;
  private int cachedDisplayedBranchLength = SHOW_RAW_BOARD;
  private boolean showingBranch = false;
  private boolean isMainBoard = false;

  private int maxAlpha = 240;

  // Computed in drawLeelazSuggestionsBackground and stored for
  // display in drawLeelazSuggestionsForeground
  private class TextData {
    MoveData move;
    int suggestionX;
    int suggestionY;
    boolean flipWinrate;
  }

  public BoardRenderer(boolean isMainBoard) {
    uiConfig = Lizzie.config.uiConfig;
    uiPersist = Lizzie.config.persisted.getJSONObject("ui-persist");
    try {
      maxAlpha = uiPersist.getInt("max-alpha");
    } catch (JSONException e) {
    }
    this.isMainBoard = isMainBoard;
  }

  /** Draw a go board */
  public void draw(Graphics2D g) {
    //    setupSizeParameters();

    //        Stopwatch timer = new Stopwatch();
    drawGoban(g);
    if (Lizzie.config.showNameInBoard && isMainBoard) drawName(g);
    //        timer.lap("background");
    drawStones();
    //        timer.lap("stones");
    if (Lizzie.board != null && Lizzie.board.inScoreMode() && isMainBoard) {
      drawScore(g);
    } else {
      drawBranch();
    }
    //        timer.lap("branch");

    renderImages(g);
    //        timer.lap("rendering images");

    if (!isMainBoard) {
      if (Lizzie.config.showBranchNow()) {
        drawMoveNumbers(g);
      }
      return;
    }

    if (!isShowingRawBoard()) {
      drawMoveNumbers(g);
      //        timer.lap("movenumbers");
      List<TextData> textDatas = new ArrayList<>();
      if (Lizzie.frame.isShowingPolicy) drawPolicy(g);
      else if (!Lizzie.frame.isPlayingAgainstLeelaz && Lizzie.config.showBestMovesNow())
        drawLeelazSuggestionsBackground(g, textDatas);

      if (Lizzie.config.showNextMoves) {
        drawNextMoves(g);
      }

      if (!Lizzie.frame.isShowingPolicy
          && !Lizzie.frame.isPlayingAgainstLeelaz
          && Lizzie.config.showBestMovesNow()) drawLeelazSuggestionsForeground(g, textDatas);

      drawStoneMarkup(g);
    }

    //        timer.lap("leelaz");

    //        timer.print();
  }

  /**
   * Return the best move of Leelaz's suggestions
   *
   * @return the optional coordinate name of the best move
   */
  public Optional<String> bestMoveCoordinateName() {
    return bestMoves.isEmpty() ? Optional.empty() : Optional.of(bestMoves.get(0).coordinate);
  }

  /** Calculate good values for boardLength, scaledMargin, availableLength, and squareLength */
  public static int[] availableLength(
      int boardWidth, int boardHeight, boolean showCoordinates, boolean isMainBoard) {
    int[] calculatedPixelMargins =
        calculatePixelMargins(boardWidth, boardHeight, showCoordinates, isMainBoard);
    return (calculatedPixelMargins != null && calculatedPixelMargins.length >= 6)
        ? calculatedPixelMargins
        : new int[] {boardWidth, 0, boardWidth, boardHeight, 0, boardHeight};
  }

  /** Calculate good values for boardLength, scaledMargin, availableLength, and squareLength */
  public void setupSizeParameters() {
    int boardWidth0 = boardWidth;
    int boardHeight0 = boardHeight;

    int[] calculatedPixelMargins = calculatePixelMargins();
    boardWidth = calculatedPixelMargins[0];
    scaledMarginWidth = calculatedPixelMargins[1];
    availableWidth = calculatedPixelMargins[2];
    boardHeight = calculatedPixelMargins[3];
    scaledMarginHeight = calculatedPixelMargins[4];
    availableHeight = calculatedPixelMargins[5];

    squareWidth = calculateSquareWidth(availableWidth);
    squareHeight = calculateSquareHeight(availableHeight);
    if (squareWidth > squareHeight) {
      squareWidth = squareHeight;
      int newWidth = squareWidth * (Board.boardWidth - 1) + 1;
      int diff = availableWidth - newWidth;
      availableWidth = newWidth;
      boardWidth -= diff + (scaledMarginWidth - scaledMarginHeight) * 2;
      scaledMarginWidth = scaledMarginHeight;
    } else if (squareWidth < squareHeight) {
      squareHeight = squareWidth;
      int newHeight = squareHeight * (Board.boardHeight - 1) + 1;
      int diff = availableHeight - newHeight;
      availableHeight = newHeight;
      boardHeight -= diff + (scaledMarginHeight - scaledMarginWidth) * 2;
      scaledMarginHeight = scaledMarginWidth;
    }
    stoneRadius = max(squareWidth, squareHeight) < 4 ? 1 : max(squareWidth, squareHeight) / 2 - 1;

    // re-center board
    setLocation(x + (boardWidth0 - boardWidth) / 2, y + (boardHeight0 - boardHeight) / 2);
  }

  /**
   * Draw the green background and go board with lines. We cache the image for a performance boost.
   */
  private void drawGoban(Graphics2D g0) {
    int width = Lizzie.frame.getWidth();
    int height = Lizzie.frame.getHeight();

    // Draw the cached background image if frame size changes
    if (cachedBackgroundImage.getWidth() != width
        || cachedBackgroundImage.getHeight() != height
        || cachedBoardWidth != boardWidth
        || cachedBoardHeight != boardHeight
        || cachedX != x
        || cachedY != y
        || cachedBackgroundImageHasCoordinatesEnabled != showCoordinates()
        || (changedName && isMainBoard)
        || Lizzie.frame.isForceRefresh()) {
      changedName = false;
      cachedBoardWidth = boardWidth;
      cachedBoardHeight = boardHeight;
      Lizzie.frame.setForceRefresh(false);

      cachedBackgroundImage = new BufferedImage(width, height, TYPE_INT_ARGB);
      Graphics2D g = cachedBackgroundImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      // Draw the wooden background
      drawWoodenBoard(g);

      // Draw the lines
      g.setColor(Color.BLACK);
      for (int i = 0; i < Board.boardHeight; i++) {
        g.drawLine(
            x + scaledMarginWidth,
            y + scaledMarginHeight + squareHeight * i,
            x + scaledMarginWidth + availableWidth - 1,
            y + scaledMarginHeight + squareHeight * i);
      }
      for (int i = 0; i < Board.boardWidth; i++) {
        g.drawLine(
            x + scaledMarginWidth + squareWidth * i,
            y + scaledMarginHeight,
            x + scaledMarginWidth + squareWidth * i,
            y + scaledMarginHeight + availableHeight - 1);
      }

      // Draw the star points
      drawStarPoints(g);

      // Draw coordinates if enabled
      if (showCoordinates()) {
        g.setColor(Color.BLACK);
        for (int i = 0; i < Board.boardWidth; i++) {
          drawString(
              g,
              x + scaledMarginWidth + squareWidth * i,
              y + scaledMarginHeight / 3,
              MainFrame.uiFont,
              Board.asName(i),
              stoneRadius * 4 / 5,
              stoneRadius);
          if (!Lizzie.config.showNameInBoard
              || Lizzie.board != null
                  && (Lizzie.board.getHistory().getGameInfo().getPlayerWhite().equals("")
                      && Lizzie.board.getHistory().getGameInfo().getPlayerBlack().equals(""))) {
            drawString(
                g,
                x + scaledMarginWidth + squareWidth * i,
                y - scaledMarginHeight / 3 + boardHeight,
                MainFrame.uiFont,
                Board.asName(i),
                stoneRadius * 4 / 5,
                stoneRadius);
          }
        }
        for (int i = 0; i < Board.boardHeight; i++) {
          drawString(
              g,
              x + scaledMarginWidth / 3,
              y + scaledMarginHeight + squareHeight * i,
              MainFrame.uiFont,
              "" + (Board.boardHeight <= 25 ? (Board.boardHeight - i) : (i + 1)),
              stoneRadius * 4 / 5,
              stoneRadius);
          drawString(
              g,
              x - scaledMarginWidth / 3 + boardWidth,
              y + scaledMarginHeight + squareHeight * i,
              MainFrame.uiFont,
              "" + (Board.boardHeight <= 25 ? (Board.boardHeight - i) : (i + 1)),
              stoneRadius * 4 / 5,
              stoneRadius);
        }
      }
      g.dispose();
    }

    g0.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
    g0.drawImage(cachedBackgroundImage, 0, 0, null);
    cachedX = x;
    cachedY = y;
  }

  private void drawName(Graphics2D g0) {
    if (Lizzie.board == null) {
      return;
    }
    g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    String black = Lizzie.board.getHistory().getGameInfo().getPlayerBlack();
    if (black.length() > 20) black = black.substring(0, 20);
    String white = Lizzie.board.getHistory().getGameInfo().getPlayerWhite();
    if (white.length() > 20) white = white.substring(0, 20);
    if (black.equals("") && white.contentEquals("")) {
      if (!emptyName) {
        emptyName = true;
        changedName = true;
      }
      return;
    }
    if (emptyName) {
      emptyName = false;
      changedName = true;
    }
    emptyName = false;
    if (Lizzie.board.getHistory().isBlacksTurn()) {
      g0.setColor(Color.WHITE);
      g0.fillOval(
          x + boardWidth / 2 - stoneRadius * 1 / 5,
          y - scaledMarginHeight + stoneRadius + boardHeight,
          stoneRadius,
          stoneRadius);

      g0.setColor(Color.BLACK);
      g0.fillOval(
          x + boardWidth / 2 - stoneRadius * 4 / 5,
          y - scaledMarginHeight + stoneRadius + boardHeight,
          stoneRadius,
          stoneRadius);
    } else {
      g0.setColor(Color.BLACK);
      g0.fillOval(
          x + boardWidth / 2 - stoneRadius * 4 / 5,
          y - scaledMarginHeight + stoneRadius + boardHeight,
          stoneRadius,
          stoneRadius);
      g0.setColor(Color.WHITE);
      g0.fillOval(
          x + boardWidth / 2 - stoneRadius * 1 / 5,
          y - scaledMarginHeight + stoneRadius + boardHeight,
          stoneRadius,
          stoneRadius);
    }
    g0.setColor(Color.BLACK);
    String regex = "[\u4e00-\u9fa5]";

    drawStringBold(
        g0,
        x
            + boardWidth / 2
            - black.replaceAll(regex, "12").length() * stoneRadius / 4
            - stoneRadius * 5 / 4,
        y - scaledMarginHeight + stoneRadius + boardHeight + stoneRadius * 3 / 5,
        Lizzie.frame.uiFont,
        black,
        stoneRadius,
        stoneRadius * black.replaceAll(regex, "12").length() / 2);
    g0.setColor(Color.WHITE);
    drawStringBold(
        g0,
        x
            + boardWidth / 2
            + white.replaceAll(regex, "12").length() * stoneRadius / 4
            + stoneRadius * 5 / 4,
        y - scaledMarginHeight + stoneRadius + boardHeight + stoneRadius * 3 / 5,
        Lizzie.frame.uiFont,
        white,
        stoneRadius,
        stoneRadius * white.replaceAll(regex, "12").length() / 2);
  }

  /**
   * Draws the star points on the board, according to board size
   *
   * @param g graphics2d object to draw
   */
  private void drawStarPoints(Graphics2D g) {
    if (Board.boardWidth == 19 && Board.boardHeight == 19) {
      drawStarPoints0(3, 3, 6, false, g);
    } else if (Board.boardWidth == 13 && Board.boardHeight == 13) {
      drawStarPoints0(2, 3, 6, true, g);
    } else if (Board.boardWidth == 9 && Board.boardHeight == 9) {
      drawStarPoints0(2, 2, 4, true, g);
    } else if (Board.boardWidth == 7 && Board.boardHeight == 7) {
      drawStarPoints0(2, 2, 2, true, g);
    } else if (Board.boardWidth == 5 && Board.boardHeight == 5) {
      drawStarPoints0(0, 0, 2, true, g);
    }
  }

  private void drawStarPoints0(
      int nStarpoints, int edgeOffset, int gridDistance, boolean center, Graphics2D g) {
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    int starPointRadius = (int) (STARPOINT_DIAMETER * min(boardWidth, boardHeight)) / 2;
    for (int i = 0; i < nStarpoints; i++) {
      for (int j = 0; j < nStarpoints; j++) {
        int centerX = x + scaledMarginWidth + squareWidth * (edgeOffset + gridDistance * i);
        int centerY = y + scaledMarginHeight + squareHeight * (edgeOffset + gridDistance * j);
        fillCircle(g, centerX, centerY, starPointRadius);
      }
    }

    if (center) {
      int centerX = x + scaledMarginWidth + squareWidth * gridDistance;
      int centerY = y + scaledMarginHeight + squareHeight * gridDistance;
      fillCircle(g, centerX, centerY, starPointRadius);
    }
  }

  /** Draw the stones. We cache the image for a performance boost. */
  private void drawStones() {
    if (Lizzie.board == null) return;

    // draw a new image if frame size changes or board state changes
    if (cachedStonesImage.getWidth() != boardWidth
        || cachedStonesImage.getHeight() != boardHeight
        || cachedDisplayedBranchLength != displayedBranchLength
        || cachedBackgroundImageHasCoordinatesEnabled != showCoordinates()
        || !cachedZhash.equals(Lizzie.board.getData().zobrist)
        || Lizzie.board.inScoreMode()
        || lastInScoreMode) {

      cachedStonesImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
      cachedStonesShadowImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
      Graphics2D g = cachedStonesImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      Graphics2D gShadow = cachedStonesShadowImage.createGraphics();
      gShadow.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      // we need antialiasing to make the stones pretty. Java is a bit slow at antialiasing; that's
      // why we want the cache
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
      gShadow.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

      for (int i = 0; i < Board.boardWidth; i++) {
        for (int j = 0; j < Board.boardHeight; j++) {
          int stoneX = scaledMarginWidth + squareWidth * i;
          int stoneY = scaledMarginHeight + squareHeight * j;
          drawStone(
              g, gShadow, stoneX, stoneY, Lizzie.board.getStones()[Board.getIndex(i, j)], i, j);
        }
      }

      cachedZhash = Lizzie.board.getData().zobrist.clone();
      cachedDisplayedBranchLength = displayedBranchLength;
      cachedBackgroundImageHasCoordinatesEnabled = showCoordinates();
      g.dispose();
      gShadow.dispose();
      lastInScoreMode = false;
    }
    if (Lizzie.board.inScoreMode()) lastInScoreMode = true;
  }

  /*
   * Draw a white/black dot on territory and captured stones. Dame is drawn as red dot.
   */
  private void drawScore(Graphics2D go) {
    Graphics2D g = cachedStonesImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    Stone scorestones[] = Lizzie.board.scoreStones();
    int scoreRadius = stoneRadius / 4;
    for (int i = 0; i < Board.boardWidth; i++) {
      for (int j = 0; j < Board.boardHeight; j++) {
        int stoneX = scaledMarginWidth + squareWidth * i;
        int stoneY = scaledMarginHeight + squareHeight * j;
        switch (scorestones[Board.getIndex(i, j)]) {
          case WHITE_POINT:
          case BLACK_CAPTURED:
            g.setColor(Color.white);
            fillCircle(g, stoneX, stoneY, scoreRadius);
            break;
          case BLACK_POINT:
          case WHITE_CAPTURED:
            g.setColor(Color.black);
            fillCircle(g, stoneX, stoneY, scoreRadius);
            break;
          case DAME:
            g.setColor(Color.red);
            fillCircle(g, stoneX, stoneY, scoreRadius);
            break;
        }
      }
    }
    g.dispose();
  }

  /** Draw the 'ghost stones' which show a variationOpt Leelaz is thinking about */
  private void drawBranch() {
    showingBranch = false;
    branchStonesImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
    branchStonesShadowImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
    branchOpt = Optional.empty();

    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      return;
    }

    // Leela Zero isn't connected yet
    if (Lizzie.leelaz == null) return;

    // calculate best moves and branch
    bestMoves = Lizzie.leelaz.getBestMoves();
    if (Lizzie.config.showBestMovesByHold
        && MoveData.getPlayouts(bestMoves) < Lizzie.board.getData().getPlayouts()) {
      bestMoves = Lizzie.board.getData().bestMoves;
    }

    variationOpt = Optional.empty();

    if (isMainBoard && (isShowingRawBoard() || !Lizzie.config.showBranchNow())) {
      return;
    }

    Graphics2D g = (Graphics2D) branchStonesImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    Graphics2D gShadow = (Graphics2D) branchStonesShadowImage.getGraphics();
    gShadow.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    Optional<MoveData> suggestedMove = (isMainBoard ? mouseOveredMove() : getBestMove());
    if (!suggestedMove.isPresent()
        || (!isMainBoard && Lizzie.frame.isAutoEstimating)
        || (isMainBoard && Lizzie.frame.isShowingPolicy)) {
      return;
    }
    List<String> variation = suggestedMove.get().variation;
    Branch branch = new Branch(Lizzie.board, variation, displayedBranchLength);
    branchOpt = Optional.of(branch);
    variationOpt = Optional.of(variation);
    showingBranch = true;

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

    for (int i = 0; i < Board.boardWidth; i++) {
      for (int j = 0; j < Board.boardHeight; j++) {
        // Display latest stone for ghost dead stone
        int index = Board.getIndex(i, j);
        Stone stone = branch.data.stones[index];
        boolean isGhost = (stone == Stone.BLACK_GHOST || stone == Stone.WHITE_GHOST);
        if (Lizzie.board.getData().stones[index] != Stone.EMPTY && !isGhost) continue;
        if (branch.data.moveNumberList[index] > maxBranchMoves()) continue;

        int stoneX = scaledMarginWidth + squareWidth * i;
        int stoneY = scaledMarginHeight + squareHeight * j;

        drawStone(g, gShadow, stoneX, stoneY, stone.unGhosted(), i, j);
      }
    }

    g.dispose();
    gShadow.dispose();
  }

  public Optional<MoveData> mouseOveredMove() {
    return bestMoves
        .stream()
        .filter(
            move ->
                Board.asCoordinates(move.coordinate)
                    .map(c -> Lizzie.frame.isMouseOver(c[0], c[1]))
                    .orElse(false))
        .findFirst();
  }

  private Optional<MoveData> getBestMove() {
    return bestMoves.isEmpty() ? Optional.empty() : Optional.of(bestMoves.get(0));
  }

  /** Render the shadows and stones in correct background-foreground order */
  private void renderImages(Graphics2D g) {
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
    g.drawImage(cachedEstimateLargeRectImage, x, y, null);
    g.drawImage(cachedStonesShadowImage, x, y, null);
    if (Lizzie.config.showBranchNow()) {
      g.drawImage(branchStonesShadowImage, x, y, null);
    }
    g.drawImage(cachedStonesImage, x, y, null);
    if (Lizzie.config.showBranchNow()) {
      g.drawImage(branchStonesImage, x, y, null);
    }
    g.drawImage(cachedEstimateSmallRectImage, x, y, null);
  }

  /** Draw move numbers and/or mark the last played move */
  private void drawMoveNumbers(Graphics2D g) {
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    if (Lizzie.board == null) return;
    Board board = Lizzie.board;
    Optional<int[]> lastMoveOpt = branchOpt.map(b -> b.data.lastMove).orElse(board.getLastMove());
    if (Lizzie.config.allowMoveNumber == 0 && !branchOpt.isPresent()) {
      if (lastMoveOpt.isPresent()) {
        int[] lastMove = lastMoveOpt.get();

        // Mark the last coordinate
        int lastMoveMarkerRadius = stoneRadius / 2;
        int stoneX = x + scaledMarginWidth + squareWidth * lastMove[0];
        int stoneY = y + scaledMarginHeight + squareHeight * lastMove[1];

        // Set color to the opposite color of whatever is on the board
        boolean isWhite = board.getStones()[Board.getIndex(lastMove[0], lastMove[1])].isWhite();
        g.setColor(isWhite ? Color.BLACK : Color.WHITE);

        if (Lizzie.config.stoneIndicatorType == 2) {
          // Use a solid circle instead of
          fillCircle(g, stoneX, stoneY, (int) (lastMoveMarkerRadius * 0.65));
        } else if (Lizzie.config.stoneIndicatorType == 0) {
        } else {
          drawCircle(g, stoneX, stoneY, lastMoveMarkerRadius);
        }
      } else if (board.getData().moveNumber != 0 && !board.inScoreMode()) {
        g.setColor(
            board.getData().blackToPlay ? new Color(255, 255, 255, 150) : new Color(0, 0, 0, 150));
        g.fillOval(
            x + boardWidth / 2 - 4 * stoneRadius,
            y + boardHeight / 2 - 4 * stoneRadius,
            stoneRadius * 8,
            stoneRadius * 8);
        g.setColor(
            board.getData().blackToPlay ? new Color(0, 0, 0, 255) : new Color(255, 255, 255, 255));
        drawString(
            g,
            x + boardWidth / 2,
            y + boardHeight / 2,
            MainFrame.uiFont,
            "pass",
            stoneRadius * 4,
            stoneRadius * 6);
      }

      return;
    }

    int[] moveNumberList =
        branchOpt.map(b -> b.data.moveNumberList).orElse(board.getMoveNumberList());

    // Allow to display only last move number
    int lastMoveNumber =
        branchOpt
            .map(b -> b.data.moveNumber)
            .orElse(Arrays.stream(moveNumberList).max().getAsInt());

    for (int i = 0; i < Board.boardWidth; i++) {
      for (int j = 0; j < Board.boardHeight; j++) {
        int stoneX = x + scaledMarginWidth + squareWidth * i;
        int stoneY = y + scaledMarginHeight + squareHeight * j;
        int here = Board.getIndex(i, j);

        // Allow to display only last move number
        if (Lizzie.config.allowMoveNumber > -1
            && lastMoveNumber - moveNumberList[here] >= Lizzie.config.allowMoveNumber) {
          continue;
        }

        Stone stoneHere = branchOpt.map(b -> b.data.stones[here]).orElse(board.getStones()[here]);

        // don't write the move number if either: the move number is 0, or there will already be
        // playout information written
        if (moveNumberList[Board.getIndex(i, j)] > 0
            && (!branchOpt.isPresent() || !Lizzie.frame.isMouseOver(i, j))) {
          boolean reverse = (moveNumberList[Board.getIndex(i, j)] > maxBranchMoves());
          if (lastMoveOpt.isPresent() && lastMoveOpt.get()[0] == i && lastMoveOpt.get()[1] == j) {
            if (reverse) continue;
            g.setColor(Color.RED.brighter()); // stoneHere.isBlack() ? Color.RED.brighter() :
            // Color.BLUE.brighter());
          } else {
            // Draw white letters on black stones nomally.
            // But use black letters for showing black moves without stones.
            if (reverse) continue;
            g.setColor(stoneHere.isBlack() ^ reverse ? Color.WHITE : Color.BLACK);
          }

          String moveNumberString = moveNumberList[Board.getIndex(i, j)] + "";
          drawString(
              g,
              stoneX,
              stoneY,
              MainFrame.uiFont,
              moveNumberString,
              (float) (stoneRadius * 1.4),
              (int) (stoneRadius * 1.4));
        }
      }
    }
  }

  /**
   * Draw all of Leelaz's suggestions as colored stones and store statistics into textDatas for
   * future use by drawLeelazSuggestionsForeground
   */
  private void drawLeelazSuggestionsBackground(Graphics2D g, List<TextData> textDatas) {
    int minAlpha = 20;
    float winrateHueFactor = 0.9f;
    float alphaFactor = 5.0f;
    float redHue = Color.RGBtoHSB(255, 0, 0, null)[0];
    float greenHue = Color.RGBtoHSB(0, 255, 0, null)[0];
    float cyanHue = Color.RGBtoHSB(0, 255, 255, null)[0];

    if (bestMoves != null && !bestMoves.isEmpty()) {

      int maxPlayouts = 0;
      double maxWinrate = 0;
      double minWinrate = 100.0;
      for (MoveData move : bestMoves) {
        if (move.playouts > maxPlayouts) maxPlayouts = move.playouts;
        if (move.winrate > maxWinrate) maxWinrate = move.winrate;
        if (move.winrate < minWinrate) minWinrate = move.winrate;
      }

      for (int i = 0; i < Board.boardWidth; i++) {
        for (int j = 0; j < Board.boardHeight; j++) {
          Optional<MoveData> moveOpt = Optional.empty();

          // This is inefficient but it looks better with shadows
          for (MoveData m : bestMoves) {
            Optional<int[]> coord = Board.asCoordinates(m.coordinate);
            if (coord.isPresent()) {
              int[] c = coord.get();
              if (c[0] == i && c[1] == j) {
                moveOpt = Optional.of(m);
                break;
              }
            }
          }

          if (!moveOpt.isPresent()) {
            continue;
          }
          MoveData move = moveOpt.get();

          boolean isBestMove = bestMoves.get(0) == move;
          boolean hasMaxWinrate = move.winrate == maxWinrate;
          boolean flipWinrate =
              uiConfig.getBoolean("win-rate-always-black") && !Lizzie.board.getData().blackToPlay;

          if (move.playouts == 0) {
            continue; // This actually can happen
          }

          float percentPlayouts = (float) move.playouts / maxPlayouts;
          double percentWinrate =
              Math.min(
                  1,
                  Math.max(0.01, move.winrate - minWinrate)
                      / Math.max(0.01, maxWinrate - minWinrate));

          Optional<int[]> coordsOpt = Board.asCoordinates(move.coordinate);
          if (!coordsOpt.isPresent()) {
            continue;
          }
          int[] coords = coordsOpt.get();

          int suggestionX = x + scaledMarginWidth + squareWidth * coords[0];
          int suggestionY = y + scaledMarginHeight + squareHeight * coords[1];

          float hue;
          if (isBestMove && !Lizzie.config.colorByWinrateInsteadOfVisits
              || hasMaxWinrate && Lizzie.config.colorByWinrateInsteadOfVisits) {
            hue = cyanHue;
          } else {
            double fraction;
            if (Lizzie.config.colorByWinrateInsteadOfVisits) {
              fraction = percentWinrate;
              if (flipWinrate) {
                fraction = 1 - fraction;
              }
              fraction = 1 / (Math.pow(1 / fraction - 1, winrateHueFactor) + 1);
            } else {
              fraction = percentPlayouts;
            }

            // Correction to make differences between colors more perceptually linear
            fraction *= 2;
            if (fraction < 1) { // red to yellow
              fraction = Math.cbrt(fraction * fraction) / 2;
            } else { // yellow to green
              fraction = 1 - Math.sqrt(2 - fraction) / 2;
            }

            hue = redHue + (greenHue - redHue) * (float) fraction;
          }

          float saturation = 1.0f;
          float brightness = 0.85f;
          float alpha =
              minAlpha
                  + (maxAlpha - minAlpha)
                      * max(
                          0,
                          (float)
                                      log(
                                          Lizzie.config.colorByWinrateInsteadOfVisits
                                              ? percentWinrate
                                              : percentPlayouts)
                                  / alphaFactor
                              + 1);

          Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
          Color color =
              new Color(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue(), (int) alpha);

          boolean isMouseOver = Lizzie.frame.isMouseOver(coords[0], coords[1]);
          if (!branchOpt.isPresent()) {
            drawShadow(g, suggestionX, suggestionY, true, alpha / 255.0f);
            g.setColor(color);
            fillCircle(g, suggestionX, suggestionY, stoneRadius);
          }

          boolean ringedMove =
              !Lizzie.config.colorByWinrateInsteadOfVisits && (isBestMove || hasMaxWinrate);
          if (!branchOpt.isPresent() || (ringedMove && isMouseOver)) {
            int strokeWidth = 1;
            if (ringedMove) {
              strokeWidth = 2;
              if (isBestMove) {
                if (hasMaxWinrate) {
                  g.setColor(color.darker());
                  strokeWidth = 1;
                } else {
                  g.setColor(Color.RED.brighter());
                }
              } else {
                g.setColor(Color.BLUE.brighter());
              }
            } else {
              g.setColor(color.darker());
            }
            g.setStroke(new BasicStroke(strokeWidth));
            drawCircle(g, suggestionX, suggestionY, stoneRadius - strokeWidth / 2);
            g.setStroke(new BasicStroke(1));
          }

          if (!branchOpt.isPresent()
                  && (hasMaxWinrate || percentPlayouts >= Lizzie.config.minPlayoutRatioForStats)
              || isMouseOver) {
            TextData textData = new TextData();
            textData.move = move;
            textData.suggestionX = suggestionX;
            textData.suggestionY = suggestionY;
            textData.flipWinrate = flipWinrate;
            textDatas.add(textData);
          }
        }
      }
    }
  }

  /** Draw winrate/playout/score statistics from textDatas */
  private void drawLeelazSuggestionsForeground(Graphics2D g, List<TextData> textDatas) {
    for (TextData textData : textDatas) {
      double roundedWinrate = round(textData.move.winrate * 10) / 10.0;
      if (textData.flipWinrate) {
        roundedWinrate = 100.0 - roundedWinrate;
      }
      g.setColor(Color.BLACK);
      if (branchOpt.isPresent() && Lizzie.board.getData().blackToPlay) g.setColor(Color.WHITE);

      String text;
      if (Lizzie.config.handicapInsteadOfWinrate) {
        text = String.format("%.2f", Lizzie.leelaz.winrateToHandicap(textData.move.winrate));
      } else {
        text = String.format("%.1f", roundedWinrate);
      }

      if (Lizzie.leelaz.supportScoremean() && Lizzie.config.showScoremeanInSuggestion) {
        double score = Utils.actualScoreMean(textData.move.scoreMean);
        if (!Lizzie.config.showWinrateInSuggestion) {
          drawString(
              g,
              textData.suggestionX,
              textData.suggestionY
                  + (Lizzie.config.showPlayoutsInSuggestion
                      ? (-stoneRadius * 3 / 16)
                      : stoneRadius / 4),
              MainFrame.winrateFont,
              Font.PLAIN,
              String.format("%.1f", score),
              stoneRadius,
              stoneRadius * (Lizzie.config.showPlayoutsInSuggestion ? 1.5 : 1.8),
              1);

          if (Lizzie.config.showPlayoutsInSuggestion) {
            drawString(
                g,
                textData.suggestionX,
                textData.suggestionY + stoneRadius * 2 / 5,
                MainFrame.uiFont,
                Utils.getPlayoutsString(textData.move.playouts),
                (float) (stoneRadius * 0.8),
                stoneRadius * 1.4);
          }
        } else {
          drawString(
              g,
              textData.suggestionX,
              textData.suggestionY
                  - (Lizzie.config.showPlayoutsInSuggestion ? stoneRadius * 5 / 16 : 0),
              LizzieFrame.winrateFont,
              Font.PLAIN,
              text,
              stoneRadius,
              stoneRadius * (Lizzie.config.showPlayoutsInSuggestion ? 1.45 : 1.5),
              1);
          if (Lizzie.config.showPlayoutsInSuggestion) {
            drawString(
                g,
                textData.suggestionX,
                textData.suggestionY + stoneRadius * 2 / 16,
                MainFrame.uiFont,
                Utils.getPlayoutsString(textData.move.playouts),
                (float) (stoneRadius * 0.7),
                stoneRadius * 1.4);
          }
          drawString(
              g,
              textData.suggestionX,
              textData.suggestionY
                  + (Lizzie.config.showPlayoutsInSuggestion
                      ? stoneRadius * 11 / 16
                      : stoneRadius * 2 / 5),
              LizzieFrame.uiFont,
              String.format("%.1f", score),
              (float) (stoneRadius * (Lizzie.config.showPlayoutsInSuggestion ? 0.75 : 0.8)),
              stoneRadius * (Lizzie.config.showPlayoutsInSuggestion ? 1.3 : 1.4));
        }
      } else {
        if (Lizzie.config.showWinrateInSuggestion && Lizzie.config.showPlayoutsInSuggestion) {
          drawString(
              g,
              textData.suggestionX,
              textData.suggestionY,
              MainFrame.winrateFont,
              Font.PLAIN,
              text,
              stoneRadius,
              stoneRadius * 1.5,
              1);

          drawString(
              g,
              textData.suggestionX,
              textData.suggestionY + stoneRadius * 2 / 5,
              MainFrame.uiFont,
              Utils.getPlayoutsString(textData.move.playouts),
              (float) (stoneRadius * 0.8),
              stoneRadius * 1.4);
        } else {
          if (Lizzie.config.showWinrateInSuggestion) {
            drawString(
                g,
                textData.suggestionX,
                textData.suggestionY + stoneRadius / 4,
                MainFrame.winrateFont,
                Font.PLAIN,
                text,
                stoneRadius,
                stoneRadius * (Lizzie.config.showPlayoutsInSuggestion ? 1.5 : 1.6),
                1);
          } else if (Lizzie.config.showPlayoutsInSuggestion) {
            drawString(
                g,
                textData.suggestionX,
                textData.suggestionY + stoneRadius / 6,
                MainFrame.uiFont,
                Utils.getPlayoutsString(textData.move.playouts),
                (float) (stoneRadius * 0.8),
                stoneRadius * 1.4);
          }
        }
      }
    }
  }

  private void drawNextMoves(Graphics2D g) {
    if (Lizzie.board == null) return;
    g.setColor(Lizzie.board.getData().blackToPlay ? Color.BLACK : Color.WHITE);

    List<BoardHistoryNode> nexts = Lizzie.board.getHistory().getNexts();

    for (int i = 0; i < nexts.size(); i++) {
      boolean first = (i == 0);
      nexts
          .get(i)
          .getData()
          .lastMove
          .ifPresent(
              nextMove -> {
                int moveX = x + scaledMarginWidth + squareWidth * nextMove[0];
                int moveY = y + scaledMarginHeight + squareHeight * nextMove[1];
                if (first) g.setStroke(new BasicStroke(2.0f));
                drawCircle(g, moveX, moveY, stoneRadius + 1); // Slightly outside best move circle
                if (first) g.setStroke(new BasicStroke(1.0f));
              });
    }
  }

  private void drawWoodenBoard(Graphics2D g) {
    if (uiConfig.getBoolean("fancy-board")) {
      // fancy version
      if (cachedBoardImage == emptyImage) {
        cachedBoardImage = Lizzie.config.theme.board();
      }

      drawTextureImage(
          g,
          cachedBoardImage,
          x - 2 * shadowRadius,
          y - 2 * shadowRadius,
          boardWidth + 4 * shadowRadius,
          boardHeight + 4 * shadowRadius);

      if (Lizzie.config.showBorder) {
        g.setStroke(new BasicStroke(shadowRadius * 2));
        // draw border
        g.setColor(new Color(0, 0, 0, 50));
        g.drawRect(
            x - shadowRadius,
            y - shadowRadius,
            boardWidth + 2 * shadowRadius,
            boardHeight + 2 * shadowRadius);
      }
      g.setStroke(new BasicStroke(1));

    } else {
      // simple version
      JSONArray boardColor = uiConfig.getJSONArray("board-color");
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
      g.setColor(new Color(boardColor.getInt(0), boardColor.getInt(1), boardColor.getInt(2)));
      g.fillRect(x, y, boardWidth, boardHeight);
    }
  }

  /**
   * Calculates the lengths and pixel margins from a given boardLength.
   *
   * @param boardLength go board's length in pixels; must be boardLength >= BOARD_SIZE - 1
   * @return an array containing the three outputs: new boardLength, scaledMargin, availableLength
   */
  private static int[] calculatePixelMargins(
      int boardWidth, int boardHeight, boolean showCoordinates, boolean isMainBoard) {
    // boardLength -= boardLength*MARGIN/3; // account for the shadows we will draw around the edge
    // of the board
    //        if (boardLength < Board.BOARD_SIZE - 1)
    //            throw new IllegalArgumentException("boardLength may not be less than " +
    // (Board.BOARD_SIZE - 1) + ", but was " + boardLength);

    int scaledMarginWidth;
    int availableWidth;
    int scaledMarginHeight;
    int availableHeight;
    if (Board.boardWidth == Board.boardHeight) {
      boardWidth = min(boardWidth, boardHeight);
    }

    // decrease boardLength until the availableLength will result in square board intersections
    double marginWidth =
        (showCoordinates || Lizzie.config.showNameInBoard && isMainBoard && !emptyName
                ? (Board.boardWidth > 3 ? 0.06 : 0.04)
                : 0.03)
            / Board.boardWidth
            * 19.0;
    boardWidth++;
    do {
      boardWidth--;
      scaledMarginWidth = (int) (marginWidth * boardWidth);
      availableWidth = boardWidth - 2 * scaledMarginWidth;
    } while (!((availableWidth - 1) % (Board.boardWidth - 1) == 0));
    // this will be true if BOARD_SIZE - 1 square intersections, plus one line, will fit
    int squareWidth = 0;
    int squareHeight = 0;
    if (Board.boardWidth != Board.boardHeight) {
      double marginHeight =
          (showCoordinates || Lizzie.config.showNameInBoard && isMainBoard && !emptyName
                  ? (Board.boardWidth > 3 ? 0.06 : 0.04)
                  : 0.03)
              / Board.boardHeight
              * 19.0;
      boardHeight++;
      do {
        boardHeight--;
        scaledMarginHeight = (int) (marginHeight * boardHeight);
        availableHeight = boardHeight - 2 * scaledMarginHeight;
      } while (!((availableHeight - 1) % (Board.boardHeight - 1) == 0));
      squareWidth = calculateSquareWidth(availableWidth);
      squareHeight = calculateSquareHeight(availableHeight);
      if (squareWidth > squareHeight) {
        squareWidth = squareHeight;
        int newWidth = squareWidth * (Board.boardWidth - 1) + 1;
        int diff = availableWidth - newWidth;
        availableWidth = newWidth;
        boardWidth -= diff + (scaledMarginWidth - scaledMarginHeight) * 2;
        scaledMarginWidth = scaledMarginHeight;
      } else if (squareWidth < squareHeight) {
        squareHeight = squareWidth;
        int newHeight = squareHeight * (Board.boardHeight - 1) + 1;
        int diff = availableHeight - newHeight;
        availableHeight = newHeight;
        boardHeight -= diff + (scaledMarginHeight - scaledMarginWidth) * 2;
        scaledMarginHeight = scaledMarginWidth;
      }
    } else {
      boardHeight = boardWidth;
      scaledMarginHeight = scaledMarginWidth;
      availableHeight = availableWidth;
    }
    return new int[] {
      boardWidth,
      scaledMarginWidth,
      availableWidth,
      boardHeight,
      scaledMarginHeight,
      availableHeight
    };
  }

  private void drawShadow(Graphics2D g, int centerX, int centerY, boolean isGhost) {
    drawShadow(g, centerX, centerY, isGhost, 1);
  }

  private void drawShadow(
      Graphics2D g, int centerX, int centerY, boolean isGhost, float shadowStrength) {
    if (!uiConfig.getBoolean("shadows-enabled")) return;

    double r = stoneRadius * Lizzie.config.shadowSize / 100;
    final int shadowSize = (int) (r * 0.3) == 0 ? 1 : (int) (r * 0.3);
    final int fartherShadowSize = (int) (r * 0.17) == 0 ? 1 : (int) (r * 0.17);

    final Paint TOP_GRADIENT_PAINT;
    final Paint LOWER_RIGHT_GRADIENT_PAINT;

    if (isGhost) {
      TOP_GRADIENT_PAINT =
          new RadialGradientPaint(
              new Point2D.Float(centerX, centerY),
              stoneRadius + shadowSize,
              new float[] {
                ((float) stoneRadius / (stoneRadius + shadowSize)) - 0.0001f,
                ((float) stoneRadius / (stoneRadius + shadowSize)),
                1.0f
              },
              new Color[] {
                new Color(0, 0, 0, 0),
                new Color(50, 50, 50, (int) (120 * shadowStrength)),
                new Color(0, 0, 0, 0)
              });

      LOWER_RIGHT_GRADIENT_PAINT =
          new RadialGradientPaint(
              new Point2D.Float(centerX + shadowSize * 2 / 3, centerY + shadowSize * 2 / 3),
              stoneRadius + fartherShadowSize,
              new float[] {0.6f, 1.0f},
              new Color[] {new Color(0, 0, 0, 180), new Color(0, 0, 0, 0)});
    } else {
      TOP_GRADIENT_PAINT =
          new RadialGradientPaint(
              new Point2D.Float(centerX, centerY),
              stoneRadius + shadowSize,
              new float[] {0.3f, 1.0f},
              new Color[] {new Color(50, 50, 50, 150), new Color(0, 0, 0, 0)});
      LOWER_RIGHT_GRADIENT_PAINT =
          new RadialGradientPaint(
              new Point2D.Float(centerX + shadowSize, centerY + shadowSize),
              stoneRadius + fartherShadowSize,
              new float[] {0.6f, 1.0f},
              new Color[] {new Color(0, 0, 0, 140), new Color(0, 0, 0, 0)});
    }

    final Paint originalPaint = g.getPaint();

    g.setPaint(TOP_GRADIENT_PAINT);
    fillCircle(g, centerX, centerY, stoneRadius + shadowSize);
    if (!isGhost) {
      g.setPaint(LOWER_RIGHT_GRADIENT_PAINT);
      fillCircle(g, centerX + shadowSize, centerY + shadowSize, stoneRadius + fartherShadowSize);
    }
    g.setPaint(originalPaint);
  }

  /** Draws a stone centered at (centerX, centerY) */
  private void drawStone(
      Graphics2D g, Graphics2D gShadow, int centerX, int centerY, Stone color, int x, int y) {
    //        g.setRenderingHint(KEY_ALPHA_INTERPOLATION,
    //                VALUE_ALPHA_INTERPOLATION_QUALITY);
    g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

    if (color.isBlack() || color.isWhite()) {
      boolean isBlack = color.isBlack();
      boolean isGhost = (color == Stone.BLACK_GHOST || color == Stone.WHITE_GHOST);
      if (uiConfig.getBoolean("fancy-stones")) {
        drawShadow(gShadow, centerX, centerY, isGhost);
        int size = stoneRadius * 2 + 1;
        g.drawImage(
            getScaleStone(isBlack, size),
            centerX - stoneRadius,
            centerY - stoneRadius,
            size,
            size,
            null);
      } else {
        drawShadow(gShadow, centerX, centerY, true);
        Color blackColor = isGhost ? new Color(0, 0, 0) : Color.BLACK;
        Color whiteColor = isGhost ? new Color(255, 255, 255) : Color.WHITE;
        g.setColor(isBlack ? blackColor : whiteColor);
        fillCircle(g, centerX, centerY, stoneRadius);
        if (!isBlack) {
          g.setColor(blackColor);
          drawCircle(g, centerX, centerY, stoneRadius);
        }
      }
    }
  }

  /** Get scaled stone, if cached then return cached */
  private BufferedImage getScaleStone(boolean isBlack, int size) {
    BufferedImage stoneImage = isBlack ? cachedBlackStoneImage : cachedWhiteStoneImage;
    if (stoneImage.getWidth() != size || stoneImage.getHeight() != size) {
      stoneImage = new BufferedImage(size, size, TYPE_INT_ARGB);
      Image img = isBlack ? Lizzie.config.theme.blackStone() : Lizzie.config.theme.whiteStone();
      Graphics2D g2 = stoneImage.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.drawImage(img.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
      g2.dispose();
      if (isBlack) {
        cachedBlackStoneImage = stoneImage;
      } else {
        cachedWhiteStoneImage = stoneImage;
      }
    }
    return stoneImage;
  }

  public BufferedImage getWallpaper() {
    if (cachedWallpaperImage == emptyImage) {
      cachedWallpaperImage = Lizzie.config.theme.background();
    }
    return cachedWallpaperImage;
  }

  /**
   * Draw scale smooth image, enhanced display quality (Not use, for future) This function use the
   * traditional Image.getScaledInstance() method to provide the nice quality, but the performance
   * is poor. Recommended for use in a few drawings
   */
  //    public void drawScaleSmoothImage(Graphics2D g, BufferedImage img, int x, int y, int width,
  // int height, ImageObserver observer) {
  //        BufferedImage newstone = new BufferedImage(width, height, TYPE_INT_ARGB);
  //        Graphics2D g2 = newstone.createGraphics();
  //        g2.drawImage(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0,
  // observer);
  //        g2.dispose();
  //        g.drawImage(newstone, x, y, width, height, observer);
  //    }

  /**
   * Draw scale smooth image, enhanced display quality (Not use, for future) This functions use a
   * multi-step approach to prevent the information loss and produces a much higher quality that is
   * close to the Image.getScaledInstance() and faster than Image.getScaledInstance() method.
   */
  //    public void drawScaleImage(Graphics2D g, BufferedImage img, int x, int y, int width, int
  // height, ImageObserver observer) {
  //        BufferedImage newstone = (BufferedImage)img;
  //        int w = img.getWidth();
  //        int h = img.getHeight();
  //        do {
  //            if (w > width) {
  //                w /= 2;
  //                if (w < width) {
  //                    w = width;
  //                }
  //            }
  //            if (h > height) {
  //                h /= 2;
  //                if (h < height) {
  //                    h = height;
  //                }
  //            }
  //            BufferedImage tmp = new BufferedImage(w, h, TYPE_INT_ARGB);
  //            Graphics2D g2 = tmp.createGraphics();
  //            g2.setRenderingHint(KEY_INTERPOLATION,
  // VALUE_INTERPOLATION_BICUBIC);
  //            g2.drawImage(newstone, 0, 0, w, h, null);
  //            g2.dispose();
  //            newstone = tmp;
  //        }
  //        while (w != width || h != height);
  //        g.drawImage(newstone, x, y, width, height, observer);
  //    }

  /** Draw texture image */
  public void drawTextureImage(
      Graphics2D g, BufferedImage img, int x, int y, int width, int height) {
    TexturePaint paint =
        new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
    g.setPaint(paint);
    g.fill(new Rectangle(x, y, width, height));
  }

  /**
   * Draw stone Markups
   *
   * @param g
   */
  private void drawStoneMarkup(Graphics2D g) {
    if (Lizzie.board == null) return;
    BoardData data = Lizzie.board.getHistory().getData();

    data.getProperties()
        .forEach(
            (key, value) -> {
              if (SGFParser.isListProperty(key)) {
                String[] labels = value.split(",");
                for (String label : labels) {
                  String[] moves = label.split(":");
                  int[] move = SGFParser.convertSgfPosToCoord(moves[0]);
                  if (move != null) {
                    Optional<int[]> lastMove =
                        branchOpt.map(b -> b.data.lastMove).orElse(Lizzie.board.getLastMove());
                    if (lastMove.map(m -> !Arrays.equals(move, m)).orElse(true)) {
                      int moveX = x + scaledMarginWidth + squareWidth * move[0];
                      int moveY = y + scaledMarginHeight + squareHeight * move[1];
                      g.setColor(
                          Lizzie.board.getStones()[Board.getIndex(move[0], move[1])].isBlack()
                              ? Color.WHITE
                              : Color.BLACK);
                      g.setStroke(new BasicStroke(2));
                      if ("LB".equals(key) && moves.length > 1) {
                        // Label
                        double labelRadius = stoneRadius * 1.4;
                        drawString(
                            g,
                            moveX,
                            moveY,
                            MainFrame.uiFont,
                            moves[1],
                            (float) labelRadius,
                            labelRadius);
                      } else if ("TR".equals(key)) {
                        drawTriangle(g, moveX, moveY, (stoneRadius + 1) * 2 / 3);
                      } else if ("SQ".equals(key)) {
                        drawSquare(g, moveX, moveY, (stoneRadius + 1) / 2);
                      } else if ("CR".equals(key)) {
                        drawCircle(g, moveX, moveY, stoneRadius * 2 / 3);
                      } else if ("MA".equals(key)) {
                        drawMarkX(g, moveX, moveY, (stoneRadius + 1) / 2);
                      }
                    }
                  }
                }
              }
            });
  }

  /** Draws the triangle of a circle centered at (centerX, centerY) with radius $radius$ */
  private void drawTriangle(Graphics2D g, int centerX, int centerY, int radius) {
    int offset = (int) (3.0 / 2.0 * radius / Math.sqrt(3.0));
    int x[] = {centerX, centerX - offset, centerX + offset};
    int y[] = {centerY - radius, centerY + radius / 2, centerY + radius / 2};
    g.drawPolygon(x, y, 3);
  }

  /** Draws the square of a circle centered at (centerX, centerY) with radius $radius$ */
  private void drawSquare(Graphics2D g, int centerX, int centerY, int radius) {
    g.drawRect(centerX - radius, centerY - radius, radius * 2, radius * 2);
  }

  /** Draws the mark(X) of a circle centered at (centerX, centerY) with radius $radius$ */
  private void drawMarkX(Graphics2D g, int centerX, int centerY, int radius) {
    g.drawLine(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    g.drawLine(centerX - radius, centerY + radius, centerX + radius, centerY - radius);
  }

  /** Fills in a circle centered at (centerX, centerY) with radius $radius$ */
  private void fillCircle(Graphics2D g, int centerX, int centerY, int radius) {
    g.fillOval(centerX - radius, centerY - radius, 2 * radius + 1, 2 * radius + 1);
  }

  /** Draws the outline of a circle centered at (centerX, centerY) with radius $radius$ */
  private void drawCircle(Graphics2D g, int centerX, int centerY, int radius) {
    g.drawOval(centerX - radius, centerY - radius, 2 * radius + 1, 2 * radius + 1);
  }

  /**
   * Draws a string centered at (x, y) of font $fontString$, whose contents are $string$. The
   * maximum/default fontsize will be $maximumFontHeight$, and the length of the drawn string will
   * be at most maximumFontWidth. The resulting actual size depends on the length of $string$.
   * aboveOrBelow is a param that lets you set: aboveOrBelow = -1 -> y is the top of the string
   * aboveOrBelow = 0 -> y is the vertical center of the string aboveOrBelow = 1 -> y is the bottom
   * of the string
   */
  private void drawString(
      Graphics2D g,
      int x,
      int y,
      Font fontBase,
      int style,
      String string,
      float maximumFontHeight,
      double maximumFontWidth,
      int aboveOrBelow) {

    Font font = makeFont(fontBase, style);

    // set maximum size of font
    FontMetrics fm = g.getFontMetrics(font);
    font = font.deriveFont((float) (font.getSize2D() * maximumFontWidth / fm.stringWidth(string)));
    font = font.deriveFont(min(maximumFontHeight, font.getSize()));
    g.setFont(font);
    fm = g.getFontMetrics(font);
    int height = fm.getAscent() - fm.getDescent();
    int verticalOffset;
    if (aboveOrBelow == -1) {
      verticalOffset = height / 2;
    } else if (aboveOrBelow == 1) {
      verticalOffset = -height / 2;
    } else {
      verticalOffset = 0;
    }

    // bounding box for debugging
    // g.drawRect(x-(int)maximumFontWidth/2, y - height/2 + verticalOffset, (int)maximumFontWidth,
    // height+verticalOffset );
    g.drawString(string, x - fm.stringWidth(string) / 2, y + height / 2 + verticalOffset);
  }

  private void drawString(
      Graphics2D g,
      int x,
      int y,
      Font fontBase,
      String string,
      float maximumFontHeight,
      double maximumFontWidth) {
    drawString(g, x, y, fontBase, Font.PLAIN, string, maximumFontHeight, maximumFontWidth, 0);
  }

  private void drawStringBold(
      Graphics2D g,
      int x,
      int y,
      Font fontBase,
      String string,
      float maximumFontHeight,
      double maximumFontWidth) {
    drawString(g, x, y, fontBase, Font.BOLD, string, maximumFontHeight, maximumFontWidth, 0);
  }

  /** @return a font with kerning enabled */
  private Font makeFont(Font fontBase, int style) {
    Font font = fontBase.deriveFont(style, 100);
    Map<TextAttribute, Object> atts = new HashMap<>();
    atts.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
    return font.deriveFont(atts);
  }

  private int[] calculatePixelMargins() {
    return calculatePixelMargins(boardWidth, boardHeight, showCoordinates(), isMainBoard);
  }

  /**
   * Set the location to render the board
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void setLocation(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point getLocation() {
    return new Point(x, y);
  }

  /**
   * Set the maximum boardLength to render the board
   *
   * @param boardLength the boardLength of the board
   */
  public void setBoardLength(int boardWidth, int boardHeight) {
    this.shadowRadius =
        Lizzie.config.showBorder ? (int) (max(boardWidth, boardHeight) * MARGIN / 6) : 0;
    this.boardWidth = boardWidth - 4 * shadowRadius;
    this.boardHeight = boardHeight - 4 * shadowRadius;
    this.x = x + 2 * shadowRadius;
    this.y = y + 2 * shadowRadius;
  }

  public void setBoardParam(int[] param) {
    boardWidth = param[0];
    scaledMarginWidth = param[1];
    availableWidth = param[2];
    boardHeight = param[3];
    scaledMarginHeight = param[4];
    availableHeight = param[5];

    squareWidth = calculateSquareWidth(availableWidth);
    squareHeight = calculateSquareHeight(availableHeight);
    stoneRadius = max(squareWidth, squareHeight) < 4 ? 1 : max(squareWidth, squareHeight) / 2 - 1;

    // re-center board
    //    setLocation(x + (boardWidth0 - boardWidth) / 2, y + (boardHeight0 - boardHeight) / 2);

    this.shadowRadius =
        Lizzie.config.showBorder ? (int) (max(boardWidth, boardHeight) * MARGIN / 6) : 0;
    this.boardWidth = boardWidth - 4 * shadowRadius;
    this.boardHeight = boardHeight - 4 * shadowRadius;
    this.x = x + 2 * shadowRadius;
    this.y = y + 2 * shadowRadius;
  }

  /**
   * @return the actual board length, including the shadows drawn at the edge of the wooden board
   */
  public int[] getActualBoardLength() {
    return new int[] {
      (int) (boardWidth * (1 + MARGIN / 3)), (int) (boardHeight * (1 + MARGIN / 3))
    };
  }

  /**
   * Converts a location on the screen to a location on the board
   *
   * @param x x pixel coordinate
   * @param y y pixel coordinate
   * @return if there is a valid coordinate, an array (x, y) where x and y are between 0 and
   *     BOARD_SIZE - 1. Otherwise, returns Optional.empty
   */
  public Optional<int[]> convertScreenToCoordinates(int x, int y) {
    int marginWidth; // the pixel width of the margins
    int boardWidthWithoutMargins; // the pixel width of the game board without margins
    int marginHeight; // the pixel height of the margins
    int boardHeightWithoutMargins; // the pixel height of the game board without margins

    // calculate a good set of boardLength, scaledMargin, and boardLengthWithoutMargins to use
    //    int[] calculatedPixelMargins = calculatePixelMargins();
    //    setBoardLength(calculatedPixelMargins[0], calculatedPixelMargins[3]);
    marginWidth = this.scaledMarginWidth;
    marginHeight = this.scaledMarginHeight;

    // transform the pixel coordinates to board coordinates
    x =
        squareWidth == 0
            ? 0
            : Math.floorDiv(x - this.x - marginWidth + squareWidth / 2, squareWidth);
    y =
        squareHeight == 0
            ? 0
            : Math.floorDiv(y - this.y - marginHeight + squareHeight / 2, squareHeight);

    // return these values if they are valid board coordinates
    return Board.isValid(x, y) ? Optional.of(new int[] {x, y}) : Optional.empty();
  }

  /**
   * Calculate the boardLength of each intersection square
   *
   * @param availableLength the pixel board length of the game board without margins
   * @return the board length of each intersection square
   */
  private static int calculateSquareWidth(int availableWidth) {
    return availableWidth / (Board.boardWidth - 1);
  }

  private static int calculateSquareHeight(int availableHeight) {
    return availableHeight / (Board.boardHeight - 1);
  }

  public boolean isShowingRawBoard() {
    return (displayedBranchLength == SHOW_RAW_BOARD || displayedBranchLength == 0);
  }

  public boolean isShowingNormalBoard() {
    return displayedBranchLength == SHOW_NORMAL_BOARD;
  }

  private int maxBranchMoves() {
    switch (displayedBranchLength) {
      case SHOW_NORMAL_BOARD:
        return Integer.MAX_VALUE;
      case SHOW_RAW_BOARD:
        return -1;
      default:
        return displayedBranchLength;
    }
  }

  public boolean isShowingBranch() {
    return showingBranch;
  }

  public void startNormalBoard() {
    setDisplayedBranchLength(SHOW_NORMAL_BOARD);
  }

  public void setDisplayedBranchLength(int n) {
    displayedBranchLength = n;
  }

  public int getDisplayedBranchLength() {
    return displayedBranchLength;
  }

  public int getReplayBranch() {
    return mouseOveredMove().isPresent() ? mouseOveredMove().get().variation.size() : 0;
  }

  public void addSuggestionAsBranch() {
    mouseOveredMove()
        .ifPresent(
            m -> {
              if (m.variation.size() > 0) {
                if (Lizzie.board.getHistory().getCurrentHistoryNode().numberOfChildren() == 0) {
                  Stone color =
                      Lizzie.board.getHistory().getLastMoveColor() == Stone.WHITE
                          ? Stone.BLACK
                          : Stone.WHITE;
                  Lizzie.board.getHistory().pass(color, false, true);
                  Lizzie.board.getHistory().previous();
                }
                for (int i = 0; i < m.variation.size(); i++) {
                  Stone color =
                      Lizzie.board.getHistory().getLastMoveColor() == Stone.WHITE
                          ? Stone.BLACK
                          : Stone.WHITE;
                  Optional<int[]> coordOpt = Board.asCoordinates(m.variation.get(i));
                  if (!coordOpt.isPresent()
                      || !Board.isValid(coordOpt.get()[0], coordOpt.get()[1])) {
                    break;
                  }
                  int[] coord = coordOpt.get();
                  Lizzie.board.getHistory().place(coord[0], coord[1], color, i == 0);
                }
                Lizzie.board.getHistory().toBranchTop();
                Lizzie.frame.refresh(2);
              }
            });
  }

  public boolean incrementDisplayedBranchLength(int n) {
    switch (displayedBranchLength) {
      case SHOW_NORMAL_BOARD:
      case SHOW_RAW_BOARD:
        return false;
      default:
        // force nonnegative
        displayedBranchLength = max(0, displayedBranchLength + n);
        return true;
    }
  }

  public boolean isInside(int x1, int y1) {
    return x <= x1 && x1 < x + boardWidth && y <= y1 && y1 < y + boardHeight;
  }

  private boolean showCoordinates() {
    return isMainBoard && Lizzie.config.showCoordinates;
  }

  public void increaseMaxAlpha(int k) {
    maxAlpha = min(maxAlpha + k, 255);
    uiPersist.put("max-alpha", maxAlpha);
  }

  public void removeEstimateRect() {
    if (boardWidth <= 0 || boardHeight <= 0) {
      return;
    }
    cachedEstimateLargeRectImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
    cachedEstimateSmallRectImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
  }

  // isZen: estimates are for black (Zen) rather than player to move (KataGo)
  // and estimates are just <0/=0/>0 (Zen) rather than -1..+1 (KataGo)
  public void drawEstimateRect(ArrayList<Double> estimateArray, boolean isZen) {
    if (boardWidth <= 0 || boardHeight <= 0) {
      return;
    }
    boolean drawLarge = false, drawSmall = false, drawSize = false;
    int drawSmart = 0;
    if (Lizzie.config.showKataGoEstimate || isZen) {
      switch (Lizzie.config.kataGoEstimateMode) {
        case "small":
          drawSmall = true;
          break;
        case "small+dead":
          drawSmall = true;
          drawSmart = 1;
          break;
        case "large":
          drawLarge = true;
          break;
        case "large+small":
          drawLarge = true;
          drawSmall = true;
          break;
        default:
        case "large+dead":
          drawLarge = true;
          drawSmall = true;
          drawSmart = 1;
          break;
        case "large+stones":
          drawLarge = true;
          drawSmall = true;
          drawSmart = 2;
          break;
        case "size":
          drawSmall = true;
          drawSize = true;
          break;
      }
    }
    BufferedImage oldLargeRectImage = cachedEstimateLargeRectImage;
    BufferedImage oldSmallRectImage = cachedEstimateSmallRectImage;
    BufferedImage newLargeRectImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
    BufferedImage newSmallRectImage = new BufferedImage(boardWidth, boardHeight, TYPE_INT_ARGB);
    Graphics2D gl = newLargeRectImage.createGraphics();
    Graphics2D gs = newSmallRectImage.createGraphics();
    for (int i = 0; i < estimateArray.size(); i++) {

      double estimate = estimateArray.get(i);
      if (isZen) {
        // Zen's estimates are only <0 / =0 / >0
        if (estimate < 0) estimate = -1;
        else if (estimate > 0) estimate = +1;
      }
      boolean isBlack = (estimate > 0);
      if (!isZen) {
        // KataGo's estimates are for player to move, not for black.
        if (!Lizzie.board.getHistory().isBlacksTurn()) isBlack = !isBlack;
      }
      int[] c = Lizzie.board.getCoord(i);
      int x = c[1];
      int y = c[0];
      int stoneX = scaledMarginWidth + squareWidth * x;
      int stoneY = scaledMarginHeight + squareHeight * y;
      // g.setColor(Color.BLACK);

      int grey = isBlack ? 0 : 255;
      double alpha = Math.abs(estimate);

      // Large rectangles (will go behind stones).

      if (drawLarge) {
        Color cl = new Color(grey, grey, grey, (int) (255 * (0.75 * alpha)));
        gl.setColor(cl);
        gl.fillRect(
            (int) (stoneX - squareWidth * 0.5),
            (int) (stoneY - squareHeight * 0.5),
            (int) squareWidth,
            (int) squareHeight);
      }

      // Small rectangles (will go on top of stones; perhaps only "dead" stones).

      Stone stoneHere = Lizzie.board.getStones()[Board.getIndex(x, y)];
      boolean differentColor = isBlack ? stoneHere.isWhite() : stoneHere.isBlack();
      boolean anyColor = stoneHere.isWhite() || stoneHere.isBlack();
      boolean allowed =
          drawSmart == 0 || (drawSmart == 1 && differentColor) || (drawSmart == 2 && anyColor) || (drawSmart == 1 && !anyColor && drawSmall);
      if (drawSmall && allowed) {
        double lengthFactor = drawSize ? 2 * convertLength(estimate) : 1.2;
        int length = (int) (lengthFactor * stoneRadius);
        int ialpha = drawSize ? 180 : (int) (255 * alpha);
        Color cl = new Color(grey, grey, grey, ialpha);
        gs.setColor(cl);
        gs.fillRect(stoneX - length / 2, stoneY - length / 2, length, length);
      }
    }
    // Lizzie isn't very careful about threading and removeEstimateRect may have been
    // called while this was running. So only replace images if same object as at start.
    if (cachedEstimateLargeRectImage == oldLargeRectImage) {
      cachedEstimateLargeRectImage = newLargeRectImage;
    }
    if (cachedEstimateSmallRectImage == oldSmallRectImage) {
      cachedEstimateSmallRectImage = newSmallRectImage;
    }
  }

  private double convertLength(double length) {
    double lengthab = Math.abs(length);
    if (lengthab > 0.2) {
      lengthab = lengthab * 7 / 10;
      return lengthab;
    } else {
      return 0;
    }
  }

  private void drawPolicy(Graphics2D g) {
    int minAlpha = 32;
    float alphaFactor = 5.0f;
    float redHue = Color.RGBtoHSB(255, 0, 0, null)[0];
    float greenHue = Color.RGBtoHSB(0, 255, 0, null)[0];
    float cyanHue = Color.RGBtoHSB(0, 255, 255, null)[0];

    if (Lizzie.frame.isShowingPolicy && !Lizzie.leelaz.getBestMoves().isEmpty()) {
      Double maxPolicy = 0.0;
      for (int n = 0; n < Lizzie.leelaz.getBestMoves().size(); n++) {
        if (Lizzie.leelaz.getBestMoves().get(n).policy > maxPolicy)
          maxPolicy = Lizzie.leelaz.getBestMoves().get(n).policy;
      }
      for (int i = 0; i < Lizzie.leelaz.getBestMoves().size(); i++) {
        MoveData bestmove = Lizzie.leelaz.getBestMoves().get(i);
        int y1 = 0;
        int x1 = 0;
        Optional<int[]> coord = Board.asCoordinates(bestmove.coordinate);
        if (coord.isPresent()) {
          x1 = coord.get()[0];
          y1 = coord.get()[1];
          int suggestionX = x + scaledMarginWidth + squareWidth * x1;
          int suggestionY = y + scaledMarginHeight + squareHeight * y1;
          double percent = bestmove.policy / maxPolicy;
          float hue;

          if (bestmove.policy == maxPolicy) {
            hue = cyanHue;
          } else {
            double fraction;
            fraction = percent;
            // Correction to make differences between colors more perceptually linear
            fraction *= 2;
            if (fraction < 1) { // red to yellow
              fraction = Math.cbrt(fraction * fraction) / 2;
            } else { // yellow to green
              fraction = 1 - Math.sqrt(2 - fraction) / 2;
            }
            hue = redHue + (greenHue - redHue) * (float) fraction;
          }

          float saturation = 1.0f;
          float brightness = 0.85f;
          float alpha =
              minAlpha + (maxAlpha - minAlpha) * max(0, (float) log(percent) / alphaFactor + 1);

          Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
          Color color =
              new Color(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue(), (int) alpha);
          if (!branchOpt.isPresent()) {
            drawShadow(g, suggestionX, suggestionY, true, alpha / 255.0f);
            g.setColor(color);
            fillCircle(g, suggestionX, suggestionY, stoneRadius);

            String text =
                String.format("%.1f", ((double) Lizzie.leelaz.getBestMoves().get(i).policy));
            g.setColor(Color.WHITE);
            drawString(
                g,
                suggestionX,
                suggestionY,
                LizzieFrame.winrateFont,
                Font.PLAIN,
                text,
                stoneRadius,
                stoneRadius * 1.9,
                0);
          }
        }
      }
    }
  }
}
