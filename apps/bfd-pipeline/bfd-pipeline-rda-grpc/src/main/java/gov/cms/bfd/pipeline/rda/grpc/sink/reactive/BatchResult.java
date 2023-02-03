package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import java.util.List;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class BatchResult<TMessage> {
  private final int processedCount;
  private final List<ApiMessage<TMessage>> messages;
  @Nullable private final Exception error;

  BatchResult(List<ApiMessage<TMessage>> messages, int processedCount) {
    this(processedCount, messages, null);
  }

  BatchResult(List<ApiMessage<TMessage>> messages, Exception error) {
    this(0, messages, error);
  }

  public BatchResult(@Nullable Exception error) {
    this(0, List.of(), error);
  }
}
