package wagner.stephanie.lizzie.theme;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * ITheme
 */
public interface ITheme {
    public static ITheme loadTheme(String name) {
        try {
            File themes = new File("theme");
            if (!themes.isDirectory()) {
                return null;
            }
            ArrayList<URL> jarFileList = new ArrayList<URL>();
            for (File file : themes.listFiles()) {
                if (file.canRead() && file.getName().endsWith(".jar")) {
                    jarFileList.add(file.toURI().toURL());
                }
            }
            URLClassLoader loader = new URLClassLoader(jarFileList.toArray(new URL[jarFileList.size()]));
            Class<?> theme = loader.loadClass(name);
            loader.close();
            return (ITheme) theme.newInstance();
        } catch (Exception e) {
            return new DefaultTheme();
        }
    }

    // Considering that the theme may implement different pieces for each coordinate, you need to pass in the coordinates.
    public Image getBlackStone(int[] position) throws IOException;

    public Image getWhiteStone(int[] position) throws IOException;

    public Image getBoard() throws IOException;

    public Image getBackground() throws IOException;
}
