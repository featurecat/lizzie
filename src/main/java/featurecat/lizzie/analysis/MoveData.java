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

        for (int i=0; i<data.length; i++) {
            String key = data[i];
            if (key.equals("pv")) {
                //read variation to the end of line
                variation = new ArrayList<>(Arrays.asList(data));
                variation = variation.subList(i+1, data.length);
                break;
            } else {
                String value = data[++i];
                if (key.equals("move")) {
                    coordinate = value;
                }
                if (key.equals("visits")) {
                    playouts =  Integer.parseInt(value);
                }
                if (key.equals("winrate")) {
                    winrate = Integer.parseInt(value)/100.0;
                }
                if (key.equals("order")) {
                    order = Integer.parseInt(value);
                }
            }
        }
    }

    public int compareTo(MoveData b) {
        return order - b.order;
    }
}
