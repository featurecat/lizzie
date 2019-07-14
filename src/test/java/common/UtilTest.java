package featurecat.lizzie.analysis;

import static org.junit.Assert.assertEquals;

import common.Util;
import featurecat.lizzie.*;
import featurecat.lizzie.rules.*;
import org.junit.Test;

public class UtilTest {
  @Test
  public void testRemoveLzSgf() {
    assertEquals("", Util.removeLzSgf("LZ[0.7 50.0 0]"));
    assertEquals("", Util.removeLzSgf("LZ[0.7 50.0 0\r\n]"));
    assertEquals("LZ[0.7 ", Util.removeLzSgf("LZ[0.7 "));

    String sgf =
        ";B[pd][LZ[0.7 50.0 0\n"
            + "];W[dp]LZ[0.7 50.0 0\n"
            + "];B[pp]LZ[0.7 50.0 0\n"
            + "];W[dd]LZ[0.7 50.0 0\n"
            + "]]";
    assertEquals(";B[pd][;W[dp];B[pp];W[dd]]", Util.removeLzSgf(sgf));
  }
}
