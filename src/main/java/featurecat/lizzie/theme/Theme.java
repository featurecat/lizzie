package featurecat.lizzie.theme;

import static java.io.File.separator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Theme Allow to load the external image & theme config */
public class Theme {
  BufferedImage blackStoneCached = null;
  BufferedImage whiteStoneCached = null;
  BufferedImage boardCached = null;
  BufferedImage backgroundCached = null;

  private String configFile = "theme.txt";
  private String pathPrefix = "theme" + separator;
  private String path = null;
  private JSONObject config = new JSONObject();
  private JSONObject uiConfig = null;
  private Optional<List<Double>> blunderWinrateThresholds = Optional.empty();

  public Theme(JSONObject uiConfig) {
    this.uiConfig = uiConfig;
    String themeName = uiConfig.optString("theme");
    this.path = this.pathPrefix + (themeName.isEmpty() ? "" : themeName + separator);
    File file = new File(this.path + this.configFile);
    if (file.canRead()) {
      FileInputStream fp;
      try {
        fp = new FileInputStream(file);
        config = new JSONObject(new JSONTokener(fp));
        fp.close();
      } catch (IOException e) {
      } catch (JSONException e) {
      }
    }
  }

  public BufferedImage blackStone() {
    if (blackStoneCached == null) {
      blackStoneCached = getImageByKey("black-stone-image", "black.png", "black0.png");
    }
    return blackStoneCached;
  }

  public BufferedImage whiteStone() {
    if (whiteStoneCached == null) {
      whiteStoneCached = getImageByKey("white-stone-image", "white.png", "white0.png");
    }
    return whiteStoneCached;
  }

  public BufferedImage board() {
    if (boardCached == null) {
      boardCached = getImageByKey("board-image", "board.png", "board.png");
    }
    return boardCached;
  }

  public BufferedImage background() {
    if (backgroundCached == null) {
      backgroundCached = getImageByKey("background-image", "background.png", "background.jpg");
    }
    return backgroundCached;
  }

  /** Use custom font for general text */
  public String fontName() {
    String key = "font-name";
    return config.optString(key, uiConfig.optString(key, new JLabel().getFont().getFontName()));
  }

  /** Use custom font for the UI */
  public String uiFontName() {
    String key = "ui-font-name";
    return config.optString(key, uiConfig.optString(key, null));
  }

  /** Use custom font for the Leela Zero winrate on the stone */
  public String winrateFontName() {
    String key = "winrate-font-name";
    return config.optString(key, uiConfig.optString(key, null));
  }

  /** Use solid current stone indicator */
  public boolean solidStoneIndicator() {
    String key = "solid-stone-indicator";
    return config.optBoolean(key, uiConfig.optBoolean(key));
  }

  /** Show the node with the comment color */
  public boolean showCommentNodeColor() {
    String key = "show-comment-node-color";
    return config.optBoolean(key, uiConfig.optBoolean(key, true));
  }

  /** The size of the shadow */
  public int shadowSize() {
    return getIntByKey("shadow-size", 100);
  }

  /** The stroke width of the winrate line */
  public int winrateStrokeWidth() {
    return getIntByKey("winrate-stroke-width", 3);
  }

  /** The minimum width of the blunder bar */
  public int minimumBlunderBarWidth() {
    return getIntByKey("minimum-blunder-bar-width", 3);
  }

  /** The font size of the comment */
  public int commentFontSize() {
    return getIntByKey("comment-font-size", 3);
  }

  /** The size of the shadow */
  public int nodeColorMode() {
    return getIntByKey("node-color-mode", 0);
  }

  /**
   * The background color of the comment panel
   *
   * @return
   */
  public Color commentBackgroundColor() {
    return getColorByKey("comment-background-color", new Color(0, 0, 0, 200));
  }

  /** The font color of the comment */
  public Color commentFontColor() {
    return getColorByKey("comment-font-color", Color.WHITE);
  }

  /** The color of the node with the comment */
  public Color commentNodeColor() {
    return getColorByKey("comment-node-color", Color.BLUE.brighter());
  }

  /** The color of the winrate line */
  public Color winrateLineColor() {
    return getColorByKey("winrate-line-color", Color.green);
  }

  /** The color of the line that missed the winrate */
  public Color winrateMissLineColor() {
    return getColorByKey("winrate-miss-line-color", Color.blue.darker());
  }

  /** The color of the blunder bar */
  public Color blunderBarColor() {
    return getColorByKey("blunder-bar-color", new Color(255, 0, 0, 150));
  }

  /** The threshold list of the blunder winrate */
  public Optional<List<Double>> blunderWinrateThresholds() {
    String key = "blunder-winrate-thresholds";
    Optional<JSONArray> array = Optional.ofNullable(config.optJSONArray(key));
    if (!array.isPresent()) {
      array = Optional.ofNullable(uiConfig.optJSONArray(key));
    }
    array.ifPresent(
        m -> {
          blunderWinrateThresholds = Optional.of(new ArrayList<Double>());
          m.forEach(a -> blunderWinrateThresholds.get().add(new Double(a.toString())));
        });
    return blunderWinrateThresholds;
  }

  /** The color list of the blunder node */
  public Optional<Map<Double, Color>> blunderNodeColors() {
    Optional<Map<Double, Color>> map = Optional.of(new HashMap<Double, Color>());
    String key = "blunder-node-colors";
    Optional<JSONArray> array = Optional.ofNullable(config.optJSONArray(key));
    if (!array.isPresent()) {
      array = Optional.ofNullable(uiConfig.optJSONArray(key));
    }
    array.ifPresent(
        a -> {
          IntStream.range(0, a.length())
              .forEach(
                  i -> {
                    Color color = array2Color((JSONArray) a.get(i), null);
                    blunderWinrateThresholds.map(l -> l.get(i)).map(t -> map.get().put(t, color));
                  });
        });
    return map;
  }

  private Color getColorByKey(String key, Color defaultColor) {
    Color color = array2Color(config.optJSONArray(key), null);
    if (color == null) {
      color = array2Color(uiConfig.optJSONArray(key), defaultColor);
    }
    return color;
  }

  /** Convert option color array to Color */
  private Color array2Color(JSONArray a, Color defaultColor) {
    if (a != null) {
      if (a.length() == 3) {
        return new Color(a.getInt(0), a.getInt(1), a.getInt(2));
      } else if (a.length() == 4) {
        return new Color(a.getInt(0), a.getInt(1), a.getInt(2), a.getInt(3));
      }
    }
    return defaultColor;
  }

  private BufferedImage getImageByKey(String key, String defaultValue, String defaultImg) {
    BufferedImage image = null;
    String p = this.path + config.optString(key, defaultValue);
    try {
      image = ImageIO.read(new File(p));
    } catch (IOException e) {
      try {
        p = this.pathPrefix + uiConfig.optString(key, defaultValue);
        image = ImageIO.read(new File(p));
      } catch (IOException e1) {
        try {
          image = ImageIO.read(getClass().getResourceAsStream("/assets/" + defaultImg));
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
    return image;
  }

  private int getIntByKey(String key, int defaultValue) {
    return config.optInt(key, uiConfig.optInt(key, defaultValue));
  }
}
