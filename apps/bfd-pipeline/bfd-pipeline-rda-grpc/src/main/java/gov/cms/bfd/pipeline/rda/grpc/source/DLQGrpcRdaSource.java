package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * DLQ specific RdaSource implementation that checks for failed messages in the {@link MessageError}
 * table and re-queries them, then delegates actual service call and result mapping to other
 * objects. This class has no dependency on a particular RPC or data type. It does maintain the
 * ManagedChannel used to communicate with the gRPC service. The constructor creates the channel and
 * the close() method closes the channel.
 *
 * <p>This object makes the RPC call and processes the stream. Stream processing consists of
 * batching received objects and passing them to the RdaSink object for storage. Basic metrics are
 * tracked at this level.
 *
 * @param <TMessage> type of objects returned by the gRPC service
 */
@Slf4j
public class DLQGrpcRdaSource<TMessage, TClaim> extends AbstractGrpcRdaSource<TMessage, TClaim> {

  /** The maximum amount of time to wait for an {@link RdaSink} to shut down. */
  private static final Duration MAX_SINK_SHUTDOWN_WAIT = Duration.ofMinutes(5);

  /** Object for use in querying the database. */
  private final DLQDao dao;

  /** Used to compare sequence values. */
  private final BiPredicate<Long, TMessage> sequencePredicate;

  /** Number of days after which processed messages should expire and be deleted from the DLQ. */
  private final Optional<Integer> messageErrorExpirationDays;

  /**
   * The primary constructor for this class. Constructs a GrpcRdaSource and opens a channel to the
   * gRPC service.
   *
   * @param transactionManager the {@link TransactionManager} to use to communicate with the DB
   * @param sequencePredicate {@link BiPredicate} used to compare sequence values
   * @param config the configuration values used to establish the channel
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType the claim type
   * @param rdaVersion The required {@link RdaVersion} in order to ingest data
   */
  public DLQGrpcRdaSource(
      TransactionManager transactionManager,
      BiPredicate<Long, TMessage> sequencePredicate,
      RdaSourceConfig config,
      GrpcStreamCaller<TMessage> caller,
      MeterRegistry appMetrics,
      String claimType,
      RdaVersion rdaVersion) {
    this(
        transactionManager,
        sequencePredicate,
        config.createChannel(),
        caller,
        config::createCallOptions,
        appMetrics,
        claimType,
        rdaVersion,
        config.getMessageErrorExpirationDays(),
        new DLQDao(Clock.systemUTC(), Preconditions.checkNotNull(transactionManager)));
  }

  /**
   * This constructor accepts a fully constructed channel instead of a configuration object. This is
   * used internally by the primary constructor but is also used by unit tests to allow a mock
   * channel to be provided.
   *
   * @param transactionManager the {@link TransactionManager} to use to communicate with the DB
   * @param sequencePredicate {@link BiPredicate} used to compare sequence values
   * @param channel channel used to make RPC calls
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param callOptionsFactory factory for generating runtime options for the gRPC call
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType string representation of the claim type
   * @param rdaVersion The required {@link RdaVersion} in order to ingest data
   * @param messageErrorExpirationDays value for messageErrorExpirationDays
   * @param dao {@link DLQDao} used for database transactions
   */
  @VisibleForTesting
  DLQGrpcRdaSource(
      TransactionManager transactionManager,
      BiPredicate<Long, TMessage> sequencePredicate,
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      Supplier<CallOptions> callOptionsFactory,
      MeterRegistry appMetrics,
      String claimType,
      RdaVersion rdaVersion,
      Optional<Integer> messageErrorExpirationDays,
      DLQDao dao) {
    super(
        Preconditions.checkNotNull(channel),
        Preconditions.checkNotNull(caller),
        Preconditions.checkNotNull(claimType),
        callOptionsFactory,
        appMetrics,
        rdaVersion);
    this.dao = dao;
    this.sequencePredicate = sequencePredicate;
    this.messageErrorExpirationDays = messageErrorExpirationDays;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Ensures that our {@link DLQDao} is also closed.
   *
   * @throws Exception passed through if anything fails
   */
  @Override
  public void close() throws Exception {
    final var closer = new MultiCloser();
    closer.close(dao::close);
    closer.close(super::close);
    closer.finish();
  }

  /**
   * Checks for {@link MessageError}s in the database, then calls the service through the specific
   * implementation of GrpcStreamCaller provided to our constructor. Cancels the response stream if
   * reading from the stream is interrupted.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return the number of objects that were successfully processed
   * @throws ProcessingException wrapper around any Exception thrown by the service or sink
   */
  @Override
  public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TMessage, TClaim> sink)
      throws ProcessingException {
    int totalProcessed = 0;

    MessageError.ClaimType type = MessageError.ClaimType.valueOf(claimType.toUpperCase());

    messageErrorExpirationDays.ifPresent(maxAgeDays -> deleteExpiredDlqRecords(maxAgeDays, type));

    List<MessageError> messageErrors =
        dao.findAllMessageErrorsByClaimTypeAndStatus(type, MessageError.Status.UNRESOLVED);

    final Set<Long> sequenceNumbers =
        messageErrors.stream().map(MessageError::getSequenceNumber).collect(Collectors.toSet());

    if (sequenceNumbers.isEmpty()) {
      log.info("Found no {} claims in DLQ, skipping", claimType);
    } else {
      log.info(
          "Found {} {} claims in DLQ, attempting to reprocess", sequenceNumbers.size(), claimType);

      totalProcessed =
          tryRetrieveAndProcessObjects(dlqProcessingLogic(sink, type, sequenceNumbers));
    }

    return totalProcessed;
  }

  /**
   * Attempts to delete expired records from the DLQ prior to processing. All deletions happen in a
   * separate transaction. Because deleting records is not absolutely required for the DLQ
   * processing to be completed any errors during the transaction are simply logged.
   *
   * @param maxAgeDays maximum days to retain processed message error records
   * @param type the {@link MessageError.ClaimType} of records to delete
   * @return number of records deleted
   */
  @CanIgnoreReturnValue
  @VisibleForTesting
  int deleteExpiredDlqRecords(int maxAgeDays, MessageError.ClaimType type) {
    int deletedRecordCount;
    try {
      deletedRecordCount = dao.deleteExpiredMessageErrors(maxAgeDays, type);
      if (deletedRecordCount > 0) {
        log.info("Deleted {} expired {} claims from DLQ", deletedRecordCount, type);
      } else {
        log.info("Found no {} expired claims in DLQ", claimType);
      }
    } catch (Exception ex) {
      // Exceptions are not fatal, they just need to be logged for reference.
      log.warn("Caught exception while deleting expired {} claims from DLQ", claimType, ex);
      deletedRecordCount = 0;
    }
    return deletedRecordCount;
  }

  /**
   * Helper method, called internally, to return the DLQ processing logic to pass to {@link
   * AbstractGrpcRdaSource#tryRetrieveAndProcessObjects(Processor)}.
   *
   * @param sink The sink to use to write claims, etc.
   * @param type The {@link MessageError.ClaimType} associated with the claims to process.
   * @param sequenceNumbers A {@link Set} of sequence numbers to attempt to reprocess
   * @return The DLQ processing logic {@link Processor}.
   */
  @VisibleForTesting
  Processor dlqProcessingLogic(
      RdaSink<TMessage, TClaim> sink, MessageError.ClaimType type, Set<Long> sequenceNumbers) {
    return () -> {
      ProcessResult processResult = new ProcessResult();
      final String apiVersion = caller.callVersionService(channel, callOptionsFactory.get());
      checkApiVersion(apiVersion);

      for (final long startingSequenceNumber : sequenceNumbers) {
        log.info(
            "calling API for {} claims starting at sequence number {}",
            claimType,
            startingSequenceNumber);

        // The "since" parameter of the RDA API is actually non-inclusive, so we have to subtract 1
        // in order to attempt to get the claim we really want
        final long SINCE = startingSequenceNumber - 1;

        try (GrpcResponseStream<TMessage> responseStream =
            caller.callService(channel, callOptionsFactory.get(), SINCE)) {
          final Map<Object, TMessage> batch = new LinkedHashMap<>();

          try {
            if (responseStream.hasNext()) {
              setUptimeToReceiving();
              final TMessage result = responseStream.next();
              metrics.getObjectsReceived().increment();

              if (sequencePredicate.test(startingSequenceNumber, result)) {
                // It's a match, so check if we can successfully process it now.
                batch.put(sink.getClaimIdForMessage(result), result);
                int processed = submitBatchToSink(apiVersion, sink, batch);
                processResult.addCount(processed);

                if (processed > 0
                    && dao.updateState(startingSequenceNumber, type, MessageError.Status.RESOLVED)
                        > 0) {
                  log.info(
                      "{} claim with sequence ({}) processed successfully, marking as resolved",
                      claimType,
                      startingSequenceNumber);
                }
              } else {
                // We didn't get the sequence number we wanted, which means it's obsolete
                if (dao.updateState(startingSequenceNumber, type, MessageError.Status.OBSOLETE)
                    > 0) {
                  log.info(
                      "{} claim with sequence({}) was not returned, marking as obsolete",
                      claimType,
                      startingSequenceNumber);
                } else {
                  log.error(
                      "{} claim with sequence({}) was not returned, but failed to mark obsolete",
                      claimType,
                      startingSequenceNumber);
                }
              }
            }
            responseStream.cancelStream("No further messages need to be ingested.");
          } catch (GrpcResponseStream.StreamInterruptedException ex) {
            // If our thread is interrupted we cancel the stream so the server knows we're done
            // and then shut down normally.
            processResult.setInterrupted(true);
            responseStream.cancelStream("shutting down due to InterruptedException");
          } catch (Exception e) {
            responseStream.cancelStream("shutting down due to Exception");
            // If we failed to process the claim, it stays in the DLQ, nothing to do.
            log.error(
                "Failed to process "
                    + claimType
                    + " message with sequence: "
                    + startingSequenceNumber,
                e);
          }
        }
      }

      try {
        sink.shutdown(MAX_SINK_SHUTDOWN_WAIT);
      } catch (Exception ex) {
        if (processResult.getException() != null) {
          processResult.getException().addSuppressed(ex);
        } else {
          processResult.setException(ex);
        }
      }

      processResult.addCount(sink.getProcessedCount());

      return processResult;
    };
  }
}
