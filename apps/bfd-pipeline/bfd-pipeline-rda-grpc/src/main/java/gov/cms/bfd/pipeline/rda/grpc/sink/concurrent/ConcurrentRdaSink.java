package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * A sink implementation that uses a thread pool to perform all writes asynchronously.
 *
 * @param <TMessage>
 * @param <TClaim>
 */
public class ConcurrentRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  private final WriterThreadPool<TMessage, TClaim> writerPool;

  /**
   * Constructs a ConcurrentRdaSink with the specified configuration. Actual writes are delegated to
   * single-threaded sink objects produced using the provided factory method.
   *
   * @param maxThreads number of writer threads used to write claims
   * @param batchSize number of messages per batch for database writes
   * @param sinkFactory factory method to produce appropriate single threaded sinks
   */
  public ConcurrentRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    this(new WriterThreadPool<>(maxThreads, batchSize, sinkFactory));
  }

  @VisibleForTesting
  ConcurrentRdaSink(WriterThreadPool<TMessage, TClaim> writerPool) {
    this.writerPool = writerPool;
  }

  /**
   * Create an RdaSink using the specified number of threads. If maxThreads is one a single-threaded
   * sink is created using sinkFactory. Otherwise a ConcurrentRdaSink is created using the specified
   * number of threads. The sinkFactory function takes a boolean indicating whether the created sink
   * should manage sequence number updates itself (true) or not update sequence numbers (false).
   *
   * @param maxThreads number of threads to use when writing
   * @param keyExtractor function to get a key to dedup records
   * @param sinkFactory function to create a sink
   * @param <T> type of objects being written
   * @return either a simple sink or a ConcurrentRdaSink
   */
  public static <TMessage, TClaim> RdaSink<TMessage, TClaim> createSink(
      int maxThreads, int batchSize, Function<Boolean, RdaSink<TMessage, TClaim>> sinkFactory) {
    if (maxThreads == 1) {
      return sinkFactory.apply(true);
    } else {
      return new ConcurrentRdaSink<>(maxThreads, batchSize, () -> sinkFactory.apply(false));
    }
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return writerPool.readMaxExistingSequenceNumber();
  }

  /**
   * This method is not implemented since that would bypass the queue used to schedule writes.
   *
   * @param lastSequenceNumber sequence number to write to the database
   */
  @Override
  public void updateLastSequenceNumber(long lastSequenceNumber) {
    throw new UnsupportedOperationException();
  }

  /**
   * Enqueues the provided message objects for writing. They will be written to the database at some
   * unspecified point in the future. They may be written in a different order than they appear
   * within the collection EXCEPT that two objects corresponding to the same claim will always be
   * written in the order they appear in the collection.
   *
   * @param apiVersion version string for the apiSource column in the claim table
   * @param objects zero or more objects to be written to the data store
   * @return the number of objects written since last call to writeMessages or getProcessedCount
   * @throws ProcessingException if something goes wrong
   */
  @Override
  public int writeMessages(String apiVersion, Collection<TMessage> objects)
      throws ProcessingException {
    int count = 0;
    try {
      for (TMessage object : objects) {
        writerPool.addToQueue(apiVersion, object);
      }
      count = writerPool.getProcessedCount();
      if (count > 0) {
        writerPool.updateSequenceNumbers();
      }
    } catch (Exception ex) {
      throw new ProcessingException(ex, count);
    }
    return count;
  }

  @Override
  public String getDedupKeyForMessage(TMessage object) {
    return writerPool.getDedupKeyForMessage(object);
  }

  @Override
  public long getSequenceNumberForObject(TMessage object) {
    return writerPool.getSequenceNumberForObject(object);
  }

  @Nonnull
  @Override
  public TClaim transformMessage(String apiVersion, TMessage message) {
    return writerPool.transformMessage(apiVersion, message);
  }

  /**
   * This method is not implemented since that would bypass the queue used to schedule writes.
   *
   * @param objects collection of entity objects to be written to the database
   * @throws ProcessingException always throws an exception
   */
  @Override
  public int writeClaims(Collection<TClaim> objects) throws ProcessingException {
    throw new ProcessingException(new UnsupportedOperationException(), 0);
  }

  @Override
  public int getProcessedCount() throws ProcessingException {
    return writerPool.getProcessedCount();
  }

  /**
   * Shuts down the thread pool and closes all sinks. Any unwritten data is flushed to the database.
   */
  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    try {
      writerPool.shutdown(waitTime);
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  /**
   * Shuts down the thread pool and closes all sinks. Any unwritten data is flushed to the database.
   */
  @Override
  public void close() throws Exception {
    writerPool.close();
  }
}
