package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Object used to accept incoming messages, transform them into claims, accumulate them into full
 * batches, and write them to the database.
 *
 * @param <TMessage> type of RDA API gRPC stub object corresponding to a message
 * @param <TClaim> type of hibernate entity class corresponding to a claim
 */
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ThreadSafe
class ClaimWriter<TMessage, TClaim> {
  /** Unique integer identifier for this worker. Useful in logging. */
  @Getter @EqualsAndHashCode.Include private final int id;

  /** {@link RdaSink} used to transform and write claims. */
  private final RdaSink<TMessage, TClaim> sink;

  /** Number of claims per batch. */
  private final int batchSize;

  /**
   * All {@link ApiMessage}s used to construct current batch of claims. This includes any duplicates
   * and/or transformation failures that are not written. Control messages are not included since
   * they are not needed for tracking sequence numbers.
   */
  private final List<ApiMessage<TMessage>> messageBuffer;

  /**
   * Current set of claims to be written to the database. Might be smaller than {@link
   * #messageBuffer} if more than one message contained the same claim. In that case only the latest
   * version of the claim is stored since it makes any older version of that claim obsolete.
   */
  private final Map<String, TClaim> claimBuffer;

  /**
   * Used to respond to idle control messages. Two consecutive idle messages (with no other message
   * in between) trigger a flush of the current batch even if it is smaller than {@link #batchSize}.
   */
  private boolean idle;

  /**
   * Create an instance.
   *
   * @param id unique identifier for this object
   * @param sink {@link RdaSink} used to transform and write claims
   * @param batchSize number of claims per batch
   */
  ClaimWriter(int id, RdaSink<TMessage, TClaim> sink, int batchSize) {
    this.id = id;
    this.sink = sink;
    this.batchSize = batchSize;
    messageBuffer = new ArrayList<>(batchSize);
    claimBuffer = new LinkedHashMap<>(batchSize);
  }

  /**
   * Process the {@link ApiMessage}. Control messages trigger the appropriate action. Regular
   * messages are transformed into claims and buffered until either {@link #batchSize} claims have
   * been accumulated or a control message causes an incomplete batch to be written.
   *
   * @param message the {@link ApiMessage} to process
   * @return {@link Mono} containing the result if batch written or nothing if nothing was written
   */
  synchronized Mono<BatchResult<TMessage>> processMessage(ApiMessage<TMessage> message) {
    Mono<BatchResult<TMessage>> result = Mono.empty();
    try {
      var writeNeeded = ingestApiMessage(message);
      if (writeNeeded) {
        result = writeBatchToSink();
      }
    } catch (Exception ex) {
      result = Mono.just(new BatchResult<>(List.of(message), ex));
    }
    return result;
  }

  /**
   * Close the sink.
   *
   * @throws Exception pass through if thrown by sink
   */
  synchronized void close() throws Exception {
    log.debug("ClaimWriter {} closing", id);
    sink.close();
    log.info("ClaimWriter {} closed", id);
  }

  /**
   * Used by tests to confirm that batch write clears the buffers.
   *
   * @return true if both buffers are empty
   */
  @VisibleForTesting
  synchronized boolean isEmpty() {
    return messageBuffer.isEmpty() && claimBuffer.isEmpty();
  }

  /**
   * Used by tests to confirm that a specified message is in the {@link #messageBuffer}.
   *
   * @param message {@link ApiMessage} to look for
   * @return true if the message is found
   */
  @VisibleForTesting
  synchronized boolean containsMessage(ApiMessage<TMessage> message) {
    return messageBuffer.contains(message);
  }

  /**
   * Ingest the incoming {@link ApiMessage} and update our state. Returns true if the message
   * completes a batch or requires flushing an incomplete batch to the database. Otherwise returns
   * false.
   *
   * <p>Write will be required if the message:
   *
   * <ul>
   *   <li>contains a claim and {@link #claimBuffer} reaches the batch size number of claims
   *   <li>is an idle control message and the previous message was also an idle message
   *   <li>is a flush control message
   * </ul>
   *
   * @param message {@link ApiMessage} to ingest
   * @return true if a batch needs to be written
   * @throws IOException pass through if thrown by sink
   * @throws ProcessingException pass through if thrown by sink
   */
  private boolean ingestApiMessage(ApiMessage<TMessage> message)
      throws IOException, ProcessingException {
    boolean writeNeeded;
    if (message.isIdleMessage()) {
      writeNeeded = idle && claimBuffer.size() > 0;
      idle = true;
    } else if (message.isFlushMessage()) {
      writeNeeded = claimBuffer.size() > 0;
      idle = false;
    } else {
      final var claim =
          sink.transformMessage(message.getApiVersion(), message.getMessage()).orElse(null);
      messageBuffer.add(message);
      if (claim != null) {
        claimBuffer.put(message.getClaimId(), claim);
      }
      writeNeeded = claimBuffer.size() >= batchSize;
      idle = false;
    }
    return writeNeeded;
  }

  /**
   * Write the entire buffer of claims to the sink and clear the buffer.
   *
   * @return {@link BatchResult} indicating success or failure of the write
   */
  private Mono<BatchResult<TMessage>> writeBatchToSink() {
    var messages = List.copyOf(messageBuffer);
    var claims = List.copyOf(claimBuffer.values());
    messageBuffer.clear();
    claimBuffer.clear();

    Mono<BatchResult<TMessage>> result;
    try {
      final int processed = sink.writeClaims(claims);
      result = Mono.just(new BatchResult<>(messages, processed));
    } catch (Exception ex) {
      result = Mono.just(new BatchResult<>(messages, ex));
    }
    return result;
  }
}
