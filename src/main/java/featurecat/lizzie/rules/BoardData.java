package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.MoveData;

import java.util.*;

public class BoardData {
  public int moveNumber;
  public int moveMNNumber;
  public Optional<int[]> lastMove;
  public int[] moveNumberList;
  public boolean blackToPlay;
  public boolean dummy;

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

  public static BoardData empty(int size) {
    Stone[] stones = new Stone[size * size];
    for (int i = 0; i < stones.length; i++) {
      stones[i] = Stone.EMPTY;
    }

    int[] boardArray = new int[size * size];
    return new BoardData(
        stones, Optional.empty(), Stone.EMPTY, true, new Zobrist(), 0, boardArray, 0, 0, 50, 0);
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

  public static int bestMovesPlayoutThreshold = 0;
  public void tryToSetBestMoves(List<MoveData> moves) {
    if (MoveData.getPlayouts(moves) > playouts - bestMovesPlayoutThreshold) {
      bestMoves = moves;
      setPlayouts(MoveData.getPlayouts(moves));
    }
  }

  public String bestMovesToString() {
    StringBuilder sb = new StringBuilder();
    for (MoveData move : bestMoves) {
      // eg: info move R5 visits 38 winrate 5404 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5
      sb.append("move ").append(move.coordinate);
      sb.append(" visits ").append(move.playouts);
      sb.append(" winrate ").append((int)(move.winrate*100));
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
}
