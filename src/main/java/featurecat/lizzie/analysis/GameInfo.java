package featurecat.lizzie.analysis;

import java.util.Date;

public class GameInfo {
  public static final String DEFAULT_NAME_HUMAN_PLAYER = "Human";
  public static final String DEFAULT_NAME_CPU_PLAYER = "Leela Zero";
  public static final double DEFAULT_KOMI = 7.5;

  private String playerBlack = "";
  private String playerWhite = "";
  private Date date = new Date();
  private double komi = DEFAULT_KOMI;
  private int handicap = 0;

  public String getPlayerBlack() {
    return playerBlack;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public void setPlayerBlack(String playerBlack) {
    this.playerBlack = playerBlack;
  }

  public String getPlayerWhite() {
    return playerWhite;
  }

  public void setPlayerWhite(String playerWhite) {
    this.playerWhite = playerWhite;
  }

  public double getKomi() {
    return komi;
  }

  public void setKomi(double komi) {
    this.komi = komi;
  }

  public int getHandicap() {
    return handicap;
  }

  public void setHandicap(int handicap) {
    this.handicap = handicap;
  }
}
