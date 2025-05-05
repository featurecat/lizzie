package featurecat.lizzie.gui;

import javax.swing.JTextField;

// Ensure that the beginning of long text is visible after setText.

public class LeftTextField extends JTextField {

  public void setText(String t) {
    super.setText(t);
    setCaretPosition(0);
  }
}
