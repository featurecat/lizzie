package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;

public class UpdateEngine {
  private static final ResourceBundle resourceBundle = MainFrame.resourceBundle;

  public UpdateEngine() {}

  public void updateEngineMenu(List<Leelaz> engineList) {
    Menu.engine = new JMenuItem[engineList.size()];
    Menu.engineMenu.removeAll();
    for (int i = 0; i < engineList.size(); i++) {
      Menu.engine[i] = new JMenuItem();
      Menu.engineMenu.add(Menu.engine[i]);
      Menu.engine[i].setText(resourceBundle.getString("Menu.engineMenu") + i);
      Menu.engine[i].setVisible(false);
      Leelaz engineDt = engineList.get(i);
      if (engineDt != null) {
        if (engineDt.currentWeight() != "")
          Menu.engine[i].setText(Menu.engine[i].getText() + " : " + engineDt.currentWeight());
        Menu.engine[i].setVisible(true);
        int a = i;
        Menu.engine[i].addActionListener(
            new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                Lizzie.engineManager.switchEngine(a);
              }
            });
      }
    }
  }

  public void updateEngineIcon(List<Leelaz> engineList, int currentEngineNo) {
    if (Menu.engine != null) {
      for (int i = 0; i < engineList.size(); i++) {
        if (Menu.engine[i] != null) {
          Leelaz engineDt = engineList.get(i);
          if (engineDt != null) {
            if (i == currentEngineNo) {
              Menu.engine[i].setIcon(Menu.running);
              Menu.engineMenu.setText(Menu.engine[i].getText());
            } else if (engineDt.isLoaded()) Menu.engine[i].setIcon(Menu.ready);
            else if (Menu.engine[i].getIcon() != null) Menu.engine[i].setIcon(null);
          }
        }
      }
    }
  }
}
