package featurecat.lizzie.rules;

import static java.lang.Math.min;
import static java.util.Collections.singletonList;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.analysis.LeelazListener;
import featurecat.lizzie.analysis.MoveData;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import org.json.JSONException;

public class Board implements LeelazListener {
  public static int boardSize0 =
      (Lizzie.config != null
              && Lizzie.config.config != null
              && Lizzie.config.config.getJSONObject("ui") != null)
          ? Lizzie.config.config.getJSONObject("ui").optInt("board-size", 19)
          : 19;
  public static int boardWidth =
      (Lizzie.config != null
              && Lizzie.config.config != null
              && Lizzie.config.config.getJSONObject("ui") != null)
          ? Lizzie.config.config.getJSONObject("ui").optInt("board-width", boardSize0)
          : boardSize0;
  public static int boardHeight =
      (Lizzie.config != null
              && Lizzie.config.config != null
              && Lizzie.config.config.getJSONObject("ui") != null)
          ? Lizzie.config.config.getJSONObject("ui").optInt("board-height", boardSize0)
          : boardSize0;
  private static final String alphabet = "ABCDEFGHJKLMNOPQRSTUVWXYZ";

  private BoardHistoryList history;
  private Stone[] capturedStones;
  private boolean scoreMode;
  private boolean analysisMode;
  private int playoutsAnalysis;

  public String allowCoords = "";
  public String avoidCoords = "";
  public boolean isForcing = false;
  public boolean isAllowing = false;
  public boolean isAvoding = false;
  public boolean isKeepingAvoid = false;

  // Save the node for restore move when in the branch
  private Optional<BoardHistoryNode> saveNode;

  public Board() {
    initialize();
  }

  /** Initialize the board completely */
  private void initialize() {
    capturedStones = new Stone[] {};
    scoreMode = false;
    analysisMode = false;
    playoutsAnalysis = 100;
    saveNode = Optional.empty();
    Lizzie.frame.setForceRefresh(false);
    history = new BoardHistoryList(BoardData.empty(boardWidth, boardHeight));
  }

  /**
   * Calculates the array index of a stone stored at (x, y)
   *
   * @param x the x coordinate
   * @param y the y coordinate
   * @return the array index
   */
  public static int getIndex(int x, int y) {
    return x * Board.boardHeight + y;
  }

  public static int[] getCoord(int index) {
    int y = index % Board.boardHeight;
    int x = (index - y) / Board.boardHeight;
    return new int[] {x, y};
  }

  /**
   * Converts a named coordinate eg C16, T5, K10, etc to an x and y coordinate
   *
   * @param namedCoordinate a capitalized version of the named coordinate. Must be a valid 19x19 Go
   *     coordinate, without I
   * @return an optional array of coordinates, empty for pass and resign
   */
  public static Optional<int[]> asCoordinates(String namedCoordinate) {
    namedCoordinate = namedCoordinate.trim();
    if (namedCoordinate.equalsIgnoreCase("pass") || namedCoordinate.equalsIgnoreCase("resign")) {
      return Optional.empty();
    }
    // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
    String reg = "([A-HJ-Z]+)(\\d+)";
    Pattern p = Pattern.compile(reg);
    Matcher m = p.matcher(namedCoordinate);
    if (m.find() && m.groupCount() == 2) {
      int x = asDigit(m.group(1));
      int y = boardHeight - Integer.parseInt(m.group(2));
      return Optional.of(new int[] {x, y});
    } else {
      reg = "\\(([\\d]+),([\\d]+)\\)";
      p = Pattern.compile(reg);
      m = p.matcher(namedCoordinate);
      if (m.find() && m.groupCount() == 2) {
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2)); // boardHeight - Integer.parseInt(m.group(2)) - 1;
        return Optional.of(new int[] {x, y});
      } else {
        return Optional.empty();
      }
    }
  }

  public static int asDigit(String name) {
    // coordinates take the form C16 A19 Q5 K10 etc. I is not used.
    int base = alphabet.length();
    char names[] = name.toCharArray();
    int length = names.length;
    if (length > 0) {
      int x = 0;
      for (int i = length - 1; i >= 0; i--) {
        int index = alphabet.indexOf(names[i]);
        if (index == -1) {
          return index;
        }
        x += index * Math.pow(base, length - i - 1);
      }
      return x;
    } else {
      return -1;
    }
  }

  public static String asName(int c) {
    return asName(c, true);
  }

  public void clearAnalysis() {
    getData().tryToClearBestMoves();
  }

  public static String asName(int c, boolean isName) {
    if (boardWidth > 25 && isName) {
      return String.valueOf(c + 1);
    }
    StringBuilder name = new StringBuilder();
    int base = alphabet.length();
    int n = c;
    ArrayDeque<Integer> ad = new ArrayDeque<Integer>();
    if (n > 0) {
      while (n > 0) {
        ad.addFirst(n < 25 && c >= 25 ? n % base - 1 : n % base);
        n /= base;
      }
    } else {
      ad.addFirst(n);
    }
    ad.forEach(i -> name.append(alphabet.charAt(i)));
    return name.toString();
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
    if (boardWidth > 25 || boardHeight > 25) {
      return String.format("(%d,%d)", x, y); // boardHeight - y - 1);
    } else {
      return asName(x, false) + (boardHeight - y);
    }
  }

  /**
   * Checks if a coordinate is valid
   *
   * @param x x coordinate
   * @param y y coordinate
   * @return whether or not this coordinate is part of the board
   */
  public static boolean isValid(int x, int y) {
    return x >= 0 && x < boardWidth && y >= 0 && y < boardHeight;
  }

  public static boolean isValid(int[] c) {
    return c != null && c.length == 2 && isValid(c[0], c[1]);
  }

  /**
   * Open board again when the SZ property is setup by sgf
   *
   * @param size
   */
  public void reopen(int width, int height) {
    width = (width >= 2) ? width : 19;
    height = (height >= 2) ? height : 19;
    if (width != boardWidth || height != boardHeight) {
      boardWidth = width;
      boardHeight = height;
      Zobrist.init();
      clear();
      Lizzie.leelaz.boardSize(boardWidth, boardHeight);
      Lizzie.frame.setForceRefresh(true);
    }
  }

  /**
   * The comment. Thread safe
   *
   * @param comment the comment of stone
   */
  public void comment(String comment) {
    synchronized (this) {
      history.getData().comment = comment;
    }
  }

  /**
   * Update the move number. Thread safe
   *
   * @param moveNumber the move number of stone
   */
  public void moveNumber(int moveNumber) {
    synchronized (this) {
      BoardData data = history.getData();
      if (data.lastMove.isPresent()) {
        int[] moveNumberList = history.getMoveNumberList();
        moveNumberList[Board.getIndex(data.lastMove.get()[0], data.lastMove.get()[1])] = moveNumber;
        Optional<BoardHistoryNode> node = history.getCurrentHistoryNode().previous();
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

  public int moveNumberByCoord(int[] coord) {
    int moveNumber = 0;
    if (Lizzie.board.isValid(coord)) {
      int index = Lizzie.board.getIndex(coord[0], coord[1]);
      if (Lizzie.board.getHistory().getStones()[index] != Stone.EMPTY) {
        BoardHistoryNode cur = Lizzie.board.getHistory().getCurrentHistoryNode();
        moveNumber = cur.getData().moveNumberList[index];
        if (!cur.isMainTrunk()) {
          if (moveNumber > 0) {
            moveNumber = cur.getData().moveNumber - cur.getData().moveMNNumber + moveNumber;
          } else {
            BoardHistoryNode p = cur.firstParentWithVariations().orElse(cur);
            while (p != cur && moveNumber == 0) {
              moveNumber = p.getData().moveNumberList[index];
              if (moveNumber > 0) {
                BoardHistoryNode topOfTop = p.firstParentWithVariations().orElse(p);
                if (topOfTop != p) {
                  moveNumber = p.getData().moveNumber - p.getData().moveMNNumber + moveNumber;
                }
              } else {
                cur = p;
                p = cur.firstParentWithVariations().orElse(cur);
              }
            }
          }
        }
      }
    }
    return moveNumber;
  }

  /**
   * Add a stone to the board representation. Thread safe
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param color the type of stone to place
   */
  public void addStone(int x, int y, Stone color) {
    synchronized (this) {
      if (!isValid(x, y) || history.getStones()[getIndex(x, y)] != Stone.EMPTY) return;

      Stone[] stones = history.getData().stones;
      Zobrist zobrist = history.getData().zobrist;

      // set the stone at (x, y) to color
      stones[getIndex(x, y)] = color;
      zobrist.toggleStone(x, y, color);

      Lizzie.frame.refresh();
    }
  }

  /**
   * Remove a stone from the board representation. Thread safe
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param color the type of stone to place
   */
  public void removeStone(int x, int y, Stone color) {
    synchronized (this) {
      if (!isValid(x, y) || history.getStones()[getIndex(x, y)] == Stone.EMPTY) return;

      BoardData data = history.getData();
      Stone[] stones = data.stones;
      Zobrist zobrist = data.zobrist;

      // set the stone at (x, y) to empty
      Stone oriColor = stones[getIndex(x, y)];
      stones[getIndex(x, y)] = Stone.EMPTY;
      zobrist.toggleStone(x, y, oriColor);
      data.moveNumberList[Board.getIndex(x, y)] = 0;

      Lizzie.frame.refresh();
    }
  }

  /**
   * Add a key and value to node
   *
   * @param key
   * @param value
   */
  public void addNodeProperty(String key, String value) {
    synchronized (this) {
      history.getData().addProperty(key, value);
      if ("MN".equals(key)) {
        moveNumber(Integer.parseInt(value));
      }
    }
  }

  /**
   * Add a keys and values to node
   *
   * @param properties
   */
  public void addNodeProperties(Map<String, String> properties) {
    synchronized (this) {
      history.getData().addProperties(properties);
    }
  }

  public void setKomi(double komi) {
    getHistory().getGameInfo().setKomi(komi);
    Lizzie.leelaz.komi(komi);
  }

  /**
   * The pass. Thread safe
   *
   * @param color the type of pass
   */
  public void pass(Stone color) {
    pass(color, false, false, false);
  }

  public void pass(Stone color, boolean newBranch) {
    pass(color, newBranch, false, false);
  }

  public void pass(Stone color, boolean newBranch, boolean dummy) {
    pass(color, newBranch, dummy, false);
  }

  /**
   * The pass. Thread safe
   *
   * @param color the type of pass
   * @param newBranch add a new branch
   */
  public void pass(Stone color, boolean newBranch, boolean dummy, boolean changeMove) {
    synchronized (this) {

      // check to see if this move is being replayed in history
      if (history.getNext().map(n -> !n.lastMove.isPresent()).orElse(false) && !newBranch) {
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
      int[] moveNumberList =
          newBranch && history.getNext(true).isPresent()
              ? new int[Board.boardWidth * Board.boardHeight]
              : history.getMoveNumberList().clone();

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
              history.getData().blackCaptures,
              history.getData().whiteCaptures,
              0.0,
              0,
              0.0);
      newState.dummy = dummy;

      // update leelaz with pass
      if (!Lizzie.leelaz.isInputCommand) Lizzie.leelaz.playMove(color, "pass");
      if (Lizzie.frame.isPlayingAgainstLeelaz)
        Lizzie.leelaz.genmove((history.isBlacksTurn() ? "W" : "B"));

      // update history with pass
      history.addOrGoto(newState, newBranch, changeMove);

      Lizzie.frame.refresh();
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
    place(x, y, color, false);
  }

  public void place(int x, int y, Stone color, boolean newBranch) {
    place(x, y, color, newBranch, false);
  }

  /**
   * Places a stone onto the board representation. Thread safe
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param color the type of stone to place
   * @param newBranch add a new branch
   */
  public void place(int x, int y, Stone color, boolean newBranch, boolean changeMove) {
    Lizzie.frame.clearBeforeMove();
    synchronized (this) {
      if (scoreMode) {
        // Mark clicked stone as dead
        Stone[] stones = history.getStones();
        toggleLiveStatus(capturedStones, x, y);
        return;
      }

      if (!isValid(x, y) || (history.getStones()[getIndex(x, y)] != Stone.EMPTY && !newBranch))
        return;

      updateWinrate();
      double nextWinrate = -100;
      if (history.getData().winrate >= 0) nextWinrate = 100 - history.getData().winrate;
      double nextScoreMean = -history.getData().scoreMean;

      // check to see if this coordinate is being replayed in history
      Optional<int[]> nextLast = history.getNext().flatMap(n -> n.lastMove);
      if (nextLast.isPresent()
          && nextLast.get()[0] == x
          && nextLast.get()[1] == y
          && !newBranch
          && !changeMove) {
        // this is the next coordinate in history. Just increment history so that we don't erase the
        // redo's
        Lizzie.leelaz.beginModifyingBoard();
        history.next();
        // should be opposite from the bottom case
        if (Lizzie.frame.isPlayingAgainstLeelaz
            && Lizzie.frame.playerIsBlack != getData().blackToPlay) {
          Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
          Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "W" : "B"));
        } else if (!Lizzie.frame.isPlayingAgainstLeelaz) {
          Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
        }
        Lizzie.leelaz.endModifyingBoard();
        return;
      }

      // load a copy of the data at the current node of history
      Stone[] stones = history.getStones().clone();
      Zobrist zobrist = history.getZobrist();
      Optional<int[]> lastMove = Optional.of(new int[] {x, y});
      int moveNumber = history.getMoveNumber() + 1;
      int moveMNNumber =
          history.getMoveMNNumber() > -1 && !newBranch ? history.getMoveMNNumber() + 1 : -1;
      int[] moveNumberList =
          newBranch && history.getNext(true).isPresent()
              ? new int[Board.boardWidth * Board.boardHeight]
              : history.getMoveNumberList().clone();

      moveNumberList[Board.getIndex(x, y)] = moveMNNumber > -1 ? moveMNNumber : moveNumber;

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

      for (int i = 0; i < boardWidth * boardHeight; i++) {
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
              0,
              nextScoreMean);
      newState.moveMNNumber = moveMNNumber;
      newState.dummy = false;

      // don't make this coordinate if it is suicidal or violates superko
      if (isSuicidal > 0 || history.violatesKoRule(newState)) return;

      // update leelaz with board position
      Lizzie.leelaz.beginModifyingBoard();
      if (Lizzie.frame.isPlayingAgainstLeelaz
          && Lizzie.frame.playerIsBlack == getData().blackToPlay) {
        Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
        Lizzie.leelaz.genmove((Lizzie.board.getData().blackToPlay ? "W" : "B"));
      } else if (!Lizzie.frame.isPlayingAgainstLeelaz && !Lizzie.leelaz.isInputCommand) {
        Lizzie.leelaz.playMove(color, convertCoordinatesToName(x, y));
      }

      // update history with this coordinate
      history.addOrGoto(newState, newBranch, changeMove);
      Lizzie.leelaz.endModifyingBoard();

      Lizzie.frame.refresh();
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
    Optional<int[]> coords = asCoordinates(namedCoordinate);
    if (coords.isPresent()) {
      place(coords.get()[0], coords.get()[1]);
    } else {
      pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE);
    }
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
                Optional.empty(),
                Stone.EMPTY,
                blackToPlay,
                zobrist,
                0,
                new int[boardWidth * boardHeight],
                0,
                0,
                0.0,
                0,
                0.0));
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
  public static int removeDeadChain(int x, int y, Stone color, Stone[] stones, Zobrist zobrist) {
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
  private static boolean hasLibertiesHelper(int x, int y, Stone color, Stone[] stones) {
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
  private static int cleanupHasLibertiesHelper(
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
   * Get current board state
   *
   * @return the stones array corresponding to the current board state
   */
  public Stone[] getStones() {
    return history.getStones();
  }

  /**
   * Shows where to mark the last coordinate
   *
   * @return the last played stone, if any, Optional.empty otherwise
   */
  public Optional<int[]> getLastMove() {
    return history.getLastMove();
  }

  /**
   * Gets the move played in this position
   *
   * @return the next move, if any, Optional.empty otherwise
   */
  public Optional<int[]> getNextMove() {
    return history.getNextMove();
  }

  /**
   * Gets current board move number
   *
   * @return the int array corresponding to the current board move number
   */
  public int[] getMoveNumberList() {
    return history.getMoveNumberList();
  }

  /** Goes to the next coordinate, thread safe */
  public boolean nextMove() {
    Lizzie.frame.clearBeforeMove();
    synchronized (this) {
      updateWinrate();
      Lizzie.leelaz.beginModifyingBoard();
      if (history.next().isPresent()) {
        // update leelaz board position, before updating to next node
        Optional<int[]> lastMoveOpt = history.getData().lastMove;
        if (lastMoveOpt.isPresent()) {
          int[] lastMove = lastMoveOpt.get();
          String name = convertCoordinatesToName(lastMove[0], lastMove[1]);
          Lizzie.leelaz.playMove(history.getLastMoveColor(), name);
        } else {
          Lizzie.leelaz.playMove(history.getLastMoveColor(), "pass");
        }
        Lizzie.frame.refresh();
        Lizzie.leelaz.endModifyingBoard();
        return true;
      }
      Lizzie.leelaz.endModifyingBoard();
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
      updateWinrate();
      return nextVariation(fromBackChildren);
    }
  }

  /** Save the move number for restore If in the branch, save the back routing from children */
  public void saveMoveNumber() {
    BoardHistoryNode currentNode = history.getCurrentHistoryNode();
    int curMoveNum = currentNode.getData().moveNumber;
    if (curMoveNum > 0) {
      if (!currentNode.isMainTrunk()) {
        // If in branch, save the back routing from children
        saveBackRouting(currentNode);
      }
      goToMoveNumber(0);
    }
    saveNode = Optional.of(currentNode);
  }

  /** Save the back routing from children */
  public void saveBackRouting(BoardHistoryNode node) {
    Optional<BoardHistoryNode> prev = node.previous();
    prev.ifPresent(
        n -> {
          n.setFromBackChildren(n.getVariations().indexOf(node));
          saveBackRouting(n);
        });
  }

  /** Restore move number by saved node */
  public void restoreMoveNumber() {
    saveNode.ifPresent(n -> restoreMoveNumber(n));
  }

  /** Restore move number by node */
  public void restoreMoveNumber(BoardHistoryNode node) {
    Stone[] stones = history.getStones();
    for (int i = 0; i < stones.length; i++) {
      Stone stone = stones[i];
      if (stone.isBlack() || stone.isWhite()) {
        int y = i % Board.boardWidth;
        int x = (i - y) / Board.boardHeight;
        Lizzie.leelaz.playMove(stone, convertCoordinatesToName(x, y));
      }
    }
    int moveNumber = node.getData().moveNumber;
    if (moveNumber > 0) {
      if (node.isMainTrunk()) {
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
      BoardHistoryNode currentNode = history.getCurrentHistoryNode();
      if (currentNode.hasVariations() && delta > 0) {
        nextMove(currentNode.getFromBackChildren());
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
        BoardHistoryNode currentNode = history.getCurrentHistoryNode();
        if (!currentNode.isFirstChild()) {
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
      if (history.nextVariation(idx).isPresent()) {
        // Update leelaz board position, before updating to next node
        Optional<int[]> lastMoveOpt = history.getData().lastMove;
        if (lastMoveOpt.isPresent()) {
          int[] lastMove = lastMoveOpt.get();
          String name = convertCoordinatesToName(lastMove[0], lastMove[1]);
          Lizzie.leelaz.playMove(history.getLastMoveColor(), name);
        } else {
          Lizzie.leelaz.playMove(history.getLastMoveColor(), "pass");
        }
        Lizzie.frame.refresh();
        return true;
      }
      return false;
    }
  }

  /**
   * Returns all the nodes at the given depth in the history tree, always including a node from the
   * main variation (possibly less deep that the given depth).
   *
   * @return the list of candidate nodes
   */
  private List<BoardHistoryNode> branchCandidates(BoardHistoryNode node) {
    int targetDepth = node.getData().moveNumber;
    Stream<BoardHistoryNode> nodes = singletonList(history.root()).stream();
    for (int i = 0; i < targetDepth; i++) {
      nodes = nodes.flatMap(n -> n.getVariations().stream());
    }
    LinkedList<BoardHistoryNode> result = nodes.collect(Collectors.toCollection(LinkedList::new));

    if (result.isEmpty() || !result.get(0).isMainTrunk()) {
      BoardHistoryNode endOfMainTrunk = history.root();
      while (endOfMainTrunk.next().isPresent()) {
        endOfMainTrunk = endOfMainTrunk.next().get();
      }
      result.addFirst(endOfMainTrunk);
      return result;
    } else {
      return result;
    }
  }

  /**
   * Moves to next variation (variation to the right) if possible. The variation must have a move
   * with the same move number as the current move in it.
   *
   * @return true if there exist a target variation
   */
  public boolean nextBranch() {
    synchronized (this) {
      BoardHistoryNode currentNode = history.getCurrentHistoryNode();
      Optional<BoardHistoryNode> targetNode = Optional.empty();
      boolean foundIt = false;
      for (BoardHistoryNode candidate : branchCandidates(currentNode)) {
        if (foundIt) {
          targetNode = Optional.of(candidate);
          break;
        } else if (candidate == currentNode) {
          foundIt = true;
        }
      }
      if (targetNode.isPresent()) {
        moveToAnyPosition(targetNode.get());
      }
      return targetNode.isPresent();
    }
  }

  /**
   * Moves to previous variation (variation to the left) if possible, or back to main trunk To move
   * to another variation, the variation must have the same number of moves in it.
   *
   * <p>Note: This method will always move back to main trunk, even if variation has more moves than
   * main trunk (if this case it will move to the last move in the trunk).
   *
   * @return true if there exist a target variation
   */
  public boolean previousBranch() {
    synchronized (this) {
      BoardHistoryNode currentNode = history.getCurrentHistoryNode();
      Optional<BoardHistoryNode> targetNode = Optional.empty();
      for (BoardHistoryNode candidate : branchCandidates(currentNode)) {
        if (candidate == currentNode) {
          break;
        } else {
          targetNode = Optional.of(candidate);
        }
      }
      if (targetNode.isPresent()) {
        moveToAnyPosition(targetNode.get());
      }
      return targetNode.isPresent();
    }
  }

  /**
   * Jump anywhere in the board history tree.
   *
   * @param targetNode history node to be located
   * @return void
   */
  public void moveToAnyPosition(BoardHistoryNode targetNode) {
    List<Integer> targetParents = new ArrayList<Integer>();
    List<Integer> sourceParents = new ArrayList<Integer>();

    BiConsumer<BoardHistoryNode, List<Integer>> populateParent =
        (node, parentList) -> {
          Optional<BoardHistoryNode> prevNode = node.previous();
          while (prevNode.isPresent()) {
            BoardHistoryNode p = prevNode.get();
            for (int m = 0; m < p.numberOfChildren(); m++) {
              if (p.getVariation(m).get() == node) {
                parentList.add(m);
              }
            }
            node = p;
            prevNode = p.previous();
          }
        };

    // Compute the path from the current node to the root
    populateParent.accept(history.getCurrentHistoryNode(), sourceParents);

    // Compute the path from the target node to the root
    populateParent.accept(targetNode, targetParents);

    // Compute the distance from source to the deepest common answer
    int targetDepth = targetParents.size();
    int sourceDepth = sourceParents.size();
    int maxDepth = min(targetParents.size(), sourceParents.size());
    int depth;
    for (depth = 0; depth < maxDepth; depth++) {
      int sourceParent = sourceParents.get(sourceDepth - depth - 1);
      int targetParent = targetParents.get(targetDepth - depth - 1);
      if (sourceParent != targetParent) {
        break;
      }
    }

    // Move all the way up to the deepest common ansestor
    for (int m = 0; m < sourceDepth - depth; m++) {
      previousMove();
    }

    // Then all the way down to the target
    for (int m = targetDepth - depth; m > 0; m--) {
      nextVariation(targetParents.get(m - 1));
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
      BoardHistoryNode currentNode = history.getCurrentHistoryNode();
      if (currentNode.next(true).isPresent()) {
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
      if (currentNode.previous().isPresent()) {
        BoardHistoryNode pre = currentNode.previous().get();
        previousMove();
        int idx = pre.indexOfNode(currentNode);
        pre.deleteChild(idx);
      } else {
        clear(); // Clear the board if we're at the top
      }
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
    Lizzie.leelaz.clear();
    Lizzie.frame.resetTitle();
    Lizzie.frame.clear();
    initialize();
    setKomi(getHistory().getGameInfo().getKomi());
  }

  /** Goes to the previous coordinate, thread safe */
  public boolean previousMove() {
    Lizzie.frame.clearBeforeMove();
    synchronized (this) {
      if (inScoreMode()) setScoreMode(false);
      updateWinrate();
      Lizzie.leelaz.beginModifyingBoard();
      if (history.previous().isPresent()) {
        Lizzie.leelaz.undo();
        Lizzie.frame.refresh();
        Lizzie.leelaz.endModifyingBoard();
        return true;
      }
      Lizzie.leelaz.endModifyingBoard();
      return false;
    }
  }

  public boolean undoToChildOfPreviousWithVariation() {
    BoardHistoryNode start = history.getCurrentHistoryNode();
    Optional<BoardHistoryNode> goal = start.findChildOfPreviousWithVariation();
    if (!goal.isPresent() || start == goal.get()) return false;
    while (history.getCurrentHistoryNode() != goal.get() && previousMove()) ;
    return true;
  }

  public void setScoreMode(boolean on) {
    if (on) {
      // load a copy of the data at the current node of history
      capturedStones = history.getStones().clone();
    } else {
      capturedStones = new Stone[] {};
    }
    scoreMode = on;
    Lizzie.frame.updateScoreMenu(on);
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
      while (x < boardWidth) {
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
        if (y + 1 < boardHeight
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
    Lizzie.frame.refresh();
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
    // Found will either be black or white, or dame if both are found in area
    Stone found = Stone.EMPTY;
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
      while (x < boardWidth && emptyOrCaptured(shdwstones, x, y)) {
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
        if (y + 1 < boardHeight && shdwstones[getIndex(x, y + 1)] != Stone.DAME) {
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
      if (x < boardWidth && shdwstones[getIndex(x, y)] != found) {
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

    for (int i = 0; i < boardWidth; i++) {
      for (int j = 0; j < boardHeight; j++) {
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
    for (int i = 0; i < boardWidth; i++) {
      for (int j = 0; j < boardHeight; j++) {
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
      if (!getNextMove().isPresent()) return;
      String answer =
          JOptionPane.showInputDialog(
              "# playouts for analysis (e.g. 100 (fast) or 50000 (slow)): ");
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
          (history.getPrevious().isPresent()
              && !history.getPrevious().get().lastMove.isPresent()
              && !getLastMove().isPresent());
      // Note: We cannot replace this history.getNext() with getNextMove()
      // because the latter returns null if the next move is "pass".
      if (!history.getNext().isPresent() || isSuccessivePass) {
        // Reached the end...
        toggleAnalysis();
        Lizzie.leelaz.togglePonder();
      } else if (bestMoves.isEmpty()) {
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

  public void updateWinrate() {
    Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
    if (stats.maxWinrate >= 0 && stats.totalPlayouts > history.getData().getPlayouts()) {
      history.getData().winrate = stats.maxWinrate;
      // we won't set playouts here. but setting winrate is ok... it shows the user that we are
      // computing. i think its fine.
    }
  }

  public boolean changeMove(int moveNumber, String changeMove) {
    Optional<int[]> changeCoord = asCoordinates(changeMove);
    if ("pass".equalsIgnoreCase(changeMove)) {
      changeMove(moveNumber, (int[]) null);
      return true;
    } else if ("swap".equalsIgnoreCase(changeMove)) {
      changeMove(moveNumber, null, true);
      return true;
    } else if (changeCoord.isPresent()
        && Board.isValid(changeCoord.get()[0], changeCoord.get()[1])) {
      changeCoord.map(c -> changeMove(moveNumber, c, false));
      return true;
    } else {
      return false;
    }
  }

  public boolean changeMove(int moveNumber, int[] coords) {
    return changeMove(moveNumber, coords, false);
  }

  public boolean changeMove(int moveNumber, int[] coords, boolean swapColorOnly) {
    if (moveNumber <= 0) {
      return false;
    }

    int endMoveNumber = history.getEnd().moveNumberOfNode();
    if (moveNumber > endMoveNumber) {
      return false;
    }

    int currentMoveNumber = history.getMoveNumber();

    goToMoveNumber(moveNumber - 1);

    Optional<BoardHistoryNode> changeNode = history.getCurrentHistoryNode().next();
    Optional<BoardHistoryNode> relink = changeNode.flatMap(n -> n.next());

    // Change Move
    if (swapColorOnly) {
      if (changeNode.isPresent()) {
        Optional<int[]> c = changeNode.get().getData().lastMove;
        if (c.isPresent() && isValid(c.get())) {
          changeNode
              .map(n -> n.getData())
              .map(d -> d.lastMoveColor)
              .ifPresent(s -> place(c.get()[0], c.get()[1], s.opposite(), false, true));
        } else {
          pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE, false, false, true);
        }
      }
    } else {
      if (coords != null && Board.isValid(coords[0], coords[1])) {
        place(
            coords[0], coords[1], history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE, false, true);
      } else {
        pass(history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE, false, false, true);
      }
    }

    Optional<BoardHistoryNode> node = relink;
    while (node.isPresent()) {
      Optional<int[]> lastMove = node.get().getData().lastMove;
      if (lastMove.isPresent()) {
        int[] m = lastMove.get();
        if (Board.isValid(m[0], m[1])) {
          place(
              m[0],
              m[1],
              swapColorOnly
                  ? node.get().getData().lastMoveColor
                  : history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE,
              false,
              true);
        } else {
          pass(
              swapColorOnly
                  ? node.get().getData().lastMoveColor
                  : history.isBlacksTurn() ? Stone.BLACK : Stone.WHITE,
              false,
              false,
              true);
        }
        node = node.get().next();
      }
    }

    goToMoveNumber(currentMoveNumber);

    return true;
  }

  public ArrayList<MoveList> getMoveList() {
    ArrayList<MoveList> moveList = new ArrayList<MoveList>();

    Optional<BoardHistoryNode> node = history.getCurrentHistoryNode().now();
    Optional<int[]> passstep = Optional.empty();
    while (node.isPresent()) {
      Optional<int[]> lastMove = node.get().getData().lastMove;
      if (lastMove == passstep) {
        MoveList move = new MoveList();
        move.isPass = true;
        move.isBlack = node.get().getData().lastMoveColor.isBlack();
        moveList.add(move);
        node = node.get().previous();
      } else {
        if (lastMove.isPresent()) {

          int[] n = lastMove.get();
          MoveList move = new MoveList();
          move.x = n[0];
          move.y = n[1];
          move.isPass = false;
          move.isBlack = node.get().getData().lastMoveColor.isBlack();
          move.moveNum = node.get().getData().moveNumber;
          moveList.add(move);
          node = node.get().previous();
        }
      }
    }
    moveList.remove(moveList.size() - 1);
    return moveList;
  }

  public boolean setAvoidCoords(int x, int y) {
    Optional<int[]> boardCoordinates = Lizzie.frame.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      if (isCoordsEmpty(boardCoordinates.get()[0], boardCoordinates.get()[1])) {
        if (avoidCoords == "")
          avoidCoords =
              convertCoordinatesToName(boardCoordinates.get()[0], boardCoordinates.get()[1]);
        else
          avoidCoords =
              avoidCoords
                  + ","
                  + convertCoordinatesToName(boardCoordinates.get()[0], boardCoordinates.get()[1]);
        return true;
      } else return false;
    }
    return false;
  }

  public boolean setAllowCoords(int x, int y) {
    Optional<int[]> boardCoordinates = Lizzie.frame.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      if (isCoordsEmpty(boardCoordinates.get()[0], boardCoordinates.get()[1])) {
        allowCoords =
            convertCoordinatesToName(boardCoordinates.get()[0], boardCoordinates.get()[1]);
        return true;
      } else return false;
    }
    return false;
  }

  public boolean addAllowCoords(int x, int y) {
    Optional<int[]> boardCoordinates = Lizzie.frame.convertScreenToCoordinates(x, y);
    if (boardCoordinates.isPresent()) {
      if (isCoordsEmpty(boardCoordinates.get()[0], boardCoordinates.get()[1])) {
        allowCoords =
            allowCoords
                + ","
                + convertCoordinatesToName(boardCoordinates.get()[0], boardCoordinates.get()[1]);
        return true;
      } else return false;
    }
    return false;
  }

  public boolean isCoordsEmpty(int x, int y) {
    if (history.getStones()[getIndex(x, y)] != Stone.EMPTY) {
      return false;
    }
    return true;
  }

  public boolean setAsMainBranch() {
    if (history.getCurrentHistoryNode().isMainTrunk()) return false;
    BoardHistoryNode topNode = history.getCurrentHistoryNode().topOfFatherBranch();
    BoardHistoryNode mainNode = history.getCurrentHistoryNode().nodeBeforeTopOfFatherBranch();
    BoardHistoryNode oldFirstVar = mainNode.variations.get(0);
    for (int i = 0; i < mainNode.variations.size(); i++) {
      if (mainNode.variations.get(i) == topNode) {
        mainNode.variations.remove(i);
        mainNode.variations.add(i, oldFirstVar);
        mainNode.variations.remove(0);
        mainNode.variations.add(0, topNode);
        return true;
      }
    }
    return false;
  }
}
