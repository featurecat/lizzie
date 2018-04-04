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


/**
 * The window used to display the game.
 */
public class LizzieFrame extends JFrame {
    private static final String[] commands = {
            "enter|play against Leela Zero",
            "space|toggle pondering",
            "left arrow|undo",
            "right arrow|redo",
            "right click|undo",
            "scrollwheel|undo/redo",
            "c|toggle coordinates",
            "p|pass",
            "m|show/hide move number",
            "o|open SGF",
            "s|save SGF",
            "v|toggle variation display",
            "home|go to start",
            "end|go to end",
            "ctrl|undo/redo 10 moves",
    };
    private static BoardRenderer boardRenderer;

    private final BufferStrategy bs;

    public int[] mouseHoverCoordinate;
    public boolean showControls = false;
    public boolean showCoordinates = false;
    public boolean isPlayingAgainstLeelaz = false;
    public boolean playerIsBlack = true;

    static {
        // load fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, LizzieFrame.class.getResourceAsStream("/fonts/OpenSans-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, LizzieFrame.class.getResourceAsStream("/fonts/OpenSans-Semibold.ttf")));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a window
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
                BufferedImage background = ImageIO.read(new File("assets/background.jpg"));
                int drawWidth = Math.max(background.getWidth(), getWidth());
                int drawHeight = Math.max(background.getHeight(), getHeight());
                g.drawImage(background, 0, 0, drawWidth, drawHeight, null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int maxSize = (int) (Math.min(getWidth(), getHeight() - topInset) * 0.98);
            maxSize = Math.max(maxSize, Board.BOARD_SIZE + 5); // don't let maxWidth become too small

            drawCommandString(g);

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
        userAlreadyKnowsAboutCommandString = true;

        Graphics2D g = (Graphics2D) cachedImage.getGraphics();
        int maxSize = Math.min(getWidth(), getHeight());
        Font font = new Font("Open Sans", Font.PLAIN, (int) (maxSize * 0.04));
        g.setFont(font);
        int lineHeight = (int) (font.getSize() * 1.15);

        int boxWidth = (int) (maxSize * 0.85);
        int boxHeight = (int) (commands.length * lineHeight);

        int commandsX = (int) (getWidth() / 2 - boxWidth / 2);
        int commandsY = (int) (getHeight() / 2 - boxHeight / 2);


        BufferedImage result = new BufferedImage(boxWidth, boxHeight, BufferedImage.TYPE_INT_ARGB);
        filter.filter(cachedImage.getSubimage(commandsX, commandsY, boxWidth, boxHeight), result);
        g.drawImage(result, commandsX, commandsY, null);

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(commandsX, commandsY, boxWidth, boxHeight);
        int strokeRadius = 2;
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(commandsX + strokeRadius, commandsY + strokeRadius, boxWidth - 2 * strokeRadius, boxHeight - 2 * strokeRadius);

        int verticalLineX = (int) (commandsX + boxWidth * 0.3);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawLine(verticalLineX, commandsY + 2 * strokeRadius, verticalLineX, commandsY + boxHeight - 2 * strokeRadius);


        g.setStroke(new BasicStroke(1));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics metrics = g.getFontMetrics(font);

        g.setColor(Color.WHITE);
        for (int i = 0; i < commands.length; i++) {
            String[] split = commands[i].split("\\|");
            g.drawString(split[0], verticalLineX - metrics.stringWidth(split[0]) - strokeRadius * 4, font.getSize() + (int) (commandsY + i * lineHeight));
            g.drawString(split[1], verticalLineX + strokeRadius * 4, font.getSize() + (int) (commandsY + i * lineHeight));
        }
    }

    private boolean userAlreadyKnowsAboutCommandString = false;

    private void drawCommandString(Graphics2D g) {
        if (userAlreadyKnowsAboutCommandString)
            return;

        int maxSize = (int) (Math.min(getWidth(), getHeight()) * 0.98);

        Font font = new Font("Open Sans", Font.PLAIN, (int) (maxSize * 0.03));
        String commandString = "hold x = view controls";
        int strokeRadius = 2;

        int showCommandsHeight = (int) (font.getSize()*1.1);
        int showCommandsWidth = g.getFontMetrics(font).stringWidth(commandString) + 4*strokeRadius;
        int showCommandsX = this.getInsets().left;
        int showCommandsY = getHeight() - showCommandsHeight - this.getInsets().bottom;
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(showCommandsX, showCommandsY, showCommandsWidth, showCommandsHeight);
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(showCommandsX + strokeRadius, showCommandsY + strokeRadius, showCommandsWidth - 2 * strokeRadius, showCommandsHeight - 2 * strokeRadius);
        g.setStroke(new BasicStroke(1));

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(commandString, showCommandsX+2*strokeRadius, showCommandsY + font.getSize());
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
            if (!isPlayingAgainstLeelaz || (playerIsBlack == Lizzie.board.getData().blackToPlay))
                Lizzie.board.place(boardCoordinates[0], boardCoordinates[1]);
        }
    }

    public void onMouseMoved(int x, int y) {

        int[] newMouseHoverCoordinate = boardRenderer.convertScreenToCoordinates(x, y);
        if (mouseHoverCoordinate != null && newMouseHoverCoordinate != null && (mouseHoverCoordinate[0] != newMouseHoverCoordinate[0] || mouseHoverCoordinate[1] != newMouseHoverCoordinate[1])) {
            mouseHoverCoordinate = newMouseHoverCoordinate;
            repaint();
        } else {
            mouseHoverCoordinate = newMouseHoverCoordinate;
        }
    }

    public void toggleCoordinates() {
        showCoordinates = !showCoordinates;
    }
}
