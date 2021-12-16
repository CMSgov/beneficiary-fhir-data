package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Several classes contain multiple resources that require cleanup in a close method. An exception
 * thrown while cleaning up one of these resources should not abort the entire process. This class
 * allows calls to multiple close methods to be chained and any exceptions thrown by them to be
 * consolidated into a single exception that can be thrown after all of the calls have been
 * completed.
 */
@ThreadSafe
public class MultiCloser {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCloser.class);

  private Exception error = null;

  /**
   * Invoke the Closer and capture any exception it throws.
   *
   * @param closer method to perform some cleanup that might throw
   */
  public synchronized void close(Closer closer) {
    try {
      closer.close();
    } catch (Exception ex) {
      LOGGER.error("captured exception: message={}", ex.getMessage(), ex);
      if (error == null) {
        error = ex;
      } else {
        error.addSuppressed(ex);
      }
    }
  }

  /**
   * Called after all close methods have been called to throw any captured exceptions.
   *
   * @throws Exception consolidated exception including any exceptions thrown by close methods
   */
  public synchronized void finish() throws Exception {
    if (error != null) {
      throw error;
    }
  }

  public interface Closer {
    void close() throws Exception;
  }
}
