package wagner.stephanie.lizzie.rules;
import java.util.ArrayList;

/**
 * Node structure for a special doubly linked list
 */
public class BoardHistoryNode {
    private BoardHistoryNode previous;
    private ArrayList<BoardHistoryNode> nexts;

    private BoardData data;

    /**
     * Initializes a new list node
     */
    public BoardHistoryNode(BoardData data) {
        previous = null;
        nexts = new ArrayList<BoardHistoryNode>();
        this.data = data;
    }

    /**
     * Remove all subsequent nodes.
     */
    public void clear() {
        nexts.clear();
    }

    /**
     * Sets up for a new node. Overwrites future history.
     *
     * @param node the node following this one
     * @return the node that was just set
     */
    public BoardHistoryNode add(BoardHistoryNode node) {
        nexts.clear();
        nexts.add(node);
        node.previous = this;

        return node;
    }

    /**
     * If we already have a next node with the same BoardData, move to it,
     * otherwise add it and move to it.
     *
     * @param node the node following this one
     * @return the node that was just set
     */
    public BoardHistoryNode addOrGoto(BoardData data) {
        for (int i = 0; i < nexts.size(); i++) {
            if (nexts.get(i).data.zobrist.equals(data.zobrist)) {
                return nexts.get(i);
            }
        }
        BoardHistoryNode node = new BoardHistoryNode(data);
        nexts.add(node);
        node.previous = this;

        return node;
    }

    /**
     * @return data stored on this node
     */
    public BoardData getData() {
        return data;
    }

    public BoardHistoryNode previous() {
        return previous;
    }

    public BoardHistoryNode next() {
        if (nexts.size() == 0) {
            return null;
        } else {
            return nexts.get(0);
        }
    }
}
