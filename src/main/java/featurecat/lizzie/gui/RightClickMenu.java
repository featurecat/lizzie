package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class RightClickMenu extends JPopupMenu {
  public int mousex;
  public int mousey;

  private JMenuItem allow;
  private JMenuItem addAllow;
  private JMenuItem avoid;
  private JMenuItem addSuggestionAsBranch;
  private JCheckBoxMenuItem keepingAvoid;
  private static JMenuItem clearAvoidAllow;
  final ResourceBundle resourceBundle = MainFrame.resourceBundle;

  public RightClickMenu() {

    PopupMenuListener listener =
        new PopupMenuListener() {
          public void popupMenuCanceled(PopupMenuEvent e) {}

          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            if (Lizzie.leelaz.isPondering()) {
              Lizzie.leelaz.ponder();
            }
            Lizzie.frame.isShowingRightMenu = false;
          }

          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        };

    this.addPopupMenuListener(listener);

    addSuggestionAsBranch =
        new JMenuItem(resourceBundle.getString("RightClickMenu.addSuggestionAsBranch"));
    allow = new JMenuItem(resourceBundle.getString("RightClickMenu.allow"));
    addAllow = new JMenuItem(resourceBundle.getString("RightClickMenu.addAllow"));
    avoid = new JMenuItem(resourceBundle.getString("RightClickMenu.avoid"));
    keepingAvoid = new JCheckBoxMenuItem(resourceBundle.getString("RightClickMenu.keepingAvoid"));
    clearAvoidAllow = new JMenuItem(resourceBundle.getString("RightClickMenu.clearAvoidAllow"));

    this.add(addSuggestionAsBranch);
    this.add(allow);
    this.add(addAllow);
    this.add(avoid);
    this.add(keepingAvoid);
    this.add(clearAvoidAllow);
    if (Lizzie.config.panelUI && Lizzie.frame.isMouseOver) {
      addSuggestionAsBranch.setVisible(true);
    } else {
      addSuggestionAsBranch.setVisible(false);
    }
    if (Lizzie.board.allowCoords != "") {
      addAllow.setVisible(true);
    } else {
      addAllow.setVisible(false);
    }
    if (Lizzie.board.isKeepingAvoid) {
      keepingAvoid.setState(true);
    } else {
      keepingAvoid.setState(false);
    }
    if (Lizzie.leelaz.isKataGo) {
      allow.setVisible(false);
      addAllow.setVisible(false);
      avoid.setVisible(false);
      keepingAvoid.setVisible(false);
      clearAvoidAllow.setVisible(false);
    }
    addSuggestionAsBranch.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            addSuggestionAsBranch();
          }
        });

    addAllow.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            addAllow();
          }
        });

    allow.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            allow();
          }
        });
    avoid.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            avoid();
          }
        });
    keepingAvoid.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            keepingAvoid();
          }
        });

    clearAvoidAllow.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            clearAvoidAllow();
          }
        });
  }

  private void addSuggestionAsBranch() {
    Lizzie.frame.addSuggestionAsBranch();
  }

  private void clearAvoidAllow() {
    Lizzie.board.allowCoords = "";
    Lizzie.board.avoidCoords = "";
    Lizzie.board.isForcing = false;
    Lizzie.board.isAllowing = false;
    Lizzie.board.isAvoding = false;
    Lizzie.board.isKeepingAvoid = false;
    Lizzie.leelaz.ponder();
  }

  private void allow() {
    if (Lizzie.board.setAllowCoords(mousex, mousey)) {
      Lizzie.board.isForcing = true;
      Lizzie.board.isAllowing = true;
      Lizzie.board.isAvoding = false;
      Lizzie.leelaz.analyzeAvoid(
          "allow",
          Lizzie.board.getHistory().isBlacksTurn() ? "b" : "w",
          Lizzie.board.allowCoords,
          1);
      Lizzie.board.getHistory().getData().tryToClearBestMoves();
    }
  }

  private void addAllow() {
    Lizzie.board.addAllowCoords(mousex, mousey);
    Lizzie.leelaz.analyzeAvoid(
        "allow", Lizzie.board.getHistory().isBlacksTurn() ? "b" : "w", Lizzie.board.allowCoords, 1);
    Lizzie.board.getHistory().getData().tryToClearBestMoves();
  }

  public void avoid() {
    Lizzie.board.setAvoidCoords(mousex, mousey);
    Lizzie.board.isForcing = true;
    Lizzie.board.isAvoding = true;
    Lizzie.board.isAllowing = false;
    Lizzie.leelaz.analyzeAvoid(
        "avoid",
        Lizzie.board.getHistory().isBlacksTurn() ? "b" : "w",
        Lizzie.board.avoidCoords,
        Lizzie.config.config.getJSONObject("leelaz").getInt("avoid-keep-variations"));
    Lizzie.board.getHistory().getData().tryToClearBestMoves();
  }

  private void keepingAvoid() {
    Lizzie.board.isKeepingAvoid = !Lizzie.board.isKeepingAvoid;
    if (Lizzie.board.isAvoding && Lizzie.board.isKeepingAvoid) {
      Lizzie.leelaz.analyzeAvoid(
          "avoid",
          Lizzie.board.getHistory().isBlacksTurn() ? "b" : "w",
          Lizzie.board.avoidCoords,
          Lizzie.config.config.getJSONObject("leelaz").getInt("avoid-keep-variations"));
      Lizzie.board.getHistory().getData().tryToClearBestMoves();
    }
  }

  public void storeXY(int x, int y) {
    mousex = x;
    mousey = y;
  }
}
