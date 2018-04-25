# Theme
The theme's function is to make Lizzie's display richer, so simply change the assets file can not do things.

The way to load a theme is to copy the theme's Jar file to the theme directory, and then set the theme's class name in the configuration file.

Any class that implements the wagner.stephanie.lizzie.theme.ITheme interface can be used as a theme class.

Use ```javac -classpath ...``` to ensure that the theme's source files can be imported into the ITheme class.

A theme class needs to implement the following methods:
```java
public Image getBlackStone(int[] position) throws IOException;

public Image getWhiteStone(int[] position) throws IOException;

 public Image getBoard() throws IOException;

 public Image getBackground() throws IOException;
```

If you just want to replace the image file, create the assets directory. And use ``` wagner.staphanie.lizzie.theme.FromFileTheme ``` in the theme column in the configuration file. Then the assets 

directory should look like this:
* Backgroung.jpg: Your background image.
* Board.png: Your board image.
* Black0.png: Your black stone image.
* White0.png: Your white stone image.
