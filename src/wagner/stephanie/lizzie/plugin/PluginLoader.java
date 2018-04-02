package wagner.stephanie.lizzie.plugin;


import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;
import wagner.stephanie.lizzie.Lizzie;


public final class PluginLoader extends URLClassLoader{
    public Class pluginClass;
    public IPlugin plugin;
    public String name;
    public String version;
    public String className;

    public PluginLoader(String uri) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        super(new URL[]{});
        addURL(new File(uri).toURI().toURL());
        pluginClass = loadClass("plugin.Plugin");
        plugin = (IPlugin) pluginClass.newInstance();
        plugin.onInit(Lizzie.class);
        name = plugin.getName();
        version = plugin.getVersion();
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

    public boolean onDraw(Graphics2D g) {
        return plugin.onDraw(g);
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
        return (name + " " + version).hashCode();
    }
}
