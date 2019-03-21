package featurecat.lizzie.rules;

import featurecat.lizzie.analysis.GameInfo;
import java.util.List;
import java.util.Optional;

/** Linked list data structure to store board history */
public class BoardHistoryList {
  private GameInfo gameInfo;
  private BoardHistoryNode head;

  /**
   * Initialize a new board history list, whose first node is data
   *
   * @param data the data to be stored for the first entry
   */
  public BoardHistoryList(BoardData data) {
    head = new BoardHistoryNode(data);
    gameInfo = new GameInfo();
  }

  public GameInfo getGameInfo() {
    return gameInfo;
  }

  public void setGameInfo(GameInfo gameInfo) {
    this.gameInfo = gameInfo;
  }

  public BoardHistoryList shallowCopy() {
    BoardHistoryList copy = new BoardHistoryList(null);
    copy.head = head;
    copy.gameInfo = gameInfo;
    return copy;
  }

  /** Clear history. */
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
    addOrGoto(data, false);
  }

  public void addOrGoto(BoardData data, boolean newBranch) {
    head = head.addOrGoto(data, newBranch);
  }

  /**
   * moves the pointer to the left, returns the data stored there
   *
   * @return data of previous node, Optional.empty if there is no previous node
   */
  public Optional<BoardData> previous() {
    if (!head.previous().isPresent()) return Optional.empty();
    else head = head.previous().get();

    return Optional.of(head.getData());
  }

  public void toStart() {
    while (previous().isPresent()) ;
  }

  /**
   * moves the pointer to the right, returns the data stored there
   *
   * @return the data of next node, Optional.empty if there is no next node
   */
  public Optional<BoardData> next() {
    Optional<BoardHistoryNode> n = head.next();
    n.ifPresent(x -> head = x);
    return n.map(x -> x.getData());
  }

  /**
   * Moves the pointer to the variation number idx, returns the data stored there.
   *
   * @return the data of next node, Optional.empty if there is no variation with index.
   */
  public Optional<BoardData> nextVariation(int idx) {
    Optional<BoardHistoryNode> n = head.getVariation(idx);
    n.ifPresent(x -> head = x);
    return n.map(x -> x.getData());
  }

  /**
   * Does not change the pointer position
   *
   * @return the data stored at the next index, if any, Optional.empty otherwise.
   */
  public Optional<BoardData> getNext() {
    return head.next().map(x -> x.getData());
  }

  /** @return nexts for display */
  public List<BoardHistoryNode> getNexts() {
    return head.getVariations();
  }

  /**
   * Does not change the pointer position.
   *
   * @return the data stored at the previous index, if any, Optional.empty otherwise.
   */
  public Optional<BoardData> getPrevious() {
    return head.previous().map(p -> p.getData());
  }

  /** @return the data of the current node */
  public BoardData getData() {
    return head.getData();
  }

  public void setStone(int[] coordinates, Stone stone) {
    if (!Board.isValid(coordinates[0], coordinates[1])) return;
    int index = Board.getIndex(coordinates[0], coordinates[1]);
    head.getData().stones[index] = stone;
    head.getData().zobrist.toggleStone(coordinates[0], coordinates[1], stone);
  }

  public Stone[] getStones() {
    return head.getData().stones;
  }

  public Optional<int[]> getLastMove() {
    return head.getData().lastMove;
  }

  public Optional<int[]> getNextMove() {
    return getNext().flatMap(n -> n.lastMove);
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

  public int getMoveMNNumber() {
    return head.getData().moveMNNumber;
  }

  public int[] getMoveNumberList() {
    return head.getData().moveNumberList;
  }

  public BoardHistoryNode getCurrentHistoryNode() {
    return head;
  }

  /**
   * @param data the board position to check against superko
   * @return whether or not the given position violates the superko rule at the head's state
   */
  public boolean violatesSuperko(BoardData data) {
    BoardHistoryNode head = this.head;

    // check to see if this position has occurred before
    while (head.previous().isPresent()) {
      // if two zobrist hashes are equal, and it's the same player to coordinate, they are the same
      // position
      if (data.zobrist.equals(head.getData().zobrist)
          && data.blackToPlay == head.getData().blackToPlay) return true;

      head = head.previous().get();
    }

    // no position matched this position, so it's valid
    return false;
  }

  public boolean violatesKoRule(BoardData data) {
    // check if the current position is identical to the position two moves ago
    return this.head
        .previous()
        .map(p -> p != null && data.zobrist.equals(p.getData().zobrist))
        .orElse(false);
  }

  /**
   * Returns the root node
   *
   * @return root node
   */
  public BoardHistoryNode root() {
    BoardHistoryNode top = head;
    while (top.previous().isPresent()) {
      top = top.previous().get();
    }
    return top;
  }

  /**
   * Returns the length of current branch
   *
   * @return length of current branch
   */
  public int currentBranchLength() {
    return getMoveNumber() + head.getDepth();
  }

  /**
   * Returns the length of main trunk
   *
   * @return length of main trunk
   */
  public int mainTrunkLength() {
    return root().getDepth();
  }
}
