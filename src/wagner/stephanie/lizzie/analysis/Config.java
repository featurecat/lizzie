package wagner.stephanie.lizzie.analysis;

import org.json.*;

import java.io.*;

public class Config {

    public boolean showMoveNumber = false;

    public JSONObject config;

    public Config() throws IOException {
        File file = new File("lizzie.json");
        if (!file.canRead()) {
            System.err.println("Config file not exists, try to create");
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

    private void createNewConfig(File file) throws IOException, JSONException {
        config = new JSONObject();

        // About engine parameter
        JSONObject leelaz = new JSONObject();
        leelaz.put("weights", "network");
        leelaz.put("threads", 2);
        leelaz.put("gpu", new JSONArray("[0]"));
        leelaz.put("noise", false);

        config.put("leelaz", leelaz);

        // About User Interface display
        JSONObject ui = new JSONObject();

        ui.put("board-color", new JSONArray("[178, 140, 0]"));
        ui.put("branch-stone-alpha", 160);

        config.put("ui", ui);

        file.createNewFile();

        FileOutputStream fp = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fp);

        writer.write(config.toString());

        writer.close();
        fp.close();
    }


}
