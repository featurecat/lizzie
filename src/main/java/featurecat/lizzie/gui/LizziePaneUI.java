package featurecat.lizzie.gui;

import java.awt.Dimension;
import java.awt.Point;
import javax.swing.plaf.PanelUI;

public abstract class LizziePaneUI extends PanelUI {

  public abstract void toWindow(Point position, Dimension size);
}
