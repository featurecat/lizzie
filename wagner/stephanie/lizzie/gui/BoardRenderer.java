package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.analysis.MoveData;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Stone;

import java.awt.*;
import java.util.List;

public class BoardRenderer {
    private static final double MARGIN = 0.035; // percentage of the boardWidth to offset before drawing black lines
    private static final double STAR_POINT_WIDTH = 0.015;

    private int x, y;
    private int boardWidth;

    /**
     * Calculates the widths and pixel margins from a given boardWidth.
     * Precondition: the boardWidth must be boardWidth >= BOARD_SIZE - 1
     *
     * @return an array containing the three outputs: newWidth, scaledMargin, availableWidth
     */
    public static int[] calculatePixelMargins(int size) {
        if (size < Board.BOARD_SIZE - 1)
            throw new IllegalArgumentException("boardWidth may not be less than " + (Board.BOARD_SIZE - 1) + ", but was " + size);

        int scaledMargin;
        int availableWidth;

        // decrease boardWidth until the availableWidth will result in square board intersections
        size++;
        do {
            size--;
            scaledMargin = (int) (MARGIN * size);
            availableWidth = size - 2 * scaledMargin;
        }
        while (!((availableWidth - 1) % (Board.BOARD_SIZE - 1) == 0));
        // this will be true if BOARD_SIZE - 1 square intersections plus one line will fit

        return new int[]{size, scaledMargin, availableWidth};
    }

    /**
     * Generates a random stone arrangement -- not necessarily a legal one
     *
     * @return a random stone array
     */
    public static Stone[] getRandomStones() {
        Stone[] stones = new Stone[Board.BOARD_SIZE * Board.BOARD_SIZE];
        for (int i = 0; i < stones.length; i++) {
            stones[i] = Math.random() > 0.5 ? Stone.EMPTY : Math.random() > 0.5 ? Stone.BLACK : Stone.WHITE;
        }
        return stones;
    }

    /**
     * Draw a go board
     *
     * @param g graphics instance
     */
    public void draw(Graphics g) {
        int scaledMargin; // the pixel boardWidth of the margins
        int availableWidth; // the pixel boardWidth of the game board without margins

        // calculate a good set of boardWidth, scaledMargin, and availableWidth to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardWidth = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableWidth = calculatedPixelMargins[2];

        // draw the wooden background
        g.setColor(Color.ORANGE.darker());
        g.fillRect(x, y, boardWidth, boardWidth);

        // draw the lines
        g.setColor(Color.BLACK);
        int squareSize = calculateSquareSize(availableWidth);
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            g.drawLine(x + scaledMargin, y + scaledMargin + squareSize * i,
                    x + scaledMargin + availableWidth - 1, y + scaledMargin + squareSize * i);
        }
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            g.drawLine(x + scaledMargin + squareSize * i, y + scaledMargin,
                    x + scaledMargin + squareSize * i, y + scaledMargin + availableWidth - 1);
        }

        // draw the star points
        int starPointRadius = (int) (STAR_POINT_WIDTH * boardWidth) / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int centerX = x + scaledMargin + squareSize * (3 + 6 * i) - starPointRadius;
                int centerY = y + scaledMargin + squareSize * (3 + 6 * j) - starPointRadius;
                g.fillOval(centerX, centerY, 2 * starPointRadius, 2 * starPointRadius);
            }
        }

        // draw the stones
        int stoneRadius = squareSize / 2 - 1;
        if (Lizzie.board != null) {
            for (int i = 0; i < Board.BOARD_SIZE; i++) {
                for (int j = 0; j < Board.BOARD_SIZE; j++) {
                    // TODO for some reason these are always off center?!
                    int stoneX = x + scaledMargin + squareSize * i - stoneRadius;
                    int stoneY = y + scaledMargin + squareSize * j - stoneRadius;

                    switch (Lizzie.board.getStones()[Board.getIndex(i, j)]) {
                        case EMPTY:
                            break;
                        case BLACK:
                            g.setColor(Color.BLACK);
                            g.fillOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            break;
                        case WHITE:
                            g.setColor(Color.WHITE);
                            g.fillOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            g.setColor(Color.BLACK);
                            g.drawOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                            break;
                        default:
                    }


                }
            }

            // mark the last coordinate
            int[] lastMove = Lizzie.board.getLastMove();
            if (lastMove != null) {
                int circleRadius = squareSize / 4;

                int stoneX = x + scaledMargin + squareSize * lastMove[0] - circleRadius;
                int stoneY = y + scaledMargin + squareSize * lastMove[1] - circleRadius;

                // set color to the opposite color of whatever is on the board
                g.setColor(Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])] == Stone.WHITE ?
                        Color.BLACK : Color.WHITE);
                g.drawOval(stoneX, stoneY, circleRadius * 2 + 1, circleRadius * 2 + 1);
            }
        }

        // draw Leelaz suggestions
        // TODO clean up this MESS
        List<MoveData> bestMoves = Lizzie.leelaz.getBestMoves();
        if (!bestMoves.isEmpty()) {
            final double MIN_ACCEPTABLE_PLAYOUTS = 0.0;

//            int minPlayouts = Integer.MAX_VALUE;
            int maxPlayouts = 0;
//            double minWinrate = Double.MAX_VALUE;
            double maxWinrate = 0;
            for (MoveData move : bestMoves) {
                if (move.playouts < MIN_ACCEPTABLE_PLAYOUTS)
                    continue;
//                if (move.playouts < minPlayouts)
//                    minPlayouts = move.playouts;
                if (move.playouts > maxPlayouts)
                    maxPlayouts = move.playouts;
//                if (move.winrate < minWinrate)
//                    minWinrate = move.winrate;
                if (move.winrate > maxWinrate)
                    maxWinrate = move.winrate;
            }

            final int MIN_ALPHA = 24;
            final int MAX_ALPHA = 230;

            for (int i = 0; i < bestMoves.size(); i++) {
                MoveData move = bestMoves.get(i);
                double percentPlayouts = (Math.max(0, (double)move.playouts) / Math.max(1, maxPlayouts));
                double percentWinrate = (Math.max(0, move.winrate - maxWinrate + 5) / Math.max(1, 5));
                if (percentPlayouts < MIN_ACCEPTABLE_PLAYOUTS) {
                    continue;
                }
                int[] coordinates = Board.convertNameToCoordinates(move.coordinate);
                int suggestionX = x + scaledMargin + squareSize * coordinates[0] - stoneRadius;
                int suggestionY = y + scaledMargin + squareSize * coordinates[1] - stoneRadius;


                int alpha = (int)(MIN_ALPHA + (MAX_ALPHA-MIN_ALPHA)*Math.max(0, Math.log(percentPlayouts) / 5 + 1));

                //(int) ((double)(bestMoves.size() - i)/bestMoves.size() * MAX_ALPHA);//(int) Math.min(Math.max(MIN_ALPHA, percentPlayouts * MAX_ALPHA), MAX_ALPHA);
                //Color color = new Color(red, green, blue, alpha);
                float hue = (float)(-0.3*Math.max(0, Math.log(percentPlayouts) / 3 + 1));//0.51944f; //hue
                float saturation = 0.75f; //saturation
                float brightness = 0.8f; //brightness

                Color myRGBColor = Color.getHSBColor(hue, saturation, brightness);
//                int green = (int)(255*Math.max(0, Math.log(percentPlayouts) / 3 + 1));
//                int red = 255-green;
//                int blue = 0;
//                Color color = new Color(red,green,blue,alpha);
                Color color = new Color(myRGBColor.getRed(), myRGBColor.getBlue(), myRGBColor.getGreen(), alpha);

                g.setColor(color);
                g.fillOval(suggestionX, suggestionY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
                g.setColor(color.darker());
                g.drawOval(suggestionX, suggestionY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);

                if (alpha > 64) {
                    g.setColor(Color.BLACK);
                    Font font = new Font("Sans Serif", Font.BOLD, 12);
                    g.setFont(font);
                    String winrateString = String.format("%.0f", move.winrate);
                    g.drawString(winrateString + "%", suggestionX+ stoneRadius - g.getFontMetrics(font).stringWidth(winrateString), suggestionY + stoneRadius+font.getSize()/2 );
                    font = new Font("Sans Serif", Font.PLAIN, 8);
                    g.setFont(font);
                    String playouts = ""+move.playouts;
                    g.drawString("" + move.playouts, suggestionX+stoneRadius - g.getFontMetrics(font).stringWidth(playouts)/2, suggestionY + stoneRadius+ 14);
                }
            }
        }
    }

    private int[] calculatePixelMargins() {
        return calculatePixelMargins(boardWidth);
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
     * Set the maximum boardWidth to render the board
     *
     * @param boardWidth the boardWidth of the board
     */
    public void setBoardWidth(int boardWidth) {
        this.boardWidth = boardWidth;
    }

    /**
     * Converts a location on the screen to a location on the board
     *
     * @param x x pixel coordinate
     * @param y y pixel coordinate
     * @return if there is a valid coordinate, an array (x, y) where x and y are between 0 and BOARD_SIZE - 1. Otherwise, returns null
     */
    public int[] convertScreenToCoordinates(int x, int y) {
        int marginWidth; // the pixel width of the margins
        int boardWidthWithoutMargins; // the pixel width of the game board without margins

        // calculate a good set of boardWidth, scaledMargin, and boardWidthWithoutMargins to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        boardWidth = calculatedPixelMargins[0];
        marginWidth = calculatedPixelMargins[1];
        boardWidthWithoutMargins = calculatedPixelMargins[2];

        int squareSize = calculateSquareSize(boardWidthWithoutMargins);

        // transform the pixel coordinates to board coordinates
        x = (x - this.x - marginWidth + squareSize / 2) / squareSize;
        y = (y - this.y - marginWidth + squareSize / 2) / squareSize;

        // return these values if they are valid board coordinates
        if (Board.isValid(x, y))
            return new int[]{x, y};
        else
            return null;
    }

    /**
     * Calculate the boardWidth of each intersection square
     *
     * @param availableWidth the pixel boardWidth of the game board without margins
     * @return the boardWidth of each intersection square
     */
    public int calculateSquareSize(int availableWidth) {
        return availableWidth / (Board.BOARD_SIZE - 1);
    }
}