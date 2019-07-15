package featurecat.lizzie.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import featurecat.lizzie.Config;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.rules.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class RecordWatcherTest {
  @BeforeClass
  public static void setup() throws IOException {
    Lizzie.config = new Config();
    Lizzie.board = new Board();
    Lizzie.frame = new LizzieFrame();
  }

  private void assertNeedToMove(BoardHistoryList history, String sgf, boolean needToMove) {
    BoardHistoryNode current = history.getCurrentHistoryNode();
    boolean currentWasMain = current.getData().main;
    Optional<BoardHistoryNode> oldMainChild = current.getMainChild();

    history.toStart();
    history.root().resetMainSubTree();
    assertTrue(SGFParser.loadFromString(sgf, true));

    BoardHistoryNode endNode = history.getCurrentHistoryNode();
    Optional<ArrayList<Integer>> idxList =
        RecordWatcher.getGotoIdxListIfNeed(current, endNode, oldMainChild);
    assertEquals(needToMove, currentWasMain && idxList.isPresent());

    history.setCurrentHistoryNode(current);
  }

  @Test
  public void testGetGotoIdxListIfNeed() {
    Lizzie.board = new Board();
    assertTrue(Lizzie.board.getData().main);

    String sgf = "(;GM[1]FF[4]SZ[19];B[dq])";
    assertTrue(SGFParser.loadFromString(sgf));
    while (Lizzie.board.nextMove()) ;

    BoardHistoryList history = Lizzie.board.getHistory();
    assertEquals(1, history.getMoveNumber());
    assertTrue(history.getData().main);

    // 最新局面から手が更新された場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd])";
    assertNeedToMove(history, sgf, true);

    // 最新局面でないところにいた場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd])";
    history.previous();
    assertNeedToMove(history, sgf, false);

    // 検討した後で本譜でない手があった場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd];W[aa])";
    while (history.next().isPresent()) ;
    history.place(0, 0, Stone.WHITE);
    history.previous();
    assertNeedToMove(history, sgf, true);

    // 検討した後で本譜でない手があった場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd];W[aa];B[bb])";
    while (history.next().isPresent()) ;
    history.place(2, 4, Stone.BLACK);
    history.previous();
    assertNeedToMove(history, sgf, true);

    // 違う本譜になった場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd];W[aa];B[cc])";
    assertArrayEquals(history.getLastMove().get(), new int[] {0, 0});
    assertNeedToMove(history, sgf, true);

    // 違う変化にいる場合
    sgf = "(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd];W[dd];B[ee])";
    assertNeedToMove(history, sgf, false);

    // 最新局面に移動
    history.previous();
    history.nextVariation(1);
    history.nextVariation(0);

    // エラーの場合
    sgf = "(;GM[1]FF[4]SZ[19];BDLlXIo[DLKj])";
    assertEquals(5, history.getMoveNumber());
    assertNeedToMove(history, sgf, false);
    assertEquals(5, history.getMoveNumber());
  }

  private static String TestFilePath = "test.sgf";

  private void saveSgf(String sgf) throws IOException {
    File file = new File(TestFilePath);

    try (FileWriter writer = new FileWriter(file)) {
      writer.write(sgf);
    }

    StringBuilder builder = new StringBuilder();
    try (FileReader reader = new FileReader(file)) {
      while (reader.ready()) {
        builder.append((char) reader.read());
      }
    }

    assertEquals(sgf, builder.toString());
  }

  /** fileが削除できるかテスト */
  @Test
  public void testFile() throws IOException {
    File file = new File("test2.sgf");

    try (FileWriter filewriter = new FileWriter(file)) {
      filewriter.write("(;GM[1]FF[4]SZ[19];B[dq])");
    }
    SGFParser.load(file.getPath());

    assertTrue(file.exists());
    assertTrue(file.delete());
  }

  @Test
  public void testLoadFileExtend() throws IOException, InterruptedException {
    Lizzie.board = new Board();
    BoardHistoryList history = Lizzie.board.getHistory();

    RecordWatcher watcher = new RecordWatcher();
    watcher.setFilePath(TestFilePath);

    // 最新局面から手が更新された場合
    saveSgf("(;GM[1]FF[4]SZ[19];B[dq])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(1, Lizzie.board.getData().moveNumber);

    // 最新局面でないところにいた場合
    assertTrue(Lizzie.board.previousMove());
    assertEquals(0, Lizzie.board.getData().moveNumber);

    saveSgf("(;GM[1]FF[4]SZ[19];B[dq];W[cd])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(0, Lizzie.board.getData().moveNumber);

    // 検討した後で本譜でない手があった場合
    assertTrue(Lizzie.board.nextMove());
    assertArrayEquals(new int[] {3, 16}, Lizzie.board.getLastMove().get());
    assertTrue(Lizzie.board.nextMove());
    assertArrayEquals(new int[] {2, 3}, Lizzie.board.getLastMove().get());
    assertEquals(2, Lizzie.board.getData().moveNumber);

    Lizzie.board.place(0, 0, Stone.WHITE);
    assertEquals(3, Lizzie.board.getData().moveNumber);
    Lizzie.board.previousMove();
    assertEquals(2, Lizzie.board.getData().moveNumber);

    saveSgf("(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[aa])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(3, Lizzie.board.getData().moveNumber);

    // 違う本譜になった場合
    Lizzie.board.previousMove();
    assertEquals(2, Lizzie.board.getData().moveNumber);

    saveSgf("(;GM[1]FF[4]SZ[19];B[dq];W[cd];B[pd])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(3, Lizzie.board.getData().moveNumber);

    // 違う変化にいる場合
    while (Lizzie.board.nextMove()) ;
    assertEquals(3, Lizzie.board.getData().moveNumber);

    saveSgf("(;GM[1]FF[4]SZ[19];B[dq];W[ee])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(3, Lizzie.board.getData().moveNumber);

    // エラーの場合
    saveSgf("(;GM[1]FF[4]SZ[19];BDLlXIo[DLKj])");
    watcher.loadFileExtend(watcher.getFile());
    assertEquals(3, history.getMoveNumber());

    // ファイルが消えた場合
    assertTrue(watcher.getFile().delete());
  }
}
