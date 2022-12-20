package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ReportingCallback.ProcessedBatch;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Callable object submitted to an ExecutorService to process RDA API sequence numbers and write
 * them to the database using an RdaSink object. Each thread has its own BlockingQueue to receive
 * inbound sequence numbers for processing and uses a callback function to report its successes and
 * failures.
 *
 * <p>The thread is stopped by calling its close() method. This sets a flag that causes the thread
 * to drain its queue and shut down.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA entity class
 */
public class SequenceNumberWriterThread<TMessage, TClaim>
    implements Callable<Integer>, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(SequenceNumberWriterThread.class);

  /**
   * Max time the main thread can be blocked if our queue is full. This would only happen if our
   * thread has become stuck and is not draining the queue.
   */
  private static final Duration AddTimeout = Duration.ofMinutes(5);

  /**
   * Poll interval for reading from our queue. Gives us a chance to log idleness when debugging as
   * well as to check the stopped flag periodically.
   */
  private static final Duration ReadTimeout = Duration.ofMillis(500);

  /**
   * A fairly arbitrary but high limit. Should never be reached in real life unless our thread has
   * died or isn't draining the queue.
   */
  private static final int MaxQueueSize = 10_000;

  /** Used to create a sink when the thread starts. */
  private final Supplier<RdaSink<TMessage, TClaim>> sinkFactory;
  /** Used to receive sequence numbers from the main thread. */
  private final BlockingQueue<Entry> inputQueue;
  /** Used to report exceptions to the main thread. */
  private final ReportingCallback<TMessage> errorReportingFunction;
  /** Used to tell the thread to stop running. */
  private final AtomicBoolean stopped;

  public SequenceNumberWriterThread(
      Supplier<RdaSink<TMessage, TClaim>> sinkFactory,
      ReportingCallback<TMessage> errorReportingFunction) {
    this.sinkFactory = sinkFactory;
    this.errorReportingFunction = errorReportingFunction;
    inputQueue = new LinkedBlockingQueue<>(MaxQueueSize);
    stopped = new AtomicBoolean(false);
  }

  /**
   * Adds a sequence number to our queue so that it can be written to the database. Does not wait
   * for the number to be written.
   *
   * @param sequenceNumber to be written to the database
   * @throws Exception if adding to the queue fails
   */
  public void add(Long sequenceNumber) throws Exception {
    final Entry entry = new Entry(sequenceNumber);
    addEntryToInputQueue(entry);
  }

  /** Sets the stopped flag to cause the thread to stop once it has reached an idle moment. */
  @Override
  public void close() {
    stopped.set(true);
    LOGGER.info("shutdown requested");
  }

  /**
   * Processes sequence numbers from its work queue until a shutdown is requested or an exception is
   * caught. Exceptions other than InterruptedException are passed back to the main thread using the
   * reportingFunction.
   */
  @Override
  public Integer call() throws InterruptedException {
    LOGGER.info("started");
    try (RdaSink<TMessage, TClaim> sink = sinkFactory.get()) {
      var running = true;
      while (running) {
        running = runOnce(sink);
      }
      LOGGER.info("terminating normally");
    } catch (Exception ex) {
      // this is just a catch all for safety, runOnce() should handle all non-interrupts
      LOGGER.error("terminating due to uncaught exception: ex={}", ex.getMessage(), ex);
      reportError(ex);
    }
    LOGGER.info("stopped");
    return 0;
  }

  /**
   * If the stopped flag has been set all available entries in the queue are processed and then
   * false is returned. Otherwise reads an entry from the queue and processes it. If no entries are
   * available within the allowed wait period nothing is done. Any exception thrown by the database
   * update is reported back via our errorReportingFunction.
   *
   * @param sink RdaSink to use for writing to the database
   * @return true if another iteration is called for, false otherwise
   */
  @VisibleForTesting
  boolean runOnce(RdaSink<TMessage, TClaim> sink) throws InterruptedException {
    boolean keepRunning = true;
    final Entry entry;
    if (stopped.get()) {
      LOGGER.info("shutdown requested");
      keepRunning = false;
      entry = readEntryFromInputQueueWithoutWaiting();
    } else {
      entry = waitForEntryFromInputQueue();
    }
    if (entry != null) {
      try {
        LOGGER.debug("writing sequenceNumber {}", entry.sequenceNumber);
        sink.updateLastSequenceNumber(entry.sequenceNumber);
      } catch (Exception ex) {
        reportError(ex);
        keepRunning = false;
      }
    }
    return keepRunning;
  }

  /**
   * Waits for at least one entry to appear in the queue and returns it. If multiple entries are in
   * the queue they are all removed and only the last one is returned. This is safe because the last
   * one written would overwrite any others anyway.
   *
   * @return the Entry to be written or null if the queue is empty
   */
  @Nullable
  private Entry waitForEntryFromInputQueue() throws InterruptedException {
    Entry entry = inputQueue.poll(ReadTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (entry == null) {
      LOGGER.debug("no objects on input queue within timeout period");
    } else {
      // Consume all available values so we only write the most recent one
      for (var next = inputQueue.peek(); next != null; next = inputQueue.peek()) {
        entry = inputQueue.take();
      }
    }
    return entry;
  }

  /**
   * Reads as many entries from the input queue as possible without waiting. If multiple entries are
   * in the queue they are all removed and only the last one is returned. This is safe because the
   * last one written would overwrite any others anyway.
   *
   * @return the Entry to be written or null if the queue is empty
   */
  @Nullable
  private Entry readEntryFromInputQueueWithoutWaiting() throws InterruptedException {
    Entry entry = null;
    // Consume all available values so we only write the most recent one
    for (var next = inputQueue.peek(); next != null; next = inputQueue.peek()) {
      entry = inputQueue.take();
    }
    return entry;
  }

  /**
   * Attempts to add an entry to our queue. This should only fail if we are interrupted or if our
   * thread is stuck and the queue has become full.
   *
   * @param entry an entry to add to our queue
   * @throws IOException thrown if the add fails
   */
  private void addEntryToInputQueue(Entry entry) throws InterruptedException, IOException {
    final boolean added = inputQueue.offer(entry, AddTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!added) {
      LOGGER.error("unable to add an object to queue within timeout period");
      throw new IOException("timeout exceeded while adding object to input queue");
    }
  }

  private void reportError(Exception error) throws InterruptedException {
    errorReportingFunction.accept(new ProcessedBatch<>(0, Collections.emptyList(), error));
  }

  @AllArgsConstructor
  private static class Entry {
    private final long sequenceNumber;
  }
}
