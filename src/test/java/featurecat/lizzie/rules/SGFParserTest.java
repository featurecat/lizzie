package featurecat.lizzie.rules;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import common.Util;
import featurecat.lizzie.Config;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.gui.LizzieFrame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class SGFParserTest {

  private Lizzie lizzie;

  @Test
  public void run() throws IOException {
    lizzie = new Lizzie();
    lizzie.config = new Config();
    lizzie.board = new Board();
    lizzie.frame = new LizzieFrame();
    // new Thread( () -> {
    lizzie.leelaz = new Leelaz();
    // }).start();

    testVariaionOnly1();
    testFull1();
    testMore1();
  }

  public void testVariaionOnly1() throws IOException {

    String sgfString =
        "(;B[pd];W[dp];B[pp];W[dd];B[fq]"
            + "(;W[cn];B[cc];W[cd];B[dc];W[ed];B[fc];W[fd]"
            + "(;B[gb]"
            + "(;W[hc];B[nq])"
            + "(;W[gc];B[ec];W[hc];B[hb];W[ic]))"
            + "(;B[gc];W[ec];B[eb];W[fb];B[db];W[hc];B[gb];W[gd];B[hb]))"
            + "(;W[nq];B[cn];W[fp];B[gp];W[fo];B[dq];W[cq];B[eq];W[cp];B[dm];W[fm]))";

    int variationNum = 4;
    String mainBranch =
        ";B[pd];W[dp];B[pp];W[dd];B[fq];W[cn];B[cc];W[cd];B[dc];W[ed];B[fc];W[fd];B[gb];W[hc];B[nq]";
    String variation1 = ";W[gc];B[ec];W[hc];B[hb];W[ic]";
    String variation2 = ";B[gc];W[ec];B[eb];W[fb];B[db];W[hc];B[gb];W[gd];B[hb]";
    String variation3 = ";W[nq];B[cn];W[fp];B[gp];W[fo];B[dq];W[cq];B[eq];W[cp];B[dm];W[fm]";

    // Load correctly
    boolean loaded = SGFParser.loadFromString(sgfString);
    assertTrue(loaded);

    // Variations
    List<String> moveList = new ArrayList<String>();
    Util.getVariationTree(moveList, 0, lizzie.board.getHistory().getCurrentHistoryNode(), 0, true);

    assertEquals(moveList.size(), variationNum);
    assertEquals(moveList.get(0), mainBranch);
    assertEquals(moveList.get(1), variation1);
    assertEquals(moveList.get(2), variation2);
    assertEquals(moveList.get(3), variation3);

    // Save correctly
    String saveSgf = SGFParser.saveToString();
    assertTrue(saveSgf.trim().length() > 0);

    assertEquals(sgfString, Util.trimGameInfo(saveSgf));
  }

  public void testFull1() throws IOException {

    String sgfInfo = "(;CA[utf8]AP[MultiGo:4.4.4]SZ[19]";
    String sgfAwAb =
        "AB[pe][pq][oq][nq][mq][cp][dq][eq][fp]AB[qd]AW[dc][cf][oc][qo][op][np][mp][ep][fq]";
    String sgfContent =
        ";W[lp]C[25th question Overall view Black first Superior    White 1 has a long hand. The first requirement in the layout phase is to have a big picture.    What is the next black point in this situation?]"
            + "(;B[qi]C[Correct Answer Limiting the thickness    Black 1 is broken. The reason why Black is under the command of four hands is to win the first hand and occupy the black one.    That is to say, on the lower side, the bigger one is the right side. Black 1 is both good and bad, and it limits the development of white and thick. It is good chess. Black 1 is appropriate, and it will not work if you go all the way or take a break.];W[lq];B[rp]C[1 Figure (turning head value?)    After black 1 , white is like 2 songs, then it is not too late to fly black again. There is a saying that \"the head is worth a thousand dollars\" in the chessboard, but in the situation of this picture, the white song has no such value.    Because after the next white A, black B, white must be on the lower side to be complete. It can be seen that for Black, the meaning of playing chess below is also not significant.    The following is a gesture that has come to an end. Both sides have no need to rush to settle down here.])"
            + "(;B[kq];W[pi]C[2 diagram (failure)    Black 1 jump failed. The reason is not difficult to understand from the above analysis. If Black wants to jump out, he shouldn’t have four hands in the first place. By the white 2 on the right side of the hand, it immediately constitutes a strong appearance, black is not good. Although the black got some fixed ground below, but the position was too low, and it became a condensate, black is not worth the candle. ]))";
    String sgfString = sgfInfo + sgfAwAb + sgfContent;

    int variationNum = 2;
    String mainBranch =
        ";W[lp]C[25th question Overall view Black first Superior    White 1 has a long hand. The first requirement in the layout phase is to have a big picture.    What is the next black point in this situation?];B[qi]C[Correct Answer Limiting the thickness    Black 1 is broken. The reason why Black is under the command of four hands is to win the first hand and occupy the black one.    That is to say, on the lower side, the bigger one is the right side. Black 1 is both good and bad, and it limits the development of white and thick. It is good chess. Black 1 is appropriate, and it will not work if you go all the way or take a break.];W[lq];B[rp]C[1 Figure (turning head value?)    After black 1 , white is like 2 songs, then it is not too late to fly black again. There is a saying that \"the head is worth a thousand dollars\" in the chessboard, but in the situation of this picture, the white song has no such value.    Because after the next white A, black B, white must be on the lower side to be complete. It can be seen that for Black, the meaning of playing chess below is also not significant.    The following is a gesture that has come to an end. Both sides have no need to rush to settle down here.]";
    String variation1 =
        ";B[kq];W[pi]C[2 diagram (failure)    Black 1 jump failed. The reason is not difficult to understand from the above analysis. If Black wants to jump out, he shouldn’t have four hands in the first place. By the white 2 on the right side of the hand, it immediately constitutes a strong appearance, black is not good. Although the black got some fixed ground below, but the position was too low, and it became a condensate, black is not worth the candle. ]";

    Stone[] expectStones = Util.convertStones(sgfAwAb);

    // Load correctly
    boolean loaded = SGFParser.loadFromString(sgfString);
    assertTrue(loaded);

    // Variations
    List<String> moveList = new ArrayList<String>();
    Util.getVariationTree(moveList, 0, lizzie.board.getHistory().getCurrentHistoryNode(), 0, true);

    assertEquals(moveList.size(), variationNum);
    assertEquals(moveList.get(0), mainBranch);
    assertEquals(moveList.get(1), variation1);

    // AW/AB
    assertArrayEquals(expectStones, Lizzie.board.getHistory().getStones());

    // Save correctly
    String saveSgf = SGFParser.saveToString();
    assertTrue(saveSgf.trim().length() > 0);

    String sgf = Util.trimGameInfo(saveSgf);
    String[] ret = Util.splitAwAbSgf(sgf);
    Stone[] actualStones = Util.convertStones(ret[0]);

    // AW/AB
    assertArrayEquals(expectStones, actualStones);

    // Content
    assertEquals("(" + sgfContent, ret[1]);
  }

  public void testMore1() throws IOException {

    String sgfInfo = "(;CA[gb2312]AP[MultiGo:4.4.4]SZ[19]EV[Question 1 Ko]US[new]CP[newnet]";
    String headComment = "C[Question 1\r\n" + "Black first]";
    String sgfAwAb =
        "AB[pc][pd][pe][nd][qf][rf][pg][ph][rh][sc][sb][rb]AW[sd][qd][qc][qe][re][rc][pf][of][ng][qg][qh][pi][pj][qj][ri]AB[qb]";
    String sgfContent =
        "(;B[sg]N[Correct answer Retired tiger]C[Correct answer back tiger](;W[qi];B[se]N[ 1 figure (ko)]C[1 figure (ko)])(;W[se]MN[2];B[sf]LB[rg:A][qi:B]N[5 figure (difference)]C[5 figure (difference)]))(;B[sf];W[rg]N[2 figure (failure)]C[2 figure (failure)])(;B[rg];W[qi]N[3 figure (bad move)]C[3 figure (bad move)];B[sg]MN[1];W[se]N[4 figure (not liberty)]C[4 figure (not liberty)]))";
    String sgfString = sgfInfo + sgfAwAb + headComment + sgfContent;

    int variationNum = 4;
    String mainBranch =
        ";B[sg]C[Correct answer back tiger]N[Correct answer Retired tiger];W[qi];B[se]C[1 figure (ko)]N[ 1 figure (ko)]";
    String variation1 =
        ";W[se]MN[2];B[sf]C[5 figure (difference)]LB[rg:A][qi:B]N[5 figure (difference)]";
    String variation2 = ";B[sf];W[rg]C[2 figure (failure)]N[2 figure (failure)]";
    String variation3 =
        ";B[rg];W[qi]C[3 figure (bad move)]N[3 figure (bad move)];B[sg]MN[1];W[se]C[4 figure (not liberty)]N[4 figure (not liberty)]";

    Stone[] expectStones = Util.convertStones(sgfAwAb);

    // Load correctly
    boolean loaded = SGFParser.loadFromString(sgfString);
    assertTrue(loaded);

    // Variations
    List<String> moveList = new ArrayList<String>();
    Util.getVariationTree(moveList, 0, lizzie.board.getHistory().getCurrentHistoryNode(), 0, true);

    assertEquals(moveList.size(), variationNum);
    assertEquals(moveList.get(0), mainBranch);
    assertEquals(moveList.get(1), variation1);
    assertEquals(moveList.get(2), variation2);
    assertEquals(moveList.get(3), variation3);

    // AW/AB
    assertArrayEquals(expectStones, Lizzie.board.getHistory().getStones());

    // MN[2] in variation 2
    Lizzie.board.nextMove();
    Lizzie.board.nextMove();
    Lizzie.board.nextBranch();
    assertEquals("2", Lizzie.board.getData().getProperty("MN"));
    assertEquals(2, Lizzie.board.getData().moveNumber);

    // LB[rg:A][qi:B] in variation 2
    Lizzie.board.nextMove();
    assertEquals("rg:A,qi:B", Lizzie.board.getData().getProperty("LB"));

    // Save correctly
    String saveSgf = SGFParser.saveToString();
    assertTrue(saveSgf.trim().length() > 0);

    String sgf = Util.trimGameInfo(saveSgf);
    String[] ret = Util.splitAwAbSgf(sgf);
    Stone[] actualStones = Util.convertStones(ret[0]);

    // AW/AB
    assertArrayEquals(expectStones, actualStones);

    // Content
    assertEquals("(" + headComment + sgfContent, ret[1]);
  }
}
