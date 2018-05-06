package wagner.stephanie.lizzie.analysis;

import jdk.nashorn.internal.runtime.arrays.ArrayIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * holds the data from Leelaz's pondering mode
 */
public class MoveData {
    public String coordinate;
    public int playouts;
    public double winrate;
    public List<String> variation;

    /**
     * Parses a leelaz ponder output line
     * @param line line of ponder output
     */
    public MoveData(String line) throws ArrayIndexOutOfBoundsException {
        String[] data = line.trim().split(" +");

        coordinate = data[0];
        playouts = Integer.parseInt(data[2]);
        winrate = Double.parseDouble(data[4].substring(0, data[4].length() - 2));

        variation = new ArrayList<>();
        // Leela 0.11.0 has extra fields before PV.
        line = line.split("PV: *")[1];
        data = line.trim().split(" +");
        // System.out.println(" ## " + line); System.out.flush();
        variation.addAll(Arrays.asList(data));
    }
}
