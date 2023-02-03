package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class ApiMessage<TMessage> {
  /**
   * Used to identify special message that triggers on-idle buffer flush logic in {@link
   * ClaimWriter}.
   */
  private static final long IdleSequenceNumber = -1;
  /**
   * Used to identify special message that triggers immediate buffer flush logic in {@link
   * ClaimWriter}.
   */
  private static final long FlushSequenceNumber = -2;

  /** Unique id of the claim associated with this message. */
  private final String claimId;
  /** Unique sequence number of this message. */
  private final long sequenceNumber;
  /** Version string from the RDA APi server that produced the message. */
  private final String apiVersion;
  /** Message received from the RDA API that contains a claim. */
  private final TMessage message;

  /**
   * Is this a special message to detect when a Flux is idle?
   *
   * @return true if it is
   */
  boolean isIdleMessage() {
    return message == null && sequenceNumber == IdleSequenceNumber;
  }

  /**
   * Is this a special message to signal immediate buffer flush is needed?
   *
   * @return true if it is
   */
  boolean isFlushMessage() {
    return message == null && sequenceNumber == FlushSequenceNumber;
  }

  /**
   * Create a special message used to detect when a writer is idle.
   *
   * @return the message
   * @param <TMessage> underlying API message type
   */
  static <TMessage> ApiMessage<TMessage> createIdleMessage() {
    return new ApiMessage<>(null, IdleSequenceNumber, null, null);
  }

  /**
   * Create a special message used to signal an immediate buffer flush is needed.
   *
   * @return the message
   * @param <TMessage> underlying API message type
   */
  static <TMessage> ApiMessage<TMessage> createFlushMessage() {
    return new ApiMessage<>(null, FlushSequenceNumber, null, null);
  }
}
