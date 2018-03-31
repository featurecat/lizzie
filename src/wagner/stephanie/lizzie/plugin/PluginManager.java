package wagner.stephanie.lizzie.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.TreeSet;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PluginManager {
    public static TreeSet<PluginLoader> plugins;

    public static void loadPlugins() throws IOException {
        if (plugins != null) {
            for (PluginLoader plugin : plugins) {
                plugin.onShutdown();
            }
        }
        plugins = new TreeSet<PluginLoader>();
        File path = new File("./plugin/");
        assert path.isDirectory();

        for (String pluginDirectory : path.list()) {
            File pluginDir = new File(pluginDirectory);
            if (!pluginDir.isDirectory()) {
                continue;
            }
            File pluginInfo = new File(pluginDir, "info.json");
            if (!pluginInfo.exists()) {
                continue;
            }
            FileInputStream stream = new FileInputStream(pluginInfo);
            JSONObject config = new JSONObject(new JSONTokener(stream));
            stream.close();
            try {
                plugins.add(new PluginLoader(config));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
