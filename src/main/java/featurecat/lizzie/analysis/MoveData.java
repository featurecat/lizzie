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

        for(int i=1; i<data.length; i+=2) {
            String tag = data[i-1];
            String value = data[i];

            if (tag.equals("move")) {
                coordinate = value;
            } else if (tag.equals("visits")) {
                playouts = Integer.parseInt(value);
            } else if (tag.equals("winrate")) {
                winrate = Integer.parseInt(value) / 100.0;
            } else if (tag.equals("order")) {
                order = Integer.parseInt(value);
            } else if (tag.equals("pv")) {
                variation = new ArrayList<>(Arrays.asList(data));
                variation = variation.subList(i, variation.size());
                break;
            }
        }
    }

    public int compareTo(MoveData b) {
        return order - b.order;
    }
}
