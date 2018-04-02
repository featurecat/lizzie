package wagner.stephanie.lizzie.plugin;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import wagner.stephanie.lizzie.Lizzie;

public class PluginManager {
    public static HashSet<PluginLoader> plugins;

    public static void loadPlugins() throws IOException {
        if (plugins != null) {
            for (PluginLoader plugin : plugins) {
                plugin.onShutdown();
            }
        }
        plugins = new HashSet<PluginLoader>();
        File path = new File("./plugin/");
        assert path.isDirectory();

        for (String pluginDirectory : path.list()) {
            File pluginDir = new File(path, pluginDirectory);
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

    public static void onDraw(Graphics2D g0) {
        int width = Lizzie.frame.getWidth();
        int height = Lizzie.frame.getHeight();
        BufferedImage cachedImageParent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cachedImageParent.createGraphics();
        for (PluginLoader plugin : plugins) {
            BufferedImage cachedImage = new BufferedImage(Lizzie.frame.getWidth(), Lizzie.frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            plugin.onDraw(cachedImage.createGraphics());
            
        }
    }
}
