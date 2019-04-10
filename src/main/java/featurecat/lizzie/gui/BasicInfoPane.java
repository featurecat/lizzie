package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import featurecat.lizzie.Lizzie;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** The window used to display the game. */
public class BasicInfoPane extends LizziePane {

  public BasicInfoPane(LizzieMain owner) {
    super(owner);
    setVisible(true);
  }

  private BufferedImage cachedImage;

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
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (Lizzie.config.showCaptured) drawCaptured(g, x, y, width, height);

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

  private void drawCaptured(Graphics2D g, int posX, int posY, int width, int height) {
    // Draw border
    g.setColor(new Color(0, 0, 0, 130));
    g.fillRect(posX, posY, width, height);

    // border. does not include bottom edge
    int strokeRadius = Lizzie.config.showBorder ? 3 : 1;
    g.setStroke(new BasicStroke(strokeRadius == 1 ? strokeRadius : 2 * strokeRadius));
    if (Lizzie.config.showBorder) {
      g.drawLine(
          posX + strokeRadius,
          posY + strokeRadius,
          posX - strokeRadius + width,
          posY + strokeRadius);
      g.drawLine(
          posX + strokeRadius,
          posY + 3 * strokeRadius,
          posX + strokeRadius,
          posY - strokeRadius + height);
      g.drawLine(
          posX - strokeRadius + width,
          posY + 3 * strokeRadius,
          posX - strokeRadius + width,
          posY - strokeRadius + height);
    }

    // Draw middle line
    g.drawLine(
        posX - strokeRadius + width / 2,
        posY + 3 * strokeRadius,
        posX - strokeRadius + width / 2,
        posY - strokeRadius + height);
    g.setColor(Color.white);

    // Draw black and white "stone"
    int diam = height / 3;
    int smallDiam = diam / 2;
    int bdiam = diam, wdiam = diam;
    if (Lizzie.board.inScoreMode()) {
      // do nothing
    } else if (Lizzie.board.getHistory().isBlacksTurn()) {
      wdiam = smallDiam;
    } else {
      bdiam = smallDiam;
    }
    g.setColor(Color.black);
    g.fillOval(
        posX + width / 4 - bdiam / 2, posY + height * 3 / 8 + (diam - bdiam) / 2, bdiam, bdiam);

    g.setColor(Color.WHITE);
    g.fillOval(
        posX + width * 3 / 4 - wdiam / 2, posY + height * 3 / 8 + (diam - wdiam) / 2, wdiam, wdiam);

    // Draw captures
    String bval, wval;
    setPanelFont(g, (float) (height * 0.18));
    if (Lizzie.board.inScoreMode()) {
      double score[] = Lizzie.board.getScore(Lizzie.board.scoreStones());
      bval = String.format("%.0f", score[0]);
      wval = String.format("%.1f", score[1]);
    } else {
      bval = String.format("%d", Lizzie.board.getData().blackCaptures);
      wval = String.format("%d", Lizzie.board.getData().whiteCaptures);
    }

    g.setColor(Color.WHITE);
    int bw = g.getFontMetrics().stringWidth(bval);
    int ww = g.getFontMetrics().stringWidth(wval);
    boolean largeSubBoard = Lizzie.config.showLargeSubBoard();
    int bx = (largeSubBoard ? diam : -bw / 2);
    int wx = (largeSubBoard ? bx : -ww / 2);

    g.drawString(bval, posX + width / 4 + bx, posY + height * 7 / 8);
    g.drawString(wval, posX + width * 3 / 4 + wx, posY + height * 7 / 8);

    // Status Indicator
    int statusDiam = height / 8;
    g.setColor((Lizzie.leelaz != null && Lizzie.leelaz.isPondering()) ? Color.GREEN : Color.RED);
    g.fillOval(
        posX - strokeRadius + width / 2 - statusDiam / 2,
        posY + height * 3 / 8 + (diam - statusDiam) / 2,
        statusDiam,
        statusDiam);
  }

  private void setPanelFont(Graphics2D g, float size) {
    Font font = new Font(Lizzie.config.fontName, Font.PLAIN, (int) size);
    g.setFont(font);
  }
}
