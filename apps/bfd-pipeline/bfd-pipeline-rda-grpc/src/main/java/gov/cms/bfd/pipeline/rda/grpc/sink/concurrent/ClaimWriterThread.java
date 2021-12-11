package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClaimWriterThread<TMessage, TClaim> implements Runnable, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimWriterThread.class);
  private static final Duration AddTimeout = Duration.ofMinutes(5);
  private static final Duration ReadTimeout = Duration.ofMillis(250);
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
    LOGGER.info("thread started");
    final var allObjects = new ArrayList<TMessage>();
    final var uniqueObjects = new LinkedHashMap<String, TClaim>();
    try (RdaSink<TMessage, TClaim> sink = sinkFactory.get()) {
      var running = true;
      String apiVersion = "unknown";
      while (running) {
        final Entry<TMessage> entry = takeEntryFromInputQueue();
        boolean sendOk;
        if (entry == null) {
          // queue is empty
          sendOk = uniqueObjects.size() >= 1;
        } else if (entry == shutdownValue) {
          running = false;
          sendOk = uniqueObjects.size() >= 1;
          LOGGER.info("shutdown requested");
        } else {
          // add object to buffer and send if buffer is full
          final String objectKey = sink.getDedupKeyForMessage(entry.object);
          allObjects.add(entry.object);
          uniqueObjects.put(objectKey, sink.transformMessage(entry.apiVersion, entry.object));
          apiVersion = entry.apiVersion;
          sendOk = uniqueObjects.size() >= batchSize;
        }
        if (sendOk) {
          writeBatch(sink, allObjects, uniqueObjects, apiVersion);
          uniqueObjects.clear();
          allObjects.clear();
        }
      }
    } catch (Exception ex) {
      LOGGER.error("caught exception: ex={}", ex.getMessage(), ex);
      try {
        reportError(ImmutableList.copyOf(allObjects), ex);
      } catch (InterruptedException ignored) {
        LOGGER.error(
            "unable to report terminal exception to pool: message={}", ex.getMessage(), ex);
      }
    }
    LOGGER.info("thread stopped");
  }

  private void writeBatch(
      RdaSink<TMessage, TClaim> sink,
      ArrayList<TMessage> allObjects,
      LinkedHashMap<String, TClaim> uniqueObjects,
      String apiVersion)
      throws InterruptedException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "writing batch: allObjects={} uniqueObjects={}", allObjects.size(), uniqueObjects.size());
    }
    try {
      final List<TClaim> batch = ImmutableList.copyOf(uniqueObjects.values());
      final var processed = sink.writeClaims(apiVersion, batch);
      reportSuccess(allObjects, processed);
    } catch (Exception ex) {
      reportError(allObjects, ex);
    }
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

  @AllArgsConstructor
  private static class Entry<T> {
    private final String apiVersion;
    private final T object;
  }

  @Value
  public static class ProcessedBatch<T> {
    int processed;
    List<T> batch;
    Exception error;
  }

  public interface ReportingCallback<TMessage> {
    void accept(ProcessedBatch<TMessage> result) throws InterruptedException;
  }
}
