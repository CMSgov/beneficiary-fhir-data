package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Writer threads report their results by invoking a callback method that implements this interface.
 * Use of the interface decouples the threads from knowing who/what calls them.
 *
 * @param <TMessage> the type of objects being written.
 */
public interface ReportingCallback<TMessage> {

  /**
   * Accepts a processed batch.
   *
   * @param result the batch
   * @throws InterruptedException if the thread is interrupted during execution
   */
  void accept(ProcessedBatch<TMessage> result) throws InterruptedException;

  /**
   * Represents a processed batch.
   *
   * @param <T> the type of the objects in the batch
   */
  @Value
  class ProcessedBatch<T> {
    /** Number of objects that were successfully processed. */
    int processed;

    /** Objects in the batch being reported. */
    List<T> batch;

    /** Possible exception thrown during processing. */
    @Nullable Exception error;
  }
}
