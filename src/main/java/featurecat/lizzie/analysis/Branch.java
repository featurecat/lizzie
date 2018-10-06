package featurecat.lizzie.analysis;

import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.Stone;
import featurecat.lizzie.rules.Zobrist;
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

    // Dont care about winrate for branch
    this.data =
        new BoardData(
            stones,
            lastMove,
            lastMoveColor,
            blackToPlay,
            zobrist,
            moveNumber,
            moveNumberList,
            board.getData().blackCaptures,
            board.getData().whiteCaptures,
            0.0,
            0);

    for (int i = 0; i < variation.size(); i++) {
      int[] coord = Board.convertNameToCoordinates(variation.get(i));
      if (coord == null) break;
      data.lastMove = coord;
      data.stones[Board.getIndex(coord[0], coord[1])] =
          data.blackToPlay ? Stone.BLACK_GHOST : Stone.WHITE_GHOST;
      data.moveNumberList[Board.getIndex(coord[0], coord[1])] = i + 1;
      data.lastMoveColor = data.blackToPlay ? Stone.WHITE : Stone.BLACK;
      data.blackToPlay = !data.blackToPlay;
    }
  }
}
