package featurecat.lizzie.gui;

import static java.awt.RenderingHints.*;
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
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
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

  private int x, y;
  private int boardLength;
  private int shadowRadius;

  private JSONObject uiConfig, uiPersist;
  private int scaledMargin, availableLength, squareLength, stoneRadius;
  private Optional<Branch> branchOpt = Optional.empty();
  private List<MoveData> bestMoves;

  private BufferedImage cachedBackgroundImage = emptyImage;
  private boolean cachedBackgroundImageHasCoordinatesEnabled = false;
  private int cachedX, cachedY;

  private BufferedImage cachedStonesImage = emptyImage;
  private BufferedImage cachedBoardImage = emptyImage;
  private BufferedImage cachedWallpaperImage = emptyImage;
  private BufferedImage cachedStonesShadowImage = emptyImage;
  private Zobrist cachedZhash = new Zobrist(); // defaults to an empty board

  private BufferedImage cachedBlackStoneImage = emptyImage;
  private BufferedImage cachedWhiteStoneImage = emptyImage;

  private BufferedImage branchStonesImage = emptyImage;
  private BufferedImage branchStonesShadowImage;

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
    setupSizeParameters();

    //        Stopwatch timer = new Stopwatch();
    drawGoban(g);
    //        timer.lap("background");
    drawStones();
    //        timer.lap("stones");
    if (Lizzie.board.inScoreMode() && isMainBoard) {
      drawScore(g);
    } else {
      drawBranch();
    }
    //        timer.lap("branch");

    renderImages(g);
    //        timer.lap("rendering images");

    if (!isMainBoard) {
      drawMoveNumbers(g);
      return;
    }

    if (!isShowingRawBoard()) {
      drawMoveNumbers(g);
      //        timer.lap("movenumbers");
      if (!Lizzie.frame.isPlayingAgainstLeelaz && Lizzie.config.showBestMovesNow())
        drawLeelazSuggestions(g);

      if (Lizzie.config.showNextMoves) {
        drawNextMoves(g);
      }

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
  private void setupSizeParameters() {
    int boardLength0 = boardLength;

    int[] calculatedPixelMargins = calculatePixelMargins();
    boardLength = calculatedPixelMargins[0];
    scaledMargin = calculatedPixelMargins[1];
    availableLength = calculatedPixelMargins[2];

    squareLength = calculateSquareLength(availableLength);
    stoneRadius = squareLength < 4 ? 1 : squareLength / 2 - 1;

    // re-center board
    setLocation(x + (boardLength0 - boardLength) / 2, y + (boardLength0 - boardLength) / 2);
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
        || cachedX != x
        || cachedY != y
        || cachedBackgroundImageHasCoordinatesEnabled != showCoordinates()
        || Lizzie.board.isForceRefresh()) {

      Lizzie.board.setForceRefresh(false);

      cachedBackgroundImage = new BufferedImage(width, height, TYPE_INT_ARGB);
      Graphics2D g = cachedBackgroundImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      // Draw the wooden background
      drawWoodenBoard(g);

      // Draw the lines
      g.setColor(Color.BLACK);
      for (int i = 0; i < Board.boardSize; i++) {
        g.drawLine(
            x + scaledMargin,
            y + scaledMargin + squareLength * i,
            x + scaledMargin + availableLength - 1,
            y + scaledMargin + squareLength * i);
      }
      for (int i = 0; i < Board.boardSize; i++) {
        g.drawLine(
            x + scaledMargin + squareLength * i,
            y + scaledMargin,
            x + scaledMargin + squareLength * i,
            y + scaledMargin + availableLength - 1);
      }

      // Draw the star points
      drawStarPoints(g);

      // Draw coordinates if enabled
      if (showCoordinates()) {
        g.setColor(Color.BLACK);
        for (int i = 0; i < Board.boardSize; i++) {
          drawString(
              g,
              x + scaledMargin + squareLength * i,
              y + scaledMargin / 2,
              LizzieFrame.uiFont,
              Board.asName(i),
              stoneRadius * 4 / 5,
              stoneRadius);
          drawString(
              g,
              x + scaledMargin + squareLength * i,
              y - scaledMargin / 2 + boardLength,
              LizzieFrame.uiFont,
              Board.asName(i),
              stoneRadius * 4 / 5,
              stoneRadius);
        }
        for (int i = 0; i < Board.boardSize; i++) {
          drawString(
              g,
              x + scaledMargin / 2,
              y + scaledMargin + squareLength * i,
              LizzieFrame.uiFont,
              "" + (Board.boardSize - i),
              stoneRadius * 4 / 5,
              stoneRadius);
          drawString(
              g,
              x - scaledMargin / 2 + +boardLength,
              y + scaledMargin + squareLength * i,
              LizzieFrame.uiFont,
              "" + (Board.boardSize - i),
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

  /**
   * Draws the star points on the board, according to board size
   *
   * @param g graphics2d object to draw
   */
  private void drawStarPoints(Graphics2D g) {
    if (Board.boardSize == 19) {
      drawStarPoints0(3, 3, 6, false, g);
    } else if (Board.boardSize == 13) {
      drawStarPoints0(2, 3, 6, true, g);
    } else if (Board.boardSize == 9) {
      drawStarPoints0(2, 2, 4, true, g);
    } else if (Board.boardSize == 7) {
      drawStarPoints0(2, 2, 2, true, g);
    } else if (Board.boardSize == 5) {
      drawStarPoints0(0, 0, 2, true, g);
    }
  }

  private void drawStarPoints0(
      int nStarpoints, int edgeOffset, int gridDistance, boolean center, Graphics2D g) {
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    int starPointRadius = (int) (STARPOINT_DIAMETER * boardLength) / 2;
    for (int i = 0; i < nStarpoints; i++) {
      for (int j = 0; j < nStarpoints; j++) {
        int centerX = x + scaledMargin + squareLength * (edgeOffset + gridDistance * i);
        int centerY = y + scaledMargin + squareLength * (edgeOffset + gridDistance * j);
        fillCircle(g, centerX, centerY, starPointRadius);
      }
    }

    if (center) {
      int centerX = x + scaledMargin + squareLength * gridDistance;
      int centerY = y + scaledMargin + squareLength * gridDistance;
      fillCircle(g, centerX, centerY, starPointRadius);
    }
  }

  /** Draw the stones. We cache the image for a performance boost. */
  private void drawStones() {
    // draw a new image if frame size changes or board state changes
    if (cachedStonesImage.getWidth() != boardLength
        || cachedStonesImage.getHeight() != boardLength
        || cachedDisplayedBranchLength != displayedBranchLength
        || cachedBackgroundImageHasCoordinatesEnabled != showCoordinates()
        || !cachedZhash.equals(Lizzie.board.getData().zobrist)
        || Lizzie.board.inScoreMode()
        || lastInScoreMode) {

      cachedStonesImage = new BufferedImage(boardLength, boardLength, TYPE_INT_ARGB);
      cachedStonesShadowImage = new BufferedImage(boardLength, boardLength, TYPE_INT_ARGB);
      Graphics2D g = cachedStonesImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      Graphics2D gShadow = cachedStonesShadowImage.createGraphics();
      gShadow.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

      // we need antialiasing to make the stones pretty. Java is a bit slow at antialiasing; that's
      // why we want the cache
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
      gShadow.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

      for (int i = 0; i < Board.boardSize; i++) {
        for (int j = 0; j < Board.boardSize; j++) {
          int stoneX = scaledMargin + squareLength * i;
          int stoneY = scaledMargin + squareLength * j;
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
    for (int i = 0; i < Board.boardSize; i++) {
      for (int j = 0; j < Board.boardSize; j++) {
        int stoneX = scaledMargin + squareLength * i;
        int stoneY = scaledMargin + squareLength * j;
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
    branchStonesImage = new BufferedImage(boardLength, boardLength, TYPE_INT_ARGB);
    branchStonesShadowImage = new BufferedImage(boardLength, boardLength, TYPE_INT_ARGB);
    branchOpt = Optional.empty();

    if (Lizzie.frame.isPlayingAgainstLeelaz) {
      return;
    }
    // calculate best moves and branch
    bestMoves = Lizzie.leelaz.getBestMoves();
    variationOpt = Optional.empty();

    if (isMainBoard && (isShowingRawBoard() || !Lizzie.config.showBranchNow())) {
      return;
    }

    Graphics2D g = (Graphics2D) branchStonesImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    Graphics2D gShadow = (Graphics2D) branchStonesShadowImage.getGraphics();
    gShadow.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    Optional<MoveData> suggestedMove = (isMainBoard ? mouseOveredMove() : getBestMove());
    if (!suggestedMove.isPresent()) {
      return;
    }
    List<String> variation = suggestedMove.get().variation;
    Branch branch = new Branch(Lizzie.board, variation);
    branchOpt = Optional.of(branch);
    variationOpt = Optional.of(variation);
    showingBranch = true;

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

    for (int i = 0; i < Board.boardSize; i++) {
      for (int j = 0; j < Board.boardSize; j++) {
        // Display latest stone for ghost dead stone
        int index = Board.getIndex(i, j);
        Stone stone = branch.data.stones[index];
        boolean isGhost = (stone == Stone.BLACK_GHOST || stone == Stone.WHITE_GHOST);
        if (Lizzie.board.getData().stones[index] != Stone.EMPTY && !isGhost) continue;
        if (branch.data.moveNumberList[index] > maxBranchMoves()) continue;

        int stoneX = scaledMargin + squareLength * i;
        int stoneY = scaledMargin + squareLength * j;

        drawStone(g, gShadow, stoneX, stoneY, stone.unGhosted(), i, j);
      }
    }

    g.dispose();
    gShadow.dispose();
  }

  private Optional<MoveData> mouseOveredMove() {
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
    g.drawImage(cachedStonesShadowImage, x, y, null);
    if (Lizzie.config.showBranchNow()) {
      g.drawImage(branchStonesShadowImage, x, y, null);
    }
    g.drawImage(cachedStonesImage, x, y, null);
    if (Lizzie.config.showBranchNow()) {
      g.drawImage(branchStonesImage, x, y, null);
    }
  }

  /** Draw move numbers and/or mark the last played move */
  private void drawMoveNumbers(Graphics2D g) {
    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    Board board = Lizzie.board;
    Optional<int[]> lastMoveOpt = branchOpt.map(b -> b.data.lastMove).orElse(board.getLastMove());
    if (Lizzie.config.allowMoveNumber == 0 && !branchOpt.isPresent()) {
      if (lastMoveOpt.isPresent()) {
        int[] lastMove = lastMoveOpt.get();

        // Mark the last coordinate
        int lastMoveMarkerRadius = stoneRadius / 2;
        int stoneX = x + scaledMargin + squareLength * lastMove[0];
        int stoneY = y + scaledMargin + squareLength * lastMove[1];

        // Set color to the opposite color of whatever is on the board
        boolean isWhite = board.getStones()[Board.getIndex(lastMove[0], lastMove[1])].isWhite();
        g.setColor(isWhite ? Color.BLACK : Color.WHITE);

        if (Lizzie.config.solidStoneIndicator) {
          // Use a solid circle instead of
          fillCircle(g, stoneX, stoneY, (int) (lastMoveMarkerRadius * 0.65));
        } else {
          drawCircle(g, stoneX, stoneY, lastMoveMarkerRadius);
        }
      } else if (board.getData().moveNumber != 0 && !board.inScoreMode()) {
        g.setColor(
            board.getData().blackToPlay ? new Color(255, 255, 255, 150) : new Color(0, 0, 0, 150));
        g.fillOval(
            x + boardLength / 2 - 4 * stoneRadius,
            y + boardLength / 2 - 4 * stoneRadius,
            stoneRadius * 8,
            stoneRadius * 8);
        g.setColor(
            board.getData().blackToPlay ? new Color(0, 0, 0, 255) : new Color(255, 255, 255, 255));
        drawString(
            g,
            x + boardLength / 2,
            y + boardLength / 2,
            LizzieFrame.uiFont,
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

    for (int i = 0; i < Board.boardSize; i++) {
      for (int j = 0; j < Board.boardSize; j++) {
        int stoneX = x + scaledMargin + squareLength * i;
        int stoneY = y + scaledMargin + squareLength * j;
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
              LizzieFrame.uiFont,
              moveNumberString,
              (float) (stoneRadius * 1.4),
              (int) (stoneRadius * 1.4));
        }
      }
    }
  }

  /**
   * Draw all of Leelaz's suggestions as colored stones with winrate/playout statistics overlayed
   */
  private void drawLeelazSuggestions(Graphics2D g) {
    int minAlpha = 32;
    float hueFactor = 3.0f;
    float alphaFactor = 5.0f;
    float greenHue = Color.RGBtoHSB(0, 1, 0, null)[0];
    float cyanHue = Color.RGBtoHSB(0, 1, 1, null)[0];

    if (!bestMoves.isEmpty()) {

      int maxPlayouts = 0;
      double maxWinrate = 0;
      for (MoveData move : bestMoves) {
        if (move.playouts > maxPlayouts) maxPlayouts = move.playouts;
        if (move.winrate > maxWinrate) maxWinrate = move.winrate;
      }

      for (int i = 0; i < Board.boardSize; i++) {
        for (int j = 0; j < Board.boardSize; j++) {
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

          if (move.playouts == 0) {
            continue; // This actually can happen
          }

          float percentPlayouts = (float) move.playouts / maxPlayouts;

          Optional<int[]> coordsOpt = Board.asCoordinates(move.coordinate);
          if (!coordsOpt.isPresent()) {
            continue;
          }
          int[] coords = coordsOpt.get();

          int suggestionX = x + scaledMargin + squareLength * coords[0];
          int suggestionY = y + scaledMargin + squareLength * coords[1];

          // 0 = Reddest hue
          float logPlayouts = (float) log(percentPlayouts);
          float otherHue = -greenHue * max(0, logPlayouts / hueFactor + 1);
          float hue = isBestMove ? cyanHue : otherHue;
          float saturation = 0.75f;
          float brightness = 0.85f;
          float alpha = (minAlpha + (maxAlpha - minAlpha) * max(0, logPlayouts / alphaFactor + 1));

          Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
          Color color =
              new Color(hsbColor.getRed(), hsbColor.getBlue(), hsbColor.getGreen(), (int) alpha);

          boolean isMouseOver = Lizzie.frame.isMouseOver(coords[0], coords[1]);
          if (!branchOpt.isPresent()) {
            drawShadow(g, suggestionX, suggestionY, true, alpha / 255.0f);
            g.setColor(color);
            fillCircle(g, suggestionX, suggestionY, stoneRadius);
          }

          if (!branchOpt.isPresent() || isBestMove && isMouseOver) {
            int strokeWidth = 1;
            if (isBestMove != hasMaxWinrate) {
              strokeWidth = 2;
              g.setColor(isBestMove ? Color.RED : Color.BLUE);
              g.setStroke(new BasicStroke(strokeWidth));
            } else {
              g.setColor(color.darker());
            }
            drawCircle(g, suggestionX, suggestionY, stoneRadius - strokeWidth / 2);
            g.setStroke(new BasicStroke(1));
          }

          if (!branchOpt.isPresent()
                  && (hasMaxWinrate
                      || percentPlayouts >= uiConfig.getDouble("min-playout-ratio-for-stats"))
              || isMouseOver) {
            double roundedWinrate = round(move.winrate * 10) / 10.0;
            if (uiConfig.getBoolean("win-rate-always-black")
                && !Lizzie.board.getData().blackToPlay) {
              roundedWinrate = 100.0 - roundedWinrate;
            }
            g.setColor(Color.BLACK);
            if (branchOpt.isPresent() && Lizzie.board.getData().blackToPlay)
              g.setColor(Color.WHITE);

            String text;
            if (Lizzie.config.handicapInsteadOfWinrate) {
              text = String.format("%.2f", Lizzie.leelaz.winrateToHandicap(move.winrate));
            } else {
              text = String.format("%.1f", roundedWinrate);
            }

            drawString(
                g,
                suggestionX,
                suggestionY,
                LizzieFrame.winrateFont,
                Font.PLAIN,
                text,
                stoneRadius,
                stoneRadius * 1.5,
                1);

            drawString(
                g,
                suggestionX,
                suggestionY + stoneRadius * 2 / 5,
                LizzieFrame.uiFont,
                Lizzie.frame.getPlayoutsString(move.playouts),
                (float) (stoneRadius * 0.8),
                stoneRadius * 1.4);
          }
        }
      }
    }
  }

  private void drawNextMoves(Graphics2D g) {
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
                int moveX = x + scaledMargin + squareLength * nextMove[0];
                int moveY = y + scaledMargin + squareLength * nextMove[1];
                if (first) g.setStroke(new BasicStroke(3.0f));
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
          boardLength + 4 * shadowRadius,
          boardLength + 4 * shadowRadius);

      if (Lizzie.config.showBorder) {
        g.setStroke(new BasicStroke(shadowRadius * 2));
        // draw border
        g.setColor(new Color(0, 0, 0, 50));
        g.drawRect(
            x - shadowRadius,
            y - shadowRadius,
            boardLength + 2 * shadowRadius,
            boardLength + 2 * shadowRadius);
      }
      g.setStroke(new BasicStroke(1));

    } else {
      // simple version
      JSONArray boardColor = uiConfig.getJSONArray("board-color");
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
      g.setColor(new Color(boardColor.getInt(0), boardColor.getInt(1), boardColor.getInt(2)));
      g.fillRect(x, y, boardLength, boardLength);
    }
  }

  /**
   * Calculates the lengths and pixel margins from a given boardLength.
   *
   * @param boardLength go board's length in pixels; must be boardLength >= BOARD_SIZE - 1
   * @return an array containing the three outputs: new boardLength, scaledMargin, availableLength
   */
  private int[] calculatePixelMargins(int boardLength) {
    // boardLength -= boardLength*MARGIN/3; // account for the shadows we will draw around the edge
    // of the board
    //        if (boardLength < Board.BOARD_SIZE - 1)
    //            throw new IllegalArgumentException("boardLength may not be less than " +
    // (Board.BOARD_SIZE - 1) + ", but was " + boardLength);

    int scaledMargin;
    int availableLength;

    // decrease boardLength until the availableLength will result in square board intersections
    double margin =
        (showCoordinates() ? 1.5 / (Board.boardSize + 2) : 1.0d / (Board.boardSize + 1));
    boardLength++;
    do {
      boardLength--;
      scaledMargin = (int) (margin * boardLength);
      availableLength = boardLength - 2 * scaledMargin;
    } while (!((availableLength - 1) % (Board.boardSize - 1) == 0));
    // this will be true if BOARD_SIZE - 1 square intersections, plus one line, will fit

    return new int[] {boardLength, scaledMargin, availableLength};
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
                      int moveX = x + scaledMargin + squareLength * move[0];
                      int moveY = y + scaledMargin + squareLength * move[1];
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
                            LizzieFrame.uiFont,
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

  /** @return a font with kerning enabled */
  private Font makeFont(Font fontBase, int style) {
    Font font = fontBase.deriveFont(style, 100);
    Map<TextAttribute, Object> atts = new HashMap<>();
    atts.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
    return font.deriveFont(atts);
  }

  private int[] calculatePixelMargins() {
    return calculatePixelMargins(boardLength);
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
  public void setBoardLength(int boardLength) {
    this.shadowRadius = Lizzie.config.showBorder ? (int) (boardLength * MARGIN / 6) : 0;
    this.boardLength = boardLength - 4 * shadowRadius;
    this.x = x + 2 * shadowRadius;
    this.y = y + 2 * shadowRadius;
  }

  /**
   * @return the actual board length, including the shadows drawn at the edge of the wooden board
   */
  public int getActualBoardLength() {
    return (int) (boardLength * (1 + MARGIN / 3));
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
    int marginLength; // the pixel width of the margins
    int boardLengthWithoutMargins; // the pixel width of the game board without margins

    // calculate a good set of boardLength, scaledMargin, and boardLengthWithoutMargins to use
    int[] calculatedPixelMargins = calculatePixelMargins();
    setBoardLength(calculatedPixelMargins[0]);
    marginLength = calculatedPixelMargins[1];
    boardLengthWithoutMargins = calculatedPixelMargins[2];

    int squareSize = calculateSquareLength(boardLengthWithoutMargins);

    // transform the pixel coordinates to board coordinates
    x = Math.floorDiv(x - this.x - marginLength + squareSize / 2, squareSize);
    y = Math.floorDiv(y - this.y - marginLength + squareSize / 2, squareSize);

    // return these values if they are valid board coordinates
    return Board.isValid(x, y) ? Optional.of(new int[] {x, y}) : Optional.empty();
  }

  /**
   * Calculate the boardLength of each intersection square
   *
   * @param availableLength the pixel board length of the game board without margins
   * @return the board length of each intersection square
   */
  private int calculateSquareLength(int availableLength) {
    return availableLength / (Board.boardSize - 1);
  }

  private boolean isShowingRawBoard() {
    return (displayedBranchLength == SHOW_RAW_BOARD || displayedBranchLength == 0);
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

  public void setDisplayedBranchLength(int n) {
    displayedBranchLength = n;
  }

  public int getDisplayedBranchLength() {
    return displayedBranchLength;
  }

  public int getReplayBranch() {
    return mouseOveredMove().isPresent() ? mouseOveredMove().get().variation.size() : 0;
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
    return x <= x1 && x1 < x + boardLength && y <= y1 && y1 < y + boardLength;
  }

  private boolean showCoordinates() {
    return isMainBoard && Lizzie.config.showCoordinates;
  }

  public void increaseMaxAlpha(int k) {
    maxAlpha = min(maxAlpha + k, 255);
    uiPersist.put("max-alpha", maxAlpha);
  }
}
