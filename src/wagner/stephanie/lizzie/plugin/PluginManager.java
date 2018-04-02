package wagner.stephanie.lizzie.plugin;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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

        for (File jarFile : path.listFiles()) {
            try {
                plugins.add(new PluginLoader(jarFile.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void onMousePressed(MouseEvent e) {
        for (PluginLoader plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onMouseReleased(MouseEvent e) {
        for (PluginLoader plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onMouseMoved(MouseEvent e) {
        for (PluginLoader plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onKeyPressed(KeyEvent e) {
        for (PluginLoader plugin : plugins) {
            plugin.onKeyPressed(e);
        }
    }

    public static void onKeyReleased(KeyEvent e) {
        for (PluginLoader plugin : plugins) {
            plugin.onKeyReleased(e);
        }
    }

    public static void onShutdown(){
        
        for (PluginLoader plugin : plugins) {
            try {plugin.onShutdown();
            } catch(IOException e) {
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
            BufferedImage cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (plugin.onDraw(cachedImage.createGraphics())) {
                g.drawImage(cachedImage, 0, 0, null);
            }
        }
        g0.drawImage(cachedImageParent, 0, 0, null);
    }
}
