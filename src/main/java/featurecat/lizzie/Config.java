package featurecat.lizzie;

import org.json.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class Config {

    public boolean showMoveNumber = false;
    public boolean showWinrate = true;
    public boolean showVariationGraph = true;
    public boolean showRawBoard = false;
    public boolean handicapInsteadOfWinrate = false;

    public boolean showBranch = true;
    public boolean showBestMoves = true;
    public boolean showNextMoves = true;
    public boolean showSubBoard = true;
    public boolean largeSubBoard = false;
    
    public JSONObject config;
    public JSONObject leelazConfig;
    public JSONObject uiConfig;
    public JSONObject persisted;

    private String configFilename = "config.txt";
    private String persistFilename = "persist";

    private JSONObject loadAndMergeConfig(JSONObject defaultCfg, String fileName) throws IOException {
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

        JSONObject mergedcfg = null;
        boolean modified = false;
        try {
            mergedcfg = new JSONObject(new JSONTokener(fp));
            modified = merge_defaults(mergedcfg, defaultCfg);
        } catch (JSONException e) {
            mergedcfg = null;
            e.printStackTrace();
        }

        fp.close();

        // Validate and correct settings
        if (validateAndCorrectSettings(mergedcfg)) {
            modified = true;
        }

        if (modified) {
            writeConfig(mergedcfg, file);
        }
        return mergedcfg;


    }

    /**
     * Check settings to ensure its consistency, especially for those whose types are not <code>boolean</code>.
     * If any inconsistency is found, try to correct it or to report it.
     * <br>
     * For example, we only support 9x9, 13x13 or 19x19(default) sized boards. If the configured board size
     * is not in the list above, we should correct it.
     *
     * @param config The config json object to check
     * @return if any correction has been made.
     */
    private boolean validateAndCorrectSettings(JSONObject config) {
        // We don't care persisted settings. They are managed automatically.
        if (config == null || !config.has("ui")) {
            return false;
        }

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
        // Check if the engine program exists.
        String enginePath = leelaz.optString("engine-program", "./leelaz");
        if (!Files.exists(Paths.get(enginePath)) && !Files.exists(Paths.get(enginePath + ".exe" /* For windows */))) {
            // FIXME: I don't know how to handle it properly.. Possibly showing a warning dialog may be a good idea?
            leelaz.put("engine-program", "./leelaz");
            madeCorrections = true;
        }

        // Similar checks for startup directory. It should exist and should be a directory.
        String engineStartLocation = leelaz.optString("engine-start-location", ".");
        if (!(Files.exists(Paths.get(engineStartLocation)) && Files.isDirectory(Paths.get(engineStartLocation)))) {
            leelaz.put("engine-start-location", ".");
            madeCorrections = true;
        }

        return madeCorrections;
    }

    public Config() throws IOException {
        JSONObject defaultConfig = createDefaultConfig();
        JSONObject persistConfig = createPersistConfig();

        // Main properties
        this.config = loadAndMergeConfig(defaultConfig, configFilename);
        // Persisted properties
        this.persisted = loadAndMergeConfig(persistConfig, persistFilename);

        leelazConfig = config.getJSONObject("leelaz");
        uiConfig = config.getJSONObject("ui");

        showMoveNumber = uiConfig.getBoolean("show-move-number");
        showBranch = uiConfig.getBoolean("show-leelaz-variation");
        showWinrate = uiConfig.getBoolean("show-winrate");
        showVariationGraph = uiConfig.getBoolean("show-variation-graph");
        showBestMoves = uiConfig.getBoolean("show-best-moves");
        showNextMoves = uiConfig.getBoolean("show-next-moves");
        showSubBoard = uiConfig.getBoolean("show-subboard");
        largeSubBoard = uiConfig.getBoolean("large-subboard");
        handicapInsteadOfWinrate = uiConfig.getBoolean("handicap-instead-of-winrate");
    }

    // Modifies config by adding in values from default_config that are missing.
    // Returns whether it added anything.
    public boolean merge_defaults(JSONObject config, JSONObject defaults_config) {
        boolean modified = false;
        Iterator<String> keys = defaults_config.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object new_val = defaults_config.get(key);
            if (new_val instanceof JSONObject) {
                if (!config.has(key)) {
                    config.put(key, new JSONObject());
                    modified = true;
                }
                Object old_val = config.get(key);
                modified |= merge_defaults((JSONObject) old_val, (JSONObject) new_val);
            } else {
                if (!config.has(key)) {
                    config.put(key, new_val);
                    modified = true;
                }
            }
        }
        return modified;
    }

    public void toggleShowMoveNumber() {
        this.showMoveNumber = !this.showMoveNumber;
    }
    public void toggleShowBranch() {
        this.showBranch = !this.showBranch;
    }
    public void toggleShowWinrate() {
        this.showWinrate = !this.showWinrate;
    }
    public void toggleShowVariationGraph() {
        this.showVariationGraph = !this.showVariationGraph;
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
    public boolean showLargeSubBoard() {
        return showSubBoard && largeSubBoard;
    }




    private JSONObject createDefaultConfig() {
        JSONObject config = new JSONObject();

        // About engine parameter
        JSONObject leelaz = new JSONObject();
        leelaz.put("network-file", "network.gz");
        leelaz.put("engine-command", "./leelaz --gtp --lagbuffer 0 --weights %network-file --threads 2");
        leelaz.put("engine-start-location", ".");
        leelaz.put("max-analyze-time-minutes", 5);
        leelaz.put("max-game-thinking-time-seconds", 2);
        leelaz.put("print-comms", false);
        leelaz.put("analyze-update-interval-centisec", 10);
        leelaz.put("automatically-download-latest-network", false);

        config.put("leelaz", leelaz);

        // About User Interface display
        JSONObject ui = new JSONObject();

        ui.put("board-color", new JSONArray("[217, 152, 77]"));
        ui.put("theme", "DefaultTheme");
        ui.put("shadows-enabled", true);
        ui.put("fancy-stones", true);
        ui.put("fancy-board", true);
        ui.put("shadow-size", 100);
        ui.put("show-move-number", false);
        ui.put("show-leelaz-variation", true);
        ui.put("show-winrate", true);
        ui.put("show-variation-graph", true);
        ui.put("show-best-moves", true);
        ui.put("show-next-moves", true);
        ui.put("show-subboard", true);
        ui.put("large-subboard", false);
        ui.put("win-rate-always-black", false);
        ui.put("confirm-exit", false);
        ui.put("handicap-instead-of-winrate",false);
        ui.put("board-size", 19);

        config.put("ui", ui);
        return config;
    }

    private JSONObject createPersistConfig() {
        JSONObject config = new JSONObject();

        // About engine parameter
        JSONObject filesys = new JSONObject();
        filesys.put("last-folder", "");

        config.put("filesystem", filesys);

        // About User Interface display
        //JSONObject ui = new JSONObject();

        //ui.put("window-height", 657);
        //ui.put("window-width", 687);

        //config.put("ui", ui);
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

}
