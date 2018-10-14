package featurecat.lizzie.rules;

import java.util.Random;

/** Used to maintain zobrist hashes for ko detection */
public class Zobrist {
  private static final long[] blackZobrist, whiteZobrist;

  // initialize zobrist hashing
  static {
    Random random = new Random();
    blackZobrist = new long[Board.boardSize * Board.boardSize];
    whiteZobrist = new long[Board.boardSize * Board.boardSize];

    for (int i = 0; i < blackZobrist.length; i++) {
      blackZobrist[i] = random.nextLong();
      whiteZobrist[i] = random.nextLong();
    }
  }

  // hash to be used to compare two board states
  private long zhash;

  public Zobrist() {
    zhash = 0;
  }

  public Zobrist(long zhash) {
    this.zhash = zhash;
  }

  /** @return a copy of this zobrist */
  public Zobrist clone() {
    return new Zobrist(zhash);
  }

  /**
   * Call this method to alter the current zobrist hash for this stone
   *
   * @param x x coordinate -- must be valid
   * @param y y coordinate -- must be valid
   * @param color color of the stone to alter (for adding or removing a stone color)
   */
  public void toggleStone(int x, int y, Stone color) {
    switch (color) {
      case BLACK:
        zhash ^= blackZobrist[Board.getIndex(x, y)];
        break;
      case WHITE:
        zhash ^= whiteZobrist[Board.getIndex(x, y)];
        break;
      default:
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Zobrist && (((Zobrist) o).zhash == zhash);
  }

  @Override
  public int hashCode() {
    return (int) zhash;
  }

  @Override
  public String toString() {
    return "" + zhash;
  }
}
