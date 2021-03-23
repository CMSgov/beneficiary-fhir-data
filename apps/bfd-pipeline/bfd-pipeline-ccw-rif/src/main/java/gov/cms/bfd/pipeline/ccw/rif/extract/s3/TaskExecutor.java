package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ScheduledThreadPoolExecutor} that can be used to run asynchronous tasks. Why this
 * instead of something from {@link Executors}? Because this will properly "bubble up" task failures
 * to the {@link Thread} and its uncaught exception handler, allowing our application to gracefully
 * shutdown when things go boom.
 */
public final class TaskExecutor extends ScheduledThreadPoolExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

  private final String name;

  /**
   * Constructs a new {@link TaskExecutor} instance.
   *
   * @param threadPoolSize the number of threads to maintain in the thread pool
   */
  public TaskExecutor(String name, int threadPoolSize) {
    super(threadPoolSize);
    setKeepAliveTime(100L, TimeUnit.MILLISECONDS);
    allowCoreThreadTimeOut(true);

    this.name = name;
  }

  /**
   * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable,
   *     java.lang.Throwable)
   */
  @Override
  protected void afterExecute(Runnable runnable, Throwable throwable) {
    super.afterExecute(runnable, throwable);

    /*
     * We need to ensure that failure of asynchronous tasks cause exceptions to get thrown on this
     * thread, which should ultimately bubble up to something that the application can handle, e.g.
     * in an uncaught exception handler.
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
      if (throwable instanceof CancellationException) {
        // Don't bubble up cancellations, as they're not "real" errors.
        LOGGER.trace(
            String.format(
                "Asynchronous task '%s' on the '%s' executor was cancelled.",
                getTaskId(runnable), this.name),
            throwable);
      } else {
        throw new RuntimeException(
            String.format(
                "Asynchronous task '%s' on the '%s' executor failed.",
                getTaskId(runnable), this.name),
            throwable);
      }
    }
  }

  /**
   * Returns an identifying {@link String} for the specified task object, e.g. a {@link Runnable},
   * {@link Future}, etc. Only suitable for use in debugging.
   *
   * @param task the task object (e.g. {@link Runnable}, {@link Future}) to try and get an
   *     identifying {@link String} for
   */
  public static String getTaskId(Object task) {
    /*
     * The JDK executors really don't provide much monitoring or other metadata, which makes them a
     * pain to debug. This isn't much, but it does help somewhat when trying to track down problems.
     */

    try {
      if (task.getClass().getName().contains("ScheduledFutureTask")) {
        /*
         * Tasks submitted to TaskExecutor get wrapped in a ScheduledFutureTask, which this branch
         * covers.
         */

        Field timeField = task.getClass().getDeclaredField("time");
        timeField.setAccessible(true);
        return "" + timeField.get(task);
      } else {
        return String.format("(unknown: %s)", task);
      }
    } catch (NoSuchFieldException
        | SecurityException
        | IllegalArgumentException
        | IllegalAccessException e) {
      return String.format("(unknown: %s)", task);
    }
  }
}
