package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.analysis.LeelazListener;
import featurecat.lizzie.analysis.MoveData;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import javax.swing.*;
import org.json.JSONException;

public class Board implements LeelazListener {
  public static final int BOARD_SIZE =
      Lizzie.config.config.getJSONObject("ui").optInt("board-size", 19);
  private static final String alphabet = "ABCDEFGHJKLMNOPQRST";

  private BoardHistoryList history;
  private Stone[] capturedStones;

  private boolean scoreMode;

  private boolean analysisMode = false;
  private int playoutsAnalysis = 100;

  // Save the node for restore move when in the branch
  private BoardHistoryNode saveNode = null;

  public Board() {
    initialize();
  }

  /** Initialize the board completely */
  private void initialize() {
    Stone[] stones = new Stone[BOARD_SIZE * BOARD_SIZE];
    for (int i = 0; i < stones.length; i++) {
      stones[i] = Stone.EMPTY;
    }

    capturedStones = null;
    scoreMode = false;

    int[] boardArray = new int[BOARD_SIZE * BOARD_SIZE];
    BoardData boardData =
        new BoardData(stones, null, Stone.EMPTY, true, new Zobrist(), 0, boardArray, 0, 0, 50, 0);
    history = new BoardHistoryList(boardData);
  }

  /**
   * Calculates the array index of a stone stored at (x, y)
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the array index
   */
  public static int getIndex(int x, int y) {
    return x * Board.BOARD_SIZE + y;
  }

  /**
   * Converts a named coordinate eg C16, T5, K10, etc to an x and y coordinate
   *
   * @param namedCoordinate a capitalized version of the named coordinate. Must be a valid 19x19 Go
   *     coordinate, without I
   * @return an array containing x, followed by y
   */
  public static int[] convertNameToCoordinates(String namedCoordinate) {
    namedCoordinate = namedCoordinate.trim();
    if (namedCoordinate.equalsIgnoreCase("pass")) {
      return null;
    }
    // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
    int x = alphabet.indexOf(namedCoordinate.charAt(0));
    int y = BOARD_SIZE - Integer.parseInt(namedCoordinate.substring(1));
    return new int[] {x, y};
  }

  /**
   * Converts a x and y coordinate to a named coordinate eg C16, T5, K10, etc
   *
   * @param x x coordinate -- must be valid
   * @param y y coordinate -- must be valid
   * @return a string representing the coordinate
   */
  public static String convertCoordinatesToName(int x, int y) {
    // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
    return alphabet.charAt(x) + "" + (BOARD_SIZE - y);
  }

  /**
   * Checks if a coordinate is valid
   *
   * @param x x coordinate
   * @param y y coordinate
   * @return whether or not this coordinate is part of the board
   */
  public static boolean isValid(int x, int y) {
    return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
  }

  /**
   * The comment. Thread safe
   *
   * @param comment the comment of stone
   */
  public void comment(String comment) {
    synchronized (this) {
      if (history.getData() != null) {
        history.getData().comment = comment;
      }
    }
  }

  /**
   * The pass. Thread safe
   *
   * @param color the type of pass
   */
  public void pass(Stone color) {
    synchronized (this) {

      // check to see if this move is being replayed in history
      BoardData next = history.getNext();
      if (next != null && next.lastMove == null) {
        // this is the next move in history. Just increment history so that we don't erase the
        // redo's
        history.next();
        Lizzie.leelaz.playMove(color, "pass");
        if (Lizzie.frame.isPlayingAgainstLeelaz)
          Lizzie.leelaz.genmove((history.isBlacksTurn() ? "B" : "W"));

        return;
      }

      Stone[] stones = history.getStones().clone();
      Zobrist zobrist = history.getZobrist();
      int moveNumber = history.getMoveNumber() + 1;
      int[] moveNumberList = history.getMoveNumberList().clone();

      // build the new game state
      BoardData newState =
          new BoardData(
              stones,
              null,
              color,
              color.equals(Stone.WHITE),
              zobrist,
              moveNumber,
              moveNumberList,
              history.getData().blackCaptures,
              history.getData().whiteCaptures,
              0,
              0);

      // update leelaz with pass
      Lizzie.leelaz.playMove(color, "pass");
      if (Lizzie.frame.isPlayingAgainstLeelaz)
        Lizzie.leelaz.genmove((history.isBlacksTurn() ? "W" : "B"));

      // update history with pass
      history.addOrGoto(newState);

      Lizzie.frame.repaint();
    }
  }

  /** overloaded method for pass(), chooses color in an alternating pattern */
  public void pass() {
    pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
  }

  /**
   * Places a stone onto the board representation. Thread safe
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param color the type of stone to place
   */
  public void place(int x, int y, Stone color) {
    synchronized (this) {
      if (scoreMode) {
        // Mark clicked stone as dead
        Stone[] stones = history.getStones();
        toggleLiveStatus(capturedStones, x, y);
        return;
      }

      if (!isValid(x, y) || history.getStones()[getIndex(x, y)] != Stone.EMPTY) return;

      // Update winrate
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();

      if (stats.maxWinrate >= 0 && stats.totalPlayouts > history.getData().playouts) {
        history.getData().winrate = stats.maxWinrate;
        history.getData().playouts = stats.totalPlayouts;
      }
      double nextWinrate = -100;
      if (history.getData().winrate >= 0) nextWinrate = 100 - history.getData().winrate;

      // check to see if this coordinate is being replayed in history
      BoardData next = history.getNext();
      if (next != null && next.lastMove != null && next.lastMove[0] == x && next.lastMove[1] == y) {
        // this is the next coordinate in history. Just increment history so that we don't erase the
        // redo's
        history.next();
        // should be opposite from the bottom case
        if (Lizzie.frame.isPlayingAgainstLeelaz
            && Lizzie.frame.playerIsBlack != getData().blackToPlay) {
          Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
          Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "W" : "B"));
        } else if (!Lizzie.frame.isPlayingAgainstLeelaz) {
          Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
        }
        return;
      }

      // load a copy of the data at the current node of history
      Stone[] stones = history.getStones().clone();
      Zobrist zobrist = history.getZobrist();
      int[] lastMove = new int[] {x, y}; // keep track of the last played stone
      int moveNumber = history.getMoveNumber() + 1;
      int[] moveNumberList = history.getMoveNumberList().clone();

      moveNumberList[Board.getIndex(x, y)] = moveNumber;

      // set the stone at (x, y) to color
      stones[getIndex(x, y)] = color;
      zobrist.toggleStone(x, y, color);

      // remove enemy stones
      int capturedStones = 0;
      capturedStones += removeDeadChain(x + 1, y, color.opposite(), stones, zobrist);
      capturedStones += removeDeadChain(x, y + 1, color.opposite(), stones, zobrist);
      capturedStones += removeDeadChain(x - 1, y, color.opposite(), stones, zobrist);
      capturedStones += removeDeadChain(x, y - 1, color.opposite(), stones, zobrist);

      // check to see if the player made a suicidal coordinate
      int isSuicidal = removeDeadChain(x, y, color, stones, zobrist);

      for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
        if (stones[i].equals(Stone.EMPTY)) {
          moveNumberList[i] = 0;
        }
      }

      int bc = history.getData().blackCaptures;
      int wc = history.getData().whiteCaptures;
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

      // don't make this coordinate if it is suicidal or violates superko
      if (isSuicidal > 0 || history.violatesSuperko(newState)) return;

      // update leelaz with board position
      if (Lizzie.frame.isPlayingAgainstLeelaz
          && Lizzie.frame.playerIsBlack == getData().blackToPlay) {
        Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
        Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "W" : "B"));
      } else if (!Lizzie.frame.isPlayingAgainstLeelaz) {
        Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
      }

      // update history with this coordinate
      history.addOrGoto(newState);

      Lizzie.frame.repaint();
    }
  }

  /**
   * overloaded method for place(), chooses color in an alternating pattern
   *
   * @param x x coordinate
   * @param y y coordinate
   */
  public void place(int x, int y) {
    place(x, y, history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
  }

  /**
   * overloaded method for place. To be used by the LeelaZ engine. Color is then assumed to be
   * alternating
   *
   * @param namedCoordinate the coordinate to place a stone,
   */
  public void place(String namedCoordinate) {
    if (namedCoordinate.contains("pass")) {
      pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
      return;
    } else if (namedCoordinate.contains("resign")) {
      pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
      return;
    }

    int[] coordinates = convertNameToCoordinates(namedCoordinate);

    place(coordinates[0], coordinates[1]);
  }

  /** for handicap */
  public void flatten() {
    Stone[] stones = history.getStones();
    boolean blackToPlay = history.isBlacksTurn();
    Zobrist zobrist = history.getZobrist().clone();
    BoardHistoryList oldHistory = history;
    history =
        new BoardHistoryList(
            new BoardData(
                stones,
                null,
                Stone.EMPTY,
                blackToPlay,
                zobrist,
                0,
                new int[BOARD_SIZE * BOARD_SIZE],
                0,
                0,
                0.0,
                0));
    history.setGameInfo(oldHistory.getGameInfo());
  }

  /**
   * Removes a chain if it has no liberties
   *
   * @param x x coordinate -- needn't be valid
   * @param y y coordinate -- needn't be valid
   * @param color the color of the chain to remove
   * @param stones the stones array to modify
   * @param zobrist the zobrist object to modify
   * @return number of removed stones
   */
  private int removeDeadChain(int x, int y, Stone color, Stone[] stones, Zobrist zobrist) {
    if (!isValid(x, y) || stones[getIndex(x, y)] != color) return 0;

    boolean hasLiberties = hasLibertiesHelper(x, y, color, stones);

    // either remove stones or reset what hasLibertiesHelper does to the board
    return cleanupHasLibertiesHelper(x, y, color.recursed(), stones, zobrist, !hasLiberties);
  }

  /**
   * Recursively determines if a chain has liberties. Alters the state of stones, so it must be
   * counteracted
   *
   * @param x x coordinate -- needn't be valid
   * @param y y coordinate -- needn't be valid
   * @param color the color of the chain to be investigated
   * @param stones the stones array to modify
   * @return whether or not this chain has liberties
   */
  private boolean hasLibertiesHelper(int x, int y, Stone color, Stone[] stones) {
    if (!isValid(x, y)) return false;

    if (stones[getIndex(x, y)] == Stone.EMPTY) return true; // a liberty was found
    else if (stones[getIndex(x, y)] != color)
      return false; // we are either neighboring an enemy stone, or one we've already recursed on

    // set this index to be the recursed color to keep track of where we've already searched
    stones[getIndex(x, y)] = color.recursed();

    // set removeDeadChain to true if any recursive calls return true. Recurse in all 4 directions
    boolean hasLiberties =
        hasLibertiesHelper(x + 1, y, color, stones)
            || hasLibertiesHelper(x, y + 1, color, stones)
            || hasLibertiesHelper(x - 1, y, color, stones)
            || hasLibertiesHelper(x, y - 1, color, stones);

    return hasLiberties;
  }

  /**
   * cleans up what hasLibertyHelper does to the board state
   *
   * @param x x coordinate -- needn't be valid
   * @param y y coordinate -- needn't be valid
   * @param color color to clean up. Must be a recursed stone type
   * @param stones the stones array to modify
   * @param zobrist the zobrist object to modify
   * @param removeStones if true, we will remove all these stones. otherwise, we will set them to
   *     their unrecursed version
   * @return number of removed stones
   */
  private int cleanupHasLibertiesHelper(
      int x, int y, Stone color, Stone[] stones, Zobrist zobrist, boolean removeStones) {
    int removed = 0;
    if (!isValid(x, y) || stones[getIndex(x, y)] != color) return 0;

    stones[getIndex(x, y)] = removeStones ? Stone.EMPTY : color.unrecursed();
    if (removeStones) {
      zobrist.toggleStone(x, y, color.unrecursed());
      removed = 1;
    }

    // use the flood fill algorithm to replace all adjacent recursed stones
    removed += cleanupHasLibertiesHelper(x + 1, y, color, stones, zobrist, removeStones);
    removed += cleanupHasLibertiesHelper(x, y + 1, color, stones, zobrist, removeStones);
    removed += cleanupHasLibertiesHelper(x - 1, y, color, stones, zobrist, removeStones);
    removed += cleanupHasLibertiesHelper(x, y - 1, color, stones, zobrist, removeStones);
    return removed;
  }

  /**
   * get current board state
   *
   * @return the stones array corresponding to the current board state
   */
  public Stone[] getStones() {
    return history.getStones();
  }

  /**
   * shows where to mark the last coordinate
   *
   * @return the last played stone
   */
  public int[] getLastMove() {
    return history.getLastMove();
  }

  /**
   * get the move played in this position
   *
   * @return the next move, if any
   */
  public int[] getNextMove() {
    return history.getNextMove();
  }

  /**
   * get current board move number
   *
   * @return the int array corresponding to the current board move number
   */
  public int[] getMoveNumberList() {
    return history.getMoveNumberList();
  }

  /** Goes to the next coordinate, thread safe */
  public boolean nextMove() {
    synchronized (this) {
      // Update win rate statistics
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
      if (stats.totalPlayouts >= history.getData().playouts) {
        history.getData().winrate = stats.maxWinrate;
        history.getData().playouts = stats.totalPlayouts;
      }
      if (history.next() != null) {
        // update leelaz board position, before updating to next node
        if (history.getData().lastMove == null) {
          Lizzie.leelaz.playMove(history.getLastMoveColor(), "pass");
        } else {
          Lizzie.leelaz.playMove(
              history.getLastMoveColor(),
              convertCoordinatesToName(history.getLastMove()[0], history.getLastMove()[1]));
        }
        Lizzie.frame.repaint();
        return true;
      }
      return false;
    }
  }

  /**
   * Goes to the next coordinate, thread safe
   *
   * @param fromBackChildren by back children branch
   * @return true when has next variation
   */
  public boolean nextMove(int fromBackChildren) {
    synchronized (this) {
      // Update win rate statistics
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
      if (stats.totalPlayouts >= history.getData().playouts) {
        history.getData().winrate = stats.maxWinrate;
        history.getData().playouts = stats.totalPlayouts;
      }
      return nextVariation(fromBackChildren);
    }
  }

  /** Save the move number for restore If in the branch, save the back routing from children */
  public void saveMoveNumber() {
    BoardHistoryNode curNode = history.getCurrentHistoryNode();
    int curMoveNum = curNode.getData().moveNumber;
    if (curMoveNum > 0) {
      if (!BoardHistoryList.isMainTrunk(curNode)) {
        // If in branch, save the back routing from children
        saveBackRouting(curNode);
      }
      goToMoveNumber(0);
    }
    saveNode = curNode;
  }

  /** Save the back routing from children */
  public void saveBackRouting(BoardHistoryNode node) {
    if (node != null && node.previous() != null) {
      node.previous().setFromBackChildren(node.previous().getNexts().indexOf(node));
      saveBackRouting(node.previous());
    }
  }

  /** Restore move number by saved node */
  public void restoreMoveNumber() {
    restoreMoveNumber(saveNode);
  }

  /** Restore move number by node */
  public void restoreMoveNumber(BoardHistoryNode node) {
    if (node == null) {
      return;
    }
    int moveNumber = node.getData().moveNumber;
    if (moveNumber > 0) {
      if (BoardHistoryList.isMainTrunk(node)) {
        goToMoveNumber(moveNumber);
      } else {
        // If in Branch, restore by the back routing
        goToMoveNumberByBackChildren(moveNumber);
      }
    }
  }

  /** Go to move number by back routing from children when in branch */
  public void goToMoveNumberByBackChildren(int moveNumber) {
    int delta = moveNumber - history.getMoveNumber();
    for (int i = 0; i < Math.abs(delta); i++) {
      BoardHistoryNode curNode = history.getCurrentHistoryNode();
      if (curNode.numberOfChildren() > 1 && delta > 0) {
        nextMove(curNode.getFromBackChildren());
      } else {
        if (!(delta > 0 ? nextMove() : previousMove())) {
          break;
        }
      }
    }
  }

  public boolean goToMoveNumber(int moveNumber) {
    return goToMoveNumberHelper(moveNumber, false);
  }

  public boolean goToMoveNumberWithinBranch(int moveNumber) {
    return goToMoveNumberHelper(moveNumber, true);
  }

  public boolean goToMoveNumberBeyondBranch(int moveNumber) {
    // Go to main trunk if current branch is shorter than moveNumber.
    if (moveNumber > history.currentBranchLength() && moveNumber <= history.mainTrunkLength()) {
      goToMoveNumber(0);
    }
    return goToMoveNumber(moveNumber);
  }

  public boolean goToMoveNumberHelper(int moveNumber, boolean withinBranch) {
    int delta = moveNumber - history.getMoveNumber();
    boolean moved = false;
    for (int i = 0; i < Math.abs(delta); i++) {
      if (withinBranch && delta < 0) {
        BoardHistoryNode curNode = history.getCurrentHistoryNode();
        if (!curNode.isFirstChild()) {
          break;
        }
      }
      if (!(delta > 0 ? nextMove() : previousMove())) {
        break;
      }
      moved = true;
    }
    return moved;
  }

  /** Goes to the next variation, thread safe */
  public boolean nextVariation(int idx) {
    synchronized (this) {
      // Don't update winrate here as this is usually called when jumping between variations
      if (history.nextVariation(idx) != null) {
        // update leelaz board position, before updating to next node
        if (history.getData().lastMove == null) {
          Lizzie.leelaz.playMove(history.getLastMoveColor(), "pass");
        } else {
          Lizzie.leelaz.playMove(
              history.getLastMoveColor(),
              convertCoordinatesToName(history.getLastMove()[0], history.getLastMove()[1]));
        }
        Lizzie.frame.repaint();
        return true;
      }
      return false;
    }
  }

  /*
   * Moves to next variation (variation to the right) if possible
   * To move to another variation, the variation must have a move with the same move number as the current move in it.
   * Note: Will only look within variations that start at the same move on the main trunk/branch, and if on trunk
   * only in the ones closest
   */
  public boolean nextBranch() {
    synchronized (this) {
      BoardHistoryNode curNode = history.getCurrentHistoryNode();
      int curMoveNum = curNode.getData().moveNumber;
      // First check if there is a branch to move to, if not, stay in same place
      // Requirement: variaton need to have a move number same as current
      if (BoardHistoryList.isMainTrunk(curNode)) {
        // Check if there is a variation tree to the right that is deep enough
        BoardHistoryNode startVarNode = BoardHistoryList.findChildOfPreviousWithVariation(curNode);
        if (startVarNode == null) return false;
        startVarNode = startVarNode.previous();
        boolean isDeepEnough = false;
        for (int i = 1; i < startVarNode.numberOfChildren(); i++) {
          if (BoardHistoryList.hasDepth(
              startVarNode.getVariation(i), curMoveNum - startVarNode.getData().moveNumber - 1)) {
            isDeepEnough = true;
            break;
          }
        }
        if (!isDeepEnough) return false;
      } else {
        // We are in a variation, is there some variation to the right?
        BoardHistoryNode tmpNode = curNode;
        while (tmpNode != null) {
          // Try to move to the right
          BoardHistoryNode prevBranch = BoardHistoryList.findChildOfPreviousWithVariation(tmpNode);
          int idx = BoardHistoryList.findIndexOfNode(prevBranch.previous(), prevBranch);
          // Check if there are branches to the right, that are deep enough
          boolean isDeepEnough = false;
          for (int i = idx + 1; i < prevBranch.previous().numberOfChildren(); i++) {
            if (BoardHistoryList.hasDepth(
                prevBranch.previous().getVariation(i),
                curMoveNum - prevBranch.previous().getData().moveNumber - 1)) {
              isDeepEnough = true;
              break;
            }
          }
          if (isDeepEnough) break;
          // Did not find a deep enough branch, move up unless we reached main trunk
          if (BoardHistoryList.isMainTrunk(prevBranch.previous())) {
            // No right hand side branch to move too
            return false;
          }
          tmpNode = prevBranch.previous();
        }
      }

      // At this point, we know there is somewhere to move to... Move there, one step at the time
      // (because of Leelaz)
      // Start moving up the tree until we reach a moves with variations that are deep enough
      BoardHistoryNode prevNode;
      int startIdx = 0;
      while (curNode.previous() != null) {
        prevNode = curNode;
        previousMove();
        curNode = history.getCurrentHistoryNode();
        startIdx = BoardHistoryList.findIndexOfNode(curNode, prevNode) + 1;
        if (curNode.numberOfChildren() > 1 && startIdx <= curNode.numberOfChildren()) {
          // Find the variation that is deep enough
          boolean isDeepEnough = false;
          for (int i = startIdx; i < curNode.numberOfChildren(); i++) {
            if (BoardHistoryList.hasDepth(
                curNode.getVariation(i), curMoveNum - curNode.getData().moveNumber - 1)) {
              isDeepEnough = true;
              break;
            }
          }
          if (isDeepEnough) break;
        }
      }
      // Now move forward in new branch
      while (curNode.getData().moveNumber < curMoveNum) {
        if (curNode.numberOfChildren() == 1) {
          // One-way street, just move to next
          if (!nextVariation(0)) {
            // Not supposed to happen, fail-safe
            break;
          }
        } else {
          // Has several variations, need to find the closest one that is deep enough
          for (int i = startIdx; i < curNode.numberOfChildren(); i++) {
            if (BoardHistoryList.hasDepth(
                curNode.getVariation(i), curMoveNum - curNode.getData().moveNumber - 1)) {
              nextVariation(i);
              break;
            }
          }
        }
        startIdx = 0;
        curNode = history.getCurrentHistoryNode();
      }
      return true;
    }
  }

  /*
   * Moves to previous variation (variation to the left) if possible, or back to main trunk
   * To move to another variation, the variation must have the same number of moves in it.
   * If no variation with sufficient moves exist, move back to main trunk.
   * Note: Will always move back to main trunk, even if variation has more moves than main trunk (if that
   * is the case it will move to the last move in the trunk)
   */
  public boolean previousBranch() {
    synchronized (this) {
      BoardHistoryNode curNode = history.getCurrentHistoryNode();
      BoardHistoryNode prevNode;
      int curMoveNum = curNode.getData().moveNumber;

      if (BoardHistoryList.isMainTrunk(curNode)) {
        // Not possible to move further to the left, so just return
        return false;
      }
      // We already know we can move back (back to main trunk if necessary), so just start moving
      // Move backwards first
      int depth = 0;
      int startIdx = 0;
      boolean foundBranch = false;
      while (!BoardHistoryList.isMainTrunk(curNode)) {
        prevNode = curNode;
        // Move back
        previousMove();
        curNode = history.getCurrentHistoryNode();
        depth++;
        startIdx = BoardHistoryList.findIndexOfNode(curNode, prevNode);
        // If current move has children, check if any of those are deep enough (starting at the one
        // closest)
        if (curNode.numberOfChildren() > 1 && startIdx != 0) {
          foundBranch = false;
          for (int i = startIdx - 1; i >= 0; i--) {
            if (BoardHistoryList.hasDepth(curNode.getVariation(i), depth - 1)) {
              foundBranch = true;
              startIdx = i;
              break;
            }
          }
          if (foundBranch) break; // Found a variation (or main trunk) and it is deep enough
        }
      }

      if (!foundBranch) {
        // Back at main trunk, and it is not long enough, move forward until we reach the end..
        while (nextVariation(0)) {}

        ;
        return true;
      }

      // At this point, we are either back at the main trunk, or on top of variation we know is long
      // enough
      // Move forward
      while (curNode.getData().moveNumber < curMoveNum && curNode.next() != null) {
        if (curNode.numberOfChildren() == 1) {
          // One-way street, just move to next
          if (!nextVariation(0)) {
            // Not supposed to happen...
            break;
          }
        } else {
          foundBranch = false;
          // Several variations to choose between, make sure we select the one closest that is deep
          // enough (if any)
          for (int i = startIdx; i >= 0; i--) {
            if (BoardHistoryList.hasDepth(
                curNode.getVariation(i), curMoveNum - curNode.getData().moveNumber - 1)) {
              nextVariation(i);
              foundBranch = true;
              break;
            }
          }
          if (!foundBranch) {
            // Not supposed to happen, fail-safe
            nextVariation(0);
          }
        }
        // We have now moved one step further down
        curNode = history.getCurrentHistoryNode();
        startIdx = curNode.numberOfChildren() - 1;
      }
      return true;
    }
  }

  public void moveBranchUp() {
    synchronized (this) {
      history.getCurrentHistoryNode().topOfBranch().moveUp();
    }
  }

  public void moveBranchDown() {
    synchronized (this) {
      history.getCurrentHistoryNode().topOfBranch().moveDown();
    }
  }

  public void deleteMove() {
    synchronized (this) {
      BoardHistoryNode curNode = history.getCurrentHistoryNode();
      if (curNode.next() != null) {
        // Will delete more than one move, ask for confirmation
        int ret =
            JOptionPane.showConfirmDialog(
                null,
                "This will delete all moves and branches after this move",
                "Delete",
                JOptionPane.OK_CANCEL_OPTION);
        if (ret != JOptionPane.OK_OPTION) {
          return;
        }
      }
      // Clear the board if we're at the top
      if (curNode.previous() == null) {
        clear();
        return;
      }
      previousMove();
      int idx = BoardHistoryList.findIndexOfNode(curNode.previous(), curNode);
      curNode.previous().deleteChild(idx);
    }
  }

  public void deleteBranch() {
    int originalMoveNumber = history.getMoveNumber();
    undoToChildOfPreviousWithVariation();
    int moveNumberBeforeOperation = history.getMoveNumber();
    deleteMove();
    boolean canceled = (history.getMoveNumber() == moveNumberBeforeOperation);
    if (canceled) {
      goToMoveNumber(originalMoveNumber);
    }
  }

  public BoardData getData() {
    return history.getData();
  }

  public BoardHistoryList getHistory() {
    return history;
  }

  /** Clears all history and starts over from empty board. */
  public void clear() {
    Lizzie.leelaz.sendCommand("clear_board");
    Lizzie.frame.resetTitle();
    initialize();
  }

  /** Goes to the previous coordinate, thread safe */
  public boolean previousMove() {
    synchronized (this) {
      if (inScoreMode()) setScoreMode(false);
      // Update win rate statistics
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();

      if (stats.totalPlayouts >= history.getData().playouts) {
        history.getData().winrate = stats.maxWinrate;
        history.getData().playouts = stats.totalPlayouts;
      }
      if (history.previous() != null) {
        Lizzie.leelaz.undo();
        Lizzie.frame.repaint();
        return true;
      }
      return false;
    }
  }

  public boolean undoToChildOfPreviousWithVariation() {
    BoardHistoryNode start = history.getCurrentHistoryNode();
    BoardHistoryNode goal = history.findChildOfPreviousWithVariation(start);
    if (start == goal) return false;
    while ((history.getCurrentHistoryNode() != goal) && previousMove()) ;
    return true;
  }

  public void setScoreMode(boolean on) {
    if (on) {
      // load a copy of the data at the current node of history
      capturedStones = history.getStones().clone();
    } else {
      capturedStones = null;
    }
    scoreMode = on;
  }

  /*
   * Starting at position stonex, stoney, remove all stones with same color within an area bordered by stones
   * of opposite color (AKA captured stones)
   */
  private void toggleLiveStatus(Stone[] stones, int stonex, int stoney) {
    Stone[] shdwstones = stones.clone();
    Stone toggle = stones[getIndex(stonex, stoney)];
    Stone toggleToo;
    switch (toggle) {
      case BLACK:
        toggleToo = Stone.BLACK_CAPTURED;
        break;
      case BLACK_CAPTURED:
        toggleToo = Stone.BLACK;
        break;
      case WHITE:
        toggleToo = Stone.WHITE_CAPTURED;
        break;
      case WHITE_CAPTURED:
        toggleToo = Stone.WHITE;
        break;
      default:
        return;
    }
    boolean lastup, lastdown;
    // This is using a flood fill algorithm that uses a Q instead of being recursive
    Queue<int[]> visitQ = new ArrayDeque<>();
    visitQ.add(new int[] {stonex, stoney});
    while (!visitQ.isEmpty()) {
      int[] curpos = visitQ.remove();
      int x = curpos[0];
      int y = curpos[1];

      // Move all the way left
      while (x > 0
          && (stones[getIndex(x - 1, y)] == Stone.EMPTY || stones[getIndex(x - 1, y)] == toggle)) {
        x--;
      }

      lastup = lastdown = false;
      // Find all stones within empty area line by line (new lines added to Q)
      while (x < BOARD_SIZE) {
        if (shdwstones[getIndex(x, y)] == Stone.EMPTY) {
          shdwstones[getIndex(x, y)] = Stone.DAME; // Too mark that it has been visited
        } else if (stones[getIndex(x, y)] == toggle) {
          stones[getIndex(x, y)] = toggleToo;
        } else {
          break;
        }
        // Check above
        if (y - 1 >= 0
            && (shdwstones[getIndex(x, y - 1)] == Stone.EMPTY
                || stones[getIndex(x, y - 1)] == toggle)) {
          if (!lastup) visitQ.add(new int[] {x, y - 1});
          lastup = true;
        } else {
          lastup = false;
        }
        // Check below
        if (y + 1 < BOARD_SIZE
            && (shdwstones[getIndex(x, y + 1)] == Stone.EMPTY
                || stones[getIndex(x, y + 1)] == toggle)) {
          if (!lastdown) visitQ.add(new int[] {x, y + 1});
          lastdown = true;
        } else {
          lastdown = false;
        }
        x++;
      }
    }
    Lizzie.frame.repaint();
  }

  /*
   * Check if a point on the board is empty or contains a captured stone
   */
  private boolean emptyOrCaptured(Stone[] stones, int x, int y) {
    int curidx = getIndex(x, y);
    if (stones[curidx] == Stone.EMPTY
        || stones[curidx] == Stone.BLACK_CAPTURED
        || stones[curidx] == Stone.WHITE_CAPTURED) return true;
    return false;
  }

  /*
   * Starting from startx, starty, mark all empty points within area as either white, black or dame.
   * If two stones of opposite color (neither marked as captured) is encountered, the area is dame.
   *
   * @return A stone with color white, black or dame
   */
  private Stone markEmptyArea(Stone[] stones, int startx, int starty) {
    Stone[] shdwstones = stones.clone();
    Stone found =
        Stone.EMPTY; // Found will either be black or white, or dame if both are found in area
    boolean lastup, lastdown;
    Queue<int[]> visitQ = new ArrayDeque<>();
    visitQ.add(new int[] {startx, starty});
    Deque<Integer> allPoints = new ArrayDeque<>();
    // Check one line at the time, new lines added to visitQ
    while (!visitQ.isEmpty()) {
      int[] curpos = visitQ.remove();
      int x = curpos[0];
      int y = curpos[1];
      if (!emptyOrCaptured(shdwstones, x, y)) {
        continue;
      }
      // Move all the way left
      while (x > 0 && emptyOrCaptured(shdwstones, x - 1, y)) {
        x--;
      }
      // Are we on the border, or do we have a stone to the left?
      if (x > 0 && shdwstones[getIndex(x - 1, y)] != found) {
        if (found == Stone.EMPTY) found = shdwstones[getIndex(x - 1, y)];
        else found = Stone.DAME;
      }

      lastup = lastdown = false;
      while (x < BOARD_SIZE && emptyOrCaptured(shdwstones, x, y)) {
        // Check above
        if (y - 1 >= 0 && shdwstones[getIndex(x, y - 1)] != Stone.DAME) {
          if (emptyOrCaptured(shdwstones, x, y - 1)) {
            if (!lastup) visitQ.add(new int[] {x, y - 1});
            lastup = true;
          } else {
            lastup = false;
            if (found != shdwstones[getIndex(x, y - 1)]) {
              if (found == Stone.EMPTY) {
                found = shdwstones[getIndex(x, y - 1)];
              } else {
                found = Stone.DAME;
              }
            }
          }
        }
        // Check below
        if (y + 1 < BOARD_SIZE && shdwstones[getIndex(x, y + 1)] != Stone.DAME) {
          if (emptyOrCaptured(shdwstones, x, y + 1)) {
            if (!lastdown) {
              visitQ.add(new int[] {x, y + 1});
            }
            lastdown = true;
          } else {
            lastdown = false;
            if (found != shdwstones[getIndex(x, y + 1)]) {
              if (found == Stone.EMPTY) {
                found = shdwstones[getIndex(x, y + 1)];
              } else {
                found = Stone.DAME;
              }
            }
          }
        }
        // Add current stone to empty area and mark as visited
        if (shdwstones[getIndex(x, y)] == Stone.EMPTY) allPoints.add(getIndex(x, y));

        // Use dame stone to mark as visited
        shdwstones[getIndex(x, y)] = Stone.DAME;
        x++;
      }
      // At this point x is at the edge of the board or on a stone
      if (x < BOARD_SIZE && shdwstones[getIndex(x, y)] != found) {
        if (found == Stone.EMPTY) found = shdwstones[getIndex(x, y)];
        else found = Stone.DAME;
      }
    }
    // Finally mark all points as black or white captured if they were surronded by white or black
    if (found == Stone.WHITE) found = Stone.WHITE_POINT;
    else if (found == Stone.BLACK) found = Stone.BLACK_POINT;
    // else found == DAME and will be set as this.
    while (!allPoints.isEmpty()) {
      int idx = allPoints.remove();
      stones[idx] = found;
    }
    return found;
  }

  /*
   * Mark all empty points on board as black point, white point or dame
   */
  public Stone[] scoreStones() {

    Stone[] scoreStones = capturedStones.clone();

    for (int i = 0; i < BOARD_SIZE; i++) {
      for (int j = 0; j < BOARD_SIZE; j++) {
        if (scoreStones[getIndex(i, j)] == Stone.EMPTY) {
          markEmptyArea(scoreStones, i, j);
        }
      }
    }
    return scoreStones;
  }

  /*
   * Count score for whole board, including komi and captured stones
   */
  public double[] getScore(Stone[] scoreStones) {
    double score[] =
        new double[] {
          getData().blackCaptures, getData().whiteCaptures + getHistory().getGameInfo().getKomi()
        };
    for (int i = 0; i < BOARD_SIZE; i++) {
      for (int j = 0; j < BOARD_SIZE; j++) {
        switch (scoreStones[getIndex(i, j)]) {
          case BLACK_POINT:
            score[0]++;
            break;
          case BLACK_CAPTURED:
            score[1] += 2;
            break;

          case WHITE_POINT:
            score[1]++;
            break;
          case WHITE_CAPTURED:
            score[0] += 2;
            break;
        }
      }
    }
    return score;
  }

  public boolean inAnalysisMode() {
    return analysisMode;
  }

  public boolean inScoreMode() {
    return scoreMode;
  }

  public void toggleAnalysis() {
    if (analysisMode) {
      Lizzie.leelaz.removeListener(this);
      analysisMode = false;
    } else {
      if (getNextMove() == null) return;
      String answer =
          JOptionPane.showInputDialog(
              "# playouts for analysis (e.g. 100 (fast) or 50000 (slow)): ");
      if (answer == null) return;
      try {
        playoutsAnalysis = Integer.parseInt(answer);
      } catch (NumberFormatException err) {
        System.out.println("Not a valid number");
        return;
      }
      Lizzie.leelaz.addListener(this);
      analysisMode = true;
      if (!Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
    }
  }

  public void bestMoveNotification(List<MoveData> bestMoves) {
    if (analysisMode) {
      boolean isSuccessivePass =
          (history.getPrevious() != null
              && history.getPrevious().lastMove == null
              && getLastMove() == null);
      // Note: We cannot replace this history.getNext() with getNextMove()
      // because the latter returns null if the next move is "pass".
      if (history.getNext() == null || isSuccessivePass) {
        // Reached the end...
        toggleAnalysis();
      } else if (bestMoves == null || bestMoves.size() == 0) {
        // If we get empty list, something strange happened, ignore notification
      } else {
        // sum the playouts to proceed like leelaz's --visits option.
        int sum = 0;
        for (MoveData move : bestMoves) {
          sum += move.playouts;
        }
        if (sum >= playoutsAnalysis) {
          nextMove();
        }
      }
    }
  }

  public void autosave() {
    if (autosaveToMemory()) {
      try {
        Lizzie.config.persist();
      } catch (IOException err) {
      }
    }
  }

  public boolean autosaveToMemory() {
    try {
      String sgf = SGFParser.saveToString();
      if (sgf.equals(Lizzie.config.persisted.getString("autosave"))) {
        return false;
      }
      Lizzie.config.persisted.put("autosave", sgf);
    } catch (Exception err) { // IOException or JSONException
      return false;
    }
    return true;
  }

  public void resumePreviousGame() {
    try {
      SGFParser.loadFromString(Lizzie.config.persisted.getString("autosave"));
      while (nextMove()) ;
    } catch (JSONException err) {
    }
  }
}
