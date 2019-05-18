package featurecat.lizzie.analysis;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.gui.CountResults;
import featurecat.lizzie.gui.LizzieFrame;
import featurecat.lizzie.rules.MoveList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JOptionPane;

public class YaZenGtp {
  private static final ResourceBundle resourceBundle =
      ResourceBundle.getBundle("l10n.DisplayStrings");
  public Process process;
  private BufferedInputStream inputStream;
  private BufferedOutputStream outputStream;

  public boolean gtpConsole;

  private String engineCommand;
  // private List<String> commands;
  private int cmdNumber;
  private int currentCmdNum;
  private ArrayDeque<String> cmdQueue;
  private ScheduledExecutorService executor;
  ArrayList<Integer> detailedCount = new ArrayList<Integer>();
  public int blackEatCount = 0;
  public int whiteEatCount = 0;
  public int blackPrisonerCount = 0;
  public int whitePrisonerCount = 0;
  CountResults results;
  boolean firstcount = true;
  public int numberofcount = 0;
  public boolean noread = false;

  public YaZenGtp() throws IOException {

    cmdNumber = 1;
    currentCmdNum = 0;
    cmdQueue = new ArrayDeque<>();
    gtpConsole = true;
    engineCommand = "YAZenGtp.exe";
    startEngine(engineCommand, 0);
  }

  public void startEngine(String engineCommand, int index) {
    ProcessBuilder processBuilder = new ProcessBuilder(engineCommand);
    processBuilder.redirectErrorStream(true);
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      JOptionPane.showMessageDialog(null, resourceBundle.getString("YaZenGtp.nofile"));
      return;
    }
    initializeStreams();

    executor = Executors.newSingleThreadScheduledExecutor();
    executor.execute(this::read);
  }

  private void initializeStreams() {
    inputStream = new BufferedInputStream(process.getInputStream());
    outputStream = new BufferedOutputStream(process.getOutputStream());
  }

  private void read() {
    try {
      int c;
      StringBuilder line = new StringBuilder();
      while ((c = inputStream.read()) != -1) {
        line.append((char) c);

        if ((c == '\n')) {

          parseLine(line.toString());
          line = new StringBuilder();
        }
      }
      System.out.println("YaZenGtp process ended.");

      shutdown();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private void parseLine(String line) {
    synchronized (this) {
      Lizzie.gtpConsole.addLineForce(line);
      if (line.startsWith("=  ")) {
        String[] params = line.trim().split(" ");
        for (int i = 2; i < params.length; i++) detailedCount.add(Integer.parseInt(params[i]));
      }

      if (line.startsWith(" ")) {

        String[] params = line.trim().split(" ");
        if (params.length == 19) {
          for (int i = 0; i < params.length; i++) detailedCount.add(Integer.parseInt(params[i]));
        }
      }
    }
    if (line.startsWith("= ")) {
      String[] params = line.trim().split(" ");
      if (params.length == 14) {
        if (noread) {
          numberofcount = 0;
        } else {

          blackEatCount = Integer.parseInt(params[3]);
          whiteEatCount = Integer.parseInt(params[4]);
          blackPrisonerCount = Integer.parseInt(params[5]);
          whitePrisonerCount = Integer.parseInt(params[6]);
          int blackpoint = Integer.parseInt(params[7]);
          int whitepoint = Integer.parseInt(params[8]);
          if (!Lizzie.frame.isAutocounting)
            Lizzie.frame.boardRenderer.drawCountBlock(detailedCount);
          else {
            if (Lizzie.config.showSubBoard) {
              Lizzie.frame.boardRenderer.removeCountBlock();
              Lizzie.frame.subBoardRenderer.drawCountBlock(detailedCount);
            } else Lizzie.frame.boardRenderer.drawCountBlock(detailedCount);
          }

          Lizzie.frame.repaint();
          if (firstcount) {
            results = LizzieFrame.countResults;
            results.Counts(
                blackEatCount,
                whiteEatCount,
                blackPrisonerCount,
                whitePrisonerCount,
                blackpoint,
                whitepoint);
            results.setVisible(true);
            firstcount = false;
            numberofcount = 0;
          } else {
            results.Counts(
                blackEatCount,
                whiteEatCount,
                blackPrisonerCount,
                whitePrisonerCount,
                blackpoint,
                whitepoint);
            results.setVisible(true);
            Lizzie.frame.setVisible(true);
            numberofcount = 0;
          }
        }
      }
    }
  }

  public void shutdown() {
    process.destroy();
  }

  public void sendCommand(String command) {
    synchronized (cmdQueue) {
      if (!cmdQueue.isEmpty()) {
        cmdQueue.removeLast();
      }
      cmdQueue.addLast(command);
      trySendCommandFromQueue();
    }
  }

  private void trySendCommandFromQueue() {
    synchronized (cmdQueue) {
      if (cmdQueue.isEmpty()) {
        return;
      }
      String command = cmdQueue.removeFirst();
      sendCommandToZen(command);
    }
  }

  private void sendCommandToZen(String command) {
    // System.out.printf("> %d %s\n", cmdNumber, command);
    try {
      Lizzie.gtpConsole.addZenCommand(command, cmdNumber);
    } catch (Exception ex) {
    }
    cmdNumber++;
    try {
      outputStream.write((command + "\n").getBytes());
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void playmove(int x, int y, boolean isblack) {
    String coordsname = Lizzie.board.convertCoordinatesToName(x, y);
    String color = isblack ? "b" : "w";

    sendCommand("play" + " " + color + " " + coordsname);
  }

  public void syncboradstat() {
    sendCommand("clear_board");
    cmdNumber = 1;
    ArrayList<MoveList> movelist = Lizzie.board.getmovelist();
    int lenth = movelist.size();
    for (int i = 0; i < lenth; i++) {
      MoveList move = movelist.get(lenth - 1 - i);
      if (!move.isPass) {
        playmove(move.x, move.y, move.isBlack);
      }
    }
  }

  public void countStones() {
    if (numberofcount > 5) {
      noread = true;
      cmdQueue.clear();
      Lizzie.frame.noAutoCounting();
      return;
    }
    numberofcount++;
    detailedCount.clear();
    blackEatCount = 0;
    whiteEatCount = 0;
    blackPrisonerCount = 0;
    whitePrisonerCount = 0;
    sendCommand("territory_statistics territory");
    //
    sendCommand("score_statistics");
  }
}
