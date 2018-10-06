package featurecat.lizzie.rules;

public enum Stone {
  BLACK,
  WHITE,
  EMPTY,
  BLACK_RECURSED,
  WHITE_RECURSED,
  BLACK_GHOST,
  WHITE_GHOST,
  DAME,
  BLACK_POINT,
  WHITE_POINT,
  BLACK_CAPTURED,
  WHITE_CAPTURED;

  /**
   * used to find the opposite color stone
   *
   * @return the opposite stone type
   */
  public Stone opposite() {
    switch (this) {
      case BLACK:
        return WHITE;
      case WHITE:
        return BLACK;
      default:
        return this;
    }
  }

  /**
   * used to keep track of which stones were visited during removal of dead stones
   *
   * @return the recursed version of this stone color
   */
  public Stone recursed() {
    switch (this) {
      case BLACK:
        return BLACK_RECURSED;
      case WHITE:
        return WHITE_RECURSED;
      default:
        return this;
    }
  }

  /**
   * used to keep track of which stones were visited during removal of dead stones
   *
   * @return the unrecursed version of this stone color
   */
  public Stone unrecursed() {
    switch (this) {
      case BLACK_RECURSED:
        return BLACK;
      case WHITE_RECURSED:
        return WHITE;
      default:
        return this;
    }
  }

  /** @return Whether or not this stone is of the black variants. */
  public boolean isBlack() {
    return this == BLACK || this == BLACK_RECURSED || this == BLACK_GHOST;
  }

  /** @return Whether or not this stone is of the white variants. */
  public boolean isWhite() {
    return this != EMPTY && !this.isBlack();
  }

  public Stone unGhosted() {
    switch (this) {
      case BLACK:
      case BLACK_GHOST:
        return BLACK;
      case WHITE:
      case WHITE_GHOST:
        return WHITE;
      default:
        return EMPTY;
    }
  }
}
