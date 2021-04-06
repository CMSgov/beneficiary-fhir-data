package gov.cms.bfd.pipeline.dc.geo;

/**
 * Wrapper for exceptions thrown during batch process. Intended to capture the number of objects
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

  @Override
  public Exception getCause() {
    return (Exception) super.getCause();
  }
}
