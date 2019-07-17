package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.Leelaz;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
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
  public static JMenuItem[] engine = new JMenuItem[10];
  public static JMenu engineMenu;

  public Menu() {
    setBorder(new EmptyBorder(0, 0, 0, 0));
    final JMenu fileMenu = new JMenu(" 文件  ");
    this.add(fileMenu);

    final JMenuItem open = new JMenuItem("打开棋谱(O)");
    open.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openFile();
          }
        });
    fileMenu.add(open);

    final JMenuItem save = new JMenuItem("保存棋谱(S)");
    save.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.saveFile();
          }
        });
    fileMenu.add(save);

    final JMenuItem openUrl = new JMenuItem("打开在线棋谱(Q)");
    openUrl.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openOnlineDialog();
          }
        });
    fileMenu.add(openUrl);

    fileMenu.addSeparator();
    final JMenuItem copy = new JMenuItem("复制到剪贴板(CTRL+C)");
    copy.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.copySgf();
          }
        });
    fileMenu.add(copy);

    final JMenuItem paste = new JMenuItem("从剪贴板粘贴(CTRL+V)");
    paste.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.pasteSgf();
          }
        });
    fileMenu.add(paste);

    fileMenu.addSeparator();

    final JMenuItem resume = new JMenuItem("恢复棋谱");
    fileMenu.add(resume);
    resume.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.resumePreviousGame();
          }
        });

    final JCheckBoxMenuItem autoSave = new JCheckBoxMenuItem("自动保存棋谱(10秒一次)");
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

    fileMenu.addSeparator();

    final JMenuItem forceExit = new JMenuItem("强制退出");
    forceExit.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            System.exit(0);
          }
        });
    fileMenu.add(forceExit);

    final JMenuItem exit = new JMenuItem();
    exit.setText("退出");
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

    final JMenu viewMenu = new JMenu(" 显示  ");
    this.add(viewMenu);

    final JMenu mainBoardPos = new JMenu("主棋盘位置");
    viewMenu.add(mainBoardPos);

    final JMenuItem left = new JMenuItem("左移([)");
    left.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.boardPositionProportion > 0) Lizzie.frame.boardPositionProportion--;
            Lizzie.frame.refresh(2);
          }
        });
    mainBoardPos.add(left);

    final JMenuItem right = new JMenuItem("右移(])");
    right.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.boardPositionProportion < 8) Lizzie.frame.boardPositionProportion++;
            Lizzie.frame.refresh(2);
          }
        });
    mainBoardPos.add(right);

    final JCheckBoxMenuItem coords = new JCheckBoxMenuItem("坐标(C)");
    coords.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleCoordinates();
          }
        });
    viewMenu.add(coords);

    final JMenu moveMenu = new JMenu("手数(M)");
    viewMenu.add(moveMenu);

    final JCheckBoxMenuItem noMove = new JCheckBoxMenuItem("不显示");
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

    final JCheckBoxMenuItem oneMove = new JCheckBoxMenuItem("最近1手");
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

    final JCheckBoxMenuItem fiveMove = new JCheckBoxMenuItem("最近5手");
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

    final JCheckBoxMenuItem tenMove = new JCheckBoxMenuItem("最近10手");
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

    final JCheckBoxMenuItem allMove = new JCheckBoxMenuItem("全部");
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

    final JMenu panelView = new JMenu("面板");
    viewMenu.add(panelView);

    final JCheckBoxMenuItem subBoard = new JCheckBoxMenuItem("小棋盘(ALT+Z)");
    subBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowSubBoard();
          }
        });
    panelView.add(subBoard);

    final JCheckBoxMenuItem winrateGraph = new JCheckBoxMenuItem("胜率面板(W)");
    winrateGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowWinrate();
          }
        });
    panelView.add(winrateGraph);

    final JCheckBoxMenuItem comment = new JCheckBoxMenuItem("评论面板(T)");
    comment.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowComment();
          }
        });
    panelView.add(comment);

    final JCheckBoxMenuItem variationGraph = new JCheckBoxMenuItem("分支面板(G)");
    variationGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowVariationGraph();
          }
        });
    panelView.add(variationGraph);

    final JCheckBoxMenuItem captured = new JCheckBoxMenuItem("左上角面板");
    captured.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowCaptured();
          }
        });
    panelView.add(captured);

    final JCheckBoxMenuItem status = new JCheckBoxMenuItem("左下角状态");
    status.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleShowStatus();
          }
        });
    panelView.add(status);

    final JCheckBoxMenuItem gtpConsole = new JCheckBoxMenuItem("命令窗口(E)");
    gtpConsole.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.toggleGtpConsole();
          }
        });
    panelView.add(gtpConsole);

    viewMenu.addSeparator();

    final JCheckBoxMenuItem bigSubBoard = new JCheckBoxMenuItem("放大小棋盘(ALT+V)");
    bigSubBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleLargeSubBoard();
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(bigSubBoard);

    final JCheckBoxMenuItem bigWinGraph = new JCheckBoxMenuItem("放大胜率图(Ctrl+W)");
    bigWinGraph.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.toggleLargeWinrate();
            Lizzie.frame.refresh(2);
          }
        });
    viewMenu.add(bigWinGraph);

    final JCheckBoxMenuItem winrateAlwaysBlack = new JCheckBoxMenuItem("总是显示黑胜率");
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

    viewMenu.addSeparator();

    final JMenuItem defaultView = new JMenuItem("默认模式");
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

    final JMenuItem classicView = new JMenuItem("经典模式");
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

    final JMenuItem simpleView = new JMenuItem("精简模式");
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
            int minlength = Math.min(Lizzie.frame.getWidth(), Lizzie.frame.getHeight());
            Lizzie.frame.setBounds(
                Lizzie.frame.getX(), Lizzie.frame.getY(), (int) (minlength * 0.94), minlength);
            Lizzie.frame.refresh(2);
          }
        });

    viewMenu.addSeparator();

    final JMenu kataGo = new JMenu("KataGo相关设置");
    viewMenu.add(kataGo);

    final JMenu kataGoSugg = new JMenu("KataGo推荐点显示");
    kataGo.add(kataGoSugg);

    final JCheckBoxMenuItem kataGoSuggestion1 = new JCheckBoxMenuItem("胜率+计算量", false);
    kataGoSuggestion1.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoScoreMean = false;
            Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataGoSugg.add(kataGoSuggestion1);

    final JCheckBoxMenuItem kataGoSuggestion2 = new JCheckBoxMenuItem("目差+计算量");
    kataGoSuggestion2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoScoreMean = true;
            Lizzie.config.kataGoNotShowWinrate = true;
            Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
            Lizzie.config.uiConfig.put(
                "katago-notshow-winrate", Lizzie.config.kataGoNotShowWinrate);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataGoSugg.add(kataGoSuggestion2);

    final JCheckBoxMenuItem kataGoSuggestion3 = new JCheckBoxMenuItem("胜率+计算量+目差");
    kataGoSuggestion3.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoScoreMean = true;
            Lizzie.config.kataGoNotShowWinrate = false;
            Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
            Lizzie.config.uiConfig.put(
                "katago-notshow-winrate", Lizzie.config.kataGoNotShowWinrate);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });
    kataGoSugg.add(kataGoSuggestion3);

    final JMenu kataScoreMean = new JMenu("KataGo目差显示");
    kataGo.add(kataScoreMean);

    final JCheckBoxMenuItem kataScoreMean1 = new JCheckBoxMenuItem("目差");
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

    final JCheckBoxMenuItem kataScoreMean2 = new JCheckBoxMenuItem("盘面差");
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

    final JMenu kataMeanView = new JMenu("KataGo目差视角");
    kataGo.add(kataMeanView);

    final JCheckBoxMenuItem kataMeanAlwaysBlack = new JCheckBoxMenuItem("永远为黑视角");
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

    final JCheckBoxMenuItem kataMeanBlackWhite = new JCheckBoxMenuItem("黑白交替视角");
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

    final JMenu kataEstimate = new JMenu("KataGo评估显示");
    kataGo.add(kataEstimate);

    final JCheckBoxMenuItem kataEstimate1 = new JCheckBoxMenuItem("关闭评估");
    kataEstimate.add(kataEstimate1);
    kataEstimate1.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = false;
            if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.ponder();
            Lizzie.frame.removeEstimateRect();
          }
        });

    final JCheckBoxMenuItem kataEstimate2 = new JCheckBoxMenuItem("显示在大棋盘上");
    kataEstimate.add(kataEstimate2);
    kataEstimate2.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainbord = true;
            Lizzie.config.showKataGoEstimateOnSubbord = false;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubbord", Lizzie.config.showKataGoEstimateOnSubbord);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainbord);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    final JCheckBoxMenuItem kataEstimate3 = new JCheckBoxMenuItem("显示在小棋盘上");
    kataEstimate.add(kataEstimate3);
    kataEstimate3.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainbord = false;
            Lizzie.config.showKataGoEstimateOnSubbord = true;
            Lizzie.frame.removeEstimateRect();
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubbord", Lizzie.config.showKataGoEstimateOnSubbord);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainbord);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    final JCheckBoxMenuItem kataEstimate4 = new JCheckBoxMenuItem("显示在大小棋盘上");
    kataEstimate.add(kataEstimate4);
    kataEstimate4.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimate = true;
            Lizzie.config.showKataGoEstimateOnMainbord = true;
            Lizzie.config.showKataGoEstimateOnSubbord = true;
            Lizzie.leelaz.ponder();
            Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onsubbord", Lizzie.config.showKataGoEstimateOnSubbord);
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-onmainboard", Lizzie.config.showKataGoEstimateOnMainbord);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    kataEstimate.addSeparator();

    final JCheckBoxMenuItem kataEstimate5 = new JCheckBoxMenuItem("以方块大小表示占有率");
    kataEstimate.add(kataEstimate5);
    kataEstimate5.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimateBySize = true;
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-bysize", Lizzie.config.showKataGoEstimateBySize);
            try {
              Lizzie.config.save();
            } catch (IOException es) {
              // TODO Auto-generated catch block
            }
          }
        });

    final JCheckBoxMenuItem kataEstimate6 = new JCheckBoxMenuItem("以方块透明度表示占有率");
    kataEstimate.add(kataEstimate6);
    kataEstimate6.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.config.showKataGoEstimateBySize = false;
            Lizzie.config.uiConfig.put(
                "show-katago-estimate-bysize", Lizzie.config.showKataGoEstimateBySize);
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
            if (!Lizzie.config.showKataGoScoreMean) kataGoSuggestion1.setState(true);
            else kataGoSuggestion1.setState(false);
            if (Lizzie.config.showKataGoScoreMean && Lizzie.config.kataGoNotShowWinrate)
              kataGoSuggestion2.setState(true);
            else kataGoSuggestion2.setState(false);
            if (Lizzie.config.showKataGoScoreMean && !Lizzie.config.kataGoNotShowWinrate)
              kataGoSuggestion3.setState(true);
            else kataGoSuggestion3.setState(false);
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
            if (Lizzie.config.showKataGoEstimate) {
              kataEstimate1.setState(false);
              if (Lizzie.config.showKataGoEstimateOnMainbord
                  && Lizzie.config.showKataGoEstimateOnSubbord) {
                kataEstimate4.setState(true);
                kataEstimate2.setState(false);
                kataEstimate3.setState(false);
              } else if (Lizzie.config.showKataGoEstimateOnMainbord) {
                kataEstimate2.setState(true);
                kataEstimate4.setState(false);
                kataEstimate3.setState(false);
              } else if (Lizzie.config.showKataGoEstimateOnSubbord) {
                kataEstimate3.setState(true);
                kataEstimate2.setState(false);
                kataEstimate4.setState(false);
              }
            } else {
              kataEstimate1.setState(true);
              kataEstimate2.setState(false);
              kataEstimate3.setState(false);
              kataEstimate4.setState(false);
            }
            if (Lizzie.config.showKataGoEstimateBySize) {
              kataEstimate5.setState(true);
              kataEstimate6.setState(false);
            } else {
              kataEstimate6.setState(true);
              kataEstimate5.setState(false);
            }
            if (Lizzie.config.uiConfig.getBoolean("win-rate-always-black"))
              winrateAlwaysBlack.setState(true);
            else winrateAlwaysBlack.setState(false);
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

    final JMenu gameMenu = new JMenu(" 棋局  ");
    this.add(gameMenu);

    final JMenuItem newGame = new JMenuItem("新对局(N)");
    gameMenu.add(newGame);
    newGame.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
            Lizzie.frame.startGame();
          }
        });

    final JMenuItem continueGameBlack = new JMenuItem("续弈[执黑](回车)");
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

    final JMenuItem continueGameWhite = new JMenuItem("续弈[执白](回车)");
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
    final JMenuItem breakGame = new JMenuItem("中断对局(空格)");
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

    final JMenuItem setInfo = new JMenuItem("设置棋局信息(I)");
    setInfo.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.editGameInfo();
          }
        });
    gameMenu.add(setInfo);

    final JMenuItem bestOne = new JMenuItem("落最佳一手(逗号)");
    bestOne.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.playBestMove();
          }
        });
    gameMenu.add(bestOne);

    final JMenuItem pass = new JMenuItem("停一手(P)");
    pass.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.pass();
          }
        });
    gameMenu.add(pass);
    gameMenu.addSeparator();

    final JMenuItem clearBoard = new JMenuItem("清空棋盘(Ctrl+Home)");
    clearBoard.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.clear();
            Lizzie.frame.refresh(0);
          }
        });
    gameMenu.add(clearBoard);

    final JMenuItem backToMain = new JMenuItem("返回主分支(CTRL+左)");
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

    final JMenuItem gotoStart = new JMenuItem("跳转到最前(Home)");
    gotoStart.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.previousMove()) ;
          }
        });
    gameMenu.add(gotoStart);

    final JMenuItem gotoEnd = new JMenuItem("跳转到最后(End)");
    gotoEnd.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            while (Lizzie.board.nextMove()) ;
          }
        });
    gameMenu.add(gotoEnd);

    final JMenuItem gotoLeft = new JMenuItem("跳转到左分支(左)");
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

    final JMenuItem gotoRight = new JMenuItem("跳转到右分支(右)");
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

    final JMenu analyzeMenu = new JMenu(" 分析  ", false);
    this.add(analyzeMenu);

    final JMenuItem toggleAnalyze = new JMenuItem("分析/停止(空格)");
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

    final JMenuItem autoAnalyze = new JMenuItem("自动分析(A)");
    autoAnalyze.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.board.toggleAnalysis();
          }
        });
    analyzeMenu.add(autoAnalyze);

    final JMenuItem estimate = new JMenuItem("形势判断(点)");
    estimate.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.estimateByZen();
          }
        });
    analyzeMenu.add(estimate);

    engineMenu = new JMenu(" 引擎  ");
    this.add(engineMenu);

    for (int i = 0; i < engine.length; i++) {
      engine[i] = new JMenuItem();
      engineMenu.add(engine[i]);
      engine[i].setText("引擎" + i);
      engine[i].setVisible(false);
    }

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

    final JMenu configMenu = new JMenu(" 设置  ");
    this.add(configMenu);

    final JMenuItem engineConfig = new JMenuItem("引擎设置");
    engineConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(0);
          }
        });
    configMenu.add(engineConfig);

    final JMenuItem viewConfig = new JMenuItem("界面设置");
    viewConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(1);
          }
        });
    configMenu.add(viewConfig);

    final JMenuItem themeConfig = new JMenuItem("主题设置");
    themeConfig.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(2);
          }
        });
    configMenu.add(themeConfig);

    final JMenuItem about = new JMenuItem("关于");
    about.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.openConfigDialog(3);
          }
        });
    configMenu.add(about);
  }

  public void updateEngineMenu(List<Leelaz> engineList) {
    for (int i = 0; i < engineList.size(); i++) {
      Leelaz engineDt = engineList.get(i);
      if (engineDt != null) {
        if (engineDt.currentWeight() != "")
          engine[i].setText(engine[i].getText() + ": " + engineDt.currentWeight());
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
        if (i == currentEngineNo) engine[i].setIcon(running);
        else if (engineDt.isLoaded()) engine[i].setIcon(ready);
      }
    }
  }
}
