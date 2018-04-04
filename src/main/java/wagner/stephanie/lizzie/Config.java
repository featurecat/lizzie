package wagner.stephanie.lizzie;

import org.json.*;

import java.io.*;

public class Config {

    public boolean showMoveNumber = false;
    public boolean showVariation = true;

    public JSONObject config;

    public Config() throws IOException {
        File file = new File("lizzie.properties");
        if (!file.canRead()) {
            System.err.println("Creating config file");
            try {
                createNewConfig(file);
            } catch (JSONException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        FileInputStream fp = new FileInputStream(file);

        this.config = null;

        while (this.config == null) {
            try {
                this.config = new JSONObject(new JSONTokener(fp));
            } catch (JSONException e) {
                this.config = null;
                e.printStackTrace();
            }
        }

        fp.close();
    }

    public void toggleShowMoveNumber() {
        this.showMoveNumber = !this.showMoveNumber;
    }

    public void toggleShowVariation() {
        this.showVariation = !this.showVariation;
    }

    private void createNewConfig(File file) throws IOException, JSONException {
        config = new JSONObject();

        // About engine parameter
        JSONObject leelaz = new JSONObject();
        leelaz.put("weights", "network");
        leelaz.put("threads", 2);
        leelaz.put("gpu", new JSONArray("[]"));
        leelaz.put("max-analyze-time-minutes", 2);
        leelaz.put("max-game-thinking-time-seconds", 2);

        config.put("leelaz", leelaz);

        // About User Interface display
        JSONObject ui = new JSONObject();

        ui.put("board-color", new JSONArray("[178, 140, 0]"));
        ui.put("shadows-enabled", true);
        ui.put("fancy-stones", true);
        ui.put("fancy-board", true);
        ui.put("shadow-size", 100);

        config.put("ui", ui);

        file.createNewFile();

        FileOutputStream fp = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fp);


        writer.write(config.toString(2));

        writer.close();
        fp.close();
    }


}
