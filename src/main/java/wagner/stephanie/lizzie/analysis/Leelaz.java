package wagner.stephanie.lizzie.analysis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import wagner.stephanie.lizzie.Lizzie;
import wagner.stephanie.lizzie.rules.Stone;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collections;

/**
 * an interface with leelaz.exe go engine. Can be adapted for GTP, but is specifically designed for GCP's Leela Zero.
 * leelaz is modified to output information as it ponders
 * see www.github.com/gcp/leela-zero
 */
public class Leelaz {
    private static final long MINUTE = 60 * 1000; // number of milliseconds in a minute
    //    private static final long SECOND = 1000;
    private long maxAnalyzeTimeMillis;//, maxThinkingTimeMillis;
    private int cmdNumber;
    private int currentCmdNum;

    private Process process;

    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;

    private boolean printCommunication;

    private List<MoveData> bestMoves;
    private List<MoveData> bestMovesTemp;

    private List<LeelazListener> listeners;

    private boolean isPondering;
    private long startPonderTime;

    // fixed_handicap
    public boolean isSettingHandicap = false;

    // genmove
    public boolean isThinking = false;

    private boolean isCheckingVersion;

    /**
     * Initializes the leelaz process and starts reading output
     *
     * @throws IOException
     */
    public Leelaz() throws IOException, JSONException {
        bestMoves = new ArrayList<>();
        bestMovesTemp = new ArrayList<>();
        listeners = new CopyOnWriteArrayList<>();

        isPondering = false;
        startPonderTime = System.currentTimeMillis();
        cmdNumber = 1;
        currentCmdNum = -1;

        JSONObject config = Lizzie.config.config.getJSONObject("leelaz");

        printCommunication = config.getBoolean("print-comms");
        maxAnalyzeTimeMillis = MINUTE * config.getInt("max-analyze-time-minutes");
//        maxThinkingTimeMillis = SECOND * config.getInt("max-game-thinking-time-seconds");

        // list of commands for the leelaz process
        List<String> commands = new ArrayList<>();
        commands.add("./leelaz"); // windows, linux, mac all understand this
        commands.add("-g");
        commands.add("-t");
        commands.add(""+config.getInt("threads"));
        String weightsString = config.getString("weights");
        if (!weightsString.isEmpty()) {
            // Leela 0.11.0 lacks this option
            commands.add("-w");
            commands.add(weightsString);
        }
        commands.add("-b");
        commands.add("0");

//        if (config.getBoolean("noise")) {
//            commands.add("-n");
//        }

        try {
            JSONArray gpu = config.getJSONArray("gpu");

            for (int i = 0; i < gpu.length(); i++) {
                commands.add("--gpu");
                commands.add(String.valueOf(gpu.getInt(i)));
            }
        } catch (Exception e) {
            // Nothing
        }

        // run leelaz
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        initializeStreams();

        // Send a version request to check that we have a supported version
        // Response handled in  parseLine
        isCheckingVersion = true;
        sendCommand("version");

        // start a thread to continuously read Leelaz output
        new Thread(this::read).start();
    }

    /**
     * Initializes the input and output streams
     */
    private void initializeStreams() {
        inputStream = new BufferedInputStream(process.getInputStream());
        outputStream = new BufferedOutputStream(process.getOutputStream());
    }

    private void parseInfo(String line) {

        bestMoves = new ArrayList<>();
        String[] variations = line.split(" info ");
        for (String var : variations) {
            bestMoves.add(new MoveData(var));
        }
        // Not actually necessary to sort with current version of LZ (0.15)
        // but not guaranteed to be ordered in the future
        Collections.sort(bestMoves);
    }

    /**
     * Parse a line of Leelaz output
     *
     * @param line output line
     */
    private void parseLine(String line) {
        synchronized (this) {
            if (line.equals("\n")) {
                // End of response
            } else if (line.startsWith("info")) {
                if (currentCmdNum == cmdNumber -1) {
                    // This should not be stale data when the command number match
                    parseInfo(line.substring(5));
                    notifyBestMoveListeners();
                    if (Lizzie.frame != null) Lizzie.frame.repaint();
                    if (System.currentTimeMillis() - startPonderTime > maxAnalyzeTimeMillis) {
                        sendCommand("name"); // Send any command to stop ponder
                    }
                }
            } else if (line.startsWith("play")) {
                // In lz-genmove_analyze
                if (Lizzie.frame.isPlayingAgainstLeelaz) {
                    Lizzie.board.place(line.substring(5).trim());
                }
                isThinking = false;

            } else if (Lizzie.frame != null && line.startsWith("=")) {
                if (printCommunication) {
                    System.out.print(line);
                }
                String[] params = line.trim().split(" ");
                currentCmdNum = Integer.parseInt(params[0].substring(1).trim());

                if (params.length == 1) return;


                if (isSettingHandicap) {
                    for (int i = 2; i < params.length; i++) {
                        int[] coordinates = Lizzie.board.convertNameToCoordinates(params[i]);
                        Lizzie.board.getHistory().setStone(coordinates, Stone.BLACK);
                    }
                    isSettingHandicap = false;
                } else if (isThinking && !isPondering) {
                    if (Lizzie.frame.isPlayingAgainstLeelaz) {
                        Lizzie.board.place(params[1]);
                    }
                } else if (isCheckingVersion) {
                    String[] ver = params[1].split("\\.");
                    int minor = Integer.parseInt(ver[1]);
                    // Gtp support added in version 15
                    if (minor < 15) {
                        JOptionPane.showMessageDialog(Lizzie.frame, "Lizzie requires version 0.15 or later of Leela Zero for analysis (found " + params[1] + ")");
                    }
                    isCheckingVersion = false;
                }
            }
        }
    }

    /**
     * Parse a move-data line of Leelaz output
     *
     * @param line output line
     */
    private void parseMoveDataLine(String line) {
        line = line.trim();
        // ignore passes, and only accept lines that start with a coordinate letter
        if (line.length() > 0 && Character.isLetter(line.charAt(0)) && !line.startsWith("pass")) {
            if (!(Lizzie.frame != null && Lizzie.frame.isPlayingAgainstLeelaz && Lizzie.frame.playerIsBlack != Lizzie.board.getData().blackToPlay)) {
                try {
                    bestMovesTemp.add(new MoveData(line));
                } catch (ArrayIndexOutOfBoundsException e) {
                    // this is very rare but is possible. ignore
                }
            }
        }
    }

    /**
     * Continually reads and processes output from leelaz
     */
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
            // this line will be reached when Leelaz shuts down
            System.out.println("Leelaz process ended.");

            shutdown();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Sends a command for leelaz to execute
     *
     * @param command a GTP command containing no newline characters
     */
    public void sendCommand(String command) {
        command = cmdNumber + " " + command;
        cmdNumber++;
        if (printCommunication) {
            System.out.printf("> %s\n", command);
        }
        if (command.startsWith("fixed_handicap"))
            isSettingHandicap = true;
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param color color of stone to play
     * @param move  coordinate of the coordinate
     */
    public void playMove(Stone color, String move) {
        synchronized (this) {
            String colorString;
            switch (color) {
                case BLACK:
                    colorString = "B";
                    break;
                case WHITE:
                    colorString = "W";
                    break;
                default:
                    throw new IllegalArgumentException("The stone color must be B or W, but was " + color.toString());
            }

            sendCommand("play " + colorString + " " + move);
            bestMoves = new ArrayList<>();

            if (isPondering && !Lizzie.frame.isPlayingAgainstLeelaz)
                ponder();
        }
    }

    public void genmove(String color) {
        String command = "genmove " + color;
        /*
         * We don't support displaying this while playing, so no reason to request it (for now)
        if (isPondering) {
            command = "lz-genmove_analyze " + color + " 10";
        }*/
        sendCommand(command);
        isThinking = true;
    }

    public void undo() {
        synchronized (this) {
            sendCommand("undo");
            bestMoves = new ArrayList<>();
            if (isPondering)
                ponder();
        }
    }

    /**
     * this initializes leelaz's pondering mode at its current position
     */
    private void ponder() {
        isPondering = true;
        startPonderTime = System.currentTimeMillis();
        sendCommand("lz-analyze " + Lizzie.config.config.getJSONObject("leelaz").getInt("analyze-update-interval-centisec"));    // until it responds to this, incoming ponder results are obsolete
    }

    public void togglePonder() {
        isPondering = !isPondering;
        if (isPondering) {
            ponder();
        } else {
            sendCommand("name"); // ends pondering
        }
    }

    /**
     * End the process
     */
    public void shutdown() {
        process.destroy();
    }

    public List<MoveData> getBestMoves() {
        synchronized (this) {
            return bestMoves;
        }
    }

    public boolean isPondering() {
        return isPondering;
    }

    public class WinrateStats {
        public double maxWinrate;
        public int totalPlayouts;

        public WinrateStats(double maxWinrate, int totalPlayouts) {
            this.maxWinrate = maxWinrate;
            this.totalPlayouts = totalPlayouts;
        }
    }

    /*
     * Return the best win rate and total number of playouts.
     * If no analysis available, win rate is negative and playouts is 0.
     */
    public WinrateStats getWinrateStats() {
        WinrateStats stats = new WinrateStats(-100, 0);

        if (bestMoves != null && !bestMoves.isEmpty()) {
            // we should match the Leelaz UCTNode get_eval, which is a weighted average
            final List<MoveData> moves = bestMoves;

            // get the total number of playouts in moves
            stats.totalPlayouts = moves.stream().reduce(0,
                                                        (Integer result, MoveData move) -> result + move.playouts,
                                                        (Integer a, Integer b) -> a + b);

            // set maxWinrate to the weighted average winrate of moves
            stats.maxWinrate = moves.stream().reduce(0d,
                                                     (Double result, MoveData move) ->
                                                         result + move.winrate * move.playouts / stats.totalPlayouts,
                                                     (Double a, Double b) -> a + b);
        }

        return stats;
    }
    
    /*
     * initializes the normalizing factor for winrate_to_handicap_stones conversion.
     */
    public void estimatePassWinrate() {
    	// we use A1 instead of pass, because valuenetwork is more accurate for A1 on empty board than a pass.
    	// probably the reason for higher accuracy is that networks have randomness which produces occasionally A1 as first move, but never pass.
    	// for all practical purposes, A1 should equal pass for the value it provides, hence good replacement.
    	// this way we avoid having to run lots of playouts for accurate winrate for pass.
    	playMove(Stone.BLACK, "A1");
    	togglePonder();
    	WinrateStats stats = getWinrateStats();

    	// we could use a timelimit or higher minimum playouts to get a more accurate measurement.
    	while( stats.totalPlayouts < 1 ) {
    		try {
    			Thread.sleep(100);
    		} catch (InterruptedException e) {
    			throw new Error(e);
    		}
    		stats=getWinrateStats();
    	}
    	mHandicapWinrate=stats.maxWinrate; 
    	togglePonder();
    	undo();
    	Lizzie.board.clear();
    }

    public static double mHandicapWinrate=25;

    /**
     * Convert winrate to handicap stones, by normalizing winrate by first move pass winrate (one stone handicap).
     */
    public static double winrateToHandicap(double pWinrate) {
    	// we assume each additional handicap lowers winrate by fixed percentage.
    	// this is pretty accurate for human handicap games at least.
    	// also this kind of property is a requirement for handicaps to determined based on rank difference.

    	// lets convert the 0%-50% range and 100%-50% from both the move and and pass into range of 0-1
    	double moveWinrateSymmetric = 1-Math.abs(1-(pWinrate/100)*2);
    	double passWinrateSymmetric = 1-Math.abs(1-(mHandicapWinrate/100)*2);

    	// convert the symmetric move winrate into correctly scaled log scale, so that winrate of passWinrate equals 1 handicap.
    	double handicapSymmetric = Math.log(moveWinrateSymmetric)/Math.log(passWinrateSymmetric);

    	// make it negative if we had low winrate below 50.
    	return Math.signum(pWinrate-50)*handicapSymmetric;
    }

    public synchronized void addListener(LeelazListener listener) {
        listeners.add(listener);
    }

    // Beware, due to race conditions, bestMoveNotification can be called once even after item is removed
    // with removeListener
    public synchronized void removeListener(LeelazListener listener) {
        listeners.remove(listener);
    }

    private synchronized void notifyBestMoveListeners() {
        for (LeelazListener listener: listeners) {
            listener.bestMoveNotification(bestMoves);
        }
    }
}
