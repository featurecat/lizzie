package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Stone;

import java.awt.*;

public class BoardRenderer {
    private static final double MARGIN = 0.035; // percentage of the size to offset before drawing black lines
    private static final double STAR_POINT_WIDTH = 0.015;

    private int x, y;
    private int size;

    /**
     * Calculates the widths and pixel margins from a given size.
     * Precondition: the size must be size >= BOARD_SIZE - 1
     *
     * @return an array containing the three outputs: newWidth, scaledMargin, availableWidth
     */
    public static int[] calculatePixelMargins(int size) {
        if (size < Board.BOARD_SIZE - 1)
            throw new IllegalArgumentException("size may not be less than " + (Board.BOARD_SIZE - 1) + ", but was " + size);

        int scaledMargin;
        int availableWidth;

        // decrease size until the availableWidth will result in square board intersections
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
        int scaledMargin; // the pixel size of the margins
        int availableWidth; // the pixel size of the game board without margins

        // calculate a good set of size, scaledMargin, and availableWidth to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        size = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableWidth = calculatedPixelMargins[2];

        // draw the wooden background
        g.setColor(Color.ORANGE.darker());
        g.fillRect(x, y, size, size);

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
        int starPointRadius = (int) (STAR_POINT_WIDTH * size) / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int centerX = x + scaledMargin + squareSize * (3 + 6 * i) - starPointRadius;
                int centerY = y + scaledMargin + squareSize * (3 + 6 * j) - starPointRadius;
                g.fillArc(centerX, centerY, 2 * starPointRadius, 2 * starPointRadius, 0, 360);
            }
        }

        // draw the stones
        if (Lizzie.board != null) {
            int stoneRadius = squareSize / 2 - 1;

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

            // mark the last move
            int[] lastMove = Lizzie.board.getLastMove();
            if (lastMove != null) {
                stoneRadius = squareSize / 4;

                int stoneX = x + scaledMargin + squareSize * lastMove[0] - stoneRadius;
                int stoneY = y + scaledMargin + squareSize * lastMove[1] - stoneRadius;

                // set color to the opposite color of whatever is on the board
                g.setColor(Lizzie.board.getStones()[Board.getIndex(lastMove[0], lastMove[1])] == Stone.WHITE ?
                        Color.BLACK : Color.WHITE);
                Graphics2D g2 = (Graphics2D) g;
                g2.drawOval(stoneX, stoneY, stoneRadius * 2 + 1, stoneRadius * 2 + 1);
            }
        }
    }

    public int[] calculatePixelMargins() {
        return calculatePixelMargins(size);
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
     * Set the maximum size to render the board
     *
     * @param size the size of the board
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Converts a location on the screen to a location on the board
     *
     * @param x x pixel coordinate
     * @param y y pixel coordinate
     * @return if there is a valid coordinate, an array (x, y) where x and y are between 0 and BOARD_SIZE - 1. Otherwise, returns null
     */
    public int[] convertScreenToCoordinates(int x, int y) {
        int scaledMargin; // the pixel size of the margins
        int availableWidth; // the pixel size of the game board without margins

        // calculate a good set of size, scaledMargin, and availableWidth to use
        int[] calculatedPixelMargins = calculatePixelMargins();
        size = calculatedPixelMargins[0];
        scaledMargin = calculatedPixelMargins[1];
        availableWidth = calculatedPixelMargins[2];

        int squareSize = calculateSquareSize(availableWidth);

        // transform the pixel coordinates to board coordinates
        x = (x - this.x - scaledMargin + squareSize / 2) / squareSize;
        y = (y - this.y - scaledMargin + squareSize / 2) / squareSize;

        // return these values if they are valid board coordinates
        if (Board.isValid(x, y))
            return new int[]{x, y};
        else
            return null;
    }

    /**
     * Calculate the size of each intersection square
     *
     * @param availableWidth the pixel size of the game board without margins
     * @return the size of each intersection square
     */
    public int calculateSquareSize(int availableWidth) {
        return availableWidth / (Board.BOARD_SIZE - 1);
    }
}