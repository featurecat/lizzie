package wagner.stephanie.lizzie.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Config {

    public boolean showMoveNumber = false;

    public org.json.JSONObject config;
    
    public Config() throws IOException {
        File file = new File("lizzie.json");
        if (!file.canRead()) {
            System.err.println("Config file not exists, try to create");
            try {
                createNewConfig(file);
            } catch (org.json.JSONException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        FileInputStream fp = new FileInputStream(file);

        this.config = null;

        while (this.config == null) {
            try {
                this.config = new org.json.JSONObject(new org.json.JSONTokener(fp));
            } catch (org.json.JSONException e) {
                this.config = null;
                e.printStackTrace();
            }
        }
        
        fp.close();
    }

    public void toggleShowMoveNumber() {
        this.showMoveNumber = !this.showMoveNumber;
    }

    private void createNewConfig(File file) throws IOException, org.json.JSONException {
        org.json.JSONObject config = new org.json.JSONObject();
        
        // About engine parameter
        org.json.JSONObject leelaz = new org.json.JSONObject();
        leelaz.put("weights", "network");
        leelaz.put("threads", 2);
        leelaz.put("gpu", new org.json.JSONArray("[0]"));
        leelaz.put("noise", false);

        config.put("leelaz", leelaz);

        // About User Interface display
        org.json.JSONObject ui = new org.json.JSONObject();

        ui.put("board-color", new org.json.JSONArray("[178, 140, 0]"));
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
