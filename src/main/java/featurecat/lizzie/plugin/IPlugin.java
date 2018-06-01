package featurecat.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public interface IPlugin {

    public static IPlugin load(String uri) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {new File(uri).toURI().toURL()});
        IPlugin plugin = (IPlugin) loader.loadClass("plugin.Plugin").newInstance();
        plugin.onInit();

        loader.close();

        return plugin;
    }

    public default void onInit() throws IOException {

    }

    public default void onMousePressed(MouseEvent e) {

    }

    public default void onMouseReleased(MouseEvent e) {

    }

    public default void onMouseMoved(MouseEvent e) {

    }

    public default void onKeyPressed(KeyEvent e) {

    }

    public default void onKeyReleased(KeyEvent e) {

    }

    public default boolean onDraw(Graphics2D g) {
        return false;
    }

    public default void onShutdown() throws IOException {

    }

    public default void onSgfLoaded() {

    }

    public String getName();
    public String getVersion();
}
