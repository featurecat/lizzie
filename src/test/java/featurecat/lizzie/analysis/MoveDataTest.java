package featurecat.lizzie.analysis;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MoveDataTest {
  @Test
  public void testFromInfoLine() {
    String info = "move R5 visits 38 winrate 5404 order 0 pv R5 Q5 R6 S4 Q10 C3 D3 C4 C6 C5 D5";
    MoveData moveData = MoveData.fromInfo(info);

    assertEquals(moveData.coordinate, "R5");
    assertEquals(moveData.playouts, 38);
    assertEquals(moveData.winrate, 54.04, 0.01);
    assertEquals(
        moveData.variation,
        Arrays.asList("R5", "Q5", "R6", "S4", "Q10", "C3", "D3", "C4", "C6", "C5", "D5"));
  }

  private void testSummary(
      String summary, String coordinate, int playouts, double winrate, List<String> variation) {
    MoveData moveData = MoveData.fromSummary(summary);
    assertEquals(moveData.coordinate, coordinate);
    assertEquals(moveData.playouts, playouts);
    assertEquals(moveData.winrate, winrate, 0.01);
    assertEquals(moveData.variation, variation);
  }

  @Test
  public void summaryLine1() {
    testSummary(
        " P16 ->       4 (V: 50.94%) (N:  5.79%) PV: P16 N18 R5 Q5",
        "P16", 4, 50.94, Arrays.asList("P16", "N18", "R5", "Q5"));
  }

  @Test
  public void summaryLine2() {
    testSummary(
        "  D9 ->      59 (V: 60.61%) (N: 52.59%) PV: D9 D12 E9 C13 C15 F17",
        "D9", 59, 60.61, Arrays.asList("D9", "D12", "E9", "C13", "C15", "F17"));
  }

  @Test
  public void summaryLine3() {
    testSummary(
        "  B2 ->       1 (V: 46.52%) (N: 86.74%) PV: B2", "B2", 1, 46.52, Arrays.asList("B2"));
  }

  @Test
  public void summaryLine4() {
    testSummary(
        " D16 ->      33 (V: 53.63%) (N: 27.64%) PV: D16 D4 Q16 O4 C3 C4",
        "D16", 33, 53.63, Arrays.asList("D16", "D4", "Q16", "O4", "C3", "C4"));
  }

  @Test
  public void summaryLine5() {
    testSummary(
        " Q16 ->       0 (V:  0.00%) (N:  0.52%) PV: Q16\n", "Q16", 0, 0.0, Arrays.asList("Q16"));
  }
}
