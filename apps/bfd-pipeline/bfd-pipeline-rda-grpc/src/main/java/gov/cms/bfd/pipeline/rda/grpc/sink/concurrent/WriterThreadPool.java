package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Manages a pool of worker threads to write claim and sequence number updates to a database
 * concurrently. The lifecycle of the pool and all of its resources are tied to the lifecycle of
 * this object so calling close() method clears up all resources.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA claim entity class
 */
@Slf4j
public class WriterThreadPool<TMessage, TClaim> implements AutoCloseable {
  /** Used to assign claims to workers based on their claimId values. */
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;
  /** Used to perform database i/o. */
  private final RdaSink<TMessage, TClaim> sink;
  /** Our thread pool used to execute writers. */
  private final ExecutorService threadPool;
  /** Used to receive messages from main thread. */
  private final BlockingQueue<ReportingCallback.ProcessedBatch<TMessage>> outputQueue;
  /** Used to update sequence number in background. */
  private final SequenceNumberWriterThread<TMessage, TClaim> sequenceNumberWriter;
  /** Our collection of claim writers. */
  private final List<ClaimWriterThread<TMessage, TClaim>> writers;

  /**
   * Create new instance with the provided configuration.
   *
   * @param maxThreads size of thread pool
   * @param batchSize number of claims per batch
   * @param sinkFactory factory to create {@link RdaSink} instances
   */
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
      log.info("added writer writer: sink={} writer={}", sink.getClass().getSimpleName(), writer);
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
    final String key = sink.getClaimIdForMessage(object);
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

  /**
   * The primary key for the claim contained in the message. Used by callers to remove duplicates
   * from a collection of objects prior to calling writeMessages. Callers may log this value so it
   * must not contain any PII or PHI.
   *
   * @param object object to get a key from
   * @return a unique key to dedup objects of this type
   */
  public String getClaimIdForMessage(TMessage object) {
    return sink.getClaimIdForMessage(object);
  }

  /**
   * Extract the sequence number from the message object and return it.
   *
   * @param object object to get the sequence number from
   * @return the sequence number within the message object
   */
  public long getSequenceNumberForObject(TMessage object) {
    return sink.getSequenceNumberForObject(object);
  }

  /**
   * Checks if the error limit has been exceeded.
   *
   * @throws ProcessingException If the error limit was reached.
   */
  public void checkErrorCount() throws ProcessingException {
    sink.checkErrorCount();
  }

  /**
   * Use the provided RDA API message object plus the API version string to produce an appropriate
   * entity object for writing to the database. This operation is provided by the sink because the
   * sink has to be aware of the specific types involved and also because that allows the message
   * transformation to be performed in worker threads rather than in the main thread.
   *
   * @param apiVersion appropriate string for the apiSource column of the claim table
   * @param message an RDA API message object of the correct type for this sync
   * @return an optional containing the appropriate entity object containing the data from the
   *     message if successfully converted, {@link Optional#empty()} otherwise
   * @throws IOException If there was an issue writing out a {@link DataTransformer.ErrorMessage}
   * @throws ProcessingException If there was an issue transforming the message
   */
  @Nonnull
  public Optional<TClaim> transformMessage(String apiVersion, TMessage message)
      throws IOException, ProcessingException {
    return sink.transformMessage(apiVersion, message);
  }

  /**
   * The pipeline job passes a starting sequence number to the RDA API to get a stream of change
   * objects for processing. This method allows the sink to provide the next logical starting
   * sequence number for the call. An Optional is returned to handle the case when no records have
   * been added to the database yet.
   *
   * @return Possibly empty Optional containing highest recorded sequence number.
   * @throws ProcessingException if the operation fails
   */
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return sink.readMaxExistingSequenceNumber();
  }

  /**
   * Schedules the current sequence number to be written to the database by a worker thread.
   *
   * @throws ProcessingException if scheduled the write fails
   */
  public void updateSequenceNumbers() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getSafeResumeSequenceNumber();
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
   * @throws Exception if the closer encounters an issue
   */
  public void shutdown(Duration waitTime) throws Exception {
    log.info("shutdown started");
    final MultiCloser closer = new MultiCloser();
    writers.forEach(
        writer -> {
          log.info("calling claimWriterThread.close: writer={}", writer);
          closer.close(() -> writer.close());
        });
    log.info("calling sequenceNumberWriterThread.close");
    closer.close(sequenceNumberWriter::close);
    log.info("calling threadPool.shutdown");
    closer.close(threadPool::shutdown);
    closer.close(
        () -> {
          log.info("calling threadPool.awaitTermination");
          awaitThreadPoolTermination(waitTime);
        });
    log.info("calling updateSequenceNumberDirectly");
    closer.close(this::updateSequenceNumberDirectly);
    log.info("shutdown complete");
    closer.finish();
  }

  /**
   * Wait for thread pool to terminate.
   *
   * @param waitTime maximum time to wait
   * @throws InterruptedException if wait is interrupted
   * @throws IOException if pool never terminates
   */
  private void awaitThreadPoolTermination(Duration waitTime)
      throws InterruptedException, IOException {
    boolean successful;
    try {
      successful = threadPool.awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      log.info("interrupted while waiting for thread pool termination, retrying once...");
      successful = threadPool.awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (!successful) {
      throw new IOException("threadPool did not shut down");
    }
  }

  @Override
  public void close() throws Exception {
    final MultiCloser closer = new MultiCloser();
    if (!threadPool.isShutdown()) {
      closer.close(() -> shutdown(Duration.ofMinutes(5)));
    }
    log.info("calling this.updateSequenceNumberForClose");
    closer.close(this::updateSequenceNumberForClose);
    log.info("calling sink.close");
    closer.close(sink::close);
    log.info("close complete");
    closer.finish();
  }

  /**
   * Updates the sequence number in the database to the current safe value. Write takes place in the
   * caller's thread.
   *
   * @throws ProcessingException if the write false
   */
  private void updateSequenceNumberDirectly() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getSafeResumeSequenceNumber();
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
      log.warn("uncollected final processedCount: {}", count);
    }
    updateSequenceNumberDirectly();
  }
}
