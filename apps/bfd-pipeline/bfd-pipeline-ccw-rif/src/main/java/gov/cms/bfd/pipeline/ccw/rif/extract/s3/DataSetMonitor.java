package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.S3TaskManager;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This ETL pipeline is fed by data pushed from CMS' Chronic Conditions Data (CCW) into Amazon's S3
 * API. The data in S3 will be structured as follows:
 *
 * <ul>
 *   <li>Amazon S3 Bucket: <code>&lt;s3-bucket-name&gt;</code>
 *       <ul>
 *         <li><code>1997-07-16T19:20:30Z</code>
 *             <ul>
 *               <li><code>Incoming</code>
 *                   <ul>
 *                     <li><code>23_manifest.xml</code>
 *                     <li><code>beneficiaries_42.rif</code>
 *                     <li><code>bcarrier_58.rif</code>
 *                     <li><code>pde_93.rif</code>
 *                   </ul>
 *               <li><code>Done</code>
 *                   <ul>
 *                     <li><code>64_manifest.xml</code>
 *                     <li><code>beneficiaries_45.rif</code>
 *                   </ul>
 *             </ul>
 *       </ul>
 * </ul>
 *
 * <p>In that structure, there will be one top-level directory in the bucket for each data set that
 * has yet to be completely processed by the ETL pipeline. Its name will be an <a
 * href="https://www.w3.org/TR/NOTE-datetime">ISO 8601 date and time</a> expressed in UTC, to a
 * precision of at least seconds. This will represent (roughly) the time that the data set was
 * created. Within each of those directories will be manifest files and the RIF files that they
 * reference.
 *
 * <p>The ETL operates in a loop: periodically checking for the oldest manifest file that can be
 * found and then handing it off to the rest of the pipeline for processing.
 */
public final class DataSetMonitor {
  /**
   * The {@link Logger} message that will be recorded if/when the {@link DataSetMonitor} starts
   * scanning for data sets.
   */
  public static final String LOG_MESSAGE_STARTING_WORKER =
      "Starting data set monitor: watching for data sets to process...";

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMonitor.class);

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception in {@link DataSetMonitor}.
   */
  public static final int EXIT_CODE_MONITOR_ERROR = 2;

  private final MetricRegistry appMetrics;
  private final ExtractionOptions options;
  private final int scanRepeatDelay;
  private final DataSetMonitorListener listener;

  private TaskExecutor dataSetWatcherExecutor;
  private S3TaskManager s3TaskManager;
  private ScheduledFuture<?> dataSetWatcherFuture;
  private DataSetMonitorWorker dataSetWatcher;

  /**
   * Constructs a new {@link DataSetMonitor} instance. Note that this must be used as a singleton
   * service in the application: only one instance running at a time is supported.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param options the {@link ExtractionOptions} to use
   * @param scanRepeatDelay the number of milliseconds to wait after completing one poll/process
   *     operation and starting another
   * @param listener the {@link DataSetMonitorListener} that will be notified when events occur
   */
  public DataSetMonitor(
      MetricRegistry appMetrics,
      ExtractionOptions options,
      int scanRepeatDelay,
      DataSetMonitorListener listener) {
    this.appMetrics = appMetrics;
    this.options = options;
    this.scanRepeatDelay = scanRepeatDelay;
    this.listener = listener;

    this.dataSetWatcherExecutor = null;
    this.dataSetWatcherFuture = null;
    this.dataSetWatcher = null;
  }

  /**
   * Starts this monitor: it will begin regularly polling for and processing new data sets on a
   * background thread. This particular method will return immediately, but that background
   * processing will continue to run asynchronously until {@link #stop()} is called.
   */
  public void start() {
    // Instances of this class are single-use-only.
    if (this.dataSetWatcherExecutor != null || this.dataSetWatcherFuture != null)
      throw new IllegalStateException();

    this.dataSetWatcherExecutor = new TaskExecutor("Data Set Watcher Executor", 1);
    this.s3TaskManager = new S3TaskManager(appMetrics, options);
    this.dataSetWatcher = new DataSetMonitorWorker(appMetrics, options, s3TaskManager, listener);
    Runnable errorNotifyingDataSetWatcher =
        new ErrorNotifyingRunnableWrapper(dataSetWatcher, listener);

    /*
     * This kicks off the data set watcher, which will be run periodically
     * (as configured), until either A) this instance's stop() method is
     * called to request a graceful shutdown, or B) one of the watcher
     * execution runs fails. In turn, it basically acts as the application's
     * main loop, and will drive all of the application's data processing,
     * etc. on the ScheduledExecutorService's separate thread. (To be clear:
     * this method does not block, and should return normally almost
     * immediately.)
     */
    LOGGER.info(LOG_MESSAGE_STARTING_WORKER);
    this.dataSetWatcherFuture =
        dataSetWatcherExecutor.scheduleWithFixedDelay(
            errorNotifyingDataSetWatcher, 0, scanRepeatDelay, TimeUnit.MILLISECONDS);
  }

  /**
   * Stops this monitor: it will wait for the current polling/processing operation (if any) to
   * complete normally, then stop any future polling/processing runs from starting. This method will
   * block until any current operations have completed and future ones have been cancelled.
   *
   * <p><strong>Note:</strong> This might block for a while! Some data sets have terabytes of data,
   * and there is no way to safely stop processing in the middle of a data set.
   */
  public void stop() {
    LOGGER.debug("Stopping...");

    // If we haven't started yet, this is easy.
    if (dataSetWatcherFuture == null) {
      return;
    }

    // If something has already shut us down, we're done.
    if (dataSetWatcherExecutor.isShutdown()) {
      return;
    }

    /*
     * Signal the scheduler to stop after the current DataSetMonitorWorker
     * execution (if any), then wait for that to happen.
     */
    dataSetWatcherExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    dataSetWatcherExecutor.shutdown();
    dataSetWatcherFuture.cancel(false);
    waitForStop();

    // Clean house.
    s3TaskManager.shutdownSafely();

    LOGGER.debug("Stopped.");
  }

  /**
   * Waits for the data set watcher to complete, presumably after something has asked it to stop.
   * (If that hasn't happened, and doesn't, this method will block forever.)
   */
  private void waitForStop() {
    // If we haven't started yet, this is easy.
    if (dataSetWatcherFuture == null) return;

    try {
      /*
       * Our Future here is tied to a Runnable, so there's nothing for
       * this `.get()` call to actually return. Instead, per the JavaDoc
       * for ScheduledExecutorService.scheduleWithFixedDelay(...), this
       * method will block unless/until something cancels the Future. At
       * that time, it will wait for any current execution/iteration of
       * the Runnable to complete normally. Then, it will throw a
       * CancellationException to signal that things have gracefully
       * stopped. A bit counter-intuitive, but it works. Alternatively, if
       * one of the Runnable's iterations has failed, this will return an
       * ExecutionException that wraps the failure.
       */
      LOGGER.info("Waiting for any in-progress data set processing to gracefully complete...");
      dataSetWatcherFuture.get();
    } catch (CancellationException e) {
      /*
       * This is expected to occur when the app is being gracefully shut
       * down (see the stop() method). It should only happen **after** the
       * last execution of our Runnable. Accordingly, we'll just log the
       * event and allow this method to stop blocking and return.
       */
      LOGGER.info("Data set processing has been gracefully stopped.");
      return;
    } catch (InterruptedException e) {
      /*
       * Many Java applications use InterruptedExceptions to signal that a
       * thread should stop what it's doing ASAP. This app doesn't, so
       * this is unexpected, and accordingly, we don't know what to do.
       * Safest bet is to blow up.
       */
      throw new BadCodeMonkeyException(e);
    } catch (ExecutionException e) {
      /*
       * This will only occur if the Runnable (dataSetWatcherFuture)
       * failed with an unhandled exception. This is unexpected, and
       * accordingly, we don't know what to do. Safest bet is to blow up.
       */
      throw new BadCodeMonkeyException(e);
    }

    LOGGER.info("Data set processing was stopped.");
  }

  /**
   * @return <code>true</code> if this {@link DataSetMonitor} is running, <code>false</code> if it
   *     is not
   */
  public boolean isStopped() {
    return dataSetWatcherExecutor != null && dataSetWatcherExecutor.isShutdown();
  }

  /**
   * Wraps a {@link Runnable} (presumably our {@link DataSetMonitorWorker}) and catches any
   * unhandled exceptions if it blows up.
   *
   * <p>This is needed because otherwise, we won't find out about that error unless/until we try to
   * call <code>dataSetWatcherFuture.get()</code>. And starting up a separate thread to poll that
   * would be silly.
   */
  private static final class ErrorNotifyingRunnableWrapper implements Runnable {
    private final Runnable wrappedRunnable;
    private final DataSetMonitorListener listener;

    /**
     * Constructs a new {@link RunnableWrapper} instance.
     *
     * @param wrappedRunnable the {@link Runnable} to wrap and execute
     * @param listener the {@link Consumer} to send any errors that are caught to
     */
    public ErrorNotifyingRunnableWrapper(
        Runnable wrappedRunnable, DataSetMonitorListener listener) {
      this.wrappedRunnable = wrappedRunnable;
      this.listener = listener;
    }

    /** @see java.lang.Runnable#run() */
    @Override
    public void run() {
      try {
        wrappedRunnable.run();
      } catch (Throwable t) {
        /*
         * First, notify our error receiver so that they can do whatever
         * it is they need to do.
         */
        listener.errorOccurred(t);

        /*
         * As it's expected that the error receiver might synchronously
         * call System.exit(...), there's no guarantee that anything
         * else we try to do here will succeed. So... we don't do
         * anything.
         */
      }
    }
  }
}
