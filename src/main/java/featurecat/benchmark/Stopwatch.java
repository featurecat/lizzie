package featurecat.benchmark;

import java.util.LinkedHashMap;

/** Simple stopwatch profiler to benchmark how long code takes to run */
public class Stopwatch {
  private LinkedHashMap<String, Long> times;
  private long startTime;
  private long lastTime;

  /** Begins timing from the moment this object is created. */
  public Stopwatch() {
    times = new LinkedHashMap<>();
    startTime = System.nanoTime();
    lastTime = startTime;
  }

  /**
   * Mark down the current time that it took $marker$ to run.
   *
   * @param marker a tag to describe what section of code is being profiled
   */
  public void lap(String marker) {
    long currentTime = System.nanoTime();
    times.put(marker, currentTime - lastTime);
    lastTime = currentTime;
  }

  /** Print the recorded profiler statistics */
  public void print() {
    System.out.println("\n======== profiler ========");
    long totalTime = lastTime - startTime;
    for (String marker : times.keySet()) {
      System.out.printf("%5.1f%%  %s\n", 100.0 * times.get(marker) / totalTime, marker);
    }
    System.out.println("in " + totalTime / 1_000_000.0 + " ms");
  }

  public void printTimePerAction(int numActionsExecuted) {
    System.out.println("\n======== profiler ========");
    long totalTime = System.nanoTime() - startTime;
    System.out.println((totalTime / 1_000_000.0 / numActionsExecuted) + " ms per action");
    System.out.println(
        numActionsExecuted + " total actions executed in " + totalTime / 1_000_000_000.0 + " s");
  }

  /**
   * Reset the Stopwatch so it can be used again. Begins timing from the moment this method is
   * executed.
   */
  public void reset() {
    times = new LinkedHashMap<>();
    startTime = System.nanoTime();
    lastTime = startTime;
  }
}
