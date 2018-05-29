package featurecat.lizzie.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.plugin.PluginManager;

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
        reader.close();
        fp.close();
        String value = builder.toString();
        if (value.isEmpty()) {
            return false;
        }

        boolean returnValue = parse(value);
        PluginManager.onSgfLoaded();
        return returnValue;
    }

    public static boolean loadFromString(String sgfString) {
        // Clear the board
        Lizzie.board.clear();

        return parse(sgfString);
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
        // Drop anything outside "(;...)"
        final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;.*\\)).*?");
        Matcher sgfMatcher = SGF_PATTERN.matcher(value);
        if (sgfMatcher.matches()) {
            value = sgfMatcher.group(1);
        } else {
            return false;
        }
        int subTreeDepth = 0;
        boolean inTag = false, isMultiGo = false, escaping = false;
        String tag = null;
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (value.charAt(value.length() - 2) == ')') {
            isMultiGo = true;
        }

        String blackPlayer = "", whitePlayer = "";

        PARSE_LOOP:
        for (byte b : value.getBytes()) {
            // Check unicode charactors (UTF-8)
            char c = (char) b;
            if (((int) b & 0x80) != 0) {
                continue;
            }
            if (escaping) {
                // Any char following "\" is inserted verbatim
                // (ref) "3.2. Text" in https://www.red-bean.com/sgf/sgf4.html
                tagContentBuilder.append(c);
                escaping = false;
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
                            break PARSE_LOOP;
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
                        // Ignore small letters in tags for the long format Smart-Go file.
                        // (ex) "PlayerBlack" ==> "PB"
                        // It is the default format of mgt, an old SGF tool.
                        // (Mgt is still supported in Debian and Ubuntu.)
                        tag = tagTemp.replaceAll("[a-z]", "");
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
                    } else if (tag.equals("AB")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Lizzie.board.pass(Stone.BLACK);
                        } else {
                            Lizzie.board.place(move[0], move[1], Stone.BLACK);
                        }
                        Lizzie.board.flatten();
                    } else if (tag.equals("AW")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Lizzie.board.pass(Stone.WHITE);
                        } else {
                            Lizzie.board.place(move[0], move[1], Stone.WHITE);
                        }
                        Lizzie.board.flatten();
                    } else if (tag.equals("PB")) {
                        blackPlayer = tagContent;
                    } else if (tag.equals("PW")) {
                        whitePlayer = tagContent;
                    }  else if (tag.equals("KM")) {
                        Lizzie.board.getHistory().getGameInfo().setKomi(Double.parseDouble(tagContent));
                    }
                    break;
                case ';':
                    break;
                default:
                    if (subTreeDepth > 1 && !isMultiGo) {
                        break;
                    }
                    if (inTag) {
                        if (c == '\\') {
                            escaping = true;
                            continue;
                        }
                        tagContentBuilder.append(c);
                    } else {
                        if (c != '\n' && c != '\r' && c != '\t' && c != ' ') {
                            tagBuilder.append(c);
                        }
                    }
            }
        }

        Lizzie.frame.setPlayers(whitePlayer, blackPlayer);

        // Rewind to game start
        while (Lizzie.board.previousMove()) ;

        return true;
    }

    public static String saveToString() throws IOException {
        try (StringWriter writer = new StringWriter()) {
            saveToStream(Lizzie.board, writer);
            return writer.toString();
        }
    }

    public static void save(Board board, String filename) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filename))) {
            saveToStream(board, writer);
        }
    }

    private static void saveToStream(Board board, Writer writer) throws IOException {
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
        if (handicap != 0) {
            builder.append("AB");
            Stone[] stones = history.getStones();
            for (int i = 0; i < stones.length; i++) {
                Stone stone = stones[i];
                if (stone.isBlack()) {
                    // i = x * Board.BOARD_SIZE + y;
                    int corY = i % Board.BOARD_SIZE;
                    int corX = (i - corY) / Board.BOARD_SIZE;

                    char x = (char) (corX + 'a');
                    char y = (char) (corY + 'a');
                    builder.append(String.format("[%c%c]", x, y));
                }
            }
        }

        // replay moves, and convert them to tags.
        // *  format: ";B[xy]" or ";W[xy]"
        // *  with 'xy' = coordinates ; or 'tt' for pass.
        BoardData data;

        // TODO: this code comes from cngoodboy's plugin PR #65. It looks like it might be useful for handling
        //       AB/AW commands for sgfs in general -- we can extend it beyond just handicap. TODO integrate it
//        data = history.getData();
//
//        // For handicap
//        ArrayList<int[]> abList = new ArrayList<int[]>();
//        ArrayList<int[]> awList = new ArrayList<int[]>();
//
//        for (int i = 0; i < Board.BOARD_SIZE; i++) {
//            for (int j = 0; j < Board.BOARD_SIZE; j++) {
//                switch (data.stones[Board.getIndex(i, j)]) {
//                    case BLACK:
//                        abList.add(new int[]{i, j});
//                        break;
//                    case WHITE:
//                        awList.add(new int[]{i, j});
//                        break;
//                    default:
//                        break;
//                }
//            }
//        }
//
//        if (!abList.isEmpty()) {
//            builder.append(";AB");
//            for (int i = 0; i < abList.size(); i++) {
//                builder.append(String.format("[%s]", convertCoordToSgfPos(abList.get(i))));
//            }
//        }
//
//        if (!awList.isEmpty()) {
//            builder.append(";AW");
//            for (int i = 0; i < awList.size(); i++) {
//                builder.append(String.format("[%s]", convertCoordToSgfPos(awList.get(i))));
//            }
//        }

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
}
