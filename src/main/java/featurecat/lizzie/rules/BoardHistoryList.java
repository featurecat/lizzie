package featurecat.lizzie.rules;

import featurecat.lizzie.analysis.GameInfo;
import java.util.List;

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
    head = head.addOrGoto(data);
  }

  /**
   * moves the pointer to the left, returns the data stored there
   *
   * @return data of previous node, null if there is no previous node
   */
  public BoardData previous() {
    if (head.previous() == null) return null;
    else head = head.previous();

    return head.getData();
  }

  public void toStart() {
    while (previous() != null) ;
  }

  /**
   * moves the pointer to the right, returns the data stored there
   *
   * @return the data of next node, null if there is no next node
   */
  public BoardData next() {
    if (head.next() == null) return null;
    else head = head.next();

    return head.getData();
  }

  /**
   * moves the pointer to the variation number idx, returns the data stored there
   *
   * @return the data of next node, null if there is no variaton with index
   */
  public BoardData nextVariation(int idx) {
    if (head.getVariation(idx) == null) return null;
    else head = head.getVariation(idx);

    return head.getData();
  }

  /**
   * Does not change the pointer position
   *
   * @return the data stored at the next index. null if not present
   */
  public BoardData getNext() {
    if (head.next() == null) return null;
    else return head.next().getData();
  }

  /** @return nexts for display */
  public List<BoardHistoryNode> getNexts() {
    return head.getNexts();
  }

  /**
   * Does not change the pointer position
   *
   * @return the data stored at the previous index. null if not present
   */
  public BoardData getPrevious() {
    if (head.previous() == null) return null;
    else return head.previous().getData();
  }

  /** @return the data of the current node */
  public BoardData getData() {
    return head.getData();
  }

  public void setStone(int[] coordinates, Stone stone) {
    int index = Board.getIndex(coordinates[0], coordinates[1]);
    head.getData().stones[index] = stone;
    head.getData().zobrist.toggleStone(coordinates[0], coordinates[1], stone);
  }

  public Stone[] getStones() {
    return head.getData().stones;
  }

  public int[] getLastMove() {
    return head.getData().lastMove;
  }

  public int[] getNextMove() {
    BoardData next = getNext();
    if (next == null) return null;
    else return next.lastMove;
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
    while (head.previous() != null) {
      // if two zobrist hashes are equal, and it's the same player to coordinate, they are the same
      // position
      if (data.zobrist.equals(head.getData().zobrist)
          && data.blackToPlay == head.getData().blackToPlay) return true;

      head = head.previous();
    }

    // no position matched this position, so it's valid
    return false;
  }

  /**
   * Returns the root node
   *
   * @return root node
   */
  public BoardHistoryNode root() {
    BoardHistoryNode top = head;
    while (top.previous() != null) {
      top = top.previous();
    }
    return top;
  }

  /**
   * Returns the length of current branch
   *
   * @return length of current branch
   */
  public int currentBranchLength() {
    return getMoveNumber() + BoardHistoryList.getDepth(head);
  }

  /**
   * Returns the length of main trunk
   *
   * @return length of main trunk
   */
  public int mainTrunkLength() {
    return BoardHistoryList.getDepth(root());
  }

  /*
   * Static helper methods
   */

  /**
   * Returns the number of moves in a tree (only the left-most (trunk) variation)
   *
   * @return number of moves in a tree
   */
  public static int getDepth(BoardHistoryNode node) {
    int c = 0;
    while (node.next() != null) {
      c++;
      node = node.next();
    }
    return c;
  }

  /**
   * Check if there is a branch that is at least depth deep (at least depth moves)
   *
   * @return true if it is deep enough, false otherwise
   */
  public static boolean hasDepth(BoardHistoryNode node, int depth) {
    int c = 0;
    if (depth <= 0) return true;
    while (node.next() != null) {
      if (node.numberOfChildren() > 1) {
        for (int i = 0; i < node.numberOfChildren(); i++) {
          if (hasDepth(node.getVariation(i), depth - c - 1)) return true;
        }
        return false;
      } else {
        node = node.next();
        c++;
        if (c >= depth) return true;
      }
    }
    return false;
  }

  /**
   * Find top of variation (the first move that is on the main trunk)
   *
   * @return top of variaton, if on main trunk, return start move
   */
  public static BoardHistoryNode findTop(BoardHistoryNode start) {
    BoardHistoryNode top = start;
    while (start.previous() != null) {
      if (start.previous().next() != start) {
        top = start.previous();
      }
      start = start.previous();
    }
    return top;
  }

  /**
   * Find first move with variations in tree above node
   *
   * @return The child (in the current variation) of the first node with variations
   */
  public static BoardHistoryNode findChildOfPreviousWithVariation(BoardHistoryNode node) {
    while (node.previous() != null) {
      if (node.previous().numberOfChildren() > 1) {
        return node;
      }
      node = node.previous();
    }
    return null;
  }

  /**
   * Given a parent node and a child node, find the index of the child node
   *
   * @return index of child node, -1 if child node not a child of parent
   */
  public static int findIndexOfNode(BoardHistoryNode parentNode, BoardHistoryNode childNode) {
    if (parentNode.next() == null) return -1;
    for (int i = 0; i < parentNode.numberOfChildren(); i++) {
      if (parentNode.getVariation(i) == childNode) return i;
    }
    return -1;
  }

  /**
   * Check if node is part of the main trunk (rightmost branch)
   *
   * @return true if node is part of main trunk, false otherwise
   */
  public static boolean isMainTrunk(BoardHistoryNode node) {
    while (node.previous() != null) {
      if (node.previous().next() != node) {
        return false;
      }
      node = node.previous();
    }
    return true;
  }
}
