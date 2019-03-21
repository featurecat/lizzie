package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;

public class AvoidMoveDialog extends JDialog {
  public final ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");
  private JRadioButton rdoAvoid;
  private JRadioButton rdoAllow;
  private JRadioButton rdoBlack;
  private JRadioButton rdoWhite;
  private JFormattedTextField txtUntilMove;
  private JTextField txtCoordList;
  private JTextField txtParam;

  public AvoidMoveDialog() {
    setTitle(resourceBundle.getString("LizzieAvoidMove.title.config"));
    setModalityType(ModalityType.APPLICATION_MODAL);
    setType(Type.POPUP);
    setBounds(100, 100, 416, 282);
    getContentPane().setLayout(new BorderLayout());
    JPanel buttonPane = new JPanel();
    getContentPane().add(buttonPane, BorderLayout.CENTER);
    JButton okButton = new JButton(resourceBundle.getString("LizzieAvoidMove.button.ok"));
    okButton.setBounds(112, 208, 74, 29);
    okButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
            applyChange();
          }
        });
    buttonPane.setLayout(null);
    okButton.setActionCommand("OK");
    buttonPane.add(okButton);
    getRootPane().setDefaultButton(okButton);

    JButton cancelButton = new JButton(resourceBundle.getString("LizzieAvoidMove.button.cancel"));
    cancelButton.setBounds(218, 208, 74, 29);
    cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });
    cancelButton.setActionCommand("Cancel");
    buttonPane.add(cancelButton);

    JLabel lblCoordList = new JLabel(resourceBundle.getString("LizzieAvoidMove.title.coordList"));
    lblCoordList.setBounds(10, 87, 101, 14);
    buttonPane.add(lblCoordList);
    lblCoordList.setHorizontalAlignment(SwingConstants.LEFT);

    NumberFormat nf = NumberFormat.getIntegerInstance();
    nf.setGroupingUsed(false);

    txtCoordList = new JFormattedTextField();
    txtCoordList.setBounds(110, 84, 283, 20);
    buttonPane.add(txtCoordList);
    txtCoordList.setColumns(10);

    ButtonGroup group = new ButtonGroup();

    rdoAllow = new JRadioButton(resourceBundle.getString("LizzieAvoidMove.rdoAllow.text"));
    rdoAllow.setBounds(187, 32, 69, 23);
    buttonPane.add(rdoAllow);
    group.add(rdoAllow);

    rdoAvoid = new JRadioButton(resourceBundle.getString("LizzieAvoidMove.rdoAvoid.text"));
    rdoAvoid.setBounds(112, 32, 74, 23);
    rdoAvoid.setSelected(true);
    buttonPane.add(rdoAvoid);
    group.add(rdoAvoid);

    JLabel lblUtilMove = new JLabel(resourceBundle.getString("LizzieAvoidMove.title.untilMove"));
    lblUtilMove.setBounds(10, 112, 74, 14);
    buttonPane.add(lblUtilMove);

    txtUntilMove =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtUntilMove.setBounds(110, 109, 47, 20);
    buttonPane.add(txtUntilMove);
    txtUntilMove.setColumns(10);

    JLabel lblPrompt1 = new JLabel(resourceBundle.getString("LizzieAvoidMove.lblPrompt1.text"));
    lblPrompt1.setBounds(10, 11, 383, 14);
    buttonPane.add(lblPrompt1);

    JLabel lblType = new JLabel(resourceBundle.getString("LizzieAvoidMove.lblType.text"));
    lblType.setBounds(10, 36, 74, 14);
    buttonPane.add(lblType);

    JLabel lblColor = new JLabel(resourceBundle.getString("LizzieAvoidMove.lblColor.text"));
    lblColor.setBounds(10, 62, 74, 14);
    buttonPane.add(lblColor);

    ButtonGroup colorGroup = new ButtonGroup();
    rdoBlack = new JRadioButton(resourceBundle.getString("LizzieAvoidMove.rdoBlack.text"));
    rdoBlack.setBounds(112, 57, 74, 23);
    rdoBlack.setSelected(true);
    buttonPane.add(rdoBlack);
    colorGroup.add(rdoBlack);

    rdoWhite = new JRadioButton(resourceBundle.getString("LizzieAvoidMove.rdoWhite.text"));
    rdoWhite.setBounds(187, 57, 74, 23);
    buttonPane.add(rdoWhite);
    colorGroup.add(rdoWhite);

    txtParam = new JTextField();
    txtParam.setColumns(10);
    txtParam.setBounds(110, 176, 283, 20);
    buttonPane.add(txtParam);

    JLabel lblParam = new JLabel(resourceBundle.getString("LizzieAvoidMove.lblParam.text"));
    lblParam.setHorizontalAlignment(SwingConstants.LEFT);
    lblParam.setBounds(10, 180, 74, 14);
    buttonPane.add(lblParam);

    JLabel lblPrompt2 = new JLabel(resourceBundle.getString("LizzieAvoidMove.lblPrompt2.text"));
    lblPrompt2.setBounds(10, 157, 349, 14);
    buttonPane.add(lblPrompt2);

    setLocationRelativeTo(getOwner());
  }

  private void applyChange() {
    if (checkInput()) {
      if (txtParam.getText() != null && !txtParam.getText().trim().isEmpty()) {
        Lizzie.leelaz.analyzeAvoid(txtParam.getText().trim());
      } else {
        Lizzie.leelaz.analyzeAvoid(
            getVisitType(), getColor(), txtCoordList.getText().trim(), getUntilMove());
      }
    }
  }

  private Integer txtFieldValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()
        || txt.getText().trim().length() >= String.valueOf(Integer.MAX_VALUE).length()) {
      return 0;
    } else {
      return Integer.parseInt(txt.getText().trim());
    }
  }

  private class DigitOnlyFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
        throws BadLocationException {
      String newStr = string != null ? string.replaceAll("\\D++", "") : "";
      if (!newStr.isEmpty()) {
        fb.insertString(offset, newStr, attr);
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
        throws BadLocationException {
      String newStr = text != null ? text.replaceAll("\\D++", "") : "";
      if (!newStr.isEmpty()) {
        fb.replace(offset, length, newStr, attrs);
      }
    }
  }

  private boolean checkInput() {
    if ((txtCoordList.getText() == null || txtCoordList.getText().isEmpty())
        && (txtParam.getText() == null || txtParam.getText().isEmpty())) {
      return false;
    }
    return true;
  }

  private String getVisitType() {
    if (rdoAvoid.isSelected()) {
      return "avoid";
    } else if (rdoAllow.isSelected()) {
      return "allow";
    } else {
      return "";
    }
  }

  private String getColor() {
    if (rdoBlack.isSelected()) {
      return "b";
    } else if (rdoWhite.isSelected()) {
      return "w";
    } else {
      return "";
    }
  }

  private Integer getUntilMove() {
    return txtFieldValue(txtUntilMove);
  }
}
