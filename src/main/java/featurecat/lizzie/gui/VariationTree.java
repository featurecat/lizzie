package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.BoardHistoryNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;

public class VariationTree {

  private int YSPACING;
  private int XSPACING;
  private int DOT_DIAM = 11; // Should be odd number
  private int DOT_DIAM_S = 9; // For small node
  private int CENTER_DIAM = 5;
  private int RING_DIAM = 15;
  private int diam = DOT_DIAM;

  private ArrayList<Integer> laneUsageList;
  private BoardHistoryNode curMove;
  private Rectangle area;
  private Point clickPoint;

  public VariationTree() {
    laneUsageList = new ArrayList<Integer>();
    area = new Rectangle(0, 0, 0, 0);
    clickPoint = new Point(0, 0);
  }

  public Optional<BoardHistoryNode> drawTree(
      Graphics2D g,
      int posx,
      int posy,
      int startLane,
      int maxposy,
      int minposx,
      BoardHistoryNode startNode,
      int variationNumber,
      boolean isMain,
      boolean calc) {
    Optional<BoardHistoryNode> node = Optional.empty();
    if (!calc) {
      if (isMain) g.setColor(Color.white);
      else g.setColor(Color.gray.brighter());
    }

    // Finds depth on leftmost variation of this tree
    int depth = startNode.getDepth() + 1;
    int lane = startLane;
    // Figures out how far out too the right (which lane) we have to go not to collide with other
    // variations
    int moveNumber = startNode.getData().moveNumber;
    while (lane < laneUsageList.size() && laneUsageList.get(lane) <= moveNumber + depth) {
      // laneUsageList keeps a list of how far down it is to a variation in the different "lanes"
      laneUsageList.set(lane, moveNumber - 1);
      lane++;
    }
    if (lane >= laneUsageList.size()) {
      laneUsageList.add(0);
    }
    if (variationNumber > 1) laneUsageList.set(lane - 1, moveNumber - 1);
    laneUsageList.set(lane, moveNumber);

    // At this point, lane contains the lane we should use (the main branch is in lane 0)

    BoardHistoryNode cur = startNode;
    int curposx = posx + lane * XSPACING;
    int dotoffset = DOT_DIAM / 2;
    if (Lizzie.config.nodeColorMode == 1 && cur.getData().blackToPlay
        || Lizzie.config.nodeColorMode == 2 && !cur.getData().blackToPlay) {
      diam = DOT_DIAM_S;
    } else {
      diam = DOT_DIAM;
    }
    int dotoffsety = diam / 2;
    int diff = (DOT_DIAM - diam) / 2;

    if (calc) {
      if (inNode(curposx + dotoffset, posy + dotoffset)) {
        return Optional.of(startNode);
      }
    } else if (lane > 0) {

      // Draw line back to main branch
      if (lane - startLane > 0 || variationNumber > 1) {
        // Need a horizontal and an angled line
        drawLine(
            g,
            curposx + dotoffset,
            posy + dotoffsety,
            curposx + dotoffset - XSPACING,
            posy + dotoffsety - YSPACING,
            minposx);
        drawLine(
            g,
            posx + (startLane - variationNumber) * XSPACING + 2 * dotoffset,
            posy - YSPACING + dotoffsety,
            curposx + dotoffset - XSPACING,
            posy + dotoffsety - YSPACING,
            minposx);
      } else {
        // Just an angled line
        drawLine(
            g,
            curposx + dotoffset,
            posy + dotoffsety,
            curposx + 2 * dotoffset - XSPACING,
            posy + 2 * dotoffsety - YSPACING,
            minposx);
      }
    }

    // Draw all the nodes and lines in this lane (not variations)
    Color curcolor = null;
    if (!calc) {
      curcolor = g.getColor();
      if (curposx > minposx && posy > 0) {
        if (startNode.previous().isPresent()) {
          if (Lizzie.config.showCommentNodeColor && !cur.getData().comment.isEmpty()) {
            g.setColor(Lizzie.config.commentNodeColor);
            g.fillOval(
                curposx + (DOT_DIAM + diff - RING_DIAM) / 2,
                posy + (DOT_DIAM + diff - RING_DIAM) / 2,
                RING_DIAM,
                RING_DIAM);
          }
          g.setColor(Lizzie.frame.getBlunderNodeColor(cur));
          g.fillOval(curposx + diff, posy + diff, diam, diam);
          if (startNode == curMove) {
            g.setColor(Color.BLACK);
            g.fillOval(
                curposx + (DOT_DIAM + diff - CENTER_DIAM) / 2,
                posy + (DOT_DIAM + diff - CENTER_DIAM) / 2,
                CENTER_DIAM,
                CENTER_DIAM);
          }
        } else {
          g.fillRect(curposx, posy, DOT_DIAM, DOT_DIAM);
          g.setColor(Color.BLACK);
          g.drawRect(curposx, posy, DOT_DIAM, DOT_DIAM);
        }
      }
      g.setColor(curcolor);
    }

    // Draw main line
    while (cur.next().isPresent() && posy + YSPACING < maxposy) {
      posy += YSPACING;
      cur = cur.next().get();
      if (calc) {
        if (inNode(curposx + dotoffset, posy + dotoffset)) {
          return Optional.of(cur);
        }
      } else if (curposx > minposx && posy > 0) {
        if (Lizzie.config.nodeColorMode == 1 && cur.getData().blackToPlay
            || Lizzie.config.nodeColorMode == 2 && !cur.getData().blackToPlay) {
          diam = DOT_DIAM_S;
        } else {
          diam = DOT_DIAM;
        }
        dotoffsety = diam / 2;
        diff = (DOT_DIAM - diam) / 2;
        if (Lizzie.config.showCommentNodeColor && !cur.getData().comment.isEmpty()) {
          g.setColor(Lizzie.config.commentNodeColor);
          g.fillOval(
              curposx + (DOT_DIAM + diff - RING_DIAM) / 2,
              posy + (DOT_DIAM + diff - RING_DIAM) / 2,
              RING_DIAM,
              RING_DIAM);
        }
        g.setColor(Lizzie.frame.getBlunderNodeColor(cur));
        g.fillOval(curposx + diff, posy + diff, diam, diam);
        if (cur == curMove) {
          g.setColor(Color.BLACK);
          g.fillOval(
              curposx + (DOT_DIAM + diff - CENTER_DIAM) / 2,
              posy + (DOT_DIAM + diff - CENTER_DIAM) / 2,
              CENTER_DIAM,
              CENTER_DIAM);
        }
        g.setColor(curcolor);
        g.drawLine(
            curposx + dotoffset,
            posy - 1 + diff,
            curposx + dotoffset,
            posy
                - YSPACING
                + dotoffset
                + (diff > 0 ? dotoffset + 1 : dotoffsety)
                + (Lizzie.config.nodeColorMode == 0 ? 1 : 0));
      }
    }
    // Now we have drawn all the nodes in this variation, and has reached the bottom of this
    // variation
    // Move back up, and for each, draw any variations we find
    while (cur.previous().isPresent() && (isMain || cur != startNode)) {
      cur = cur.previous().get();
      int curwidth = lane;
      // Draw each variation, uses recursion
      for (int i = 1; i < cur.numberOfChildren(); i++) {
        curwidth++;
        // Recursion, depth of recursion will normally not be very deep (one recursion level for
        // every variation that has a variation (sort of))
        Optional<BoardHistoryNode> variation = cur.getVariation(i);
        if (variation.isPresent()) {
          Optional<BoardHistoryNode> subNode =
              drawTree(g, posx, posy, curwidth, maxposy, minposx, variation.get(), i, false, calc);
          if (calc && subNode.isPresent()) {
            return subNode;
          }
        }
      }
      posy -= YSPACING;
    }
    return node;
  }

  public void draw(Graphics2D g, int posx, int posy, int width, int height) {
    draw(g, posx, posy, width, height, false);
  }

  public Optional<BoardHistoryNode> draw(
      Graphics2D g, int posx, int posy, int width, int height, boolean calc) {
    if (width <= 0 || height <= 0) {
      return Optional.empty(); // we don't have enough space
    }

    // Use dense tree for saving space if large-subboard
    YSPACING = (Lizzie.config.showLargeSubBoard() ? 20 : 30);
    XSPACING = YSPACING;

    int strokeRadius = Lizzie.config.showBorder ? 2 : 0;
    if (!calc) {
      // Draw background
      area.setBounds(posx, posy, width, height);
      g.setColor(new Color(0, 0, 0, 60));
      g.fillRect(posx, posy, width, height);

      if (Lizzie.config.showBorder) {
        // draw edge of panel
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.drawLine(
            posx + strokeRadius,
            posy + strokeRadius,
            posx + strokeRadius,
            posy - strokeRadius + height);
        g.setStroke(new BasicStroke(1));
      }
    }

    int middleY = posy + height / 2;
    int xoffset = 30;
    laneUsageList.clear();

    curMove = Lizzie.board.getHistory().getCurrentHistoryNode();

    // Is current move a variation? If so, find top of variation
    BoardHistoryNode top = curMove.findTop();
    int curposy = middleY - YSPACING * (curMove.getData().moveNumber - top.getData().moveNumber);
    // Go to very top of tree (visible in assigned area)
    BoardHistoryNode node = top;
    while (curposy > posy + YSPACING && node.previous().isPresent()) {
      node = node.previous().get();
      curposy -= YSPACING;
    }
    int lane = getCurLane(node, curMove, curposy, posy + height, 0, true);
    int startx = posx + xoffset;
    if (((lane + 1) * XSPACING + xoffset + DOT_DIAM + strokeRadius - width) > 0) {
      startx = startx - ((lane + 1) * XSPACING + xoffset + DOT_DIAM + strokeRadius - width);
    }
    return drawTree(g, startx, curposy, 0, posy + height, posx + strokeRadius, node, 0, true, calc);
  }

  private void drawLine(Graphics g, int x1, int y1, int x2, int y2, int minx) {
    if (x1 <= minx && x2 <= minx) {
      return;
    }
    int nx1 = x1, ny1 = y1, nx2 = x2, ny2 = y2;
    if (x1 > minx && x2 <= minx) {
      ny2 = y2 - (x1 - minx) / (x1 - x2) * (y2 - y1);
      nx2 = minx;
    } else if (x2 > minx && x1 <= minx) {
      ny1 = y1 - (x2 - minx) / (x2 - x1) * (y1 - y2);
      nx1 = minx;
    }
    g.drawLine(nx1, ny1, nx2, ny2);
  }

  public boolean inNode(int x, int y) {
    return Math.abs(clickPoint.x - x) < XSPACING / 2 && Math.abs(clickPoint.y - y) < YSPACING / 2;
  }

  public void onClicked(int x, int y) {
    if (area.contains(x, y)) {
      clickPoint.setLocation(x, y);
      Optional<BoardHistoryNode> node = draw(null, area.x, area.y, area.width, area.height, true);
      node.ifPresent(n -> Lizzie.board.moveToAnyPosition(n));
    }
  }

  private int getCurLane(
      BoardHistoryNode start,
      BoardHistoryNode curMove,
      int curposy,
      int maxy,
      int laneCount,
      boolean isMain) {
    BoardHistoryNode next = start;
    int nexty = curposy;
    while (next.next().isPresent() && nexty + YSPACING < maxy) {
      nexty += YSPACING;
      next = next.next().get();
    }
    while (next.previous().isPresent() && (isMain || next != start)) {
      next = next.previous().get();
      for (int i = 1; i < next.numberOfChildren(); i++) {
        laneCount++;
        if (next.findIndexOfNode(curMove, true) == i) {
          return laneCount;
        }
        Optional<BoardHistoryNode> variation = next.getVariation(i);
        if (variation.isPresent()) {
          int subLane = getCurLane(variation.get(), curMove, nexty, maxy, laneCount, false);
          if (subLane > 0) {
            return subLane;
          }
        }
      }
      nexty -= YSPACING;
    }
    return 0;
  }
}
