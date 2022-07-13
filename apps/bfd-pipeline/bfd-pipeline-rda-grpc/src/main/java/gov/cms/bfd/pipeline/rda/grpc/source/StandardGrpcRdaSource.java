package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.DroppedConnectionException;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
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

  private final Clock clock;
  private final Optional<Long> startingSequenceNumber;
  /** Expected time before RDA API server drops its connection when it has nothing to send. */
  private final long minIdleMillisBeforeConnectionDrop;

  /**
   * The primary constructor for this class. Constructs a GrpcRdaSource and opens a channel to the
   * gRPC service.
   *
   * @param config the configuration values used to establish the channel
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType the claim type
   * @param startingSequenceNumber optional hard coded sequence number
   */
  public StandardGrpcRdaSource(
      RdaSourceConfig config,
      GrpcStreamCaller<TMessage> caller,
      MetricRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber) {
    this(
        Clock.systemUTC(),
        config.createChannel(),
        caller,
        config::createCallOptions,
        appMetrics,
        claimType,
        startingSequenceNumber,
        config.getMinIdleMillisBeforeConnectionDrop());
  }

  /**
   * This constructor accepts a fully constructed channel instead of a configuration object. This is
   * used internally by the primary constructor but is also used by unit tests to allow a mock
   * channel to be provided.
   *
   * @param clock used to access current time
   * @param channel channel used to make RPC calls
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param startingSequenceNumber optional hard coded sequence number
   */
  @VisibleForTesting
  StandardGrpcRdaSource(
      Clock clock,
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      Supplier<CallOptions> callOptionsFactory,
      MetricRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber,
      long minIdleMillisBeforeConnectionDrop) {
    super(
        Preconditions.checkNotNull(channel),
        Preconditions.checkNotNull(caller),
        Preconditions.checkNotNull(claimType),
        callOptionsFactory,
        appMetrics);
    this.clock = clock;
    this.startingSequenceNumber = Preconditions.checkNotNull(startingSequenceNumber);
    this.minIdleMillisBeforeConnectionDrop = minIdleMillisBeforeConnectionDrop;
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
    return tryRetrieveAndProcessObjects(
        () -> {
          ProcessResult processResult = new ProcessResult();
          int processed = 0;
          long lastProcessedTime = clock.millis();

          final long startingSequenceNumber = getStartingSequenceNumber(sink);
          log.info(
              "calling API for {} claims starting at sequence number {}",
              claimType,
              startingSequenceNumber);
          final String apiVersion = caller.callVersionService(channel, callOptionsFactory.get());

          final GrpcResponseStream<TMessage> responseStream =
              caller.callService(channel, callOptionsFactory.get(), startingSequenceNumber);
          final Map<Object, TMessage> batch = new LinkedHashMap<>();
          try {
            while (responseStream.hasNext()) {
              setUptimeToReceiving();
              final TMessage result = responseStream.next();
              metrics.getObjectsReceived().mark();
              batch.put(sink.getDedupKeyForMessage(result), result);
              if (batch.size() >= maxPerBatch) {
                processed += submitBatchToSink(apiVersion, sink, batch);
              }
              lastProcessedTime = clock.millis();
            }
            if (batch.size() > 0) {
              processed += submitBatchToSink(apiVersion, sink, batch);
            }
          } catch (GrpcResponseStream.StreamInterruptedException ex) {
            // If our thread is interrupted we cancel the stream so the server knows we're done
            // and then shut down normally.
            responseStream.cancelStream("shutting down due to InterruptedException");
            processResult.setWasInterrupted(true);
          } catch (GrpcResponseStream.DroppedConnectionException ex) {
            processResult.setException(
                logOrReturnDroppedConnectionException(lastProcessedTime, ex));
          } catch (ProcessingException ex) {
            processed += ex.getProcessedCount();
            processResult.setException(ex);
          } catch (Exception ex) {
            processResult.setException(ex);
          }
          sink.shutdown(Duration.ofMinutes(5));
          processed += sink.getProcessedCount();

          processResult.setCount(processed);
          return processResult;
        });
  }

  /**
   * The RDA API server drops open connections abruptly when it has no data to transmit for some
   * period of time. These closures are not clean at the protocol level, so they appear as errors to
   * gRPC, but we don't want to trigger alerts when they happen since they are not unexpected.
   *
   * <p>This method determines if we have been idle long enough that such a drop is possible. If the
   * drop is expected it simply logs the event but if the drop is not expected it returns the
   * exception so that normal error logic can be applied to it.
   *
   * @param lastProcessedTime time in millis when we last processed a message from the server
   * @param exception the exception to evaluate
   * @return The Exception if idle time was not exceeded, null otherwise.
   * @throws DroppedConnectionException if not an expected drop
   */
  private Exception logOrReturnDroppedConnectionException(
      long lastProcessedTime, DroppedConnectionException exception)
      throws DroppedConnectionException {
    Exception e = null;
    final long idleMillis = clock.millis() - lastProcessedTime;
    if (idleMillis >= minIdleMillisBeforeConnectionDrop) {
      log.info(
          "RDA API server dropped connection after idle time: idleMillis={} message='{}'",
          idleMillis,
          exception.getMessage());
    } else {
      e = exception;
    }

    return e;
  }

  /**
   * Uses the hard coded starting sequence number if one has been configured. Otherwise asks the
   * sink to provide its maximum known sequence number as our starting point. When all else fails we
   * start at the beginning.
   *
   * @param sink used to obtain the maximum known sequence number
   * @return a valid RDA change sequence number
   */
  private long getStartingSequenceNumber(RdaSink<TMessage, TClaim> sink)
      throws ProcessingException {
    if (startingSequenceNumber.isPresent()) {
      return startingSequenceNumber.get();
    } else {
      return sink.readMaxExistingSequenceNumber().map(seqNo -> seqNo + 1).orElse(MIN_SEQUENCE_NUM);
    }
  }
}
