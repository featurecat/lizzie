package wagner.stephanie.lizzie.analysis;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.BoardData;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Stone;
import java.util.List;
import wagner.stephanie.lizzie.rules.Zobrist;

public class Branch {
    public BoardData data;

    public Branch(Board board, List<String> variation) {
        Stone[] stones = board.getStones().clone();
        int[] lastMove = board.getLastMove() == null ? null : board.getLastMove();
        Stone lastMoveColor = board.getData().lastMoveColor;
        boolean blackToPlay = board.getData().blackToPlay;
        Zobrist zobrist = board.getData().zobrist == null? null:board.getData().zobrist.clone();
        int moveNumber = 0;
        int[] moveNumberList = new int[Board.BOARD_SIZE * Board.BOARD_SIZE];

        this.data = new BoardData(stones, lastMove, lastMoveColor, blackToPlay, zobrist, moveNumber, moveNumberList);

        for (int i = 0; i < variation.size(); i++) {
            int[] coord = Board.convertNameToCoordinates(variation.get(i).trim());
            data.lastMove = coord;
            if (data.blackToPlay) {
                data.stones[Board.getIndex(coord[0], coord[1])] = Stone.BLACK;
            } else {
                data.stones[Board.getIndex(coord[0], coord[1])] = Stone.WHITE;
            }
            data.moveNumberList[Board.getIndex(coord[0], coord[1])] = i + 1;
            data.lastMoveColor = data.blackToPlay?Stone.WHITE:Stone.BLACK;
            data.blackToPlay = !data.blackToPlay;
        }
    }
}
