package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ReportingCallback.ProcessedBatch;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Callable object submitted to an ExecutorService to process RDA API messages and write them to
 * the database using an RdaSink object. Each thread has its own BlockingQueue to receive inbound
 * messages for processing and uses a callback function to report its successes and failures.
 *
 * <p>The thread is stopped by calling its close() method. This sets a flag that causes the thread
 * to drain its queue and shut down.
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
   * Poll interval for reading from our queue. Gives us a chance to log idleness when debugging as
   * well as to check the stopped flag periodically.
   */
  private static final Duration ReadTimeout = Duration.ofMillis(500);

  /**
   * Extra padding added to the queue's maximum size. This padding allows the main thread to
   * continue adding messages to the queue while the thread is busy writing a batch to the database.
   */
  private static final int InputQueuePaddingMultiple = 4;

  /** Used to create a sink when the thread starts. */
  private final Supplier<RdaSink<TMessage, TClaim>> sinkFactory;
  /** Number of claims to write to the database in a single transaction. */
  private final int batchSize;
  /** Used to receive messages from the main thread. */
  private final BlockingQueue<Entry<TMessage>> inputQueue;
  /** Used to report results to the main thread. */
  private final ReportingCallback<TMessage> reportingFunction;
  /** Used to tell the thread to stop running. */
  private final AtomicBoolean stopped;

  /**
   * Instantiates a new claim writer thread.
   *
   * @param sinkFactory the sink factory
   * @param batchSize the number of claims to write to the database in a single transaction
   * @param reportingFunction the reporting function to report results to the main thread
   */
  public ClaimWriterThread(
      Supplier<RdaSink<TMessage, TClaim>> sinkFactory,
      int batchSize,
      ReportingCallback<TMessage> reportingFunction) {
    this.sinkFactory = sinkFactory;
    this.batchSize = batchSize;
    this.reportingFunction = reportingFunction;
    inputQueue = new ArrayBlockingQueue<>(InputQueuePaddingMultiple * batchSize);
    stopped = new AtomicBoolean(false);
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

  /** Sets the stopped flag to cause the thread to stop once it has reached an idle moment. */
  @Override
  public void close() {
    stopped.set(true);
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
    drainQueueUntilStoppedFlagIsSet();
    LOGGER.info("stopped");
    return 0;
  }

  /**
   * Reads one entry from the queue and processes it. Adds entries to a buffer. Once a full batch
   * has been detected it will be written to the database and the list and map reset. Any exception
   * thrown by the database update is * reported back via our errorReportingFunction. If shutdown
   * has been requested the buffer is flushed and false is returned to stop the thread.
   *
   * @param sink RdaSink to use for writing to the database
   * @param buffer the buffer
   * @return true if another iteration is called for, false otherwise
   * @throws InterruptedException the interrupted exception
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
        if (stopped.get()) {
          LOGGER.info("shutdown requested");
          keepRunning = false;
        }
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
   * Ensure that our {@link WriterThreadPool} does not block trying to add records to our queue
   * after we shut down. This eliminates a possible race condition where we report an error to the
   * {@link WriterThreadPool} but it keeps adding messages to our queue for a while before it sees
   * the reported error. If we just exit immediately our queue might fill and prevent the {@link
   * WriterThreadPool} from seeing the error. Since we are no longer processing any messages we can
   * simply ignore any that are added to the queue.
   */
  @VisibleForTesting
  void drainQueueUntilStoppedFlagIsSet() throws InterruptedException {
    if (!stopped.get()) {
      LOGGER.info("waiting for stop signal");
      do {
        takeEntryFromInputQueue();
      } while (!stopped.get());
    }
  }

  /**
   * Writes all unique objects from the buffer to the database and reports the results using the
   * reporting function. Any exceptions will be caught by our caller and reported as errors using
   * the reporting function.
   *
   * @param sink the sink
   * @param buffer the buffer
   * @throws Exception if the sink cannot write the claims or there is an issue reporting the
   *     batches
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

  /**
   * Takes an entry from the input queue entry.
   *
   * @return the entry from the queue
   * @throws InterruptedException if the queue is interrupted during operations
   */
  @Nullable
  private Entry<TMessage> takeEntryFromInputQueue() throws InterruptedException {
    final Entry<TMessage> entry;
    if (stopped.get()) {
      // Once close has been called we just process objects already in the queue until
      // we've drained the queue but never wait for any new ones to arrive before stopping.
      entry = inputQueue.poll();
    } else {
      entry = inputQueue.poll(ReadTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (entry == null) {
      LOGGER.debug("no objects on input queue within timeout period");
    }
    return entry;
  }

  /**
   * Adds an entry to the input queue.
   *
   * @param entry the entry to add
   * @throws InterruptedException if the queue is interrupted during operations
   * @throws IOException if the item could not be added to the queue (timeout)
   */
  private void addEntryToInputQueue(Entry<TMessage> entry)
      throws InterruptedException, IOException {
    final boolean added = inputQueue.offer(entry, AddTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!added) {
      LOGGER.error("unable to add an object to queue within timeout period");
      throw new IOException("timeout exceeded while adding object to input queue");
    }
  }

  /**
   * Reports a number of successful processed batches.
   *
   * @param batch the batch that was processed
   * @param processed the number of processed items
   * @throws InterruptedException if the reporting process is interrupted
   */
  private void reportSuccess(List<TMessage> batch, int processed) throws InterruptedException {
    reportingFunction.accept(new ProcessedBatch<>(processed, ImmutableList.copyOf(batch), null));
  }

  /**
   * Reports an error while processing batches.
   *
   * @param batch the batch that had the error
   * @param error the error itself
   * @throws InterruptedException if the reporting process is interrupted
   */
  private void reportError(List<TMessage> batch, Exception error) throws InterruptedException {
    reportingFunction.accept(new ProcessedBatch<>(0, ImmutableList.copyOf(batch), error));
  }

  /**
   * Reports an error while processing batches.
   *
   * @param error the error
   * @throws InterruptedException if the reporting process is interrupted
   */
  private void reportError(Exception error) throws InterruptedException {
    reportError(Collections.emptyList(), error);
  }

  /**
   * Unit of work added to the input queue. Contains all of the information required by the thread
   * to transform an RDA API message into a JPA entity for writing to the database.
   *
   * @param <TMessage> the RDA API message class
   */
  @Data
  @VisibleForTesting
  static class Entry<TMessage> {
    /** The API version for this entry. */
    private final String apiVersion;
    /** The message for this entry. */
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
    /** Holds all the messages in this buffer. */
    private final List<TMessage> allMessages = new ArrayList<>();
    /** A map of all the unique claims added to the buffer. */
    private final Map<String, TClaim> uniqueClaims = new LinkedHashMap<>();

    /**
     * Add a claim to the buffer.
     *
     * @param sink used to transform message into claim
     * @param entry holds apiVersion and message
     * @throws IOException if there was an issue writing out a {@link MessageError}.
     * @throws ProcessingException if there was an issue transforming the message
     */
    void add(RdaSink<TMessage, TClaim> sink, Entry<TMessage> entry)
        throws IOException, ProcessingException {
      final String claimKey = sink.getClaimIdForMessage(entry.getObject());
      final Optional<TClaim> claim =
          sink.transformMessage(entry.getApiVersion(), entry.getObject());

      if (claim.isPresent()) {
        allMessages.add(entry.getObject());
        uniqueClaims.put(claimKey, claim.get());
      }
    }

    /** Removes all items the buffer (messages and claim map). */
    void clear() {
      allMessages.clear();
      uniqueClaims.clear();
    }

    /**
     * Gets the number of messages in the buffer.
     *
     * @return the number of messages
     */
    int getFullCount() {
      return allMessages.size();
    }

    /**
     * Gets the number of unique claims in the buffer.
     *
     * @return the unique claim count
     */
    int getUniqueCount() {
      return uniqueClaims.size();
    }

    /**
     * Gets an immutable copy of the list of buffer messages.
     *
     * @return the message list
     */
    List<TMessage> getMessages() {
      return ImmutableList.copyOf(allMessages);
    }

    /**
     * Gets an immutable copy of the map of unique claims in the buffer.
     *
     * @return the unique claims map
     */
    List<TClaim> getClaims() {
      return ImmutableList.copyOf(uniqueClaims.values());
    }
  }
}
