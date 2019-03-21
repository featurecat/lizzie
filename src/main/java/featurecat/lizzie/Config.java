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
  public boolean showCoordinates = false;

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
   * For example, we only support 9x9, 13x13 or 19x19(default) sized boards. If the configured board
   * size is not in the list above, we should correct it.
   *
   * @param config The config json object to check
   * @return if any correction has been made.
   */
  private boolean validateAndCorrectSettings(JSONObject config) {
    boolean madeCorrections = false;

    // Check ui configs
    JSONObject ui = config.getJSONObject("ui");

    // Check board-size. We support only 9x9, 13x13 or 19x19
    int boardSize = ui.optInt("board-size", 19);
    if (boardSize != 19 && boardSize != 13 && boardSize != 9) {
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
    startMaximized = uiConfig.getBoolean("window-maximized");
    showDynamicKomi = uiConfig.getBoolean("show-dynamic-komi");
    appendWinrateToComment = uiConfig.optBoolean("append-winrate-to-comment");
    showCoordinates = uiConfig.optBoolean("show-coordinates");

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
    ui.put("window-size", new JSONArray("[1024, 768]"));
    ui.put("window-maximized", false);
    ui.put("show-dynamic-komi", true);
    ui.put("min-playout-ratio-for-stats", 0.0);
    ui.put("theme", "default");
    ui.put("only-last-move-number", 0);
    ui.put("new-move-number-in-branch", true);
    ui.put("append-winrate-to-comment", false);
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
    writeConfig(this.persisted, new File(persistFilename));
  }

  public void save() throws IOException {
    writeConfig(this.config, new File(configFilename));
  }
}
