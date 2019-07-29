package featurecat.lizzie.gui;

import static java.lang.Math.max;
import static java.lang.Math.min;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

public class LizzieLayout implements LayoutManager2, java.io.Serializable {
  int hgap;
  int vgap;

  private Component north;
  private Component west;
  private Component east;
  private Component south;
  private Component mainBoard;
  private Component subBoard;
  private Component winratePane;
  private Component variationPane;
  private Component basicInfoPane;
  private Component commentPane;
  private Component consolePane;

  public static final String NORTH = "North";
  public static final String SOUTH = "South";
  public static final String EAST = "East";
  public static final String WEST = "West";
  public static final String MAIN_BOARD = "mainBoard";
  public static final String SUB_BOARD = "subBoard";
  public static final String WINRATE = "winratePane";
  public static final String VARIATION = "variationPane";
  public static final String BASIC_INFO = "basicInfoPane";
  public static final String COMMENT = "commentPane";
  public static final String CONSOLE = "consolePane";

  public static final int MODE_FUSION = 0;
  public static final int MODE_SEPARATION = 1;

  private int mode = 0;

  public LizzieLayout() {
    this(3, 0);
  }

  public LizzieLayout(int hgap, int vgap) {
    this.hgap = hgap;
    this.vgap = vgap;
  }

  public int getHgap() {
    return hgap;
  }

  public void setHgap(int hgap) {
    this.hgap = hgap;
  }

  public int getVgap() {
    return vgap;
  }

  public void setVgap(int vgap) {
    this.vgap = vgap;
  }

  public void addLayoutComponent(Component comp, Object constraints) {
    synchronized (comp.getTreeLock()) {
      if ((constraints == null) || (constraints instanceof String)) {
        addLayoutComponent((String) constraints, comp);
      } else {
        throw new IllegalArgumentException(
            "cannot add to layout: constraint must be a string (or null)");
      }
    }
  }

  /** @deprecated replaced by <code>addLayoutComponent(Component, Object)</code>. */
  @Deprecated
  public void addLayoutComponent(String name, Component comp) {
    synchronized (comp.getTreeLock()) {
      if (name == null) {
        name = MAIN_BOARD;
      }
      if (NORTH.equals(name)) {
        north = comp;
      } else if (SOUTH.equals(name)) {
        south = comp;
      } else if (EAST.equals(name)) {
        east = comp;
      } else if (WEST.equals(name)) {
        west = comp;
      } else if (BASIC_INFO.equals(name)) {
        basicInfoPane = comp;
      } else if (MAIN_BOARD.equals(name)) {
        mainBoard = comp;
      } else if (VARIATION.equals(name)) {
        variationPane = comp;
      } else if (WINRATE.equals(name)) {
        winratePane = comp;
      } else if (SUB_BOARD.equals(name)) {
        subBoard = comp;
      } else if (COMMENT.equals(name)) {
        commentPane = comp;
      } else if (CONSOLE.equals(name)) {
        consolePane = comp;
      } else {
        throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
      }
    }
  }

  public void removeLayoutComponent(Component comp) {
    synchronized (comp.getTreeLock()) {
      if (comp == north) {
        north = null;
      } else if (comp == south) {
        south = null;
      } else if (comp == east) {
        east = null;
      } else if (comp == west) {
        west = null;
      } else if (comp == basicInfoPane) {
        basicInfoPane = null;
      } else if (comp == mainBoard) {
        mainBoard = null;
      } else if (comp == variationPane) {
        variationPane = null;
      } else if (comp == winratePane) {
        winratePane = null;
      } else if (comp == subBoard) {
        subBoard = null;
      }
      if (comp == commentPane) {
        commentPane = null;
      } else if (comp == consolePane) {
        consolePane = null;
      }
    }
  }

  public Component getLayoutComponent(Object constraints) {
    if (NORTH.equals(constraints)) {
      return north;
    } else if (SOUTH.equals(constraints)) {
      return south;
    } else if (WEST.equals(constraints)) {
      return west;
    } else if (EAST.equals(constraints)) {
      return east;
    } else if (BASIC_INFO.equals(constraints)) {
      return basicInfoPane;
    } else if (MAIN_BOARD.equals(constraints)) {
      return mainBoard;
    } else if (SUB_BOARD.equals(constraints)) {
      return subBoard;
    } else if (VARIATION.equals(constraints)) {
      return variationPane;
    } else if (WINRATE.equals(constraints)) {
      return winratePane;
    } else if (COMMENT.equals(constraints)) {
      return commentPane;
    } else if (CONSOLE.equals(constraints)) {
      return consolePane;
    } else {
      throw new IllegalArgumentException(
          "cannot get component: unknown constraint: " + constraints);
    }
  }

  public Component getLayoutComponent(Container target, Object constraints) {
    boolean ltr = target.getComponentOrientation().isLeftToRight();
    Component result = null;
    if (NORTH.equals(constraints)) {
      return north;
    } else if (SOUTH.equals(constraints)) {
      return south;
    } else if (WEST.equals(constraints)) {
      return west;
    } else if (EAST.equals(constraints)) {
      return east;
    } else if (MAIN_BOARD.equals(constraints)) {
      result = mainBoard;
    } else if (SUB_BOARD.equals(constraints)) {
      result = subBoard;
    } else if (COMMENT.equals(constraints)) {
      result = commentPane;
    } else if (VARIATION.equals(constraints)) {
      result = variationPane;
    } else if (WINRATE.equals(constraints)) {
      result = winratePane;
    } else if (BASIC_INFO.equals(constraints)) {
      result = basicInfoPane;
    } else {
      throw new IllegalArgumentException(
          "cannot get component: invalid constraint: " + constraints);
    }

    return result;
  }

  public Object getConstraints(Component comp) {
    if (comp == null) {
      return null;
    }
    if (comp == north) {
      return NORTH;
    } else if (comp == south) {
      return SOUTH;
    } else if (comp == west) {
      return WEST;
    } else if (comp == east) {
      return EAST;
    } else if (comp == basicInfoPane) {
      return BASIC_INFO;
    } else if (comp == mainBoard) {
      return MAIN_BOARD;
    } else if (comp == subBoard) {
      return SUB_BOARD;
    } else if (comp == variationPane) {
      return VARIATION;
    } else if (comp == winratePane) {
      return WINRATE;
    } else if (comp == commentPane) {
      return COMMENT;
    } else if (comp == consolePane) {
      return CONSOLE;
    }
    return null;
  }

  public Dimension minimumLayoutSize(Container target) {
    synchronized (target.getTreeLock()) {
      Dimension dim = new Dimension(0, 0);

      boolean ltr = target.getComponentOrientation().isLeftToRight();
      Component c = null;

      if ((c = getChild(WINRATE, ltr)) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width + hgap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(VARIATION, ltr)) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width + hgap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(BASIC_INFO, ltr)) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(MAIN_BOARD, ltr)) != null) {
        Dimension d = c.getMinimumSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + vgap;
      }
      if ((c = getChild(SUB_BOARD, ltr)) != null) {
        Dimension d = c.getMinimumSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + vgap;
      }

      Insets insets = target.getInsets();
      dim.width += insets.left + insets.right;
      dim.height += insets.top + insets.bottom;

      return dim;
    }
  }

  public Dimension preferredLayoutSize(Container target) {
    synchronized (target.getTreeLock()) {
      Dimension dim = new Dimension(0, 0);

      boolean ltr = target.getComponentOrientation().isLeftToRight();
      Component c = null;

      if ((c = getChild(WINRATE, ltr)) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width + hgap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(VARIATION, ltr)) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width + hgap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(BASIC_INFO, ltr)) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = getChild(MAIN_BOARD, ltr)) != null) {
        Dimension d = c.getPreferredSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + vgap;
      }
      if ((c = getChild(SUB_BOARD, ltr)) != null) {
        Dimension d = c.getPreferredSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + vgap;
      }

      Insets insets = target.getInsets();
      dim.width += insets.left + insets.right;
      dim.height += insets.top + insets.bottom;

      return dim;
    }
  }

  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public float getLayoutAlignmentX(Container parent) {
    return 0.5f;
  }

  public float getLayoutAlignmentY(Container parent) {
    return 0.5f;
  }

  public void invalidateLayout(Container target) {}

  public void layoutContainer(Container target) {
    if (mode != MODE_FUSION) {
      return;
    }
    synchronized (target.getTreeLock()) {
      Container main = getMain(target);
      Insets insets = target.getInsets();

      int x = 0; // target.getX();
      int y = 0; // target.getY();
      int width = target.getWidth();
      int height = target.getHeight();

      // layout parameters
      int top = insets.top;
      int bottom = height - insets.bottom;
      int left = insets.left;
      int right = width - insets.right;

      boolean ltr = target.getComponentOrientation().isLeftToRight();
      Component c = null;

      if ((c = getChild(NORTH, ltr)) != null) {
        c.setSize(right - left, c.getHeight());
        Dimension d = c.getPreferredSize();
        c.setBounds(left, top, right - left, d.height);
        top += d.height + vgap;
      }
      if ((c = getChild(SOUTH, ltr)) != null) {
        c.setSize(right - left, c.getHeight());
        Dimension d = c.getPreferredSize();
        c.setBounds(left, bottom - d.height, right - left, d.height);
        bottom -= d.height + vgap;
      }
      if ((c = getChild(EAST, ltr)) != null) {
        c.setSize(c.getWidth(), bottom - top);
        Dimension d = c.getPreferredSize();
        c.setBounds(right - d.width, top, d.width, bottom - top);
        right -= d.width + hgap;
      }
      if ((c = getChild(WEST, ltr)) != null) {
        c.setSize(c.getWidth(), bottom - top);
        Dimension d = c.getPreferredSize();
        c.setBounds(left, top, d.width, bottom - top);
        left += d.width + hgap;
      }
      x = left;
      y = top;
      width = right - left;
      height = bottom - top;

      int maxBound = Math.max(width, height);

      boolean noWinrate = (getChild(WINRATE, ltr) == null || !Lizzie.config.showWinrate);
      boolean noVariation = (getChild(VARIATION, ltr) == null || !Lizzie.config.showVariationGraph);
      boolean noBasic = (getChild(BASIC_INFO, ltr) == null || !Lizzie.config.showCaptured);
      boolean noSubBoard = (getChild(SUB_BOARD, ltr) == null || !Lizzie.config.showSubBoard);
      boolean noComment = (getChild(COMMENT, ltr) == null || !Lizzie.config.showComment);
      //      boolean onlyMainBoard = noWinrate && noVariation && noBasic && noSubBoard &&
      // noComment;

      // board
      int maxSize0 = (int) (min(width, height));
      int maxw = maxSize0; // Board.boardWidth > Board.boardHeight ? (width) * 3 /
      // 4 : maxSize0;
      int maxh = maxSize0;
      if (noBasic && noWinrate && noSubBoard) {
        maxw = width;
      } else if (noVariation && noComment) {
        maxw = (width) * 3 / 4;
      }
      int[] mainBoardParam =
          BoardRenderer.availableLength(
              max(maxw, Board.boardWidth + 5),
              max(maxh, Board.boardHeight + 5),
              Lizzie.config.showCoordinates); // don't let maxWidth become too small
      int boardX =
          x
              + (width - mainBoardParam[0])
                  / 8
                  * ((main == null || !(main instanceof LizzieMain))
                      ? Lizzie.config.boardPositionProportion
                      : ((LizzieMain) main).boardPositionProportion);
      if (noBasic && noWinrate && noSubBoard) {
        boardX = x;
      } else if (noVariation && noComment) {
        boardX = x + (width - mainBoardParam[0]);
      }
      int boardY = y + (height - mainBoardParam[3]) / 2;

      int panelMargin = (int) (mainBoardParam[0] * 0.02);

      // captured stones
      int capx = x;
      int capy = y;
      int capw = boardX - panelMargin - x;
      int caph = maxSize0 / 8;

      // move statistics (winrate bar)
      // boardX equals width of space on each side
      int statx = capx;
      int staty = capy + caph;
      int statw = capw;
      int stath = maxSize0 / 10;

      // winrate graph
      int grx = statx;
      int gry = staty + stath;
      int grw = statw;
      int grh = maxSize0 / 3;

      // variation tree container
      int vx = boardX + mainBoardParam[0] + panelMargin;
      int vy = capy;
      int vw = right - vx;
      int vh = bottom - vy;

      // pondering message
      double ponderingSize = .02;
      int ponderingY = bottom - (int) (maxSize0 * 0.033) - (int) (maxBound * ponderingSize);

      // subboard
      int subBoardY = gry + grh + 1;
      int subBoardWidth = grw;
      int subBoardHeight = ponderingY - subBoardY;
      int[] subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
      int subBoardX = statx + (statw - subBoardParam[0]) / 2;

      if (width >= height) {
        // Landscape mode
        if (Lizzie.config.showLargeSubBoard() && !noSubBoard) {
          boardX = x + width - mainBoardParam[0] - panelMargin;
          int spaceW = boardX - panelMargin - x;
          int spaceH = height;
          int panelW = spaceW / 2;
          int panelH = spaceH / 4;

          // captured stones
          capw = (noVariation && noComment) ? spaceW : panelW;
          caph = (int) (panelH * 0.2);
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = (int) (panelH * 0.4);
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = panelH - caph - stath;
          // variation tree container
          vx = statx + statw;
          vw = panelW;
          vh = panelH;
          // subboard
          subBoardY = gry + grh;
          subBoardWidth = spaceW;
          subBoardHeight = ponderingY - subBoardY;
          subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
          subBoardX = statx + (spaceW - subBoardParam[0]) / 2;
        } else if (Lizzie.config.showLargeWinrate() && !noWinrate) {
          boardX = x + width - mainBoardParam[0] - panelMargin;
          int spaceW = boardX - panelMargin - x;
          int spaceH = height;
          int panelW = spaceW / 2;
          int panelH = spaceH / 4;

          // captured stones
          capy = y + panelH + 1;
          capw = spaceW;
          caph = (int) ((ponderingY - y - panelH) * 0.15);
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = caph;
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = ponderingY - gry;
          // variation tree container
          vx = x + panelW;
          vw = panelW;
          vh = panelH;
          // subboard
          subBoardY = y;
          subBoardWidth = panelW - x;
          subBoardHeight = panelH;
          subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
          subBoardX = statx + (vw - subBoardParam[0]) / 2;
        }
      } else {
        // Portrait mode
        if (Lizzie.config.showLargeSubBoard() && !noSubBoard) {
          // board
          mainBoardParam =
              BoardRenderer.availableLength(
                  (int) (maxSize0 * 0.8), (int) (maxSize0 * 0.8), Lizzie.config.showCoordinates);
          boardY = height - mainBoardParam[3];
          int spaceW = width;
          int spaceH = boardY - panelMargin - y;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;
          boardX = x + (spaceW - mainBoardParam[0]) / 2 + x;

          // captured stones
          capw = panelW / 2;
          caph = panelH / 2;
          // move statistics (winrate bar)
          staty = capy + caph;
          statw = capw;
          stath = caph;
          // winrate graph
          gry = staty + stath;
          grw = statw;
          grh = spaceH - caph - stath;
          // variation tree container
          vx = capx + capw;
          vw = panelW / 2;
          vh = spaceH;
          // subboard
          subBoardX = vx + vw;
          subBoardWidth = panelW;
          subBoardHeight = boardY - y;
          subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
          subBoardY = capy + (gry + grh - capy - subBoardParam[0]) / 2;
          // pondering message
          ponderingY = height;
        } else if (Lizzie.config.showLargeWinrate() && !noWinrate) {
          // board
          mainBoardParam =
              BoardRenderer.availableLength(
                  (int) (maxSize0 * 0.8), (int) (maxSize0 * 0.8), Lizzie.config.showCoordinates);
          boardY = height - mainBoardParam[3];
          int spaceW = width;
          int spaceH = boardY - panelMargin - y;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;
          boardX = (spaceW - mainBoardParam[0]) / 2 + x;

          // captured stones
          capw = panelW / 2;
          caph = panelH / 4;
          // move statistics (winrate bar)
          statx = capx;
          staty = capy + caph;
          statw = spaceW;
          stath = boardY - caph - 1;
          // winrate graph
          gry = staty + stath;
          grw = spaceW;
          grh = boardY - gry - 1;
          // variation tree container
          vx = capx + capw;
          vy = capy;
          vw = panelW;
          vh = caph;
          // subboard
          subBoardY = capy;
          subBoardWidth = panelW / 2;
          subBoardHeight = caph;
          subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
          subBoardX = vx + vw;
          // pondering message
          ponderingY = height;
        } else {
          // Normal
          // board
          boardY = (height - mainBoardParam[3] + y) / 2;
          int spaceW = width;
          int spaceH = boardY - panelMargin - y;
          int panelW = spaceW / 2;
          int panelH = spaceH / 2;

          // captured stones
          capw = panelW * 3 / 4;
          caph = panelH / 2;
          // move statistics (winrate bar)
          statx = capx + capw;
          staty = capy;
          statw = width - capx - capw;
          stath = caph;
          // winrate graph
          grx = statx;
          gry = staty + stath;
          grw = statw;
          grh = boardY - gry;
          // subboard
          subBoardWidth = capw;
          subBoardHeight = boardY - caph - capy;
          subBoardParam = BoardRenderer.availableLength(subBoardWidth, subBoardHeight, false);
          subBoardX = capx + (capw - subBoardParam[0]) / 2;
          subBoardY = capy + caph + (boardY - capy - caph - subBoardParam[3]) / 2;
          // variation tree container
          vx = capx;
          vy = boardY + mainBoardParam[3];
          vw = spaceW;
          vh = ponderingY - vy;
        }
      }

      // variation tree
      int treex = vx;
      int treey = vy;
      int treew = vw;
      int treeh = vh;

      // comment panel
      int cx = vx, cy = vy, cw = vw, ch = vh;
      if (Lizzie.config.showComment) {
        if (width >= height) {
          if (Lizzie.config.showVariationGraph) {
            treeh = vh / 2;
            cy = vy + treeh;
            ch = treeh;
          }
        } else {
          if (Lizzie.config.showVariationGraph) {
            if (Lizzie.config.showLargeSubBoard()) {
              treeh = vh / 2;
              cy = vy + treeh;
              ch = treeh;
            } else {
              treew = vw / 2;
              cx = vx + treew;
              cw = treew;
            }
          }
        }
      }

      if ((c = getChild(MAIN_BOARD, ltr)) != null) {
        c.setBounds(boardX, boardY, mainBoardParam[0], mainBoardParam[3]);
        if (c instanceof LizziePane) {
          ((LizziePane) c).boardParams = mainBoardParam;
        }
        //        c.repaint();
      }
      if ((c = getChild(SUB_BOARD, ltr)) != null) {
        c.setBounds(subBoardX, subBoardY, subBoardParam[0], subBoardParam[3]);
        if (c instanceof LizziePane) {
          ((LizziePane) c).boardParams = subBoardParam;
        }
        //        c.repaint();
      }
      if ((c = getChild(BASIC_INFO, ltr)) != null) {
        c.setBounds(capx, capy, capw, caph);
      }
      if ((c = getChild(WINRATE, ltr)) != null) {
        c.setBounds(statx, staty, statw, stath + grh);
      }
      if ((c = getChild(VARIATION, ltr)) != null) {
        c.setBounds(treex, treey, treew, treeh);
      }
      if ((c = getChild(COMMENT, ltr)) != null) {
        //        ((CommentPane)c).setCommentBounds(cx, cy, cw, ch);
        c.setBounds(cx, cy, cw, ch);
        c.repaint();
      }
    }
  }

  private Component getChild(String key, boolean ltr) {
    Component result = null;

    if (key == NORTH) {
      result = north;
    } else if (key == SOUTH) {
      result = south;
    } else if (key == WEST) {
      result = west;
    } else if (key == EAST) {
      result = east;
    } else if (key == MAIN_BOARD) {
      result = mainBoard;
    } else if (key == SUB_BOARD) {
      result = subBoard;
    } else if (key == VARIATION) {
      result = variationPane;
    } else if (key == WINRATE) {
      result = winratePane;
    } else if (key == COMMENT) {
      result = commentPane;
    } else if (key == BASIC_INFO) {
      result = basicInfoPane;
    }
    return result;
  }

  public String toString() {
    return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
  }

  private Container getMain(Container target) {
    Container p = (target != null) ? target.getParent() : null;
    while (p != null && !(p instanceof LizzieMain)) {
      p = p.getParent();
    }
    return p;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }
}
