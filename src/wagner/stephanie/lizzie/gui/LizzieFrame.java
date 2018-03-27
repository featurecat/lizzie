package wagner.stephanie.lizzie.gui;

import com.jhlabs.image.GaussianFilter;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.SGFParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
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
            "left arrow|undo",
            "right arrow|redo",
            "space|toggle pondering",
            "right click|undo",
            "scrollwheel|undo/redo",
            "c|toggle coordinates",
            "p|pass",
            "m|show/hide move number",
            "o|open SGF",
            "s|save SGF",
            "home|go to start",
            "end|go to end",
            "ctrl|undo/redo 10 moves",
    };
    private static BoardRenderer boardRenderer;

    private final BufferStrategy bs;

    public int[] mouseHoverCoordinate;
    public boolean showControls = false;
    public boolean showCoordinates = false;

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

    BufferedImage cachedImage;
    /**
     * Draws the game board and interface
     *
     * @param g0 not used
     */
    public void paint(Graphics g0) {
        if (bs == null)
            return;

        if (!showControls) {
            // initialize
            cachedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) cachedImage.getGraphics();

            int topInset = this.getInsets().top;

            try {
                g.drawImage(ImageIO.read(new File("assets/background.jpg")), 0, 0, getWidth(), getHeight(), null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // simple graphics
//        g.setColor(Color.GREEN.darker().darker());
//        g.fillRect(0, 0, getWidth(), getHeight());

            int maxSize = (int) (Math.min(getWidth(), getHeight() - topInset) * 0.98);
            maxSize = Math.max(maxSize, Board.BOARD_SIZE + 5); // don't let maxWidth become too small

            int boardX = (getWidth() - maxSize) / 2;
            int boardY = topInset + (getHeight() - topInset - maxSize) / 2 + 3;
            boardRenderer.setLocation(boardX, boardY);
            boardRenderer.setBoardLength(maxSize);
            boardRenderer.draw(g);

            // cleanup
            g.dispose();
        }

        // draw the image
        Graphics2D bsGraphics = (Graphics2D) bs.getDrawGraphics();
        bsGraphics.drawImage(cachedImage, 0, 0, null);

        // cleanup
        bsGraphics.dispose();
        bs.show();
    }

    private GaussianFilter filter = new GaussianFilter(15);

    /**
     * Display the controls
     */
    void drawControls() {
        Graphics2D g = (Graphics2D)cachedImage.getGraphics();
        int maxSize = Math.min(getWidth(), getHeight());
        Font font = new Font("Open Sans", Font.PLAIN, (int) (maxSize * 0.04));
        g.setFont(font);
        int lineHeight = (int)(font.getSize() * 1.15);

        int boxWidth = (int)(maxSize* 0.85);
        int boxHeight = (int) (commands.length * lineHeight);

        int commandsX = (int) (getWidth()/2 - boxWidth/2);
        int commandsY = (int) (getHeight()/2 - boxHeight/2);


        BufferedImage result = new BufferedImage(boxWidth, boxHeight, BufferedImage.TYPE_INT_ARGB);
        filter.filter(cachedImage.getSubimage(commandsX, commandsY, boxWidth, boxHeight), result);
        g.drawImage(result, commandsX, commandsY, null);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(commandsX, commandsY, boxWidth, boxHeight);
        int strokeRadius = 2;
        g.setStroke(new BasicStroke(2*strokeRadius));
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(commandsX+strokeRadius, commandsY+strokeRadius, boxWidth-2*strokeRadius, boxHeight-2*strokeRadius);

        int verticalLineX = (int)(commandsX + boxWidth * 0.3);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawLine(verticalLineX, commandsY+2*strokeRadius, verticalLineX, commandsY+boxHeight-2*strokeRadius);


        g.setStroke(new BasicStroke(1));

        FontMetrics metrics = g.getFontMetrics(font);

        g.setColor(Color.WHITE);
        for (int i = 0; i < commands.length; i++) {
            String[] split = commands[i].split("\\|");
            g.drawString(split[0], verticalLineX - metrics.stringWidth(split[0]) - strokeRadius*4, font.getSize() + (int) (commandsY + i * lineHeight));
            g.drawString(split[1], verticalLineX + strokeRadius*4, font.getSize() + (int) (commandsY + i * lineHeight));
        }
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

    public void toggleCoordinates() {
        showCoordinates = !showCoordinates;
    }
}