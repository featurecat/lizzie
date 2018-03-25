package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.SGFParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * The window used to display the game.
 */
public class LizzieFrame extends JFrame {
    public static final int FPS = 30; // frames per second

    private static final String[] commands = {
            "left arrow = undo",
            "right arrow = redo",
            "space = toggle pondering",
            "right click = undo",
            "mouse wheel scroll = undo/redo",
            "p = pass",
            "m = show/hide move number",
            "o = open SGF",
            "s = save SGF",
            "home = go to start",
            "end = go to end",
            "ctrl = undo/redo 10 moves",
    };
    private static BoardRenderer boardRenderer;

    private final BufferStrategy bs;

    public int[] mouseHoverCoordinate;

    /**
     * Creates a window and refreshes the game state at FPS.
     */
    public LizzieFrame() {
        super("Lizzie - Leela Zero Interface");

        boardRenderer = new BoardRenderer();

        // on 1080p screens in Windows, this is a good width/height. removing a default size causes problems in Linux
        setSize(657, 687);
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
        this.addMouseWheelListener(input);
        this.addMouseMotionListener(input);

        // when the window is closed: save the SGF file, then run shutdown()
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Lizzie.shutdown();
            }
        });

    }

    public static void saveSgf() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf", "SGF");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.exists()) {
                int ret = JOptionPane.showConfirmDialog(null, "The SGF file already exists, do you want to replace it?", "Warning", JOptionPane.OK_CANCEL_OPTION);
                if (ret == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            if (!file.getPath().endsWith(".sgf")) {
                file = new File(file.getPath() + ".sgf");
            }
            try {
                SGFParser.save(file.getPath());
            } catch (IOException err) {
                JOptionPane.showConfirmDialog(null, "Failed to save the SGF file.", "Error", JOptionPane.ERROR);
            }
        }
    }

    public static void openSgf() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("*.sgf", "SGF");
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getPath().endsWith(".sgf")) {
                file = new File(file.getPath() + ".sgf");
            }
            try {
                System.out.println(file.getPath());
                SGFParser.load(file.getPath());
            } catch (IOException err) {
                JOptionPane.showConfirmDialog(null, "Failed to open the SGF file.", "Error", JOptionPane.ERROR);
            }
        }
    }

    /**
     * Draws the game board and interface
     *
     * @param g0 not used
     */
    public void paint(Graphics g0) {
        if (bs == null)
            return;

        // initialize
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();

        int topInset = this.getInsets().top;

        g.setColor(Color.GREEN.darker().darker());
        g.fillRect(0, 0, getWidth(), getHeight());

        int maxSize = (int) (Math.min(getWidth(), getHeight() - topInset) * 0.99);
        maxSize = Math.max(maxSize, Board.BOARD_SIZE + 5); // don't let maxWidth become too small

        int boardX = (getWidth() - maxSize) / 2;
        int boardY = topInset + (getHeight() - topInset - maxSize) / 2;
        boardRenderer.setLocation(boardX, boardY);
        boardRenderer.setBoardLength(maxSize);
        boardRenderer.draw(g);

        // draw the commands, right of the board.
        Font font = new Font("Sans Serif", Font.PLAIN, (int) (maxSize * 0.02));
        g.setFont(font);
        int commandsX = (int) (boardX + maxSize * 1.01);
        int commandsY = (int) (getHeight() * 0.2);

        Color boxColor = new Color(200, 255, 200);
        g.setColor(boxColor);
        g.fillRect(commandsX, commandsY, (int) (maxSize * 0.35), (int) (commands.length * font.getSize() * 1.1));
        g.setColor(boxColor.darker());
        g.drawRect(commandsX, commandsY, (int) (maxSize * 0.35), (int) (commands.length * font.getSize() * 1.1));

        g.setColor(Color.BLACK);
        for (int i = 0; i < commands.length; i++) {
            g.drawString(commands[i], commandsX, font.getSize() + (int) (commandsY + i * font.getSize() * 1.1));
        }


        // cleanup
        g.dispose();
        bs.show();
    }

    /**
     * Checks whether or not something was clicked and performs the appropriate action
     *
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

    public void onMouseMoved(int x, int y) {
        mouseHoverCoordinate = boardRenderer.convertScreenToCoordinates(x, y);
    }
}