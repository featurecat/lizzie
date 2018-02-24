package test;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.gui.BoardRenderer;

public class BoardRendererTest {
    // TODO download JUnit
//    @Test
    public static void testCalculatePixelMargins() {
        // test all reasonable widths for consistency. They shouldn't be much more than Board.BOARD_SIZE apart
        final int MAXIMUM_ACCEPTABLE_ERROR = 2;
        final int MAXIMUM_WIDTH_TO_TEST = 10000;

        int lastWidth = Board.BOARD_SIZE - 1;
        for (int width = Board.BOARD_SIZE - 1; width < MAXIMUM_WIDTH_TO_TEST; width++) {
            System.out.print("width: " + width + ", ");
            int[] widths = BoardRenderer.calculatePixelMargins(width);
            int offset = lastWidth - widths[0];
            if (Math.abs(offset) > Board.BOARD_SIZE + MAXIMUM_ACCEPTABLE_ERROR)
                System.out.print("ERRONEOUS VALUE, ");
            lastWidth = widths[0];
            printIntArray(widths);
        }
    }

    public static void main(String[] args) {
        testCalculatePixelMargins();
    }

    public static void printIntArray(int[] a) {
        for (int x : a)
            System.out.print(x + " ");
        System.out.println();
    }
}
