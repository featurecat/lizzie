package common;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.SGFParser;
import featurecat.lizzie.rules.Stone;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

  private static ArrayList<Integer> laneUsageList = new ArrayList<Integer>();

  /**
   * Get Variation Tree as String List The logic is same as the function VariationTree.drawTree
   *
   * @param startLane
   * @param startNode
   * @param variationNumber
   * @param isMain
   */
  public static void getVariationTree(
      List<String> moveList,
      int startLane,
      BoardHistoryNode startNode,
      int variationNumber,
      boolean isMain) {
    // Finds depth on leftmost variation of this tree
    int depth = BoardHistoryList.getDepth(startNode) + 1;
    int lane = startLane;
    // Figures out how far out too the right (which lane) we have to go not to collide with other
    // variations
    while (lane < laneUsageList.size()
        && laneUsageList.get(lane) <= startNode.getData().moveNumber + depth) {
      // laneUsageList keeps a list of how far down it is to a variation in the different "lanes"
      laneUsageList.set(lane, startNode.getData().moveNumber - 1);
      lane++;
    }
    if (lane >= laneUsageList.size()) {
      laneUsageList.add(0);
    }
    if (variationNumber > 1) laneUsageList.set(lane - 1, startNode.getData().moveNumber - 1);
    laneUsageList.set(lane, startNode.getData().moveNumber);

    // At this point, lane contains the lane we should use (the main branch is in lane 0)
    BoardHistoryNode cur = startNode;

    // Draw main line
    StringBuilder sb = new StringBuilder();
    sb.append(formatMove(cur.getData()));
    while (cur.next() != null) {
      cur = cur.next();
      sb.append(formatMove(cur.getData()));
    }
    moveList.add(sb.toString());
    // Now we have drawn all the nodes in this variation, and has reached the bottom of this
    // variation
    // Move back up, and for each, draw any variations we find
    while (cur.previous() != null && cur != startNode) {
      cur = cur.previous();
      int curwidth = lane;
      // Draw each variation, uses recursion
      for (int i = 1; i < cur.numberOfChildren(); i++) {
        curwidth++;
        // Recursion, depth of recursion will normally not be very deep (one recursion level for
        // every variation that has a variation (sort of))
        getVariationTree(moveList, curwidth, cur.getVariation(i), i, false);
      }
    }
  }

  private static String formatMove(BoardData data) {
    String stone = "";
    if (Stone.BLACK.equals(data.lastMoveColor)) stone = "B";
    else if (Stone.WHITE.equals(data.lastMoveColor)) stone = "W";
    else return stone;

    char x = data.lastMove == null ? 't' : (char) (data.lastMove[0] + 'a');
    char y = data.lastMove == null ? 't' : (char) (data.lastMove[1] + 'a');

    String comment = "";
    if (data.comment != null && data.comment.trim().length() > 0) {
      comment = String.format("C[%s]", data.comment);
    }
    return String.format(";%s[%c%c]%s", stone, x, y, comment);
  }

  public static String trimGameInfo(String sgf) {
    String gameInfo = String.format("(?s).*AP\\[Lizzie: %s\\]", Lizzie.lizzieVersion);
    return sgf.replaceFirst(gameInfo, "(");
  }

  public static String[] splitAwAbSgf(String sgf) {
    String[] ret = new String[2];
    String regex = "(A[BW]{1}(\\[[a-z]{2}\\])+)";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(sgf);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      sb.append(matcher.group(0));
    }
    ret[0] = sb.toString();
    ret[1] = sgf.replaceAll(regex, "");
    return ret;
  }

  public static Stone[] convertStones(String awAb) {
    Stone[] stones = new Stone[Board.BOARD_SIZE * Board.BOARD_SIZE];
    for (int i = 0; i < stones.length; i++) {
      stones[i] = Stone.EMPTY;
    }
    String regex = "(A[BW]{1})|(?<=\\[)([a-z]{2})(?=\\])";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(awAb);
    StringBuilder sb = new StringBuilder();
    Stone stone = Stone.EMPTY;
    while (matcher.find()) {
      String str = matcher.group(0);
      if ("AB".equals(str)) {
        stone = Stone.BLACK;
      } else if ("AW".equals(str)) {
        stone = Stone.WHITE;
      } else {
        int[] move = SGFParser.convertSgfPosToCoord(str);
        int index = Board.getIndex(move[0], move[1]);
        stones[index] = stone;
      }
    }
    return stones;
  }
}
