package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.Leelaz;
import wagner.stephanie.lizzie.rules.BoardHistoryNode;

import java.awt.*;
import java.awt.geom.Point2D;

public class WinrateGraph {

    private int DOT_RADIUS = 6;

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
        g.drawLine(posx, posy + height/2, posx + width, posy + height/2);

        g.setStroke(new BasicStroke(1));

        // Go to end of variation and work our way backwards to the root
        while (node.next() != null) node = node.next();
        int numMoves = node.getData().moveNumber-1;

        if (numMoves < 1) return;

        // Plot
        width = (int)(width*0.95); // Leave some space after last move
        double lastWr = 50;
        boolean lastNodeOk = false;
        int movenum = numMoves;
        g.setColor(Color.green);
        g.setStroke(new BasicStroke(3));

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
                  g.drawLine(posx + ((movenum + 1)*width/numMoves),
                             posy + height - (int)(lastWr*height/100),
                             posx + (movenum*width/numMoves),
                             posy + height - (int)(wr*height/100));
               if (node == curMove)
                  g.fillOval(posx + (movenum*width/numMoves) - DOT_RADIUS,
                             posy + height - (int)(wr*height/100) - DOT_RADIUS,
                             DOT_RADIUS*2,
                             DOT_RADIUS*2);
               lastWr = wr;
               lastNodeOk = true;
            } else {
               lastNodeOk = false;
            }
            node = node.previous();
            movenum--;
        }

        g.setStroke(new BasicStroke(1));
    }
}
