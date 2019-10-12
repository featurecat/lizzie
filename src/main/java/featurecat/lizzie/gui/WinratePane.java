package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.min;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.rules.BoardData;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Optional;

/** The window used to display the game. */
public class WinratePane extends LizziePane {

  private LizzieMain owner;
  private static WinrateGraph winrateGraph;
  private BufferedImage cachedImage;
  public int winRateGridLines = 3;

  //  private final BufferStrategy bs;

  /** Creates a window */
  public WinratePane(LizzieMain owner) {
    super(owner);
    this.owner = owner;

    winrateGraph = new WinrateGraph();

    setVisible(false);

    //    createBufferStrategy(2);
    //    bs = getBufferStrategy();

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) { // left click
              onClicked(e.getX(), e.getY());
            }
          }
        });

    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
            onMouseDragged(e.getX(), e.getY());
          }
        });
  }

  /** Clears related status from empty board. */
  public void clear() {
    if (winrateGraph != null) {
      winrateGraph.clear();
    }
  }

  /**
   * Draws the game board and interface
   *
   * @param g0 not used
   */
  @Override
  protected void paintComponent(Graphics g0) {
    super.paintComponent(g0);

    int x = 0; // getX();
    int y = 0; // getY();
    int width = getWidth();
    int height = getHeight();

    // initialize

    cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) cachedImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    if (Lizzie.leelaz != null) { // && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showWinrate) {
        g.drawImage(owner.getWinrateContainer(this), x, y, null);
        int hh = height * 3 / 13;
        drawMoveStatistics(g, x, y, width, hh);
        winrateGraph.draw(g, x, y + hh, width, height - hh);
      }
    }

    // cleanup
    g.dispose();

    // draw the image
    Graphics2D bsGraphics = (Graphics2D) g0; // bs.getDrawGraphics();
    bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    bsGraphics.drawImage(cachedImage, 0, 0, null);

    // cleanup
    bsGraphics.dispose();
    //    bs.show();
  }

  private void drawMoveStatistics(Graphics2D g, int posX, int posY, int width, int height) {
    if (width < 0 || height < 0) return; // we don't have enough space

    double lastWR = 50; // winrate the previous move
    boolean validLastWinrate = false; // whether it was actually calculated
    Optional<BoardData> previous = Lizzie.board.getHistory().getPrevious();
    if (previous.isPresent() && previous.get().getPlayouts() > 0) {
      lastWR = previous.get().winrate;
      validLastWinrate = true;
    }

    Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
    double curWR = stats.maxWinrate; // winrate on this move
    boolean validWinrate = (stats.totalPlayouts > 0); // and whether it was actually calculated
    if (Lizzie.frame.isPlayingAgainstLeelaz
        && Lizzie.frame.playerIsBlack == !Lizzie.board.getHistory().getData().blackToPlay) {
      validWinrate = false;
    }

    if (!validWinrate) {
      curWR = 100 - lastWR; // display last move's winrate for now (with color difference)
    }
    double whiteWR, blackWR;
    if (Lizzie.board.getData().blackToPlay) {
      blackWR = curWR;
    } else {
      blackWR = 100 - curWR;
    }

    whiteWR = 100 - blackWR;

    // Background rectangle
    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(posX, posY, width, height);

    // border. does not include bottom edge
    int strokeRadius = Lizzie.config.showBorder ? 3 : 1;
    g.setStroke(new BasicStroke(strokeRadius == 1 ? strokeRadius : 2 * strokeRadius));
    g.drawLine(
        posX + strokeRadius, posY + strokeRadius,
        posX - strokeRadius + width, posY + strokeRadius);
    if (Lizzie.config.showBorder) {
      g.drawLine(
          posX + strokeRadius, posY + 3 * strokeRadius,
          posX + strokeRadius, posY - strokeRadius + height);
      g.drawLine(
          posX - strokeRadius + width, posY + 3 * strokeRadius,
          posX - strokeRadius + width, posY - strokeRadius + height);
    }

    // resize the box now so it's inside the border
    posX += 2 * strokeRadius;
    posY += 2 * strokeRadius;
    width -= 4 * strokeRadius;
    height -= 4 * strokeRadius;

    // Title
    strokeRadius = 2;
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(Color.WHITE);
    setPanelFont(g, (int) (min(width, height) * 0.2));

    String text = "";
    if (Lizzie.leelaz.isKataGo) {
      double score = Lizzie.leelaz.scoreMean;
      if (Lizzie.board.getHistory().isBlacksTurn()) {
        if (Lizzie.config.showKataGoBoardScoreMean) {
          score = score + Lizzie.board.getHistory().getGameInfo().getKomi();
        }
      } else {
        if (Lizzie.config.showKataGoBoardScoreMean) {
          score = score - Lizzie.board.getHistory().getGameInfo().getKomi();
        }
        if (Lizzie.config.kataGoScoreMeanAlwaysBlack) {
          score = -score;
        }
      }
      text =
          LizzieMain.resourceBundle.getString("LizzieFrame.katago.scoreMean")
              + ":"
              + String.format("%.1f", score)
              + " ";
      text =
          text
              + LizzieMain.resourceBundle.getString("LizzieFrame.katago.scoreStdev")
              + ":"
              + String.format("%.1f", Lizzie.leelaz.scoreStdev)
              + " ";
    }
    // Last move
    if (validLastWinrate && validWinrate) {

      if (Lizzie.config.handicapInsteadOfWinrate) {
        double currHandicapedWR = Lizzie.leelaz.winrateToHandicap(100 - curWR);
        double lastHandicapedWR = Lizzie.leelaz.winrateToHandicap(lastWR);
        text =
            text
                + LizzieMain.resourceBundle.getString("LizzieFrame.display.lastMove")
                + String.format(":%.2f", currHandicapedWR - lastHandicapedWR);
      } else {
        text =
            text
                + LizzieMain.resourceBundle.getString("LizzieFrame.display.lastMove")
                + String.format(":%.1f%%", 100 - lastWR - curWR);
      }

      g.drawString(
          text, posX + 2 * strokeRadius, posY + height - 2 * strokeRadius); // - font.getSize());
    } else {
      // I think it's more elegant to just not display anything when we don't have
      // valid data --dfannius
      // g.drawString(resourceBundle.getString("LizzieFrame.display.lastMove") + ": ?%",
      //              posX + 2 * strokeRadius, posY + height - 2 * strokeRadius);
    }

    if (validWinrate || validLastWinrate) {
      int maxBarwidth = (int) (width);
      int barWidthB = (int) (blackWR * maxBarwidth / 100);
      int barWidthW = (int) (whiteWR * maxBarwidth / 100);
      int barPosY = posY + height / 3;
      int barPosxB = (int) (posX);
      int barPosxW = barPosxB + barWidthB;
      int barHeight = height / 3;

      // Draw winrate bars
      g.fillRect(barPosxW, barPosY, barWidthW, barHeight);
      g.setColor(Color.BLACK);
      g.fillRect(barPosxB, barPosY, barWidthB, barHeight);

      // Show percentage above bars
      g.setColor(Color.WHITE);
      g.drawString(
          String.format("%.1f%%", blackWR),
          barPosxB + 2 * strokeRadius,
          posY + barHeight - 2 * strokeRadius);
      String winString = String.format("%.1f%%", whiteWR);
      int sw = g.getFontMetrics().stringWidth(winString);
      g.drawString(
          winString,
          barPosxB + maxBarwidth - sw - 2 * strokeRadius,
          posY + barHeight - 2 * strokeRadius);

      g.setColor(Color.GRAY);
      Stroke oldstroke = g.getStroke();
      Stroke dashed =
          new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {4}, 0);
      g.setStroke(dashed);

      for (int i = 1; i <= winRateGridLines; i++) {
        int x = barPosxB + (int) (i * (maxBarwidth / (winRateGridLines + 1)));
        g.drawLine(x, barPosY, x, barPosY + barHeight);
      }
      g.setStroke(oldstroke);
    }
  }

  private void setPanelFont(Graphics2D g, float size) {
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, (int) size);
    g.setFont(font);
  }

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {
    int moveNumber = winrateGraph.moveNumber(x, y);
    if (Lizzie.config.showWinrate && moveNumber >= 0) {
      Lizzie.frame.isPlayingAgainstLeelaz = false;
      Lizzie.board.goToMoveNumberBeyondBranch(moveNumber);
      repaint();
    }
  }

  public void onMouseDragged(int x, int y) {
    int moveNumber = winrateGraph.moveNumber(x, y);
    if (Lizzie.config.showWinrate && moveNumber >= 0) {
      if (Lizzie.board.goToMoveNumberWithinBranch(moveNumber)) {
        repaint();
      }
    }
  }

  public int moveNumber(int x, int y) {
    return winrateGraph.moveNumber(x, y);
  }
}
