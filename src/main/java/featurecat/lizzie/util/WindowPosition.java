package featurecat.lizzie.util;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.gui.BasicInfoPane;
import featurecat.lizzie.gui.BoardPane;
import featurecat.lizzie.gui.CommentPane;
import featurecat.lizzie.gui.LizzieMain;
import featurecat.lizzie.gui.LizziePane;
import featurecat.lizzie.gui.SubBoardPane;
import featurecat.lizzie.gui.VariationTreePane;
import featurecat.lizzie.gui.WinratePane;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

public class WindowPosition {

  public static JSONObject create(JSONObject ui) {
    if (ui == null) {
      ui = new JSONObject();
    }

    // Main Window Position & Size
    ui.put("main-window-position", new JSONArray("[]"));
    ui.put("gtp-console-position", new JSONArray("[]"));
    ui.put("board-position-proportion", 4);
    // Panes
    ui.put("main-board-position", new JSONArray("[]"));
    ui.put("sub-board-position", new JSONArray("[]"));
    ui.put("basic-info-position", new JSONArray("[]"));
    ui.put("winrate-position", new JSONArray("[]"));
    ui.put("variation-tree-position", new JSONArray("[]"));
    ui.put("comment-position", new JSONArray("[]"));

    ui.put("window-maximized", false);
    ui.put("toolbar-position", "South");

    return ui;
  }

  public static JSONObject save(JSONObject ui) {
    if (ui == null) {
      ui = new JSONObject();
    }

    boolean windowIsMaximized = Lizzie.frame.getExtendedState() == JFrame.MAXIMIZED_BOTH;
    ui.put("window-maximized", windowIsMaximized);
    ui.put("toolbar-position", Lizzie.config.toolbarPosition);
    ui.put("board-position-proportion", Lizzie.frame.boardPositionProportion);

    JSONArray mainPos = new JSONArray();
    if (!windowIsMaximized) {
      mainPos.put(Lizzie.frame.getX());
      mainPos.put(Lizzie.frame.getY());
      mainPos.put(Lizzie.frame.getWidth());
      mainPos.put(Lizzie.frame.getHeight());
    }
    ui.put("main-window-position", mainPos);

    JSONArray gtpPos = new JSONArray();
    gtpPos.put(Lizzie.gtpConsole.getX());
    gtpPos.put(Lizzie.gtpConsole.getY());
    gtpPos.put(Lizzie.gtpConsole.getWidth());
    gtpPos.put(Lizzie.gtpConsole.getHeight());
    ui.put("gtp-console-position", gtpPos);

    // Panes
    if (Lizzie.frame instanceof LizzieMain) {
      LizzieMain main = (LizzieMain) Lizzie.frame;
      ui.put("main-board-position", getWindowPos(main.boardPane));
      ui.put("sub-board-position", getWindowPos(main.subBoardPane));
      ui.put("basic-info-position", getWindowPos(main.basicInfoPane));
      ui.put("winrate-position", getWindowPos(main.winratePane));
      ui.put("variation-tree-position", getWindowPos(main.variationTreePane));
      ui.put("comment-position", getWindowPos(main.commentPane));
    }

    return ui;
  }

  public static JSONArray mainWindowPos() {

    // Main
    boolean persisted = (Lizzie.config.persistedUi != null);
    if (persisted
        && Lizzie.config.persistedUi.optJSONArray("main-window-position") != null
        && Lizzie.config.persistedUi.optJSONArray("main-window-position").length() == 4) {
      return Lizzie.config.persistedUi.getJSONArray("main-window-position");
    } else {
      return null;
    }
  }

  public static JSONArray gtpWindowPos() {

    boolean persisted = Lizzie.config.persistedUi != null;
    if (persisted
        && Lizzie.config.persistedUi.optJSONArray("gtp-console-position") != null
        && Lizzie.config.persistedUi.optJSONArray("gtp-console-position").length() == 4) {
      return Lizzie.config.persistedUi.getJSONArray("gtp-console-position");
    } else {
      return null;
    }
  }

  public static void restorePane(JSONObject ui, LizziePane pane) {
    if (ui == null) {
      return;
    }
    JSONArray pos = getPersistedPanePos(pane, ui);
    if (pos != null) {
      pane.toWindow(
          new Point(pos.getInt(0), pos.getInt(1)), new Dimension(pos.getInt(2), pos.getInt(3)));
    }
  }

  public static JSONArray getWindowPos(LizziePane pane) {
    JSONArray panePos = new JSONArray("[]");
    Window paneWindow = SwingUtilities.getWindowAncestor(pane);
    if (!(paneWindow instanceof LizzieMain)) {
      Insets insets = paneWindow.getInsets();
      panePos.put(paneWindow.getX());
      panePos.put(paneWindow.getY());
      panePos.put(paneWindow.getWidth() - insets.left - insets.right);
      panePos.put(paneWindow.getHeight() - insets.top - insets.bottom);
    }
    return panePos;
  }

  public static JSONArray getPersistedPanePos(LizziePane pane, JSONObject ui) {
    String key = "";
    if (pane instanceof BoardPane) {
      key = "main-board-position";
    } else if (pane instanceof SubBoardPane) {
      key = "sub-board-position";
    } else if (pane instanceof BasicInfoPane) {
      key = "basic-info-position";
    } else if (pane instanceof WinratePane) {
      key = "winrate-position";
    } else if (pane instanceof VariationTreePane) {
      key = "variation-tree-position";
    } else if (pane instanceof CommentPane) {
      key = "comment-position";
    }
    JSONArray pos = null;
    if (!key.isEmpty()) {
      if (ui.optJSONArray(key) != null && ui.optJSONArray(key).length() == 4) {
        pos = ui.getJSONArray(key);
      }
    }
    return pos;
  }
}
