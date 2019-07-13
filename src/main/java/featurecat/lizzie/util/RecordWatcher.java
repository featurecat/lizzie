package featurecat.lizzie.util;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.SGFParser;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecordWatcher implements ActionListener {
  private File file = new File("C:\\Users\\ebifrier\\Desktop\\M20181009w_v.sgf");
  private long lastLoaded = -1;

  public void actionPerformed(ActionEvent e) {
    if (lastLoaded < 0 || file.lastModified() > lastLoaded) {
      loadFileExtend(file);
      lastLoaded = file.lastModified();
      System.out.println(String.format("loaded file: %s", file.getPath()));
    }
  }

  private void loadFileExtend(File file) {
    if (file == null || !file.exists()) return;

    try {
      BoardHistoryList history = Lizzie.board.getHistory();
      BoardHistoryNode current = history.getCurrentHistoryNode();
      int variationSize = current.numberOfChildren();

      history.toStart();
      SGFParser.load(file.getPath(), true);

      // 以下のすべての条件が満たされる場合は自動的に棋譜を進めます。
      // (1) currentから読み込んだ棋譜に打ち手を戻さず移動できる
      // (2) currentに変化が増えている（currentノードに手が読み込まれた場合）
      //
      // 考えられる状況
      // 成功 case1)
      //   currentが最新局面にいる＆後続する変化がないときにそこからの手が追加された場合は
      //   条件(1),(2)は両方とも満たされます。
      // 成功 case2)
      //   currentは最新局面にいるが検討された手がいくつかある場合
      //   条件(1),(2)は両方とも満たされます。
      //
      // 失敗 case1)
      //   currentが過去局面にいて、すでに読み込まれた手を再度読み込んだ場合は
      //   条件(2)が満たされないため自動的に移動しません。
      // 失敗 case2)
      // 　currentが最新局面より新しい手を検討している場合は
      //   条件(1)が満たされないため自動的に移動しません。
      BoardHistoryNode endNode = history.getCurrentHistoryNode();
      Optional<ArrayList<Integer>> idxList = current.getGotoVariationIdxList(endNode);
      boolean needToMove = (idxList.isPresent() && current.numberOfChildren() != variationSize);

      // restore the starting node
      history.setCurrentHistoryNode(current);

      if (needToMove) {
        gotoNode(idxList.get());
        Lizzie.frame.refresh();
      }
    } catch (IOException err) {
      System.err.println(
          String.format("failed to load file: %s\n%s", file.getPath(), err.getMessage()));
    }
  }

  private void gotoNode(List<Integer> idxList) {
    for (int idx : idxList) {
      Lizzie.board.nextVariation(idx);
    }
  }
}
