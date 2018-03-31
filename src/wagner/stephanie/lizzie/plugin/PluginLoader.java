package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;
import wagner.stephanie.lizzie.Lizzie;


public final class PluginLoader {
    public Class pluginClass;
    public IPlugin plugin;
    public String name;
    public String version;
    public String className;

    public PluginLoader(JSONObject config) throws ClassNotFoundException, InstantiationException, IllegalAccessException, JSONException, IOException {
        this.name = config.getString("name");
        this.version = config.getString("version");
        this.className = config.getString("class-name");
        pluginClass = Class.forName(this.className);
        plugin = (IPlugin) pluginClass.newInstance();
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
        plugin.onKeyReleased(e);
    }

    public void onDraw(Graphics2D g) {
        plugin.onDraw(g);
    }

    public void onShutdown() throws IOException {
        plugin.onShutdown();
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() == PluginLoader.class) {
            PluginLoader plug = (PluginLoader) o;
            return name.equals(plug.name) && version.equals(plug.version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (className + " " + version).hashCode();
    }
}
