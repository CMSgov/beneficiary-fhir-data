package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriterThreadPool<TMessage, TClaim> implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriterThreadPool.class);
  private static final HashFunction Hasher = Hashing.goodFastHash(32);

  private final SequenceNumberTracker sequenceNumbers;
  private final RdaSink<TMessage, TClaim> sink;
  private final ExecutorService threadPool;
  private final BlockingQueue<ClaimWriterThread.ProcessedBatch<TMessage>> outputQueue;
  private final SequenceNumberWriterThread<TMessage, TClaim> sequenceNumberWriter;
  private final List<ClaimWriterThread<TMessage, TClaim>> writers;

  public WriterThreadPool(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
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
    writers = new ArrayList<>(maxThreads);
    for (int i = 1; i <= maxThreads; ++i) {
      var writer = new ClaimWriterThread<>(sinkFactory, batchSize, outputQueue::put);
      writers.add(writer);
      threadPool.submit(writer);
    }
  }

  /**
   * Adds a single object to queue for storage. This will not cause the object to be written
   * immediately nor will it wait for it to be written. Objects are assigned to workers based on
   * their claim ids (value returned by {@code RdaSink.getDedupKeyForMessage}) so that all updates
   * for any given claim are sequential. Updates for unrelated claims can happen in any order.
   *
   * @param apiVersion version string for this batch
   * @param objects object to be enqueued
   * @throws Exception if adding to queue fails
   */
  public void addToQueue(String apiVersion, TMessage object) throws Exception {
    sequenceNumbers.addSequenceNumber(sink.getSequenceNumberForObject(object));
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
    var results = new ArrayList<ClaimWriterThread.ProcessedBatch<TMessage>>();
    outputQueue.drainTo(results);
    for (ClaimWriterThread.ProcessedBatch<TMessage> result : results) {
      count += result.getProcessed();
      if (result.getError() == null) {
        // batch was successfully written so mark sequence numbers as processed
        for (TMessage object : result.getBatch()) {
          long sequenceNumber = sink.getSequenceNumberForObject(object);
          sequenceNumbers.removeSequenceNumber(sequenceNumber);
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

  public RdaSink<TMessage, TClaim> getSink() {
    return sink;
  }

  /**
   * Schedules the current sequence number to be written to the database by a worker thread.
   *
   * @throws ProcessingException
   */
  public void updateSequenceNumbers() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getNextSequenceNumber();
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
    for (ClaimWriterThread<TMessage, TClaim> writer : writers) {
      closer.close(writer::close);
    }
    closer.close(sequenceNumberWriter::close);
    closer.close(threadPool::shutdown);
    closer.close(
        () -> {
          boolean successful =
              threadPool.awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
          if (!successful) {
            throw new IOException("threadPool did not shut down");
          }
        });
    closer.close(this::updateSequenceNumbersDirectly);
    LOGGER.info("shutdown complete");
    closer.finish();
  }

  @Override
  public void close() throws Exception {
    final MultiCloser closer = new MultiCloser();
    if (!threadPool.isShutdown()) {
      closer.close(() -> shutdown(Duration.ofMinutes(5)));
    }
    closer.close(this::updateSequenceNumbersForClose);
    closer.close(sink::close);
    closer.finish();
  }

  private void updateSequenceNumbersDirectly() throws ProcessingException {
    final long sequenceNumber = sequenceNumbers.getNextSequenceNumber();
    if (sequenceNumber > 0) {
      try {
        sink.updateLastSequenceNumber(sequenceNumber);
      } catch (Exception ex) {
        throw new ProcessingException(ex, 0);
      }
    }
  }

  private void updateSequenceNumbersForClose() throws Exception {
    int count = getProcessedCount();
    if (count != 0) {
      LOGGER.warn("uncollected final processedCount: {}", count);
    }
    updateSequenceNumbersDirectly();
  }
}
