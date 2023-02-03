package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class ClaimWriter<TMessage, TClaim> {
  @Getter @EqualsAndHashCode.Include private final int id;
  private final RdaSink<TMessage, TClaim> sink;
  private final int batchSize;
  private final List<ApiMessage<TMessage>> messageBuffer;
  private final Map<String, TClaim> claimBuffer;
  private boolean idle;

  ClaimWriter(int id, RdaSink<TMessage, TClaim> sink, int batchSize) {
    this.id = id;
    this.sink = sink;
    this.batchSize = batchSize;
    messageBuffer = new ArrayList<>();
    claimBuffer = new LinkedHashMap<>();
  }

  synchronized Mono<BatchResult<TMessage>> processMessage(ApiMessage<TMessage> message) {
    Mono<BatchResult<TMessage>> result = Mono.empty();
    try {
      var writeNeeded = addToBatch(message);
      if (writeNeeded) {
        result = writeBatch();
      }
    } catch (Exception ex) {
      result = Mono.just(new BatchResult<>(List.of(message), ex));
    }
    return result;
  }

  synchronized void close() throws Exception {
    log.debug("ClaimWriter {} closing", id);
    sink.close();
    log.info("ClaimWriter {} closed", id);
  }

  private boolean addToBatch(ApiMessage<TMessage> message) throws IOException, ProcessingException {
    boolean writeNeeded;
    if (message.isIdleMessage()) {
      writeNeeded = idle && claimBuffer.size() > 0;
      idle = true;
    } else if (message.isFlushMessage()) {
      writeNeeded = claimBuffer.size() > 0;
      idle = false;
    } else {
      //        if (new Random().nextInt(10_000) == 13) {
      //          throw new IOException("Random stop during transformation!");
      //        }
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

  private Mono<BatchResult<TMessage>> writeBatch() {
    var messages = List.copyOf(messageBuffer);
    var claims = List.copyOf(claimBuffer.values());
    messageBuffer.clear();
    claimBuffer.clear();

    Mono<BatchResult<TMessage>> result;
    try {
      if (new Random().nextInt(100) == 13) {
        throw new IOException("Random stop during write!");
      }
      final int processed = sink.writeClaims(claims);
      //          log.info(
      //              "ClaimWriter {} wrote unique={} all={} processed={} idle={} seq={}",
      //              id,
      //              claims.size(),
      //              messages.size(),
      //              processed,
      //              idle,
      //              message.sequenceNumber);
      result = Mono.just(new BatchResult<>(messages, processed));
    } catch (Exception ex) {
      result = Mono.just(new BatchResult<>(messages, ex));
    }
    return result;
  }
}
