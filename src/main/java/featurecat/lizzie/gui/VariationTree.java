package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.WrapString;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.JLabel;

import org.json.JSONException;

public class VariationTree {

    private int YSPACING;
    private int XSPACING;
    private int DOT_DIAM = 11; // Should be odd number

    private ArrayList<Integer> laneUsageList;
    private BoardHistoryNode curMove;

    public VariationTree()
    {
        laneUsageList = new ArrayList<Integer>();
    }

    public void drawTree(Graphics2D g, int posx, int posy, int startLane, int maxposy, BoardHistoryNode startNode, int variationNumber, boolean isMain)
    {
        if (isMain) g.setColor(Color.white);
        else g.setColor(Color.gray.brighter());


        // Finds depth on leftmost variation of this tree
        int depth = BoardHistoryList.getDepth(startNode) + 1;
        int lane = startLane;
        // Figures out how far out too the right (which lane) we have to go not to collide with other variations
        while (lane < laneUsageList.size() && laneUsageList.get(lane) <= startNode.getData().moveNumber + depth) {
            // laneUsageList keeps a list of how far down it is to a variation in the different "lanes"
            laneUsageList.set(lane, startNode.getData().moveNumber - 1);
            lane++;
        }
        if (lane >= laneUsageList.size())
        {
                laneUsageList.add(0);
        }
        if (variationNumber > 1)
            laneUsageList.set(lane - 1, startNode.getData().moveNumber - 1);
        laneUsageList.set(lane, startNode.getData().moveNumber);

        // At this point, lane contains the lane we should use (the main branch is in lane 0)

        BoardHistoryNode cur  = startNode;
        int curposx = posx + lane*XSPACING;
        int dotoffset = DOT_DIAM/2;

        // Draw line back to main branch
        if (lane > 0) {
            if (lane - startLane > 0 || variationNumber > 1) {
                // Need a horizontal and an angled line
                g.drawLine(curposx + dotoffset, posy + dotoffset, curposx + dotoffset - XSPACING, posy + dotoffset - YSPACING);
                g.drawLine(posx + (startLane - variationNumber )*XSPACING + 2*dotoffset, posy - YSPACING + dotoffset, curposx + dotoffset - XSPACING, posy + dotoffset - YSPACING);
            } else {
                // Just an angled line
                g.drawLine(curposx + dotoffset, posy + dotoffset, curposx + 2*dotoffset - XSPACING, posy + 2*dotoffset - YSPACING);
            }
        }

        // Draw all the nodes and lines in this lane (not variations)
        Color curcolor = g.getColor();
        if (startNode == curMove) {
            g.setColor(Color.green.brighter().brighter());
        }
        if (startNode.previous() != null) {
            g.fillOval(curposx, posy, DOT_DIAM, DOT_DIAM);
            g.setColor(Color.BLACK);
            g.drawOval(curposx, posy, DOT_DIAM, DOT_DIAM);
        } else {
            g.fillRect(curposx, posy, DOT_DIAM, DOT_DIAM);
            g.setColor(Color.BLACK);
            g.drawRect(curposx, posy, DOT_DIAM, DOT_DIAM);
        }
        g.setColor(curcolor);

        // Draw main line
        while (cur.next() != null && posy + YSPACING < maxposy) {
            posy += YSPACING;
            cur = cur.next();
            if (cur == curMove)  {
                g.setColor(Color.green.brighter().brighter());
            }
            g.fillOval(curposx , posy, DOT_DIAM, DOT_DIAM);
            g.setColor(Color.BLACK);
            g.drawOval(curposx, posy, DOT_DIAM, DOT_DIAM);
            g.setColor(curcolor);
            g.drawLine(curposx + dotoffset, posy-1, curposx + dotoffset , posy - YSPACING + 2*dotoffset+2);
        }
        // Now we have drawn all the nodes in this variation, and has reached the bottom of this variation
        // Move back up, and for each, draw any variations we find
        while (cur.previous() != null && cur != startNode) {
            cur = cur.previous();
            int curwidth = lane;
            // Draw each variation, uses recursion
            for (int i = 1; i < cur.numberOfChildren(); i++) {
                curwidth++;
                // Recursion, depth of recursion will normally not be very deep (one recursion level for every variation that has a variation (sort of))
                drawTree(g, posx, posy, curwidth, maxposy, cur.getVariation(i), i,false);
            }
            posy -= YSPACING;
        }
    }

    public void draw(Graphics2D g, int posx, int posy, int width, int height) {
        if (width <= 0 || height <= 0)
            return; // we don't have enough space

        // Use dense tree for saving space if large-subboard
        YSPACING = (Lizzie.config.showLargeSubBoard() ? 20 : 30);
        XSPACING = YSPACING;

        // Draw background
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRect(posx, posy, width, height);
        
        // Draw Comment
        int cHeight = drawCommnet(g, posx, posy, width, height);
        height = height - cHeight;

        // draw edge of panel
        int strokeRadius = 2;
        g.setStroke(new BasicStroke(2 * strokeRadius));
        g.drawLine(posx+strokeRadius, posy+strokeRadius, posx+strokeRadius, posy-strokeRadius+height);
        g.setStroke(new BasicStroke(1));


        int middleY = posy + height/2;
        int xoffset = 30;
        laneUsageList.clear();

        curMove = Lizzie.board.getHistory().getCurrentHistoryNode();

        // Is current move a variation? If so, find top of variation
        BoardHistoryNode top = BoardHistoryList.findTop(curMove);
        int curposy = middleY - YSPACING*(curMove.getData().moveNumber - top.getData().moveNumber);
        // Go to very top of tree (visible in assigned area)
        BoardHistoryNode node = top;
        while  (curposy > posy + YSPACING && node.previous() != null) {
            node = node.previous();
            curposy -= YSPACING;
        }
        drawTree(g, posx + xoffset, curposy, 0, posy + height, node, 0,true);
    }
    
    private int drawCommnet(Graphics2D g, int x, int y, int w, int h) {
    	int cHeight = 0;
        String comment = (Lizzie.board.getHistory().getData() != null && Lizzie.board.getHistory().getData().comment != null) ? Lizzie.board.getHistory().getData().comment : "";
        if (comment != null && comment.trim().length() > 0) {
        	cHeight = (int)(h * 0.1);
	        String systemDefaultFontName = new JLabel().getFont().getFontName();
	    	// May be need to set up a Chinese Font for display a Chinese Text in the non-Chinese environment
	    	// String systemDefaultFontName = "宋体";
	        int fontSize = (int)(Math.max(w, cHeight) * 0.07);
	        try {
	        	fontSize = Lizzie.config.uiConfig.getInt("comment-font-size");
	        } catch (JSONException e) {
	        	if (fontSize < 16) {
	        		fontSize = 16;
	        	}
	        }
	        Font font = new Font(systemDefaultFontName, Font.PLAIN, fontSize);
	        FontMetrics fm = g.getFontMetrics(font);
	        int stringWidth = fm.stringWidth(comment);
	        int stringHeight = fm.getAscent() - fm.getDescent();
	        int width = stringWidth;
	        int height = (int)(stringHeight * 1.2);
	
	        ArrayList<String> list = (ArrayList<String>) WrapString.wrap(comment, fm, w);
	        if (list != null && list.size() > 0) {
	            int ystart = h - cHeight;
	            if (list.size() * height > cHeight) {
	            	cHeight = list.size() * height;
	            	if (cHeight > (int)(h * 0.4)) {
	            		cHeight = (int)(h * 0.4);
	            	}
	                ystart = h - cHeight;
	            }
	            // Draw background
	            Color oriColor = g.getColor();
	            g.setColor(new Color(0, 0, 0, 100));
	            g.fillRect(x, ystart-height, w, cHeight + height * 2);
	            g.setColor(Color.white);
	            g.setFont(font);
	            int i = 0;
	            for (String s : list) {
	                g.drawString(s, x, ystart + height * i);
	                i++;
	            }
	            g.setColor(oriColor);
	        }
        }
        return cHeight;
    }
}
