package gov.cms.bfd.pipeline.rda.grpc.sink.reactive;

import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.SequenceNumberTracker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
class SequenceNumberWriter<TMessage, TClaim> {
  private final RdaSink<TMessage, TClaim> sink;
  /** Used to track sequence numbers to update progress table in database. */
  private final SequenceNumberTracker sequenceNumbers;

  private long lastSequenceNumber = 0;

  SequenceNumberWriter(RdaSink<TMessage, TClaim> sink, SequenceNumberTracker sequenceNumbers) {
    this.sink = sink;
    this.sequenceNumbers = sequenceNumbers;
  }

  Flux<Long> updateDb(Long time) {
    long newSequenceNumber = sequenceNumbers.getSafeResumeSequenceNumber();
    if (newSequenceNumber != lastSequenceNumber) {
      try {
        sink.updateLastSequenceNumber(newSequenceNumber);
        log.debug(
            "SequenceNumberWriter updated last={} new={}", lastSequenceNumber, newSequenceNumber);
        lastSequenceNumber = newSequenceNumber;
        return Flux.just(newSequenceNumber);
      } catch (Exception ex) {
        log.error("SequenceNumberWriter error: {}", ex.getMessage(), ex);
        return Flux.error(ex);
      }
    } else {
      return Flux.empty();
    }
  }

  void close() throws Exception {
    log.debug("SequenceNumberWriter closing");
    updateDb(0L).singleOrEmpty().block();
    sink.close();
    log.info("SequenceNumberWriter closed");
  }
}
