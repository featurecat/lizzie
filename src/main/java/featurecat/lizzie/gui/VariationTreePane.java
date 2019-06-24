package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import featurecat.lizzie.Lizzie;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/** The window used to display the game. */
public class VariationTreePane extends LizziePane {
  private static VariationTree variationTree;

  private LizzieMain owner;

  //  private final BufferStrategy bs;

  /** Creates a window */
  public VariationTreePane(LizzieMain owner) {
    super(owner);
    this.owner = owner;

    variationTree = new VariationTree();

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

    // layout parameters
    // initialize

    cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) cachedImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (Lizzie.leelaz != null && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showVariationGraph) {
        g.drawImage(owner.getVariationContainer(this), x, y, null);
        if (Lizzie.config.showVariationGraph) {
          variationTree.draw(g, x, y, width, height);
        }
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

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {
    if (Lizzie.config.showVariationGraph) {
      variationTree.onClicked(x, y);
    }
  }
}
