package featurecat.lizzie.util;

import static java.lang.Math.round;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.rules.BoardData;
import featurecat.lizzie.rules.BoardHistoryNode;
import java.awt.Color;
import java.awt.FontMetrics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JTextField;

public class Utils {

  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * @return a shorter, rounded string version of playouts. e.g. 345 -> 345, 1265 -> 1.3k, 44556 ->
   *     45k, 133523 -> 134k, 1234567 -> 1.2m
   */
  public static String getPlayoutsString(int playouts) {
    if (playouts >= 1_000_000) {
      double playoutsDouble = (double) playouts / 100_000; // 1234567 -> 12.34567
      return round(playoutsDouble) / 10.0 + "m";
    } else if (playouts >= 10_000) {
      double playoutsDouble = (double) playouts / 1_000; // 13265 -> 13.265
      return round(playoutsDouble) + "k";
    } else if (playouts >= 1_000) {
      double playoutsDouble = (double) playouts / 100; // 1265 -> 12.65
      return round(playoutsDouble) / 10.0 + "k";
    } else {
      return String.valueOf(playouts);
    }
  }

  /**
   * Truncate text that is too long for the given width
   *
   * @param line
   * @param fm
   * @param fitWidth
   * @return fitted
   */
  public static String truncateStringByWidth(String line, FontMetrics fm, int fitWidth) {
    if (line.isEmpty()) {
      return "";
    }
    int width = fm.stringWidth(line);
    if (width > fitWidth) {
      int guess = line.length() * fitWidth / width;
      String before = line.substring(0, guess).trim();
      width = fm.stringWidth(before);
      if (width > fitWidth) {
        int diff = width - fitWidth;
        int i = 0;
        for (; (diff > 0 && i < 5); i++) {
          diff = diff - fm.stringWidth(line.substring(guess - i - 1, guess - i));
        }
        return line.substring(0, guess - i).trim();
      } else {
        return before;
      }
    } else {
      return line;
    }
  }

  public static double lastWinrateDiff(BoardHistoryNode node) {

    // Last winrate
    Optional<BoardData> lastNode = node.previous().flatMap(n -> Optional.of(n.getData()));
    boolean validLastWinrate = lastNode.map(d -> d.getPlayouts() > 0).orElse(false);
    double lastWR = validLastWinrate ? lastNode.get().winrate : 50;

    // Current winrate
    BoardData data = node.getData();
    boolean validWinrate = false;
    double curWR = 50;
    if (data == Lizzie.board.getHistory().getData()) {
      Leelaz.WinrateStats stats = Lizzie.leelaz.getWinrateStats();
      curWR = stats.maxWinrate;
      validWinrate = (stats.totalPlayouts > 0);
      if (Lizzie.frame.isPlayingAgainstLeelaz
          && Lizzie.frame.playerIsBlack == !Lizzie.board.getHistory().getData().blackToPlay) {
        validWinrate = false;
      }
    } else {
      validWinrate = (data.getPlayouts() > 0);
      curWR = validWinrate ? data.winrate : 100 - lastWR;
    }

    // Last move difference winrate
    if (validLastWinrate && validWinrate) {
      return 100 - lastWR - curWR;
    } else {
      return 0;
    }
  }

  public static Color getBlunderNodeColor(BoardHistoryNode node) {
    if (Lizzie.config.nodeColorMode == 1 && node.getData().blackToPlay
        || Lizzie.config.nodeColorMode == 2 && !node.getData().blackToPlay) {
      return Color.WHITE;
    }
    double diffWinrate = lastWinrateDiff(node);
    Optional<Double> st =
        diffWinrate >= 0
            ? Lizzie.config.blunderWinrateThresholds.flatMap(
                l -> l.stream().filter(t -> (t > 0 && t <= diffWinrate)).reduce((f, s) -> s))
            : Lizzie.config.blunderWinrateThresholds.flatMap(
                l -> l.stream().filter(t -> (t < 0 && t >= diffWinrate)).reduce((f, s) -> f));
    if (st.isPresent()) {
      return Lizzie.config.blunderNodeColors.map(m -> m.get(st.get())).get();
    } else {
      return Color.WHITE;
    }
  }

  public static Integer txtFieldValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()
        || txt.getText().trim().length() >= String.valueOf(Integer.MAX_VALUE).length()) {
      return 0;
    } else {
      return Integer.parseInt(txt.getText().trim());
    }
  }

  public static Double txtFieldDoubleValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()) {
      return 0.0;
    } else {
      return new Double(txt.getText().trim());
    }
  }

  public static int intOfMap(Map map, String key) {
    if (map == null) {
      return 0;
    }
    List s = (List<String>) map.get(key);
    if (s == null || s.size() <= 0) {
      return 0;
    }
    try {
      return Integer.parseInt((String) s.get(0));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static String stringOfMap(Map map, String key) {
    if (map == null) {
      return "";
    }
    List s = (List<String>) map.get(key);
    if (s == null || s.size() <= 0) {
      return "";
    }
    try {
      return (String) s.get(0);
    } catch (NumberFormatException e) {
      return "";
    }
  }
}
