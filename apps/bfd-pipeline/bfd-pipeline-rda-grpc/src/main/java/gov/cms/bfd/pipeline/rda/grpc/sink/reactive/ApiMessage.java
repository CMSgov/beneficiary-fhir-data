package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class ApiMessage<TMessage> {
  /** Used to identify {@link #IdleMessage}. */
  static final long IdleSequenceNumber = -1;
  /** Used to identify {@link #FlushMessage}. */
  static final long FlushSequenceNumber = -2;

  private final String claimId;
  private final long sequenceNumber;
  private final String apiVersion;
  private final TMessage message;
}
