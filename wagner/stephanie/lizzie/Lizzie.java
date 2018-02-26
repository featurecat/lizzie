package wagner.stephanie.lizzie;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.gui.LizzieFrame;

import java.io.IOException;

/**
 * Main class.
 */
public class Lizzie {
    public static LizzieFrame frame;
    public static Leelaz leelaz;
    public static Board board;

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException {
        leelaz = new Leelaz();
        leelaz.ponder();

        board = new Board();
        frame = new LizzieFrame();
    }
}
