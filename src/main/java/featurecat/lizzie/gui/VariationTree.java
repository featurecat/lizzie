package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;

import java.awt.*;
import java.util.ArrayList;

public class VariationTree {

    private int YSPACING;
    private int XSPACING;
    private int DOT_DIAM = 11; // Should be odd number

    private ArrayList<Integer> laneUsageList, laneUsageList1;
    private BoardHistoryNode curMove, curMove1;

	private int posx1, posy1, width1, height1;   //Reserve a copy of varaition window axis  

    public VariationTree()
    {
        laneUsageList = new ArrayList<Integer>();
		laneUsageList1 = new ArrayList<Integer>();  //for mouse click event
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

	    posx1=posx;posy1=posy;width1=width;height1=height;  //backup for mouse click event

        // Draw background
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRect(posx, posy, width, height);

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
	
	 
    //Clone from drawTree() method but draw nothing, just caculate (x,y)->BoardNode
	public BoardHistoryNode inSubtree(int posx, int posy, int mouseX, int mouseY, int startLane, int maxposy, BoardHistoryNode startNode, int variationNumber)
	{
        // Finds depth on leftmost variation of this tree
        int depth = BoardHistoryList.getDepth(startNode) + 1;
        int lane = startLane;
        // Figures out how far out too the right (which lane) we have to go not to collide with other variations
        while (lane < laneUsageList1.size() && laneUsageList1.get(lane) <= startNode.getData().moveNumber + depth) {
            // laneUsageList keeps a list of how far down it is to a variation in the different "lanes"
            laneUsageList1.set(lane, startNode.getData().moveNumber - 1);
            lane++;
        }
        if (lane >= laneUsageList1.size())
        {
                laneUsageList1.add(0);
        }
        if (variationNumber > 1)
            laneUsageList1.set(lane - 1, startNode.getData().moveNumber - 1);
        laneUsageList1.set(lane, startNode.getData().moveNumber);

        // At this point, lane contains the lane we should use (the main branch is in lane 0)

        BoardHistoryNode cur  = startNode;
        int curposx = posx + lane*XSPACING;
        int dotoffset = DOT_DIAM/2;


        // Check first node 
		if ( Math.abs(mouseX-curposx-dotoffset) < XSPACING/2 && Math.abs(mouseY-posy-dotoffset) < YSPACING/2)
			return startNode;


        // Draw main line
        while (cur.next() != null && posy + YSPACING < maxposy) {
            posy += YSPACING;
            cur = cur.next();
			if (Math.abs(mouseX-curposx-dotoffset) < XSPACING/2 && Math.abs(mouseY-posy-dotoffset) < YSPACING/2)
				return cur;

        }
        // Now we have drawn all the nodes in this variation, and has reached the bottom of this variation
        // Move back up, and for each, draw any variations we find
        while (cur.previous() != null && cur != startNode) {
            cur = cur.previous();
            int curwidth = lane;
			BoardHistoryNode ret;
            // Draw each variation, uses recursion
            for (int i = 1; i < cur.numberOfChildren(); i++) {
                curwidth++;
                // Recursion, depth of recursion will normally not be very deep (one recursion level for every variation that has a variation (sort of))
                if((ret=inSubtree(posx, posy, mouseX, mouseY, curwidth, maxposy, cur.getVariation(i), i)) !=null)
					return ret;
            }
            posy -= YSPACING;
        }
		return null;
    }
    
    //Clone from draw() method but draw nothing, just caculate (x,y)->BoardNode
	public BoardHistoryNode inVariationTree(int mouseX, int mouseY) {

	    //check if (x,y) is in the variation window
	    if (mouseX < posx1 || mouseX > posx1+width1 || mouseY < posy1 || mouseY > posy1+height1)
		    return null;
	 
        // Use dense tree for saving space if large-subboard
        YSPACING = (Lizzie.config.showLargeSubBoard() ? 20 : 30);
        XSPACING = YSPACING;


        int middleY = posy1 + height1/2;
        int xoffset = 30;
        laneUsageList1.clear();

        curMove1 = Lizzie.board.getHistory().getCurrentHistoryNode();

        // Is current move a variation? If so, find top of variation
        BoardHistoryNode top = BoardHistoryList.findTop(curMove1);
        int curposy = middleY - YSPACING*(curMove1.getData().moveNumber - top.getData().moveNumber);
        // Go to very top of tree (visible in assigned area)
        BoardHistoryNode node = top;
        while  (curposy > posy1 + YSPACING && node.previous() != null) {
            node = node.previous();
            curposy -= YSPACING;
        }
        return inSubtree(posx1 + xoffset, curposy, mouseX, mouseY,0, posy1 + height1, node, 0);
	
    }
	
	public int jumpVariationTree(int mouseX, int mouseY){
		BoardHistoryNode node;
		node = inVariationTree(mouseX, mouseY);    //check if (x,y) hit proper branch node
        if (node==null) return -1;  //check if any error occur
        Lizzie.board.jumpToAnyPosition(node);  
        return 0;
	}
}
