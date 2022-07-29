package gov.cms.bfd.pipeline.rda.grpc;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.concurrent.ThreadSafe;
import lombok.extern.slf4j.Slf4j;

/**
 * Several classes contain multiple resources that require cleanup in a close method. An exception
 * thrown while cleaning up one of these resources should not abort the entire process. This class
 * allows calls to multiple close methods to be chained and any exceptions thrown by them to be
 * consolidated into a single exception that can be thrown after all the calls have been completed.
 */
@ThreadSafe
@Slf4j
public class MultiCloser implements AutoCloseable {

  private Exception error = null;
  private final Queue<AutoCloseable> closeables = new ConcurrentLinkedQueue<>();

  /**
   * Adds an {@link AutoCloseable} to be managed by the {@link MultiCloser}.
   *
   * @param closeable The {@link AutoCloseable} to be managed.
   */
  public void add(AutoCloseable closeable) {
    closeables.add(closeable);
  }

  /**
   * Invoke the Closer and capture any exception it throws.
   *
   * @param closer method to perform some cleanup that might throw
   */
  public synchronized void syncClose(AutoCloseable closer) {
    try {
      closer.close();
    } catch (Exception ex) {
      log.error("captured exception: message={}", ex.getMessage(), ex);
      if (error == null) {
        error = ex;
      } else {
        error.addSuppressed(ex);
      }
    }
  }

  /**
   * Closes all the managed {@link AutoCloseable} objects, suppressing any exceptions till the end,
   * then throwing if needed.
   *
   * @throws Exception If any managed {@link AutoCloseable} throws, with added supressions for each
   *     one thrown.
   */
  @Override
  public void close() throws Exception {
    closeables.forEach(this::syncClose);

    if (error != null) {
      throw error;
    }
  }
}
