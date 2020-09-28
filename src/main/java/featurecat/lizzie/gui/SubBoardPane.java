package featurecat.lizzie.gui;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.util.Utils;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** The window used to display the game. */
public class SubBoardPane extends LizziePane {

  private static BoardRenderer subBoardRenderer;
  private BufferedImage cachedImage;

  //  private final BufferStrategy bs;

  /** Creates a window */
  public SubBoardPane(LizzieMain owner) {
    super(owner);

    subBoardRenderer = new BoardRenderer(false);

    setVisible(false);

    // TODO BufferStrategy does not support transparent background?
    //    createBufferStrategy(2);
    //    bs = getBufferStrategy();

    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
              subBoardRenderer.increaseBestmoveIndexSub(1);
              repaint();
            }
            if (e.getButton() == MouseEvent.BUTTON3) {
              subBoardRenderer.increaseBestmoveIndexSub(-1);
              repaint();
            }
            if (e.getButton() == MouseEvent.BUTTON2) {
              Lizzie.config.toggleLargeSubBoard();
              owner.invalidLayout();
            }
            subBoardRenderer.setClickedSub(true);
          }
        });

    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            if (!subBoardRenderer.getIsMouseOverSub()) {
              subBoardRenderer.setIsMouseOverSub(true);
              repaint();
            }
          }
        });

    addMouseWheelListener(
        new MouseWheelListener() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0) {
              Utils.doBranchSub(subBoardRenderer, 1);
              repaint();
            } else if (e.getWheelRotation() < 0) {
              Utils.doBranchSub(subBoardRenderer, -1);
              repaint();
            }
          }
        });
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
    // layout parameters

    // initialize
    cachedImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D) cachedImage.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    if (Lizzie.leelaz != null) { // && Lizzie.leelaz.isLoaded()) {
      if (Lizzie.config.showSubBoard) {
        try {
          subBoardRenderer.setLocation(x, y);
          if (boardParams == null || width != boardParams[0] || height != boardParams[3]) {
            boardParams =
                subBoardRenderer.availableLength(
                    max(width, Board.boardWidth + 5),
                    max(height, Board.boardHeight + 5),
                    Lizzie.config.showCoordinates,
                    false);
          }
          subBoardRenderer.setBoardParam(boardParams);
          subBoardRenderer.draw(g);
        } catch (Exception e) {
          // This can happen when no space is left for subboard.
        }
      }
    }

    // cleanup
    g.dispose();

    // draw the image
    // TODO BufferStrategy does not support transparent background?
    Graphics2D bsGraphics = (Graphics2D) g0; // bs.getDrawGraphics();
    bsGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    bsGraphics.drawImage(cachedImage, 0, 0, null);

    // cleanup
    bsGraphics.dispose();
    // TODO BufferStrategy does not support transparent background?
    //    bs.show();
  }

  /**
   * Checks whether or not something was clicked and performs the appropriate action
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void onClicked(int x, int y) {

    if (Lizzie.config.showSubBoard && subBoardRenderer.isInside(x, y)) {
      Lizzie.config.toggleLargeSubBoard();
    }
    repaint();
  }

  public boolean isInside(int x1, int y1) {
    return subBoardRenderer.isInside(x1, y1);
  }

  public void removeEstimateRect() {
    subBoardRenderer.removeEstimateRect();
  }

  public void drawEstimateRect(ArrayList<Double> estimateArray, boolean isZen) {
    subBoardRenderer.drawEstimateRect(estimateArray, isZen);
  }

  public void clearBeforeMove() {
    subBoardRenderer.clearBeforeMove();
  }

  public void clearIsMouseOverSub() {
    if (subBoardRenderer.getIsMouseOverSub()) {
      subBoardRenderer.setIsMouseOverSub(false);
      Utils.setDisplayedBranchLength(subBoardRenderer, -2);
      repaint();
    }
  }

  public boolean processSubBoardMouseWheelMoved(MouseWheelEvent e) {
    // TODO Auto-generated method stub
    if (subBoardRenderer.isInside(e.getX(), e.getY())) {
      return true;
    }
    return false;
  }
}
