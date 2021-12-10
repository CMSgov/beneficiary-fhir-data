package gov.cms.bfd.pipeline.rda.grpc.sink;

import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceNumberWriterThread<TMessage, TClaim> implements Runnable, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimWriterThread.class);
  private static final Duration AddTimeout = Duration.ofMinutes(5);
  private static final Duration ReadTimeout = Duration.ofMillis(250);
  private static final int InputQueueSize = 1000;

  private final Supplier<RdaSink<TMessage, TClaim>> sinkFactory;
  private final Entry shutdownValue;
  private final BlockingQueue<Entry> inputQueue;
  private final ClaimWriterThread.ReportingCallback<TMessage> errorReportingFunction;

  public SequenceNumberWriterThread(
      Supplier<RdaSink<TMessage, TClaim>> sinkFactory,
      ClaimWriterThread.ReportingCallback<TMessage> errorReportingFunction) {
    this.sinkFactory = sinkFactory;
    this.errorReportingFunction = errorReportingFunction;
    inputQueue = new ArrayBlockingQueue<>(InputQueueSize);
    shutdownValue = new Entry(0L);
  }

  public void add(Long sequenceNumber) throws Exception {
    final Entry entry = new Entry(sequenceNumber);
    addEntryToInputQueue(entry);
  }

  @Override
  public void close() throws Exception {
    addEntryToInputQueue(shutdownValue);
  }

  @Override
  public void run() {
    LOGGER.info("thread started");
    try (RdaSink<TMessage, TClaim> sink = sinkFactory.get()) {
      var running = true;
      while (running) {
        final Entry entry = waitForEntryFromInputQueue();
        if (entry == shutdownValue) {
          running = false;
          LOGGER.info("shutdown requested");
        } else if (entry != null) {
          writeSequenceNumber(sink, entry.sequenceNumber);
        }
      }
    } catch (Exception ex) {
      LOGGER.error("caught exception: ex={}", ex.getMessage(), ex);
    }
    LOGGER.info("thread stopped");
  }

  private void writeSequenceNumber(RdaSink<TMessage, TClaim> sink, long sequenceNumber)
      throws InterruptedException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("writing sequenceNumber {}", sequenceNumber);
    }
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("writing sequenceNumber {}", sequenceNumber);
      }
      sink.updateLastSequenceNumber(sequenceNumber);
    } catch (Exception ex) {
      errorReportingFunction.accept(
          new ClaimWriterThread.ProcessedBatch<>(0, Collections.emptyList(), ex));
    }
  }

  @Nullable
  private Entry waitForEntryFromInputQueue() throws InterruptedException {
    Entry entry = inputQueue.poll(ReadTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (entry == null) {
      LOGGER.debug("no objects on input queue within timeout period");
    } else {
      // Grab any more entries already in the queue so we can maximize the impact of each write.
      Entry nextEntry = inputQueue.poll(1, TimeUnit.MILLISECONDS);
      while (nextEntry != null) {
        entry = nextEntry;
        nextEntry = inputQueue.poll(1, TimeUnit.MILLISECONDS);
      }
    }
    return entry;
  }

  private void addEntryToInputQueue(Entry entry) throws InterruptedException, IOException {
    final boolean added = inputQueue.offer(entry, AddTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!added) {
      LOGGER.error("unable to add an object to queue within timeout period");
      throw new IOException("timeout exceeded while adding object to input queue");
    }
  }

  @AllArgsConstructor
  private static class Entry {
    private final long sequenceNumber;
  }
}
