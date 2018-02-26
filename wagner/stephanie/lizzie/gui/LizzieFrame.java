package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.Lizzie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * The window used to display the game.
 */
public class LizzieFrame extends JFrame {
    private static BoardRenderer boardRenderer = new BoardRenderer();
    private static final int FPS = 30; // frames per second

    private final BufferStrategy bs;

    /**
     * Creates a window and refreshes the game state at FPS.
     */
    public LizzieFrame() {
        super("Lizzie - Leela Zero Interface");

        setSize(800, 600);
        setLocationRelativeTo(null); // start centered
        setExtendedState(Frame.MAXIMIZED_BOTH); // start maximized

        setVisible(true);

        createBufferStrategy(2);
        bs = getBufferStrategy();

        // set fps
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(
                this::repaint,
                0,
                1000 / FPS,
                TimeUnit.MILLISECONDS);

        Input input = new Input();
        this.addMouseListener(input);
        this.addKeyListener(input);

        // shut down leelaz, then shut down the program when the window is closed
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                Lizzie.leelaz.shutdown();
                System.exit(0);
            }
        });
    }

    /**
     * Draws the game board and interface
     * @param g0 not used
     */
    public void paint(Graphics g0) {
        if (bs == null)
            return;

        // initialize
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int topInset = this.getInsets().top;

        // test
        g.setColor(new Color((float) (bouncingMod(System.currentTimeMillis() / 100, 100) / 100.0),
                (float) (bouncingMod(System.currentTimeMillis() / 200 ,100) / 100.0),
                (float) (bouncingMod(System.currentTimeMillis() / 300 , 100) / 100.0)));
        g.fillRect(0, 0, getWidth(), getHeight());

        int maxSize = (int) (Math.min(getWidth(), getHeight() - topInset) * 0.9);
        maxSize = Math.max(maxSize, Board.BOARD_SIZE + 5); // don't let maxWidth become too small

        boardRenderer.setLocation((getWidth() - maxSize) / 2, topInset + (getHeight() - topInset - maxSize) / 2);
        boardRenderer.setBoardWidth(maxSize);
        boardRenderer.draw(g);

        // cleanup
        g.dispose();
        bs.show();
    }

    // instead of the usual mod pattern (0 1 2 0 1 2...), it bounces back and forth: (0 1 2 1 0 1 2 1...)
    private static long bouncingMod(long a, int mod) {
        a = a%(2*mod);
        if (a >= mod)
            a = 2*mod - a;
        return a;
    }

    /**
     * Checks whether or not something was clicked and performs the appropriate action
     * @param x x coordinate
     * @param y y coordinate
     */
    public void onClicked(int x, int y) {
        // check for board click
        int[] boardCoordinates = boardRenderer.convertScreenToCoordinates(x, y);

        if (boardCoordinates != null) {
            Lizzie.board.place(boardCoordinates[0], boardCoordinates[1]);
        }
    }
}