package wagner.stephanie.lizzie;

import wagner.stephanie.lizzie.rules.Stone;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: add license information from Leela Zero repo?
 * an interface with leelaz.exe go engine. Can be adapted for GTP, but is specifically designed for GCP's Leela Zero.
 */
public class Leelaz {
    private Process process;

    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;

    // keeps track of whether or not we need to resync all the moves
//    private boolean inSync = true;

    /**
     * Initializes the leelaz process and starts reading output
     *
     * @throws IOException
     */
    public Leelaz() throws IOException {
        // list of commands for the leelaz process
        // TODO replace path names
        List<String> commands = new ArrayList<>();
        commands.add("C:\\Users\\sww\\Desktop\\lizzie\\leelaz.exe");
        commands.add("-g");
        commands.add("-wbest");

        // run leelaz.exe
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File("C:\\Users\\sww\\Desktop\\lizzie"));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();

        initializeStreams();

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

    /**
     * Continually reads and processes output from leelaz
     */
    private void read() {
        try {
            int c;
            while ((c = inputStream.read()) != -1) {
                System.out.print((char) c);
            }
            // this line will only be reached if Leelaz crashes
            System.err.println("Leelaz process ended unexpectedly.");
            System.exit(-1);
        } catch (Exception e) {
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
        try {
            outputStream.write((command + "\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param color color of stone to play
     * @param move coordinate of the move
     */
    public void playMove(Stone color, String move) {
        String colorString;
        switch (color) {
            case BLACK:
                colorString = "B";
                break;
            case WHITE:
                colorString = "W";
                break;
            default:
                throw new IllegalArgumentException("The stone color must be BLACK or WHITE, but was " + color.toString());
        }

//        if (inSync)
        sendCommand("play " + colorString + " " + move);
    }

    public void undo() {
//        inSync = false;
        sendCommand("undo");
    }

    /**
     * this initializes leelaz's pondering mode at its current position
     */
    public void ponder() {
        sendCommand("time_left b 0 0");
    }
}
