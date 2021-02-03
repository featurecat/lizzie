package featurecat.lizzie.rules;

import featurecat.lizzie.Lizzie;
import java.util.StringJoiner;

public class RegionOfInterest {
  private String vertices;
  private int[] startCorner;
  private int left, right, top, bottom;

  public RegionOfInterest() {
    reset();
  }

  public void reset() {
    vertices = "";
    startCorner = null;
  }

  public String vertices() {
    return vertices;
  }

  public Boolean isEnabled() {
    return !vertices.isEmpty();
  }

  public Boolean isEnabledOrInSetting() {
    return isEnabled() || isInSetting();
  }

  public int left() {
    return left;
  }

  public int right() {
    return right;
  }

  public int top() {
    return top;
  }

  public int bottom() {
    return bottom;
  }

  public void startSetting(int[] corner) {
    startCorner = corner;
    if (startCorner == null) return;
    left = right = startCorner[0];
    top = bottom = startCorner[1];
  }

  public Boolean isInSetting() {
    return startCorner != null;
  }

  public Boolean updateSetting(int[] endCorner) {
    int[] a = startCorner, b = endCorner;
    if (a == null || b == null) return false;
    int i0 = Math.min(a[0], b[0]), j0 = Math.min(a[1], b[1]);
    int i1 = Math.max(a[0], b[0]), j1 = Math.max(a[1], b[1]);
    boolean changed = (left != i0) || (right != i1) || (top != j0) || (bottom != j1);
    left = i0;
    right = i1;
    top = j0;
    bottom = j1;
    return changed;
  }

  public void finishSetting() {
    vertices = verticesInRectangle();
    startCorner = null;
  }

  private String verticesInRectangle() {
    if (left == right && top == bottom) return "";
    StringJoiner sj = new StringJoiner(",");
    for (int i = left; i <= right; i++)
      for (int j = top; j <= bottom; j++) sj.add(Lizzie.board.convertCoordinatesToName(i, j));
    return sj.toString();
  }
}
