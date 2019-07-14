package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Node structure for the board history / sgf tree */
public class BoardHistoryNode {
  private Optional<BoardHistoryNode> previous;
  private ArrayList<BoardHistoryNode> variations;

  private BoardData data;

  // Save the children for restore to branch
  private int fromBackChildren;

  /** Initializes a new list node */
  public BoardHistoryNode(BoardData data) {
    previous = Optional.empty();
    variations = new ArrayList<BoardHistoryNode>();
    this.data = data;
  }

  /** Remove all subsequent nodes. */
  public void clear() {
    variations.clear();
  }

  /**
   * Sets up for a new node. Overwrites future history.
   *
   * @param node the node following this one
   * @return the node that was just set
   */
  public BoardHistoryNode add(BoardHistoryNode node) {
    variations.clear();
    variations.add(node);
    node.previous = Optional.of(this);

    return node;
  }

  /**
   * If we already have a next node with the same BoardData, move to it, otherwise add it and move
   * to it.
   *
   * @param data the node following this one
   * @return the node that was just set
   */
  public BoardHistoryNode addOrGoto(BoardData data) {
    return addOrGoto(data, false);
  }

  public BoardHistoryNode addOrGoto(BoardData data, boolean newBranch) {
    return addOrGoto(data, newBranch, false);
  }

  /**
   * If we already have a next node with the same BoardData, move to it, otherwise add it and move
   * to it.
   *
   * @param data the node following this one
   * @param newBranch add a new branch
   * @return the node that was just set
   */
  public BoardHistoryNode addOrGoto(BoardData data, boolean newBranch, boolean changeMove) {
    // If you play a hand and immediately return it, it is most likely that you have made a mistake.
    // Ask whether to delete the previous node.
    //        if (!variations.isEmpty() && !variations.get(0).data.zobrist.equals(data.zobrist)) {
    //            // You may just mark this hand, so its not necessarily wrong. Answer when the
    // first query is wrong or it will not ask whether the move is wrong.
    //            if (!variations.get(0).data.verify) {
    //                int ret = JOptionPane.showConfirmDialog(null, "Do you want undo?", "Undo",
    // JOptionPane.OK_CANCEL_OPTION);
    //                if (ret == JOptionPane.OK_OPTION) {
    //                    variations.remove(0);
    //                } else {
    //                    variations.get(0).data.verify = true;
    //                }
    //            }
    //        }
    if (!newBranch) {
      for (int i = 0; i < variations.size(); i++) {
        if (variations.get(i).data.zobrist.equals(data.zobrist)) {
          //                if (i != 0) {
          //                    // Swap selected next to foremost
          //                    BoardHistoryNode currentNext = variations.get(i);
          //                    variations.set(i, variations.get(0));
          //                    variations.set(0, currentNext);
          //                }
          if (i != 0 && changeMove) {
            break;
          }
          return variations.get(i);
        }
      }
    }
    if (!this.previous.isPresent()) {
      data.moveMNNumber = 1;
    }
    Optional<BoardHistoryNode> next = next(true);
    boolean nextDummy = next.isPresent() && next.get().isEndDummay();
    if (!newBranch && nextDummy) {
      changeMove = true;
    }
    if (Lizzie.config.newMoveNumberInBranch && !variations.isEmpty() && !changeMove) {
      if (!newBranch) {
        data.moveNumberList = new int[Board.boardWidth * Board.boardHeight];
        data.moveMNNumber = -1;
      }
      if (data.moveMNNumber == -1) {
        data.moveMNNumber = data.dummy ? 0 : 1;
      }
      data.lastMove.ifPresent(
          m -> data.moveNumberList[Board.getIndex(m[0], m[1])] = data.moveMNNumber);
    }
    BoardHistoryNode node = new BoardHistoryNode(data);
    if (changeMove) {
      next.ifPresent(
          n -> {
            node.variations = n.variations;
            n.previous = null;
            n.variations.stream().forEach(v -> v.previous = Optional.of(node));
            variations.set(0, node);
          });
    } else {
      // Add node
      variations.add(node);
    }
    node.previous = Optional.of(this);

    return node;
  }

  /** @return data stored on this node */
  public BoardData getData() {
    return data;
  }

  /** @return variations for display */
  public List<BoardHistoryNode> getVariations() {
    return variations;
  }

  public Optional<BoardHistoryNode> previous() {
    return previous;
  }

  public Optional<BoardHistoryNode> next() {
    return next(false);
  }

  public Optional<BoardHistoryNode> next(boolean includeDummy) {
    return variations.isEmpty() || (!includeDummy && variations.get(0).isEndDummay())
        ? Optional.empty()
        : Optional.of(variations.get(0));
  }

  public boolean isEndDummay() {
    return this.data.dummy && variations.isEmpty();
  }

  public BoardHistoryNode topOfBranch() {
    BoardHistoryNode top = this;
    while (top.previous.isPresent() && top.previous.get().variations.size() == 1) {
      top = top.previous.get();
    }
    return top;
  }

  public int numberOfChildren() {
    return variations.size();
  }

  public boolean hasVariations() {
    return numberOfChildren() > 1;
  }

  public boolean isFirstChild() {
    return previous.flatMap(p -> p.next().map(n -> n == this)).orElse(false);
  }

  /** main属性を持つ子ノードがあればそれを返します。 */
  public Optional<BoardHistoryNode> getMainChild() {
    for (BoardHistoryNode node : variations) {
      if (node.getData().main) return Optional.of(node);
    }

    return Optional.empty();
  }

  /** サブツリーすべてにおいてmain属性を解除します。 */
  public void resetMainSubTree() {
    for (BoardHistoryNode node : variations) {
      node.resetMainSubTree();
    }

    this.data.main = false;
  }

  public Optional<BoardHistoryNode> getVariation(int idx) {
    if (variations.size() <= idx) {
      return Optional.empty();
    } else {
      return Optional.of(variations.get(idx));
    }
  }

  /**
   * nodeが何番目の変化になるか調べます。
   *
   * @param node 次ノードの候補になるノード
   */
  public Optional<Integer> getVariationIdx(BoardHistoryNode node) {
    for (int idx = 0; idx < variations.size(); ++idx) {
      if (variations.get(idx) == node) {
        return Optional.of(idx);
      }
    }

    return Optional.empty();
  }

  /**
   * 開始局面からこのtargetの途中にこのノードがあるか調べます。
   *
   * @param endNode 調べる経路の末端となるノード
   */
  public boolean inTheMiddle(BoardHistoryNode endNode) {
    while (endNode != this) {
      if (!endNode.previous().isPresent()) return false;
      endNode = endNode.previous().get();
    }

    return true;
  }

  /**
   * this から endNode に移動するため必要なvariationsのidxのリストを返します。
   * この戻り値の通りにvariationsのidxをたどるとthisからendNodeにノードの移動ができます。
   *
   * @param endNode
   */
  public Optional<ArrayList<Integer>> getGotoVariationIdxList(BoardHistoryNode endNode) {
    ArrayList<Integer> idxList = new ArrayList<Integer>();

    while (endNode != this) {
      if (!endNode.previous().isPresent()) {
        return Optional.empty();
      }

      BoardHistoryNode previous = endNode.previous().get();
      Optional<Integer> idx = previous.getVariationIdx(endNode);
      if (!idx.isPresent()) {
        return Optional.empty();
      }

      idxList.add(idx.get());
      endNode = previous;
    }

    Collections.reverse(idxList);
    return Optional.of(idxList);
  }

  public void moveUp() {
    previous.ifPresent(p -> p.moveChildUp(this));
  }

  public void moveDown() {
    previous.ifPresent(p -> p.moveChildDown(this));
  }

  public void moveChildUp(BoardHistoryNode child) {
    for (int i = 1; i < variations.size(); i++) {
      if (variations.get(i).data.zobrist.equals(child.data.zobrist)) {
        BoardHistoryNode tmp = variations.get(i - 1);
        variations.set(i - 1, child);
        variations.set(i, tmp);
        if ((i - 1) == 0) {
          child.swapMoveNumberList(tmp);
        }
        return;
      }
    }
  }

  public void moveChildDown(BoardHistoryNode child) {
    for (int i = 0; i < variations.size() - 1; i++) {
      if (variations.get(i).data.zobrist.equals(child.data.zobrist)) {
        BoardHistoryNode tmp = variations.get(i + 1);
        variations.set(i + 1, child);
        variations.set(i, tmp);
        if (i == 0) {
          tmp.swapMoveNumberList(child);
        }
        return;
      }
    }
  }

  public void swapMoveNumberList(BoardHistoryNode child) {
    int childStart = child.getData().moveMNNumber;
    child.resetMoveNumberListBranch(this.getData().moveMNNumber);
    this.resetMoveNumberListBranch(childStart);
  }

  public void resetMoveNumberListBranch(int start) {
    this.resetMoveNumberList(start);
    BoardHistoryNode node = this;
    while (node.next().isPresent()) {
      node = node.next().get();
      start++;
      node.resetMoveNumberList(start);
    }
  }

  public void resetMoveNumberList(int start) {
    BoardData data = this.getData();
    int[] moveNumberList = data.moveNumberList;
    data.moveMNNumber = start;
    if (data.lastMove.isPresent() && !data.dummy) {
      int[] move = data.lastMove.get();
      moveNumberList[Board.getIndex(move[0], move[1])] = start;
    }
    Optional<BoardHistoryNode> node = this.previous();
    int moveNumber = start;
    while (node.isPresent()) {
      BoardData nodeData = node.get().getData();
      if (nodeData.lastMove.isPresent()) {
        int[] move = nodeData.lastMove.get();
        moveNumber = (moveNumber > 1) ? moveNumber - 1 : 0;
        moveNumberList[Board.getIndex(move[0], move[1])] = moveNumber;
      }
      node = node.get().previous();
    }
  }

  public void deleteChild(int idx) {
    if (idx < numberOfChildren()) {
      variations.remove(idx);
    }
  }

  /** @param fromBackChildren the fromBackChildren to set */
  public void setFromBackChildren(int fromBackChildren) {
    this.fromBackChildren = fromBackChildren;
  }

  /** @return the fromBackChildren */
  public int getFromBackChildren() {
    return fromBackChildren;
  }

  /**
   * Returns the number of moves in a tree (only the left-most (trunk) variation)
   *
   * @return number of moves in a tree
   */
  public int getDepth() {
    BoardHistoryNode node = this;
    int c = 0;
    while (node.next().isPresent()) {
      c++;
      node = node.next().get();
    }
    return c;
  }

  /**
   * Given some depth, returns the child at the given depth in the main trunk. If the main variation
   * is too short, silently stops early.
   *
   * @return the child at the given depth
   */
  public BoardHistoryNode childAtDepth(int depth) {
    BoardHistoryNode node = this;
    for (int i = 0; i < depth; i++) {
      Optional<BoardHistoryNode> next = node.next();
      if (next.isPresent()) {
        node = next.get();
      } else {
        break;
      }
    }
    return node;
  }

  /**
   * Check if there is a branch that is at least depth deep (at least depth moves)
   *
   * @return true if it is deep enough, false otherwise
   */
  public boolean hasDepth(int depth) {
    BoardHistoryNode node = this;
    int c = 0;
    while (node.next().isPresent()) {
      if (node.hasVariations()) {
        int variationDepth = depth - c - 1;
        return node.getVariations().stream().anyMatch(v -> v.hasDepth(variationDepth));
      } else {
        node = node.next().get();
        c++;
        if (c >= depth) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find top of variation (the first move that is on the main trunk)
   *
   * @return top of variation, if on main trunk, return start move
   */
  public BoardHistoryNode findTop() {
    BoardHistoryNode start = this;
    BoardHistoryNode top = start;
    while (start.previous().isPresent()) {
      BoardHistoryNode pre = start.previous().get();
      if (pre.next(true).isPresent() && pre.next(true).get() != start) {
        top = pre;
      }
      start = pre;
    }
    return top;
  }

  /**
   * Finds the first parent with variations.
   *
   * @return the first parent with variations, if any, Optional.empty otherwise
   */
  public Optional<BoardHistoryNode> firstParentWithVariations() {
    return this.findChildOfPreviousWithVariation().flatMap(v -> v.previous());
  }

  /**
   * Find first move with variations in tree above node.
   *
   * @return The child (in the current variation) of the first node with variations
   */
  public Optional<BoardHistoryNode> findChildOfPreviousWithVariation() {
    BoardHistoryNode node = this;
    while (node.previous().isPresent()) {
      BoardHistoryNode pre = node.previous().get();
      if (pre.hasVariations()) {
        return Optional.of(node);
      }
      node = pre;
    }
    return Optional.empty();
  }

  /**
   * Given a child node, find the index of that child node in its parent
   *
   * @return index of child node, -1 if child node not a child of parent
   */
  public int indexOfNode(BoardHistoryNode childNode) {
    if (!next(true).isPresent()) {
      return -1;
    }
    for (int i = 0; i < numberOfChildren(); i++) {
      if (getVariation(i).map(v -> v == childNode).orElse(false)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Given a child node, find the index of that child node in its parent
   *
   * @return index of child node, -1 if child node not a child of parent
   */
  public int findIndexOfNode(BoardHistoryNode childNode, boolean allSub) {
    if (!next().isPresent()) {
      return -1;
    }
    for (int i = 0; i < numberOfChildren(); i++) {
      Optional<BoardHistoryNode> node = getVariation(i);
      while (node.isPresent()) {
        if (node.map(n -> n == childNode).orElse(false)) {
          return i;
        }
        node = node.get().next();
      }
    }
    return -1;
  }

  /**
   * Given a child node, find the depth of that child node in its parent
   *
   * @return depth of child node, 0 if child node not a child of parent
   */
  public int depthOfNode(BoardHistoryNode childNode) {
    if (!next().isPresent()) {
      return 0;
    }
    for (int i = 0; i < numberOfChildren(); i++) {
      Optional<BoardHistoryNode> node = getVariation(i);
      int move = 1;
      while (node.isPresent()) {
        if (node.map(n -> n == childNode).orElse(false)) {
          return move;
        }
        move++;
        node = node.get().next();
      }
    }
    return 0;
  }

  /**
   * The move number of that node in its branch
   *
   * @return move number of node, 0 if node not a child of branch
   */
  public int moveNumberInBranch() {
    Optional<BoardHistoryNode> top = firstParentWithVariations();
    return top.isPresent() ? top.get().moveNumberOfNode() + top.get().depthOfNode(this) : 0;
  }

  /**
   * The move number of that node
   *
   * @return move number of node
   */
  public int moveNumberOfNode() {
    return isMainTrunk() ? getData().moveNumber : moveNumberInBranch();
  }

  /**
   * Check if node is part of the main trunk (rightmost branch)
   *
   * @return true if node is part of main trunk, false otherwise
   */
  public boolean isMainTrunk() {
    BoardHistoryNode node = this;
    while (node.previous().isPresent()) {
      BoardHistoryNode pre = node.previous().get();
      if (pre.next().isPresent() && pre.next().get() != node) {
        return false;
      }
      node = pre;
    }
    return true;
  }

  /**
   * Finds the next node with the comment.
   *
   * @return the first next node with comment, if any, Optional.empty otherwise
   */
  public Optional<BoardHistoryNode> findNextNodeWithComment() {
    BoardHistoryNode node = this;
    while (node.next().isPresent()) {
      BoardHistoryNode next = node.next().get();
      if (!next.getData().comment.isEmpty()) {
        return Optional.ofNullable(next);
      }
      node = next;
    }
    return Optional.empty();
  }

  /**
   * Finds the previous node with the comment.
   *
   * @return the first previous node with comment, if any, Optional.empty otherwise
   */
  public Optional<BoardHistoryNode> findPreviousNodeWithComment() {
    BoardHistoryNode node = this;
    while (node.previous().isPresent()) {
      BoardHistoryNode previous = node.previous().get();
      if (!previous.getData().comment.isEmpty()) {
        return Optional.ofNullable(previous);
      }
      node = previous;
    }
    return Optional.empty();
  }

  /**
   * Go to the next node with the comment.
   *
   * @return the move count to the next node with comment, 0 otherwise
   */
  public int goToNextNodeWithComment() {
    BoardHistoryNode node = this;
    int moves = 0;
    while (node.next().isPresent()) {
      moves++;
      BoardHistoryNode next = node.next().get();
      if (!next.getData().comment.isEmpty()) {
        break;
      }
      node = next;
    }
    return moves;
  }

  /**
   * Go to the previous node with the comment.
   *
   * @return the move count to the previous node with comment, 0 otherwise
   */
  public int goToPreviousNodeWithComment() {
    BoardHistoryNode node = this;
    int moves = 0;
    while (node.previous().isPresent()) {
      moves++;
      BoardHistoryNode previous = node.previous().get();
      if (!previous.getData().comment.isEmpty()) {
        break;
      }
      node = previous;
    }
    return moves;
  }

  public boolean compare(BoardHistoryNode node) {
    BoardData sData = this.getData();
    BoardData dData = node.getData();

    boolean dMove =
        sData.lastMove.isPresent() && dData.lastMove.isPresent()
            || !sData.lastMove.isPresent() && !dData.lastMove.isPresent();
    if (dMove && sData.lastMove.isPresent()) {
      int[] sM = sData.lastMove.get();
      int[] dM = dData.lastMove.get();
      dMove =
          (sM != null
              && sM.length == 2
              && dM != null
              && dM.length == 2
              && sM[0] == dM[0]
              && sM[1] == dM[1]);
    }
    return dMove
        && sData.comment != null
        && sData.comment.equals(dData.comment)
        && this.numberOfChildren() == node.numberOfChildren();
  }

  public void sync(BoardHistoryNode node) {
    if (node == null) return;

    BoardHistoryNode cur = this;
    // Compare
    while (node != null) {
      if (!cur.compare(node)) {
        BoardData sData = cur.getData();
        sData.sync(node.getData());
        if (node.numberOfChildren() > 0) {
          for (int i = 0; i < node.numberOfChildren(); i++) {
            if (node.getVariation(i).isPresent()) {
              if (cur.variations.size() <= i) {
                cur.addOrGoto(node.getVariation(i).get().getData().clone(), (i > 0));
              }
              if (i > 0) {
                cur.variations.get(i).sync(node.getVariation(i).get());
              }
            }
          }
        }
      }
      cur = cur.next(true).map(n -> n).orElse(null);
      node = node.next(true).map(n -> n).orElse(null);
    }
  }

  public Optional<BoardHistoryNode> now() {
    return Optional.of(this);
  }
}
