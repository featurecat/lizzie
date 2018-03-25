package wagner.stephanie.lizzie.rules;

public class BoardData {
    public int moveNumber;
    public int[] lastMove;
    public int[] moveNumberList;
    public boolean blackToPlay;

    public Stone lastMoveColor;
    public Stone[] stones;
    public Zobrist zobrist;

    public BoardData(Stone[] stones, int[] lastMove, Stone lastMoveColor, boolean blackToPlay, Zobrist zobrist, int moveNumber, int[] moveNumberList) {
        this.moveNumber = moveNumber;
        this.lastMove = lastMove;
        this.moveNumberList = moveNumberList;
        this.blackToPlay = blackToPlay;

        this.lastMoveColor = lastMoveColor;
        this.stones = stones;
        this.zobrist = zobrist;
    }
}
