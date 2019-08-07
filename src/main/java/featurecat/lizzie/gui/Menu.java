package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class Menu extends JMenuBar {

  final ButtonGroup buttonGroup = new ButtonGroup();

  Font headFont;
  public static ImageIcon running;
  public static ImageIcon ready;
  public static JMenuItem[] engine;
  public static JMenu engineMenu;
  private static final ResourceBundle resourceBundle = MainFrame.resourceBundle;

  public Menu() {
    setBorder(new EmptyBorder(0, 0, 0, 0));
    final JMenu fileMenu = new JMenu(resourceBundle.getString("Menu.file"));
    this.add(fileMenu);

    final JMenuItem open = new JMenuItem(resourceBundle.getString("Menu.file.open"));
    open.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openFile();
          }
        });
    fileMenu.add(open);

    final JMenuItem save = new JMenuItem(resourceBundle.getString("Menu.file.save"));
    save.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.saveFile();
          }
        });
    fileMenu.add(save);

    final JMenuItem openUrl = new JMenuItem(resourceBundle.getString("Menu.file.openUrl"));
    openUrl.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openOnlineDialog();
          }
        });
    fileMenu.add(openUrl);

    fileMenu.addSeparator();
    final JMenuItem copy = new JMenuItem(resourceBundle.getString("Menu.file.copy"));
    copy.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.copySgf();
          }
        });
    fileMenu.add(copy);

    final JMenuItem paste = new JMenuItem(resourceBundle.getString("Menu.file.paste"));
    paste.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.pasteSgf();
          }
        });
    fileMenu.add(paste);

    fileMenu.addSeparator();

    final JCheckBoxMenuItem autoSave =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.file.autoSave"));
    autoSave.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.config.uiConfig.optInt("autosave-interval-seconds", -1) > 0) {
              Lizzie.config.uiConfig.put("autosave-interval-seconds", -1);
              Lizzie.config.uiConfig.put("resume-previous-game", false);
              try {
                Lizzie.config.save();
              } catch (IOException es) {
                // TODO Auto-generated catch block
              }
            } else {
              Lizzie.config.uiConfig.put("autosave-interval-seconds", 10);
              Lizzie.config.uiConfig.put("resume-previous-game", true);
              try {
                Lizzie.config.save();
              } catch (IOException es) {
                // TODO Auto-generated catch block
              }
            }
          }
        });
    fileMenu.add(autoSave);

    final JMenuItem resume = new JMenuItem(resourceBundle.getString("Menu.file.resume"));
    fileMenu.add(resume);
    resume.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.resumePreviousGame();
          }
        });

    fileMenu.addSeparator();

    final JMenuItem forceExit = new JMenuItem(resourceBundle.getString("Menu.file.forceExit"));
    forceExit.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            System.exit(0);
          }
        });
    fileMenu.add(forceExit);

    final JMenuItem exit = new JMenuItem();
    exit.setText(resourceBundle.getString("Menu.file.exit"));
    exit.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.shutdown();
          }
        });
    fileMenu.add(exit);

    fileMenu.addMenuListener(
        new MenuListener() {
          public void menuSelected(MenuEvent e) {
            if (Lizzie.config.uiConfig.optInt("autosave-interval-seconds", -1) > 0)
              autoSave.setState(true);
            else autoSave.setState(false);
          }

          @Override
          public void menuDeselected(MenuEvent e) {}

          @Override
          public void menuCanceled(MenuEvent e) {}
        });

    final JMenu viewMenu = new JMenu(resourceBundle.getString("Menu.view"));
    this.add(viewMenu);

    final JMenu mainBoardPos = new JMenu(resourceBundle.getString("Menu.view.mainBoardPos"));
    viewMenu.add(mainBoardPos);

    final JMenuItem left = new JMenuItem(resourceBundle.getString("Menu.view.mainBoardPos.left"));
    left.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.boardPositionProportion > 0) Lizzie.frame.boardPositionProportion--;
            Lizzie.frame.refresh(2);
          }
        });
    mainBoardPos.add(left);

    final JMenuItem right = new JMenuItem(resourceBundle.getString("Menu.view.mainBoardPos.right"));
    right.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.boardPositionProportion < 8) Lizzie.frame.boardPositionProportion++;
            Lizzie.frame.refresh(2);
          }
        });
    mainBoardPos.add(right);

    final JCheckBoxMenuItem coords =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.coords"));
    coords.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleCoordinates();
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(coords);

    final JMenu moveMenu = new JMenu(resourceBundle.getString("Menu.view.move"));
    viewMenu.add(moveMenu);

    final JCheckBoxMenuItem noMove =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.move.noMove"));
    noMove.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.allowMoveNumber = 0;
            Lizzie.config.uiConfig.put("allow-move-number", 0);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
            }
          }
        });
    moveMenu.add(noMove);

    final JCheckBoxMenuItem oneMove =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.move.oneMove"));
    oneMove.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.allowMoveNumber = 1;
            Lizzie.config.uiConfig.put("allow-move-number", 1);
            Lizzie.config.onlyLastMoveNumber = 1;
            Lizzie.config.uiConfig.put("only-last-move-number", 1);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
            }
          }
        });
    moveMenu.add(oneMove);

    final JCheckBoxMenuItem fiveMove =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.move.fiveMove"));
    fiveMove.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.allowMoveNumber = 5;
            Lizzie.config.uiConfig.put("allow-move-number", 5);
            Lizzie.config.onlyLastMoveNumber = 5;
            Lizzie.config.uiConfig.put("only-last-move-number", 5);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
            }
          }
        });
    moveMenu.add(fiveMove);

    final JCheckBoxMenuItem tenMove =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.move.tenMove"));
    tenMove.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.allowMoveNumber = 10;
            Lizzie.config.uiConfig.put("allow-move-number", 10);
            Lizzie.config.onlyLastMoveNumber = 10;
            Lizzie.config.uiConfig.put("only-last-move-number", 10);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
            }
          }
        });
    moveMenu.add(tenMove);

    final JCheckBoxMenuItem allMove =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.move.allMove"));
    allMove.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.allowMoveNumber = -1;
            Lizzie.config.uiConfig.put("allow-move-number", -1);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
            }
          }
        });
    moveMenu.add(allMove);

    final JMenu Suggestions = new JMenu(resourceBundle.getString("Menu.view.Suggestions"));
    viewMenu.add(Suggestions);

    final JCheckBoxMenuItem suggestion1 =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.Suggestions.Suggestion1"));
    suggestion1.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showWinrateInSuggestion = !Lizzie.config.showWinrateInSuggestion;
            Lizzie.config.uiConfig.put(
                "show-winrate-in-suggestion", Lizzie.config.showWinrateInSuggestion);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    Suggestions.add(suggestion1);

    final JCheckBoxMenuItem suggestion2 =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.Suggestions.Suggestion2"));
    suggestion2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showPlayoutsInSuggestion = !Lizzie.config.showPlayoutsInSuggestion;
            Lizzie.config.uiConfig.put(
                "show-playouts-in-suggestion", Lizzie.config.showPlayoutsInSuggestion);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    Suggestions.add(suggestion2);

    final JCheckBoxMenuItem suggestion3 =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.Suggestions.Suggestion3"));
    suggestion3.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showScoremeanInSuggestion = !Lizzie.config.showScoremeanInSuggestion;
            Lizzie.config.uiConfig.put(
                "show-scoremean-in-suggestion", Lizzie.config.showScoremeanInSuggestion);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    Suggestions.add(suggestion3);

    final JMenu panelView = new JMenu(resourceBundle.getString("Menu.view.panelView"));
    viewMenu.add(panelView);

    final JCheckBoxMenuItem subBoard =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.subBoard"));
    subBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowSubBoard();
          }
        });
    panelView.add(subBoard);

    final JCheckBoxMenuItem winrateGraph =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.winrateGraph"));
    winrateGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowWinrate();
          }
        });
    panelView.add(winrateGraph);

    final JCheckBoxMenuItem comment =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.comment"));
    comment.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowComment();
          }
        });
    panelView.add(comment);

    final JCheckBoxMenuItem variationGraph =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.variationGraph"));
    variationGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowVariationGraph();
          }
        });
    panelView.add(variationGraph);

    final JCheckBoxMenuItem captured =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.captured"));
    captured.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowCaptured();
          }
        });
    panelView.add(captured);

    final JCheckBoxMenuItem status =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.status"));
    status.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowStatus();
          }
        });
    panelView.add(status);

    final JCheckBoxMenuItem gtpConsole =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.gtpConsole"));
    gtpConsole.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.toggleGtpConsole();
          }
        });
    panelView.add(gtpConsole);

    final JCheckBoxMenuItem toolBar =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.panelView.toolBar"));
    toolBar.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.toggleToolBar();
          }
        });
    panelView.add(toolBar);

    viewMenu.addSeparator();

    final JCheckBoxMenuItem bigSubBoard =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.bigSubBoard"));
    bigSubBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleLargeSubBoard();
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(bigSubBoard);

    final JCheckBoxMenuItem bigWinGraph =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.bigWinGraph"));
    bigWinGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleLargeWinrate();
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(bigWinGraph);

    final JCheckBoxMenuItem winrateAlwaysBlack =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.winrateAlwaysBlack"));
    winrateAlwaysBlack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.config.uiConfig.getBoolean("win-rate-always-black")) {
              Lizzie.config.uiConfig.put("win-rate-always-black", false);
              try {
                Lizzie.config.save();
              } catch (IOException es) {
                // TODO Auto-generated catch block
              }
            } else {
              Lizzie.config.uiConfig.put("win-rate-always-black", true);
              try {
                Lizzie.config.save();
              } catch (IOException es) {
                // TODO Auto-generated catch block
              }
            }
          }
        });
    viewMenu.add(winrateAlwaysBlack);

    final JCheckBoxMenuItem showName =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.showName"));
    showName.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showNameInBoard = !Lizzie.config.showNameInBoard;
            Lizzie.config.uiConfig.put("show-name-in-board", Lizzie.config.showNameInBoard);
            Lizzie.frame.refresh(2);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    viewMenu.add(showName);

    viewMenu.addSeparator();

    final JMenuItem defaultView = new JMenuItem(resourceBundle.getString("Menu.view.defaultView"));
    defaultView.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
            if (!Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
            if (Lizzie.config.showLargeSubBoard()) Lizzie.config.toggleLargeSubBoard();
            if (Lizzie.config.showLargeWinrate()) Lizzie.config.toggleLargeWinrate();
            if (!Lizzie.config.showComment) Lizzie.config.toggleShowComment();
            if (!Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
            if (!Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
            if (!Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
            if (Lizzie.frame.getWidth() - Lizzie.frame.getHeight() < 450)
              Lizzie.frame.setBounds(
                  Lizzie.frame.getX(),
                  Lizzie.frame.getY(),
                  Lizzie.frame.getHeight() + 450,
                  Lizzie.frame.getHeight());
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(defaultView);

    final JMenuItem classicView = new JMenuItem(resourceBundle.getString("Menu.view.classicView"));
    classicView.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
            if (!Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
            if (Lizzie.config.showLargeWinrate()) Lizzie.config.toggleLargeWinrate();
            if (!Lizzie.config.showLargeSubBoard()) Lizzie.config.toggleLargeSubBoard();
            if (!Lizzie.config.showComment) Lizzie.config.toggleShowComment();
            if (!Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
            if (Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
            if (!Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
            if (Lizzie.frame.getWidth() - Lizzie.frame.getHeight() < 450)
              Lizzie.frame.setBounds(
                  Lizzie.frame.getX(),
                  Lizzie.frame.getY(),
                  Lizzie.frame.getHeight() + 450,
                  Lizzie.frame.getHeight());
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(classicView);

    final JMenuItem simpleView = new JMenuItem(resourceBundle.getString("Menu.view.simpleView"));
    viewMenu.add(simpleView);
    simpleView.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
            if (Lizzie.config.showComment) Lizzie.config.toggleShowComment();
            if (Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
            if (Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
            if (Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
            if (Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
            int minlength =
                Math.min(Lizzie.frame.getWidth(), Lizzie.frame.getHeight()) < 681
                    ? 681
                    : Math.min(Lizzie.frame.getWidth(), Lizzie.frame.getHeight());
            Lizzie.frame.setBounds(
                Lizzie.frame.getX(), Lizzie.frame.getY(), (int) (minlength * 0.94), minlength);
            Lizzie.frame.refresh(2);
          }
        });

    viewMenu.addSeparator();

    final JMenu kataGo = new JMenu(resourceBundle.getString("Menu.view.kataGo"));
    viewMenu.add(kataGo);

    final JMenu kataScoreMean =
        new JMenu(resourceBundle.getString("Menu.view.kataGo.kataScoreMean"));
    kataGo.add(kataScoreMean);

    final JCheckBoxMenuItem kataScoreMean1 =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataScoreMean.kataScoreMean1"));
    kataScoreMean1.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoBoardScoreMean = false;
            Lizzie.config.uiConfig.put(
                "show-katago-boardscoremean", Lizzie.config.showKataGoBoardScoreMean);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataScoreMean.add(kataScoreMean1);

    final JCheckBoxMenuItem kataScoreMean2 =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataScoreMean.kataScoreMean2"));
    kataScoreMean2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoBoardScoreMean = true;
            Lizzie.config.uiConfig.put(
                "show-katago-boardscoremean", Lizzie.config.showKataGoBoardScoreMean);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataScoreMean.add(kataScoreMean2);

    final JMenu kataMeanView = new JMenu(resourceBundle.getString("Menu.view.kataGo.kataMeanView"));
    kataGo.add(kataMeanView);

    final JCheckBoxMenuItem kataMeanAlwaysBlack =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataMeanView.kataMeanAlwaysBlack"));
    kataMeanAlwaysBlack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoScoreMeanAlwaysBlack = true;
            Lizzie.config.uiConfig.put(
                "katago-scoremean-alwaysblack", Lizzie.config.kataGoScoreMeanAlwaysBlack);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataMeanView.add(kataMeanAlwaysBlack);

    final JCheckBoxMenuItem kataMeanBlackWhite =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataMeanView.kataMeanBlackWhite"));
    kataMeanBlackWhite.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoScoreMeanAlwaysBlack = false;
            Lizzie.config.uiConfig.put(
                "katago-scoremean-alwaysblack", Lizzie.config.kataGoScoreMeanAlwaysBlack);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataMeanView.add(kataMeanBlackWhite);

    final JMenu kataEstimate = new JMenu(resourceBundle.getString("Menu.view.kataGo.kataEstimate"));
    kataGo.add(kataEstimate);

    final JMenu kataEstimateDisplay =
        new JMenu(resourceBundle.getString("Menu.view.kataGo.kataEstimate.display"));
    kataEstimate.add(kataEstimateDisplay);

    final JCheckBoxMenuItem kataEstimateDisplayNone =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.display.none"));

    final JCheckBoxMenuItem kataEstimateDisplayMain =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.display.main"));

    final JCheckBoxMenuItem kataEstimateDisplaySub =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.display.sub"));

    final JCheckBoxMenuItem kataEstimateDisplayBoth =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.display.both"));

    kataEstimateDisplay.add(kataEstimateDisplayNone);
    kataEstimateDisplay.add(kataEstimateDisplayMain);
    kataEstimateDisplay.add(kataEstimateDisplaySub);
    kataEstimateDisplay.add(kataEstimateDisplayBoth);

    kataEstimateDisplayNone.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = false;
            if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.ponder();
            Lizzie.frame.removeEstimateRect();
          }
        });

    kataEstimateDisplayMain.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainboard = true;
            Lizzie.config.showKataGoEstimateOnSubboard = false;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubboard", Lizzie.config.showKataGoEstimateOnSubboard);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainboard);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateDisplaySub.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainboard = false;
            Lizzie.config.showKataGoEstimateOnSubboard = true;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubboard", Lizzie.config.showKataGoEstimateOnSubboard);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainboard);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateDisplayBoth.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainboard = true;
            Lizzie.config.showKataGoEstimateOnSubboard = true;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubboard", Lizzie.config.showKataGoEstimateOnSubboard);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainboard);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    final JMenu kataEstimateMode =
        new JMenu(resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode"));
    kataEstimate.add(kataEstimateMode);

    final JCheckBoxMenuItem kataEstimateModeLarge =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.large"));

    final JCheckBoxMenuItem kataEstimateModeSmall =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.small"));

    final JCheckBoxMenuItem kataEstimateModeLargeAndSmall =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.largeAndSmall"));

    final JCheckBoxMenuItem kataEstimateModeLargeAndDead =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.largeAndDead"));

    final JCheckBoxMenuItem kataEstimateModeLargeAndStones =
        new JCheckBoxMenuItem(
            resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.largeAndStones"));

    final JCheckBoxMenuItem kataEstimateModeSize =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.view.kataGo.kataEstimate.mode.size"));

    kataEstimateMode.add(kataEstimateModeLarge);
    kataEstimateMode.add(kataEstimateModeSmall);
    kataEstimateMode.add(kataEstimateModeLargeAndSmall);
    kataEstimateMode.add(kataEstimateModeLargeAndDead);
    kataEstimateMode.add(kataEstimateModeLargeAndStones);
    kataEstimateMode.add(kataEstimateModeSize);

    kataEstimateModeLarge.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "large";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateModeSmall.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "small";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateModeLargeAndSmall.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "large+small";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateModeLargeAndDead.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "large+dead";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateModeLargeAndStones.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "large+stones";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimateModeSize.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.kataGoEstimateMode = "size";
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("katago-estimate-mode", Lizzie.config.kataGoEstimateMode);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    viewMenu.addMenuListener(
        new MenuListener() {
          public void menuSelected(MenuEvent e) {
            if (Lizzie.config.showWinrateInSuggestion) suggestion1.setState(true);
            else suggestion1.setState(false);
            if (Lizzie.config.showPlayoutsInSuggestion) suggestion2.setState(true);
            else suggestion2.setState(false);
            if (Lizzie.config.showScoremeanInSuggestion) suggestion3.setState(true);
            else suggestion3.setState(false);
            if (Lizzie.config.showKataGoBoardScoreMean) {
              kataScoreMean1.setState(false);
              kataScoreMean2.setState(true);
            } else {
              kataScoreMean1.setState(true);
              kataScoreMean2.setState(false);
            }
            if (Lizzie.config.kataGoScoreMeanAlwaysBlack) {
              kataMeanAlwaysBlack.setState(true);
              kataMeanBlackWhite.setState(false);
            } else {
              kataMeanAlwaysBlack.setState(false);
              kataMeanBlackWhite.setState(true);
            }
            {
              boolean onMain =
                  Lizzie.config.showKataGoEstimate && Lizzie.config.showKataGoEstimateOnMainboard;
              boolean onSub =
                  Lizzie.config.showKataGoEstimate && Lizzie.config.showKataGoEstimateOnSubboard;
              kataEstimateDisplayNone.setState(!onMain && !onSub);
              kataEstimateDisplayMain.setState(onMain && !onSub);
              kataEstimateDisplaySub.setState(!onMain && onSub);
              kataEstimateDisplayBoth.setState(onMain && onSub);
            }
            kataEstimateModeLarge.setState(Lizzie.config.kataGoEstimateMode.equals("large"));
            kataEstimateModeSmall.setState(Lizzie.config.kataGoEstimateMode.equals("small"));
            kataEstimateModeLargeAndSmall.setState(
                Lizzie.config.kataGoEstimateMode.equals("large+small"));
            kataEstimateModeLargeAndDead.setState(
                Lizzie.config.kataGoEstimateMode.equals("large+dead"));
            kataEstimateModeLargeAndStones.setState(
                Lizzie.config.kataGoEstimateMode.equals("large+stones"));
            kataEstimateModeSize.setState(Lizzie.config.kataGoEstimateMode.equals("size"));
            if (Lizzie.config.uiConfig.getBoolean("win-rate-always-black"))
              winrateAlwaysBlack.setState(true);
            else winrateAlwaysBlack.setState(false);
            if (Lizzie.config.showNameInBoard) showName.setState(true);
            else showName.setState(false);
            if (Lizzie.config.showWinrate && Lizzie.config.showLargeWinrate())
              bigWinGraph.setState(true);
            else bigWinGraph.setState(false);
            if (Lizzie.config.showSubBoard && Lizzie.config.showLargeSubBoard())
              bigSubBoard.setState(true);
            else bigSubBoard.setState(false);
            if (Lizzie.config.showSubBoard) subBoard.setState(true);
            else subBoard.setState(false);
            if (Lizzie.config.showWinrate) winrateGraph.setState(true);
            else winrateGraph.setState(false);
            if (Lizzie.config.showComment) comment.setState(true);
            else comment.setState(false);
            if (Lizzie.config.showVariationGraph) variationGraph.setState(true);
            else variationGraph.setState(false);
            if (Lizzie.config.showCaptured) captured.setState(true);
            else captured.setState(false);
            if (Lizzie.config.showStatus) status.setState(true);
            else status.setState(false);
            if (Lizzie.gtpConsole.isVisible()) gtpConsole.setState(true);
            else gtpConsole.setState(false);
            if (Lizzie.config.showToolBar) toolBar.setState(true);
            else toolBar.setSelected(false);
            if (Lizzie.config.showCoordinates) coords.setState(true);
            else coords.setState(false);
            switch (Lizzie.config.allowMoveNumber) {
              case 0:
                noMove.setState(true);
                oneMove.setState(false);
                fiveMove.setState(false);
                tenMove.setState(false);
                allMove.setState(false);
                break;
              case 1:
                noMove.setState(false);
                oneMove.setState(true);
                fiveMove.setState(false);
                tenMove.setState(false);
                allMove.setState(false);
                break;
              case 5:
                noMove.setState(false);
                oneMove.setState(false);
                fiveMove.setState(true);
                tenMove.setState(false);
                allMove.setState(false);
                break;
              case 10:
                noMove.setState(false);
                oneMove.setState(false);
                fiveMove.setState(false);
                tenMove.setState(true);
                allMove.setState(false);
                break;
              case -1:
                noMove.setState(false);
                oneMove.setState(false);
                fiveMove.setState(false);
                tenMove.setState(false);
                allMove.setState(true);
                break;
              default:
                noMove.setState(false);
                oneMove.setState(false);
                fiveMove.setState(false);
                tenMove.setState(false);
                allMove.setState(false);
            }
          }

          @Override
          public void menuDeselected(MenuEvent e) {}

          @Override
          public void menuCanceled(MenuEvent e) {}
        });

    final JMenu gameMenu = new JMenu(resourceBundle.getString("Menu.game"));
    this.add(gameMenu);

    final JMenuItem newGame = new JMenuItem(resourceBundle.getString("Menu.game.newGame"));
    gameMenu.add(newGame);
    newGame.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
            Lizzie.frame.startGame();
          }
        });

    final JMenuItem continueGameBlack =
        new JMenuItem(resourceBundle.getString("Menu.game.continueGameBlack"));
    continueGameBlack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            boolean playerIsBlack = true;
            Lizzie.leelaz.sendCommand(
                "time_settings 0 "
                    + Lizzie.config
                        .config
                        .getJSONObject("leelaz")
                        .getInt("max-game-thinking-time-seconds")
                    + " 1");
            Lizzie.frame.playerIsBlack = playerIsBlack;
            Lizzie.frame.isPlayingAgainstLeelaz = true;
            if (Lizzie.board.getData().blackToPlay != playerIsBlack) {
              Lizzie.leelaz.genmove("W");
            }
          }
        });
    gameMenu.add(continueGameBlack);

    final JMenuItem continueGameWhite =
        new JMenuItem(resourceBundle.getString("Menu.game.continueGameWhite"));
    continueGameWhite.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            boolean playerIsBlack = false;
            Lizzie.leelaz.sendCommand(
                "time_settings 0 "
                    + Lizzie.config
                        .config
                        .getJSONObject("leelaz")
                        .getInt("max-game-thinking-time-seconds")
                    + " 1");
            Lizzie.frame.playerIsBlack = playerIsBlack;
            Lizzie.frame.isPlayingAgainstLeelaz = true;
            if (Lizzie.board.getData().blackToPlay != playerIsBlack) {
              Lizzie.leelaz.genmove("B");
            }
          }
        });
    gameMenu.add(continueGameWhite);

    gameMenu.addSeparator();
    final JMenuItem breakGame = new JMenuItem(resourceBundle.getString("Menu.game.breakGame"));
    breakGame.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.isPlayingAgainstLeelaz) {
              Lizzie.frame.isPlayingAgainstLeelaz = false;
              Lizzie.leelaz.isThinking = false;
              Lizzie.leelaz.ponder();
            }
          }
        });
    gameMenu.add(breakGame);

    final JMenuItem setInfo = new JMenuItem(resourceBundle.getString("Menu.game.setInfo"));
    setInfo.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.editGameInfo();
          }
        });
    gameMenu.add(setInfo);

    final JMenuItem bestOne = new JMenuItem(resourceBundle.getString("Menu.game.bestOne"));
    bestOne.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.playBestMove();
          }
        });
    gameMenu.add(bestOne);

    final JMenuItem pass = new JMenuItem(resourceBundle.getString("Menu.game.pass"));
    pass.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.pass();
          }
        });
    gameMenu.add(pass);
    gameMenu.addSeparator();

    final JMenuItem clearBoard = new JMenuItem(resourceBundle.getString("Menu.game.clearBoard"));
    clearBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.clear();
            Lizzie.frame.refresh(0);
          }
        });
    gameMenu.add(clearBoard);

    final JMenuItem backToMain = new JMenuItem(resourceBundle.getString("Menu.game.backToMain"));
    backToMain.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.board.undoToChildOfPreviousWithVariation()) {
              Lizzie.board.previousMove();
            }
          }
        });
    gameMenu.add(backToMain);

    final JMenuItem gotoStart = new JMenuItem(resourceBundle.getString("Menu.game.gotoStart"));
    gotoStart.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.previousMove()) ;
          }
        });
    gameMenu.add(gotoStart);

    final JMenuItem gotoEnd = new JMenuItem(resourceBundle.getString("Menu.game.gotoEnd"));
    gotoEnd.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.nextMove()) ;
          }
        });
    gameMenu.add(gotoEnd);

    final JMenuItem gotoLeft = new JMenuItem(resourceBundle.getString("Menu.game.gotoLeft"));
    gotoLeft.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.isPlayingAgainstLeelaz) {
              Lizzie.frame.isPlayingAgainstLeelaz = false;
            }
            Lizzie.board.previousBranch();
          }
        });
    gameMenu.add(gotoLeft);

    final JMenuItem gotoRight = new JMenuItem(resourceBundle.getString("Menu.game.gotoRight"));
    gotoRight.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.isPlayingAgainstLeelaz) {
              Lizzie.frame.isPlayingAgainstLeelaz = false;
            }
            Lizzie.board.nextBranch();
          }
        });
    gameMenu.add(gotoRight);

    final JMenu analyzeMenu = new JMenu(resourceBundle.getString("Menu.analyze"));
    this.add(analyzeMenu);

    final JMenuItem toggleAnalyze =
        new JMenuItem(resourceBundle.getString("Menu.analyze.toggleAnalyze"));
    toggleAnalyze.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.isPlayingAgainstLeelaz) {
              Lizzie.frame.isPlayingAgainstLeelaz = false;
              Lizzie.leelaz.isThinking = false;
            }
            Lizzie.leelaz.togglePonder();
          }
        });
    analyzeMenu.add(toggleAnalyze);

    final JMenuItem autoAnalyze =
        new JMenuItem(resourceBundle.getString("Menu.analyze.autoAnalyze"));
    autoAnalyze.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.toggleAnalysis();
          }
        });
    analyzeMenu.add(autoAnalyze);

    final JCheckBoxMenuItem showPolicy =
        new JCheckBoxMenuItem(resourceBundle.getString("Menu.analyze.showPolicy"));
    showPolicy.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowPolicy();
          }
        });
    analyzeMenu.add(showPolicy);

    final JMenuItem estimate = new JMenuItem(resourceBundle.getString("Menu.analyze.estimate"));
    estimate.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.estimateByZen();
          }
        });
    analyzeMenu.add(estimate);

    analyzeMenu.addMenuListener(
        new MenuListener() {
          public void menuSelected(MenuEvent e) {
            if (Lizzie.frame.isShowingPolicy) showPolicy.setState(true);
            else showPolicy.setState(false);
          }

          @Override
          public void menuDeselected(MenuEvent e) {}

          @Override
          public void menuCanceled(MenuEvent e) {}
        });

    final JMenu configMenu = new JMenu(resourceBundle.getString("Menu.configMenu"));
    this.add(configMenu);

    final JMenuItem engineConfig =
        new JMenuItem(resourceBundle.getString("Menu.configMenu.engineConfig"));
    engineConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(0);
          }
        });
    configMenu.add(engineConfig);

    final JMenuItem viewConfig =
        new JMenuItem(resourceBundle.getString("Menu.configMenu.viewConfig"));
    viewConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(1);
          }
        });
    configMenu.add(viewConfig);

    final JMenuItem themeConfig =
        new JMenuItem(resourceBundle.getString("Menu.configMenu.themeConfig"));
    themeConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(2);
          }
        });
    configMenu.add(themeConfig);

    final JMenuItem about = new JMenuItem(resourceBundle.getString("Menu.configMenu.about"));
    about.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(3);
          }
        });
    configMenu.add(about);

    engineMenu = new JMenu(resourceBundle.getString("Menu.engineMenu"));
    this.add(engineMenu);

    running = new ImageIcon();
    try {
      running.setImage(ImageIO.read(getClass().getResourceAsStream("/assets/running.png")));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ready = new ImageIcon();
    try {
      ready.setImage(ImageIO.read(getClass().getResourceAsStream("/assets/ready.png")));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void updateEngineMenu(List<Leelaz> engineList) {
    engine = new JMenuItem[engineList.size()];
    engineMenu.removeAll();
    for (int i = 0; i < engineList.size(); i++) {
      engine[i] = new JMenuItem();
      engineMenu.add(engine[i]);
      engine[i].setText(resourceBundle.getString("Menu.engineMenu") + i);
      engine[i].setVisible(false);
      Leelaz engineDt = engineList.get(i);
      if (engineDt != null) {
        if (engineDt.currentWeight() != "")
          engine[i].setText(engine[i].getText() + " : " + engineDt.currentWeight());
        engine[i].setVisible(true);
        int a = i;
        engine[i].addActionListener(
            new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                Lizzie.engineManager.switchEngine(a);
              }
            });
      }
    }
  }

  public void updateEngineIcon(List<Leelaz> engineList, int currentEngineNo) {
    for (int i = 0; i < engineList.size(); i++) {
      Leelaz engineDt = engineList.get(i);
      if (engineDt != null) {
        if (i == currentEngineNo) {
          engine[i].setIcon(running);
          engineMenu.setText(engine[i].getText());
        } else if (engineDt.isLoaded()) engine[i].setIcon(ready);
      }
    }
  }
}
