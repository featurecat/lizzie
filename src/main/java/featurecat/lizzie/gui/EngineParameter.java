package featurecat.lizzie.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

public class EngineParameter extends JDialog {

  public String enginePath = "";
  public String weightPath = "";
  public String parameters = "";
  public String commandLine = "";

  private final JPanel contentPanel = new JPanel();
  private JTextField txtCommandLine;
  private JTextField txtParameter;
  private Color oriColor;

  /** Create the dialog. */
  public EngineParameter(String enginePath, String weightPath, ConfigDialog configDialog) {
    setTitle(configDialog.resourceBundle.getString("LizzieConfig.title.parameterConfig"));
    setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
    setModal(true);
    setType(Type.POPUP);
    setModalityType(ModalityType.APPLICATION_MODAL);
    setBounds(100, 100, 680, 660);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(null);
    JLabel lblEngneCommand =
        new JLabel(configDialog.resourceBundle.getString("LizzieConfig.title.engine"));
    lblEngneCommand.setBounds(6, 17, 83, 16);
    contentPanel.add(lblEngneCommand);
    txtCommandLine = new JTextField();
    txtCommandLine.setEditable(false);
    txtCommandLine.setBounds(89, 12, 565, 26);
    txtCommandLine.setText(enginePath + " --weights " + weightPath);
    contentPanel.add(txtCommandLine);
    txtCommandLine.setColumns(10);
    JLabel lblParameter =
        new JLabel(configDialog.resourceBundle.getString("LizzieConfig.title.parameter"));
    lblParameter.setBounds(6, 45, 83, 16);
    contentPanel.add(lblParameter);
    txtParameter = new JTextField();
    txtParameter.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            if (!txtParameter.getText().isEmpty()) {
              txtParameter.setBackground(oriColor);
            }
          }
        });
    txtParameter.setColumns(10);
    txtParameter.setBounds(89, 44, 565, 26);
    txtParameter.setText("-g --lagbuffer 0 ");
    oriColor = txtParameter.getBackground();
    contentPanel.add(txtParameter);

    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setBounds(6, 110, 648, 478);
    contentPanel.add(scrollPane);
    Font font = new Font("Consolas", Font.PLAIN, 12);
    scrollPane.setVerticalScrollBarPolicy(
        javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    JTextPane txtParams = new JTextPane();
    scrollPane.setViewportView(txtParams);
    txtParams.setFont(font);
    txtParams.setText(configDialog.commandHelp);
    txtParams.setEditable(false);

    JLabel lblParameterList =
        new JLabel(configDialog.resourceBundle.getString("LizzieConfig.title.parameterList"));
    lblParameterList.setBounds(6, 81, 114, 16);
    contentPanel.add(lblParameterList);
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    JButton okButton = new JButton(configDialog.resourceBundle.getString("LizzieConfig.button.ok"));
    okButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (txtParameter.getText().isEmpty()) {
              txtParameter.setBackground(Color.RED);
            } else {
              parameters = txtParameter.getText().trim();
              commandLine = txtCommandLine.getText() + " " + parameters;
              setVisible(false);
            }
          }
        });
    okButton.setActionCommand("OK");
    buttonPane.add(okButton);
    getRootPane().setDefaultButton(okButton);
    JButton cancelButton =
        new JButton(configDialog.resourceBundle.getString("LizzieConfig.button.cancel"));
    cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });
    cancelButton.setActionCommand("Cancel");
    buttonPane.add(cancelButton);
    setLocationRelativeTo(getOwner());
  }
}
