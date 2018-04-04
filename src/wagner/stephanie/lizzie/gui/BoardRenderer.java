package wagner.stephanie.lizzie.gui;

import org.json.JSONArray;
import org.json.JSONObject;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.Branch;
import wagner.stephanie.lizzie.analysis.MoveData;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.Stone;
import wagner.stephanie.lizzie.rules.Zobrist;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardRenderer {
    private static final double MARGIN = 0.03; // percentage of the boardLength to offset before drawing black lines
    private static final double STARPOINT_DIAMETER = 0.015;

    private int x, y;
    private int boardLength;

    private JSONObject uiConfig;

    private int scaledMargin, availableLength, squareLength, stoneRadius;
    private Branch branch;
    private List<MoveData> bestMoves;

    private BufferedImage cachedBackgroundImage = null;
    private boolean cachedBackgroundImageHasCoordinatesEnabled = false;

    private BufferedImage cachedStonesImage = null;
    private BufferedImage cachedStonesShadowImage = null;
    private Zobrist cachedZhash = new Zobrist(); // defaults to an empty board

    private BufferedImage branchStonesImage = null;
    private BufferedImage branchStonesShadowImage = null;


    public BoardRenderer() {
        uiConfig = Lizzie.config.config.getJSONObject("ui");
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
        drawBranch();
//        timer.lap("branch");

        renderImages(g);
//        timer.lap("rendering images");

        drawMoveNumbers(g);
//        timer.lap("movenumbers");
        if (!Lizzie.frame.isPlayingAgainstLeelaz)
            drawLeelazSuggestions(g);
//        timer.lap("leelaz");

//        timer.print();
    }

    /**
     * Calculate good values for boardLength, scaledMargin, availableLength, and squareLength
     */
    private void setupSizeParameters() {
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardLength = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableLength = calculatedPixelMargins[2];

        squareLength = calculateSquareLength(availableLength);
        stoneRadius = squareLength / 2 - 1;
    }

    /**
     * Draw the green background and go board with lines. We cache the image for a performance boost.
     */
    private void drawBackground(Graphics2D g0) {
        // draw the cached background image if frame size changes
        if (cachedBackgroundImage == null || cachedBackgroundImage.getWidth() != Lizzie.frame.getWidth() ||
                cachedBackgroundImage.getHeight() != Lizzie.frame.getHeight() ||
                cachedBackgroundImageHasCoordinatesEnabled != Lizzie.frame.showCoordinates) {

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

            // draw coordinates if enabled
            if (Lizzie.frame.showCoordinates) {
                g.setColor(Color.BLACK);
                String alphabet = "ABCDEFGHJKLMNOPQRST";
                for (int i = 0; i < Board.BOARD_SIZE; i++) {
                    drawString(g, x+scaledMargin+squareLength*i, y+scaledMargin/2, "Open Sans", ""+alphabet.charAt(i), stoneRadius*4/5, stoneRadius);
                    drawString(g, x+scaledMargin+squareLength*i, y-scaledMargin/2+boardLength, "Open Sans", ""+alphabet.charAt(i), stoneRadius*4/5, stoneRadius);
                }
                for (int i = 0; i < Board.BOARD_SIZE; i++) {
                    drawString(g, x+scaledMargin/2, y+scaledMargin+squareLength*i, "Open Sans", ""+(i+1), stoneRadius*4/5, stoneRadius);
                    drawString(g, x-scaledMargin/2+ +boardLength, y+scaledMargin+squareLength*i, "Open Sans", ""+(i+1), stoneRadius*4/5, stoneRadius);
                }
            }
            cachedBackgroundImageHasCoordinatesEnabled = Lizzie.frame.showCoordinates;
            g.dispose();
        }

        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g0.drawImage(cachedBackgroundImage, 0, 0, null);
    }

    /**
     * Draw the stones. We cache the image for a performance boost.
     */
    private void drawStones() {
        // draw a new image if frame size changes or board state changes
        if (cachedStonesImage==null || cachedStonesImage.getWidth() != boardLength ||
                cachedStonesImage.getHeight() != boardLength ||
                !cachedZhash.equals(Lizzie.board.getData().zobrist)) {

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
                    drawStone(g, gShadow, stoneX, stoneY, Lizzie.board.getStones()[Board.getIndex(i, j)]);
                }
            }

            cachedZhash = Lizzie.board.getData().zobrist;
            g.dispose();
            gShadow.dispose();
        }
    }

    /**
     * Draw the 'ghost stones' which show a variation Leelaz is thinking about
     */
    private void drawBranch() {
        branchStonesImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
        branchStonesShadowImage = new BufferedImage(boardLength, boardLength, BufferedImage.TYPE_INT_ARGB);
        branch=null;

        if (Lizzie.frame.isPlayingAgainstLeelaz) {
            return;
        }
        // calculate best moves and branch
        bestMoves = Lizzie.leelaz.getBestMoves();
        branch = null;

        // We can't early-out until now, since we need bestMoves for later
        if (!Lizzie.config.showVariation)
            return;

        Graphics2D g = (Graphics2D) branchStonesImage.getGraphics();
        Graphics2D gShadow = (Graphics2D) branchStonesShadowImage.getGraphics();

        if (Lizzie.frame.mouseHoverCoordinate != null) {
            for (int i = 0; i < bestMoves.size(); i++) {
                MoveData move = bestMoves.get(i);
                int[] coord = Board.convertNameToCoordinates(move.coordinate);

                if (coord[0] == Lizzie.frame.mouseHoverCoordinate[0] && coord[1] == Lizzie.frame.mouseHoverCoordinate[1]) {
                    branch = new Branch(Lizzie.board, move.variation);
                    break;
                }
            }
        }

        if (branch == null)
            return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                int stoneX = scaledMargin + squareLength * i;
                int stoneY = scaledMargin + squareLength * j;

                // check if board is empty to prevent overwriting stones if there are under-the-stones situations
                if (Lizzie.board.getStones()[Board.getIndex(i, j)] == Stone.EMPTY)
                    drawStone(g, gShadow, stoneX, stoneY, branch.data.stones[Board.getIndex(i, j)].unGhosted());
            }
        }

        g.dispose();
        gShadow.dispose();
    }

    /**
     * render the shadows and stones in correct background-foreground order
     */
    private void renderImages(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(cachedStonesShadowImage, x, y, null);
        g.drawImage(branchStonesShadowImage, x, y, null);
        g.drawImage(cachedStonesImage, x, y, null);
        g.drawImage(branchStonesImage, x, y, null);
    }

    /**
     * Draw move numbers and/or mark the last played move
     */
    private void drawMoveNumbers(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] lastMove = branch == null ? Lizzie.board.getLastMove() : branch.data.lastMove;
        if (!Lizzie.config.showMoveNumber && branch == null){
            if (lastMove != null) {
                // mark the last coordinate
                int lastMoveMarkerRadius = stoneRadius / 2;
                int stoneX = x + scaledMargin + squareLength * lastMove[0];
                int stoneY = y + scaledMargin + squareLength * lastMove[1];

                // set color to the opposite color of whatever is on the board
                g.setColor(Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])].isWhite() ?
                        Color.BLACK : Color.WHITE);
                drawCircle(g, stoneX, stoneY, lastMoveMarkerRadius);
            } else if (lastMove == null && Lizzie.board.getData().moveNumber != 0) {
                g.setColor(Lizzie.board.getData().blackToPlay ? new Color(255, 255, 255,150) : new Color(0,0,0,150));
                g.fillOval(x+boardLength/2-4*stoneRadius, y+boardLength/2 - 4*stoneRadius, stoneRadius*8, stoneRadius*8);
                g.setColor(Lizzie.board.getData().blackToPlay ? new Color(0,0,0,255) : new Color(255, 255, 255,255));
                drawString(g,x+boardLength/2, y+boardLength/2, "Open Sans", "pass", stoneRadius*4, stoneRadius*6);
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
                    else
                        g.setColor(stoneAtThisPoint.isBlack() ? Color.WHITE : Color.BLACK);

                    String moveNumberString = moveNumberList[Board.getIndex(i, j)] + "";
                    drawString(g, stoneX, stoneY, "Open Sans", moveNumberString, (float) (stoneRadius * 1.4), (int) (stoneRadius * 1.4));
                }
            }
        }
    }

    /**
     * Draw all of Leelaz's suggestions as colored stones with winrate/playout statistics overlayed
     */
    private void drawLeelazSuggestions(Graphics2D g) {
        final int MIN_ALPHA = 32;
        final int MIN_ALPHA_TO_DISPLAY_TEXT = 64;
        final int MAX_ALPHA = 240;
        final double HUE_SCALING_FACTOR = 3.0;
        final double ALPHA_SCALING_FACTOR = 5.0;

        if (!bestMoves.isEmpty()) {

            int maxPlayouts = 0;
            for (MoveData move : bestMoves) {
                if (move.playouts > maxPlayouts)
                    maxPlayouts = move.playouts;
            }

            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    MoveData move=null;

                    // this is inefficient but it looks better with shadows
                    for (MoveData m : bestMoves) {
                        int[] coord = Board.convertNameToCoordinates(m.coordinate);
                        if (coord[0] == i && coord[1] == j) {
                            move = m;
                            break;
                        }
                    }

                    if (move==null)
                        continue;

                    boolean isBestMove = bestMoves.get(0) == move;

                    if (move.playouts == 0) // this actually can happen
                        continue;

                    double percentPlayouts = (double) move.playouts / maxPlayouts;

                    int[] coordinates = Board.convertNameToCoordinates(move.coordinate);
                    int suggestionX = x + scaledMargin + squareLength * coordinates[0];
                    int suggestionY = y + scaledMargin + squareLength * coordinates[1];


                    // -0.32 = Greenest hue, 0 = Reddest hue
                    float hue = (float) (-0.32 * Math.max(0, Math.log(percentPlayouts) / HUE_SCALING_FACTOR + 1));
                    float saturation = 0.75f; //saturation
                    float brightness = 0.85f; //brightness
                    int alpha = (int) (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * Math.max(0, Math.log(percentPlayouts) /
                            ALPHA_SCALING_FACTOR + 1));
//                    if (uiConfig.getBoolean("shadows-enabled"))
//                        alpha = 255;

                    Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
                    Color color = new Color(hsbColor.getRed(), hsbColor.getBlue(), hsbColor.getGreen(), alpha);

                    if (branch == null) {
                        drawShadow(g, suggestionX, suggestionY, true, (float)alpha / 255);
                        g.setColor(color);
                        fillCircle(g, suggestionX, suggestionY, stoneRadius);
                    }

                    if (branch == null || (isBestMove && Lizzie.frame.mouseHoverCoordinate != null && coordinates[0] == Lizzie.frame.mouseHoverCoordinate[0] && coordinates[1] == Lizzie.frame.mouseHoverCoordinate[1])) {
                        // highlight LeelaZero's top recommended move
                        int strokeWidth = 1;
                        if (isBestMove) { // this is the best move
                            strokeWidth = 2;
                            g.setColor(Color.RED);
                            g.setStroke(new BasicStroke(strokeWidth));
                        } else {
                            g.setColor(color.darker());
                        }
                        drawCircle(g, suggestionX, suggestionY, stoneRadius - strokeWidth / 2);
                        g.setStroke(new BasicStroke(1));
                    }


                    if (branch == null && alpha >= MIN_ALPHA_TO_DISPLAY_TEXT || (Lizzie.frame.mouseHoverCoordinate != null && coordinates[0] == Lizzie.frame.mouseHoverCoordinate[0] && coordinates[1] == Lizzie.frame.mouseHoverCoordinate[1])) {
                        double roundedWinrate = Math.round(move.winrate * 10) / 10.0;
                        g.setColor(Color.BLACK);
                        if (branch != null && Lizzie.board.getData().blackToPlay)
                            g.setColor(Color.WHITE);

                        drawString(g, suggestionX, suggestionY, "Open Sans Semibold", Font.PLAIN, String.format("%.1f", roundedWinrate), stoneRadius, stoneRadius * 1.5, 1);
                        drawString(g, suggestionX, suggestionY + stoneRadius * 2 / 5, "Open Sans", getPlayoutsString(move.playouts), (float) (stoneRadius * 0.8), stoneRadius * 1.4);
                    }
                }
            }

            int[] nextMove = Lizzie.board.getNextMove();
            if (nextMove != null) {
                if (Lizzie.board.getData().blackToPlay) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                int moveX = x + scaledMargin + squareLength * nextMove[0];
                int moveY = y + scaledMargin + squareLength * nextMove[1];
                drawCircle(g, moveX, moveY, stoneRadius + 1); // slightly outside best move circle
            }
        }
    }

    private void drawWoodenBoard(Graphics2D g) {
        if (uiConfig.getBoolean("fancy-board")) {
            // fancy version
            try {
                int shadowRadius = (int) (boardLength * MARGIN / 6);
                g.drawImage(ImageIO.read(new File("assets/board.png")), x - 2 * shadowRadius, y - 2 * shadowRadius, boardLength + 4 * shadowRadius, boardLength + 4 * shadowRadius, null);
                g.setStroke(new BasicStroke(shadowRadius * 2));
                // draw border
                g.setColor(new Color(0, 0, 0, 50));
                g.drawRect(x - shadowRadius, y - shadowRadius, boardLength + 2 * shadowRadius, boardLength + 2 * shadowRadius);
                g.setStroke(new BasicStroke(1));
            } catch (IOException e) {
                e.printStackTrace();
            }

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
    private static int[] calculatePixelMargins(int boardLength) {
        if (boardLength < Board.BOARD_SIZE - 1)
            throw new IllegalArgumentException("boardLength may not be less than " + (Board.BOARD_SIZE - 1) + ", but was " + boardLength);

        int scaledMargin;
        int availableLength;

        // decrease boardLength until the availableLength will result in square board intersections
        boardLength++;
        do {
            boardLength--;
            scaledMargin = (int) (MARGIN * boardLength);
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
        final int fartherShadowSize = (int) (stoneRadius * 0.17* uiConfig.getInt("shadow-size") / 100);


        final Paint TOP_GRADIENT_PAINT;
        final Paint LOWER_RIGHT_GRADIENT_PAINT;

        if (isGhost) {
            TOP_GRADIENT_PAINT = new RadialGradientPaint(new Point2D.Float(centerX, centerY),
                    stoneRadius + shadowSize, new float[]{((float)stoneRadius / (stoneRadius+shadowSize))-0.0001f, ((float)stoneRadius / (stoneRadius+shadowSize)), 1.0f}, new Color[]{
                    new Color(0, 0, 0, 0), new Color(50, 50, 50, (int)(120 * shadowStrength)), new Color(0, 0, 0, 0)
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
    private void drawStone(Graphics2D g, Graphics2D gShadow, int centerX, int centerY, Stone color) {
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
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, false);
                    try {
                        g.drawImage(ImageIO.read(new File("assets/black0.png")), centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(Color.BLACK);
                    fillCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            case WHITE:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, false);
                    try {
                        g.drawImage(ImageIO.read(new File("assets/white0.png")), centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    try {
                        g.drawImage(ImageIO.read(new File("assets/black0.png")), centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    drawShadow(gShadow, centerX, centerY, true);
                    g.setColor(new Color(0, 0, 0));//, uiConfig.getInt("branch-stone-alpha")));
                    fillCircle(g, centerX, centerY, stoneRadius);
                }
                break;

            case WHITE_GHOST:
                if (uiConfig.getBoolean("fancy-stones")) {
                    drawShadow(gShadow, centerX, centerY, true);
                    try {
                        g.drawImage(ImageIO.read(new File("assets/white0.png")), centerX - stoneRadius, centerY - stoneRadius, stoneRadius * 2 + 1, stoneRadius * 2 + 1, null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
    private void drawString(Graphics2D g, int x, int y, String fontString, int style, String string, float maximumFontHeight, double maximumFontWidth, int aboveOrBelow) {

        Font font = makeFont(fontString, style);

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

    private void drawString(Graphics2D g, int x, int y, String fontString, String string, float maximumFontHeight, double maximumFontWidth) {
        drawString(g, x, y, fontString, Font.PLAIN, string, maximumFontHeight, maximumFontWidth, 0);
    }

    /**
     * @return a font with kerning enabled
     */
    private Font makeFont(String fontString, int style) {
        Font font = new Font(fontString, style, 100);
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
     *
     */
    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
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
        boardLength = calculatedPixelMargins[0];
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
}
