package wagner.stephanie.lizzie.gui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wagner.stephanie.benchmark.Stopwatch;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.Branch;
import wagner.stephanie.lizzie.analysis.MoveData;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.Stone;
import wagner.stephanie.lizzie.rules.Zobrist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class BoardRenderer {
    private static final double MARGIN = 0.03; // percentage of the boardLength to offset before drawing black lines
    private static final double STARPOINT_DIAMETER = 0.015;

    private int x, y;
    private int boardLength;

    private JSONObject uiConfig;

    static {
        // load fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./OpenSans-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("./OpenSans-Semibold.ttf")));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    public BoardRenderer() {
        uiConfig = Lizzie.config.config.getJSONObject("ui");
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

    /**
     * Draws a stone centered at (centerX, centerY)
     */
    private void drawStone(Graphics2D g, int centerX, int centerY, Stone color, int squareLength) {
        int stoneRadius = squareLength / 2 - 1;

        switch (color) {
            case BLACK:
                g.setColor(Color.BLACK);
                fillCircle(g, centerX, centerY, stoneRadius);
                break;

            case WHITE:
                g.setColor(Color.WHITE);
                fillCircle(g, centerX, centerY, stoneRadius);
                g.setColor(Color.BLACK);
                drawCircle(g, centerX, centerY, stoneRadius);
                break;

            case BLACK_GHOST:
                g.setColor(new Color(0, 0, 0, uiConfig.getInt("branch-stone-alpha")));
                fillCircle(g, centerX, centerY, stoneRadius);
                break;

            case WHITE_GHOST:
                g.setColor(new Color(255, 255, 255, uiConfig.getInt("branch-stone-alpha")));
                fillCircle(g, centerX, centerY, stoneRadius);
                g.setColor(new Color(0, 0, 0, uiConfig.getInt("branch-stone-alpha")));
                drawCircle(g, centerX, centerY, stoneRadius);
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
     */
    private void drawString(Graphics2D g, int x, int y, String fontString, String string, int maximumFontHeight, int maximumFontWidth) {
        Font font;
        maximumFontHeight++;
        do {
            font = new Font(fontString, Font.PLAIN, maximumFontHeight--);
            g.setFont(font);
        } while (g.getFontMetrics(font).stringWidth(string) > maximumFontWidth);
        g.drawString(string, x - g.getFontMetrics(font).stringWidth(string) / 2, y + font.getSize() / 3);
    }

    private BufferedImage cachedBackgroundImage = null;

    // TODO pull scaledMargin, availableLength, squareLength out into class fields

    /**
     * Draw the green background and go board with lines. We should cache the image for a performance boost
     */
    private void drawBackground(Graphics2D g0, int scaledMargin, int availableLength, int squareLength) {
        // draw the cached background image if frame size changes
        if (cachedBackgroundImage == null || cachedBackgroundImage.getWidth() != Lizzie.frame.getWidth() ||
                cachedBackgroundImage.getHeight() != Lizzie.frame.getHeight()) {

            cachedBackgroundImage = new BufferedImage(Lizzie.frame.getWidth(), Lizzie.frame.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cachedBackgroundImage.createGraphics();

            // draw the wooden background
            JSONArray boardColor = uiConfig.getJSONArray("board-color");
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(new Color(boardColor.getInt(0), boardColor.getInt(1), boardColor.getInt(2)));
            g.fillRect(x, y, boardLength, boardLength);

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
        }

        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g0.drawImage(cachedBackgroundImage, 0, 0, null);
    }


    private BufferedImage cachedStonesImage = null;
    private Zobrist cachedZhash = new Zobrist(); // defaults to an empty board

    /**
     * draw the stones. we should cache the image for a performance boost
     */
    private void drawStones(Graphics2D g0, int scaledMargin, int availableLength, int squareLength) {
        // draw a new image if frame size changes or board state changes
        if (cachedStonesImage == null || cachedStonesImage.getWidth() != Lizzie.frame.getWidth() ||
                cachedStonesImage.getHeight() != Lizzie.frame.getHeight() ||
                !cachedZhash.equals(Lizzie.board.getData().zobrist)) {

            cachedStonesImage = new BufferedImage(Lizzie.frame.getWidth(), Lizzie.frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cachedStonesImage.createGraphics();

            // we need antialiasing to make the stones pretty. Java is a bit slow at antialiasing; that's why we want the cache
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    int stoneX = x + scaledMargin + squareLength * i;
                    int stoneY = y + scaledMargin + squareLength * j;
                    drawStone(g, stoneX, stoneY, Lizzie.board.getStones()[Board.getIndex(i, j)], squareLength);
                }
            }

            cachedZhash = Lizzie.board.getData().zobrist;
        }

        g0.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g0.drawImage(cachedStonesImage, 0, 0, null);
    }

    /**
     * Draw the 'ghost stones' which show a variation Leelaz is thinking about
     */
    private void drawBranch(Graphics2D g, int scaledMargin, int availableLength, int squareLength, Branch branch) {
        if (branch == null)
            return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                int stoneX = x + scaledMargin + squareLength * i;
                int stoneY = y + scaledMargin + squareLength * j;

                // check if board is empty to prevent overwriting stones if there are under-the-stones situations
                if (Lizzie.board.getStones()[Board.getIndex(i, j)] == Stone.EMPTY)
                    drawStone(g, stoneX, stoneY, branch.data.stones[Board.getIndex(i, j)], squareLength);
            }
        }
    }

    /**
     * Draw move numbers and/or mark the last played move
     */
    private void drawMoveNumbers(Graphics2D g, int scaledMargin, int availableLength, int squareLength, Branch branch) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[] lastMove = branch == null ? Lizzie.board.getLastMove() : branch.data.lastMove;
        if (!Lizzie.config.showMoveNumber && lastMove != null) {
            // mark the last coordinate
            int lastMoveMarkerRadius = squareLength / 4;
            int stoneX = x + scaledMargin + squareLength * lastMove[0];
            int stoneY = y + scaledMargin + squareLength * lastMove[1];

            // set color to the opposite color of whatever is on the board
            Stone[] stones = branch == null ? Lizzie.board.getStones() : branch.data.stones;
            g.setColor(stones[Board.getIndex(lastMove[0], lastMove[1])].isWhite() ? Color.BLACK : Color.WHITE);
            drawCircle(g, stoneX, stoneY, lastMoveMarkerRadius);

            return;
        }

        int[] moveNumberList = branch == null ? Lizzie.board.getMoveNumberList() : branch.data.moveNumberList;

        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                int stoneX = x + scaledMargin + squareLength * i;
                int stoneY = y + scaledMargin + squareLength * j;

                Stone stoneAtThisPoint = branch == null ? Lizzie.board.getStones()[Board.getIndex(i, j)] :
                        branch.data.stones[Board.getIndex(i, j)];

                if (moveNumberList[Board.getIndex(i, j)] != 0) {
                    if (lastMove != null && i == lastMove[0] && j == lastMove[1])
                        g.setColor(stoneAtThisPoint.isBlack() ? Color.RED : Color.BLUE);
                    else
                        g.setColor(stoneAtThisPoint.isBlack() ? Color.WHITE : Color.BLACK);

                    String moveNumberString = moveNumberList[Board.getIndex(i, j)] + "";
                    drawString(g, stoneX, stoneY, "Open Sans", moveNumberString, (int) (squareLength * 0.75),
                            (int) (squareLength * 0.85));
                }
            }
        }
    }

    /**
     * Draw all of Leelaz's suggestions as colored stones with winrate/playout statistics overlayed
     */
    private void drawLeelazSuggestions(Graphics2D g, int scaledMargin, int availableLength, int squareLength, List<MoveData> bestMoves, Branch branch) {
        int stoneRadius = squareLength / 2 - 1;

        // TODO clean up this MESS
        if (!bestMoves.isEmpty() && branch == null) {
            final double MIN_ACCEPTABLE_PLAYOUTS = 0.0;

            int maxPlayouts = 0;
            for (MoveData move : bestMoves) {
                if (move.playouts < MIN_ACCEPTABLE_PLAYOUTS)
                    continue;
                if (move.playouts > maxPlayouts)
                    maxPlayouts = move.playouts;
            }

            final int MIN_ALPHA = 32;
            final int MAX_ALPHA = 240;

            for (int i = 0; i < bestMoves.size(); i++) {
                MoveData move = bestMoves.get(i);
                double percentPlayouts = (Math.max(0, (double) move.playouts) / Math.max(1, maxPlayouts));
                if (percentPlayouts < MIN_ACCEPTABLE_PLAYOUTS) {
                    continue;
                }
                int[] coordinates = Board.convertNameToCoordinates(move.coordinate);
                int suggestionX = x + scaledMargin + squareLength * coordinates[0] - stoneRadius;
                int suggestionY = y + scaledMargin + squareLength * coordinates[1] - stoneRadius;

                // -0.32 = Greenest, 0 = Reddest
                float hue = (float) (-0.32 * Math.max(0, Math.log(percentPlayouts) / 3 + 1));
                float saturation = 0.75f; //saturation
                float brightness = 0.85f; //brightness
                int alpha = (int) (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * Math.max(0, Math.log(percentPlayouts) / 5 + 1));

                Color hsbColor = Color.getHSBColor(hue, saturation, brightness);
                Color color = new Color(hsbColor.getRed(), hsbColor.getBlue(), hsbColor.getGreen(), alpha);

                g.setColor(color);
                g.fillOval(suggestionX, suggestionY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                int strokeWidth = 0;
                // highlight the top recommended move
                if (i == 0) {
                    strokeWidth = 2;
                    g.setColor(Color.RED);
                    g.setStroke(new BasicStroke(strokeWidth));
                } else {
                    g.setColor(color.darker());
                }
                g.drawOval(suggestionX + strokeWidth / 2, suggestionY + strokeWidth / 2, stoneRadius * 2 + 1 - strokeWidth, stoneRadius * 2 + 1 - strokeWidth);
                g.setStroke(new BasicStroke(1));

                if (alpha > 64) {
                    g.setColor(Color.BLACK);
                    Font font = new Font("Open Sans Semibold", Font.PLAIN, (int) (stoneRadius * 0.85));
                    g.setFont(font);
                    String winrateString = String.format("%.0f", move.winrate) + "%";
                    g.drawString(winrateString, suggestionX + stoneRadius - g.getFontMetrics(font).stringWidth(winrateString) / 2, suggestionY + stoneRadius);
                    String playouts;
                    int fontSize = (int) (stoneRadius * 0.8);
                    do {
                        font = new Font("Open Sans", Font.PLAIN, fontSize--);
                        g.setFont(font);
                        playouts = "" + move.playouts;
                    } while (g.getFontMetrics(font).stringWidth(playouts) > stoneRadius * 1.7);

                    g.drawString("" + move.playouts, suggestionX + stoneRadius - g.getFontMetrics(font).stringWidth(playouts) / 2, suggestionY + stoneRadius + font.getSize());
                }
            }
        }
    }

    /**
     * Draw a go board
     *
     * @param g graphics instance
     */
    public void draw(Graphics2D g) {
        if (Lizzie.frame == null || Lizzie.board == null)
            return;

        Stopwatch watch = new Stopwatch();
        // Get best moves for foucs and winrate show (TODO: what is foucs?)
        List<MoveData> bestMoves = Lizzie.leelaz.getBestMoves();
        Branch branch = null;

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

        int scaledMargin; // the pixel length of the margins
        int availableLength; // the pixel length of the game board without margins
        int squareLength; // the length of each square

        // calculate a good set of boardLength, scaledMargin, and availableLength to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardLength = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableLength = calculatedPixelMargins[2];

        squareLength = calculateSquareLength(availableLength);

        watch.lap("initial");
        // cached drawing methods
        drawBackground(g, scaledMargin, availableLength, squareLength);
        watch.lap("background");
        drawStones(g, scaledMargin, availableLength, squareLength);
        watch.lap("stones");

        // non-cached drawing methods
        drawBranch(g, scaledMargin, availableLength, squareLength, branch);
        watch.lap("branch");
        drawMoveNumbers(g, scaledMargin, availableLength, squareLength, branch);
        watch.lap("move numbers");

        drawLeelazSuggestions(g, scaledMargin, availableLength, squareLength, bestMoves, branch);
        watch.lap("leelaz suggestions");
        watch.print();
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
    public int calculateSquareLength(int availableLength) {
        return availableLength / (Board.BOARD_SIZE - 1);
    }
}

/*

        // If show move number is enable
        // Last move color is different
        if (lastMove != null) {
            int stoneX = x + scaledMargin + squareLength * lastMove[0] - stoneRadius;
            int stoneY = y + scaledMargin + squareLength * lastMove[1] - stoneRadius;

            Stone currentColor = Stone.EMPTY;

            if (branch == null) {
                currentColor = Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])];
            } else {
                currentColor = branch.data.stones[Board.getIndex(lastMove[0], lastMove[1])];
            }

            Color color = currentColor.equals(Stone.BLACK) ? Color.RED : Color.BLUE;
            if (Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])] == Stone.EMPTY) {
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), branchAlpha);
            }
            g.setColor(color);
            String moveNumberString = String.valueOf(moveNumberList[Board.getIndex(lastMove[0], lastMove[1])]);
            int fontSize = (int) (stoneRadius * 1.5);
            Font font;
            do {
                font = new Font("Open Sans", Font.PLAIN, fontSize--);
                g.setFont(font);
            } while (g.getFontMetrics(font).stringWidth(moveNumberString) > stoneRadius * 1.7);
            g.drawString(moveNumberString,
                    stoneX + stoneRadius - g.getFontMetrics(font).stringWidth(moveNumberString) / 2, stoneY + stoneRadius + fontSize / 3);
        }
 */