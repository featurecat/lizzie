package wagner.stephanie.lizzie.gui;

import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.BoardHistoryNode;

import java.awt.*;

public class WinrateGraph {

    private int DOT_RADIUS = 4;

    public void draw(Graphics2D g, int posx, int posy, int width, int height)
    {
        BoardHistoryNode curMove = Lizzie.board.getHistory().getCurrentHistoryNode();
        BoardHistoryNode node = curMove;

        // Background rectangle
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(posx, posy, width, height);
        g.setColor(Color.white);
        g.drawLine(posx, posy + height/2, posx + width, posy + height/2);


        // Go to bottom to find number of moves in main line
        while (node.next() != null) node = node.next();
        int numMoves = node.getData().moveNumber;

        if (numMoves < 2) return;

        node = curMove;
        // Go to first move (second node) to start plotting
        while (node.previous() != null) node = node.previous();
        node = node.next();

        // Plot
        width = (int)(width*0.95); // Leave some space after last move
        double lastWr = 50;
        int movenum = 1;
        g.setColor(Color.green);
        while (node != null)
        {
            double wr = node.getData().winrate;
            if (node == curMove)
            {
                double bwr = Lizzie.leelaz.getBestWinrate();
                if (bwr >= 0) {
                    wr = bwr;
                }
            }
            if (wr < 0)
            {
                wr = 100 - lastWr;
            }
            else if (!node.getData().blackToPlay)
            {
                wr = 100 - wr;
            }

            g.drawLine(posx + ((movenum - 1)*width/numMoves), posy + height - (int)(lastWr*height/100), posx + (movenum*width/numMoves), posy + height - (int)(wr*height/100));
            if (node == curMove)
                g.fillOval(posx + (movenum*width/numMoves) - DOT_RADIUS, posy + height - (int)(wr*height/100) - DOT_RADIUS, DOT_RADIUS*2, DOT_RADIUS*2);
            node = node.next();
            lastWr = wr;
            movenum++;
        }
    }
}
