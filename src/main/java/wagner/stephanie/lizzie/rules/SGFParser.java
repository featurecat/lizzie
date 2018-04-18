package wagner.stephanie.lizzie.rules;

import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.analysis.GameInfo;

import java.io.*;
import java.text.SimpleDateFormat;

public class SGFParser {
    private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static boolean load(String filename) throws IOException {
        // Clear the board
        Lizzie.board.clear();

        File file = new File(filename);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        FileInputStream fp = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fp);
        StringBuilder builder = new StringBuilder();
        while (reader.ready()) {
            builder.append((char) reader.read());
        }
        String value = builder.toString();
        if (value.isEmpty()) {
            return false;
        }
        reader.close();
        fp.close();
        return parse(value);
    }

    public static int[] convertSgfPosToCoord(String pos) {
        if (pos.equals("tt") || pos.isEmpty())
            return null;
        int[] ret = new int[2];
        ret[0] = (int) pos.charAt(0) - 'a';
        ret[1] = (int) pos.charAt(1) - 'a';
        return ret;
    }

    private static boolean parse(String value) {
        value = value.trim();
        if (value.charAt(0) != '(') {
            return false;
        } else if (value.charAt(value.length() - 1) != ')') {
            return false;
        }
        int subTreeDepth = 0;
        boolean inTag = false, isMultiGo = false;
        String tag = null;
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (value.charAt(value.length() - 2) == ')') {
            isMultiGo = true;
        }

        for (byte b : value.getBytes()) {
            // Check unicode charactors (UTF-8)
            char c = (char) b;
            if (((int)b & 0x80) != 0) {
                continue;
            }
            switch (c) {
                case '(':
                    if (!inTag) {
                        subTreeDepth += 1;
                    }
                    break;
                case ')':
                    if (!inTag) {
                        subTreeDepth -= 1;
                        if (isMultiGo) {
                            return true;
                        }
                    }
                    break;
                case '[':
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    inTag = true;
                    String tagTemp = tagBuilder.toString();
                    if (!tagTemp.isEmpty()) {
                        tag = tagTemp;
                    }
                    tagContentBuilder = new StringBuilder();
                    break;
                case ']':
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    inTag = false;
                    tagBuilder = new StringBuilder();
                    String tagContent = tagContentBuilder.toString();
                    // We got tag, we can parse this tag now.
                    if (tag.equals("B")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Lizzie.board.pass(Stone.BLACK);
                        } else {
                            Lizzie.board.place(move[0], move[1], Stone.BLACK);
                        }
                    } else if (tag.equals("W")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Lizzie.board.pass(Stone.WHITE);
                        } else {
                            Lizzie.board.place(move[0], move[1], Stone.WHITE);
                        }
                    }
                    break;
                case ';':
                    break;
                default:
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    if (inTag) {
                        tagContentBuilder.append(c);
                    } else {
                        if (c != '\n' && c != '\r' && c != '\t' && c != ' ') {
                            tagBuilder.append(c);
                        }
                    }
            }
        }

        // Rewind to game start
        while (Lizzie.board.previousMove());

        return true;
    }

    public static boolean save(Board board, String filename) throws IOException {
        FileOutputStream fp = new FileOutputStream(filename);
        OutputStreamWriter writer = new OutputStreamWriter(fp);

        try
        {
            // collect game info
            BoardHistoryList history = board.getHistory();
            GameInfo gameInfo = history.getGameInfo();
            String playerBlack = gameInfo.getPlayerBlack();
            String playerWhite = gameInfo.getPlayerWhite();
            Double komi = gameInfo.getKomi();
            Integer handicap = gameInfo.getHandicap();
            String date = SGF_DATE_FORMAT.format(gameInfo.getDate());

            // add SGF header
            StringBuilder builder = new StringBuilder("(;");
            if (handicap != 0) builder.append(String.format("HA[%s]", handicap));
            builder.append(String.format("KM[%s]PW[%s]PB[%s]DT[%s]AP[Lizzie: %s]",
                    komi, playerWhite, playerBlack, date, Lizzie.lizzieVersion));

            // move to the first move
            history.toStart();

            // add handicap stones to SGF
            if (handicap != 0)
            {
                builder.append("AB");
                Stone[] stones = history.getStones();
                for (int i = 0; i < stones.length; i++) {
                    Stone stone = stones[i];
                    if (stone.isBlack())
                    {
                        // i = x * Board.BOARD_SIZE + y;
                        int corY = i % Board.BOARD_SIZE;
                        int corX = (i - corY) / Board.BOARD_SIZE;

                        char x = (char)(corX + 'a');
                        char y = (char)(corY + 'a');
                        builder.append(String.format("[%c%c]", x, y));
                    }
                }
            }

            // replay moves, and convert them to tags.
            // *  format: ";B[xy]" or ";W[xy]"
            // *  with 'xy' = coordinates ; or 'tt' for pass.
            BoardData data;
            while ((data = history.next()) != null) {

                String stone;
                if (Stone.BLACK.equals(data.lastMoveColor)) stone = "B";
                else if (Stone.WHITE.equals(data.lastMoveColor)) stone = "W";
                else continue;

                char x = data.lastMove == null ? 't' : (char) (data.lastMove[0] + 'a');
                char y = data.lastMove == null ? 't' : (char) (data.lastMove[1] + 'a');

                builder.append(String.format(";%s[%c%c]", stone, x, y));
            }

            // close file
            builder.append(')');
            writer.append(builder.toString());
        }
        finally
        {
            writer.close();
            fp.close();
        }
        return true;
    }
}