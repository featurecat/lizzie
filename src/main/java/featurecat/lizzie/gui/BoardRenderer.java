package featurecat.lizzie.gui;

import org.json.JSONArray;
import org.json.JSONObject;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Branch;
import featurecat.lizzie.analysis.MoveData;
import featurecat.lizzie.plugin.PluginManager;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.Stone;
import featurecat.lizzie.rules.Zobrist;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import featurecat.lizzie.theme.DefaultTheme;
import featurecat.lizzie.theme.ITheme;

public class BoardRenderer {
    private static final double MARGIN = 0.03; // percentage of the boardLength to offset before drawing black lines
    private static final double MARGIN_WITH_COORDINATES = 0.06;
    private static final double STARPOINT_DIAMETER = 0.015;

    private int x, y;
    private int boardLength;

    private JSONObject uiConfig;

    private int scaledMargin, availableLength, squareLength, stoneRadius;
    private Branch branch;
    private List<MoveData> bestMoves;

    private BufferedImage cachedBackgroundImage = null;
    private boolean cachedBackgroundImageHasCoordinatesEnabled = false;
    private int cachedX, cachedY;

    private BufferedImage cachedStonesImage = null;
    private BufferedImage cachedStonesShadowImage = null;
    private Zobrist cachedZhash = new Zobrist(); // defaults to an empty board

    private BufferedImage branchStonesImage = null;
    private BufferedImage branchStonesShadowImage = null;

    private boolean lastInScoreMode = false;

    public ITheme theme;
    public List<String> variation;

    // special values of displayedBranchLength
    public static final int SHOW_RAW_BOARD = -1;
    public static final int SHOW_NORMAL_BOARD = -2;

    private int displayedBranchLength = SHOW_NORMAL_BOARD;
    private int cachedDisplayedBranchLength = SHOW_RAW_BOARD;
    private boolean showingBranch = false;
    private boolean isMainBoard = false;

    public BoardRenderer(boolean isMainBoard) {
        uiConfig = Lizzie.config.config.getJSONObject("ui");
        theme = ITheme.loadTheme(uiConfig.getString("theme"));
        if (theme == null) {
            theme = new DefaultTheme();
        }
        this.isMainBoard = isMainBoard;
    }

    /**
     * Draw a go board
     */
    public void draw(Graphics2D g) {
        if (Lizzie.frame == null || Lizzie.board == null)
            return;

        setupSizeParameters();

//        Stopwatch timer = new Stopwatch();
        drawBackground(g);
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
            if (!Lizzie.frame.isPlayingAgainstLeelaz && Lizzie.config.showBestMoves)
                drawLeelazSuggestions(g);

            if (Lizzie.config.showNextMoves) {
                drawNextMoves(g);
            }
        }

        PluginManager.onDraw(g);
//        timer.lap("leelaz");

//        timer.print();
    }

    /**
     * Return the best move of Leelaz's suggestions
     *
     * @return the coordinate name of the best move
     */
    public String bestMoveCoordinateName() {
        if (bestMoves == null || bestMoves.size() == 0) {
            return null;
        } else {
            return bestMoves.get(0).coordinate;
        }
    }

    /**
     * Calculate good values for boardLength, scaledMargin, availableLength, and squareLength
     */
    private void setupSizeParameters() {
        int originalBoardLength = boardLength;

        int[] calculatedPixelMargins = calculatePixelMargins();
        boardLength = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableLength = calculatedPixelMargins[2];

        squareLength = calculateSquareLength(availableLength);
        stoneRadius = squareLength / 2 - 1;

        // re-center board
        setLocation(x + (originalBoardLength - boardLength) / 2, y + (originalBoardLength - boardLength) / 2);
    }

    /**
     * Draw the green background and go board with lines. We cache the image for a performance boost.
     */
    private void drawBackground(Graphics2D g0) {
        // draw the cached background image if frame size changes
        if (cachedBackgroundImage == null || cachedBackgroundImage.getWidth() != Lizzie.frame.getWidth() ||
                cachedBackgroundImage.getHeight() != Lizzie.frame.getHeight() ||
                cachedX != x || cachedY != y ||
                cachedBackgroundImageHasCoordinatesEnabled != showCoordinates()) {

            cachedBackgroundImage = new BufferedImage(Lizzie.frame.getWidth(), Lizzie.frame.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cachedBackgroundImage.createGraphics();

            // draw the wooden background
            drawWoodenBoard(g);

            // draw the lines
            g.setColor(Color.BLACK);
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                g.drawLine(x + scaledMargin, y + scaledMargin + squareLength * i,
                        x + scaledMargin + availableLength - 1, y + scaledMargin + squareLength * i);
            }
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                g.drawLine(x + scaledMargin + squareLength * i, y + scaledMargin,
                        x + scaledMargin + squareLength * i, y + scaledMargin + availableLength - 1);
            }

            // draw the star points
            drawStarPoints(g);

            // draw coordinates if enabled
            if (showCoordinates()) {
                g.setColor(Color.BLACK);
                String alphabet = "ABCDEFGHJKLMNOPQRST";
                for (int i = 0; i < Board.BOARD_SIZE; i++) {
                    drawString(g, x + scaledMargin + squareLength * i, y + scaledMargin / 2, LizzieFrame.OpenSansRegularBase, "" + alphabet.charAt(i), stoneRadius * 4 / 5, stoneRadius);
                    drawString(g, x + scaledMargin + squareLength * i, y - scaledMargin / 2 + boardLength, LizzieFrame.OpenSansRegularBase, "" + alphabet.charAt(i), stoneRadius * 4 / 5, stoneRadius);
                }
                for (int i = 0; i < Board.BOARD_SIZE; i++) {
                    drawString(g, x + scaledMargin / 2, y + scaledMargin + squareLength * i, LizzieFrame.OpenSansRegularBase, "" + (Board.BOARD_SIZE - i), stoneRadius * 4 / 5, stoneRadius);
                    drawString(g, x - scaledMargin / 2 + +boardLength, y + scaledMargin + squareLength * i, LizzieFrame.OpenSansRegularBase, "" + (Board.BOARD_SIZE - i), stoneRadius * 4 / 5, stoneRadius);
                }
            }
            cachedBackgroundImageHasCoordinatesEnabled = showCoordinates();
            g.dispose();
        }

        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g0.drawImage(cachedBackgroundImage, 0, 0, null);
        cachedX = x;
        cachedY = y;
    }

    /**
     * Draw the star points on the board, according to board size
     *
     * @param g graphics2d object to draw
     */
    private void drawStarPoints(Graphics2D g) {
        if (Board.BOARD_SIZE == 9) {
            drawStarPoints9x9(g);
        } else if (Board.BOARD_SIZE == 13) {
            drawStarPoints13x13(g);
        } else {
            drawStarPoints19x19(g);
        }
    }

    private void drawStarPoints19x19(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int starPointRadius = (int) (STARPOINT_DIAMETER * boardLength) / 2;
        final int NUM_STARPOINTS = 3;
        final int STARPOINT_EDGE_OFFSET = 3;
        final int STARPOINT_GRID_DISTANCE = 6;
        for (int i = 0; i < NUM_STARPOINTS; i++) {
            for (int j = 0; j < NUM_STARPOINTS; j++) {
                int centerX = x + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * i);
                int centerY = y + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * j);
                fillCircle(g, centerX, centerY, starPointRadius);
            }
        }
    }

    private void drawStarPoints13x13(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int starPointRadius = (int) (STARPOINT_DIAMETER * boardLength) / 2;
        final int NUM_STARPOINTS = 2;
        final int STARPOINT_EDGE_OFFSET = 3;
        final int STARPOINT_GRID_DISTANCE = 6;
        for (int i = 0; i < NUM_STARPOINTS; i++) {
            for (int j = 0; j < NUM_STARPOINTS; j++) {
                int centerX = x + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * i);
                int centerY = y + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * j);
                fillCircle(g, centerX, centerY, starPointRadius);
            }
        }

        // Draw center
        int centerX = x + scaledMargin + squareLength * STARPOINT_GRID_DISTANCE;
        int centerY = y + scaledMargin + squareLength * STARPOINT_GRID_DISTANCE;
        fillCircle(g, centerX, centerY, starPointRadius);
    }

    private void drawStarPoints9x9(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int starPointRadius = (int) (STARPOINT_DIAMETER * boardLength) / 2;
        final int NUM_STARPOINTS = 2;
        final int STARPOINT_EDGE_OFFSET = 2;
        final int STARPOINT_GRID_DISTANCE = 4;
        for (int i = 0; i < NUM_STARPOINTS; i++) {
            for (int j = 0; j < NUM_STARPOINTS; j++) {
                int centerX = x + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * i);
                int centerY = y + scaledMargin + squareLength * (STARPOINT_EDGE_OFFSET + STARPOINT_GRID_DISTANCE * j);
                fillCircle(g, centerX, centerY, starPointRadius);
            }
        }

        // Draw center
        int centerX = x + scaledMargin + squareLength * STARPOINT_GRID_DISTANCE;
        int centerY = y + scaledMargin + squareLength * STARPOINT_GRID_DISTANCE;
        fillCircle(g, centerX, centerY, starPointRadius);
    }

    /**
     * Draw the stones. We cache the image for a performance boost.
     */
    private void drawStones() {
        // draw a new image if frame size changes or board state changes
        if (cachedStonesImage == null || cachedStonesImage.getWidth() != boardLength ||
                cachedStonesImage.getHeight() != boardLength ||
                cachedDisplayedBranchLength != displayedBranchLength ||
                !cachedZhash.equals(Lizzie.board.getData().zobrist)
                || Lizzie.board.inScoreMode() || lastInScoreMode) {

            cachedStonesImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
            cachedStonesShadowImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cachedStonesImage.createGraphics();
            Graphics2D gShadow = cachedStonesShadowImage.createGraphics();

            // we need antialiasing to make the stones pretty. Java is a bit slow at antialiasing; that's why we want the cache
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gShadow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    int stoneX = scaledMargin + squareLength * i;
                    int stoneY = scaledMargin + squareLength * j;
                    drawStone(g, gShadow, stoneX, stoneY, Lizzie.board.getStones()[Board.getIndex(i, j)], i, j);
                }
            }

            cachedZhash = Lizzie.board.getData().zobrist.clone();
            cachedDisplayedBranchLength = displayedBranchLength;
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
        Stone scorestones[] = Lizzie.board.scoreStones();
        int scoreRadius = stoneRadius / 4;
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                int stoneX = scaledMargin + squareLength * i;
                int stoneY = scaledMargin + squareLength * j;
                switch (scorestones[Board.getIndex(i, j)]) {
                    case WHITE_POINT:
                    case BLACK_CAPTURED:
                        g.setColor(Color.white);
                        fillCircle(g,  stoneX, stoneY, scoreRadius);
                        break;
                    case BLACK_POINT:
                    case WHITE_CAPTURED:
                        g.setColor(Color.black);
                        fillCircle(g,  stoneX, stoneY, scoreRadius);
                        break;
                    case DAME:
                        g.setColor(Color.red);
                        fillCircle(g,  stoneX, stoneY, scoreRadius);
                        break;
                }
            }
        }
        g.dispose();
    }

    /**
     * Draw the 'ghost stones' which show a variation Leelaz is thinking about
     */
    private void drawBranch() {
        showingBranch = false;
        branchStonesImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
        branchStonesShadowImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
        branch = null;

        if (Lizzie.frame.isPlayingAgainstLeelaz || Lizzie.leelaz == null) {
            return;
        }
        // calculate best moves and branch
        bestMoves = Lizzie.leelaz.getBestMoves();
        branch = null;
        variation = null;

        if (isMainBoard && (isShowingRawBoard() || !Lizzie.config.showBranch)) {
            return;
        }

        Graphics2D g = (Graphics2D) branchStonesImage.getGraphics();
        Graphics2D gShadow = (Graphics2D) branchStonesShadowImage.getGraphics();

        MoveData suggestedMove = (isMainBoard ? mouseHoveredMove() : getBestMove());
        if (suggestedMove == null)
            return;
        variation = suggestedMove.variation;
        branch = new Branch(Lizzie.board, variation);

        if (branch == null)
            return;
        showingBranch = true;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                if (Lizzie.board.getData().stones[Board.getIndex(i,j)] != Stone.EMPTY)
                    continue;
                if (branch.data.moveNumberList[Board.getIndex(i,j)] > maxBranchMoves())
                    continue;

                int stoneX = scaledMargin + squareLength * i;
                int stoneY = scaledMargin + squareLength * j;

                drawStone(g, gShadow, stoneX, stoneY, branch.data.stones[Board.getIndex(i, j)].unGhosted(), i, j);

            }
        }

        g.dispose();
        gShadow.dispose();
    }

    private MoveData mouseHoveredMove() {
        if (Lizzie.frame.mouseHoverCoordinate != null) {
            for (int i = 0; i < bestMoves.size(); i++) {
                MoveData move = bestMoves.get(i);
                int[] coord = Board.convertNameToCoordinates(move.coordinate);
                if (coord == null) {
                    continue;
                }

                if (coord[0] == Lizzie.frame.mouseHoverCoordinate[0] && coord[1] == Lizzie.frame.mouseHoverCoordinate[1]) {
                    return move;
                }
            }
        }
        return null;
    }

    private MoveData getBestMove() {
        return bestMoves.isEmpty() ? null : bestMoves.get(0);
    }

    /**
     * render the shadows and stones in correct background-foreground order
     */
    private void renderImages(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(cachedStonesShadowImage, x, y, null);
        if (Lizzie.config.showBranch) {
            g.drawImage(branchStonesShadowImage, x, y, null);
        }
        g.drawImage(cachedStonesImage, x, y, null);
        if (Lizzie.config.showBranch) {
            g.drawImage(branchStonesImage, x, y, null);
        }
    }

    /**
     * Draw move numbers and/or mark the last played move
     */
    private void drawMoveNumbers(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] lastMove = branch == null ? Lizzie.board.getLastMove() : branch.data.lastMove;
        if (!Lizzie.config.showMoveNumber && branch == null) {
            if (lastMove != null) {
                // mark the last coordinate
                int lastMoveMarkerRadius = stoneRadius / 2;
                int stoneX = x + scaledMargin + squareLength * lastMove[0];
                int stoneY = y + scaledMargin + squareLength * lastMove[1];

                // set color to the opposite color of whatever is on the board
                g.setColor(Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])].isWhite() ?
                        Color.BLACK : Color.WHITE);
                drawCircle(g, stoneX, stoneY, lastMoveMarkerRadius);
            } else if (lastMove == null && Lizzie.board.getData().moveNumber != 0 && !Lizzie.board.inScoreMode()) {
                g.setColor(Lizzie.board.getData().blackToPlay ? new Color(255, 255, 255, 150) : new Color(0, 0, 0, 150));
                g.fillOval(x + boardLength / 2 - 4 * stoneRadius, y + boardLength / 2 - 4 * stoneRadius, stoneRadius * 8, stoneRadius * 8);
                g.setColor(Lizzie.board.getData().blackToPlay ? new Color(0, 0, 0, 255) : new Color(255, 255, 255, 255));
                drawString(g, x + boardLength / 2, y + boardLength / 2, LizzieFrame.OpenSansRegularBase, "pass", stoneRadius * 4, stoneRadius * 6);
            }

            return;
        }

        int[] moveNumberList = branch == null ? Lizzie.board.getMoveNumberList() : branch.data.moveNumberList;

        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                int stoneX = x + scaledMargin + squareLength * i;
                int stoneY = y + scaledMargin + squareLength * j;

                Stone stoneAtThisPoint = branch == null ? Lizzie.board.getStones()[Board.getIndex(i, j)] :
                        branch.data.stones[Board.getIndex(i, j)];

                // don't write the move number if either: the move number is 0, or there will already be playout information written
                if (moveNumberList[Board.getIndex(i, j)] > 0 && !(branch != null && Lizzie.frame.mouseHoverCoordinate != null && i == Lizzie.frame.mouseHoverCoordinate[0] && j == Lizzie.frame.mouseHoverCoordinate[1])) {
                    if (lastMove != null && i == lastMove[0] && j == lastMove[1])
                        g.setColor(Color.RED.brighter());//stoneAtThisPoint.isBlack() ? Color.RED.brighter() : Color.BLUE.brighter());
                    else {
                        // Draw white letters on black stones nomally.
                        // But use black letters for showing black moves without stones.
                        boolean reverse = (moveNumberList[Board.getIndex(i, j)] > maxBranchMoves());
                        g.setColor(stoneAtThisPoint.isBlack() ^ reverse ? Color.WHITE : Color.BLACK);
                    }

                    String moveNumberString = moveNumberList[Board.getIndex(i, j)] + "";
                    drawString(g, stoneX, stoneY, LizzieFrame.OpenSansRegularBase, moveNumberString, (float) (stoneRadius * 1.4), (int) (stoneRadius * 1.4));
                }
            }
        }
    }

    /**
     * Draw all of Leelaz's suggestions as colored stones with winrate/playout statistics overlayed
     */
    private void drawLeelazSuggestions(Graphics2D g) {
        if (Lizzie.leelaz == null)
            return;

        final int MIN_ALPHA = 32;
        final int MIN_ALPHA_TO_DISPLAY_TEXT = 64;
        final int MAX_ALPHA = 240;
        final double HUE_SCALING_FACTOR = 3.0;
        final double ALPHA_SCALING_FACTOR = 5.0;
        final float GREEN_HUE = Color.RGBtoHSB(0,1,0,null)[0];
        final float CYAN_HUE = Color.RGBtoHSB(0,1,1,null)[0];

        if (!bestMoves.isEmpty()) {

            int maxPlayouts = 0;
            double maxWinrate = 0;
            for (MoveData move : bestMoves) {
                if (move.playouts > maxPlayouts)
                    maxPlayouts = move.playouts;
                if (move.winrate > maxWinrate)
                    maxWinrate = move.winrate;
            }

            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    MoveData move = null;

                    // this is inefficient but it looks better with shadows
                    for (MoveData m : bestMoves) {
                        int[] coord = Board.convertNameToCoordinates(m.coordinate);
                        // Handle passes
                        if (coord == null) {
                            continue;
                        }
                        if (coord[0] == i && coord[1] == j) {
                            move = m;
                            break;
                        }
                    }

                    if (move == null)
                        continue;

                    boolean isBestMove = bestMoves.get(0) == move;

                    if (move.playouts == 0) // this actually can happen
                        continue;

                    double percentPlayouts = (double) move.playouts / maxPlayouts;

                    int[] coordinates = Board.convertNameToCoordinates(move.coordinate);
                    int suggestionX = x + scaledMargin + squareLength * coordinates[0];
                    int suggestionY = y + scaledMargin + squareLength * coordinates[1];


                    // 0 = Reddest hue
                    float hue = isBestMove ? CYAN_HUE : (float) (-GREEN_HUE * Math.max(0, Math.log(percentPlayouts) / HUE_SCALING_FACTOR + 1));
                    float saturation = 0.75f; //saturation
                    float brightness = 0.85f; //brightness
                    int alpha = (int) (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * Math.max(0, Math.log(percentPlayouts) /
                            ALPHA_SCALING_FACTOR + 1));
//                    if (uiConfig.getBoolean("shadows-enabled"))
//                        alpha = 255;

                    Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
                    Color color = new Color(hsbColor.getRed(), hsbColor.getBlue(), hsbColor.getGreen(), alpha);

                    if (branch == null) {
                        drawShadow(g, suggestionX, suggestionY, true, (float) alpha / 255);
                        g.setColor(color);
                        fillCircle(g, suggestionX, suggestionY, stoneRadius);
                    }

                    if (branch == null || (isBestMove && Lizzie.frame.mouseHoverCoordinate != null && coordinates[0] == Lizzie.frame.mouseHoverCoordinate[0] && coordinates[1] == Lizzie.frame.mouseHoverCoordinate[1])) {
                        int strokeWidth = 1;
                        if (isBestMove != (move.winrate == maxWinrate)) {
                            strokeWidth = 2;
                            g.setColor(isBestMove ? Color.RED : Color.BLUE);
                            g.setStroke(new BasicStroke(strokeWidth));
                        } else {
                            g.setColor(color.darker());
                        }
                        drawCircle(g, suggestionX, suggestionY, stoneRadius - strokeWidth / 2);
                        g.setStroke(new BasicStroke(1));
                    }


                    if (branch == null && alpha >= MIN_ALPHA_TO_DISPLAY_TEXT || (Lizzie.frame.mouseHoverCoordinate != null && coordinates[0] == Lizzie.frame.mouseHoverCoordinate[0] && coordinates[1] == Lizzie.frame.mouseHoverCoordinate[1])) {
                        double roundedWinrate = Math.round(move.winrate * 10) / 10.0;
                        if (uiConfig.getBoolean("win-rate-always-black") && !Lizzie.board.getData().blackToPlay) {
                           roundedWinrate = 100.0 - roundedWinrate;
                        }
                        g.setColor(Color.BLACK);
                        if (branch != null && Lizzie.board.getData().blackToPlay)
                            g.setColor(Color.WHITE);
                        
                        String text;
                        if (Lizzie.config.handicapInsteadOfWinrate) {
                            text=String.format("%.2f", Lizzie.leelaz.winrateToHandicap(move.winrate));
                        } else {
                            text=String.format("%.1f", roundedWinrate);
                        }
                        
                        drawString(g, suggestionX, suggestionY, LizzieFrame.OpenSansSemiboldBase, Font.PLAIN, text, stoneRadius, stoneRadius * 1.5, 1);
                        drawString(g, suggestionX, suggestionY + stoneRadius * 2 / 5, LizzieFrame.OpenSansRegularBase, getPlayoutsString(move.playouts), (float) (stoneRadius * 0.8), stoneRadius * 1.4);
                    }
                }
            }


        }
    }

    private void drawNextMoves(Graphics2D g) {

        List<BoardHistoryNode> nexts = Lizzie.board.getHistory().getNexts();

        for (int i = 0; i < nexts.size(); i++) {
            int[] nextMove = nexts.get(i).getData().lastMove;
            if (nextMove == null) continue;
            if (Lizzie.board.getData().blackToPlay) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }
            int moveX = x + scaledMargin + squareLength * nextMove[0];
            int moveY = y + scaledMargin + squareLength * nextMove[1];
            if (i == 0) {
                g.setStroke(new BasicStroke(3.0f));
            }
            drawCircle(g, moveX, moveY, stoneRadius + 1); // slightly outside best move circle
            if (i == 0) {
                g.setStroke(new BasicStroke(1.0f));
            }
        }
    }

    private void drawWoodenBoard(Graphics2D g) {
        if (uiConfig.getBoolean("fancy-board")) {
            // fancy version
            int shadowRadius = (int) (boardLength * MARGIN / 6);
            Image boardImage = theme.getBoard();
            g.drawImage(boardImage == null ? theme.getBoard() : boardImage, x - 2 * shadowRadius, y - 2 * shadowRadius, boardLength + 4 * shadowRadius, boardLength + 4 * shadowRadius, null);
            g.setStroke(new BasicStroke(shadowRadius * 2));
            // draw border
            g.setColor(new Color(0, 0, 0, 50));
            g.drawRect(x - shadowRadius, y - shadowRadius, boardLength + 2 * shadowRadius, boardLength + 2 * shadowRadius);
            g.setStroke(new BasicStroke(1));

        } else {
            // simple version
            JSONArray boardColor = uiConfig.getJSONArray("board-color");
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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
        //boardLength -= boardLength*MARGIN/3; // account for the shadows we will draw around the edge of the board
//        if (boardLength < Board.BOARD_SIZE - 1)
//            throw new IllegalArgumentException("boardLength may not be less than " + (Board.BOARD_SIZE - 1) + ", but was " + boardLength);

        int scaledMargin;
        int availableLength;

        // decrease boardLength until the availableLength will result in square board intersections
        double margin = showCoordinates() ? MARGIN_WITH_COORDINATES : MARGIN;
        boardLength++;
        do {
            boardLength--;
            scaledMargin = (int) (margin * boardLength);
            availableLength = boardLength - 2 * scaledMargin;
        }
        while (!((availableLength - 1) % (Board.BOARD_SIZE - 1) == 0));
        // this will be true if BOARD_SIZE - 1 square intersections, plus one line, will fit

        return new int[]{boardLength, scaledMargin, availableLength};
    }

    private void drawShadow(Graphics2D g, int centerX, int centerY, boolean isGhost) {
        drawShadow(g, centerX, centerY, isGhost, 1);
    }

    private void drawShadow(Graphics2D g, int centerX, int centerY, boolean isGhost, float shadowStrength) {
        if (!uiConfig.getBoolean("shadows-enabled"))
            return;

        final int shadowSize = (int) (stoneRadius * 0.3 * uiConfig.getInt("shadow-size") / 100);
        final int fartherShadowSize = (int) (stoneRadius * 0.17 * uiConfig.getInt("shadow-size") / 100);


        final Paint TOP_GRADIENT_PAINT;
        final Paint LOWER_RIGHT_GRADIENT_PAINT;

        if (isGhost) {
            TOP_GRADIENT_PAINT = new RadialGradientPaint(new Point2D.Float(centerX, centerY),
                    stoneRadius + shadowSize, new float[]{((float) stoneRadius / (stoneRadius + shadowSize)) - 0.0001f, ((float) stoneRadius / (stoneRadius + shadowSize)), 1.0f}, new Color[]{
                    new Color(0, 0, 0, 0), new Color(50, 50, 50, (int) (120 * shadowStrength)), new Color(0, 0, 0, 0)
            });

            LOWER_RIGHT_GRADIENT_PAINT = new RadialGradientPaint(new Point2D.Float(centerX + shadowSize * 2 / 3, centerY + shadowSize * 2 / 3),
                    stoneRadius + fartherShadowSize, new float[]{0.6f, 1.0f}, new Color[]{
                    new Color(0, 0, 0, 180), new Color(0, 0, 0, 0)
            });
        } else {
            TOP_GRADIENT_PAINT = new RadialGradientPaint(new Point2D.Float(centerX, centerY),
                    stoneRadius + shadowSize, new float[]{0.3f, 1.0f}, new Color[]{
                    new Color(50, 50, 50, 150), new Color(0, 0, 0, 0)
            });
            LOWER_RIGHT_GRADIENT_PAINT = new RadialGradientPaint(new Point2D.Float(centerX + shadowSize, centerY + shadowSize),
                    stoneRadius + fartherShadowSize, new float[]{0.6f, 1.0f}, new Color[]{
                    new Color(0, 0, 0, 140), new Color(0, 0, 0, 0)
            });
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

    /**
     * Draws a stone centered at (centerX, centerY)
     */
    private void drawStone(Graphics2D g, Graphics2D gShadow, int centerX, int centerY, Stone color, int x, int y) {
//        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
//                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // if no shadow graphics is supplied, just draw onto the same graphics
        if (gShadow == null)
            gShadow = g;

        switch (color) {
            case BLACK:
            case BLACK_CAPTURED:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, false);
                    Image stone = theme.getBlackStone(new int[]{x, y});
                    g.drawImage(stone, centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(Color.BLACK);
                    fillCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            case WHITE:
            case WHITE_CAPTURED:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, false);
                    Image stone = theme.getWhiteStone(new int[]{x, y});
                    g.drawImage(stone, centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(Color.WHITE);
                    fillCircle(g, centerX, centerY, stoneRadius);
                    g.setColor(Color.BLACK);
                    drawCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            case BLACK_GHOST:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, true);
                    Image stone = theme.getBlackStone(new int[]{x, y});
                    g.drawImage(stone, centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(new Color(0, 0, 0));//, uiConfig.getInt("branch-stone-alpha")));
                    fillCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            case WHITE_GHOST:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, true);
                    Image stone = theme.getWhiteStone(new int[]{x, y});
                    g.drawImage(stone, centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(new Color(255, 255, 255));//, uiConfig.getInt("branch-stone-alpha")));
                    fillCircle(g, centerX, centerY, stoneRadius);
                    g.setColor(new Color(0, 0, 0));//, uiConfig.getInt("branch-stone-alpha")));
                    drawCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            default:
        }
    }

    /**
     * Fills in a circle centered at (centerX, centerY) with radius $radius$
     */
    private void fillCircle(Graphics2D g, int centerX, int centerY, int radius) {
        g.fillOval(centerX - radius, centerY - radius, 2 * radius + 1, 2 * radius + 1);
    }

    /**
     * Draws the outline of a circle centered at (centerX, centerY) with radius $radius$
     */
    private void drawCircle(Graphics2D g, int centerX, int centerY, int radius) {
        g.drawOval(centerX - radius, centerY - radius, 2 * radius + 1, 2 * radius + 1);
    }

    /**
     * Draws a string centered at (x, y) of font $fontString$, whose contents are $string$.
     * The maximum/default fontsize will be $maximumFontHeight$, and the length of the drawn string will be at most maximumFontWidth.
     * The resulting actual size depends on the length of $string$.
     * aboveOrBelow is a param that lets you set:
     * aboveOrBelow = -1 -> y is the top of the string
     * aboveOrBelow = 0  -> y is the vertical center of the string
     * aboveOrBelow = 1  -> y is the bottom of the string
     */
    private void drawString(Graphics2D g, int x, int y, Font fontBase, int style, String string, float maximumFontHeight, double maximumFontWidth, int aboveOrBelow) {

        Font font = makeFont(fontBase, style);

        // set maximum size of font
        font = font.deriveFont((float) (font.getSize2D() * maximumFontWidth / g.getFontMetrics(font).stringWidth(string)));
        font = font.deriveFont(Math.min(maximumFontHeight, font.getSize()));
        g.setFont(font);

        FontMetrics metrics = g.getFontMetrics(font);

        int height = metrics.getAscent() - metrics.getDescent();
        int verticalOffset;
        switch (aboveOrBelow) {
            case -1:
                verticalOffset = height / 2;
                break;

            case 1:
                verticalOffset = -height / 2;
                break;

            default:
                verticalOffset = 0;
        }
        // bounding box for debugging
        // g.drawRect(x-(int)maximumFontWidth/2, y - height/2 + verticalOffset, (int)maximumFontWidth, height+verticalOffset );
        g.drawString(string, x - metrics.stringWidth(string) / 2, y + height / 2 + verticalOffset);
    }

    private void drawString(Graphics2D g, int x, int y, Font fontBase, String string, float maximumFontHeight, double maximumFontWidth) {
        drawString(g, x, y, fontBase, Font.PLAIN, string, maximumFontHeight, maximumFontWidth, 0);
    }

    /**
     * @return a font with kerning enabled
     */
    private Font makeFont(Font fontBase, int style) {
        Font font = fontBase.deriveFont(style, 100);
        Map<TextAttribute, Object> atts = new HashMap<>();
        atts.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        return font.deriveFont(atts);
    }


    /**
     * @return a shorter, rounded string version of playouts. e.g. 345 -> 345, 1265 -> 1.3k, 44556 -> 45k, 133523 -> 134k, 1234567 -> 1.2m
     */
    private String getPlayoutsString(int playouts) {
        if (playouts >= 1_000_000) {
            double playoutsDouble = (double) playouts / 100_000; // 1234567 -> 12.34567
            return Math.round(playoutsDouble) / 10.0 + "m";
        } else if (playouts >= 10_000) {
            double playoutsDouble = (double) playouts / 1_000; // 13265 -> 13.265
            return Math.round(playoutsDouble) + "k";
        } else if (playouts >= 1_000) {
            double playoutsDouble = (double) playouts / 100; // 1265 -> 12.65
            return Math.round(playoutsDouble) / 10.0 + "k";
        } else {
            return String.valueOf(playouts);
        }
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
        this.boardLength = boardLength;
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
     * @return if there is a valid coordinate, an array (x, y) where x and y are between 0 and BOARD_SIZE - 1. Otherwise, returns null
     */
    public int[] convertScreenToCoordinates(int x, int y) {
        int marginLength; // the pixel width of the margins
        int boardLengthWithoutMargins; // the pixel width of the game board without margins

        // calculate a good set of boardLength, scaledMargin, and boardLengthWithoutMargins to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        setBoardLength(calculatedPixelMargins[0]);
        marginLength = calculatedPixelMargins[1];
        boardLengthWithoutMargins = calculatedPixelMargins[2];

        int squareSize = calculateSquareLength(boardLengthWithoutMargins);

        // transform the pixel coordinates to board coordinates
        x = (x - this.x - marginLength + squareSize / 2) / squareSize;
        y = (y - this.y - marginLength + squareSize / 2) / squareSize;

        // return these values if they are valid board coordinates
        if (Board.isValid(x, y))
            return new int[]{x, y};
        else
            return null;
    }

    /**
     * Calculate the boardLength of each intersection square
     *
     * @param availableLength the pixel board length of the game board without margins
     * @return the board length of each intersection square
     */
    private int calculateSquareLength(int availableLength) {
        return availableLength / (Board.BOARD_SIZE - 1);
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

    public boolean incrementDisplayedBranchLength(int n) {
        switch (displayedBranchLength) {
        case SHOW_NORMAL_BOARD:
        case SHOW_RAW_BOARD:
            return false;
        default:
            // force nonnegative
            displayedBranchLength = Math.max(0, displayedBranchLength + n);
            return true;
        }
    }

    public boolean isInside(int x1, int y1) {
        return (x <= x1 && x1 < x + boardLength && y <= y1 && y1 < y + boardLength);
    }

    private boolean showCoordinates() {
        return isMainBoard && Lizzie.frame.showCoordinates;
    }
}
