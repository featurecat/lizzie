package featurecat.lizzie.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds the data from Leelaz's pondering mode
 */
public class MoveData implements Comparable<MoveData> {
    public String coordinate;
    public int playouts;
    public double winrate;
    public int order;
    public List<String> variation;

    private MoveData() {}

    public int compareTo(MoveData b) {
        return order - b.order;
    }

    /**
     * Parses a leelaz ponder output line. For example:
     *
     * info move R5 visits 38 winrate 5404 order 0 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5
     *
     * @param line line of ponder output
     */
    public static MoveData fromInfo(String line) throws ArrayIndexOutOfBoundsException {
        MoveData result = new MoveData();
        String[] data = line.trim().split(" ");

        // Todo: Proper tag parsing in case gtp protocol is extended(?)/changed
        for (int i=0; i<data.length; i++) {
            String key = data[i];
            if (key.equals("pv")) {
                // Read variation to the end of line
                result.variation = new ArrayList<>(Arrays.asList(data));
                result.variation = result.variation.subList(i + 1, data.length);
                break;
            } else {
                String value = data[++i];
                if (key.equals("move")) {
                    result.coordinate = value;
                }
                if (key.equals("visits")) {
                    result.playouts = Integer.parseInt(value);
                }
                if (key.equals("winrate")) {
                    result.winrate = Integer.parseInt(value) / 100.0;
                }
            }
        }

        return result;
    }

    /**
     * Parses a leelaz summary output line. For example:
     *
     * P16 ->       4 (V: 50.94%) (N:  5.79%) PV: P16 N18 R5 Q5
     *
     * @param line line of summary output
     */
    public static MoveData fromSummary(String summary) {
      return new MoveData(); // TODO
    }
}
