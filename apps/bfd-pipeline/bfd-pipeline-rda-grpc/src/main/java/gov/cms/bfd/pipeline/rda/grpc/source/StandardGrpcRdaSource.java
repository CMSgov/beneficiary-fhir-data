package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.DroppedConnectionException;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * General RdaSource implementation that delegates actual service call and result mapping to other
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
public class StandardGrpcRdaSource<TMessage, TClaim>
    extends AbstractGrpcRdaSource<TMessage, TClaim> {

  /** The maximum amount of time to wait for an {@link RdaSink} to shut down. */
  private static final Duration MAX_SINK_SHUTDOWN_WAIT = Duration.ofMinutes(5);

  /** A clock for generating timestamps. */
  private final Clock clock;

  /** The start of the sequence numbers. */
  private final Optional<Long> startingSequenceNumber;

  /** Expected time before RDA API server drops its connection when it has nothing to send. */
  private final long minIdleMillisBeforeConnectionDrop;

  /** The type of RDA API server to connect to. */
  private final RdaSourceConfig.ServerType serverType;

  /**
   * The primary constructor for this class. Constructs a GrpcRdaSource and opens a channel to the
   * gRPC service.
   *
   * @param config the configuration values used to establish the channel
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType the claim type
   * @param startingSequenceNumber optional hard coded sequence number
   * @param rdaVersion The required {@link RdaVersion} in order to ingest data
   */
  public StandardGrpcRdaSource(
      RdaSourceConfig config,
      GrpcStreamCaller<TMessage> caller,
      MeterRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber,
      RdaVersion rdaVersion) {
    this(
        Clock.systemUTC(),
        config.createChannel(),
        caller,
        config::createCallOptions,
        appMetrics,
        claimType,
        startingSequenceNumber,
        config.getMinIdleMillisBeforeConnectionDrop(),
        config.getServerType(),
        rdaVersion);
  }

  /**
   * This constructor accepts a fully constructed channel instead of a configuration object. This is
   * used internally by the primary constructor but is also used by unit tests to allow a mock
   * channel to be provided.
   *
   * @param clock used to access current time
   * @param channel channel used to make RPC calls
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param callOptionsFactory factory for generating runtime options for the gRPC call
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType string representation of the claim type
   * @param startingSequenceNumber optional hard coded sequence number
   * @param minIdleMillisBeforeConnectionDrop the amount of time before a connection drop is
   *     expected
   * @param serverType the server type
   * @param rdaVersion The required {@link RdaVersion} in order to ingest data
   */
  @VisibleForTesting
  StandardGrpcRdaSource(
      Clock clock,
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      Supplier<CallOptions> callOptionsFactory,
      MeterRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber,
      long minIdleMillisBeforeConnectionDrop,
      RdaSourceConfig.ServerType serverType,
      RdaVersion rdaVersion) {
    super(
        Preconditions.checkNotNull(channel),
        Preconditions.checkNotNull(caller),
        Preconditions.checkNotNull(claimType),
        callOptionsFactory,
        appMetrics,
        rdaVersion);
    this.clock = clock;
    this.startingSequenceNumber = Preconditions.checkNotNull(startingSequenceNumber);
    this.minIdleMillisBeforeConnectionDrop = minIdleMillisBeforeConnectionDrop;
    this.serverType = serverType;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation always reads starting sequence number from the database. When configured
   * to communicate with a remote API server it also gets the API version number from RDA API and
   * downloads several claims from the API.
   *
   * <p>The connection tests are skipped for InProcess servers because those are not running during
   * smoke test execution and so they cannot be tested. Also they do not have connection parameters
   * or potential network issues to be tested.
   *
   * @param sink to process batches of objects
   * @return true if all actions were completed successfully
   * @throws Exception if any of the actions threw an exception
   */
  @Override
  public boolean performSmokeTest(RdaSink<TMessage, TClaim> sink) throws Exception {
    log.info("smoke test: begin test: claimType={}", claimType);

    // Query the database to get a starting sequence number.  This verifies that the database is
    // accessible.
    final long startingSequenceNumber =
        sink.readMaxExistingSequenceNumber().orElse(MIN_SEQUENCE_NUM);
    log.info(
        "smoke test: read starting sequence number: claimType={} startingSequenceNumber={}",
        claimType,
        startingSequenceNumber);

    if (serverType == RdaSourceConfig.ServerType.Remote) {
      // Call the RDA API version service to confirm the API is accessible.
      final String apiVersion = caller.callVersionService(channel, callOptionsFactory.get());
      log.info(
          "smoke test: read RDA API version: claimType={} apiVersion={}", claimType, apiVersion);

      // Doesn't use startingSequenceNumber because we should not block waiting for new data.
      try (var responseStream =
          caller.callService(channel, callOptionsFactory.get(), MIN_SEQUENCE_NUM)) {
        for (int i = 1; i <= 3 && responseStream.hasNext(); ++i) {
          final TMessage message = responseStream.next();
          log.info(
              "smoke test: downloaded claim: claimType={} seq={}",
              claimType,
              sink.getSequenceNumberForObject(message));
        }
        // be a nice client that lets the server know when we are leaving before the stream is done
        responseStream.cancelStream("smoke test: finished");
      }
    }

    log.info("smoke test: end test: claimType={}", claimType);

    return true;
  }

  /**
   * {@inheritDoc} Calls the service through the specific implementation of GrpcStreamCaller
   * provided to our constructor. Cancels the response stream if reading from the stream is
   * interrupted.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return the number of objects that were successfully processed
   * @throws ProcessingException wrapper around any Exception thrown by the service or sink
   */
  @Override
  public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TMessage, TClaim> sink)
      throws ProcessingException {
    sink.checkErrorCount();

    return tryRetrieveAndProcessObjects(
        () -> {
          boolean flushBatch = true;
          ProcessResult processResult = new ProcessResult();
          long lastProcessedTime = clock.millis();

          final long startingSequenceNumber = getStartingSequenceNumber(sink);
          log.info(
              "calling API for {} claims starting at sequence number {}",
              claimType,
              startingSequenceNumber);
          final String apiVersion = caller.callVersionService(channel, callOptionsFactory.get());
          checkApiVersion(apiVersion);

          try (var responseStream =
              caller.callService(channel, callOptionsFactory.get(), startingSequenceNumber)) {
            final Map<Object, TMessage> batch = new LinkedHashMap<>();
            try {
              while (responseStream.hasNext()) {
                setUptimeToReceiving();
                final TMessage result = responseStream.next();
                metrics.getObjectsReceived().increment();
                if (sink.isDeleteMessage(result)) {
                  metrics.getDeleteMessagesSkipped().increment();
                  log.warn(
                      "skipping DELETE message: claimType={} claimId={} seq={}",
                      claimType,
                      sink.getClaimIdForMessage(result),
                      sink.getSequenceNumberForObject(result));
                } else if (sink.isValidMessage(result)) {
                  batch.put(sink.getClaimIdForMessage(result), result);
                  if (batch.size() >= maxPerBatch) {
                    ClaimSequenceNumberRange sequenceNumberRange =
                        caller.callSequenceNumberRangeService(channel, callOptionsFactory.get());
                    sink.updateSequenceNumberRange(sequenceNumberRange);
                    processResult.addCount(submitBatchToSink(apiVersion, sink, batch));
                  }
                  lastProcessedTime = clock.millis();
                } else {
                  metrics.getInvalidObjectsSkipped().increment();
                  log.info(
                      "skipping invalid claim: claimType={} claimId={} seq={}",
                      claimType,
                      sink.getClaimIdForMessage(result),
                      sink.getSequenceNumberForObject(result));
                }
              }
            } catch (GrpcResponseStream.StreamInterruptedException ex) {
              log.info("shutting down due to interrupted stream");
              flushBatch = false;
              processResult.setInterrupted(true);
            } catch (GrpcResponseStream.DroppedConnectionException ex) {
              log.info("shutting down due to dropped stream");
              if (isUnexpectedDroppedConnectionException(lastProcessedTime, ex)) {
                processResult.setException(ex);
              }
            } catch (ProcessingException ex) {
              log.info("shutting down due to ProcessingException: {}", ex.getMessage());
              flushBatch = false;
              processResult.addCount(ex.getProcessedCount());
              processResult.setException(ex);
            } catch (Exception ex) {
              log.info("shutting down due to Exception: {}", ex.getMessage());
              flushBatch = false;
              processResult.setException(ex);
            }

            MultiCloser closer = new MultiCloser();

            closer.close(() -> responseStream.cancelStream("shutting down"));

            if (batch.size() > 0 && flushBatch) {
              closer.close(
                  () -> processResult.addCount(submitBatchToSink(apiVersion, sink, batch)));
            }

            closer.close(() -> sink.shutdown(MAX_SINK_SHUTDOWN_WAIT));
            closer.close(() -> processResult.addCount(sink.getProcessedCount()));

            try {
              closer.finish();
            } catch (Exception ex) {
              if (processResult.getException() != null) {
                processResult.getException().addSuppressed(ex);
              } else {
                processResult.setException(ex);
              }
            }

            return processResult;
          }
        });
  }

  /**
   * The RDA API server drops open connections abruptly when it has no data to transmit for some
   * period of time. These closures are not clean at the protocol level, so they appear as errors to
   * gRPC, but we don't want to trigger alerts when they happen since they are not unexpected.
   *
   * <p>This method determines if we have been idle long enough that such a drop is possible. If the
   * drop is expected it simply logs the event and returns false to indicate the exception should
   * not be treated as an error. But if the drop is not expected it returns true to indicate that
   * the exception should be treated as an error and have normal error logic applied to it.
   *
   * @param lastProcessedTime time in millis when we last processed a message from the server
   * @param exception the exception to evaluate
   * @return true if not an expected drop and the exception should be treated as an error
   */
  private boolean isUnexpectedDroppedConnectionException(
      long lastProcessedTime, DroppedConnectionException exception) {
    final long idleMillis = clock.millis() - lastProcessedTime;

    if (idleMillis >= minIdleMillisBeforeConnectionDrop) {
      log.info(
          "RDA API server dropped connection after idle time: idleMillis={} message='{}'",
          idleMillis,
          exception.getMessage());
      return false;
    } else {
      return true;
    }
  }

  /**
   * Uses the hard coded starting sequence number if one has been configured. Otherwise asks the
   * sink to provide its maximum known sequence number as our starting point. When all else fails we
   * start at the beginning. The RDA API assumes we are sending the highest sequence number that we
   * already have so they will start at that plus 1. For a configured starting sequence number we
   * subtract one but for a value from the database we can just pass it through unchanged.
   *
   * @param sink used to obtain the maximum known sequence number
   * @return a valid RDA change sequence number
   */
  private long getStartingSequenceNumber(RdaSink<TMessage, TClaim> sink)
      throws ProcessingException {
    if (startingSequenceNumber.isPresent()) {
      return startingSequenceNumber.map(x -> x - 1).get();
    } else {
      return sink.readMaxExistingSequenceNumber().orElse(MIN_SEQUENCE_NUM);
    }
  }
}
