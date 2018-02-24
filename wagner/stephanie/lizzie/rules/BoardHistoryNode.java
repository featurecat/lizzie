package wagner.stephanie.lizzie.rules;

/**
 * Node structure for a special doubly linked list
 */
public class BoardHistoryNode {
    private BoardHistoryNode previous;
    private BoardHistoryNode next;

    private BoardData data;

    /**
     * Initializes a new list node
     */
    public BoardHistoryNode(BoardData data) {
        previous = null;
        next = null;
        this.data = data;
    }

    /**
     * Sets up for a new node. Overwrites future history.
     *
     * @param node the node following this one
     * @return the node that was just set
     */
    public BoardHistoryNode add(BoardHistoryNode node) {
        next = node;
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
        return next;
    }
}
