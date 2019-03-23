package featurecat.lizzie;

import featurecat.lizzie.theme.Theme;
import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.*;

import javax.swing.*;

public class Config {

  public boolean showBorder = false;
  public boolean showMoveNumber = false;
  public int onlyLastMoveNumber = 0;
  // 0: Do not show; -1: Show all move number; other: Show last move number
  public int allowMoveNumber = -1;
  public boolean newMoveNumberInBranch = true;
  public boolean showWinrate = true;
  public boolean largeWinrate = false;
  public boolean showBlunderBar = true;
  public boolean weightedBlunderBarHeight = false;
  public boolean dynamicWinrateGraphWidth = false;
  public boolean showVariationGraph = true;
  public boolean showComment = true;
  public boolean showRawBoard = false;
  public boolean showBestMovesTemporarily = false;
  public boolean showCaptured = true;
  public boolean handicapInsteadOfWinrate = false;
  public boolean showDynamicKomi = true;
  public double replayBranchIntervalSeconds = 1.0;
  public boolean showCoordinates = false;
  public boolean colorByWinrateInsteadOfVisits = false;

  public boolean showStatus = true;
  public boolean showBranch = true;
  public boolean showBestMoves = true;
  public boolean showNextMoves = true;
  public boolean showSubBoard = true;
  public boolean largeSubBoard = false;
  public boolean startMaximized = true;

  public JSONObject config;
  public JSONObject leelazConfig;
  public JSONObject uiConfig;
  public JSONObject persisted;
  public JSONObject persistedUi;

  private String configFilename = "config.txt";
  private String persistFilename = "persist";

  public Theme theme;
  public float winrateStrokeWidth = 3;
  public int minimumBlunderBarWidth = 3;
  public int shadowSize = 100;
  public String fontName = null;
  public String uiFontName = null;
  public String winrateFontName = null;
  public int commentFontSize = 0;
  public Color commentFontColor = null;
  public Color commentBackgroundColor = null;
  public Color winrateLineColor = null;
  public Color winrateMissLineColor = null;
  public Color blunderBarColor = null;
  public boolean solidStoneIndicator = false;
  public boolean showCommentNodeColor = true;
  public Color commentNodeColor = null;
  public Optional<List<Double>> blunderWinrateThresholds;
  public Optional<Map<Double, Color>> blunderNodeColors;
  public int nodeColorMode = 0;
  public boolean appendWinrateToComment = false;
  public int boardPositionProportion = 4;
  public String gtpConsoleStyle = "";
  private final String defaultGtpConsoleStyle =
      "body {background:#000000; color:#d0d0d0; font-family:Consolas, Menlo, Monaco, 'Ubuntu Mono', monospace; margin:4px;} .command {color:#ffffff;font-weight:bold;} .winrate {color:#ffffff;font-weight:bold;} .coord {color:#ffffff;font-weight:bold;}";

  private JSONObject loadAndMergeConfig(
      JSONObject defaultCfg, String fileName, boolean needValidation) throws IOException {
    File file = new File(fileName);
    if (!file.canRead()) {
      System.err.printf("Creating config file %s\n", fileName);
      try {
        writeConfig(defaultCfg, file);
      } catch (JSONException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    FileInputStream fp = new FileInputStream(file);

    JSONObject mergedcfg = new JSONObject(new JSONTokener(fp));
    boolean modified = mergeDefaults(mergedcfg, defaultCfg);

    fp.close();

    // Validate and correct settings
    if (needValidation && validateAndCorrectSettings(mergedcfg)) {
      modified = true;
    }

    if (modified) {
      writeConfig(mergedcfg, file);
    }
    return mergedcfg;
  }

  /**
   * Check settings to ensure its consistency, especially for those whose types are not <code>
   * boolean</code>. If any inconsistency is found, try to correct it or to report it. <br>
   * For example, we only support square boards of size >= 2x2. If the configured board
   * size is not in the list above, we should correct it.
   *
   * @param config The config json object to check
   * @return if any correction has been made.
   */
  private boolean validateAndCorrectSettings(JSONObject config) {
    boolean madeCorrections = false;

    // Check ui configs
    JSONObject ui = config.getJSONObject("ui");

    // Check board-size
    int boardSize = ui.optInt("board-size", 19);
    if (boardSize < 2) {
      // Correct it to default 19x19
      ui.put("board-size", 19);
      madeCorrections = true;
    }

    // Check engine configs
    JSONObject leelaz = config.getJSONObject("leelaz");
    // Checks for startup directory. It should exist and should be a directory.
    String engineStartLocation = getBestDefaultLeelazPath();
    if (!(Files.exists(Paths.get(engineStartLocation))
        && Files.isDirectory(Paths.get(engineStartLocation)))) {
      leelaz.put("engine-start-location", ".");
      madeCorrections = true;
    }

    return madeCorrections;
  }

  public Config() throws IOException {
    JSONObject defaultConfig = createDefaultConfig();
    JSONObject persistConfig = createPersistConfig();

    // Main properties
    this.config = loadAndMergeConfig(defaultConfig, configFilename, true);
    // Persisted properties
    this.persisted = loadAndMergeConfig(persistConfig, persistFilename, false);

    leelazConfig = config.getJSONObject("leelaz");
    uiConfig = config.getJSONObject("ui");
    persistedUi = persisted.getJSONObject("ui-persist");

    theme = new Theme(uiConfig);

    showBorder = uiConfig.optBoolean("show-border", false);
    showMoveNumber = uiConfig.getBoolean("show-move-number");
    onlyLastMoveNumber = uiConfig.optInt("only-last-move-number");
    allowMoveNumber = showMoveNumber ? (onlyLastMoveNumber > 0 ? onlyLastMoveNumber : -1) : 0;
    newMoveNumberInBranch = uiConfig.optBoolean("new-move-number-in-branch", true);
    showStatus = uiConfig.getBoolean("show-status");
    showBranch = uiConfig.getBoolean("show-leelaz-variation");
    showWinrate = uiConfig.getBoolean("show-winrate");
    largeWinrate = uiConfig.optBoolean("large-winrate", false);
    showBlunderBar = uiConfig.optBoolean("show-blunder-bar", true);
    weightedBlunderBarHeight = uiConfig.optBoolean("weighted-blunder-bar-height", false);
    dynamicWinrateGraphWidth = uiConfig.optBoolean("dynamic-winrate-graph-width", false);
    showVariationGraph = uiConfig.getBoolean("show-variation-graph");
    showComment = uiConfig.optBoolean("show-comment", true);
    showCaptured = uiConfig.getBoolean("show-captured");
    showBestMoves = uiConfig.getBoolean("show-best-moves");
    showNextMoves = uiConfig.getBoolean("show-next-moves");
    showSubBoard = uiConfig.getBoolean("show-subboard");
    largeSubBoard = uiConfig.getBoolean("large-subboard");
    handicapInsteadOfWinrate = uiConfig.getBoolean("handicap-instead-of-winrate");
    showDynamicKomi = uiConfig.getBoolean("show-dynamic-komi");
    appendWinrateToComment = uiConfig.optBoolean("append-winrate-to-comment");
    showCoordinates = uiConfig.optBoolean("show-coordinates");
    replayBranchIntervalSeconds = uiConfig.optDouble("replay-branch-interval-seconds", 1.0);
    colorByWinrateInsteadOfVisits = uiConfig.optBoolean("color-by-winrate-instead-of-visits");
    boardPositionProportion = uiConfig.optInt("board-postion-proportion", 4);

    winrateStrokeWidth = theme.winrateStrokeWidth();
    minimumBlunderBarWidth = theme.minimumBlunderBarWidth();
    shadowSize = theme.shadowSize();
    fontName = theme.fontName();
    uiFontName = theme.uiFontName();
    winrateFontName = theme.winrateFontName();
    commentFontSize = theme.commentFontSize();
    commentFontColor = theme.commentFontColor();
    commentBackgroundColor = theme.commentBackgroundColor();
    winrateLineColor = theme.winrateLineColor();
    winrateMissLineColor = theme.winrateMissLineColor();
    blunderBarColor = theme.blunderBarColor();
    solidStoneIndicator = theme.solidStoneIndicator();
    showCommentNodeColor = theme.showCommentNodeColor();
    commentNodeColor = theme.commentNodeColor();
    blunderWinrateThresholds = theme.blunderWinrateThresholds();
    blunderNodeColors = theme.blunderNodeColors();
    nodeColorMode = theme.nodeColorMode();

    gtpConsoleStyle = uiConfig.optString("gtp-console-style", defaultGtpConsoleStyle);
  }

  // Modifies config by adding in values from default_config that are missing.
  // Returns whether it added anything.
  public boolean mergeDefaults(JSONObject config, JSONObject defaultsConfig) {
    boolean modified = false;
    Iterator<String> keys = defaultsConfig.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object newVal = defaultsConfig.get(key);
      if (newVal instanceof JSONObject) {
        if (!config.has(key)) {
          config.put(key, new JSONObject());
          modified = true;
        }
        Object oldVal = config.get(key);
        modified |= mergeDefaults((JSONObject) oldVal, (JSONObject) newVal);
      } else {
        if (!config.has(key)) {
          config.put(key, newVal);
          modified = true;
        }
      }
    }
    return modified;
  }

  public void toggleShowMoveNumber() {
    if (this.onlyLastMoveNumber > 0) {
      allowMoveNumber =
          (allowMoveNumber == -1 ? onlyLastMoveNumber : (allowMoveNumber == 0 ? -1 : 0));
    } else {
      allowMoveNumber = (allowMoveNumber == 0 ? -1 : 0);
    }
  }

  public void toggleNodeColorMode() {
    this.nodeColorMode = this.nodeColorMode > 1 ? 0 : this.nodeColorMode + 1;
  }

  public void toggleShowBranch() {
    this.showBranch = !this.showBranch;
  }

  public void toggleShowWinrate() {
    this.showWinrate = !this.showWinrate;
  }

  public void toggleLargeWinrate() {
    this.largeWinrate = !this.largeWinrate;
  }

  public void toggleShowVariationGraph() {
    this.showVariationGraph = !this.showVariationGraph;
  }

  public void toggleShowComment() {
    this.showComment = !this.showComment;
  }

  public void toggleShowCommentNodeColor() {
    this.showCommentNodeColor = !this.showCommentNodeColor;
  }

  public void toggleShowBestMoves() {
    this.showBestMoves = !this.showBestMoves;
  }

  public void toggleShowNextMoves() {
    this.showNextMoves = !this.showNextMoves;
  }

  public void toggleHandicapInsteadOfWinrate() {
    this.handicapInsteadOfWinrate = !this.handicapInsteadOfWinrate;
  }

  public void toggleLargeSubBoard() {
    this.largeSubBoard = !this.largeSubBoard;
  }

  public void toggleCoordinates() {
    showCoordinates = !showCoordinates;
  }

  public void toggleEvaluationColoring() {
    colorByWinrateInsteadOfVisits = !colorByWinrateInsteadOfVisits;
  }

  public boolean showLargeSubBoard() {
    return showSubBoard && largeSubBoard;
  }

  public boolean showLargeWinrate() {
    return showWinrate && largeWinrate;
  }

  public boolean showBestMovesNow() {
    return showBestMoves || showBestMovesTemporarily;
  }

  public boolean showBranchNow() {
    return showBranch || showBestMovesTemporarily;
  }

  /**
   * Scans the current directory as well as the current PATH to find a reasonable default leelaz
   * binary.
   *
   * @return A working path to a leelaz binary. If there are none on the PATH, "./leelaz" is
   *     returned for backwards compatibility.
   */
  public static String getBestDefaultLeelazPath() {
    List<String> potentialPaths = new ArrayList<>();
    potentialPaths.add(".");
    potentialPaths.addAll(Arrays.asList(System.getenv("PATH").split(":")));

    for (String potentialPath : potentialPaths) {
      for (String potentialExtension : Arrays.asList(new String[] {"", ".exe"})) {
        File potentialLeelaz = new File(potentialPath, "leelaz" + potentialExtension);
        if (potentialLeelaz.exists() && potentialLeelaz.canExecute()) {
          return potentialLeelaz.getPath();
        }
      }
    }

    return "./leelaz";
  }

  private JSONObject createDefaultConfig() {
    JSONObject config = new JSONObject();

    // About engine parameter
    JSONObject leelaz = new JSONObject();
    leelaz.put("network-file", "network.gz");
    leelaz.put(
        "engine-command",
        String.format(
            "%s --gtp --lagbuffer 0 --weights %%network-file", getBestDefaultLeelazPath()));
    leelaz.put("engine-start-location", ".");
    leelaz.put("max-analyze-time-minutes", 5);
    leelaz.put("max-game-thinking-time-seconds", 2);
    leelaz.put("print-comms", false);
    leelaz.put("analyze-update-interval-centisec", 10);

    config.put("leelaz", leelaz);

    // About User Interface display
    JSONObject ui = new JSONObject();

    ui.put("board-color", new JSONArray("[217, 152, 77]"));
    ui.put("shadows-enabled", true);
    ui.put("fancy-stones", true);
    ui.put("fancy-board", true);
    ui.put("shadow-size", 100);
    ui.put("show-move-number", false);
    ui.put("show-status", true);
    ui.put("show-leelaz-variation", true);
    ui.put("show-winrate", true);
    ui.put("large-winrate", false);
    ui.put("winrate-stroke-width", 3);
    ui.put("show-blunder-bar", true);
    ui.put("minimum-blunder-bar-width", 3);
    ui.put("weighted-blunder-bar-height", false);
    ui.put("dynamic-winrate-graph-width", false);
    ui.put("show-comment", true);
    ui.put("comment-font-size", 0);
    ui.put("show-variation-graph", true);
    ui.put("show-captured", true);
    ui.put("show-best-moves", true);
    ui.put("show-next-moves", true);
    ui.put("show-subboard", true);
    ui.put("large-subboard", false);
    ui.put("win-rate-always-black", false);
    ui.put("confirm-exit", false);
    ui.put("resume-previous-game", false);
    ui.put("autosave-interval-seconds", -1);
    ui.put("handicap-instead-of-winrate", false);
    ui.put("board-size", 19);
    ui.put("show-dynamic-komi", true);
    ui.put("min-playout-ratio-for-stats", 0.0);
    ui.put("theme", "default");
    ui.put("only-last-move-number", 0);
    ui.put("new-move-number-in-branch", true);
    ui.put("append-winrate-to-comment", false);
    ui.put("replay-branch-interval-seconds", 1.0);
    ui.put("gtp-console-style", defaultGtpConsoleStyle);
    config.put("ui", ui);
    return config;
  }

  private JSONObject createPersistConfig() {
    JSONObject config = new JSONObject();

    // About engine parameter
    JSONObject filesys = new JSONObject();
    filesys.put("last-folder", "");

    config.put("filesystem", filesys);

    // About autosave
    config.put("autosave", "");

    // About User Interface display
    JSONObject ui = new JSONObject();

    // ui.put("window-height", 657);
    // ui.put("window-width", 687);
    // ui.put("max-alpha", 240);

    // Main Window Position & Size
    ui.put("main-window-position", new JSONArray("[]"));
    ui.put("gtp-console-position", new JSONArray("[]"));
    ui.put("window-maximized", false);

    config.put("filesystem", filesys);

    // Avoid the key "ui" because it was used to distinguish "config" and "persist"
    // in old version of validateAndCorrectSettings().
    // If we use "ui" here, we will have trouble to run old lizzie.
    config.put("ui-persist", ui);
    return config;
  }

  private void writeConfig(JSONObject config, File file) throws IOException, JSONException {
    file.createNewFile();

    FileOutputStream fp = new FileOutputStream(file);
    OutputStreamWriter writer = new OutputStreamWriter(fp);

    writer.write(config.toString(2));

    writer.close();
    fp.close();
  }

  public void persist() throws IOException {
    boolean windowIsMaximized = Lizzie.frame.getExtendedState() == JFrame.MAXIMIZED_BOTH;

    JSONArray mainPos = new JSONArray();
    if (!windowIsMaximized) {
      mainPos.put(Lizzie.frame.getX());
      mainPos.put(Lizzie.frame.getY());
      mainPos.put(Lizzie.frame.getWidth());
      mainPos.put(Lizzie.frame.getHeight());
    }
    persistedUi.put("main-window-position", mainPos);
    JSONArray gtpPos = new JSONArray();
    gtpPos.put(Lizzie.gtpConsole.getX());
    gtpPos.put(Lizzie.gtpConsole.getY());
    gtpPos.put(Lizzie.gtpConsole.getWidth());
    gtpPos.put(Lizzie.gtpConsole.getHeight());
    persistedUi.put("gtp-console-position", gtpPos);
    persistedUi.put("board-postion-propotion", Lizzie.frame.BoardPositionProportion);
    persistedUi.put("window-maximized", windowIsMaximized);
    writeConfig(this.persisted, new File(persistFilename));
  }

  public void save() throws IOException {
    writeConfig(this.config, new File(configFilename));
  }
}
