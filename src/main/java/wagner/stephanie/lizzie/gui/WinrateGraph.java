package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.Leelaz;
import wagner.stephanie.lizzie.rules.BoardHistoryList;
import wagner.stephanie.lizzie.rules.BoardHistoryNode;

import java.awt.*;
import java.awt.geom.Point2D;

public class WinrateGraph {

    private int DOT_RADIUS = 6;
    private int[] params = {0, 0, 0, 0, 0};
    public int storedMoveNumber = -1;

    public void draw(Graphics2D g, int posx, int posy, int width, int height)
    {
        BoardHistoryNode curMove = Lizzie.board.getHistory().getCurrentHistoryNode();
        BoardHistoryNode node = curMove;

        // draw background rectangle
        final Paint gradient = new GradientPaint(new Point2D.Float(posx, posy), new Color(0, 0, 0, 150), new Point2D.Float(posx, posy+height), new Color(255, 255, 255, 150));
        final Paint borderGradient = new GradientPaint(new Point2D.Float(posx, posy), new Color(0, 0, 0, 150), new Point2D.Float(posx, posy+height), new Color(255, 255, 255, 150));

        Paint original = g.getPaint();
        g.setPaint(gradient);

        g.fillRect(posx, posy, width, height);

        // draw border
        int strokeRadius = 3;
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.setPaint(borderGradient);
        g.drawRect(posx+ strokeRadius, posy + strokeRadius, width - 2 * strokeRadius, height- 2 * strokeRadius);

        g.setPaint(original);

        // resize the box now so it's inside the border
        posx += 2*strokeRadius;
        posy += 2*strokeRadius;
        width -= 4*strokeRadius;
        height -= 4*strokeRadius;

        // draw lines marking 50% 60% 70% etc.
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                new float[]{4}, 0);
        g.setStroke(dashed);

        g.setColor(Color.white);
        int winRateGridLines = Lizzie.frame.winRateGridLines;
        for (int i = 1; i <= winRateGridLines; i++) {
            double percent = i * 100.0 / (winRateGridLines + 1);
            int y = posy + height - (int) (height * convertWinrate(percent) / 100);
            g.drawLine(posx, y, posx + width, y);
        }

        g.setColor(Color.green);
        g.setStroke(new BasicStroke(3));

        BoardHistoryNode topOfVariation = null;
        int numMoves = 0;
        if (!BoardHistoryList.isMainTrunk(curMove))
        {
            // We're in a variation, need to draw both main trunk and variation
            // Find top of variation
            topOfVariation = BoardHistoryList.findTop(curMove);
            // Find depth of main trunk, need this for plot scaling
            numMoves = BoardHistoryList.getDepth(topOfVariation) + topOfVariation.getData().moveNumber - 1;
            g.setStroke(dashed);
        }

        // Go to end of variation and work our way backwards to the root
        while (node.next() != null) node = node.next();
        if (numMoves < node.getData().moveNumber-1) {
            numMoves = node.getData().moveNumber - 1;
        }

        if (numMoves < 1) return;

        // Plot
        width = (int)(width*0.95); // Leave some space after last move
        double lastWr = 50;
        boolean lastNodeOk = false;
        int movenum = node.getData().moveNumber - 1;
        int lastOkMove = -1;

        while (node.previous() != null)
        {
            double wr = node.getData().winrate;
            int playouts = node.getData().playouts;
            if (node == curMove)
            {
                Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
                double bwr = stats.maxWinrate;
                if (bwr >= 0 && stats.totalPlayouts > playouts) {
                    wr = bwr;
                    playouts = stats.totalPlayouts;
                }
                if (storedMoveNumber >= 0) {
                    Stroke previousStroke = g.getStroke();
                    int x = posx + (movenum*width/numMoves);
                    g.setStroke(dashed);
                    g.setColor(Color.white);
                    g.drawLine(x, posy, x, posy + height);
                    g.setStroke(previousStroke);
                }
            }
            if (playouts > 0) {
                if (wr < 0)
                {
                    wr = 100 - lastWr;
                }
                else if (!node.getData().blackToPlay)
                {
                    wr = 100 - wr;
                }
                if (Lizzie.frame.isPlayingAgainstLeelaz && Lizzie.frame.playerIsBlack == !node.getData().blackToPlay) {
                    wr = lastWr;
                }

                if (lastNodeOk)
                    g.setColor(Color.green);
                else
                    g.setColor(Color.gray);

                if (lastOkMove > 0) {
                    g.drawLine(posx + (lastOkMove * width / numMoves),
                            posy + height - (int) (convertWinrate(lastWr) * height / 100),
                            posx + (movenum * width / numMoves),
                            posy + height - (int) (convertWinrate(wr) * height / 100));
                }

                if (storedMoveNumber >= 0 ? movenum == storedMoveNumber - 1 : node == curMove)
                {
                    g.setColor(Color.green);
                    g.fillOval(posx + (movenum*width/numMoves) - DOT_RADIUS,
                            posy + height - (int)(convertWinrate(wr)*height/100) - DOT_RADIUS,
                            DOT_RADIUS*2,
                            DOT_RADIUS*2);
                }
                lastWr = wr;
                // Check if we were in a variation and has reached the main trunk
                if (node == topOfVariation) {
                    // Reached top of variation, go to end of main trunk before continuing
                    while (node.next() != null) node = node.next();
                    movenum = node.getData().moveNumber - 1;
                    lastWr = node.getData().winrate;
                    if (!node.getData().blackToPlay)
                        lastWr = 100 - lastWr;
                    g.setStroke(new BasicStroke(3));
                    topOfVariation = null;
                }
                lastNodeOk = true;
                lastOkMove = movenum;
            } else {
                lastNodeOk = false;
            }

            node = node.previous();
            movenum--;
        }

        g.setStroke(new BasicStroke(1));

        // record parameters for calculating moveNumber
        params[0] = posx;
        params[1] = posy;
        params[2] = width;
        params[3] = height;
        params[4] = numMoves;
    }

    private double convertWinrate(double winrate) {
        double maxHandicap = 10;
        if (Lizzie.config.config.getJSONObject("ui").getBoolean("handicap-instead-of-winrate")) {
            double handicap = Lizzie.leelaz.winrateToHandicap(winrate);
            // handicap == + maxHandicap => r == 1.0
            // handicap == - maxHandicap => r == 0.0
            double r = 0.5 + handicap / (2 * maxHandicap);
            return Math.max(0, Math.min(r, 1)) * 100;
        } else {
            return winrate;
        }
    }

    public int moveNumber(int x, int y)
    {
        int posx = params[0];
        int posy = params[1];
        int width = params[2];
        int height = params[3];
        int numMoves = params[4];
        if (posx <= x && x < posx + width && posy <= y && y < posy + height) {
            // x == posx + (movenum * width / numMoves) ==> movenum = ...
            int movenum = Math.round((x - posx) * numMoves / (float) width);
            // movenum == moveNumber - 1 ==> moveNumber = ...
            return movenum + 1;
        } else {
            return -1;
        }
    }
}
