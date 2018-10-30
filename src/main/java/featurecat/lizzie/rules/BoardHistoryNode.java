package featurecat.lizzie.rules;

import java.util.ArrayList;
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

  /**
   * If we already have a next node with the same BoardData, move to it, otherwise add it and move
   * to it.
   *
   * @param data the node following this one
   * @param newBranch add a new branch
   * @return the node that was just set
   */
  public BoardHistoryNode addOrGoto(BoardData data, boolean newBranch) {
    // If you play a hand and immediately return it, it is most likely that you have made a mistake.
    // Ask whether to delete the previous node.
    //        if (!variations.isEmpty() && !variations.get(0).data.zobrist.equals(data.zobrist)) {
    //            // You may just mark this hand, so it's not necessarily wrong. Answer when the
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
          return variations.get(i);
        }
      }
    }
    BoardHistoryNode node = new BoardHistoryNode(data);
    // Add node
    variations.add(node);
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
    return variations.isEmpty() ? Optional.empty() : Optional.of(variations.get(0));
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

  public Optional<BoardHistoryNode> getVariation(int idx) {
    if (variations.size() <= idx) {
      return Optional.empty();
    } else {
      return Optional.of(variations.get(idx));
    }
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
        return;
      }
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
      if (pre.next().isPresent() && pre.next().get() != start) {
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
   * Given a child node, find the index of that child node in it's parent
   *
   * @return index of child node, -1 if child node not a child of parent
   */
  public int findIndexOfNode(BoardHistoryNode childNode) {
    if (!next().isPresent()) {
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
}
