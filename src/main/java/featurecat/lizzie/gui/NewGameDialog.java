/*
 * Created by JFormDesigner on Wed Apr 04 22:17:33 CEST 2018
 */

package featurecat.lizzie.gui;

import featurecat.lizzie.analysis.GameInfo;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** @author unknown */
public class NewGameDialog extends JDialog {
  // create formatters
  public static final DecimalFormat FORMAT_KOMI = new DecimalFormat("#0.0");
  public static final DecimalFormat FORMAT_HANDICAP = new DecimalFormat("0");
  public static final JLabel PLACEHOLDER = new JLabel("");

  static {
    FORMAT_HANDICAP.setMaximumIntegerDigits(1);
  }

  private JPanel dialogPane = new JPanel();
  private JPanel contentPanel = new JPanel();
  private JPanel buttonBar = new JPanel();
  private JButton okButton = new JButton();

  private JCheckBox checkBoxPlayerIsBlack;
  private JTextField textFieldBlack;
  private JTextField textFieldWhite;
  private JTextField textFieldKomi;
  private JTextField textFieldHandicap;

  private boolean cancelled = true;
  private GameInfo gameInfo;

  public NewGameDialog() {
    initComponents();
  }

  private static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");

  private void initComponents() {
    setMinimumSize(new Dimension(100, 100));
    setResizable(false);
    setTitle(resourceBundle.getString("NewGameDialog.title"));
    setModal(true);

    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    initDialogPane(contentPane);

    pack();
    setLocationRelativeTo(getOwner());
  }

  private void initDialogPane(Container contentPane) {
    dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
    dialogPane.setLayout(new BorderLayout());

    initContentPanel();
    initButtonBar();

    contentPane.add(dialogPane, BorderLayout.CENTER);
  }

  private void initContentPanel() {
    GridLayout gridLayout = new GridLayout(5, 2, 4, 4);
    contentPanel.setLayout(gridLayout);

    checkBoxPlayerIsBlack =
        new JCheckBox(resourceBundle.getString("NewGameDialog.PlayBlack"), true);
    checkBoxPlayerIsBlack.addChangeListener(evt -> togglePlayerIsBlack());
    textFieldWhite = new JTextField();
    textFieldBlack = new JTextField();
    textFieldKomi = new JFormattedTextField(FORMAT_KOMI);
    textFieldHandicap = new JFormattedTextField(FORMAT_HANDICAP);
    textFieldHandicap.addPropertyChangeListener(evt -> modifyHandicap());

    contentPanel.add(checkBoxPlayerIsBlack);
    contentPanel.add(PLACEHOLDER);
    contentPanel.add(new JLabel(resourceBundle.getString("NewGameDialog.Black")));
    contentPanel.add(textFieldBlack);
    contentPanel.add(new JLabel(resourceBundle.getString("NewGameDialog.White")));
    contentPanel.add(textFieldWhite);
    contentPanel.add(new JLabel(resourceBundle.getString("NewGameDialog.Komi")));
    contentPanel.add(textFieldKomi);
    contentPanel.add(new JLabel(resourceBundle.getString("NewGameDialog.Handicap")));
    contentPanel.add(textFieldHandicap);

    textFieldKomi.setEnabled(false);

    dialogPane.add(contentPanel, BorderLayout.CENTER);
  }

  private void togglePlayerIsBlack() {
    JTextField humanTextField = playerIsBlack() ? textFieldBlack : textFieldWhite;
    JTextField computerTextField = playerIsBlack() ? textFieldWhite : textFieldBlack;

    humanTextField.setEnabled(true);
    humanTextField.setText(GameInfo.DEFAULT_NAME_HUMAN_PLAYER);
    computerTextField.setEnabled(false);
    computerTextField.setText(GameInfo.DEFAULT_NAME_CPU_PLAYER);
  }

  private void modifyHandicap() {
    try {
      int handicap = FORMAT_HANDICAP.parse(textFieldHandicap.getText()).intValue();
      if (handicap < 0) throw new IllegalArgumentException();

      textFieldKomi.setText(FORMAT_KOMI.format(GameInfo.DEFAULT_KOMI));
    } catch (ParseException | RuntimeException e) {
      // do not correct user mistakes
    }
  }

  private void initButtonBar() {
    buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
    buttonBar.setLayout(new GridBagLayout());
    ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[] {0, 80};
    ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

    // ---- okButton ----
    okButton.setText("OK");
    okButton.addActionListener(e -> apply());

    int center = GridBagConstraints.CENTER;
    int both = GridBagConstraints.BOTH;
    buttonBar.add(
        okButton,
        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, center, both, new Insets(0, 0, 0, 0), 0, 0));

    dialogPane.add(buttonBar, BorderLayout.SOUTH);
  }

  public void apply() {
    try {
      // validate data
      String playerBlack = textFieldBlack.getText();
      String playerWhite = textFieldWhite.getText();
      double komi = FORMAT_KOMI.parse(textFieldKomi.getText()).doubleValue();
      int handicap = FORMAT_HANDICAP.parse(textFieldHandicap.getText()).intValue();

      // apply new values
      gameInfo.setPlayerBlack(playerBlack);
      gameInfo.setPlayerWhite(playerWhite);
      gameInfo.setKomi(komi);
      gameInfo.setHandicap(handicap);

      // close window
      cancelled = false;
      setVisible(false);
    } catch (ParseException e) {
      // hide input mistakes.
    }
  }

  public void setGameInfo(GameInfo gameInfo) {
    this.gameInfo = gameInfo;

    textFieldBlack.setText(gameInfo.getPlayerBlack());
    textFieldWhite.setText(gameInfo.getPlayerWhite());
    textFieldHandicap.setText(FORMAT_HANDICAP.format(gameInfo.getHandicap()));
    textFieldKomi.setText(FORMAT_KOMI.format(gameInfo.getKomi()));

    // update player names
    togglePlayerIsBlack();
  }

  public boolean playerIsBlack() {
    return checkBoxPlayerIsBlack.isSelected();
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(
        () -> {
          try {
            NewGameDialog window = new NewGameDialog();
            window.setVisible(true);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }
}
