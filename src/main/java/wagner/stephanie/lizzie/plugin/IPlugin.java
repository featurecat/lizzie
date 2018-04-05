package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public abstract class IPlugin {

    public static final IPlugin load(String uri) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {new File(uri).toURI().toURL()});
        IPlugin plugin = (IPlugin) loader.loadClass("plugin.Plugin").newInstance();
        plugin.onInit();

        loader.close();

        return plugin;
    }

    public void onInit() throws IOException {

    }

    public void onMousePressed(MouseEvent e) {

    }

    public void onMouseReleased(MouseEvent e) {

    }

    public void onMouseMoved(MouseEvent e) {

    }

    public void onKeyPressed(KeyEvent e) {

    }

    public void onKeyReleased(KeyEvent e) {

    }

    public boolean onDraw(Graphics2D g) {
        return false;
    }

    public void onShutdown() throws IOException {

    }

    public void onSgfLoaded() {

    }

    public abstract String getName();
    public abstract String getVersion();
}
