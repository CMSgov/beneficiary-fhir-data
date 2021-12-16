package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a pool of worker threads to write claim and sequence number updates to a database
 * concurrently. The lifecycle of the pool and all of its resources are tied to the lifecycle of
 * this object so calling close() method clears up all resources.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA claim entity class
 */
public class WriterThreadPool<TMessage, TClaim> implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriterThreadPool.class);
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  private final SequenceNumberTracker sequenceNumbers;
  private final RdaSink<TMessage, TClaim> sink;
  private final ExecutorService threadPool;
  private final BlockingQueue<ReportingCallback.ProcessedBatch<TMessage>> outputQueue;
  private final SequenceNumberWriterThread<TMessage, TClaim> sequenceNumberWriter;
  private final List<ClaimWriterThread<TMessage, TClaim>> writers;

  public WriterThreadPool(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    Preconditions.checkArgument(maxThreads > 0);
    Preconditions.checkArgument(batchSize > 0);
    sequenceNumbers = new SequenceNumberTracker(0);
    sink = sinkFactory.get();
    threadPool =
        Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat(sink.getClass().getSimpleName() + "-thread-%d")
                .build());
    outputQueue = new LinkedBlockingQueue<>();
    sequenceNumberWriter = new SequenceNumberWriterThread<>(sinkFactory, outputQueue::put);
    threadPool.submit(sequenceNumberWriter);
    var writers = ImmutableList.<ClaimWriterThread<TMessage, TClaim>>builder();
    for (int i = 1; i <= maxThreads; ++i) {
      var writer = new ClaimWriterThread<>(sinkFactory, batchSize, outputQueue::put);
      writers.add(writer);
      threadPool.submit(writer);
      LOGGER.info(
          "added writer writer: sink={} writer={}", sink.getClass().getSimpleName(), writer);
    }
    this.writers = writers.build();
  }

  /**
   * Adds a single object to queue for storage. This will not cause the object to be written
   * immediately nor will it wait for it to be written. Objects are assigned to workers based on
   * their claim ids (value returned by {@code RdaSink.getDedupKeyForMessage}) so that all updates
   * for any given claim are sequential. Updates for unrelated claims can happen in any order.
   *
   * @param apiVersion version string for this batch
   * @param object object to be enqueued
   * @throws Exception if adding to queue fails
   */
  public void addToQueue(String apiVersion, TMessage object) throws Exception {
    sequenceNumbers.addActiveSequenceNumber(sink.getSequenceNumberForObject(object));
    final String key = sink.getDedupKeyForMessage(object);
    final int hash = Hasher.hashString(key, StandardCharsets.UTF_8).asInt();
    final int writerIndex = Math.abs(hash) % writers.size();
    writers.get(writerIndex).add(apiVersion, object);
  }

  /**
   * Scan the output from our threads to accumulate the current processed count. Any errors reported
   * by threads are combined into a single ProcessingException. Total count is returned. Each call
   * to this method reports only the most recently accumulated results. It is safe to call this
   * method after close has been called to get a final count.
   *
   * @return count of objects written since last call to this method
   * @throws ProcessingException if any thread encountered an error
   */
  public int getProcessedCount() throws ProcessingException {
    var count = 0;
    Exception error = null;
    var results = new ArrayList<ReportingCallback.ProcessedBatch<TMessage>>();
    outputQueue.drainTo(results);
    for (ReportingCallback.ProcessedBatch<TMessage> result : results) {
      count += result.getProcessed();
      if (result.getError() == null) {
        // batch was successfully written so mark sequence numbers as processed
        for (TMessage object : result.getBatch()) {
          long sequenceNumber = sink.getSequenceNumberForObject(object);
          sequenceNumbers.removeWrittenSequenceNumber(sequenceNumber);
        }
      } else if (error != null) {
        error.addSuppressed(result.getError());
      } else {
        error = result.getError();
      }
    }
    if (error != null) {
      throw new ProcessingException(error, count);
    }
    return count;
  }

  public String getDedupKeyForMessage(TMessage object) {
    return sink.getDedupKeyForMessage(object);
  }

  public long getSequenceNumberForObject(TMessage object) {
    return sink.getSequenceNumberForObject(object);
  }

  @Nonnull
  public TClaim transformMessage(String apiVersion, TMessage message) {
    return sink.transformMessage(apiVersion, message);
  }

  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return sink.readMaxExistingSequenceNumber();
  }

  /**
   * Schedules the current sequence number to be written to the database by a worker thread.
   *
   * @throws ProcessingException if scheduled the write fails
   */
  public void updateSequenceNumbers() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getHighestWrittenSequenceNumber();
    if (sequenceNumber > 0) {
      try {
        sequenceNumberWriter.add(sequenceNumber);
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }
  }

  /**
   * Cleanly shuts down our thread pool. Any queued data will be written before this method returns.
   *
   * @param waitTime maximum amount of time to wait for the shutdown to complete
   */
  public void shutdown(Duration waitTime) throws Exception {
    LOGGER.info("shutdown started");
    final MultiCloser closer = new MultiCloser();
    writers.forEach(
        writer -> {
          LOGGER.info("calling claimWriterThread.close: writer={}", writer);
          closer.close(() -> writer.close());
        });
    LOGGER.info("calling sequenceNumberWriterThread.close");
    closer.close(sequenceNumberWriter::close);
    LOGGER.info("calling threadPool.shutdown");
    closer.close(threadPool::shutdown);
    closer.close(
        () -> {
          LOGGER.info("calling threadPool.awaitTermination");
          boolean successful =
              threadPool.awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
          if (!successful) {
            throw new IOException("threadPool did not shut down");
          }
        });
    LOGGER.info("calling updateSequenceNumberDirectly");
    closer.close(this::updateSequenceNumberDirectly);
    LOGGER.info("shutdown complete");
    closer.finish();
  }

  @Override
  public void close() throws Exception {
    final MultiCloser closer = new MultiCloser();
    if (!threadPool.isShutdown()) {
      closer.close(() -> shutdown(Duration.ofMinutes(5)));
    }
    LOGGER.info("calling this.updateSequenceNumberForClose");
    closer.close(this::updateSequenceNumberForClose);
    LOGGER.info("calling sink.close");
    closer.close(sink::close);
    LOGGER.info("close complete");
    closer.finish();
  }

  private void updateSequenceNumberDirectly() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getHighestWrittenSequenceNumber();
    if (sequenceNumber > 0) {
      try {
        sink.updateLastSequenceNumber(sequenceNumber);
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }
  }

  /**
   * Flushes the outputQueue to determine the final sequence number and processed count. Then is
   * writes the final sequence number to the database.
   *
   * <p>If the processed count is non-zero this method logs a warning. If that happens it's not a
   * serious issue since in the worst case it just means the pipeline job will reprocess a few
   * records the next time it starts but it is worth noting in the log.
   */
  private void updateSequenceNumberForClose() throws Exception {
    int count = getProcessedCount();
    if (count != 0) {
      LOGGER.warn("uncollected final processedCount: {}", count);
    }
    updateSequenceNumberDirectly();
  }
}
