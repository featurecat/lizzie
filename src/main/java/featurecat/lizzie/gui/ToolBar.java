package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

public class ToolBar extends JToolBar {
  public JTextField txtMoveNumber;
  private static final ResourceBundle resourceBundle = MainFrame.resourceBundle;

  public ToolBar() {
    JButton open = new JButton(resourceBundle.getString("ToolBar.open"));
    open.setFocusable(false);
    open.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openFile();
          }
        });
    add(open);
    addSeparator();

    JButton save = new JButton(resourceBundle.getString("ToolBar.save"));
    save.setFocusable(false);
    save.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.saveFile();
          }
        });
    add(save);
    addSeparator();

    JButton analyze = new JButton(resourceBundle.getString("ToolBar.analyze"));
    analyze.setFocusable(false);
    analyze.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.leelaz.togglePonder();
          }
        });
    add(analyze);
    addSeparator();

    JButton kataEstimate = new JButton(resourceBundle.getString("ToolBar.kataEstimate"));
    kataEstimate.setFocusable(false);
    kataEstimate.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = !Lizzie.config.showKataGoEstimate;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    add(kataEstimate);
    addSeparator();

    JButton estimate = new JButton(resourceBundle.getString("ToolBar.estimate"));
    estimate.setFocusable(false);
    estimate.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.isEstimating) {
              Lizzie.frame.noEstimateByZen();
            } else {
              Lizzie.frame.estimateByZen();
            }
          }
        });
    add(estimate);
    addSeparator();

    JButton backMain = new JButton(resourceBundle.getString("ToolBar.backMain"));
    backMain.setFocusable(false);
    backMain.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (!Lizzie.board.getHistory().getCurrentHistoryNode().isMainTrunk()) {
              Lizzie.board.previousMove();
            }
          }
        });
    add(backMain);
    addSeparator();

    JButton setMain = new JButton(resourceBundle.getString("ToolBar.setMain"));
    setMain.setFocusable(false);
    setMain.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.setAsMainBranch()) ;
          }
        });
    add(setMain);
    addSeparator();

    JButton clearBoard = new JButton(resourceBundle.getString("ToolBar.clearBoard"));
    clearBoard.setFocusable(false);
    clearBoard.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.clear();
            Lizzie.frame.refresh(2);
          }
        });
    add(clearBoard);
    addSeparator();

    JButton move = new JButton(resourceBundle.getString("ToolBar.move"));
    move.setFocusable(false);
    move.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowMoveNumber();
            Lizzie.frame.refresh(1);
          }
        });
    add(move);
    addSeparator();

    JButton coords = new JButton(resourceBundle.getString("ToolBar.coords"));
    coords.setFocusable(false);
    coords.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleCoordinates();
            Lizzie.frame.refresh(1);
          }
        });
    add(coords);
    addSeparator();

    JButton gotoFirst = new JButton("|<");
    gotoFirst.setFocusable(false);
    gotoFirst.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.previousMove()) ;
          }
        });
    add(gotoFirst);

    JButton back10 = new JButton("<<");
    back10.setFocusable(false);
    back10.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Input.undo(10);
          }
        });
    add(back10);

    JButton back1 = new JButton("<");
    back1.setFocusable(false);
    back1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Input.undo(1);
          }
        });
    add(back1);

    JButton forward1 = new JButton(">");
    forward1.setFocusable(false);
    forward1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Input.redo(1);
          }
        });
    add(forward1);

    JButton forward10 = new JButton(">>");
    forward10.setFocusable(false);
    forward10.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Input.redo(1);
          }
        });
    add(forward10);

    JButton gotoEnd = new JButton(">|");
    gotoEnd.setFocusable(false);
    gotoEnd.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.nextMove()) ;
          }
        });
    add(gotoEnd);
    addSeparator();

    txtMoveNumber = new JTextField();
    JPanel panel = new JPanel(null);
    panel.setPreferredSize(new Dimension(100, 20));
    txtMoveNumber.setBounds(2, 1, 30, 18);
    panel.add(txtMoveNumber);

    JButton gotoMove = new JButton(resourceBundle.getString("ToolBar.gotoMove"));
    gotoMove.setFocusable(false);
    gotoMove.setMargin(new Insets(0, 0, 0, 0));
    gotoMove.setBounds(32, 0, 40, 20);
    gotoMove.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
              Lizzie.board.goToMoveNumberBeyondBranch(Integer.parseInt(txtMoveNumber.getText()));
            } catch (Exception ex) {
            }
            setTxtUnfoucus();
          }
        });
    panel.add(gotoMove);

    add(panel);
  }

  public void setTxtUnfoucus() {
    if (txtMoveNumber.isFocusOwner()) {
      txtMoveNumber.setFocusable(false);
      txtMoveNumber.setFocusable(true);
    }
  }
}
