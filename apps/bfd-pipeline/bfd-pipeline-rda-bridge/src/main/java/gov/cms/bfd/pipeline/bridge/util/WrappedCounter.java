package gov.cms.bfd.pipeline.bridge.util;

/** Wrapped long value used for keeping a counter between method scopes. */
public class WrappedCounter {
  /** Keeps a counter between method scopes. */
  private long counter;

  /**
   * Constructor instantiates a new {@link WrappedCounter} for counting between method scopes.
   *
   * @param start sets the start of the counter
   */
  public WrappedCounter(long start) {
    counter = start;
  }

  /**
   * Returns the current value of the counter and increments it.
   *
   * <p>Method equivalent of i++
   *
   * @return The current value of the counter prior to incrementing it
   */
  public long inc() {
    return counter++;
  }

  /**
   * Return the current value of the wrapped counter.
   *
   * @return The current value of the counter
   */
  public long get() {
    return counter;
  }
}
