package featurecat.lizzie.plugin;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.io.IOException;

import featurecat.lizzie.Lizzie;

public class PluginManager {
    public static HashSet<IPlugin> plugins;

    public static void loadPlugins() throws IOException {
        if (plugins != null) {
            for (IPlugin plugin : plugins) {
                plugin.onShutdown();
            }
        }
        plugins = new HashSet<IPlugin>();
        File path = new File("./plugin/");
        path.mkdirs();

        for (File jarFile : path.listFiles()) {
            if (jarFile.isDirectory()) {
                continue;
            }
            try {
                plugins.add(IPlugin.load(jarFile.getPath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void onMousePressed(MouseEvent e) {
        for (IPlugin plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onMouseReleased(MouseEvent e) {
        for (IPlugin plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onMouseMoved(MouseEvent e) {
        for (IPlugin plugin : plugins) {
            plugin.onMousePressed(e);
        }
    }

    public static void onKeyPressed(KeyEvent e) {
        for (IPlugin plugin : plugins) {
            plugin.onKeyPressed(e);
        }
    }

    public static void onKeyReleased(KeyEvent e) {
        for (IPlugin plugin : plugins) {
            plugin.onKeyReleased(e);
        }
    }

    public static void onShutdown(){
        
        for (IPlugin plugin : plugins) {
            try {plugin.onShutdown();
            } catch(IOException e) {
                e.printStackTrace();
            }   
        }
    }

    public static void onSgfLoaded() {
        for (IPlugin plugin : plugins) {
            plugin.onSgfLoaded();
        }
    }

    public static void onDraw(Graphics2D g0) {
        int width = Lizzie.frame.getWidth();
        int height = Lizzie.frame.getHeight();
        BufferedImage cachedImageParent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cachedImageParent.createGraphics();
        for (IPlugin plugin : plugins) {
            BufferedImage cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = cachedImage.createGraphics();
            if (plugin.onDraw(graphics)) {
                g.drawImage(cachedImage, 0, 0, null);
            }
            graphics.dispose();
        }
        g0.drawImage(cachedImageParent, 0, 0, null);
        g.dispose();
    }
}
