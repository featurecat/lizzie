# Theme
The theme's function is to make Lizzie's display richer, so simply change the assets file can not do things.

The way to load a theme is to copy the theme's Jar file to the theme directory, and then set the theme's class name in the configuration file.

Any class that implements the ITheme interface can be used as a theme class.

Use ```javac -classpath ...``` to ensure that the theme's source files can be imported into the ITheme class.

A theme class needs to implement the following methods:
```java
public Image getBlackStone(int[] position) throws IOException;

public Image getWhiteStone(int[] position) throws IOException;

 public Image getBoard() throws IOException;

 public Image getBackground() throws IOException;
```
