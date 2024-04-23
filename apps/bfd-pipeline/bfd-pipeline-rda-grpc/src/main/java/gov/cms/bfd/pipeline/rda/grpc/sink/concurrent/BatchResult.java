package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import jakarta.annotation.Nullable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Contains the result of processing a batch of claims by {@link ClaimWriter} or sequence number
 * update {@link SequenceNumberWriter}. Failed results have a non-null {@link #error} field.
 * Successful results have a null {@link #error} field.
 *
 * @param <TMessage> The specific type of gRPC stub from the RDA API.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class BatchResult<TMessage> {
  /** Number of claims written into the database in the batch. */
  private final int processedCount;

  /**
   * All RDA API messages that were part of the batch. Not all messages will be written to the
   * database (e.g. two updates for same claim as well as control messages for idle detection and
   * flushing buffers) but all will be included in this list.
   */
  private final List<ApiMessage<TMessage>> messages;

  /** Unsuccessful requests provide the exception that was thrown during processing. */
  @Nullable private final Exception error;

  /**
   * Create an instance for a successful request.
   *
   * @param messages All RDA API messages that were part of the batch.
   * @param processedCount Number of claims written into the database in the batch.
   */
  BatchResult(List<ApiMessage<TMessage>> messages, int processedCount) {
    this(processedCount, messages, null);
  }

  /**
   * Create an instance for a failed request.
   *
   * @param messages All RDA API messages that were part of the batch.
   * @param error The exception that was thrown during processing.
   */
  BatchResult(List<ApiMessage<TMessage>> messages, Exception error) {
    this(0, messages, error);
  }

  /**
   * Create an instance for a sequence number update failure. No messages are involved in these
   * failures.
   *
   * @param error The exception that was thrown during processing.
   */
  public BatchResult(@Nullable Exception error) {
    this(0, List.of(), error);
  }
}
