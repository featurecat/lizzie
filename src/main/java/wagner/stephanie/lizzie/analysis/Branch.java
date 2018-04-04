package wagner.stephanie.lizzie.analysis;

import wagner.stephanie.lizzie.rules.Board;
import wagner.stephanie.lizzie.rules.BoardData;
import wagner.stephanie.lizzie.rules.Stone;
import wagner.stephanie.lizzie.rules.Zobrist;

import java.util.List;

public class Branch {
    public BoardData data;

    public Branch(Board board, List<String> variation) {
        int moveNumber = 0;
        int[] lastMove = board.getLastMove();
        int[] moveNumberList = new int[Board.BOARD_SIZE * Board.BOARD_SIZE];
        boolean blackToPlay = board.getData().blackToPlay;

        Stone lastMoveColor = board.getData().lastMoveColor;
        Stone[] stones = board.getStones().clone();
        Zobrist zobrist = board.getData().zobrist == null ? null : board.getData().zobrist.clone();

        this.data = new BoardData(stones, lastMove, lastMoveColor, blackToPlay, zobrist, moveNumber, moveNumberList);

        for (int i = 0; i < variation.size(); i++) {
            int[] coord = Board.convertNameToCoordinates(variation.get(i));
            data.lastMove = coord;
            data.stones[Board.getIndex(coord[0], coord[1])] = data.blackToPlay ? Stone.BLACK_GHOST : Stone.WHITE_GHOST;
            data.moveNumberList[Board.getIndex(coord[0], coord[1])] = i + 1;
            data.lastMoveColor = data.blackToPlay ? Stone.WHITE : Stone.BLACK;
            data.blackToPlay = !data.blackToPlay;
        }
    }
}
