package featurecat.lizzie.analysis;

import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.Stone;
import java.util.List;
import java.util.Optional;

public class Branch {
  public BoardData data;

  public Branch(Board board, List<String> variation) {
    int[] moveNumberList = new int[Board.boardSize * Board.boardSize];
    int moveNumber = 0;
    double winrate = 0.0;
    int playouts = 0;

    this.data =
        new BoardData(
            board.getStones().clone(),
            board.getLastMove(),
            board.getData().lastMoveColor,
            board.getData().blackToPlay,
            board.getData().zobrist.clone(),
            moveNumber,
            moveNumberList,
            board.getData().blackCaptures,
            board.getData().whiteCaptures,
            winrate,
            playouts);

    for (int i = 0; i < variation.size(); i++) {
      Optional<int[]> coordOpt = Board.asCoordinates(variation.get(i));
      if (!coordOpt.isPresent() || !Board.isValid(coordOpt.get()[0], coordOpt.get()[1])) {
        break;
      }
      int[] coord = coordOpt.get();
      data.lastMove = coordOpt;
      data.stones[Board.getIndex(coord[0], coord[1])] =
          data.blackToPlay ? Stone.BLACK_GHOST : Stone.WHITE_GHOST;
      data.moveNumberList[Board.getIndex(coord[0], coord[1])] = i + 1;
      data.lastMoveColor = data.blackToPlay ? Stone.WHITE : Stone.BLACK;
      data.blackToPlay = !data.blackToPlay;
    }
  }
}
