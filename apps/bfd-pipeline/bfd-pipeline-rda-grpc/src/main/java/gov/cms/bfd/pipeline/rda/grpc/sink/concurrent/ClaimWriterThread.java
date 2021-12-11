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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimWriterThread<TMessage, TClaim> implements Runnable, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimWriterThread.class);
  private static final Duration AddTimeout = Duration.ofMinutes(5);
  private static final Duration ReadTimeout = Duration.ofMillis(500);
  private static final int InputQueuePaddingMultiple = 2;

  private final Supplier<RdaSink<TMessage, TClaim>> sinkFactory;
  private final Entry<TMessage> shutdownValue;
  private final int batchSize;
  private final BlockingQueue<Entry<TMessage>> inputQueue;
  private final ReportingCallback<TMessage> reportingFunction;

  public ClaimWriterThread(
      Supplier<RdaSink<TMessage, TClaim>> sinkFactory,
      int batchSize,
      ReportingCallback<TMessage> reportingFunction) {
    this.sinkFactory = sinkFactory;
    this.batchSize = batchSize;
    this.reportingFunction = reportingFunction;
    inputQueue = new ArrayBlockingQueue<>(InputQueuePaddingMultiple * batchSize);
    shutdownValue = new Entry<>("", null);
  }

  public void add(String apiVersion, TMessage object) throws Exception {
    final Entry<TMessage> entry = new Entry<>(apiVersion, object);
    addEntryToInputQueue(entry);
  }

  @Override
  public void close() throws Exception {
    addEntryToInputQueue(shutdownValue);
  }

  @Override
  public void run() {
    LOGGER.info("started");
    try (RdaSink<TMessage, TClaim> sink = sinkFactory.get()) {
      final Buffer<TMessage, TClaim> buffer = new Buffer<>();
      var running = true;
      while (running) {
        running = runOnce(sink, buffer);
      }
      LOGGER.info("terminating normally");
    } catch (InterruptedException ex) {
      LOGGER.info("terminating due to InterruptedException");
    } catch (Exception ex) {
      // this is just a catch all for safety, runOnce() should handle all non-interrupts
      LOGGER.error("terminating due to uncaught exception: ex={}", ex.getMessage(), ex);
      reportErrorWhenClosing(ex);
    }
    LOGGER.info("stopped");
  }

  /**
   * Reads one entry from the queue and processes it. Adds entries to a list and trackers unique
   * entries. Once a full batch has been detected it will be written to the database and the list
   * and map reset. Any exception thrown by the database update is * reported back via our
   * errorReportingFunction.
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
      } else if (entry == shutdownValue) {
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

  private void reportErrorWhenClosing(Exception error) {
    try {
      reportError(Collections.emptyList(), error);
    } catch (InterruptedException ex) {
      LOGGER.error("unable to report final error due to InterruptedException", ex);
    }
  }

  @AllArgsConstructor
  private static class Entry<T> {
    private final String apiVersion;
    private final T object;
  }

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
