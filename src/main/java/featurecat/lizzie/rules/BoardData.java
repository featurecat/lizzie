package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.MoveData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BoardData {
  public int moveNumber;
  public int moveMNNumber;
  public Optional<int[]> lastMove;
  public int[] moveNumberList;
  public boolean blackToPlay;
  public boolean dummy;
  public boolean main;

  public Stone lastMoveColor;
  public Stone[] stones;
  public Zobrist zobrist;
  public boolean verify;

  public double winrate;
  private int playouts;
  public List<MoveData> bestMoves;
  public int blackCaptures;
  public int whiteCaptures;

  public String comment = "";

  // Node properties
  private final Map<String, String> properties = new HashMap<String, String>();

  public BoardData(
      Stone[] stones,
      Optional<int[]> lastMove,
      Stone lastMoveColor,
      boolean blackToPlay,
      Zobrist zobrist,
      int moveNumber,
      int[] moveNumberList,
      int blackCaptures,
      int whiteCaptures,
      double winrate,
      int playouts) {
    this.moveMNNumber = -1;
    this.moveNumber = moveNumber;
    this.lastMove = lastMove;
    this.moveNumberList = moveNumberList;
    this.blackToPlay = blackToPlay;
    this.dummy = false;
    this.main = false;

    this.lastMoveColor = lastMoveColor;
    this.stones = stones;
    this.zobrist = zobrist;
    this.verify = false;

    this.winrate = winrate;
    this.playouts = playouts;
    this.blackCaptures = blackCaptures;
    this.whiteCaptures = whiteCaptures;
    this.bestMoves = new ArrayList<>();
  }

  public static BoardData empty(int width, int height, boolean main) {
    Stone[] stones = new Stone[width * height];
    for (int i = 0; i < stones.length; i++) {
      stones[i] = Stone.EMPTY;
    }

    int[] boardArray = new int[width * height];
    BoardData data =
        new BoardData(
            stones, Optional.empty(), Stone.EMPTY, true, new Zobrist(), 0, boardArray, 0, 0, 50, 0);
    data.main = main;
    return data;
  }

  /**
   * Add a key and value
   *
   * @param key
   * @param value
   */
  public void addProperty(String key, String value) {
    SGFParser.addProperty(properties, key, value);
    if ("N".equals(key) && comment.isEmpty()) {
      comment = value;
    } else if ("MN".equals(key)) {
      moveMNNumber = Integer.parseInt(getOrDefault("MN", "-1"));
    }
  }

  /**
   * Get a value with key
   *
   * @param key
   * @return
   */
  public String getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Get a value with key, or the default if there is no such key
   *
   * @param key
   * @param defaultValue
   * @return
   */
  public String getOrDefault(String key, String defaultValue) {
    return SGFParser.getOrDefault(properties, key, defaultValue);
  }

  /**
   * Get the properties
   *
   * @return
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Add the properties
   *
   * @return
   */
  public void addProperties(Map<String, String> addProps) {
    SGFParser.addProperties(this.properties, addProps);
  }

  /**
   * Add the properties from string
   *
   * @return
   */
  public void addProperties(String propsStr) {
    SGFParser.addProperties(properties, propsStr);
  }

  /**
   * Get properties string
   *
   * @return
   */
  public String propertiesString() {
    return SGFParser.propertiesString(properties);
  }

  public double getWinrate() {
    if (!blackToPlay || !Lizzie.config.uiConfig.getBoolean("win-rate-always-black")) {
      return winrate;
    } else {
      return 100 - winrate;
    }
  }

  public void tryToClearBestMoves() {
    bestMoves = new ArrayList<>();
    playouts = 0;
    if (Lizzie.leelaz != null && Lizzie.leelaz.isKataGo) {
      Lizzie.leelaz.scoreMean = 0;
      Lizzie.leelaz.scoreStdev = 0;
    }
  }

  public void tryToSetBestMoves(List<MoveData> moves) {
    if (MoveData.getPlayouts(moves) > playouts) {
      bestMoves = moves;
      setPlayouts(MoveData.getPlayouts(moves));
      winrate = getWinrateFromBestMoves(moves);
    }
    if (Lizzie.leelaz != null && Lizzie.leelaz.isKataGo) {
      Lizzie.leelaz.scoreMean = moves.get(0).scoreMean;
      Lizzie.leelaz.scoreStdev = moves.get(0).scoreStdev;
    }
  }

  public static double getWinrateFromBestMoves(List<MoveData> bestMoves) {
    // return the weighted average winrate of bestMoves
    return bestMoves
        .stream()
        .mapToDouble(move -> move.winrate * move.playouts / MoveData.getPlayouts(bestMoves))
        .sum();
  }

  public static double getScoreMeanFromBestMoves(List<MoveData> bestMoves) {
    // return the weighted average scoreMean of bestMoves
    return bestMoves
        .stream()
        .mapToDouble(move -> move.scoreMean * move.playouts / MoveData.getPlayouts(bestMoves))
        .sum();
  }

  public String bestMovesToString() {
    StringBuilder sb = new StringBuilder();
    for (MoveData move : bestMoves) {
      // eg: info move R5 visits 38 winrate 5404 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5
      sb.append("move ").append(move.coordinate);
      sb.append(" visits ").append(move.playouts);
      sb.append(" winrate ").append((int) (move.winrate * 100));
      if (Lizzie.leelaz != null && Lizzie.leelaz.isKataGo)
        sb.append(" scoreMean ").append(move.scoreMean);
      sb.append(" pv ").append(move.variation.stream().reduce((a, b) -> a + " " + b).get());
      sb.append(" info "); // this order is just because of how the MoveData info parser works
    }
    return sb.toString();
  }

  public void setPlayouts(int playouts) {
    if (playouts > this.playouts) {
      this.playouts = playouts;
    }
  }

  public int getPlayouts() {
    return playouts;
  }

  public void sync(BoardData data) {
    this.moveMNNumber = data.moveMNNumber;
    this.moveNumber = data.moveNumber;
    this.lastMove = data.lastMove;
    this.moveNumberList = data.moveNumberList;
    this.blackToPlay = data.blackToPlay;
    this.dummy = data.dummy;
    this.lastMoveColor = data.lastMoveColor;
    this.stones = data.stones;
    this.zobrist = data.zobrist;
    this.verify = data.verify;
    this.blackCaptures = data.blackCaptures;
    this.whiteCaptures = data.whiteCaptures;
    this.comment = data.comment;
    this.main = data.main;
  }

  public BoardData clone() {
    BoardData data = BoardData.empty(19, 19, false);
    data.sync(this);
    return data;
  }

  public boolean isSameCoord(int[] coord) {
    if (coord == null || coord.length < 2 || !this.lastMove.isPresent()) {
      return false;
    }
    return this.lastMove.map(m -> (m[0] == coord[0] && m[1] == coord[1])).orElse(false);
  }
}
