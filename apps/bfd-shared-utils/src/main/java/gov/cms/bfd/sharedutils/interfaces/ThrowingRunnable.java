package gov.cms.bfd.sharedutils.interfaces;

/**
 * Helper Functional Interface for defining runnable logic that can throw some sort of exception.
 *
 * @param <E> The type of exception the runnable logic can throw.
 */
public interface ThrowingRunnable<E extends Throwable> {
  /**
   * Runs the specified logic.
   *
   * @throws E the exception thrown from this runnable
   */
  void run() throws E;
}
