package featurecat.lizzie.theme;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;
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

  private String themeName = null;
  private String configFile = "theme.txt";
  private String pathPrefix = "theme";
  private String path = null;
  private JSONObject config = new JSONObject();

  public Theme(String themeName) {
    this.themeName = themeName;
    this.path = this.pathPrefix + File.separator + this.themeName;
    File file = new File(this.path + File.separator + this.configFile);
    if (file.canRead()) {
      FileInputStream fp;
      try {
        fp = new FileInputStream(file);
        config = new JSONObject(new JSONTokener(fp));
        fp.close();
      } catch (FileNotFoundException e) {
      } catch (IOException e) {
      } catch (JSONException e) {
      }
    }
  }

  public BufferedImage blackStone() {
    if (blackStoneCached == null) {
      try {
        blackStoneCached =
            ImageIO.read(
                new File(
                    this.path
                        + File.separator
                        + config.optString("black-stone-image", "black.png")));
      } catch (IOException e) {
        try {
          blackStoneCached = ImageIO.read(getClass().getResourceAsStream("/assets/black0.png"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    return blackStoneCached;
  }

  public BufferedImage whiteStone() {
    if (whiteStoneCached == null) {
      try {
        whiteStoneCached =
            ImageIO.read(
                new File(
                    this.path
                        + File.separator
                        + config.optString("white-stone-image", "white.png")));
      } catch (IOException e) {
        try {
          whiteStoneCached = ImageIO.read(getClass().getResourceAsStream("/assets/white0.png"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    return whiteStoneCached;
  }

  public BufferedImage board() {
    if (boardCached == null) {
      try {
        boardCached =
            ImageIO.read(
                new File(
                    this.path + File.separator + config.optString("board-image", "board.png")));
      } catch (IOException e) {
        try {
          boardCached = ImageIO.read(getClass().getResourceAsStream("/assets/board.png"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    return boardCached;
  }

  public BufferedImage background() {
    if (backgroundCached == null) {
      try {
        backgroundCached =
            ImageIO.read(
                new File(
                    this.path
                        + File.separator
                        + config.optString("background-image", "background.png")));
      } catch (IOException e) {

        try {
          backgroundCached = ImageIO.read(getClass().getResourceAsStream("/assets/background.jpg"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    return backgroundCached;
  }

  /** Use custom font */
  public String fontName() {
    return config.optString("font-name", null);
  }

  /** Use solid current stone indicator */
  public boolean solidStoneIndicator() {
    return config.optBoolean("solid-stone-indicator");
  }

  /**
   * The background color of the comment panel
   *
   * @return
   */
  public Color commentBackgroundColor() {
    JSONArray color = config.optJSONArray("comment-background-color");
    if (color != null) {
      return new Color(color.getInt(0), color.getInt(1), color.getInt(2), color.getInt(3));
    } else {
      return new Color(0, 0, 0, 200);
    }
  }

  /** The font color of the comment */
  public Color commentFontColor() {
    JSONArray color = config.optJSONArray("comment-font-color");
    if (color != null) {
      return new Color(color.getInt(0), color.getInt(1), color.getInt(2), color.getInt(3));
    } else {
      return Color.WHITE;
    }
  }

  /** The color of the winrate line */
  public Color winrateLineColor() {
    JSONArray color = config.optJSONArray("winrate-line-color");
    if (color != null) {
      return new Color(color.getInt(0), color.getInt(1), color.getInt(2), color.getInt(3));
    } else {
      return Color.green;
    }
  }

  /** The color of the line that missed the winrate */
  public Color winrateMissLineColor() {
    JSONArray color = config.optJSONArray("winrate-miss-line-color");
    if (color != null) {
      return new Color(color.getInt(0), color.getInt(1), color.getInt(2), color.getInt(3));
    } else {
      return Color.blue.darker();
    }
  }

  /** The color of the blunder bar */
  public Color blunderBarColor() {
    JSONArray color = config.optJSONArray("blunder-bar-color");
    if (color != null) {
      return new Color(color.getInt(0), color.getInt(1), color.getInt(2), color.getInt(3));
    } else {
      return new Color(255, 0, 0, 150);
    }
  }
}
