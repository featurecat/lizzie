package featurecat.lizzie.analysis;

import featurecat.lizzie.Config;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import featurecat.lizzie.rules.Stone;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * an interface with leelaz.exe go engine. Can be adapted for GTP, but is specifically designed for GCP's Leela Zero.
 * leelaz is modified to output information as it ponders
 * see www.github.com/gcp/leela-zero
 */
public class Leelaz {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("l10n.DisplayStrings");

    private static final long MINUTE = 60 * 1000; // number of milliseconds in a minute
    private static final String baseURL = "http://zero.sjeng.org";

    private long maxAnalyzeTimeMillis;//, maxThinkingTimeMillis;
    private int cmdNumber;
    private int currentCmdNum;
    private ArrayDeque<String> cmdQueue;

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

    private boolean isLoaded = false;
    private boolean isCheckingVersion;

    // for Multiple Engine
    private String engineCommand = null;
    private List<String> commands = null;
    private JSONObject config = null;
    private String currentWeight = null;
    private boolean switching = false;
    private int currentEngineN = -1;
    private ScheduledExecutorService executor = null;

    // dynamic komi and opponent komi as reported by dynamic-komi version of leelaz
    private float dynamicKomi = Float.NaN, dynamicOppKomi = Float.NaN;
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
        currentCmdNum = 0;
        cmdQueue = new ArrayDeque<>();

        // Move config to member for other method call
        config = Lizzie.config.config.getJSONObject("leelaz");

        printCommunication = config.getBoolean("print-comms");
        maxAnalyzeTimeMillis = MINUTE * config.getInt("max-analyze-time-minutes");

        if (config.getBoolean("automatically-download-latest-network")) {
            updateToLatestNetwork();
        }


        // command string for starting the engine
        engineCommand = config.getString("engine-command");
        // substitute in the weights file
        engineCommand = engineCommand.replaceAll("%network-file", config.getString("network-file"));

        // Initialize current engine number and start engine
        currentEngineN = 0;
        startEngine(engineCommand);
        Lizzie.frame.refreshBackground();
    }

    public void startEngine(String engineCommand) throws IOException {
        // Check engine command
        if (engineCommand == null || engineCommand.trim().isEmpty()) {
            return;
        }

        String startfolder = new File(Config.getBestDefaultLeelazPath()).getParent(); // todo make this a little more obvious/less bug-prone

        // Check if network file is present
        File wf = new File(startfolder + '/' + config.getString("network-file"));
        if (!wf.exists()) {
            JOptionPane.showMessageDialog(null, resourceBundle.getString("LizzieFrame.display.network-missing"));
        }

        // create this as a list which gets passed into the processbuilder
        commands = Arrays.asList(engineCommand.split(" "));

        // get weight name
        if (engineCommand != null) {
            Pattern wPattern = Pattern.compile("(?s).*?(--weights |-w )([^ ]+)(?s).*");
            Matcher wMatcher = wPattern.matcher(engineCommand);
            if (wMatcher.matches()) {
                currentWeight = wMatcher.group(2);
                if (currentWeight != null) {
                    String[] names = currentWeight.split("[\\\\|/]");
                    if (names != null && names.length > 1) {
                        currentWeight = names[names.length - 1];
                    }
                }
            }
        }

        // run leelaz
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File(startfolder));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        initializeStreams();

        // Send a version request to check that we have a supported version
        // Response handled in parseLine
        isCheckingVersion = true;
        sendCommand("version");

        // start a thread to continuously read Leelaz output
        //new Thread(this::read).start();
        //can stop engine for switching weights
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.execute(this::read);
    }

    public void restartEngine(String engineCommand, int index) throws IOException {
        if (engineCommand == null || engineCommand.trim().isEmpty()) {
            return;
        }
        switching = true;
        this.engineCommand = engineCommand;
        // stop the ponder
        if (Lizzie.leelaz.isPondering()) {
            Lizzie.leelaz.togglePonder();
        }
        normalQuit();
        startEngine(engineCommand);
        currentEngineN = index;
        togglePonder();
    }

    public void normalQuit() {
        sendCommand("quit");
        executor.shutdown();
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (executor.awaitTermination(1, TimeUnit.SECONDS)) {
                shutdown();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void updateToLatestNetwork() {
        try {
            if (needToDownloadLatestNetwork()) {
                int dialogResult = JOptionPane.showConfirmDialog(null, resourceBundle.getString("LizzieFrame.display.download-latest-network-prompt"));
                if (dialogResult == JOptionPane.YES_OPTION) {
                    Util.saveAsFile(new URL(baseURL + "/networks/" + getBestNetworkHash() + ".gz"),
                            new File(Lizzie.config.leelazConfig.getString("network-file")));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // now we're probably still ok. Maybe we're offline -- then it's not a big problem.
        }
    }

    private String getBestNetworkHash() throws IOException {
        return Util.downloadAsString(new URL(baseURL + "/best-network-hash")).split("\n")[0];
    }

    private boolean needToDownloadLatestNetwork() throws IOException {
        File networkFile = new File(Lizzie.config.leelazConfig.getString("network-file"));
        if (!networkFile.exists()) {
            return true;
        } else {
            String currentNetworkHash = Util.getSha256Sum(networkFile);
            if (currentNetworkHash == null)
                return true;

            String bestNetworkHash = getBestNetworkHash();

            return !currentNetworkHash.equals(bestNetworkHash);
        }
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
            if (line.startsWith("komi="))
            {
                try {
                    dynamicKomi = Float.parseFloat(line.substring("komi=".length()).trim());
                }
                catch (NumberFormatException nfe) {
                    dynamicKomi = Float.NaN;
                }
            }
            else if (line.startsWith("opp_komi="))
            {
                try {
                    dynamicOppKomi = Float.parseFloat(line.substring("opp_komi=".length()).trim());
                }
                catch (NumberFormatException nfe) {
                    dynamicOppKomi = Float.NaN;
                }
            }
            else if (line.equals("\n")) {
                // End of response
            } else if (line.startsWith("info")) {
                isLoaded = true;
                // Clear switching prompt
                switching = false;
                // Display engine command in the title
                if (Lizzie.frame != null) Lizzie.frame.updateTitle();
                if (isResponseUpToDate()) {
                    // This should not be stale data when the command number match
                    parseInfo(line.substring(5));
                    notifyBestMoveListeners();
                    if (Lizzie.frame != null) Lizzie.frame.repaint();
                    // don't follow the maxAnalyzeTime rule if we are in analysis mode
                    if (System.currentTimeMillis() - startPonderTime > maxAnalyzeTimeMillis && !Lizzie.board.inAnalysisMode()) {
                        togglePonder();
                    }
                }
            } else if (line.startsWith("play")) {
                // In lz-genmove_analyze
                if (Lizzie.frame.isPlayingAgainstLeelaz) {
                    Lizzie.board.place(line.substring(5).trim());
                }
                isThinking = false;

            } else if (Lizzie.frame != null && (line.startsWith("=") || line.startsWith("?"))) {
                if (printCommunication) {
                    System.out.print(line);
                }
                String[] params = line.trim().split(" ");
                currentCmdNum = Integer.parseInt(params[0].substring(1).trim());

                trySendCommandFromQueue();

                if (line.startsWith("?") || params.length == 1) return;


                if (isSettingHandicap) {
                    for (int i = 1; i < params.length; i++) {
                        int[] coordinates = Lizzie.board.convertNameToCoordinates(params[i]);
                        Lizzie.board.getHistory().setStone(coordinates, Stone.BLACK);
                    }
                    isSettingHandicap = false;
                } else if (isThinking && !isPondering) {
                    if (Lizzie.frame.isPlayingAgainstLeelaz) {
                        Lizzie.board.place(params[1]);
                        isThinking = false;
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
            // Do no exit for switching weights
            //System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Sends a command to command queue for leelaz to execute
     *
     * @param command a GTP command containing no newline characters
     */
    public void sendCommand(String command) {
        synchronized(cmdQueue) {
            String lastCommand = cmdQueue.peekLast();
            // For efficiency, delete unnecessary "lz-analyze" that will be stopped immediately
            if (lastCommand != null && lastCommand.startsWith("lz-analyze")) {
                cmdQueue.removeLast();
            }
            cmdQueue.addLast(command);
            trySendCommandFromQueue();
        }
    }

    /**
     * Sends a command from command queue for leelaz to execute if it is ready
     */
    private void trySendCommandFromQueue() {
        // Defer sending "lz-analyze" if leelaz is not ready yet.
        // Though all commands should be deferred theoretically,
        // only "lz-analyze" is differed here for fear of
        // possible hang-up by missing response for some reason.
        // cmdQueue can be replaced with a mere String variable in this case,
        // but it is kept for future change of our mind.
        synchronized(cmdQueue) {
            String command = cmdQueue.peekFirst();
            if (command == null || (command.startsWith("lz-analyze") && !isResponseUpToDate())) {
                return;
            }
            cmdQueue.removeFirst();
            sendCommandToLeelaz(command);
        }
    }

    /**
     * Sends a command for leelaz to execute
     *
     * @param command a GTP command containing no newline characters
     */
    private void sendCommandToLeelaz(String command) {
        if (command.startsWith("fixed_handicap"))
            isSettingHandicap = true;
        command = cmdNumber + " " + command;
        cmdNumber++;
        if (printCommunication) {
            System.out.printf("> %s\n", command);
        }
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check whether leelaz is responding to the last command
     */
    private boolean isResponseUpToDate() {
        // Use >= instead of == for avoiding hang-up, though it cannot happen
        return currentCmdNum >= cmdNumber - 1;
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
        isPondering = false;
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

    public String getDynamicKomi() {
        if (Float.isNaN(dynamicKomi) || Float.isNaN(dynamicOppKomi)) {
            return null;
        }
        return String.format("%.1f / %.1f", dynamicKomi, dynamicOppKomi);
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
            // copy the list to avoid concurrent modification exception... TODO there must be a better way
            // (note the concurrent modification exception is very very rare)
            final List<MoveData> moves = new ArrayList<MoveData>(bestMoves);

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
        while (stats.totalPlayouts < 1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
            stats = getWinrateStats();
        }
        mHandicapWinrate = stats.maxWinrate;
        togglePonder();
        undo();
        Lizzie.board.clear();
    }

    public static double mHandicapWinrate = 25;

    /**
     * Convert winrate to handicap stones, by normalizing winrate by first move pass winrate (one stone handicap).
     */
    public static double winrateToHandicap(double pWinrate) {
        // we assume each additional handicap lowers winrate by fixed percentage.
        // this is pretty accurate for human handicap games at least.
        // also this kind of property is a requirement for handicaps to determined based on rank difference.

        // lets convert the 0%-50% range and 100%-50% from both the move and and pass into range of 0-1
        double moveWinrateSymmetric = 1 - Math.abs(1 - (pWinrate / 100) * 2);
        double passWinrateSymmetric = 1 - Math.abs(1 - (mHandicapWinrate / 100) * 2);

        // convert the symmetric move winrate into correctly scaled log scale, so that winrate of passWinrate equals 1 handicap.
        double handicapSymmetric = Math.log(moveWinrateSymmetric) / Math.log(passWinrateSymmetric);

        // make it negative if we had low winrate below 50.
        return Math.signum(pWinrate - 50) * handicapSymmetric;
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
        for (LeelazListener listener : listeners) {
            listener.bestMoveNotification(bestMoves);
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public String currentWeight() {
        return currentWeight;
    }

    public boolean switching() {
        return switching;
    }

    public int currentEngineN() {
        return currentEngineN;
    }

    public String engineCommand() {
        return this.engineCommand;
    }
}
