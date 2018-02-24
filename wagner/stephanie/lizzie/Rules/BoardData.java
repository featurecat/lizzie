package wagner.stephanie.lizzie.Rules;

public class BoardData {
    public Stone[] stones;
    public int[] lastMove;
    public boolean blackToPlay;
    public Zobrist zobrist;

    public BoardData(Stone[] stones, int[] lastMove, boolean blackToPlay, Zobrist zobrist) {
        this.stones = stones;
        this.lastMove = lastMove;
        this.blackToPlay = blackToPlay;
        this.zobrist = zobrist;
    }
}
