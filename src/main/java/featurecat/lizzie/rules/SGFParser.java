package featurecat.lizzie.rules;

import static java.util.Arrays.asList;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import featurecat.lizzie.analysis.Leelaz;
import featurecat.lizzie.util.EncodingDetector;
import featurecat.lizzie.util.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SGFParser {
  private static final SimpleDateFormat SGF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String[] listProps =
      new String[] {"LB", "CR", "SQ", "MA", "TR", "AB", "AW", "AE"};
  private static final String[] markupProps = new String[] {"LB", "CR", "SQ", "MA", "TR"};

  public static boolean load(String filename) throws IOException {
    return load(filename, false);
  }

  public static boolean load(String filename, boolean extend) throws IOException {
    File file = new File(filename);
    if (!file.exists() || !file.canRead()) {
      return false;
    }

    StringBuilder builder = new StringBuilder();
    String encoding = EncodingDetector.detect(filename);
    try (FileInputStream fp = new FileInputStream(file);
        InputStreamReader reader = new InputStreamReader(fp, encoding)) {
      while (reader.ready()) {
        builder.append((char) reader.read());
      }
    }

    String value = builder.toString();
    if (value.isEmpty()) {
      return false;
    }

    return loadFromString(value, extend);
  }

  public static boolean loadFromString(String sgfString) {
    return loadFromString(sgfString, false);
  }

  public static boolean loadFromString(String sgfString, boolean extend) {
    if (!extend) {
      // Clear the board
      Lizzie.board.clear();
    }

    return parse(sgfString, extend);
  }

  public static String passPos() {
    return (Board.boardWidth <= 51 && Board.boardHeight <= 51)
        ? String.format(
            "%c%c", alphabet.charAt(Board.boardWidth), alphabet.charAt(Board.boardHeight))
        : "";
  }

  public static boolean isPassPos(String pos) {
    // TODO
    String passPos = passPos();
    return pos.isEmpty() || passPos.equals(pos);
  }

  public static int[] convertSgfPosToCoord(String pos) {
    if (isPassPos(pos)) return null;
    int[] ret = new int[2];
    ret[0] = alphabet.indexOf(pos.charAt(0));
    ret[1] = alphabet.indexOf(pos.charAt(1));
    return ret;
  }

  private static boolean parse(String value, boolean extend) {
    // Drop anything outside "(;...)"
    final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;{0,1}.*\\))(?s).*?");
    Matcher sgfMatcher = SGF_PATTERN.matcher(value);
    if (sgfMatcher.matches()) {
      value = sgfMatcher.group(1);
    } else {
      return false;
    }

    // Determine the SZ property
    Pattern szPattern = Pattern.compile("(?s).*?SZ\\[([\\d:]+)\\](?s).*");
    Matcher szMatcher = szPattern.matcher(value);
    int boardWidth = 19;
    int boardHeight = 19;
    if (szMatcher.matches()) {
      String sizeStr = szMatcher.group(1);
      Pattern sizePattern = Pattern.compile("([\\d]+):([\\d]+)");
      Matcher sizeMatcher = sizePattern.matcher(sizeStr);
      if (sizeMatcher.matches()) {
        Lizzie.board.reopen(
            Integer.parseInt(sizeMatcher.group(1)), Integer.parseInt(sizeMatcher.group(2)));
      } else {
        int boardSize = Integer.parseInt(sizeStr);
        Lizzie.board.reopen(boardSize, boardSize);
      }
    } else {
      Lizzie.board.reopen(boardWidth, boardHeight);
    }

    if (extend) {
      BoardHistoryList history = Lizzie.board.getHistory();
      parseValue(value, history, false, extend, true);
    } else {
      parseValue(value, null, false, extend, true);
    }

    return true;
  }

  private static BoardHistoryList parseValue(
      String value, BoardHistoryList history, boolean isBranch, boolean extend, boolean mainMove) {

    int subTreeDepth = 0;
    // Save the variation step count
    Map<Integer, Integer> subTreeStepMap = new HashMap<Integer, Integer>();
    // Comment of the game head
    String headComment = "";
    // Game properties
    Map<String, String> gameProperties = new HashMap<String, String>();
    Map<String, String> pendingProps = new HashMap<String, String>();
    boolean inTag = false,
        isMultiGo = false,
        escaping = false,
        moveStart = false,
        addPassForMove = true;
    boolean inProp = false;
    String tag = "";
    StringBuilder tagBuilder = new StringBuilder();
    StringBuilder tagContentBuilder = new StringBuilder();
    // MultiGo 's branch: (Main Branch (Main Branch) (Branch) )
    // Other 's branch: (Main Branch (Branch) Main Branch)
    if (value.matches("(?s).*\\)\\s*\\)")) {
      isMultiGo = true;
    }
    if (isBranch) {
      subTreeDepth += 1;
      // Initialize the step count
      subTreeStepMap.put(subTreeDepth, 0);
    }

    String blackPlayer = "", whitePlayer = "";

    // Support unicode characters (UTF-8)
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (escaping) {
        // Any char following "\" is inserted verbatim
        // (ref) "3.2. Text" in https://www.red-bean.com/sgf/sgf4.html
        tagContentBuilder.append(c == 'n' ? "\n" : c);
        escaping = false;
        continue;
      }
      switch (c) {
        case '(':
          if (!inTag) {
            subTreeDepth += 1;
            // Initialize the step count
            subTreeStepMap.put(subTreeDepth, 0);
            addPassForMove = true;
            pendingProps = new HashMap<String, String>();
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
                if (history == null) {
                  Lizzie.board.previousMove();
                } else {
                  history.previous();
                }
              }
            }
            subTreeDepth -= 1;
          } else {
            // Allow the comment tag includes '('
            tagContentBuilder.append(c);
          }
          break;
        case '[':
          if (!inProp) {
            inProp = true;
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
          } else {
            tagContentBuilder.append(c);
          }
          break;
        case ']':
          if (subTreeDepth > 1 && !isMultiGo) {
            break;
          }
          inTag = false;
          inProp = false;
          tagBuilder = new StringBuilder();
          String tagContent = tagContentBuilder.toString();
          // We got tag, we can parse this tag now.
          if (tag.equals("B") || tag.equals("W")) {
            moveStart = true;
            addPassForMove = true;
            int[] move = convertSgfPosToCoord(tagContent);
            // Save the step count
            subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
            Stone color = tag.equals("B") ? Stone.BLACK : Stone.WHITE;
            boolean newBranch = (!extend && subTreeStepMap.get(subTreeDepth) == 1);
            if (move == null) {
              if (history == null) {
                Lizzie.board.pass(color, newBranch, false);
              } else {
                history.pass(color, newBranch, false);
              }
            } else {
              if (history == null) {
                Lizzie.board.place(move[0], move[1], color, newBranch, false, mainMove);
              } else {
                history.place(move[0], move[1], color, newBranch, false, mainMove);
              }
            }
            if (newBranch) {
              if (history == null) {
                processPendingPros(Lizzie.board.getHistory(), pendingProps);
              } else {
                processPendingPros(history, pendingProps);
              }
            }
          } else if (tag.equals("C")) {
            // Support comment
            if (!moveStart) {
              headComment = tagContent;
            } else {
              if (history == null) {
                Lizzie.board.comment(tagContent);
              } else {
                history.getData().comment = tagContent;
              }
            }
          } else if (tag.equals("LZ") && Lizzie.config.holdBestMovesToSgf && history == null) {
            // Content contains data for Lizzie to read
            String[] lines = tagContent.split("\n");
            String[] line1 = lines[0].split(" ");
            String line2 = "";
            if (lines.length > 1) {
              line2 = lines[1];
            }
            String versionNumber = line1[0];
            Lizzie.board.getData().winrate = 100 - Double.parseDouble(line1[1]);
            int numPlayouts =
                Integer.parseInt(
                    line1[2]
                        .replaceAll("k", "000")
                        .replaceAll("m", "000000")
                        .replaceAll("[^0-9]", ""));
            Lizzie.board.getData().setPlayouts(numPlayouts);
            if (numPlayouts > 0 && !line2.isEmpty()) {
              Lizzie.board.getData().bestMoves = Lizzie.leelaz.parseInfo(line2);
            }
          } else if (tag.equals("AB") || tag.equals("AW")) {
            int[] move = convertSgfPosToCoord(tagContent);
            Stone color = tag.equals("AB") ? Stone.BLACK : Stone.WHITE;
            if (moveStart) {
              // add to node properties
              if (history == null) {
                Lizzie.board.addNodeProperty(tag, tagContent);
              } else {
                history.addNodeProperty(tag, tagContent);
              }
              if (addPassForMove) {
                // Save the step count
                subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                boolean newBranch = (!extend && subTreeStepMap.get(subTreeDepth) == 1);
                if (history == null) {
                  Lizzie.board.pass(color, newBranch, true);
                } else {
                  history.pass(color, newBranch, true);
                }
                if (newBranch) {
                  if (history == null) {
                    processPendingPros(Lizzie.board.getHistory(), pendingProps);
                  } else {
                    processPendingPros(history, pendingProps);
                  }
                }
                addPassForMove = false;
              }
              if (history == null) {
                Lizzie.board.addNodeProperty(tag, tagContent);
              } else {
                history.addNodeProperty(tag, tagContent);
              }
              if (move != null) {
                if (history == null) {
                  Lizzie.board.addStone(move[0], move[1], color);
                } else {
                  history.addStone(move[0], move[1], color);
                }
              }
            } else {
              if (move == null) {
                if (history == null) {
                  Lizzie.board.pass(color);
                } else {
                  history.pass(color);
                }
              } else {
                if (history == null) {
                  Lizzie.board.place(move[0], move[1], color);
                } else {
                  history.place(move[0], move[1], color);
                }
              }
              if (history == null) {
                Lizzie.board.flatten();
              } else {
                history.flatten();
              }
            }
          } else if (tag.equals("PB")) {
            blackPlayer = tagContent;
            if (history == null) {
              Lizzie.board.getHistory().getGameInfo().setPlayerBlack(blackPlayer);
            } else {
              history.getGameInfo().setPlayerBlack(blackPlayer);
            }
          } else if (tag.equals("PW")) {
            whitePlayer = tagContent;
            if (history == null) {
              Lizzie.board.getHistory().getGameInfo().setPlayerWhite(whitePlayer);
            } else {
              history.getGameInfo().setPlayerWhite(whitePlayer);
            }
          } else if (tag.equals("KM")) {
            try {
              if (tagContent.trim().isEmpty()) {
                tagContent = "0.0";
              }
              if (history == null) {
                Lizzie.board.setKomi(Double.parseDouble(tagContent));
              } else {
                history.getGameInfo().setKomi(Double.parseDouble(tagContent));
              }
            } catch (NumberFormatException e) {
              e.printStackTrace();
            }
          } else if (tag.equals("HA")) {
            try {
              if (tagContent.trim().isEmpty()) {
                tagContent = "0";
              }
              int handicap = Integer.parseInt(tagContent);
              if (history == null) {
                Lizzie.board.getHistory().getGameInfo().setHandicap(handicap);
              } else {
                history.getGameInfo().setHandicap(handicap);
              }
            } catch (NumberFormatException e) {
              e.printStackTrace();
            }
          } else {
            if (moveStart) {
              // Other SGF node properties
              if ("AE".equals(tag)) {
                // remove a stone
                if (addPassForMove) {
                  // Save the step count
                  subTreeStepMap.put(subTreeDepth, subTreeStepMap.get(subTreeDepth) + 1);
                  Stone color =
                      ((history == null
                                  && Lizzie.board.getHistory().getLastMoveColor() == Stone.WHITE)
                              || (history != null && history.getLastMoveColor() == Stone.WHITE))
                          ? Stone.BLACK
                          : Stone.WHITE;
                  boolean newBranch = (!extend && subTreeStepMap.get(subTreeDepth) == 1);
                  if (history == null) {
                    Lizzie.board.pass(color, newBranch, true);
                  } else {
                    history.pass(color, newBranch, true);
                  }
                  if (newBranch) {
                    if (history == null) {
                      processPendingPros(Lizzie.board.getHistory(), pendingProps);
                    } else {
                      processPendingPros(history, pendingProps);
                    }
                  }
                  addPassForMove = false;
                }
                if (history == null) {
                  Lizzie.board.addNodeProperty(tag, tagContent);
                } else {
                  history.addNodeProperty(tag, tagContent);
                }
                int[] move = convertSgfPosToCoord(tagContent);
                if (move != null) {
                  if (history == null) {
                    Lizzie.board.removeStone(
                        move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                  } else {
                    history.removeStone(
                        move[0], move[1], tag.equals("AB") ? Stone.BLACK : Stone.WHITE);
                  }
                }
              } else {
                boolean firstProp = (subTreeStepMap.get(subTreeDepth) == 0);
                if (firstProp) {
                  addProperty(pendingProps, tag, tagContent);
                } else {
                  if (history == null) {
                    Lizzie.board.addNodeProperty(tag, tagContent);
                  } else {
                    history.addNodeProperty(tag, tagContent);
                  }
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

    if (isBranch) {
      history.toBranchTop();
    } else if (!extend) {
      Lizzie.frame.setPlayers(whitePlayer, blackPlayer);
      if (history == null) {
        if (!Utils.isBlank(gameProperties.get("RE"))
            && Utils.isBlank(Lizzie.board.getHistory().getData().comment)) {
          Lizzie.board.getHistory().getData().comment = gameProperties.get("RE");
        }

        // Rewind to game start
        while (Lizzie.board.previousMove()) ;

        // Set AW/AB Comment
        if (!headComment.isEmpty()) {
          Lizzie.board.comment(headComment);
          Lizzie.frame.refresh();
        }
        if (gameProperties.size() > 0) {
          Lizzie.board.addNodeProperties(gameProperties);
        }
      } else {
        if (!Utils.isBlank(gameProperties.get("RE")) && Utils.isBlank(history.getData().comment)) {
          history.getData().comment = gameProperties.get("RE");
        }

        // Rewind to game start
        while (history.previous().isPresent()) ;

        // Set AW/AB Comment
        if (!headComment.isEmpty()) {
          history.getData().comment = headComment;
        }
        if (gameProperties.size() > 0) {
          history.getData().addProperties(gameProperties);
        }
      }
    }
    return history;
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
    String playerB = gameInfo.getPlayerBlack();
    String playerW = gameInfo.getPlayerWhite();
    Double komi = gameInfo.getKomi();
    Integer handicap = gameInfo.getHandicap();
    String date = SGF_DATE_FORMAT.format(gameInfo.getDate());

    // add SGF header
    StringBuilder builder = new StringBuilder("(;");
    StringBuilder generalProps = new StringBuilder("");
    if (handicap != 0) generalProps.append(String.format("HA[%s]", handicap));
    generalProps.append(
        String.format(
            "KM[%s]PW[%s]PB[%s]DT[%s]AP[Lizzie: %s]SZ[%s]",
            komi,
            playerW,
            playerB,
            date,
            Lizzie.lizzieVersion,
            Board.boardWidth
                + (Board.boardWidth != Board.boardHeight ? ":" + Board.boardHeight : "")));

    // To append the winrate to the comment of sgf we might need to update the Winrate
    if (Lizzie.config.appendWinrateToComment) {
      Lizzie.board.updateWinrate();
    }

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
          builder.append(String.format("[%s]", asCoord(i)));
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
          if (stone.isBlack()) {
            abStone.append(String.format("[%s]", asCoord(i)));
          } else {
            awStone.append(String.format("[%s]", asCoord(i)));
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
    if (!history.getData().comment.isEmpty()) {
      builder.append(String.format("C[%s]", Escaping(history.getData().comment)));
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

  /** Generate node with variations */
  private static String generateNode(Board board, BoardHistoryNode node) throws IOException {
    StringBuilder builder = new StringBuilder("");

    if (node != null) {

      BoardData data = node.getData();
      String stone = "";
      if (Stone.BLACK.equals(data.lastMoveColor) || Stone.WHITE.equals(data.lastMoveColor)) {

        if (Stone.BLACK.equals(data.lastMoveColor)) stone = "B";
        else if (Stone.WHITE.equals(data.lastMoveColor)) stone = "W";

        builder.append(";");
        if (!data.dummy) {
          builder.append(
              String.format(
                  "%s[%s]",
                  stone, data.lastMove.isPresent() ? asCoord(data.lastMove.get()) : passPos()));
        }

        // Node properties
        builder.append(data.propertiesString());

        if (Lizzie.config.appendWinrateToComment) {
          // Append the winrate to the comment of sgf
          data.comment = formatComment(node);
        }

        // Write the comment
        if (!data.comment.isEmpty()) {
          builder.append(String.format("C[%s]", Escaping(data.comment)));
        }

        // Add LZ specific data to restore on next load
        if (Lizzie.config.holdBestMovesToSgf) {
          builder.append(String.format("LZ[%s]", formatNodeData(node)));
        }
      }

      if (node.numberOfChildren() > 1) {
        // Variation
        for (BoardHistoryNode sub : node.getVariations()) {
          builder.append("(");
          builder.append(generateNode(board, sub));
          builder.append(")");
        }
      } else if (node.numberOfChildren() == 1) {
        builder.append(generateNode(board, node.next().orElse(null)));
      } else {
        return builder.toString();
      }
    }

    return builder.toString();
  }

  /**
   * Format Comment with following format: Move <Move number> <Winrate> (<Last Move Rate
   * Difference>) (<Weight name> / <Playouts>)
   */
  private static String formatComment(BoardHistoryNode node) {
    BoardData data = node.getData();
    String engine = Lizzie.leelaz.currentWeight();

    // Playouts
    String playouts = Utils.getPlayoutsString(data.getPlayouts());

    // Last winrate
    Optional<BoardData> lastNode = node.previous().flatMap(n -> Optional.of(n.getData()));
    boolean validLastWinrate = lastNode.map(d -> d.getPlayouts() > 0).orElse(false);
    double lastWR = validLastWinrate ? lastNode.get().getWinrate() : 50;

    // Current winrate
    boolean validWinrate = (data.getPlayouts() > 0);
    double curWR;
    if (Lizzie.config.uiConfig.getBoolean("win-rate-always-black")) {
      curWR = validWinrate ? data.getWinrate() : lastWR;
    } else {
      curWR = validWinrate ? data.getWinrate() : 100 - lastWR;
    }

    String curWinrate = "";
    if (Lizzie.config.handicapInsteadOfWinrate) {
      curWinrate = String.format("%.2f", Leelaz.winrateToHandicap(100 - curWR));
    } else {
      curWinrate = String.format("%.1f%%", 100 - curWR);
    }

    // Last move difference winrate
    String lastMoveDiff = "";
    if (validLastWinrate && validWinrate) {
      if (Lizzie.config.handicapInsteadOfWinrate) {
        double currHandicapedWR = Leelaz.winrateToHandicap(100 - curWR);
        double lastHandicapedWR = Leelaz.winrateToHandicap(lastWR);
        lastMoveDiff = String.format(": %.2f", currHandicapedWR - lastHandicapedWR);
      } else {
        double diff;
        if (Lizzie.config.uiConfig.getBoolean("win-rate-always-black")) {
          diff = lastWR - curWR;
        } else {
          diff = 100 - lastWR - curWR;
        }
        lastMoveDiff = String.format("(%s%.1f%%)", diff >= 0 ? "+" : "-", Math.abs(diff));
      }
    }

    String wf = "%s's winrate: %s %s\n(%s / %s playouts)";
    boolean blackWinrate =
        !node.getData().blackToPlay || Lizzie.config.uiConfig.getBoolean("win-rate-always-black");
    String nc =
        String.format(
            wf, blackWinrate ? "Black" : "White", curWinrate, lastMoveDiff, engine, playouts);

    if (!data.comment.isEmpty()) {
      String wp =
          "(Black's |White's )winrate: [0-9\\.\\-]+%* \\(*[0-9.\\-+]*%*\\)*\n\\([^\\(\\)/]* \\/ [0-9\\.]*[kmKM]* playouts\\)";
      if (data.comment.matches("(?s).*" + wp + "(?s).*")) {
        nc = data.comment.replaceAll(wp, nc);
      } else {
        nc = String.format("%s\n\n%s", nc, data.comment);
      }
    }
    return nc;
  }

  /** Format Comment with following format: <Winrate> <Playouts> */
  private static String formatNodeData(BoardHistoryNode node) {
    BoardData data = node.getData();

    // Playouts
    String playouts = Utils.getPlayoutsString(data.getPlayouts());

    // Last winrate
    Optional<BoardData> lastNode = node.previous().flatMap(n -> Optional.of(n.getData()));
    boolean validLastWinrate = lastNode.map(d -> d.getPlayouts() > 0).orElse(false);
    double lastWR = validLastWinrate ? lastNode.get().winrate : 50;

    // Current winrate
    boolean validWinrate = (data.getPlayouts() > 0);
    double curWR = validWinrate ? data.winrate : 100 - lastWR;
    String curWinrate = "";
    curWinrate = String.format("%.1f", 100 - curWR);

    String wf = "%s %s %s\n%s";

    return String.format(
        wf, Lizzie.lizzieVersion, curWinrate, playouts, node.getData().bestMovesToString());
  }

  public static boolean isListProperty(String key) {
    return asList(listProps).contains(key);
  }

  public static boolean isMarkupProperty(String key) {
    return asList(markupProps).contains(key);
  }

  /**
   * Get a value with key, or the default if there is no such key
   *
   * @param key
   * @param defaultValue
   * @return
   */
  public static String getOrDefault(Map<String, String> props, String key, String defaultValue) {
    return props.getOrDefault(key, defaultValue);
  }

  /**
   * Add a key and value to the props
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
   * Add the properties by mutating the props
   *
   * @return
   */
  public static void addProperties(Map<String, String> props, Map<String, String> addProps) {
    addProps.forEach((key, value) -> addProperty(props, key, value));
  }

  /**
   * Add the properties from string
   *
   * @return
   */
  public static void addProperties(Map<String, String> props, String propsStr) {
    boolean inTag = false, escaping = false;
    String tag = "";
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

  /**
   * Get properties string by the props
   *
   * @return
   */
  public static String propertiesString(Map<String, String> props) {
    StringBuilder sb = new StringBuilder();
    props.forEach((key, value) -> sb.append(nodeString(key, value)));
    return sb.toString();
  }

  /**
   * Get node string by the key and value
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

  private static void processPendingPros(BoardHistoryList history, Map<String, String> props) {
    props.forEach((key, value) -> history.addNodeProperty(key, value));
    props = new HashMap<String, String>();
  }

  public static String Escaping(String in) {
    String out = in.replaceAll("\\\\", "\\\\\\\\");
    return out.replaceAll("\\]", "\\\\]");
  }

  public static BoardHistoryList parseSgf(String value) {
    BoardHistoryList history = null;

    // Drop anything outside "(;...)"
    final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;{0,1}.*\\))(?s).*?");
    Matcher sgfMatcher = SGF_PATTERN.matcher(value);
    if (sgfMatcher.matches()) {
      value = sgfMatcher.group(1);
    } else {
      return history;
    }

    // Determine the SZ property
    Pattern szPattern = Pattern.compile("(?s).*?SZ\\[(\\d+)\\](?s).*");
    Matcher szMatcher = szPattern.matcher(value);
    int boardWidth = 19;
    int boardHeight = 19;
    if (szMatcher.matches()) {
      String sizeStr = szMatcher.group(1);
      Pattern sizePattern = Pattern.compile("([\\d]+):([\\d]+)");
      Matcher sizeMatcher = sizePattern.matcher(sizeStr);
      if (sizeMatcher.matches()) {
        boardWidth = Integer.parseInt(sizeMatcher.group(1));
        boardHeight = Integer.parseInt(sizeMatcher.group(2));
      } else {
        boardWidth = boardHeight = Integer.parseInt(sizeStr);
      }
    }
    history = new BoardHistoryList(BoardData.empty(boardWidth, boardHeight, true));

    parseValue(value, history, false, false, false);

    return history;
  }

  public static int parseBranch(BoardHistoryList history, String value) {
    parseValue(value, history, true, false, false);
    return history.getCurrentHistoryNode().numberOfChildren() - 1;
  }

  private static boolean isSgf(String value) {
    final Pattern SGF_PATTERN = Pattern.compile("(?s).*?(\\(\\s*;{0,1}.*\\))(?s).*?");
    Matcher sgfMatcher = SGF_PATTERN.matcher(value);
    return sgfMatcher.matches();
  }

  private static String asCoord(int i) {
    int[] cor = Lizzie.board.getCoord(i);

    return asCoord(cor);
  }

  private static String asCoord(int[] c) {
    char x = alphabet.charAt(c[0]);
    char y = alphabet.charAt(c[1]);

    return String.format("%c%c", x, y);
  }
}
