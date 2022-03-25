package featurecat.lizzie.gui;

public class DefaultBoard implements BranchMove {
  @Override
  public int getBranchMove(int branchLength) {
    return branchLength;
  }
}
