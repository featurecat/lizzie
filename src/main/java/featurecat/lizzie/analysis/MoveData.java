package featurecat.lizzie.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * holds the data from Leelaz's pondering mode
 */
public class MoveData implements Comparable<MoveData> {
    public String coordinate;
    public int playouts;
    public double winrate;
    public int order;
    public List<String> variation;

    /**
     * Parses a leelaz ponder output line
     * @param line line of ponder output
     */
    public MoveData(String line) throws ArrayIndexOutOfBoundsException {
        String[] data = line.trim().split(" ");

        // Todo: Proper tag parsing in case gtp protocol is extended(?)/changed
        coordinate = data[1];
        playouts = Integer.parseInt(data[3]);
        winrate = Integer.parseInt(data[5])/100.0;
        order = Integer.parseInt(data[7]);

        variation = new ArrayList<>(Arrays.asList(data));
        variation = variation.subList(9, variation.size());
    }

    public int compareTo(MoveData b) {
        return order - b.order;
    }
}
