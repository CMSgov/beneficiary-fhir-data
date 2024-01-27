package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.SequenceNumberTracker;
import javax.annotation.concurrent.ThreadSafe;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * An object responsible for updating the progress table in the database with the appropriate
 * sequence number as claims are written to the database. Sequence numbers are provided by calls to
 * a {@link SequenceNumberTracker} and updates are written using a {@link RdaSink}.
 *
 * @param <TMessage> type of RDA API gRPC stub object corresponding to a message
 * @param <TClaim> type of hibernate entity class corresponding to a claim
 */
@Slf4j
@ThreadSafe
class SequenceNumberWriter<TMessage, TClaim> {
  /** Used to write the sequence numbers to the database. */
  private final RdaSink<TMessage, TClaim> sink;

  /**
   * Used to track sequence numbers to know what sequence number should be written to the database.
   */
  private final SequenceNumberTracker sequenceNumbers;

  /**
   * Most recently written sequence number. Used to avoid redundant writes of the same sequence
   * number.
   */
  private long previousSequenceNumber = 0;

  /**
   * Create an instance using the provided {@link RdaSink} and {@link SequenceNumberTracker}.
   *
   * @param sink used to write to the database
   * @param sequenceNumbers used to track sequence number changes
   */
  SequenceNumberWriter(RdaSink<TMessage, TClaim> sink, SequenceNumberTracker sequenceNumbers) {
    this.sink = sink;
    this.sequenceNumbers = sequenceNumbers;
  }

  /**
   * Updates the sequence number in the database. Uses the current value from the {@link
   * SequenceNumberTracker}. If the value has not changed nothing is written and the returned {@link
   * Mono} will have no value. If the value has changed the new value will be written to the
   * database and emitted by the {@link Mono}. If the write false the {@link Mono} will emit the
   * {@link Exception} associated with the failure.
   *
   * @return {@link Mono} describing the result of the update
   */
  synchronized Mono<Long> updateSequenceNumberInDatabase() {
    long newSequenceNumber = sequenceNumbers.getSafeResumeSequenceNumber();
    if (newSequenceNumber != previousSequenceNumber) {
      try {
        sink.updateLastSequenceNumber(newSequenceNumber);
        log.debug(
            "SequenceNumberWriter updated last={} new={}",
            previousSequenceNumber,
            newSequenceNumber);
        previousSequenceNumber = newSequenceNumber;
        return Mono.just(newSequenceNumber);
      } catch (Exception ex) {
        log.error("SequenceNumberWriter error: {}", ex.getMessage(), ex);
        return Mono.error(ex);
      }
    } else {
      return Mono.empty();
    }
  }

  /**
   * Performs a final update and then closes the {@link RdaSink}.
   *
   * @throws Exception if any of the operations throws
   */
  synchronized void close() throws Exception {
    log.debug("SequenceNumberWriter closing");
    final var closer = new MultiCloser();
    closer.close(() -> updateSequenceNumberInDatabase().block());
    closer.close(sink::close);
    closer.finish();
    log.info("SequenceNumberWriter closed");
  }
}
