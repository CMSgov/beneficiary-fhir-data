package gov.cms.bfd.pipeline.rif.extract.s3;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ScheduledThreadPoolExecutor} that can be used to run asynchronous tasks. Why this
 * instead of something from {@link Executors}? Because this will properly "bubble up" task failures
 * to the {@link Thread} and its uncaught exception handler, allowing our application to gracefully
 * shutdown when things go boom.
 */
public final class TaskExecutor extends ScheduledThreadPoolExecutor {
  /**
   * Constructs a new {@link TaskExecutor} instance.
   *
   * @param threadPoolSize the number of threads to maintain in the thread pool
   */
  public TaskExecutor(int threadPoolSize) {
    super(threadPoolSize);
    setKeepAliveTime(100L, TimeUnit.MILLISECONDS);
    allowCoreThreadTimeOut(true);
  }

  /**
   * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable,
   *     java.lang.Throwable)
   */
  @Override
  protected void afterExecute(Runnable runnable, Throwable throwable) {
    super.afterExecute(runnable, throwable);

    /*
     * We need to ensure that failed tasks at least get logged, so that those failures can be
     * investigated.
     */

    if (throwable == null && runnable instanceof Future<?>) {
      try {
        Future<?> future = (Future<?>) runnable;
        if (future.isDone()) {
          future.get();
        }
      } catch (CancellationException ce) {
        throwable = ce;
      } catch (ExecutionException ee) {
        throwable = ee.getCause();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    /*
     * Wrap and rethrow: Ensure that the (outer) application gets a chance to respond to this
     * error, via the thread's uncaught exception handler.
     */
    if (throwable != null) {
      throw new RuntimeException("Asynchronous task failed.", throwable);
    }
  }
}
