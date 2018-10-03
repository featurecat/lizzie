package featurecat.lizzie.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.plugin.PluginManager;

import java.io.*;
import java.text.SimpleDateFormat;

public class SGFParser {
    private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String[] listProps = new String[] { "LB", "CR", "SQ", "MA", "TR", "AB", "AW", "AE"};
    private static final String[] markupProps = new String[] { "LB", "CR", "SQ", "MA", "TR"};

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

        // Determine the SZ property
        Pattern szPattern = Pattern.compile("(?s).*?SZ\\[(\\d+)\\](?s).*");
        Matcher szMatcher = szPattern.matcher(value);
        if (szMatcher.matches()) {
            Lizzie.board.reopen(Integer.parseInt(szMatcher.group(1)));
        } else {
            Lizzie.board.reopen(19);
        }

        int subTreeDepth = 0;
        // Save the variation step count
        Map<Integer, Integer> subTreeStepMap = new HashMap<Integer, Integer>();
        // Comment of the AW/AB (Add White/Add Black) stone
        String awabComment = null;
        // Game properties
        Map<String, String> gameProperties = new HashMap<String, String>();
        boolean inTag = false, isMultiGo = false, escaping = false, moveStart = false, addPassForAwAb = true;
        String tag = null;
        StringBuilder tagBuilder = new StringBuilder();
        StringBuilder tagContentBuilder = new StringBuilder();
        // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
        // Other 's branch: (Main Branch (Branch) Main Branch)
        if (value.matches("(?s).*\\)\\s*\\)")) {
            isMultiGo = true;
        }

        String blackPlayer = "", whitePlayer = "";

        // Support unicode characters (UTF-8)
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
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
                        // Initialize the step count
                        subTreeStepMap.put(subTreeDepth, 0);
                    } else {
                        if (i > 0) {
                            // Allow the comment tag includes '('
                            tagContentBuilder.append(c);
                        }
                    }
                    break;
                case ')':
                    if (!inTag) {
                        if (isMultiGo) {
                            // Restore to the variation node
                            int varStep = subTreeStepMap.get(subTreeDepth);
                            for (int s = 0; s < varStep; s++) {
                                Lizzie.board.previousMove();
                            }
                        }
                        subTreeDepth -= 1;
                    } else {
                        // Allow the comment tag includes '('
                        tagContentBuilder.append(c);
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
                    if (tag.equals("B") || tag.equals("W")) {
                        moveStart = true;
                        addPassForAwAb = true;
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (move == null) {
                            Lizzie.board.pass(tag.equals("B") ? Stone.BLACK : Stone.WHITE);
                        } else {
                            // Save the step count
                            subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                            Lizzie.board.place(move[0], move[1], tag.equals("B") ? Stone.BLACK : Stone.WHITE);
                        }
                    } else if (tag.equals("C")) {
                        // Support comment
                        if (!moveStart) {
                            awabComment = tagContent;
                        } else {
                            Lizzie.board.comment(tagContent);
                        }
                    } else if (tag.equals("AB") || tag.equals("AW")) {
                        int[] move = convertSgfPosToCoord(tagContent);
                        if (moveStart) {
                            // add to node properties
                            Lizzie.board.addNodeProperty(tag, tagContent);
                            if (addPassForAwAb) {
                                Lizzie.board.pass(tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                                addPassForAwAb = false;
                            }
                            if (move != null) {
                                Lizzie.board.addStone(move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                            }
                        } else {
                            if (move == null) {
                                Lizzie.board.pass(tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                            } else {
                                Lizzie.board.place(move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                            }
                            Lizzie.board.flatten();
                        }
                    } else if (tag.equals("PB")) {
                        blackPlayer = tagContent;
                    } else if (tag.equals("PW")) {
                        whitePlayer = tagContent;
                    }  else if (tag.equals("KM")) {
                        try {
                            if (tagContent.trim().isEmpty()) {
                                tagContent = "0.0";
                            }
                            Lizzie.board.getHistory().getGameInfo().setKomi(Double.parseDouble(tagContent));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (moveStart) {
                            // Other SGF node properties
                            Lizzie.board.addNodeProperty(tag, tagContent);
                            if ("MN".equals(tag)) {
                                Lizzie.board.moveNumber(Integer.parseInt(tagContent));
                            } else if ("AE".equals(tag)) {
                                // remove a stone
                                if (addPassForAwAb) {
                                    Lizzie.board.pass(tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                                    addPassForAwAb = false;
                                }
                                int[] move = convertSgfPosToCoord(tagContent);
                                if (move != null) {
                                    Lizzie.board.removeStone(move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                                }
                            }
                        } else {
                            addProperty(gameProperties, tag, tagContent);
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

        // Set AW/AB Comment
        if (awabComment != null) {
            Lizzie.board.comment(awabComment);
        }
        if (gameProperties.size() > 0) {
            Lizzie.board.addNodeProperties(gameProperties);
        }

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
        BoardHistoryList history = board.getHistory().shallowCopy();
        GameInfo gameInfo = history.getGameInfo();
        String playerBlack = gameInfo.getPlayerBlack();
        String playerWhite = gameInfo.getPlayerWhite();
        Double komi = gameInfo.getKomi();
        Integer handicap = gameInfo.getHandicap();
        String date = SGF_DATE_FORMAT.format(gameInfo.getDate());

        // add SGF header
        StringBuilder builder = new StringBuilder("(;");
        StringBuilder generalProps = new StringBuilder("");
        if (handicap != 0) generalProps.append(String.format("HA[%s]", handicap));
        generalProps.append(String.format("KM[%s]PW[%s]PB[%s]DT[%s]AP[Lizzie: %s]",
                komi, playerWhite, playerBlack, date, Lizzie.lizzieVersion));

        // move to the first move
        history.toStart();

        // Game properties
        history.getData().addProperties(generalProps.toString());
        builder.append(history.getData().propertiesString());

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
        } else {
            // Process the AW/AB stone
            Stone[] stones = history.getStones();
            StringBuilder abStone = new StringBuilder();
            StringBuilder awStone = new StringBuilder();
            for (int i = 0; i < stones.length; i++) {
                Stone stone = stones[i];
                if (stone.isBlack() || stone.isWhite()) {
                    // i = x * Board.BOARD_SIZE + y;
                    int corY = i % Board.BOARD_SIZE;
                    int corX = (i - corY) / Board.BOARD_SIZE;

                    char x = (char) (corX + 'a');
                    char y = (char) (corY + 'a');

                    if (stone.isBlack()) {
                        abStone.append(String.format("[%c%c]", x, y));
                    } else {
                        awStone.append(String.format("[%c%c]", x, y));
                    }
                }
            }
            if (abStone.length() > 0) {
                builder.append("AB").append(abStone);
            }
            if (awStone.length() > 0) {
                builder.append("AW").append(awStone);
            }
        }

        // The AW/AB Comment
        if (history.getData().comment != null) {
            builder.append(String.format("C[%s]", history.getData().comment));
        }

        // replay moves, and convert them to tags.
        // *  format: ";B[xy]" or ";W[xy]"
        // *  with 'xy' = coordinates ; or 'tt' for pass.

        // Write variation tree
        builder.append(generateNode(board, history.getCurrentHistoryNode()));

        // close file
        builder.append(')');
        writer.append(builder.toString());
    }

    /**
     * Generate node with variations
     */
    private static String generateNode(Board board, BoardHistoryNode node) throws IOException {
        StringBuilder builder = new StringBuilder("");

        if (node != null) {

            BoardData data = node.getData();
            String stone = "";
            if (Stone.BLACK.equals(data.lastMoveColor) || Stone.WHITE.equals(data.lastMoveColor)) {

                if (Stone.BLACK.equals(data.lastMoveColor)) stone = "B";
                else if (Stone.WHITE.equals(data.lastMoveColor)) stone = "W";

                char x = data.lastMove == null ? 't' : (char) (data.lastMove[0] + 'a');
                char y = data.lastMove == null ? 't' : (char) (data.lastMove[1] + 'a');

                builder.append(String.format(";%s[%c%c]", stone, x, y));

                // Node properties
                builder.append(data.propertiesString());

                // Write the comment
                if (data.comment != null) {
                    builder.append(String.format("C[%s]", data.comment));
                }
            }

            if (node.numberOfChildren() > 1) {
                // Variation
                for (BoardHistoryNode sub : node.getNexts()) {
                    builder.append("(");
                    builder.append(generateNode(board, sub));
                    builder.append(")");
                }
            } else if (node.numberOfChildren() == 1) {
                builder.append(generateNode(board, node.next()));
            } else {
                return builder.toString();
            }
        }

        return builder.toString();
    }

    public static boolean isListProperty(String key) {
        for(String k : listProps) {
            if(k.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMarkupProperty(String key) {
        for(String k : markupProps) {
            if(k.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a value with key, or the default if there is no such key
     * 
     * @param key
     * @param defaultValue
     * @return
     */
    public static String optProperty(Map<String, String> props, String key, String defaultValue) {
        return props.getOrDefault(key, defaultValue);
    }

    /**
     * Add a key and value
     * 
     * @param key
     * @param value
     */
    public static void addProperty(Map<String, String> props, String key, String value) {
        if (SGFParser.isListProperty(key)) {
            // Label and add/remove stones
            props.merge(key, value, (old, val) -> old + "," + val);
        } else {
            props.put(key, value);
        }
    }

    /**
     * Add the properties
     * 
     * @return
     */
    public static void addProperties(Map<String, String> props, Map<String, String> addProps) {
        if (addProps != null && addProps.size() > 0) {
            addProps.forEach((key, value) -> addProperty(props, key, value));
        }
    }

    /**
     * Add the properties from string
     * 
     * @return
     */
    public static void addProperties(Map<String, String> props, String propsStr) {
        if (propsStr != null) {
            boolean inTag = false, escaping = false;
            String tag = null;
            StringBuilder tagBuilder = new StringBuilder();
            StringBuilder tagContentBuilder = new StringBuilder();

            for (int i = 0; i < propsStr.length(); i++) {
                char c = propsStr.charAt(i);
                if (escaping) {
                    tagContentBuilder.append(c);
                    escaping = false;
                    continue;
                }
                switch (c) {
                    case '(':
                        if (inTag) {
                            if (i > 0) {
                                tagContentBuilder.append(c);
                            }
                        }
                        break;
                    case ')':
                        if (inTag) {
                            tagContentBuilder.append(c);
                        }
                        break;
                    case '[':
                        inTag = true;
                        String tagTemp = tagBuilder.toString();
                        if (!tagTemp.isEmpty()) {
                            tag = tagTemp.replaceAll("[a-z]", "");
                        }
                        tagContentBuilder = new StringBuilder();
                        break;
                    case ']':
                        inTag = false;
                        tagBuilder = new StringBuilder();
                        addProperty(props, tag, tagContentBuilder.toString());
                        break;
                    case ';':
                        break;
                    default:
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
        }
    }

    /**
     * Get properties string
     * 
     * @return
     */
    public static String propertiesString(Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        if (props != null) {
            props.forEach((key, value) -> sb.append(nodeString(key, value)));
        }
        return sb.toString();
    }

    /**
     * Get node string
     * 
     * @param key
     * @param value
     * @return
     */
    public static String nodeString(String key, String value) {
        StringBuilder sb = new StringBuilder();
        if (SGFParser.isListProperty(key)) {
            // Label and add/remove stones
            sb.append(key);
            String[] vals = value.split(",");
            for (String val : vals) {
                sb.append("[").append(val).append("]");
            }
        } else {
            sb.append(key).append("[").append(value).append("]");
        }
        return sb.toString();
    }
}
