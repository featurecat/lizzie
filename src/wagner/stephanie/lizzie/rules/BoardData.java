package wagner.stephanie.lizzie.rules;

public class BoardData {
    public Stone[] stones;
    public int[] lastMove;
    public Stone lastMoveColor;
    public boolean blackToPlay;
    public Zobrist zobrist;
    public int moveNumber;
    public int[] moveNumberList;

    public BoardData(Stone[] stones, int[] lastMove, Stone lastMoveColor, boolean blackToPlay, Zobrist zobrist, int moveNumber, int[] moveNumberList) {
        this.stones = stones;
        this.lastMove = lastMove;
        this.lastMoveColor = lastMoveColor;
        this.blackToPlay = blackToPlay;
        this.zobrist = zobrist;
        this.moveNumber = moveNumber;
        this.moveNumberList = moveNumberList;
    }
}
