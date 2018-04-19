package wagner.stephanie.lizzie.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;

import com.jhlabs.image.GaussianFilter;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.GameInfo;
import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.SGFParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import wagner.stephanie.lizzie.theme.DefaultTheme;

/**
 * The window used to display the game.
 */
public class LizzieFrame extends JFrame {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");

    private static final String[] commands = {
            resourceBundle.getString("LizzieFrame.commands.keyN"),
            resourceBundle.getString("LizzieFrame.commands.keyEnter"),
            resourceBundle.getString("LizzieFrame.commands.keySpace"),
            resourceBundle.getString("LizzieFrame.commands.keyLeftArrow"),
            resourceBundle.getString("LizzieFrame.commands.keyRightArrow"),
            resourceBundle.getString("LizzieFrame.commands.rightClick"),
            resourceBundle.getString("LizzieFrame.commands.mouseWheelScroll"),
            resourceBundle.getString("LizzieFrame.commands.keyC"),
            resourceBundle.getString("LizzieFrame.commands.keyP"),
            resourceBundle.getString("LizzieFrame.commands.keyM"),
            resourceBundle.getString("LizzieFrame.commands.keyI"),
            resourceBundle.getString("LizzieFrame.commands.keyO"),
            resourceBundle.getString("LizzieFrame.commands.keyS"),
            resourceBundle.getString("LizzieFrame.commands.keyAltC"),
            resourceBundle.getString("LizzieFrame.commands.keyAltV"),
            resourceBundle.getString("LizzieFrame.commands.keyV"),
            resourceBundle.getString("LizzieFrame.commands.keyW"),
            resourceBundle.getString("LizzieFrame.commands.keyHome"),
            resourceBundle.getString("LizzieFrame.commands.keyEnd"),
            resourceBundle.getString("LizzieFrame.commands.keyControl"),
    };
    private static BoardRenderer boardRenderer;
    private static VariationTree variatonTree;
    private static WinrateGraph winrateGraph;

    private final BufferStrategy bs;

    public int[] mouseHoverCoordinate;
    public boolean showControls = false;
    public boolean showCoordinates = false;
    public boolean isPlayingAgainstLeelaz = false;
    public boolean playerIsBlack = true;

    // Get the font name in current system locale
    private String systemDefaultFontName = new JLabel().getFont().getFontName();

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
        variatonTree = new VariationTree();
        winrateGraph = new WinrateGraph();

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

    public static void startNewGame() {
        GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

        NewGameDialog newGameDialog = new NewGameDialog();
        newGameDialog.setGameInfo(gameInfo);
        newGameDialog.setVisible(true);
        boolean playerIsBlack = newGameDialog.playerIsBlack();
        newGameDialog.dispose();
        if (newGameDialog.isCancelled()) return;

        Lizzie.board.clear();
        Lizzie.leelaz.sendCommand("clear_board");
        Lizzie.leelaz.sendCommand("komi " + gameInfo.getKomi());

        Lizzie.leelaz.sendCommand("time_settings 0 " + Lizzie.config.config.getJSONObject("leelaz").getInt("max-game-thinking-time-seconds") + " 1");
        Lizzie.frame.playerIsBlack = playerIsBlack;
        Lizzie.frame.isPlayingAgainstLeelaz = true;

        boolean isHandicapGame = gameInfo.getHandicap() != 0;
        if (isHandicapGame)
        {
            Lizzie.board.getHistory().getData().blackToPlay = false;
            Lizzie.leelaz.sendCommand("fixed_handicap " + gameInfo.getHandicap());
            if (playerIsBlack) Lizzie.leelaz.sendCommand("genmove W");
        }
        else if (!playerIsBlack)
        {
            Lizzie.leelaz.sendCommand("genmove B");
        }
    }

    public static void editGameInfo() {
        GameInfo gameInfo = Lizzie.board.getHistory().getGameInfo();

        GameInfoDialog gameInfoDialog = new GameInfoDialog();
        gameInfoDialog.setGameInfo(gameInfo);
        gameInfoDialog.setVisible(true);

        gameInfoDialog.dispose();
    }

    public void setPlayers(String whitePlayer, String blackPlayer) {
        setTitle(String.format("Lizzie - Leela Zero Interface (%s [W] vs %s [B])",
                               whitePlayer, blackPlayer));
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
                int ret = JOptionPane.showConfirmDialog(null, resourceBundle.getString("LizzieFrame.prompt.sgfExists"), "Warning", JOptionPane.OK_CANCEL_OPTION);
                if (ret == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            if (!file.getPath().endsWith(".sgf")) {
                file = new File(file.getPath() + ".sgf");
            }
            try {
                SGFParser.save(Lizzie.board, file.getPath());
            } catch (IOException err) {
                JOptionPane.showConfirmDialog(null, resourceBundle.getString("LizzieFrame.prompt.failedToSaveSgf"), "Error", JOptionPane.ERROR);
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
                JOptionPane.showConfirmDialog(null, resourceBundle.getString("LizzieFrame.prompt.failedToOpenSgf"), "Error", JOptionPane.ERROR);
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
                BufferedImage background = boardRenderer.theme.getBackground();
                if (background == null) {
                    background = new DefaultTheme().getBackground();
                }
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

            // Todo: Make board move over when there is no space beside the board
            if (Lizzie.config.showWinrate) {
                // boardX equals width of space on each side
                int statx = (int) (boardX*0.05);
                int staty = boardY + maxSize/8;
                int statw = (int)(boardX*0.8);
                int stath = maxSize/10;
                drawMoveStatistics(g, statx, staty, statw, stath);
                winrateGraph.draw(g, statx,staty+ stath, statw, maxSize/3);
            }

            variatonTree.draw(g, maxSize + boardX, 0, maxSize, getHeight());

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
        Font font = new Font(systemDefaultFontName, Font.PLAIN, (int) (maxSize * 0.04));
        g.setFont(font);
        int lineHeight = (int) (font.getSize() * 1.15);

        int boxWidth = (int) (maxSize * 0.85);
        int boxHeight = commands.length * lineHeight;

        int commandsX = getWidth() / 2 - boxWidth / 2;
        int commandsY = getHeight() / 2 - boxHeight / 2;

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
            g.drawString(split[0], verticalLineX - metrics.stringWidth(split[0]) - strokeRadius * 4, font.getSize() + (commandsY + i * lineHeight));
            g.drawString(split[1], verticalLineX + strokeRadius * 4, font.getSize() + (commandsY + i * lineHeight));
        }
    }

    private boolean userAlreadyKnowsAboutCommandString = false;

    private void drawCommandString(Graphics2D g) {
        if (userAlreadyKnowsAboutCommandString)
            return;

        int maxSize = (int) (Math.min(getWidth(), getHeight()) * 0.98);

        Font font = new Font(systemDefaultFontName, Font.PLAIN, (int) (maxSize * 0.03));
        String commandString = resourceBundle.getString("LizzieFrame.prompt.showControlsHint");
        int strokeRadius = 2;

        int showCommandsHeight = (int) (font.getSize() * 1.1);
        int showCommandsWidth = g.getFontMetrics(font).stringWidth(commandString) + 4 * strokeRadius;
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
        g.drawString(commandString, showCommandsX + 2 * strokeRadius, showCommandsY + font.getSize());
    }

    private void drawMoveStatistics(Graphics2D g, int posX, int posY, int width, int height) {


        double lastWR;
        if (Lizzie.board.getData().moveNumber == 0)
            lastWR = 50;
        else
            lastWR = Lizzie.board.getHistory().getPrevious().winrate;
        double lastBWR = lastWR;

        double curWR = Lizzie.leelaz.getBestWinrate();
        if (curWR < 0) {
            curWR = 100 - lastWR;
        }
        double whiteWR, blackWR;
        if (Lizzie.board.getData().blackToPlay) {
            blackWR = curWR;
            lastBWR = 100 - lastWR;
        } else {
            blackWR = 100 - curWR;
        }
        whiteWR = 100 - blackWR;

        // Background rectangle
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(posX, posY, width, height);

        // Title
        Font font = new Font("Open Sans", Font.PLAIN, (int) (Math.min(width, height) * 0.2));
        int strokeRadius = 2;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setFont(font);

        // Last move
        if (lastWR < 0)
            // In case leelaz didnt have time to calculate
            g.drawString(resourceBundle.getString("LizzieFrame.display.lastMove") + ": ?%", posX+2*strokeRadius, posY + height- 2*strokeRadius);
        else
            g.drawString(resourceBundle.getString("LizzieFrame.display.lastMove") + String.format(": %.1f%%", 100 - lastWR - curWR), posX+2*strokeRadius,
                    posY + height - 2*strokeRadius);// - font.getSize());


        int maxBarwidth = (int) (width);
        int barWidthB = (int) (blackWR*maxBarwidth/100);
        int barWidthW = (int) (whiteWR*maxBarwidth/100);
        int barPosY = posY + height/3;
        int barPosxB = (int)(posX);
        int barPosxW = barPosxB + barWidthB;
        int barHeight = height/3;

        // Draw winrate bars
        g.fillRect(barPosxW, barPosY, barWidthW, barHeight);
        g.setColor(Color.BLACK);
        g.fillRect(barPosxB, barPosY, barWidthB, barHeight);

        // Show percentage above bars
        g.setColor(Color.WHITE);
        g.drawString(String.format("%.1f", blackWR), barPosxB + 2*strokeRadius, posY + barHeight - 2*strokeRadius);
        String winString = String.format("%.1f", whiteWR);
        int sw = g.getFontMetrics().stringWidth(winString);
        g.drawString(winString, barPosxB + maxBarwidth - sw - 2*strokeRadius, posY + barHeight - 2*strokeRadius);

        g.setColor(Color.GRAY);
        Stroke oldstroke = g.getStroke();
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                                         new float[]{4}, 0);
        g.setStroke(dashed);

        int middleX = barPosxB + (int)(maxBarwidth/2);
        g.drawLine(middleX , barPosY, middleX, barPosY + barHeight);
        g.setStroke(oldstroke);
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

    public void copySgf() {
        try {
            // Get sgf content from game
            String sgfContent = SGFParser.saveToString();

            // Save to clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transferableString = new StringSelection(sgfContent);
            clipboard.setContents(transferableString, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pasteSgf() {
        try {
            String sgfContent = null;
            // Get string from clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable clipboardContents = clipboard.getContents(null);
            if (clipboardContents != null) {
                if (clipboardContents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    sgfContent = (String) clipboardContents.getTransferData(DataFlavor.stringFlavor);
                }
            }

            // load game contents from sgf string
            if (sgfContent != null && !sgfContent.isEmpty()) {
                SGFParser.loadFromString(sgfContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
