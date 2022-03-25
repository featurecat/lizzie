package featurecat.lizzie.gui;

public class BranchFactory {
  public static BranchMove createBranch(int branchLength) {
    if (branchLength == BoardRenderer.SHOW_NORMAL_BOARD) {
      return new ShowNormalBoard();
    } else if (branchLength == BoardRenderer.SHOW_RAW_BOARD) {
      return new ShowRawBoard();
    } else {
      return new DefaultBoard();
    }
  }
}
