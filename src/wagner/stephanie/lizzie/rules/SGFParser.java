package wagner.stephanie.lizzie.rules;

import wagner.stephanie.lizzie.Lizzie;

import java.io.*;

public class SGFParser {
    public static boolean load(String filename) throws IOException {
        // Clear the board
        while (Lizzie.board.previousMove())
            ;

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
        boolean isTag = false, isInTag = false, isMultiGo = false;
        int subTreeDepth = 0;
        String tag = null;
        char last = '(';
        StringBuilder tagBuffer = new StringBuilder();
        StringBuilder tagContentBuffer = new StringBuilder();
        char[] charList = value.toCharArray();
        if (value.charAt(value.length() - 2) == ')') {
            isMultiGo = true;
        }

        for (char c : charList) {
            if (c == '(') {
                subTreeDepth += 1;
                last = c;
                continue;
            }
            if (c == ')') {
                subTreeDepth -= 1;
                if (isMultiGo) {
                    break;
                }
                last = c;
                continue;
            }
            // Skip subtree/branch
            if (subTreeDepth > 1 && !isMultiGo) {
                last = c;
                continue;
            }
            if (c == ']') {
                String tagContent = tagContentBuffer.toString();
                if (tag != null) {
                    System.out.println(tag);
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
                }
                isInTag = false;
                isTag = true;
                tagBuffer = new StringBuilder();
                last = c;
                continue;
            }

            if (c == '[') {
                tagContentBuffer = new StringBuilder();
                String tagTemp = tagBuffer.toString();
                if (!(tagTemp.trim()).isEmpty()) {
                    tag = new String(tagTemp.trim());
                }
                isTag = false;
                isInTag = true;
                last = c;
                continue;
            }
            if (c == ';' && !isInTag) {
                isTag = true;
                last = c;
                continue;
            }
            if (isInTag) {
                tagContentBuffer.append(c);
                last = c;
                continue;
            } else {
                tagBuffer.append(c);
                last = c;
                continue;
            }
        }
        return true;
    }

    public static boolean save(String filename) throws IOException {
        FileOutputStream fp = new FileOutputStream(filename);
        OutputStreamWriter writer = new OutputStreamWriter(fp);
        StringBuilder builder = new StringBuilder(String.format("(;KM[7.5]AP[Lizzie: %s]", Lizzie.lizzieVersion));
        BoardHistoryList history = Lizzie.board.getHistory();
        while (history.previous() != null)
            ;
        BoardData data = null;
        while ((data = history.next()) != null) {
            StringBuilder tag = new StringBuilder(";");
            if (data.lastMoveColor.equals(Stone.BLACK)) {
                tag.append("B");
            } else if (data.lastMoveColor.equals(Stone.WHITE)) {
                tag.append("W");
            } else {
                return false;
            }
            char x = (char) data.lastMove[0], y = (char) data.lastMove[1];
            x += 'a';
            y += 'a';
            tag.append(String.format("[%c%c]", x, y));
            builder.append(tag);
        }
        builder.append(')');
        writer.append(builder.toString());
        writer.close();
        fp.close();
        return true;
    }
}