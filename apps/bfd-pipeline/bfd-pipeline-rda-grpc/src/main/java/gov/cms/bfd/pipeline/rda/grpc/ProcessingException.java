package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Wrapper for exceptions thrown during batch processing. Intended to capture the number of objects
 * that were successfully processed before the exception was thrown.
 */
public class ProcessingException extends Exception {
  private final int processedCount;

  public ProcessingException(Exception cause, int processedCount) {
    super(cause);
    this.processedCount = processedCount;
  }

  /**
   * The number of objects that had been successfully processed within the batch prior to the
   * exception being thrown.
   *
   * @return number of objects successfully processed within the batch
   */
  public int getProcessedCount() {
    return processedCount;
  }

  /**
   * The actual underlying exception that terminated batch processing.
   *
   * @return the actual exception that terminated batch processing
   */
  @Override
  public Exception getCause() {
    return (Exception) super.getCause();
  }

  /**
   * Looks through the chain of exception causes to see if any of them are an InterruptedException.
   * This allows us to capture the case where some nested method call is interrupted and winds up
   * being wrapped in some other exception.
   *
   * @return true if and only if the ultimate cause is an InterruptedException
   */
  public static boolean isInterrupted(Throwable error) {
    while (error != null) {
      if (error instanceof InterruptedException) {
        return true;
      }
      Throwable cause = error.getCause();
      error = (cause == error) ? null : cause;
    }
    return false;
  }
}
