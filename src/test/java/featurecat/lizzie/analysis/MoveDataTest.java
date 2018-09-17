package featurecat.lizzie.analysis;

import org.junit.Test;
import java.util.List;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class MoveDataTest {
  @Test public void testFromInfoLine() {
    String info = "move R5 visits 38 winrate 5404 order 0 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5";
    MoveData moveData = MoveData.fromInfo(info);

    assertEquals(moveData.coordinate, "R5");
    assertEquals(moveData.playouts, 38);
    assertEquals(moveData.winrate, 54.04, 0.01);
    assertEquals(moveData.order, 0);

    List<String> expected = Arrays.asList(
      "R5", "Q5", "R6", "S4", "Q10", "C3", "D3", "C4", "C6", "C5", "D5");

    assertEquals(moveData.variation, expected);
  }
}
