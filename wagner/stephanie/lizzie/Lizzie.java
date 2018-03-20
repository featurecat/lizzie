package wagner.stephanie.lizzie;

import wagner.stephanie.lizzie.analysis.Leelaz;
import wagner.stephanie.lizzie.analysis.MoveData;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.SGFParser;
import wagner.stephanie.lizzie.gui.LizzieFrame;
import wagner.stephanie.lizzie.gui.Config;
import java.io.IOException;

/**
 * Main class.
 */
public class Lizzie {
    public static LizzieFrame frame;
    public static Leelaz leelaz;
    public static Board board;
    public static Config config;
    public static String lizzie_version = "0.3 pre-1";

    /**
     * Launches the game window, and runs the game.
     */
    public static void main(String[] args) throws IOException {
        config = new Config();
        leelaz = new Leelaz();
        leelaz.ponder();

        board = new Board();

        frame = new LizzieFrame();

        System.out.println("!!!FRAME CREATED");
    }
}
