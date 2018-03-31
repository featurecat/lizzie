package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import wagner.stephanie.lizzie.Lizzie;


public class PluginLoader {
    public Class<IPlugin> plugin;

    public PluginLoader(String packageName) {
        plugin = Class.forName(packageName);
        plugin.onInit(Lizzie.class);
    }

    public void onMousePressed(MouseEvent e) {
        plugin.onMousePressed(e);
    }

    public void onMouseReleased(MouseEvent e) {
        plugin.onMousePressed(e);
    }

    public void onMouseMoved(MouseEvent e) {
        plugin.onMouseMoved(e);
    }

    public void onKeyPressed(KeyEvent e) {
        plugin.onKeyPressed(e);
    }

    public void onKeyReleased(KeyEvent e) {
        plugin.onKeyReleased();
    }

    public void onDraw(Graphics2D g) {
        plugin.onDraw(g);
    }

    public void onShutdown() throws IOException {
        plugin.onShutdown();
    }
}
