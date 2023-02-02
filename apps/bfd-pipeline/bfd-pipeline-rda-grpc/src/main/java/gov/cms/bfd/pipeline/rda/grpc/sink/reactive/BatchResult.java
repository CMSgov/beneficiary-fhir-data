package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class BatchResult<TMessage> {
  private final int processedCount;
  private final List<ApiMessage<TMessage>> messages;
}
