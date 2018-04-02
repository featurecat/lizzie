package wagner.stephanie.lizzie.rules;

/**
 * Linked list data structure to store board history
 */
public class BoardHistoryList {
    private BoardHistoryNode head;

    /**
     * Initialize a new board history list, whose first node is data
     *
     * @param data the data to be stored for the first entry
     */
    public BoardHistoryList(BoardData data) {
        head = new BoardHistoryNode(data);
    }

    /**
     * Clear history.
     */
    public void clear() {
        head.clear();
    }

    /**
     * Add new data after head. Overwrites any data that may have been stored after head.
     *
     * @param data the data to add
     */
    public void add(BoardData data) {
        head = head.add(new BoardHistoryNode(data));
    }

    public void addOrGoto(BoardData data) {
        head = head.addOrGoto(data);
    }

    /**
     * moves the pointer to the left, returns the data stored there
     *
     * @return data of previous node, null if there is no previous node
     */
    public BoardData previous() {
        if (head.previous() == null)
            return null;
        else
            head = head.previous();

        return head.getData();
    }

    /**
     * moves the pointer to the right, returns the data stored there
     *
     * @return the data of next node, null if there is no next node
     */
    public BoardData next() {
        if (head.next() == null)
            return null;
        else
            head = head.next();

        return head.getData();
    }

    /**
     * Does not change the pointer position
     *
     * @return the data stored at the next index. null if not present
     */
    public BoardData getNext() {
        if (head.next() == null)
            return null;
        else
            return head.next().getData();
    }

    /**
     * @return the data of the current node
     */
    public BoardData getData() {
        return head.getData();
    }

    public Stone[] getStones() {
        return head.getData().stones;
    }

    public int[] getLastMove() {
        return head.getData().lastMove;
    }

    public int[] getNextMove() {
        BoardData next = getNext();
        if (next == null)
            return null;
        else
            return next.lastMove;
    }

    public Stone getLastMoveColor() {
        return head.getData().lastMoveColor;
    }

    public boolean isBlacksTurn() {
        return head.getData().blackToPlay;
    }

    public Zobrist getZobrist() {
        return head.getData().zobrist.clone();
    }

    public int getMoveNumber() {
        return head.getData().moveNumber;
    }

    public int[] getMoveNumberList() {
        return head.getData().moveNumberList;
    }

    /**
     * @param data the board position to check against superko
     * @return whether or not the given position violates the superko rule at the head's state
     */
    public boolean violatesSuperko(BoardData data) {
        BoardHistoryNode head = this.head;

        // check to see if this position has occurred before
        while (head.previous() != null) {
            // if two zobrist hashes are equal, and it's the same player to coordinate, they are the same position
            if (data.zobrist.equals(head.getData().zobrist) && data.blackToPlay == head.getData().blackToPlay)
                return true;

            head = head.previous();
        }

        // no position matched this position, so it's valid
        return false;
    }
}
