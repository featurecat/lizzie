package wagner.stephanie.lizzie.analysis;

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
    public MoveData(String line) {
        String[] data = line.trim().split(" +");

        coordinate = data[0];
        playouts = Integer.parseInt(data[2]);
        winrate = Double.parseDouble(data[4].substring(0, data[4].length() - 2));

        variation = new ArrayList<>();
        variation.addAll(Arrays.asList(data).subList(8, data.length));
    }
}
