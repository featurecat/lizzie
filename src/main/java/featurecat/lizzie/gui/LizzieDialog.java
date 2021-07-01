package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.Locale;
import javax.swing.JDialog;

// Use `LizzieDialog` instead of `JDialog` to avoid empty dialogs like #616 on Linux.

public class LizzieDialog extends JDialog {
  private String osName;

  public LizzieDialog() {
    super(Lizzie.frame); // Set owner for dual monitors. (#885)
    setOsName();
  }

  public LizzieDialog(Window owner) {
    super(owner);
    setOsName();
  }

  public LizzieDialog(Frame owner, String title, boolean modal) {
    super(owner, title, modal);
    setOsName();
  }

  public LizzieDialog(Dialog owner, String title, boolean modal) {
    super(owner, title, modal);
    setOsName();
  }

  private void setOsName() {
    osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
  }

  public void setType(Window.Type type) {
    // avoid suspicious behavior on Linux (#616)
    if (type == Type.POPUP && !isWindows()) return;
    super.setType(type);
  }

  public boolean isWindows() {
    return osName != null && !osName.contains("darwin") && osName.contains("win");
  }
}
