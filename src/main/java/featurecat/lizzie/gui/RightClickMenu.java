package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class RightClickMenu extends JPopupMenu {
  public final ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");
  public static int mousex;
  public static int mousey;
  private JMenuItem insertmode;
  private JMenuItem addblack;
  private JMenuItem addwhite;
  private JMenuItem addone;
  private JMenuItem quitinsert;
  
  public RightClickMenu() {
    insertmode = new JMenuItem(resourceBundle.getString("LizzieRightClickMenu.button.insertmode"));
    quitinsert = new JMenuItem(resourceBundle.getString("LizzieRightClickMenu.button.quitinsert"));
    addblack = new JMenuItem(resourceBundle.getString("LizzieRightClickMenu.button.addblack"));
    addwhite = new JMenuItem(resourceBundle.getString("LizzieRightClickMenu.button.addwhite"));
    addone = new JMenuItem(resourceBundle.getString("LizzieRightClickMenu.button.addone"));

    this.add(insertmode);

    insertmode.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            //System.out.println("进入插入棋子模式");
            insertmode();
          }
        });
    quitinsert.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            //System.out.println("退出插入棋子模式");
            quitinsertmode();
          }
        });
    addblack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            //System.out.println("增加黑子");
            addblack();
          }
        });
    addwhite.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
           // System.out.println("增加白子");
            addwhite();
          }
        });
    addone.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
          //  System.out.println("轮流增加棋子");
            addone();
          }
        });
  }


  private void insertmode() {

    boolean isinsertmode;
    isinsertmode = Lizzie.board.insertmode();
    if (isinsertmode) {
      this.remove(insertmode);
      this.add(addblack);
      this.add(addwhite);
      this.add(addone);
      this.add(quitinsert);
    }
    if (Lizzie.leelaz.isPondering()) {
      Lizzie.leelaz.ponder();
    }
  }

  private void quitinsertmode() {
    this.remove(quitinsert);
    this.remove(addblack);
    this.remove(addwhite);
    this.remove(addone);
    this.add(insertmode);
    Lizzie.board.quitinsertmode();
    if (Lizzie.leelaz.isPondering()) {
      Lizzie.leelaz.ponder();
    }
  }

  private void addblack() {

    Lizzie.frame.insertMove(mousex, mousey, true);
  }


  private void addwhite() {
    Lizzie.frame.insertMove(mousex, mousey, false);
  }

  private void addone() {
    Lizzie.frame.insertMove(mousex, mousey);
  }
 


  public void Store(int x, int y) {
    mousex = x;
    mousey = y;
  }
}
