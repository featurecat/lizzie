package wagner.stephanie.lizzie.rules;

public enum Stone {
    BLACK, WHITE, EMPTY, BLACK_RECURSED, WHITE_RECURSED;

    /**
     * used to find the opposite color stone
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
     * @return the recursed version of this stone color
     */
    public Stone recursed() {
        switch(this) {
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
}
