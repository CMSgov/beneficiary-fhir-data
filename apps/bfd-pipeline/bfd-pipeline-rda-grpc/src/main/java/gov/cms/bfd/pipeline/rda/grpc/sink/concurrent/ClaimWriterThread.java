package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ReportingCallback.ProcessedBatch;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Callable object submitted to an ExecutorService to process RDA API messages and write them to
 * the database using an RdaSink object. Each thread has its own BlockingQueue to receive inbound
 * messages for processing and uses a callback function to report its successes and failures.
 *
 * <p>The thread is stopped by calling its close() method. This adds a special token to the work
 * queue that tells the thread to flush its buffer and exit its run loop.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA entity class
 */
public class ClaimWriterThread<TMessage, TClaim> implements Callable<Integer>, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimWriterThread.class);

  /**
   * Max time the main thread can be blocked if our queue is full. This would only happen if our
   * thread has become stuck and is not draining the queue.
   */
  private static final Duration AddTimeout = Duration.ofMinutes(5);

  /**
   * Poll interval for reading from our queue. Not strictly necessary but allows us to log that
   * we're still alive when debugging.
   */
  private static final Duration ReadTimeout = Duration.ofMillis(500);

  /**
   * Extra padding added to the queue's maximum size. This padding allows the main thread to
   * continue adding messages to the queue while the thread is busy writing a batch to the database.
   */
  private static final int InputQueuePaddingMultiple = 4;

  /** Used to create a sink when the thread starts. */
  private final Supplier<RdaSink<TMessage, TClaim>> sinkFactory;
  /**
   * Used as a marker to cause the thread to shutdown. Comparison uses == (identity) so the actual
   * value is irrelevant. This isn't static since the entry depends on the TMessage type.
   */
  private final Entry<TMessage> shutdownToken;
  /** Number of claims to write to the database in a single transaction. */
  private final int batchSize;
  /** Used to receive messages from the main thread. */
  private final BlockingQueue<Entry<TMessage>> inputQueue;
  /** Used to report results to the main thread. */
  private final ReportingCallback<TMessage> reportingFunction;

  public ClaimWriterThread(
      Supplier<RdaSink<TMessage, TClaim>> sinkFactory,
      int batchSize,
      ReportingCallback<TMessage> reportingFunction) {
    this.sinkFactory = sinkFactory;
    this.batchSize = batchSize;
    this.reportingFunction = reportingFunction;
    inputQueue = new ArrayBlockingQueue<>(InputQueuePaddingMultiple * batchSize);
    shutdownToken = new Entry<>("", null);
  }

  /**
   * Adds an entry for this specified object to the work queue. Transformation of the message into a
   * claim will be done by the worker thread when it processes the entry.
   *
   * @param apiVersion value for the claim's apiSource column
   * @param object an RDA API message object
   * @throws Exception thrown if the entry could not be added to the queue
   */
  public void add(String apiVersion, TMessage object) throws Exception {
    final Entry<TMessage> entry = new Entry<>(apiVersion, object);
    addEntryToInputQueue(entry);
  }

  /**
   * Adds a shutdown token to the work queue to trigger a shutdown. Does not actually wait for the
   * shutdown to happen since the caller needs to trigger all threads to shutdown before waiting for
   * them to complete.
   *
   * @throws Exception thrown if the token could not be added to the queue
   */
  @Override
  public void close() throws Exception {
    try {
      addEntryToInputQueue(shutdownToken);
    } catch (InterruptedException ignored) {
      // During shutdown we might encounter an InterruptedException.
      // We can retry immediately.  The exception being thrown would have cleared the interrupted
      // status so that this can succeed on the second call.
      LOGGER.info("retrying add shutdownToken after an InterruptedException");
      addEntryToInputQueue(shutdownToken);
    }
  }

  /**
   * Processes entries from its work queue until a shutdown is requested or an exception is caught.
   * Exceptions other than InterruptedException are passed back to the main thread using the
   * reportingFunction.
   */
  @Override
  public Integer call() throws InterruptedException {
    LOGGER.info("started");
    try (RdaSink<TMessage, TClaim> sink = sinkFactory.get()) {
      final Buffer<TMessage, TClaim> buffer = new Buffer<>();
      var running = true;
      while (running) {
        running = runOnce(sink, buffer);
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
   * Reads one entry from the queue and processes it. Adds entries to a buffer. Once a full batch
   * has been detected it will be written to the database and the list and map reset. Any exception
   * thrown by the database update is * reported back via our errorReportingFunction.
   *
   * @param sink RdaSink to use for writing to the database
   * @return true if another iteration is called for, false otherwise
   */
  @VisibleForTesting
  boolean runOnce(RdaSink<TMessage, TClaim> sink, Buffer<TMessage, TClaim> buffer)
      throws InterruptedException {
    boolean keepRunning = true;
    try {
      final Entry<TMessage> entry = takeEntryFromInputQueue();
      boolean writeNeeded;
      if (entry == null) {
        // queue is empty so flush any received claims to reduce latency
        writeNeeded = buffer.getUniqueCount() >= 1;
      } else if (entry == shutdownToken) {
        // shutdown requested so flush any received claims and exit
        LOGGER.info("shutdown requested");
        keepRunning = false;
        writeNeeded = buffer.getUniqueCount() >= 1;
      } else {
        // add message and claim to buffer and write if buffer is full
        buffer.add(sink, entry);
        writeNeeded = buffer.getUniqueCount() >= batchSize;
      }
      if (writeNeeded) {
        writeBatch(sink, buffer);
        buffer.clear();
      }
    } catch (InterruptedException ex) {
      throw ex;
    } catch (Exception ex) {
      LOGGER.error("caught exception: ex={}", ex.getMessage(), ex);
      reportError(buffer.getMessages(), ex);
      keepRunning = false;
    }
    return keepRunning;
  }

  /**
   * Writes all unique objects from the buffer to the database and reports the results using the
   * reporting function. Any exceptions will be caught by our caller and reported as errors using
   * the reporting function.
   */
  private void writeBatch(RdaSink<TMessage, TClaim> sink, Buffer<TMessage, TClaim> buffer)
      throws Exception {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "writing batch: allObjects={} uniqueObjects={}",
          buffer.getFullCount(),
          buffer.getUniqueCount());
    }
    final List<TClaim> batch = buffer.getClaims();
    final var processed = sink.writeClaims(batch);
    reportSuccess(buffer.getMessages(), processed);
  }

  @Nullable
  private Entry<TMessage> takeEntryFromInputQueue() throws InterruptedException {
    final Entry<TMessage> entry = inputQueue.poll(ReadTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (entry == null) {
      LOGGER.debug("no objects on input queue within timeout period");
    }
    return entry;
  }

  private void addEntryToInputQueue(Entry<TMessage> entry)
      throws InterruptedException, IOException {
    final boolean added = inputQueue.offer(entry, AddTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!added) {
      LOGGER.error("unable to add an object to queue within timeout period");
      throw new IOException("timeout exceeded while adding object to input queue");
    }
  }

  private void reportSuccess(List<TMessage> batch, int processed) throws InterruptedException {
    reportingFunction.accept(new ProcessedBatch<>(processed, ImmutableList.copyOf(batch), null));
  }

  private void reportError(List<TMessage> batch, Exception error) throws InterruptedException {
    reportingFunction.accept(new ProcessedBatch<>(0, ImmutableList.copyOf(batch), error));
  }

  private void reportError(Exception error) throws InterruptedException {
    reportError(Collections.emptyList(), error);
  }

  /**
   * Unit of work added to the input queue. Contains all of the information required by the thread
   * to transform an RDA API message into a JPA entity for writing to the database.
   *
   * @param <TMessage> the RDA API message class
   */
  @AllArgsConstructor
  private static class Entry<TMessage> {
    private final String apiVersion;
    private final TMessage object;
  }

  /**
   * A buffer used to accumulate messages and their associated claims until we have a complete
   * batch. For any given claim ID only the last version of that claim will be written as part of a
   * batch. This class is only used by the worker thread so it does not need to be thread safe.
   */
  @NotThreadSafe
  @VisibleForTesting
  static class Buffer<TMessage, TClaim> {
    private final List<TMessage> allMessages = new ArrayList<>();
    private final Map<String, TClaim> uniqueClaims = new LinkedHashMap<>();

    void add(RdaSink<TMessage, TClaim> sink, Entry<TMessage> entry) {
      final String claimKey = sink.getDedupKeyForMessage(entry.object);
      final TClaim claim = sink.transformMessage(entry.apiVersion, entry.object);
      allMessages.add(entry.object);
      uniqueClaims.put(claimKey, claim);
    }

    void clear() {
      allMessages.clear();
      uniqueClaims.clear();
    }

    int getFullCount() {
      return allMessages.size();
    }

    int getUniqueCount() {
      return uniqueClaims.size();
    }

    List<TMessage> getMessages() {
      return ImmutableList.copyOf(allMessages);
    }

    List<TClaim> getClaims() {
      return ImmutableList.copyOf(uniqueClaims.values());
    }
  }
}
