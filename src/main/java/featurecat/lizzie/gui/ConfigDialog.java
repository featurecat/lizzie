package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConfigDialog extends JDialog {
  public final ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");

  public String enginePath = "";
  public String weightPath = "";
  public String commandHelp = "";

  private String osName;
  private Path curPath;
  private BufferedInputStream inputStream;
  private JSONObject leelazConfig;
  private List<String> fontList;
  private Theme theme;

  // Engine Tab
  private JTextField txtEngine;
  private JTextField txtEngine1;
  private JTextField txtEngine2;
  private JTextField txtEngine3;
  private JTextField txtEngine4;
  private JTextField txtEngine5;
  private JTextField txtEngine6;
  private JTextField txtEngine7;
  private JTextField txtEngine8;
  private JTextField txtEngine9;
  private JTextField[] txts;
  private JFormattedTextField txtMaxAnalyzeTime;
  private JFormattedTextField txtMaxGameThinkingTime;
  private JFormattedTextField txtAnalyzeUpdateInterval;
  private JCheckBox chkPrintEngineLog;

  // UI Tab
  private JTextField txtBoardSize;
  private JRadioButton rdoBoardSizeOther;
  private JRadioButton rdoBoardSize19;
  private JRadioButton rdoBoardSize13;
  private JRadioButton rdoBoardSize9;
  private JRadioButton rdoBoardSize7;
  private JRadioButton rdoBoardSize5;
  private JRadioButton rdoBoardSize4;
  private JFormattedTextField txtMinPlayoutRatioForStats;
  private JCheckBox chkShowCoordinates;
  private JRadioButton rdoShowMoveNumberNo;
  private JRadioButton rdoShowMoveNumberAll;
  private JRadioButton rdoShowMoveNumberLast;
  private JTextField txtShowMoveNumber;
  private JCheckBox chkShowBlunderBar;
  private JCheckBox chkDynamicWinrateGraphWidth;
  private JCheckBox chkAppendWinrateToComment;
  private JCheckBox chkColorByWinrateInsteadOfVisits;
  private JSlider sldBoardPositionProportion;

  // Theme Tab
  private JComboBox<String> cmbThemes;
  private JSpinner spnWinrateStrokeWidth;
  private JSpinner spnMinimumBlunderBarWidth;
  private JSpinner spnShadowSize;
  private JComboBox<String> cmbFontName;
  private JComboBox<String> cmbUiFontName;
  private JComboBox<String> cmbWinrateFontName;
  private JTextField txtBackgroundPath;
  private JTextField txtBoardPath;
  private JTextField txtBlackStonePath;
  private JTextField txtWhiteStonePath;
  private ColorLabel lblWinrateLineColor;
  private ColorLabel lblWinrateMissLineColor;
  private ColorLabel lblBlunderBarColor;
  private JCheckBox chkSolidStoneIndicator;
  private JCheckBox chkShowCommentNodeColor;
  private ColorLabel lblCommentNodeColor;
  private JTable tblBlunderNodes;
  private String[] columsBlunderNodes;
  private JButton btnBackgroundPath;
  private JButton btnBoardPath;
  private JButton btnBlackStonePath;
  private JButton btnWhiteStonePath;
  private JPanel pnlBoardPreview;

  public ConfigDialog() {
    setTitle(resourceBundle.getString("LizzieConfig.title.config"));
    setModalityType(ModalityType.APPLICATION_MODAL);
    setType(Type.POPUP);
    setBounds(100, 100, 661, 716);
    getContentPane().setLayout(new BorderLayout());
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    JButton okButton = new JButton(resourceBundle.getString("LizzieConfig.button.ok"));
    okButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
            saveConfig();
            applyChange();
          }
        });
    okButton.setActionCommand("OK");
    buttonPane.add(okButton);
    getRootPane().setDefaultButton(okButton);
    JButton cancelButton = new JButton(resourceBundle.getString("LizzieConfig.button.cancel"));
    cancelButton.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setVisible(false);
          }
        });
    cancelButton.setActionCommand("Cancel");
    buttonPane.add(cancelButton);
    JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    getContentPane().add(tabbedPane, BorderLayout.CENTER);

    JPanel engineTab = new JPanel();
    tabbedPane.addTab(resourceBundle.getString("LizzieConfig.title.engine"), null, engineTab, null);
    engineTab.setLayout(null);

    JLabel lblEngine = new JLabel(resourceBundle.getString("LizzieConfig.title.engine"));
    lblEngine.setBounds(6, 44, 92, 16);
    lblEngine.setHorizontalAlignment(SwingConstants.LEFT);
    engineTab.add(lblEngine);

    txtEngine = new JTextField();
    txtEngine.setBounds(87, 40, 502, 26);
    engineTab.add(txtEngine);
    txtEngine.setColumns(10);

    JLabel lblEngine1 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 1");
    lblEngine1.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine1.setBounds(6, 80, 92, 16);
    engineTab.add(lblEngine1);

    txtEngine2 = new JTextField();
    txtEngine2.setColumns(10);
    txtEngine2.setBounds(87, 105, 502, 26);
    engineTab.add(txtEngine2);

    JLabel lblEngine2 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 2");
    lblEngine2.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine2.setBounds(6, 110, 92, 16);
    engineTab.add(lblEngine2);

    txtEngine1 = new JTextField();
    txtEngine1.setColumns(10);
    txtEngine1.setBounds(87, 75, 502, 26);
    engineTab.add(txtEngine1);

    JLabel lblEngine3 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 3");
    lblEngine3.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine3.setBounds(6, 140, 92, 16);
    engineTab.add(lblEngine3);

    txtEngine3 = new JTextField();
    txtEngine3.setColumns(10);
    txtEngine3.setBounds(87, 135, 502, 26);
    engineTab.add(txtEngine3);

    JLabel lblEngine4 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 4");
    lblEngine4.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine4.setBounds(6, 170, 92, 16);
    engineTab.add(lblEngine4);

    txtEngine4 = new JTextField();
    txtEngine4.setColumns(10);
    txtEngine4.setBounds(87, 165, 502, 26);
    engineTab.add(txtEngine4);

    JLabel lblEngine5 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 5");
    lblEngine5.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine5.setBounds(6, 200, 92, 16);
    engineTab.add(lblEngine5);

    txtEngine5 = new JTextField();
    txtEngine5.setColumns(10);
    txtEngine5.setBounds(87, 195, 502, 26);
    engineTab.add(txtEngine5);

    JLabel lblEngine6 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 6");
    lblEngine6.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine6.setBounds(6, 230, 92, 16);
    engineTab.add(lblEngine6);

    txtEngine6 = new JTextField();
    txtEngine6.setColumns(10);
    txtEngine6.setBounds(87, 225, 502, 26);
    engineTab.add(txtEngine6);

    JLabel lblEngine7 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 7");
    lblEngine7.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine7.setBounds(6, 260, 92, 16);
    engineTab.add(lblEngine7);

    txtEngine7 = new JTextField();
    txtEngine7.setColumns(10);
    txtEngine7.setBounds(87, 255, 502, 26);
    engineTab.add(txtEngine7);

    JLabel lblEngine8 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 8");
    lblEngine8.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine8.setBounds(6, 290, 92, 16);
    engineTab.add(lblEngine8);

    txtEngine8 = new JTextField();
    txtEngine8.setColumns(10);
    txtEngine8.setBounds(87, 285, 502, 26);
    engineTab.add(txtEngine8);

    txtEngine9 = new JTextField();
    txtEngine9.setColumns(10);
    txtEngine9.setBounds(87, 315, 502, 26);
    engineTab.add(txtEngine9);

    JLabel lblEngine9 = new JLabel(resourceBundle.getString("LizzieConfig.title.engine") + " 9");
    lblEngine9.setHorizontalAlignment(SwingConstants.LEFT);
    lblEngine9.setBounds(6, 320, 92, 16);
    engineTab.add(lblEngine9);

    JButton button = new JButton("...");
    button.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine.setText(el);
            }
            setVisible(true);
          }
        });
    button.setBounds(595, 40, 40, 26);
    engineTab.add(button);

    JButton button_1 = new JButton("...");
    button_1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine1.setText(el);
            }
            setVisible(true);
          }
        });
    button_1.setBounds(595, 75, 40, 26);
    engineTab.add(button_1);

    JButton button_2 = new JButton("...");
    button_2.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine2.setText(el);
            }
            setVisible(true);
          }
        });
    button_2.setBounds(595, 105, 40, 26);
    engineTab.add(button_2);

    JButton button_3 = new JButton("...");
    button_3.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine3.setText(el);
            }
            setVisible(true);
          }
        });
    button_3.setBounds(595, 135, 40, 26);
    engineTab.add(button_3);

    JButton button_4 = new JButton("...");
    button_4.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine4.setText(el);
            }
            setVisible(true);
          }
        });
    button_4.setBounds(595, 165, 40, 26);
    engineTab.add(button_4);

    JButton button_5 = new JButton("...");
    button_5.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine5.setText(el);
            }
            setVisible(true);
          }
        });
    button_5.setBounds(595, 195, 40, 26);
    engineTab.add(button_5);

    JButton button_6 = new JButton("...");
    button_6.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine6.setText(el);
            }
            setVisible(true);
          }
        });
    button_6.setBounds(595, 225, 40, 26);
    engineTab.add(button_6);

    JButton button_7 = new JButton("...");
    button_7.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine7.setText(el);
            }
            setVisible(true);
          }
        });
    button_7.setBounds(595, 255, 40, 26);
    engineTab.add(button_7);

    JButton button_8 = new JButton("...");
    button_8.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine8.setText(el);
            }
            setVisible(true);
          }
        });
    button_8.setBounds(595, 285, 40, 26);
    engineTab.add(button_8);

    JButton button_9 = new JButton("...");
    button_9.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String el = getEngineLine();
            if (!el.isEmpty()) {
              txtEngine9.setText(el);
            }
            setVisible(true);
          }
        });
    button_9.setBounds(595, 315, 40, 26);
    engineTab.add(button_9);

    JLabel lblMaxAnalyzeTime =
        new JLabel(resourceBundle.getString("LizzieConfig.title.maxAnalyzeTime"));
    lblMaxAnalyzeTime.setBounds(6, 370, 157, 16);
    engineTab.add(lblMaxAnalyzeTime);

    JLabel lblMaxAnalyzeTimeMinutes =
        new JLabel(resourceBundle.getString("LizzieConfig.title.minutes"));
    lblMaxAnalyzeTimeMinutes.setBounds(213, 370, 82, 16);
    engineTab.add(lblMaxAnalyzeTimeMinutes);

    NumberFormat nf = NumberFormat.getIntegerInstance();
    nf.setGroupingUsed(false);

    txtMaxAnalyzeTime =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtMaxAnalyzeTime.setBounds(171, 365, 40, 26);
    engineTab.add(txtMaxAnalyzeTime);
    txtMaxAnalyzeTime.setColumns(10);

    JLabel lblMaxGameThinkingTime =
        new JLabel(resourceBundle.getString("LizzieConfig.title.maxGameThinkingTime"));
    lblMaxGameThinkingTime.setBounds(6, 400, 157, 16);
    engineTab.add(lblMaxGameThinkingTime);

    JLabel lblMaxGameThinkingTimeSeconds =
        new JLabel(resourceBundle.getString("LizzieConfig.title.seconds"));
    lblMaxGameThinkingTimeSeconds.setBounds(213, 400, 82, 16);
    engineTab.add(lblMaxGameThinkingTimeSeconds);

    txtMaxGameThinkingTime =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtMaxGameThinkingTime.setColumns(10);
    txtMaxGameThinkingTime.setBounds(171, 395, 40, 26);
    engineTab.add(txtMaxGameThinkingTime);

    JLabel lblAnalyzeUpdateInterval =
        new JLabel(resourceBundle.getString("LizzieConfig.title.analyzeUpdateInterval"));
    lblAnalyzeUpdateInterval.setBounds(331, 368, 157, 16);
    engineTab.add(lblAnalyzeUpdateInterval);

    JLabel lblAnalyzeUpdateIntervalCentisec =
        new JLabel(resourceBundle.getString("LizzieConfig.title.centisecond"));
    lblAnalyzeUpdateIntervalCentisec.setBounds(538, 368, 82, 16);
    engineTab.add(lblAnalyzeUpdateIntervalCentisec);

    txtAnalyzeUpdateInterval =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtAnalyzeUpdateInterval.setColumns(10);
    txtAnalyzeUpdateInterval.setBounds(496, 363, 40, 26);
    engineTab.add(txtAnalyzeUpdateInterval);

    JLabel lblPrintEngineLog =
        new JLabel(resourceBundle.getString("LizzieConfig.title.printEngineLog"));
    lblPrintEngineLog.setBounds(6, 430, 157, 16);
    engineTab.add(lblPrintEngineLog);

    chkPrintEngineLog = new JCheckBox("");
    chkPrintEngineLog.setBounds(167, 425, 201, 23);
    engineTab.add(chkPrintEngineLog);

    JPanel uiTab = new JPanel();
    tabbedPane.addTab(resourceBundle.getString("LizzieConfig.title.ui"), null, uiTab, null);
    uiTab.setLayout(null);

    JLabel lblBoardSize = new JLabel(resourceBundle.getString("LizzieConfig.title.boardSize"));
    lblBoardSize.setBounds(6, 6, 67, 16);
    lblBoardSize.setHorizontalAlignment(SwingConstants.LEFT);
    uiTab.add(lblBoardSize);

    rdoBoardSize19 = new JRadioButton("19x19");
    rdoBoardSize19.setBounds(85, 2, 84, 23);
    uiTab.add(rdoBoardSize19);

    rdoBoardSize13 = new JRadioButton("13x13");
    rdoBoardSize13.setBounds(170, 2, 84, 23);
    uiTab.add(rdoBoardSize13);

    rdoBoardSize9 = new JRadioButton("9x9");
    rdoBoardSize9.setBounds(255, 2, 57, 23);
    uiTab.add(rdoBoardSize9);

    rdoBoardSize7 = new JRadioButton("7x7");
    rdoBoardSize7.setBounds(325, 2, 67, 23);
    uiTab.add(rdoBoardSize7);

    rdoBoardSize5 = new JRadioButton("5x5");
    rdoBoardSize5.setBounds(395, 2, 67, 23);
    uiTab.add(rdoBoardSize5);

    rdoBoardSize4 = new JRadioButton("4x4");
    rdoBoardSize4.setBounds(460, 2, 67, 23);
    uiTab.add(rdoBoardSize4);

    rdoBoardSizeOther = new JRadioButton("");
    rdoBoardSizeOther.addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            if (rdoBoardSizeOther.isSelected()) {
              txtBoardSize.setEnabled(true);
            } else {
              txtBoardSize.setEnabled(false);
            }
          }
        });
    rdoBoardSizeOther.setBounds(530, 2, 29, 23);
    uiTab.add(rdoBoardSizeOther);

    ButtonGroup group = new ButtonGroup();
    group.add(rdoBoardSize19);
    group.add(rdoBoardSize13);
    group.add(rdoBoardSize9);
    group.add(rdoBoardSize7);
    group.add(rdoBoardSize5);
    group.add(rdoBoardSize4);
    group.add(rdoBoardSizeOther);

    txtBoardSize =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtBoardSize.setBounds(564, 1, 52, 26);
    uiTab.add(txtBoardSize);
    txtBoardSize.setColumns(10);

    JLabel lblMinPlayoutRatioForStats =
        new JLabel(resourceBundle.getString("LizzieConfig.title.minPlayoutRatioForStats"));
    lblMinPlayoutRatioForStats.setBounds(6, 40, 157, 16);
    uiTab.add(lblMinPlayoutRatioForStats);
    txtMinPlayoutRatioForStats =
        new JFormattedTextField(
            new InternationalFormatter() {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new NumericFilter();
            });
    txtMinPlayoutRatioForStats.setColumns(10);
    txtMinPlayoutRatioForStats.setBounds(171, 35, 57, 26);
    uiTab.add(txtMinPlayoutRatioForStats);

    JLabel lblShowCoordinates =
        new JLabel(resourceBundle.getString("LizzieConfig.title.showCoordinates"));
    lblShowCoordinates.setBounds(6, 67, 157, 16);
    uiTab.add(lblShowCoordinates);
    chkShowCoordinates = new JCheckBox("");
    chkShowCoordinates.addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            if (chkShowCoordinates.isSelected() != Lizzie.config.showCoordinates) {
              Lizzie.config.toggleCoordinates();
              Lizzie.frame.repaint();
              //Lizzie.main.invalidLayout();
            }
          }
        });
    chkShowCoordinates.setBounds(170, 64, 57, 23);
    uiTab.add(chkShowCoordinates);

    JLabel lblShowMoveNumber =
        new JLabel(resourceBundle.getString("LizzieConfig.title.showMoveNumber"));
    lblShowMoveNumber.setBounds(6, 94, 157, 16);
    uiTab.add(lblShowMoveNumber);

    rdoShowMoveNumberNo =
        new JRadioButton(resourceBundle.getString("LizzieConfig.title.showMoveNumberNo"));
    rdoShowMoveNumberNo.setBounds(170, 91, 84, 23);
    uiTab.add(rdoShowMoveNumberNo);

    rdoShowMoveNumberAll =
        new JRadioButton(resourceBundle.getString("LizzieConfig.title.showMoveNumberAll"));
    rdoShowMoveNumberAll.setBounds(261, 91, 65, 23);
    uiTab.add(rdoShowMoveNumberAll);

    rdoShowMoveNumberLast =
        new JRadioButton(resourceBundle.getString("LizzieConfig.title.showMoveNumberLast"));
    rdoShowMoveNumberLast.addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            if (rdoShowMoveNumberLast.isSelected()) {
              txtShowMoveNumber.setEnabled(true);
            } else {
              txtShowMoveNumber.setEnabled(false);
            }
          }
        });
    rdoShowMoveNumberLast.setBounds(325, 91, 67, 23);
    uiTab.add(rdoShowMoveNumberLast);

    ButtonGroup showMoveGroup = new ButtonGroup();
    showMoveGroup.add(rdoShowMoveNumberNo);
    showMoveGroup.add(rdoShowMoveNumberAll);
    showMoveGroup.add(rdoShowMoveNumberLast);

    txtShowMoveNumber =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtShowMoveNumber.setBounds(395, 89, 52, 26);
    uiTab.add(txtShowMoveNumber);
    txtShowMoveNumber.setColumns(10);

    JLabel lblShowBlunderBar =
        new JLabel(resourceBundle.getString("LizzieConfig.title.showBlunderBar"));
    lblShowBlunderBar.setBounds(6, 121, 157, 16);
    uiTab.add(lblShowBlunderBar);
    chkShowBlunderBar = new JCheckBox("");
    chkShowBlunderBar.setBounds(170, 118, 57, 23);
    uiTab.add(chkShowBlunderBar);

    JLabel lblDynamicWinrateGraphWidth =
        new JLabel(resourceBundle.getString("LizzieConfig.title.dynamicWinrateGraphWidth"));
    lblDynamicWinrateGraphWidth.setBounds(6, 148, 157, 16);
    uiTab.add(lblDynamicWinrateGraphWidth);
    chkDynamicWinrateGraphWidth = new JCheckBox("");
    chkDynamicWinrateGraphWidth.setBounds(170, 145, 57, 23);
    uiTab.add(chkDynamicWinrateGraphWidth);

    JLabel lblAppendWinrateToComment =
        new JLabel(resourceBundle.getString("LizzieConfig.title.appendWinrateToComment"));
    lblAppendWinrateToComment.setBounds(6, 175, 157, 16);
    uiTab.add(lblAppendWinrateToComment);
    chkAppendWinrateToComment = new JCheckBox("");
    chkAppendWinrateToComment.setBounds(170, 172, 57, 23);
    uiTab.add(chkAppendWinrateToComment);

    JLabel lblColorByWinrateInsteadOfVisits =
        new JLabel(resourceBundle.getString("LizzieConfig.title.colorByWinrateInsteadOfVisits"));
    lblColorByWinrateInsteadOfVisits.setBounds(6, 202, 163, 16);
    uiTab.add(lblColorByWinrateInsteadOfVisits);
    chkColorByWinrateInsteadOfVisits = new JCheckBox("");
    chkColorByWinrateInsteadOfVisits.setBounds(170, 199, 57, 23);
    uiTab.add(chkColorByWinrateInsteadOfVisits);

    JLabel lblBoardPositionProportion =
        new JLabel(resourceBundle.getString("LizzieConfig.title.boardPositionProportion"));
    lblBoardPositionProportion.setBounds(6, 229, 163, 16);
    uiTab.add(lblBoardPositionProportion);
    sldBoardPositionProportion = new JSlider();
    sldBoardPositionProportion.setPaintTicks(true);
    sldBoardPositionProportion.setSnapToTicks(true);
    sldBoardPositionProportion.addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            if (Lizzie.frame.BoardPositionProportion != sldBoardPositionProportion.getValue()) {
              Lizzie.frame.BoardPositionProportion = sldBoardPositionProportion.getValue();
              Lizzie.frame.repaint();
              //Lizzie.main.invalidLayout();
            }
          }
        });
    sldBoardPositionProportion.setValue(4);
    sldBoardPositionProportion.setMaximum(8);
    sldBoardPositionProportion.setBounds(170, 225, 200, 28);
    uiTab.add(sldBoardPositionProportion);

    // Theme Tab
    JPanel themeTab = new JPanel();
    tabbedPane.addTab(resourceBundle.getString("LizzieConfig.title.theme"), null, themeTab, null);
    themeTab.setLayout(null);

    File themeFolder = new File(Theme.pathPrefix);
    File[] themes =
        themeFolder.listFiles(
            new FileFilter() {
              public boolean accept(File f) {
                return f.isDirectory() && !".".equals(f.getName());
              }
            });
    List<String> themeList =
        Arrays.asList(themes).stream().map(t -> t.getName()).collect(Collectors.toList());
    themeList.add(0, resourceBundle.getString("LizzieConfig.title.defaultTheme"));

    JLabel lblThemes = new JLabel(resourceBundle.getString("LizzieConfig.title.theme"));
    lblThemes.setBounds(10, 11, 163, 20);
    themeTab.add(lblThemes);
    cmbThemes = new JComboBox(themeList.toArray());
    cmbThemes.addItemListener(
        new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            readThemeValues();
          }
        });
    cmbThemes.setBounds(175, 11, 199, 20);
    themeTab.add(cmbThemes);

    JLabel lblWinrateStrokeWidth =
        new JLabel(resourceBundle.getString("LizzieConfig.title.winrateStrokeWidth"));
    lblWinrateStrokeWidth.setBounds(10, 44, 163, 16);
    themeTab.add(lblWinrateStrokeWidth);
    spnWinrateStrokeWidth = new JSpinner();
    spnWinrateStrokeWidth.setModel(new SpinnerNumberModel(2, 1, 10, 1));
    spnWinrateStrokeWidth.setBounds(175, 42, 69, 20);
    themeTab.add(spnWinrateStrokeWidth);

    JLabel lblMinimumBlunderBarWidth =
        new JLabel(resourceBundle.getString("LizzieConfig.title.minimumBlunderBarWidth"));
    lblMinimumBlunderBarWidth.setBounds(10, 74, 163, 16);
    themeTab.add(lblMinimumBlunderBarWidth);
    spnMinimumBlunderBarWidth = new JSpinner();
    spnMinimumBlunderBarWidth.setModel(new SpinnerNumberModel(1, 1, 10, 1));
    spnMinimumBlunderBarWidth.setBounds(175, 72, 69, 20);
    themeTab.add(spnMinimumBlunderBarWidth);

    JLabel lblShadowSize = new JLabel(resourceBundle.getString("LizzieConfig.title.shadowSize"));
    lblShadowSize.setBounds(10, 104, 163, 16);
    themeTab.add(lblShadowSize);
    spnShadowSize = new JSpinner();
    spnShadowSize.setModel(new SpinnerNumberModel(50, 1, 100, 1));
    spnShadowSize.setBounds(175, 102, 69, 20);
    themeTab.add(spnShadowSize);

    fontList =
        Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            .stream()
            .collect(Collectors.toList());
    fontList.add(0, " ");
    Object fonts[] = fontList.toArray();

    JLabel lblFontName = new JLabel(resourceBundle.getString("LizzieConfig.title.fontName"));
    lblFontName.setBounds(10, 134, 163, 16);
    themeTab.add(lblFontName);
    cmbFontName = new JComboBox(fonts);
    cmbFontName.setMaximumRowCount(16);
    cmbFontName.setBounds(175, 133, 200, 20);
    cmbFontName.setRenderer(new FontComboBoxRenderer<String>());
    cmbFontName.addItemListener(
        new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            cmbFontName.setFont(
                new Font((String) e.getItem(), Font.PLAIN, cmbFontName.getFont().getSize()));
          }
        });
    themeTab.add(cmbFontName);

    JLabel lblUiFontName = new JLabel(resourceBundle.getString("LizzieConfig.title.uiFontName"));
    lblUiFontName.setBounds(10, 164, 163, 16);
    themeTab.add(lblUiFontName);
    cmbUiFontName = new JComboBox(fonts);
    cmbUiFontName.setMaximumRowCount(16);
    cmbUiFontName.setBounds(175, 163, 200, 20);
    cmbUiFontName.setRenderer(new FontComboBoxRenderer<String>());
    cmbUiFontName.addItemListener(
        new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            cmbUiFontName.setFont(
                new Font((String) e.getItem(), Font.PLAIN, cmbUiFontName.getFont().getSize()));
          }
        });
    themeTab.add(cmbUiFontName);

    JLabel lblWinrateFontName =
        new JLabel(resourceBundle.getString("LizzieConfig.title.winrateFontName"));
    lblWinrateFontName.setBounds(10, 194, 163, 16);
    themeTab.add(lblWinrateFontName);
    cmbWinrateFontName = new JComboBox(fonts);
    cmbWinrateFontName.setMaximumRowCount(16);
    cmbWinrateFontName.setBounds(175, 193, 200, 20);
    cmbWinrateFontName.setRenderer(new FontComboBoxRenderer<String>());
    cmbWinrateFontName.addItemListener(
        new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            cmbWinrateFontName.setFont(
                new Font((String) e.getItem(), Font.PLAIN, cmbWinrateFontName.getFont().getSize()));
          }
        });
    themeTab.add(cmbWinrateFontName);

    JLabel lblBackgroundPath =
        new JLabel(resourceBundle.getString("LizzieConfig.title.backgroundPath"));
    lblBackgroundPath.setHorizontalAlignment(SwingConstants.LEFT);
    lblBackgroundPath.setBounds(10, 225, 163, 16);
    themeTab.add(lblBackgroundPath);
    txtBackgroundPath = new JTextField();
    txtBackgroundPath.setText((String) null);
    txtBackgroundPath.setColumns(10);
    txtBackgroundPath.setBounds(175, 224, 421, 20);
    themeTab.add(txtBackgroundPath);

    JLabel lblBoardPath = new JLabel(resourceBundle.getString("LizzieConfig.title.boardPath"));
    lblBoardPath.setHorizontalAlignment(SwingConstants.LEFT);
    lblBoardPath.setBounds(10, 255, 163, 16);
    themeTab.add(lblBoardPath);
    txtBoardPath = new JTextField();
    txtBoardPath.setText((String) null);
    txtBoardPath.setColumns(10);
    txtBoardPath.setBounds(175, 254, 421, 20);
    themeTab.add(txtBoardPath);

    JLabel lblBlackStonePath =
        new JLabel(resourceBundle.getString("LizzieConfig.title.blackStonePath"));
    lblBlackStonePath.setHorizontalAlignment(SwingConstants.LEFT);
    lblBlackStonePath.setBounds(10, 285, 163, 16);
    themeTab.add(lblBlackStonePath);
    txtBlackStonePath = new JTextField();
    txtBlackStonePath.setText((String) null);
    txtBlackStonePath.setColumns(10);
    txtBlackStonePath.setBounds(175, 284, 421, 20);
    themeTab.add(txtBlackStonePath);

    JLabel lblWhiteStonePath =
        new JLabel(resourceBundle.getString("LizzieConfig.title.whiteStonePath"));
    lblWhiteStonePath.setHorizontalAlignment(SwingConstants.LEFT);
    lblWhiteStonePath.setBounds(10, 315, 163, 16);
    themeTab.add(lblWhiteStonePath);
    txtWhiteStonePath = new JTextField();
    txtWhiteStonePath.setText((String) null);
    txtWhiteStonePath.setColumns(10);
    txtWhiteStonePath.setBounds(175, 314, 421, 20);
    themeTab.add(txtWhiteStonePath);

    JLabel lblWinrateLineColorTitle =
        new JLabel(resourceBundle.getString("LizzieConfig.title.winrateLineColor"));
    lblWinrateLineColorTitle.setHorizontalAlignment(SwingConstants.LEFT);
    lblWinrateLineColorTitle.setBounds(10, 345, 163, 16);
    themeTab.add(lblWinrateLineColorTitle);
    lblWinrateLineColor = new ColorLabel();
    lblWinrateLineColor.setBounds(175, 350, 199, 9);
    themeTab.add(lblWinrateLineColor);

    JLabel lblWinrateMissLineColorTitle =
        new JLabel(resourceBundle.getString("LizzieConfig.title.winrateMissLineColor"));
    lblWinrateMissLineColorTitle.setHorizontalAlignment(SwingConstants.LEFT);
    lblWinrateMissLineColorTitle.setBounds(10, 375, 163, 16);
    themeTab.add(lblWinrateMissLineColorTitle);
    lblWinrateMissLineColor = new ColorLabel();
    lblWinrateMissLineColor.setBounds(175, 380, 199, 9);
    themeTab.add(lblWinrateMissLineColor);

    JLabel lblBlunderBarColorTitle =
        new JLabel(resourceBundle.getString("LizzieConfig.title.blunderBarColor"));
    lblBlunderBarColorTitle.setHorizontalAlignment(SwingConstants.LEFT);
    lblBlunderBarColorTitle.setBounds(10, 405, 163, 16);
    themeTab.add(lblBlunderBarColorTitle);
    lblBlunderBarColor = new ColorLabel();
    lblBlunderBarColor.setBounds(175, 410, 199, 9);
    themeTab.add(lblBlunderBarColor);

    JLabel lblSolidStoneIndicator =
        new JLabel(resourceBundle.getString("LizzieConfig.title.solidStoneIndicator"));
    lblSolidStoneIndicator.setBounds(10, 444, 163, 16);
    themeTab.add(lblSolidStoneIndicator);
    chkSolidStoneIndicator = new JCheckBox("");
    chkSolidStoneIndicator.setBounds(170, 437, 57, 23);
    themeTab.add(chkSolidStoneIndicator);

    JLabel lblShowCommentNodeColor =
        new JLabel(resourceBundle.getString("LizzieConfig.title.showCommentNodeColor"));
    lblShowCommentNodeColor.setBounds(10, 474, 163, 16);
    themeTab.add(lblShowCommentNodeColor);
    chkShowCommentNodeColor = new JCheckBox("");
    chkShowCommentNodeColor.setBounds(170, 467, 33, 23);
    themeTab.add(chkShowCommentNodeColor);

    JLabel lblCommentNodeColorTitle =
        new JLabel(resourceBundle.getString("LizzieConfig.title.commentNodeColor"));
    lblCommentNodeColorTitle.setHorizontalAlignment(SwingConstants.LEFT);
    lblCommentNodeColorTitle.setBounds(274, 474, 138, 16);
    themeTab.add(lblCommentNodeColorTitle);
    lblCommentNodeColor = new ColorLabel();
    lblCommentNodeColor.setBounds(431, 468, 22, 22);
    themeTab.add(lblCommentNodeColor);

    JLabel lblBlunderNodes =
        new JLabel(resourceBundle.getString("LizzieConfig.title.blunderNodes"));
    lblBlunderNodes.setHorizontalAlignment(SwingConstants.LEFT);
    lblBlunderNodes.setBounds(10, 497, 163, 16);
    themeTab.add(lblBlunderNodes);
    tblBlunderNodes = new JTable();
    columsBlunderNodes =
        new String[] {
          resourceBundle.getString("LizzieConfig.title.blunderThresholds"),
          resourceBundle.getString("LizzieConfig.title.blunderColor")
        };
    JScrollPane pnlScrollBlunderNodes = new JScrollPane();
    pnlScrollBlunderNodes.setViewportView(tblBlunderNodes);
    pnlScrollBlunderNodes.setBounds(175, 497, 199, 108);
    themeTab.add(pnlScrollBlunderNodes);

    JButton btnAdd = new JButton(resourceBundle.getString("LizzieConfig.button.add"));
    btnAdd.setBounds(382, 506, 89, 23);
    btnAdd.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ((BlunderNodeTableModel) tblBlunderNodes.getModel()).addRow("", Color.WHITE);
          }
        });
    themeTab.add(btnAdd);

    JButton btnRemove = new JButton(resourceBundle.getString("LizzieConfig.button.remove"));
    btnRemove.setBounds(384, 540, 89, 23);
    btnRemove.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            ((BlunderNodeTableModel) tblBlunderNodes.getModel())
                .removeRow(tblBlunderNodes.getSelectedRow());
          }
        });
    themeTab.add(btnRemove);

    btnBackgroundPath = new JButton("...");
    btnBackgroundPath.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String ip = getImagePath();
            if (!ip.isEmpty()) {
              txtBackgroundPath.setText(ip);
              pnlBoardPreview.repaint();
            }
          }
        });
    btnBackgroundPath.setBounds(598, 220, 40, 26);
    themeTab.add(btnBackgroundPath);

    btnBoardPath = new JButton("...");
    btnBoardPath.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String ip = getImagePath();
            if (!ip.isEmpty()) {
              txtBoardPath.setText(ip);
              pnlBoardPreview.repaint();
            }
          }
        });
    btnBoardPath.setBounds(598, 252, 40, 26);
    themeTab.add(btnBoardPath);

    btnBlackStonePath = new JButton("...");
    btnBlackStonePath.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String ip = getImagePath();
            if (!ip.isEmpty()) {
              txtBlackStonePath.setText(ip);
              pnlBoardPreview.repaint();
            }
          }
        });
    btnBlackStonePath.setBounds(598, 282, 40, 26);
    themeTab.add(btnBlackStonePath);

    btnWhiteStonePath = new JButton("...");
    btnWhiteStonePath.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String ip = getImagePath();
            if (!ip.isEmpty()) {
              txtWhiteStonePath.setText(ip);
              pnlBoardPreview.repaint();
            }
          }
        });
    btnWhiteStonePath.setBounds(598, 312, 40, 26);
    themeTab.add(btnWhiteStonePath);

    // Engines
    txts =
        new JTextField[] {
          txtEngine1,
          txtEngine2,
          txtEngine3,
          txtEngine4,
          txtEngine5,
          txtEngine6,
          txtEngine7,
          txtEngine8,
          txtEngine9
        };
    leelazConfig = Lizzie.config.leelazConfig;
    txtEngine.setText(leelazConfig.getString("engine-command"));
    Optional<JSONArray> enginesOpt =
        Optional.ofNullable(leelazConfig.optJSONArray("engine-command-list"));
    enginesOpt.ifPresent(
        a -> {
          IntStream.range(0, a.length())
              .forEach(
                  i -> {
                    txts[i].setText(a.getString(i));
                  });
        });
    txtMaxAnalyzeTime.setText(String.valueOf(leelazConfig.getInt("max-analyze-time-minutes")));
    txtAnalyzeUpdateInterval.setText(
        String.valueOf(leelazConfig.getInt("analyze-update-interval-centisec")));
    txtMaxGameThinkingTime.setText(
        String.valueOf(leelazConfig.getInt("max-game-thinking-time-seconds")));
    chkPrintEngineLog.setSelected(leelazConfig.getBoolean("print-comms"));
    curPath = (new File("")).getAbsoluteFile().toPath();
    osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    setBoardSize();
    txtMinPlayoutRatioForStats.setText(String.valueOf(Lizzie.config.minPlayoutRatioForStats));
    chkShowCoordinates.setSelected(Lizzie.config.showCoordinates);
    chkShowBlunderBar.setSelected(Lizzie.config.showBlunderBar);
    chkDynamicWinrateGraphWidth.setSelected(Lizzie.config.dynamicWinrateGraphWidth);
    chkAppendWinrateToComment.setSelected(Lizzie.config.appendWinrateToComment);
    chkColorByWinrateInsteadOfVisits.setSelected(Lizzie.config.colorByWinrateInsteadOfVisits);
    sldBoardPositionProportion.setValue(Lizzie.config.boardPositionProportion);
    cmbThemes.setSelectedItem(
        Lizzie.config.uiConfig.optString(
            "theme", resourceBundle.getString("LizzieConfig.title.defaultTheme")));

    readThemeValues();

    pnlBoardPreview =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            if (g instanceof Graphics2D) {
              int width = getWidth();
              int height = getHeight();
              Graphics2D bsGraphics = (Graphics2D) g;
              bsGraphics.setRenderingHint(
                  RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

              BufferedImage backgroundImage = null;
              try {
                backgroundImage = ImageIO.read(new File(theme.path + txtBackgroundPath.getText()));
                bsGraphics.drawImage(backgroundImage, 0, 0, null);
              } catch (IOException e0) {
              }
              BufferedImage boardImage = null;
              try {
                boardImage = ImageIO.read(new File(theme.path + txtBoardPath.getText()));
                bsGraphics.drawImage(boardImage, 20, 20, null);
              } catch (IOException e0) {
              }
            }
          }
        };
    pnlBoardPreview.setBounds(422, 11, 200, 200);
    themeTab.add(pnlBoardPreview);

    setShowMoveNumber();
    setLocationRelativeTo(getOwner());
  }

  private String getEngineLine() {
    String engineLine = "";
    File engineFile = null;
    File weightFile = null;
    JFileChooser chooser = new JFileChooser(".");
    if (isWindows()) {
      FileNameExtensionFilter filter =
          new FileNameExtensionFilter(
              resourceBundle.getString("LizzieConfig.title.engine"), "exe", "bat", "sh");
      chooser.setFileFilter(filter);
    } else {
      setVisible(false);
    }
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(resourceBundle.getString("LizzieConfig.prompt.selectEngine"));
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      engineFile = chooser.getSelectedFile();
      if (engineFile != null) {
        enginePath = engineFile.getAbsolutePath();
        enginePath = relativizePath(engineFile.toPath(), this.curPath);
        getCommandHelp();
        JFileChooser chooserw = new JFileChooser(".");
        chooserw.setMultiSelectionEnabled(false);
        chooserw.setDialogTitle(resourceBundle.getString("LizzieConfig.prompt.selectWeight"));
        result = chooserw.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
          weightFile = chooserw.getSelectedFile();
          if (weightFile != null) {
            weightPath = relativizePath(weightFile.toPath(), this.curPath);
            EngineParameter ep = new EngineParameter(enginePath, weightPath, this);
            ep.setVisible(true);
            if (!ep.commandLine.isEmpty()) {
              engineLine = ep.commandLine;
            }
          }
        }
      }
    }
    return engineLine;
  }

  private String getImagePath() {
    String imagePath = "";
    File imageFile = null;
    JFileChooser chooser = new JFileChooser(theme.path);
    FileNameExtensionFilter filter =
        new FileNameExtensionFilter("Image", "jpg", "png", "jpeg", "gif");
    chooser.setFileFilter(filter);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle("Image");
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      imageFile = chooser.getSelectedFile();
      if (imageFile != null) {
        imagePath = imageFile.getAbsolutePath();
        imagePath =
            relativizePath(imageFile.toPath(), new File(theme.path).getAbsoluteFile().toPath());
      }
    }
    return imagePath;
  }

  private String relativizePath(Path path, Path curPath) {
    Path relatPath;
    if (path.startsWith(curPath)) {
      relatPath = curPath.relativize(path);
    } else {
      relatPath = path;
    }
    return relatPath.toString();
  }

  private void getCommandHelp() {

    List<String> commands = new ArrayList<String>();
    commands.add(enginePath);
    commands.add("-h");

    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.directory();
    processBuilder.redirectErrorStream(true);
    try {
      Process process = processBuilder.start();
      inputStream = new BufferedInputStream(process.getInputStream());
      ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      executor.execute(this::read);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void read() {
    try {
      int c;
      StringBuilder line = new StringBuilder();
      while ((c = inputStream.read()) != -1) {
        line.append((char) c);
      }
      commandHelp = line.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void applyChange() {
    Lizzie.board.reopen(getBoardSize());
  }

  private Integer txtFieldIntValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()) {
      return 0;
    } else {
      return Integer.parseInt(txt.getText().trim());
    }
  }

  private Double txtFieldDoubleValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()) {
      return 0.0;
    } else {
      return new Double(txt.getText().trim());
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

  private class NumericFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
        throws BadLocationException {
      String newStr = string != null ? string.replaceAll("[^0-9\\.]++", "") : "";
      if (!newStr.isEmpty()) {
        fb.insertString(offset, newStr, attr);
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
        throws BadLocationException {
      String newStr = text != null ? text.replaceAll("[^0-9\\.]++", "") : "";
      if (!newStr.isEmpty()) {
        fb.replace(offset, length, newStr, attrs);
      }
    }
  }

  private class FontComboBoxRenderer<E> extends JLabel implements ListCellRenderer<E> {
    @Override
    public Component getListCellRendererComponent(
        JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
      final String fontName = (String) value;
      setText(fontName);
      setFont(new Font(fontName, Font.PLAIN, 12));
      return this;
    }
  }

  private class ColorLabel extends JLabel {

    private Color curColor;

    public ColorLabel() {
      super();
      setOpaque(true);

      addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              ColorLabel cl = (ColorLabel) e.getSource();
              Color color =
                  JColorChooser.showDialog(
                      (Component) e.getSource(), "Choose a color", cl.getColor());
              if (color != null) {
                cl.setColor(color);
              }
            }
          });
    }

    public void setColor(Color c) {
      curColor = c;
      setBackground(c);
    }

    public Color getColor() {
      return curColor;
    }
  }

  private class ColorRenderer extends JLabel implements TableCellRenderer {
    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    public ColorRenderer(boolean isBordered) {
      this.isBordered = isBordered;
      setOpaque(true);
    }

    public Component getTableCellRendererComponent(
        JTable table, Object color, boolean isSelected, boolean hasFocus, int row, int column) {
      Color newColor = (Color) color;
      setBackground(newColor);
      if (isBordered) {
        if (isSelected) {
          if (selectedBorder == null) {
            selectedBorder =
                BorderFactory.createMatteBorder(2, 5, 2, 5, table.getSelectionBackground());
          }
          setBorder(selectedBorder);
        } else {
          if (unselectedBorder == null) {
            unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5, table.getBackground());
          }
          setBorder(unselectedBorder);
        }
      }

      return this;
    }
  }

  private class ColorEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
    ColorLabel cl;

    public ColorEditor() {
      cl = new ColorLabel();
    }

    public Object getCellEditorValue() {
      return cl.getColor();
    }

    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
      cl.setColor((Color) value);
      return cl;
    }

    @Override
    public void actionPerformed(ActionEvent e) {}
  }

  class BlunderNodeTableModel extends AbstractTableModel {
    private String[] columnNames;
    private Vector<Vector<Object>> data;

    public BlunderNodeTableModel(
        List<Double> blunderWinrateThresholds,
        Map<Double, Color> blunderNodeColors,
        String[] columnNames) {
      this.columnNames = columnNames;
      data = new Vector<Vector<Object>>();
      if (blunderWinrateThresholds != null) {
        for (Double d : blunderWinrateThresholds) {
          Vector<Object> row = new Vector<Object>();
          row.add(String.valueOf(d));
          row.add(blunderNodeColors.get(d));
          data.add(row);
        }
      }
    }

    public JSONArray getThresholdArray() {
      JSONArray thresholds = new JSONArray("[]");
      data.forEach(d -> thresholds.put(new Double((String) d.get(0))));
      return thresholds;
    }

    public JSONArray getColorArray() {
      JSONArray colors = new JSONArray("[]");
      data.forEach(d -> colors.put(Theme.color2Array((Color) d.get(1))));
      return colors;
    }

    public void addRow(String threshold, Color color) {
      Vector<Object> row = new Vector<Object>();
      row.add(threshold);
      row.add(color);
      data.add(row);
      fireTableRowsInserted(0, data.size() - 1);
    }

    public void removeRow(int index) {
      if (index >= 0 && index < data.size()) {
        data.remove(index);
        fireTableRowsDeleted(0, data.size() - 1);
      }
    }

    public int getColumnCount() {
      return columnNames.length;
    }

    public int getRowCount() {
      return data.size();
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
      return data.get(row).get(col);
    }

    public Class<?> getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }

    public void setValueAt(Object value, int row, int col) {
      data.get(row).set(col, value);
      fireTableCellUpdated(row, col);
    }

    public boolean isCellEditable(int row, int col) {
      return true;
    }
  }

  public boolean isWindows() {
    return osName != null && !osName.contains("darwin") && osName.contains("win");
  }

  private void setBoardSize() {
    int size = Lizzie.config.uiConfig.optInt("board-size", 19);
    txtBoardSize.setEnabled(false);
    switch (size) {
      case 19:
        rdoBoardSize19.setSelected(true);
        break;
      case 13:
        rdoBoardSize13.setSelected(true);
        break;
      case 9:
        rdoBoardSize9.setSelected(true);
        break;
      case 7:
        rdoBoardSize7.setSelected(true);
        break;
      case 5:
        rdoBoardSize5.setSelected(true);
        break;
      case 4:
        rdoBoardSize4.setSelected(true);
        break;
      default:
        txtBoardSize.setText(String.valueOf(size));
        rdoBoardSizeOther.setSelected(true);
        txtBoardSize.setEnabled(true);
        break;
    }
  }

  private int getBoardSize() {
    if (rdoBoardSize19.isSelected()) {
      return 19;
    } else if (rdoBoardSize13.isSelected()) {
      return 13;
    } else if (rdoBoardSize9.isSelected()) {
      return 9;
    } else if (rdoBoardSize7.isSelected()) {
      return 7;
    } else if (rdoBoardSize5.isSelected()) {
      return 5;
    } else if (rdoBoardSize4.isSelected()) {
      return 4;
    } else {
      int size = Integer.parseInt(txtBoardSize.getText().trim());
      if (size < 2) {
        size = 19;
      }
      return size;
    }
  }

  private void setShowMoveNumber() {
    txtShowMoveNumber.setEnabled(false);
    if (Lizzie.config.showMoveNumber) {
      if (Lizzie.config.onlyLastMoveNumber > 0) {
        rdoShowMoveNumberLast.setSelected(true);
        txtShowMoveNumber.setText(String.valueOf(Lizzie.config.onlyLastMoveNumber));
        txtShowMoveNumber.setEnabled(true);
      } else {
        rdoShowMoveNumberAll.setSelected(true);
      }
    } else {
      rdoShowMoveNumberNo.setSelected(true);
    }
  }

  private void setFontValue(JComboBox<String> cmb, String fontName) {
    cmb.setSelectedIndex(0);
    cmb.setSelectedItem(fontName);
  }

  private void readThemeValues() {
    if (cmbThemes.getSelectedIndex() <= 0) {
      // Default
      readDefaultTheme();
    } else {
      // Read the Theme
      String themeName = (String) cmbThemes.getSelectedItem();
      if (themeName == null || themeName.isEmpty()) {
        readDefaultTheme();
      } else {
        theme = new Theme(themeName);
        spnWinrateStrokeWidth.setValue(theme.winrateStrokeWidth());
        spnMinimumBlunderBarWidth.setValue(theme.minimumBlunderBarWidth());
        spnShadowSize.setValue(theme.shadowSize());
        setFontValue(cmbFontName, theme.fontName());
        setFontValue(cmbUiFontName, theme.uiFontName());
        setFontValue(cmbWinrateFontName, theme.winrateFontName());
        txtBackgroundPath.setEnabled(true);
        btnBackgroundPath.setEnabled(true);
        txtBackgroundPath.setText(theme.backgroundPath());
        txtBoardPath.setEnabled(true);
        btnBoardPath.setEnabled(true);
        txtBoardPath.setText(theme.boardPath());
        txtBlackStonePath.setEnabled(true);
        btnBlackStonePath.setEnabled(true);
        txtBlackStonePath.setText(theme.blackStonePath());
        txtWhiteStonePath.setEnabled(true);
        btnWhiteStonePath.setEnabled(true);
        txtWhiteStonePath.setText(theme.whiteStonePath());
        lblWinrateLineColor.setColor(theme.winrateLineColor());
        lblWinrateMissLineColor.setColor(theme.winrateMissLineColor());
        lblBlunderBarColor.setColor(theme.blunderBarColor());
        chkSolidStoneIndicator.setSelected(theme.solidStoneIndicator());
        chkShowCommentNodeColor.setSelected(theme.showCommentNodeColor());
        lblCommentNodeColor.setColor(theme.commentNodeColor());
        tblBlunderNodes.setModel(
            new BlunderNodeTableModel(
                theme.blunderWinrateThresholds().orElse(null),
                theme.blunderNodeColors().orElse(null),
                columsBlunderNodes));
        TableColumn colorCol = tblBlunderNodes.getColumnModel().getColumn(1);
        colorCol.setCellRenderer(new ColorRenderer(false));
        colorCol.setCellEditor(new ColorEditor());
      }
    }
  }

  private void writeThemeValues() {
    if (cmbThemes.getSelectedIndex() <= 0) {
      // Default
      writeDefaultTheme();
    } else {
      // Write the Theme
      String themeName = (String) cmbThemes.getSelectedItem();
      if (themeName == null || themeName.isEmpty()) {
        writeDefaultTheme();
      } else {
        if (theme == null) {
          theme = new Theme(themeName);
        }
        theme.config.put("winrate-stroke-width", spnWinrateStrokeWidth.getValue());
        theme.config.put("minimum-blunder-bar-width", spnMinimumBlunderBarWidth.getValue());
        theme.config.put("shadow-size", spnShadowSize.getValue());
        theme.config.put("font-name", cmbFontName.getSelectedItem());
        theme.config.put("ui-font-name", cmbUiFontName.getSelectedItem());
        theme.config.put("winrate-font-name", cmbWinrateFontName.getSelectedItem());
        theme.config.put("background-image", txtBackgroundPath.getText().trim());
        theme.config.put("board-image", txtBoardPath.getText().trim());
        theme.config.put("black-stone-image", txtBlackStonePath.getText().trim());
        theme.config.put("white-stone-image", txtWhiteStonePath.getText().trim());
        theme.config.put("winrate-line-color", Theme.color2Array(lblWinrateLineColor.getColor()));
        theme.config.put(
            "winrate-miss-line-color", Theme.color2Array(lblWinrateMissLineColor.getColor()));
        theme.config.put("blunder-bar-color", Theme.color2Array(lblBlunderBarColor.getColor()));
        theme.config.put("solid-stone-indicator", chkSolidStoneIndicator.isSelected());
        theme.config.put("show-comment-node-color", chkShowCommentNodeColor.isSelected());
        theme.config.put("comment-node-color", Theme.color2Array(lblCommentNodeColor.getColor()));
        theme.config.put(
            "blunder-winrate-thresholds",
            ((BlunderNodeTableModel) tblBlunderNodes.getModel()).getThresholdArray());
        theme.config.put(
            "blunder-node-colors",
            ((BlunderNodeTableModel) tblBlunderNodes.getModel()).getColorArray());
        theme.save();
      }
    }
  }

  private void readDefaultTheme() {
    spnWinrateStrokeWidth.setValue(Lizzie.config.uiConfig.optInt("winrate-stroke-width", 3));
    spnMinimumBlunderBarWidth.setValue(
        Lizzie.config.uiConfig.optInt("minimum-blunder-bar-width", 3));
    spnShadowSize.setValue(Lizzie.config.uiConfig.optInt("shadow-size", 100));
    cmbFontName.setSelectedItem(Lizzie.config.uiConfig.optString("font-name", null));
    cmbUiFontName.setSelectedItem(Lizzie.config.uiConfig.optString("ui-font-name", null));
    cmbWinrateFontName.setSelectedItem(Lizzie.config.uiConfig.optString("winrate-font-name", null));
    txtBackgroundPath.setEnabled(false);
    btnBackgroundPath.setEnabled(false);
    txtBackgroundPath.setText("/asset/background.jpg");
    txtBoardPath.setEnabled(false);
    btnBoardPath.setEnabled(false);
    txtBoardPath.setText("/asset/board.png");
    txtBlackStonePath.setEnabled(false);
    btnBlackStonePath.setEnabled(false);
    txtBlackStonePath.setText("/asset/black0.png");
    txtWhiteStonePath.setEnabled(false);
    btnWhiteStonePath.setEnabled(false);
    txtWhiteStonePath.setText("/asset/white0.png");
    lblWinrateLineColor.setColor(
        Theme.array2Color(Lizzie.config.uiConfig.optJSONArray("winrate-line-color"), Color.green));
    lblWinrateMissLineColor.setColor(
        Theme.array2Color(
            Lizzie.config.uiConfig.optJSONArray("winrate-miss-line-color"), Color.blue.darker()));
    lblBlunderBarColor.setColor(
        Theme.array2Color(
            Lizzie.config.uiConfig.optJSONArray("blunder-bar-color"), new Color(255, 0, 0, 150)));
    chkSolidStoneIndicator.setSelected(Lizzie.config.uiConfig.optBoolean("solid-stone-indicator"));
    chkShowCommentNodeColor.setSelected(
        Lizzie.config.uiConfig.optBoolean("show-comment-node-color"));
    lblCommentNodeColor.setColor(
        Theme.array2Color(
            Lizzie.config.uiConfig.optJSONArray("comment-node-color"), Color.BLUE.brighter()));
    Theme defTheme = new Theme("");
    tblBlunderNodes.setModel(
        new BlunderNodeTableModel(
            defTheme.blunderWinrateThresholds().orElse(null),
            defTheme.blunderNodeColors().orElse(null),
            columsBlunderNodes));
    TableColumn colorCol = tblBlunderNodes.getColumnModel().getColumn(1);
    colorCol.setCellRenderer(new ColorRenderer(false));
    colorCol.setCellEditor(new ColorEditor());
  }

  private void writeDefaultTheme() {
    Lizzie.config.uiConfig.put("winrate-stroke-width", spnWinrateStrokeWidth.getValue());
    Lizzie.config.uiConfig.put("minimum-blunder-bar-width", spnMinimumBlunderBarWidth.getValue());
    Lizzie.config.uiConfig.put("shadow-size", spnShadowSize.getValue());
    Lizzie.config.uiConfig.put("font-name", cmbFontName.getSelectedItem());
    Lizzie.config.uiConfig.put("ui-font-name", cmbUiFontName.getSelectedItem());
    Lizzie.config.uiConfig.put("winrate-font-name", cmbWinrateFontName.getSelectedItem());
    Lizzie.config.uiConfig.put(
        "winrate-line-color", Theme.color2Array(lblWinrateLineColor.getColor()));
    Lizzie.config.uiConfig.put(
        "winrate-miss-line-color", Theme.color2Array(lblWinrateMissLineColor.getColor()));
    Lizzie.config.uiConfig.put(
        "blunder-bar-color", Theme.color2Array(lblBlunderBarColor.getColor()));
    Lizzie.config.uiConfig.put("solid-stone-indicator", chkSolidStoneIndicator.isSelected());
    Lizzie.config.uiConfig.put("show-comment-node-color", chkShowCommentNodeColor.isSelected());
    Lizzie.config.uiConfig.put(
        "comment-node-color", Theme.color2Array(lblCommentNodeColor.getColor()));
    Lizzie.config.uiConfig.put(
        "blunder-winrate-thresholds",
        ((BlunderNodeTableModel) tblBlunderNodes.getModel()).getThresholdArray());
    Lizzie.config.uiConfig.put(
        "blunder-node-colors",
        ((BlunderNodeTableModel) tblBlunderNodes.getModel()).getColorArray());
  }

  private void saveConfig() {
    try {
      leelazConfig.putOpt("max-analyze-time-minutes", txtFieldIntValue(txtMaxAnalyzeTime));
      leelazConfig.putOpt(
          "analyze-update-interval-centisec", txtFieldIntValue(txtAnalyzeUpdateInterval));
      leelazConfig.putOpt(
          "max-game-thinking-time-seconds", txtFieldIntValue(txtMaxGameThinkingTime));
      leelazConfig.putOpt("print-comms", chkPrintEngineLog.isSelected());
      leelazConfig.put("engine-command", txtEngine.getText().trim());
      JSONArray engines = new JSONArray();
      Arrays.asList(txts).forEach(t -> engines.put(t.getText().trim()));
      leelazConfig.put("engine-command-list", engines);
      Lizzie.config.uiConfig.put("board-size", getBoardSize());
      Lizzie.config.minPlayoutRatioForStats = txtFieldDoubleValue(txtMinPlayoutRatioForStats);
      Lizzie.config.uiConfig.put(
          "min-playout-ratio-for-stats", Lizzie.config.minPlayoutRatioForStats);
      Lizzie.config.uiConfig.putOpt("show-coordinates", chkShowCoordinates.isSelected());
      Lizzie.config.uiConfig.put("board-size", getBoardSize());
      Lizzie.config.showMoveNumber = !rdoShowMoveNumberNo.isSelected();
      Lizzie.config.onlyLastMoveNumber =
          rdoShowMoveNumberLast.isSelected() ? txtFieldIntValue(txtShowMoveNumber) : 0;
      Lizzie.config.allowMoveNumber =
          Lizzie.config.showMoveNumber
              ? (Lizzie.config.onlyLastMoveNumber > 0 ? Lizzie.config.onlyLastMoveNumber : -1)
              : 0;
      Lizzie.config.uiConfig.put("show-move-number", Lizzie.config.showMoveNumber);
      Lizzie.config.uiConfig.put("only-last-move-number", Lizzie.config.onlyLastMoveNumber);

      Lizzie.config.showBlunderBar = chkShowBlunderBar.isSelected();
      Lizzie.config.uiConfig.putOpt("show-blunder-bar", Lizzie.config.showBlunderBar);
      Lizzie.config.dynamicWinrateGraphWidth = chkDynamicWinrateGraphWidth.isSelected();
      Lizzie.config.uiConfig.putOpt(
          "dynamic-winrate-graph-width", Lizzie.config.dynamicWinrateGraphWidth);
      Lizzie.config.appendWinrateToComment = chkAppendWinrateToComment.isSelected();
      Lizzie.config.uiConfig.putOpt(
          "append-winrate-to-comment", Lizzie.config.appendWinrateToComment);
      Lizzie.config.colorByWinrateInsteadOfVisits = chkColorByWinrateInsteadOfVisits.isSelected();
      Lizzie.config.uiConfig.putOpt(
          "color-by-winrate-instead-of-visits", Lizzie.config.colorByWinrateInsteadOfVisits);
      Lizzie.config.boardPositionProportion = sldBoardPositionProportion.getValue();
      Lizzie.config.uiConfig.putOpt(
          "board-postion-proportion", Lizzie.config.boardPositionProportion);
      Lizzie.config.uiConfig.put("theme", cmbThemes.getSelectedItem());
      writeThemeValues();

      Lizzie.config.save();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
