package featurecat.lizzie.rules;

import java.util.ArrayList;
import java.util.List;

/** Node structure for the board history / sgf tree */
public class BoardHistoryNode {
  private BoardHistoryNode previous;
  private ArrayList<BoardHistoryNode> nexts;

  private BoardData data;

  // Save the children for restore to branch
  private int fromBackChildren;

  /** Initializes a new list node */
  public BoardHistoryNode(BoardData data) {
    previous = null;
    nexts = new ArrayList<BoardHistoryNode>();
    this.data = data;
  }

  /** Remove all subsequent nodes. */
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
   * If we already have a next node with the same BoardData, move to it, otherwise add it and move
   * to it.
   *
   * @param data the node following this one
   * @return the node that was just set
   */
  public BoardHistoryNode addOrGoto(BoardData data) {
    // If you play a hand and immediately return it, it is most likely that you have made a mistake.
    // Ask whether to delete the previous node.
    //        if (!nexts.isEmpty() && !nexts.get(0).data.zobrist.equals(data.zobrist)) {
    //            // You may just mark this hand, so it's not necessarily wrong. Answer when the
    // first query is wrong or it will not ask whether the move is wrong.
    //            if (!nexts.get(0).data.verify) {
    //                int ret = JOptionPane.showConfirmDialog(null, "Do you want undo?", "Undo",
    // JOptionPane.OK_CANCEL_OPTION);
    //                if (ret == JOptionPane.OK_OPTION) {
    //                    nexts.remove(0);
    //                } else {
    //                    nexts.get(0).data.verify = true;
    //                }
    //            }
    //        }
    for (int i = 0; i < nexts.size(); i++) {
      if (nexts.get(i).data.zobrist.equals(data.zobrist)) {
        //                if (i != 0) {
        //                    // Swap selected next to foremost
        //                    BoardHistoryNode currentNext = nexts.get(i);
        //                    nexts.set(i, nexts.get(0));
        //                    nexts.set(0, currentNext);
        //                }
        return nexts.get(i);
      }
    }
    BoardHistoryNode node = new BoardHistoryNode(data);
    // Add node
    nexts.add(node);
    node.previous = this;

    return node;
  }

  /** @return data stored on this node */
  public BoardData getData() {
    return data;
  }

  /** @return nexts for display */
  public List<BoardHistoryNode> getNexts() {
    return nexts;
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

  public BoardHistoryNode topOfBranch() {
    BoardHistoryNode top = this;
    while (top.previous != null && top.previous.nexts.size() == 1) {
      top = top.previous;
    }
    return top;
  }

  public int numberOfChildren() {
    if (nexts == null) {
      return 0;
    } else {
      return nexts.size();
    }
  }

  public boolean isFirstChild() {
    return (previous != null) && previous.next() == this;
  }

  public BoardHistoryNode getVariation(int idx) {
    if (nexts.size() <= idx) {
      return null;
    } else {
      return nexts.get(idx);
    }
  }

  public void moveUp() {
    if (previous != null) {
      previous.moveChildUp(this);
    }
  }

  public void moveDown() {
    if (previous != null) {
      previous.moveChildDown(this);
    }
  }

  public void moveChildUp(BoardHistoryNode child) {
    for (int i = 1; i < nexts.size(); i++) {
      if (nexts.get(i).data.zobrist.equals(child.data.zobrist)) {
        BoardHistoryNode tmp = nexts.get(i - 1);
        nexts.set(i - 1, child);
        nexts.set(i, tmp);
        return;
      }
    }
  }

  public void moveChildDown(BoardHistoryNode child) {
    for (int i = 0; i < nexts.size() - 1; i++) {
      if (nexts.get(i).data.zobrist.equals(child.data.zobrist)) {
        BoardHistoryNode tmp = nexts.get(i + 1);
        nexts.set(i + 1, child);
        nexts.set(i, tmp);
        return;
      }
    }
  }

  public void deleteChild(int idx) {
    if (idx < numberOfChildren()) {
      nexts.remove(idx);
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
}
