package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentRdaSink<TMessage, TClaim> implements RdaSink<TMessage, TClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentRdaSink.class);

  private final WriterThreadPool<TMessage, TClaim> writerPool;

  public ConcurrentRdaSink(
      int maxThreads, int batchSize, Supplier<RdaSink<TMessage, TClaim>> sinkFactory) {
    Preconditions.checkArgument(maxThreads > 0);
    writerPool = new WriterThreadPool<>(maxThreads, batchSize, sinkFactory);
  }

  /**
   * Create an RdaSink using the specified number of threads. If maxThreads is one a simple sink is
   * created using sinkFactory. Otherwise a ConcurrentRdaSink is created using the specified number
   * of threads. The sinkFactory function takes a boolean indicating whether the created sink should
   * manage sequence number updates itself (true) or not update sequence numbers (false).
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
  public int writeMessages(String apiVersion, Collection<TMessage> objects)
      throws ProcessingException {
    int count = 0;
    try {
      writerPool.addBatchToQueue(apiVersion, objects);
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
  public int getProcessedCount() throws ProcessingException {
    return writerPool.getProcessedCount();
  }

  @Override
  public void shutdown(Duration waitTime) throws ProcessingException {
    try {
      writerPool.shutdown(waitTime);
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }

  @Override
  public String getDedupKeyForMessage(TMessage object) {
    return writerPool.getSink().getDedupKeyForMessage(object);
  }

  @Override
  public void updateLastSequenceNumber(long lastSequenceNumber) {
    writerPool.getSink().updateLastSequenceNumber(lastSequenceNumber);
  }

  @Override
  public long getSequenceNumberForObject(TMessage object) {
    return writerPool.getSink().getSequenceNumberForObject(object);
  }

  @Override
  public void close() throws Exception {
    writerPool.close();
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return writerPool.getSink().readMaxExistingSequenceNumber();
  }

  @Nonnull
  @Override
  public TClaim transformMessage(String apiVersion, TMessage message) {
    return writerPool.getSink().transformMessage(apiVersion, message);
  }

  @Override
  public int writeClaims(String dataVersion, Collection<TClaim> objects)
      throws ProcessingException {
    throw new UnsupportedOperationException();
  }
}
