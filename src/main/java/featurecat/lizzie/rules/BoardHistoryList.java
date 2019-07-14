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

  public void addOrGoto(BoardData data, boolean newBranch, boolean changeMove) {
    head = head.addOrGoto(data, newBranch, changeMove);
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

  public void toBranchTop() {
    BoardHistoryNode start = head;
    while (start.previous().isPresent()) {
      BoardHistoryNode pre = start.previous().get();
      if (pre.next(true).isPresent() && pre.next(true).get() != start) {
        previous();
        break;
      }
      previous();
      start = pre;
    }
  }

  /**
   * moves the pointer to the right, returns the data stored there
   *
   * @return the data of next node, Optional.empty if there is no next node
   */
  public Optional<BoardData> next() {
    return next(false);
  }

  public Optional<BoardData> next(boolean includeDummay) {
    Optional<BoardHistoryNode> n = head.next(includeDummay);
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
    return getNext(false);
  }

  public Optional<BoardData> getNext(boolean includeDummy) {
    return head.next(includeDummy).map(x -> x.getData());
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

  public void setCurrentHistoryNode(BoardHistoryNode node) {
    head = node;
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

  public BoardHistoryNode getEnd() {
    BoardHistoryNode e = head;
    while (e.next().isPresent()) {
      e = e.next().get();
    }
    return e;
  }

  public void pass(Stone color) {
    pass(color, false, false, false);
  }

  public void pass(Stone color, boolean newBranch) {
    pass(color, newBranch, false, false);
  }

  public void pass(Stone color, boolean newBranch, boolean dummy) {
    pass(color, newBranch, dummy, false);
  }

  public void pass(Stone color, boolean newBranch, boolean dummy, boolean changeMove) {
    synchronized (this) {

      // check to see if this move is being replayed in history
      if (this.getNext().map(n -> !n.lastMove.isPresent()).orElse(false) && !newBranch) {
        // this is the next move in history. Just increment history so that we don't erase the
        // redo's
        this.next();
        return;
      }

      Stone[] stones = this.getStones().clone();
      Zobrist zobrist = this.getZobrist();
      int moveNumber = this.getMoveNumber() + 1;
      int[] moveNumberList =
          newBranch && this.getNext(true).isPresent()
              ? new int[Board.boardWidth * Board.boardHeight]
              : this.getMoveNumberList().clone();

      // build the new game state
      BoardData newState =
          new BoardData(
              stones,
              Optional.empty(),
              color,
              color.equals(Stone.WHITE),
              zobrist,
              moveNumber,
              moveNumberList,
              this.getData().blackCaptures,
              this.getData().whiteCaptures,
              0,
              0);
      newState.dummy = dummy;

      // update history with pass
      this.addOrGoto(newState, newBranch, changeMove);
    }
  }

  public void place(int x, int y, Stone color) {
    place(x, y, color, false);
  }

  public void place(int x, int y, Stone color, boolean newBranch) {
    place(x, y, color, newBranch, false);
  }

  public void place(int x, int y, Stone color, boolean newBranch, boolean changeMove) {
    place(x, y, color, newBranch, changeMove, false);
  }

  public void place(
      int x, int y, Stone color, boolean newBranch, boolean changeMove, boolean mainMove) {
    synchronized (this) {
      if (!Board.isValid(x, y)
          || (this.getStones()[Board.getIndex(x, y)] != Stone.EMPTY && !newBranch)) return;

      double nextWinrate = -100;
      if (this.getData().winrate >= 0) nextWinrate = 100 - this.getData().winrate;

      // check to see if this coordinate is being replayed in variation history
      for (int i = 0; i < this.getNexts().size(); ++i) {
        Optional<int[]> nextLast = this.getNexts().get(i).getData().lastMove;
        if (nextLast.isPresent()
            && nextLast.get()[0] == x
            && nextLast.get()[1] == y
            && !newBranch
            && !changeMove) {
          // this is the next coordinate in history. Just increment history so that we don't erase
          // the
          // redo's
          Optional<BoardData> data = this.nextVariation(i);
          if (mainMove) {
            data.ifPresent(n -> n.main = mainMove);
          }
          return;
        }
      }

      // load a copy of the data at the current node of history
      Stone[] stones = this.getStones().clone();
      Zobrist zobrist = this.getZobrist();
      Optional<int[]> lastMove = Optional.of(new int[] {x, y});
      int moveNumber = this.getMoveNumber() + 1;
      int moveMNNumber =
          this.getMoveMNNumber() > -1 && !newBranch ? this.getMoveMNNumber() + 1 : -1;
      int[] moveNumberList =
          newBranch && this.getNext(true).isPresent()
              ? new int[Board.boardWidth * Board.boardHeight]
              : this.getMoveNumberList().clone();

      moveNumberList[Board.getIndex(x, y)] = moveMNNumber > -1 ? moveMNNumber : moveNumber;

      // set the stone at (x, y) to color
      stones[Board.getIndex(x, y)] = color;
      zobrist.toggleStone(x, y, color);

      // remove enemy stones
      int capturedStones = 0;
      capturedStones += Board.removeDeadChain(x + 1, y, color.opposite(), stones, zobrist);
      capturedStones += Board.removeDeadChain(x, y + 1, color.opposite(), stones, zobrist);
      capturedStones += Board.removeDeadChain(x - 1, y, color.opposite(), stones, zobrist);
      capturedStones += Board.removeDeadChain(x, y - 1, color.opposite(), stones, zobrist);

      // check to see if the player made a suicidal coordinate
      int isSuicidal = Board.removeDeadChain(x, y, color, stones, zobrist);

      for (int i = 0; i < Board.boardWidth * Board.boardHeight; i++) {
        if (stones[i].equals(Stone.EMPTY)) {
          moveNumberList[i] = 0;
        }
      }

      int bc = this.getData().blackCaptures;
      int wc = this.getData().whiteCaptures;
      if (color.isBlack()) bc += capturedStones;
      else wc += capturedStones;
      BoardData newState =
          new BoardData(
              stones,
              lastMove,
              color,
              color.equals(Stone.WHITE),
              zobrist,
              moveNumber,
              moveNumberList,
              bc,
              wc,
              nextWinrate,
              0);
      newState.moveMNNumber = moveMNNumber;
      newState.main = mainMove;

      // don't make this coordinate if it is suicidal or violates superko
      if (isSuicidal > 0 || this.violatesKoRule(newState)) return;

      // update history with this coordinate
      this.addOrGoto(newState, newBranch, changeMove);
    }
  }

  public void addNodeProperty(String key, String value) {
    synchronized (this) {
      this.getData().addProperty(key, value);
      if ("MN".equals(key)) {
        moveNumber(Integer.parseInt(value));
      }
    }
  }

  public void moveNumber(int moveNumber) {
    synchronized (this) {
      BoardData data = this.getData();
      if (data.lastMove.isPresent()) {
        int[] moveNumberList = this.getMoveNumberList();
        moveNumberList[Board.getIndex(data.lastMove.get()[0], data.lastMove.get()[1])] = moveNumber;
        Optional<BoardHistoryNode> node = this.getCurrentHistoryNode().previous();
        while (node.isPresent() && node.get().numberOfChildren() <= 1) {
          BoardData nodeData = node.get().getData();
          if (nodeData.lastMove.isPresent() && nodeData.moveNumber >= moveNumber) {
            moveNumber = (moveNumber > 1) ? moveNumber - 1 : 0;
            moveNumberList[Board.getIndex(nodeData.lastMove.get()[0], nodeData.lastMove.get()[1])] =
                moveNumber;
          }
          node = node.get().previous();
        }
      }
    }
  }

  public void addStone(int x, int y, Stone color) {
    synchronized (this) {
      if (!Board.isValid(x, y) || this.getStones()[Board.getIndex(x, y)] != Stone.EMPTY) return;

      Stone[] stones = this.getData().stones;
      Zobrist zobrist = this.getData().zobrist;

      // set the stone at (x, y) to color
      stones[Board.getIndex(x, y)] = color;
      zobrist.toggleStone(x, y, color);
    }
  }

  public void removeStone(int x, int y, Stone color) {
    synchronized (this) {
      if (!Board.isValid(x, y) || this.getStones()[Board.getIndex(x, y)] == Stone.EMPTY) return;

      BoardData data = this.getData();
      Stone[] stones = data.stones;
      Zobrist zobrist = data.zobrist;

      // set the stone at (x, y) to empty
      Stone oriColor = stones[Board.getIndex(x, y)];
      stones[Board.getIndex(x, y)] = Stone.EMPTY;
      zobrist.toggleStone(x, y, oriColor);
      data.moveNumberList[Board.getIndex(x, y)] = 0;
    }
  }

  public void flatten() {
    Stone[] stones = this.getStones();
    boolean blackToPlay = this.isBlacksTurn();
    Zobrist zobrist = this.getZobrist().clone();
    BoardHistoryList oldHistory = this;

    head =
        new BoardHistoryNode(
            new BoardData(
                stones,
                Optional.empty(),
                Stone.EMPTY,
                blackToPlay,
                zobrist,
                0,
                new int[Board.boardWidth * Board.boardHeight],
                0,
                0,
                0.0,
                0));
    this.setGameInfo(oldHistory.getGameInfo());
  }

  public boolean goToMoveNumber(int moveNumber, boolean withinBranch) {
    int delta = moveNumber - this.getMoveNumber();
    boolean moved = false;
    for (int i = 0; i < Math.abs(delta); i++) {
      if (withinBranch && delta < 0) {
        BoardHistoryNode currentNode = this.getCurrentHistoryNode();
        if (!currentNode.isFirstChild()) {
          break;
        }
      }
      if (!(delta > 0 ? next().isPresent() : previous().isPresent())) {
        break;
      }
      moved = true;
    }
    return moved;
  }

  public int sync(BoardHistoryList newList) {
    int diffMoveNo = -1;

    BoardHistoryNode node = this.getCurrentHistoryNode();
    BoardHistoryNode prev = node.previous().map(p -> p).orElse(null);
    // From begin
    while (prev != null) {
      node = prev;
      prev = node.previous().map(p -> p).orElse(null);
    }
    // Compare
    BoardHistoryNode newNode = newList.getCurrentHistoryNode();

    while (newNode != null) {
      if (node == null) {
        // Add
        prev.addOrGoto(newNode.getData().clone());
        node = prev.next().map(n -> n).orElse(null);
        if (diffMoveNo < 0) {
          diffMoveNo = newNode.getData().moveNumber;
        }
        node.sync(newNode);
        break;
      } else {
        if (!node.compare(newNode)) {
          if (diffMoveNo < 0) {
            diffMoveNo = newNode.getData().moveNumber;
          }
          node.sync(newNode);
          break;
        }
      }
      prev = node;
      node = node.next(true).map(n -> n).orElse(null);
      newNode = newNode.next(true).map(n -> n).orElse(null);
    }

    return diffMoveNo;
  }
}
