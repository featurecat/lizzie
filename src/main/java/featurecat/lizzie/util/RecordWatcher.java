package featurecat.lizzie.util;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.SGFParser;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecordWatcher implements ActionListener {
  private File file_;
  private long lastLoaded_ = -1;

  public File getFile() {
    return file_;
  }

  public String getFilePath() {
    return (file_ != null ? file_.getPath() : null);
  }

  public void setFilePath(String filePath) {
    file_ = filePath != null ? new File(filePath) : null;
    if (Lizzie.frame != null) {
      Lizzie.config.persisted.put("watchFilePath", filePath);
      Lizzie.frame.updateTitle();
    }

    resetLastLoaded();
  }

  public void resetLastLoaded() {
    lastLoaded_ = -1;
  }

  public void actionPerformed(ActionEvent e) {
    File file = file_;
    if (file == null || !file.exists()) {
      return;
    }

    if (lastLoaded_ < 0 || file.lastModified() > lastLoaded_) {
      loadFileExtend(file);
      lastLoaded_ = file.lastModified();
      System.out.println(String.format("loaded file: %s", file.getPath()));
    }
  }

  public void loadFileExtend(File file) {
    BoardHistoryList history = Lizzie.board.getHistory();
    BoardHistoryNode current = history.getCurrentHistoryNode();

    try {
      boolean currentWasMain = current.getData().main;
      Optional<BoardHistoryNode> oldMainChild = current.getMainChild();

      history.toStart();
      history.root().resetMainSubTree();
      SGFParser.load(file.getPath(), true);

      BoardHistoryNode endNode = history.getCurrentHistoryNode();
      Optional<ArrayList<Integer>> idxList = getGotoIdxListIfNeed(current, endNode, oldMainChild);

      // restore the starting node
      history.setCurrentHistoryNode(current);

      if (currentWasMain && idxList.isPresent()) {
        gotoNode(idxList.get());
        Lizzie.frame.refresh();
        System.out.println("need to move");
      }
    } catch (Exception err) {
      history.setCurrentHistoryNode(current);
      System.err.println(
          String.format("failed to load file: %s\n%s", file.getPath(), err.getMessage()));
    }
  }

  /**
   * 棋譜が更新されたとき、自動的に盤面の手を進めるかどうかを判定します。
   *
   * <p>以下のすべての条件が満たされる場合は自動的に棋譜を進めます。
   *
   * <ol>
   *   <li>読み込み前にcurrentが本譜であった。
   *   <li>currentから読み込んだ棋譜に打ち手を戻さず移動できる。
   *   <li>currentに本譜の手が増えている or 次の手に新しく本譜のマークがついた。
   * </ol>
   *
   * <ol>
   *   <li>case1 成功<br>
   *       currentが最新局面にいる＆後続する変化がないとき、本譜の手が追加された場合は条件がすべて満たされます。
   *   <li>case2 成功<br>
   *       currentは最新局面にいるが検討された手がいくつかある場合でも、本譜の手が増えたときは条件がすべて満たされます。
   *   <li>case3 失敗<br>
   *       currentが過去局面にいて、すでに読み込まれた手を再度読み込んだ場合は条件(3)が満たされないため移動しません。
   *   <li>case4 失敗<br>
   *       currentが最新局面より新しい手を検討している場合は条件(1)が満たされないため移動しません。
   * </ol>
   */
  public static Optional<ArrayList<Integer>> getGotoIdxListIfNeed(
      BoardHistoryNode current, BoardHistoryNode endNode, Optional<BoardHistoryNode> oldMainChild) {
    Optional<ArrayList<Integer>> idxList = current.getGotoVariationIdxList(endNode);
    if (!idxList.isPresent()) { // (2)
      return Optional.empty();
    }

    Optional<BoardHistoryNode> newMainChild = current.getMainChild();
    if (!newMainChild.isPresent()) { // (3)
      return Optional.empty();
    }

    if (oldMainChild.isPresent() && newMainChild.get() == oldMainChild.get()) { // (3)
      return Optional.empty();
    }

    return idxList;
  }

  private void gotoNode(List<Integer> idxList) {
    for (int idx : idxList) {
      Lizzie.board.nextVariation(idx);
    }
  }
}
