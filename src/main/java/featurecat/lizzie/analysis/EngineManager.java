package featurecat.lizzie.analysis;

import featurecat.lizzie.Config;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EngineManager {

  private List<Leelaz> engineList;
  private int currentEngineNo;
  private String engineCommand;

  public EngineManager(Config config) throws JSONException, IOException {

    JSONObject eCfg = config.config.getJSONObject("leelaz");
    engineCommand = eCfg.getString("engine-command");
    // substitute in the weights file
    engineCommand = engineCommand.replaceAll("%network-file", eCfg.getString("network-file"));

    // Start default engine
    Leelaz lz = new Leelaz(engineCommand);
    Lizzie.leelaz = lz;
    Lizzie.board = lz.board;
    lz.startEngine();
    lz.preload = true;
    engineList = new ArrayList<Leelaz>();
    engineList.add(lz);
    currentEngineNo = 0;

    new Thread(
            () -> {
              // Process other engine
              Optional<JSONArray> enginesOpt =
                  Optional.ofNullable(
                      Lizzie.config.leelazConfig.optJSONArray("engine-command-list"));
              Optional<JSONArray> enginePreloadOpt =
                  Optional.ofNullable(
                      Lizzie.config.leelazConfig.optJSONArray("engine-preload-list"));
              enginesOpt.ifPresent(
                  m -> {
                    IntStream.range(0, m.length())
                        .forEach(
                            i -> {
                              String cmd = m.optString(i);
                              if (cmd != null && !cmd.isEmpty()) {
                                Leelaz e;
                                try {
                                  e = new Leelaz(cmd);
                                  // TODO: how sync the board
                                  e.board = Lizzie.board;
                                  e.preload =
                                      enginePreloadOpt.map(p -> p.optBoolean(i)).orElse(false);
                                  if (e.preload) {
                                    e.startEngine();
                                  } else {
                                    e.setWeightName();
                                  }
                                  // TODO: Need keep analyze?
                                  // e.togglePonder();
                                  engineList.add(e);
                                } catch (JSONException | IOException e1) {
                                  e1.printStackTrace();
                                }
                              } else {
                                // empty
                                engineList.add(null);
                              }
                            });
                    Lizzie.frame.updateEngineMenu(engineList);
                  });
            })
        .start();
  }

  /**
   * Switch the Engine by index number
   *
   * @param index engine index
   */
  public void switchEngine(int index) {
    if (index == this.currentEngineNo || index > this.engineList.size()) return;
    Leelaz newEng = engineList.get(index);
    if (newEng == null) return;

    Leelaz curEng = engineList.get(this.currentEngineNo);
    if (curEng.isThinking) {
      if (Lizzie.frame.isPlayingAgainstLeelaz) {
        Lizzie.frame.isPlayingAgainstLeelaz = false;
        Lizzie.leelaz.isThinking = false;
      }
      curEng.togglePonder();
    }

    // TODO: Need keep analyze?
    if (curEng.isPondering()) {
      curEng.togglePonder();
    }

    if (curEng.isKataGo && Lizzie.config.showKataGoEstimate) {
      Lizzie.frame.removeEstimateRect();
    }
    curEng.board.saveMoveNumber();
    try {
      Lizzie.leelaz = newEng;
      // TODO: how sync the board
      //      newEng.board = curEng.board;
      //      Lizzie.board = newEng.board;
      if (!newEng.isStarted()) {
        newEng.startEngine();
      } else {
        if (!newEng.isPondering()) {
          newEng.togglePonder();
        }
      }
      Lizzie.board.restoreMoveNumber();
      this.currentEngineNo = index;
      if (!curEng.preload) {
        curEng.normalQuit();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void refresh() throws JSONException, IOException {

    JSONObject eCfg = Lizzie.config.config.getJSONObject("leelaz");
    engineCommand = eCfg.getString("engine-command");
    engineCommand = engineCommand.replaceAll("%network-file", eCfg.getString("network-file"));
    Optional<JSONArray> enginesOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-command-list"));
    Optional<JSONArray> enginePreloadOpt =
        Optional.ofNullable(Lizzie.config.leelazConfig.optJSONArray("engine-preload-list"));

    String newCmd =
        (currentEngineNo == 0 ? engineCommand : enginesOpt.get().optString(currentEngineNo - 1));
    boolean preload =
        (currentEngineNo == 0
            ? true
            : enginePreloadOpt.map(p -> p.optBoolean(currentEngineNo - 1)).orElse(false));

    Leelaz lz = engineList.get(currentEngineNo);
    if (lz.isCommandChange(newCmd)) {
      forceRestart(currentEngineNo, newCmd, preload);
    } else if (preload != lz.preload) {
      lz.preload = preload;
      if (preload) {
        lz.startEngine();
      } else {
        lz.normalQuit();
      }
    }

    new Thread(
            () -> {
              if (currentEngineNo != 0) {
                updateEngine(0, engineCommand, true);
              }
              enginesOpt.ifPresent(
                  m -> {
                    IntStream.range(0, m.length())
                        .forEach(
                            i -> {
                              String cmd = m.optString(i);
                              int index = i + 1;
                              if (index != currentEngineNo) {
                                updateEngine(
                                    index,
                                    cmd,
                                    enginePreloadOpt.map(p -> p.optBoolean(i)).orElse(false));
                              }
                            });
                  });
              Lizzie.frame.updateEngineMenu(engineList);
            })
        .start();
  }

  public void forceRestart(int index, String command, boolean preload) {
    if (index < 0 || index > this.engineList.size()) return;
    if (Utils.isBlank(command)) {
      engineList.set(index, null);
      return;
    }

    Leelaz curEng = Lizzie.leelaz;
    if (curEng.isThinking) {
      if (Lizzie.frame.isPlayingAgainstLeelaz) {
        Lizzie.frame.isPlayingAgainstLeelaz = false;
        curEng.isThinking = false;
      }
      curEng.togglePonder();
    }
    if (curEng.isPondering()) {
      curEng.togglePonder();
    }
    if (curEng.isKataGo && Lizzie.config.showKataGoEstimate) {
      Lizzie.frame.removeEstimateRect();
    }
    Lizzie.board.saveMoveNumber();
    try {
      Leelaz newEng = new Leelaz(command);
      Lizzie.leelaz = newEng;
      newEng.board = Lizzie.board;
      if (!newEng.isStarted()) {
        newEng.startEngine();
      } else {
        if (!newEng.isPondering()) {
          newEng.togglePonder();
        }
      }
      newEng.preload = preload;
      Lizzie.board.restoreMoveNumber();
      engineList.set(index, Lizzie.leelaz);
      curEng.normalQuit();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void updateEngine(int index, String command, boolean preload) {
    Leelaz e = engineList.get(index);
    if ((e == null && !Utils.isBlank(command)) || (e != null && e.isCommandChange(command))) {
      Leelaz oldE = e;
      if (!Utils.isBlank(command)) {
        try {
          e = new Leelaz(command);
          e.board = Lizzie.board;
          e.preload = preload;
          if (e.preload) {
            e.startEngine();
          } else {
            e.setWeightName();
          }
          engineList.set(index, e);
        } catch (JSONException | IOException e1) {
          e1.printStackTrace();
        }
      } else {
        engineList.set(index, null);
      }
      if (oldE != null && oldE.preload) {
        oldE.normalQuit();
      }
    } else if (e != null && preload != e.preload) {
      e.preload = preload;
      if (preload) {
        try {
          e.startEngine();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      } else {
        e.normalQuit();
      }
    }
  }

  public void updateEngineIcon() {
    Lizzie.frame.updateEngineIcon(engineList, currentEngineNo);
  }
}
