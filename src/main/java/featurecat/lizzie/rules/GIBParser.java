package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.util.EncodingDetector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class GIBParser {
  private static int[][] handicapPlacement = {
    {3, 15}, {15, 3}, {15, 15}, {3, 3}, {3, 9}, {15, 9}, {9, 3}, {9, 15}, {9, 9}
  };

  public static boolean load(String filename) throws IOException {
    // Clear the board
    Lizzie.board.clear();

    File file = new File(filename);
    if (!file.exists() || !file.canRead()) {
      return false;
    }

    String encoding = EncodingDetector.detect(filename);
    FileInputStream fp = new FileInputStream(file);
    InputStreamReader reader = new InputStreamReader(fp, encoding);
    StringBuilder builder = new StringBuilder();
    while (reader.ready()) {
      builder.append((char) reader.read());
    }
    reader.close();
    fp.close();
    String value = builder.toString();
    if (value.isEmpty()) {
      return false;
    }

    boolean returnValue = parse(value);
    return returnValue;
  }

  private static void placeHandicap(int handi) {
    if (handi > 9) {
      System.out.println("More than 9 in handicap not supported!");
      handi = 9;
    }
    if (handi == 5 || handi == 7) {
      Lizzie.board.place(9, 9, Stone.BLACK);
      handi--;
    }
    for (int i = 0; i < handi; i++) {
      Lizzie.board.place(handicapPlacement[i][0], handicapPlacement[i][1], Stone.BLACK);
    }
  }

  private static boolean parse(String value) {
    String[] lines = value.trim().split("\n");
    String whitePlayer = "Player 1";
    String blackPlayer = "Player 2";
    double komi = 1.5;
    int handicap = 0;

    for (String line : lines) {
      if (line.startsWith("\\[GAMEINFOMAIN=")) {
        // See if komi is included
        int sk = line.indexOf("GONGJE:") + 7;
        int ek = line.indexOf(',', sk);

        if (sk > 0) {
          komi = Integer.parseInt(line.substring(sk, ek)) / 10.0;
        }
      }
      // Players names
      if (line.startsWith("\\[GAMEBLACKNAME=")) {
        blackPlayer = line.substring(16, line.length() - 3);
      }
      if (line.startsWith("\\[GAMEWHITENAME=")) {
        whitePlayer = line.substring(16, line.length() - 3);
      }
      // Handicap info
      if (line.startsWith("INI")) {
        String[] fields = line.split(" ");
        handicap = Integer.parseInt(fields[3]);
        if (handicap >= 2) {
          placeHandicap(handicap);
        }
      }
      // Actual moves
      if (line.startsWith("STO")) {
        String[] fields = line.split(" ");
        int x = Integer.parseInt(fields[4]);
        int y = Integer.parseInt(fields[5]);
        Stone s = fields[3].equals("1") ? Stone.BLACK : Stone.WHITE;
        Lizzie.board.place(x, y, s);
      }
      // Pass
      if (line.startsWith("SKI")) {
        Lizzie.board.pass();
      }
    }
    Lizzie.board.getHistory().getGameInfo().setKomi(komi);
    Lizzie.frame.setPlayers(whitePlayer, blackPlayer);
    // Rewind to game start
    while (Lizzie.board.previousMove()) ;

    return false;
  }
}
