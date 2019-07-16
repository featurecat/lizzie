package featurecat.lizzie.gui;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.analysis.GameInfo;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.json.JSONArray;

public class Menu extends JMenuBar {

  final ButtonGroup buttonGroup = new ButtonGroup();

  Font headFont;
  public static ImageIcon icon;
  public static ImageIcon stop;
  public static ImageIcon ready;
  public static JMenuItem[] engine = new JMenuItem[21];
  public static JMenu engineMenu;
  public static JMenu closeEngine;
  // public static MenuBar menuBar;
  JMenuItem closeall;
  JMenuItem forcecloseall;
  JMenuItem closeother;
  JMenuItem restartZen;
  JMenuItem config;
  JMenuItem moreconfig;
  // private boolean onlyboard = false;

  public Menu() {
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
          public void menuDeselected(MenuEvent e) {            
          }

          @Override
          public void menuCanceled(MenuEvent e) {            
        	  }
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
                    Lizzie.config.uiConfig.put("only-last-move-number",1);
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
                    Lizzie.config.uiConfig.put("only-last-move-number",5);
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
                    Lizzie.config.uiConfig.put("only-last-move-number",10);
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
    
    final JCheckBoxMenuItem bigSubBoard = new JCheckBoxMenuItem("放大小棋盘(ALT+V)");
    bigSubBoard.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                  
                	Lizzie.config.toggleLargeSubBoard();
                }
              });
    viewMenu.add(bigSubBoard);
   
    final JCheckBoxMenuItem bigWinGraph = new JCheckBoxMenuItem("放大胜率图(Ctrl+W)");
    bigWinGraph.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                  
                	Lizzie.config.toggleLargeWinrate();
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
    
    final JMenu panelView = new JMenu("面板显示");
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
                    Lizzie.frame.refresh(0);
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
                       Lizzie.frame.refresh(0);
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
                       Lizzie.frame.refresh(0);
                	}
              });
    
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
                    Lizzie.config.uiConfig.put("katago-notshow-winrate", Lizzie.config.kataGoNotShowWinrate);
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
                	  if(Lizzie.leelaz.isPondering())
                      Lizzie.leelaz.ponder();
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
          public void menuDeselected(MenuEvent e) {            
          }
          @Override
          public void menuCanceled(MenuEvent e) {                  	  
          }
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

//做到这里
    final JMenuItem continueGameBlackItem = new JMenuItem();
    continueGameBlackItem.setText("续弈(我执黑)(回车)");
    // aboutItem.setMnemonic('A');
    continueGameBlackItem.addActionListener(new ItemListeneryzy());
    gameMenu.add(continueGameBlackItem);

    final JMenuItem continueGameWhiteItem = new JMenuItem();
    continueGameWhiteItem.setText("续弈(我执白)(回车)");
    // aboutItem.setMnemonic('A');
    continueGameWhiteItem.addActionListener(new ItemListeneryzy());
    gameMenu.add(continueGameWhiteItem);

   
    gameMenu.addSeparator();
    final JMenuItem breakplay = new JMenuItem();
    breakplay.setText("中断对局");
    breakplay.addActionListener(new ItemListeneryzy());
    gameMenu.add(breakplay);


    final JMenuItem setinfo = new JMenuItem();
    setinfo.setText("设置棋局信息(I)");
    setinfo.addActionListener(new ItemListeneryzy());
    gameMenu.add(setinfo);


    final JMenuItem bestone = new JMenuItem();
    bestone.setText("落最佳一手(逗号)");
    bestone.addActionListener(new ItemListeneryzy());
    gameMenu.add(bestone);

    final JMenuItem pass = new JMenuItem();
    pass.setText("停一手(P)");
    pass.addActionListener(new ItemListeneryzy());
    gameMenu.add(pass);
    gameMenu.addSeparator();

    final JMenuItem empty = new JMenuItem();
    empty.setText("清空棋盘(Ctrl+Home)");
    // aboutItem.setMnemonic('A');
    empty.addActionListener(new ItemListeneryzy());
    gameMenu.add(empty);



    final JMenuItem branchStart = new JMenuItem();
    branchStart.setText("返回主分支(CTRL+左)");
    // aboutItem.setMnemonic('A');
    branchStart.addActionListener(new ItemListeneryzy());
    gameMenu.add(branchStart);
    gameMenu.addSeparator();

    final JMenuItem firstItem = new JMenuItem();
    firstItem.setText("跳转到最前(Home)");
    // aboutItem.setMnemonic('A');
    firstItem.addActionListener(new ItemListeneryzy());
    gameMenu.add(firstItem);

    final JMenuItem lastItem = new JMenuItem();
    lastItem.setText("跳转到最后(End)");
    // aboutItem.setMnemonic('A');
    lastItem.addActionListener(new ItemListeneryzy());
    gameMenu.add(lastItem);

    final JMenuItem commetup = new JMenuItem();
    commetup.setText("跳转到左分支(左)");
    // aboutItem.setMnemonic('A');
    commetup.addActionListener(new ItemListeneryzy());
    gameMenu.add(commetup);

    final JMenuItem commetdown = new JMenuItem();
    commetdown.setText("跳转到右分支(右)");
    // aboutItem.setMnemonic('A');
    commetdown.addActionListener(new ItemListeneryzy());
    gameMenu.add(commetdown);

    final JMenu analyMenu = new JMenu("分析 ", false);
    analyMenu.setText(" 分析  ");
    analyMenu.setForeground(Color.BLACK);
    analyMenu.setFont(headFont);
    this.add(analyMenu);

    final JMenuItem anaItem = new JMenuItem();
    anaItem.setText("分析/停止(空格)");
    // aboutItem.setMnemonic('A');
    anaItem.addActionListener(new ItemListeneryzy());
    analyMenu.add(anaItem);

    final JMenuItem autoanItem = new JMenuItem();
    autoanItem.setText("自动分析(A)");
    // aboutItem.setMnemonic('A');
    autoanItem.addActionListener(new ItemListeneryzy());
    analyMenu.add(autoanItem);

    final JMenuItem countsItem = new JMenuItem();
    countsItem.setText("形势判断(点)");
    // aboutItem.setMnemonic('A');
    countsItem.addActionListener(new ItemListeneryzy());
    analyMenu.add(countsItem);
    analyMenu.addSeparator();

    

    engineMenu = new JMenu("引擎 ", false);
    engineMenu.setText(" 引擎  ");
    engineMenu.setForeground(Color.BLACK);
    engineMenu.setFont(headFont);
    this.add(engineMenu);

    icon = new ImageIcon();
    try {
      icon.setImage(ImageIO.read(AnalysisFrame.class.getResourceAsStream("/assets/playing.png")));
      // icon.setImage(ImageIO.read(AnalysisFrame.class.getResourceAsStream("/assets/run.png")));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    ready = new ImageIcon();
    try {
      ready.setImage(ImageIO.read(AnalysisFrame.class.getResourceAsStream("/assets/ready.png")));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    stop = new ImageIcon();
    try {
      stop.setImage(ImageIO.read(AnalysisFrame.class.getResourceAsStream("/assets/stop.png")));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    updateEngineMenuone();
    ArrayList<EngineData> engineData = getEngineData();
    for (int i = 0; i < engineData.size(); i++) {
      EngineData engineDt = engineData.get(i);
      Lizzie.frame.toolbar.enginePkBlack.addItem("[" + (i + 1) + "]" + engineDt.name);
      Lizzie.frame.toolbar.enginePkWhite.addItem("[" + (i + 1) + "]" + engineDt.name);
    }

    engineMenu.addSeparator();

    closeEngine = new JMenu("关闭引擎 ", false);
    closeEngine.setText("关闭引擎");
    engineMenu.add(closeEngine);

    closeother = new JMenuItem();
    closeother.setText("关闭当前以外引擎");
    // aboutItem.setMnemonic('A');
    closeother.addActionListener(new ItemListeneryzy());
    closeEngine.add(closeother);

    closeall = new JMenuItem();
    closeall.setText("关闭所有引擎");
    // aboutItem.setMnemonic('A');
    closeall.addActionListener(new ItemListeneryzy());
    closeEngine.add(closeall);

    forcecloseall = new JMenuItem();
    forcecloseall.setText("强制关闭所有引擎");
    // aboutItem.setMnemonic('A');
    forcecloseall.addActionListener(new ItemListeneryzy());
    closeEngine.add(forcecloseall);


    config = new JMenuItem();
    config.setText("设置");
    config.addActionListener(new ItemListeneryzy());
    engineMenu.add(config);
   
  }

  public void updateEngineMenuone() {

    for (int i = 0; i < engine.length; i++) {
      engine[i] = new JMenuItem();
      engineMenu.add(engine[i]);
      engine[i].setText("引擎" + (i + 1) + ":");
      engine[i].setVisible(false);
    }
    ArrayList<EngineData> engineData = getEngineData();
    for (int i = 0; i < engineData.size(); i++) {
      EngineData engineDt = engineData.get(i);
      if (i > (engine.length - 2)) {
        engine[i].setText("更多引擎...");
        engine[i].setVisible(true);
        engine[i].addActionListener(
            new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                JDialog chooseMoreEngine;
                chooseMoreEngine = ChooseMoreEngine.createBadmovesDialog();
                chooseMoreEngine.setVisible(true);
              }
            });
        return;
      } else {
        engine[i].setText("引擎" + (i + 1) + ":" + engineDt.name);
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

  public void updateEngineMenu() {

    this.remove(engineMenu);
    engineMenu = new JMenu("引擎 ", false);
    engineMenu.setText(" 引擎  ");
    engineMenu.setForeground(Color.BLACK);
    engineMenu.setFont(headFont);
    this.add(engineMenu);
    for (int i = 0; i < engine.length; i++) {
      try {
        engineMenu.remove(engine[i]);
      } catch (Exception e) {
      }
      engine[i] = new JMenuItem();
      engineMenu.add(engine[i]);
      engine[i].setText("引擎" + (i + 1) + ":");
      engine[i].setVisible(false);
    }
    for (int i = 0; i < Lizzie.engineManager.engineList.size(); i++) {
      if (i <= 20
          && Lizzie.engineManager.engineList.get(i).isLoaded()
          && Lizzie.engineManager.engineList.get(i).process.isAlive()) {
        engine[i].setIcon(ready);
      }
      if (i == Lizzie.engineManager.currentEngineNo && i <= 20) {
        engine[i].setIcon(icon);
        engineMenu.setText(
            "引擎" + (i + 1) + ": " + Lizzie.engineManager.engineList.get(i).currentEnginename);
      }
    }
    ArrayList<EngineData> engineData = getEngineData();
    for (int i = 0; i < engineData.size(); i++) {
      EngineData engineDt = engineData.get(i);
      if (i > (engine.length - 2)) {
        engine[i].setText("更多引擎...");
        engine[i].setVisible(true);
        engine[i].addActionListener(
            new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                JDialog chooseMoreEngine;
                chooseMoreEngine = ChooseMoreEngine.createBadmovesDialog();
                chooseMoreEngine.setVisible(true);
              }
            });
        engineMenu.addSeparator();
        engineMenu.add(closeEngine);
        closeEngine.add(closeall);
        closeEngine.add(forcecloseall);
        closeEngine.add(closeother);
        engineMenu.add(restartZen);
        engineMenu.addSeparator();
        engineMenu.add(config);
        engineMenu.add(moreconfig);
        return;
      } else {
        engine[i].setText("引擎" + (i + 1) + ":" + engineDt.name);
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
    engineMenu.addSeparator();
    engineMenu.add(closeEngine);
    closeEngine.add(closeall);
    closeEngine.add(forcecloseall);
    closeEngine.add(closeother);
    engineMenu.add(restartZen);
    engineMenu.addSeparator();
    engineMenu.add(config);
    engineMenu.add(moreconfig);
  }

  public void changeEngineIcon(int index, int mode) {
    if (index > 20) index = 20;

    if (mode == 0) engine[index].setIcon(null);
    if (mode == 1) engine[index].setIcon(stop);
    if (mode == 2) engine[index].setIcon(ready);
    if (mode == 3) engine[index].setIcon(icon);
  }

  public ArrayList<EngineData> getEngineData() {
    ArrayList<EngineData> engineData = new ArrayList<EngineData>();
    Optional<JSONArray> enginesCommandOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-command-list"));
    Optional<JSONArray> enginesNameOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-name-list"));
    Optional<JSONArray> enginesPreloadOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-preload-list"));

    Optional<JSONArray> enginesWidthOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-width-list"));

    Optional<JSONArray> enginesHeightOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-height-list"));
    Optional<JSONArray> enginesKomiOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-komi-list"));

    int defaultEngine = Lizzie.config.uiConfig.optInt("default-engine", -1);

    for (int i = 0;
        i < (enginesCommandOpt.isPresent() ? enginesCommandOpt.get().length() + 1 : 0);
        i++) {
      if (i == 0) {
        String engineCommand = Lizzie.config.leelazConfig.getString("engine-command");
        int width = enginesWidthOpt.isPresent() ? enginesWidthOpt.get().optInt(i, 19) : 19;
        int height = enginesHeightOpt.isPresent() ? enginesHeightOpt.get().optInt(i, 19) : 19;
        String name = enginesNameOpt.isPresent() ? enginesNameOpt.get().optString(i, "") : "";
        float komi =
            enginesKomiOpt.isPresent()
                ? enginesKomiOpt.get().optFloat(i, (float) 7.5)
                : (float) 7.5;
        boolean preload =
            enginesPreloadOpt.isPresent() ? enginesPreloadOpt.get().optBoolean(i, false) : false;
        EngineData enginedt = new EngineData();
        enginedt.commands = engineCommand;
        enginedt.name = name;
        enginedt.preload = preload;
        enginedt.index = i;
        enginedt.width = width;
        enginedt.height = height;
        enginedt.komi = komi;
        if (defaultEngine == i) enginedt.isDefault = true;
        else enginedt.isDefault = false;
        engineData.add(enginedt);
      } else {
        String commands =
            enginesCommandOpt.isPresent() ? enginesCommandOpt.get().optString(i - 1, "") : "";
        if (!commands.equals("")) {
          int width = enginesWidthOpt.isPresent() ? enginesWidthOpt.get().optInt(i, 19) : 19;
          int height = enginesHeightOpt.isPresent() ? enginesHeightOpt.get().optInt(i, 19) : 19;
          String name = enginesNameOpt.isPresent() ? enginesNameOpt.get().optString(i, "") : "";
          float komi =
              enginesKomiOpt.isPresent()
                  ? enginesKomiOpt.get().optFloat(i, (float) 7.5)
                  : (float) 7.5;
          boolean preload =
              enginesPreloadOpt.isPresent() ? enginesPreloadOpt.get().optBoolean(i, false) : false;
          EngineData enginedt = new EngineData();
          enginedt.commands = commands;
          enginedt.name = name;
          enginedt.preload = preload;
          enginedt.index = i;
          enginedt.width = width;
          enginedt.height = height;
          enginedt.komi = komi;
          if (defaultEngine == i) enginedt.isDefault = true;
          else enginedt.isDefault = false;
          engineData.add(enginedt);
        }
      }
    }
    return engineData;
  }

  public void changeicon() {

    for (int i = 0; i < 21; i++) {
      if (featurecat.lizzie.gui.Menu.engine[i].getIcon() != null
          && featurecat.lizzie.gui.Menu.engine[i].getIcon() != featurecat.lizzie.gui.Menu.stop) {
        featurecat.lizzie.gui.Menu.engine[i].setIcon(featurecat.lizzie.gui.Menu.ready);
      }
    }
    if (Lizzie.engineManager.currentEngineNo <= 20) {
      if (featurecat.lizzie.gui.Menu.engine[Lizzie.engineManager.currentEngineNo].getIcon()
          == null) {
      } else {
        featurecat.lizzie.gui.Menu.engine[Lizzie.engineManager.currentEngineNo].setIcon(
            featurecat.lizzie.gui.Menu.icon);
      }
    }
  }

 
  class ItemListeneryzy implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      JMenuItem menuItem = (JMenuItem) e.getSource();
      // System.out.println("您单击的是菜单项：" + menuItem.getText());
      Lizzie.frame.setVisible(true);
      if (menuItem.getText().startsWith("打开棋谱")) {
        Lizzie.frame.openFile();
        return;
      }

      if (menuItem.getText().startsWith("保存")) {
        Lizzie.frame.saveFile();
        return;
      }
      if (menuItem.getText().startsWith("强制退出")) {
        System.exit(0);
        return;
      }
      if (menuItem.getText().startsWith("坐标")) {
        Lizzie.config.toggleCoordinates();
        return;
      }
      if (menuItem.getText().startsWith("放大小")) {
        Lizzie.config.toggleLargeSubBoard();
        return;
      }
      if (menuItem.getText().startsWith("放大胜")) {
        Lizzie.config.toggleLargeWinrate();
        return;
      }
      if (menuItem.getText().startsWith("小棋")) {
        Lizzie.config.toggleShowSubBoard();
        return;
      }
      if (menuItem.getText().startsWith("评论")) {
        Lizzie.config.toggleShowComment();
        return;
      }
      if (menuItem.getText().startsWith("左上")) {
        Lizzie.config.toggleShowCaptured();
        return;
      }
      if (menuItem.getText().startsWith("左下")) {
        Lizzie.config.toggleShowStatus();
        return;
      }
      if (menuItem.getText().startsWith("分支")) {
        Lizzie.config.toggleShowVariationGraph();
        return;
      }
      if (menuItem.getText().startsWith("胜率面板")) {
        Lizzie.config.toggleShowWinrate();
        return;
      }
      if (menuItem.getText().startsWith("命令")) {
        Lizzie.frame.toggleGtpConsole();
        return;
      }
      if (menuItem.getText().startsWith("左移")) {
        if (Lizzie.frame.boardPositionProportion > 0) Lizzie.frame.boardPositionProportion--;
        return;
      }
      if (menuItem.getText().startsWith("右移")) {
        if (Lizzie.frame.boardPositionProportion < 8) Lizzie.frame.boardPositionProportion++;
        return;
      }
      if (menuItem.getText().startsWith("人机对局")) {
        if (Lizzie.leelaz.isPondering()) Lizzie.leelaz.togglePonder();
        Lizzie.frame.startGame();
        return;
      }
      if (menuItem.getText().startsWith("续弈(我执黑")) {

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
        return;
      }
      if (menuItem.getText().startsWith("续弈(我执白")) {

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
        return;
      }
      if (menuItem.getText().startsWith("形势")) {
        Lizzie.frame.countstones();
        return;
      }
      if (menuItem.getText().startsWith("分析")) {
        if (Lizzie.frame.isPlayingAgainstLeelaz) {
          Lizzie.frame.isPlayingAgainstLeelaz = false;
          Lizzie.leelaz.isThinking = false;
        }
        Lizzie.leelaz.togglePonder();
        return;
      }


      if (menuItem.getText() == ("设置")) {
        Lizzie.frame.openConfigDialog();
        return;
      }
      if (menuItem.getText().startsWith("关闭所有")) {
        try {
          Lizzie.engineManager.killAllEngines();
        } catch (Exception ex) {

        }
        for (int i = 0; i < engine.length; i++) {
          engine[i].setIcon(null);
        }
        return;
      }

      if (menuItem.getText().startsWith("强制关闭所有")) {
        try {
          Lizzie.engineManager.forcekillAllEngines();
        } catch (Exception ex) {
        }
        for (int i = 0; i < engine.length; i++) {
          engine[i].setIcon(null);
        }
        return;
      }

      if (menuItem.getText().startsWith("关闭当前")) {
        try {
          Lizzie.engineManager.killOtherEngines();
        } catch (Exception ex) {

        }

        for (int i = 0; i < engine.length; i++) {
          engine[i].setIcon(null);
        }
        engine[Lizzie.leelaz.currentEngineN()].setIcon(icon);

        return;
      }
      if (menuItem.getText().startsWith("清空棋盘")) {
        Lizzie.board.clear();
        Lizzie.frame.refresh();
        return;
      }

      if (menuItem.getText().startsWith("跳转到最前")) {
        while (Lizzie.board.previousMove()) ;
        return;
      }
      if (menuItem.getText().startsWith("跳转到最后")) {
        while (Lizzie.board.nextMove()) ;
        return;
      }


      if (menuItem.getText().startsWith("落最佳")) {
        if (!Lizzie.frame.playCurrentVariation()) Lizzie.frame.playBestMove();
        return;
      }

      if (menuItem.getText().startsWith("不显示")) {
        Lizzie.config.allowMoveNumber = 0;
        Lizzie.config.uiConfig.put("allow-move-number", 0);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
        }

        return;
      }
      if (menuItem.getText().startsWith("最近1手")) {
        Lizzie.config.allowMoveNumber = 1;
        Lizzie.config.uiConfig.put("allow-move-number", 1);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
        }
        return;
      }
      if (menuItem.getText().startsWith("最近5手")) {
        Lizzie.config.allowMoveNumber = 5;
        Lizzie.config.uiConfig.put("allow-move-number", 5);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
        }
        return;
      }
      if (menuItem.getText().startsWith("最近10手")) {
        Lizzie.config.allowMoveNumber = 10;
        Lizzie.config.uiConfig.put("allow-move-number", 10);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
        }
        return;
      }
      if (menuItem.getText().startsWith("自定义")) {
        MovenumberDialog mvdialog = new MovenumberDialog();
        mvdialog.setVisible(true);
        return;
      }
      if (menuItem.getText() == ("全部")) {
        Lizzie.config.allowMoveNumber = -1;
        return;
      }
      if (menuItem.getText().startsWith("自动分")) {
        //Input.shouldDisableAnalysis = false;
        Lizzie.board.toggleAnalysis();
        return;
      }

      if (menuItem.getText().startsWith("返回主分支")) {
        if (Lizzie.board.undoToChildOfPreviousWithVariation()) {
          Lizzie.board.previousMove();
        }
        return;
      }

      if (menuItem.getText().startsWith("跳转到左分")) {
    	  if (Lizzie.frame.isPlayingAgainstLeelaz) {
    	      Lizzie.frame.isPlayingAgainstLeelaz = false;
    	    }
    	    Lizzie.board.previousBranch();
        return;
      }

      if (menuItem.getText().startsWith("跳转到右分")) {
    	   if (Lizzie.frame.isPlayingAgainstLeelaz) {
    		      Lizzie.frame.isPlayingAgainstLeelaz = false;
    		    }
    		    Lizzie.board.nextBranch();
        return;
      }
      if (menuItem.getText().startsWith("复制到")) {
        Lizzie.frame.copySgf();
        return;
      }
      if (menuItem.getText().startsWith("从剪贴")) {
        Lizzie.frame.pasteSgf();
        return;
      }
      if (menuItem.getText().startsWith("精简")) {

        if (Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
        if (Lizzie.config.showComment) Lizzie.config.toggleShowComment();
        // if (!Lizzie.config.showComment) Lizzie.config.toggleShowComment();
        // if (!Lizzie.config.showLargeSubBoard()) Lizzie.config.showLargeSubBoard();
        if (Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
        if (Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
        if (Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
        if (Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
      
        Lizzie.frame.repaint();
        return;
      }
      if (menuItem.getText().startsWith("经典")) {

        if (!Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
        if (!Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
        if (Lizzie.config.showLargeWinrateOnly()) Lizzie.config.toggleLargeWinrate();
        if (!Lizzie.config.showLargeSubBoard()) Lizzie.config.toggleLargeSubBoard();
        if (!Lizzie.config.showComment) Lizzie.config.toggleShowComment();
        if (!Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
        if (Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
        if (!Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
        if (Lizzie.frame.getWidth() - Lizzie.frame.getHeight() < 485)
          Lizzie.frame.setBounds(
              Lizzie.frame.getX(),
              Lizzie.frame.getY(),
              Lizzie.frame.getHeight() + 485,
              Lizzie.frame.getHeight());
        // Lizzie.frame.redrawBackgroundAnyway=true;
        Lizzie.frame.repaint();
        return;
      }
      if (menuItem.getText().startsWith("默认")) {

        if (!Lizzie.config.showSubBoard) Lizzie.config.toggleShowSubBoard();
        if (!Lizzie.config.showWinrate) Lizzie.config.toggleShowWinrate();
        if (Lizzie.config.showLargeSubBoard()) Lizzie.config.toggleLargeSubBoard();
        if (Lizzie.config.showLargeWinrate()) Lizzie.config.toggleLargeWinrate();
        if (!Lizzie.config.showComment) Lizzie.config.toggleShowComment();
        if (!Lizzie.config.showCaptured) Lizzie.config.toggleShowCaptured();
        if (!Lizzie.config.showStatus) Lizzie.config.toggleShowStatus();
        if (!Lizzie.config.showVariationGraph) Lizzie.config.toggleShowVariationGraph();
        if (Lizzie.frame.getWidth() - Lizzie.frame.getHeight() < 600)
          Lizzie.frame.setBounds(
              Lizzie.frame.getX(),
              Lizzie.frame.getY(),
              Lizzie.frame.getHeight() + 600,
              Lizzie.frame.getHeight());
        Lizzie.frame.repaint();
        return;
      }

      if (menuItem.getText().startsWith("中断人机")) {
        if (Lizzie.frame.isPlayingAgainstLeelaz) {
          Lizzie.frame.isPlayingAgainstLeelaz = false;
          Lizzie.leelaz.isThinking = false;
          Lizzie.leelaz.ponder();
        }
        return;
      }
     

      if (menuItem.getText().startsWith("打开在线")) {
        Lizzie.frame.openOnlineDialog();
        return;
      }
     
   
    
      if (menuItem.getText().startsWith("停一")) {
        Lizzie.board.pass();
        return;
      }
      if (menuItem.getText().startsWith("设置棋局信")) {
        Lizzie.frame.editGameInfo();
        return;
      }
      
      if (menuItem.getText().startsWith("总是显示黑")) {
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
        return;
      }
  
      if (menuItem.getText().startsWith("胜率+计算量+")) {
        Lizzie.config.showKataGoScoreMean = true;
        Lizzie.config.kataGoNotShowWinrate = false;
        Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
        Lizzie.config.uiConfig.put("katago-notshow-winrate", Lizzie.config.kataGoNotShowWinrate);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("胜率+计")) {
        Lizzie.config.showKataGoScoreMean = false;
        Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("目差+计")) {
        Lizzie.config.showKataGoScoreMean = true;
        Lizzie.config.kataGoNotShowWinrate = true;
        Lizzie.config.uiConfig.put("show-katago-scoremean", Lizzie.config.showKataGoScoreMean);
        Lizzie.config.uiConfig.put("katago-notshow-winrate", Lizzie.config.kataGoNotShowWinrate);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("目差")) {
        Lizzie.config.showKataGoBoardScoreMean = false;
        Lizzie.config.uiConfig.put(
            "show-katago-boardscoremean", Lizzie.config.showKataGoBoardScoreMean);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("盘面差")) {
        Lizzie.config.showKataGoBoardScoreMean = true;
        Lizzie.config.uiConfig.put(
            "show-katago-boardscoremean", Lizzie.config.showKataGoBoardScoreMean);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("永远为黑")) {
        Lizzie.config.kataGoScoreMeanAlwaysBlack = true;
        Lizzie.config.uiConfig.put(
            "katago-scoremean-alwaysblack", Lizzie.config.kataGoScoreMeanAlwaysBlack);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("黑白交替")) {
        Lizzie.config.kataGoScoreMeanAlwaysBlack = false;
        Lizzie.config.uiConfig.put(
            "katago-scoremean-alwaysblack", Lizzie.config.kataGoScoreMeanAlwaysBlack);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("关闭评估")) {
        Lizzie.config.showKataGoEstimate = false;
        Lizzie.frame.boardRenderer.removecountblock();
        if (Lizzie.config.showSubBoard) Lizzie.frame.subBoardRenderer.removecountblock();
        Lizzie.leelaz.ponder();
        Lizzie.config.uiConfig.put("show-katago-estimate", Lizzie.config.showKataGoEstimate);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("显示在大棋")) {
        Lizzie.config.showKataGoEstimate = true;
        Lizzie.config.showKataGoEstimateOnMainbord = true;
        Lizzie.config.showKataGoEstimateOnSubbord = false;
        if (Lizzie.config.showSubBoard) Lizzie.frame.subBoardRenderer.removecountblock();
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
        return;
      }
      if (menuItem.getText().startsWith("显示在小")) {
        Lizzie.config.showKataGoEstimate = true;
        Lizzie.config.showKataGoEstimateOnMainbord = false;
        Lizzie.config.showKataGoEstimateOnSubbord = true;
        Lizzie.frame.boardRenderer.removecountblock();
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
        return;
      }
      if (menuItem.getText().startsWith("显示在大小")) {
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
        return;
      }
      if (menuItem.getText().startsWith("以方块大")) {
        Lizzie.config.showKataGoEstimateBySize = true;
        Lizzie.config.uiConfig.put(
            "show-katago-estimate-bysize", Lizzie.config.showKataGoEstimateBySize);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
      if (menuItem.getText().startsWith("以方块透")) {
        Lizzie.config.showKataGoEstimateBySize = false;
        Lizzie.config.uiConfig.put(
            "show-katago-estimate-bysize", Lizzie.config.showKataGoEstimateBySize);
        try {
          Lizzie.config.save();
        } catch (IOException es) {
          // TODO Auto-generated catch block
        }
        return;
      }
    }
  }
}
