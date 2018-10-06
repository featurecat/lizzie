/*
 * Created by JFormDesigner on Wed Apr 04 22:17:33 CEST 2018
 */

package featurecat.lizzie.gui;

import featurecat.lizzie.analysis.GameInfo;
import java.awt.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;

/** @author unknown */
public class GameInfoDialog extends JDialog {
  // create formatters
  public static final DecimalFormat FORMAT_KOMI = new DecimalFormat("#0.0");
  public static final DecimalFormat FORMAT_HANDICAP = new DecimalFormat("0");

  static {
    FORMAT_HANDICAP.setMaximumIntegerDigits(1);
  }

  private JPanel dialogPane = new JPanel();
  private JPanel contentPanel = new JPanel();
  private JPanel buttonBar = new JPanel();
  private JButton okButton = new JButton();

  private JTextField textFieldBlack;
  private JTextField textFieldWhite;
  private JTextField textFieldKomi;
  private JTextField textFieldHandicap;

  private GameInfo gameInfo;

  public GameInfoDialog() {
    initComponents();
  }

  private void initComponents() {
    setMinimumSize(new Dimension(100, 100));
    setResizable(false);
    setTitle("Game Info");
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
    GridLayout gridLayout = new GridLayout(4, 2, 4, 4);
    contentPanel.setLayout(gridLayout);

    // editable
    textFieldWhite = new JTextField();
    textFieldBlack = new JTextField();

    // read-only
    textFieldKomi = new JFormattedTextField(FORMAT_KOMI);
    textFieldHandicap = new JFormattedTextField(FORMAT_HANDICAP);
    textFieldKomi.setEditable(false);
    textFieldHandicap.setEditable(false);

    contentPanel.add(new JLabel("Black"));
    contentPanel.add(textFieldBlack);
    contentPanel.add(new JLabel("White"));
    contentPanel.add(textFieldWhite);
    contentPanel.add(new JLabel("Komi"));
    contentPanel.add(textFieldKomi);
    contentPanel.add(new JLabel("Handicap"));
    contentPanel.add(textFieldHandicap);

    dialogPane.add(contentPanel, BorderLayout.CENTER);
  }

  private void initButtonBar() {
    buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
    buttonBar.setLayout(new GridBagLayout());
    ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[] {0, 80};
    ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

    // ---- okButton ----
    okButton.setText("OK");
    okButton.addActionListener(e -> apply());

    buttonBar.add(
        okButton,
        new GridBagConstraints(
            1,
            0,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));

    dialogPane.add(buttonBar, BorderLayout.SOUTH);
  }

  public void setGameInfo(GameInfo gameInfo) {
    this.gameInfo = gameInfo;

    textFieldBlack.setText(gameInfo.getPlayerBlack());
    textFieldWhite.setText(gameInfo.getPlayerWhite());
    textFieldHandicap.setText(FORMAT_HANDICAP.format(gameInfo.getHandicap()));
    textFieldKomi.setText(FORMAT_KOMI.format(gameInfo.getKomi()));
  }

  public void apply() {
    // validate data
    String playerBlack = textFieldBlack.getText();
    String playerWhite = textFieldWhite.getText();

    // apply new values
    gameInfo.setPlayerBlack(playerBlack);
    gameInfo.setPlayerWhite(playerWhite);

    // close window
    setVisible(false);
  }
}
