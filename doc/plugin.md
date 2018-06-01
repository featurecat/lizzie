# Developing plug-ins for Lizzie


The Jar file for a plugin should look like this:

./plugin
./plugin/Plugin.class
./META-INF
./META-INF/MANIFEST.MF
./...Your other classes

### Start first step

Create a "HelloWorld" directory and create a "plugin" directory in it, and create a new "Plugin.java" file in the "plugin" directory.

And write the following

```java
package plugin;

import java.io.IOException;

import IPlugin;

/**
 * Plugin
 */
public class Plugin implements IPlugin {
    public static String name = "Hello World";
    public static String version = "0.0.0";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
```

This is the most basic framework of a plugin where ``` name ``` and ``` version ``` are used to generate a hash of this plugin.

Let's edit Plugin.java to have a message box pop up when we press 'H'.

```java
    @Override
    public void onKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_H) {
            JOptionPane.showConfirmDialog(null, "Hello World!");
        }
    }
```

And add the import statement at the beginning of the file

```java
import java.swt.KeyEvent;
import javax.swing.JOptionPane;
```

Copy lizzie.jar to "HelloWorld" directory, and execute the command:

```
javac -classpath lizzie.jar ./plugin/Plugin.java
jar -cvf HelloWorld.jar ./plugin/Plugin.class
```

Copy the generated "HelloWorld.jar" to the "plugin" directory under the Lizzie directory.

Start the lizzie and press 'H'!

### More

The plugin can freely call Lizzie's own classes and interfaces, so it can do a lot of functions.

In the future, plugins may be able to enable and disable certain functions, so it is possible to do very complete functions with plug-ins.

### Methods for plug-ins

In ``` Lizzie.config ```:

``` public boolean showBranch ```

``` public boolean showBestMoves ```

``` public void toggleShowBranch() ```

``` public void toggleShowBestMoves() ```

### Callback functions

Basic callback function

``` public abstract String getName() ``` Should be override, return plug-in 's name.
``` public abstract String getVersion()  ``` Should be override, return plug-in 's version.

``` public void onInit() throws IOException ``` Called when the plugin is loaded, the plugin is loaded after loading the configuration file.

``` public void onMousePressed(MouseEvent e) ```
``` public void onMouseReleased(MouseEvent e) ```
``` public void onMouseMoved(MouseEvent e) ```
``` public void onKeyPressed(KeyEvent e) ```
``` public void onKeyReleased(KeyEvent e) ``` Like its name, it is called in the corresponding event.

``` public boolean onDraw(Graphics2D g) ``` Called when drawing, the return value represents whether to submit the drawing result.

``` public void onShutdown() throws IOException ``` Called when the program is shut down.


Special callback function

``` public void onSgfLoaded() ```
