package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.NumericGauges;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
public class GrpcRdaDLQSource<TMessage, TClaim> implements RdaSource<TMessage, TClaim> {

  /** Holds the underlying value of our uptime gauges. */
  private static final NumericGauges GAUGES = new NumericGauges();

  private final DLQDao dao;
  private final BiPredicate<Long, TMessage> sequencePredicate;
  private final GrpcStreamCaller<TMessage> caller;
  private final String claimType;
  private final Supplier<CallOptions> callOptionsFactory;
  private final Metrics metrics;
  private ManagedChannel channel;

  /**
   * The primary constructor for this class. Constructs a GrpcRdaSource and opens a channel to the
   * gRPC service.
   *
   * @param config the configuration values used to establish the channel
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param claimType the claim type
   */
  public GrpcRdaDLQSource(
      EntityManager entityManager,
      BiPredicate<Long, TMessage> sequencePredicate,
      RdaSourceConfig config,
      GrpcStreamCaller<TMessage> caller,
      MetricRegistry appMetrics,
      String claimType) {
    this(
        entityManager,
        sequencePredicate,
        config.createChannel(),
        caller,
        config::createCallOptions,
        appMetrics,
        claimType);
  }

  /**
   * This constructor accepts a fully constructed channel instead of a configuration object. This is
   * used internally by the primary constructor but is also used by unit tests to allow a mock
   * channel to be provided.
   *
   * @param channel channel used to make RPC calls
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   */
  @VisibleForTesting
  GrpcRdaDLQSource(
      EntityManager entityManager,
      BiPredicate<Long, TMessage> sequencePredicate,
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      Supplier<CallOptions> callOptionsFactory,
      MetricRegistry appMetrics,
      String claimType) {
    this.dao = new DLQDao(Preconditions.checkNotNull(entityManager));
    this.sequencePredicate = sequencePredicate;
    this.caller = Preconditions.checkNotNull(caller);
    this.claimType = Preconditions.checkNotNull(claimType);
    this.callOptionsFactory = callOptionsFactory;
    this.channel = Preconditions.checkNotNull(channel);
    metrics = new Metrics(appMetrics, claimType);
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
    metrics.calls.mark();
    boolean interrupted = false;
    Exception error = null;
    int processed = 0;

    MessageError.ClaimType type =
        claimType.equalsIgnoreCase("fiss")
            ? MessageError.ClaimType.FISS
            : MessageError.ClaimType.MCS;

    List<MessageError> messageErrors = dao.findAllMessageErrors();

    Set<Long> sequenceNumbers =
        messageErrors.stream()
            .filter(m -> m.getClaimType() == type)
            .map(MessageError::getSequenceNumber)
            .collect(Collectors.toSet());

    if (sequenceNumbers.isEmpty()) {
      log.info("Found no {} claims in DLQ, skipping", claimType);
    } else {
      log.info(
          "Found {} {} claims in DLQ, attempting to reprocess", sequenceNumbers.size(), claimType);

      try {
        setUptimeToRunning();

        final String apiVersion = caller.callVersionService(channel, callOptionsFactory.get());

        for (final long startingSequenceNumber : sequenceNumbers) {
          log.info(
              "calling API for {} claims starting at sequence number {}",
              claimType,
              startingSequenceNumber);

          final GrpcResponseStream<TMessage> responseStream =
              caller.callService(channel, callOptionsFactory.get(), startingSequenceNumber);
          final Map<Object, TMessage> batch = new LinkedHashMap<>();

          try {
            if (responseStream.hasNext()) {
              setUptimeToReceiving();
              final TMessage result = responseStream.next();
              metrics.objectsReceived.mark();

              if (sequencePredicate.test(startingSequenceNumber, result)) {
                batch.put(sink.getDedupKeyForMessage(result), result);
                processed += submitBatchToSink(apiVersion, sink, batch);

                if (processed > 0 && dao.delete(startingSequenceNumber, type) > 0) {
                  log.info(
                      "{} claim with sequence ({}) processed successfully, removed DLQ entry",
                      claimType,
                      startingSequenceNumber);
                }
              }
            }
          } catch (GrpcResponseStream.StreamInterruptedException ex) {
            // If our thread is interrupted we cancel the stream so the server knows we're done
            // and then shut down normally.
            responseStream.cancelStream("shutting down due to InterruptedException");
            interrupted = true;
          } catch (Exception e) {
            // If we failed to process the claim, it stays in the DLQ, nothing to do.
            log.error(
                "Failed to process "
                    + claimType
                    + " message with sequence: "
                    + startingSequenceNumber,
                e);
          }
          sink.shutdown(Duration.ofMinutes(5));
          processed += sink.getProcessedCount();
        }
      } catch (ProcessingException ex) {
        processed += ex.getProcessedCount();
        error = ex;
      } catch (Exception ex) {
        error = ex;
      } finally {
        setUptimeToStopped();
      }

      if (error != null) {
        // InterruptedException isn't really an error, so we exit normally rather than rethrowing.
        if (isInterrupted(error)) {
          interrupted = true;
        } else {
          metrics.failures.mark();
          throw new ProcessingException(error, processed);
        }
      }

      if (interrupted) {
        log.warn("{} claim processing interrupted with processedCount {}", claimType, processed);
      }

      metrics.successes.mark();
    }

    return processed;
  }

  /**
   * Closes the channel used to communicate with the gRPC service.
   *
   * @throws Exception if the channel could not be closed
   */
  @Override
  public void close() throws Exception {
    if (channel != null) {
      if (!channel.isShutdown()) {
        channel.shutdown();
      }
      if (!channel.isTerminated()) {
        channel.awaitTermination(5, TimeUnit.SECONDS);
      }
      channel = null;
    }
  }

  public Metrics getMetrics() {
    return metrics;
  }

  /**
   * Indicates service is running but not actively processing a new record. Called at start of job
   * and when a batch has been written.
   */
  @VisibleForTesting
  void setUptimeToRunning() {
    metrics.uptimeValue.set(10);
  }

  /** Indicates service is actively receiving a batch of data. */
  @VisibleForTesting
  void setUptimeToReceiving() {
    metrics.uptimeValue.set(20);
  }

  /** Indicates service is not running. */
  @VisibleForTesting
  void setUptimeToStopped() {
    metrics.uptimeValue.set(0);
  }

  private int submitBatchToSink(
      String apiVersion, RdaSink<TMessage, TClaim> sink, Map<Object, TMessage> batch)
      throws ProcessingException {
    final int processed = sink.writeMessages(apiVersion, List.copyOf(batch.values()));
    log.debug(
        "submitted batch to sink: type={} size={} processed={}",
        claimType,
        batch.size(),
        processed);
    batch.clear();
    metrics.batches.mark();
    metrics.objectsStored.mark(processed);
    setUptimeToRunning();
    return processed;
  }

  /** Utility Data Access Object (DAO) class for querying the database. */
  @RequiredArgsConstructor
  private static class DLQDao {

    private final EntityManager entityManager;

    public List<MessageError> findAllMessageErrors() {
      return entityManager
          .createQuery("select error from MessageError error", MessageError.class)
          .getResultList();
    }

    public Long delete(Long sequenceNumber, MessageError.ClaimType type) {
      MessageError messageError =
          entityManager.find(MessageError.class, new MessageError.PK(sequenceNumber, type));

      if (messageError != null) {
        entityManager.getTransaction().begin();
        entityManager.remove(messageError);
        entityManager.getTransaction().commit();
        return 1L;
      }

      return 0L;
    }
  }

  /**
   * Metrics are tested in unit tests so they need to be easily accessible from tests. Also this
   * class is used to write both MCS and FISS claims so the metric names need to include a claim
   * type to distinguish them.
   */
  @Getter
  @VisibleForTesting
  static class Metrics {
    /** Number of times the source has been called to retrieve data from the RDA API. */
    private final Meter calls;
    /** Number of calls that successfully called service and stored results. */
    private final Meter successes;
    /** Number of calls that ended in some sort of failure. */
    private final Meter failures;
    /** Number of objects that have been received from the RDA API. */
    private final Meter objectsReceived;
    /**
     * Number of objects that have been successfully stored by the sink. Generally <code>
     * batches * maxPerBatch</code>
     */
    private final Meter objectsStored;
    /**
     * Number of batches/transactions used to store the objects. Generally <code>
     * objectsReceived / maxPerBatch</code>
     */
    private final Meter batches;

    /** Used to provide a metric indicating whether the service is running. */
    private final Gauge<?> uptime;

    /** Holds the value that is reported in the update gauge. */
    private final AtomicLong uptimeValue;

    private Metrics(MetricRegistry appMetrics, String claimType) {
      final String base = MetricRegistry.name(GrpcRdaDLQSource.class.getSimpleName(), claimType);
      calls = appMetrics.meter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.meter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.meter(MetricRegistry.name(base, "failures"));
      objectsReceived = appMetrics.meter(MetricRegistry.name(base, "objects", "received"));
      objectsStored = appMetrics.meter(MetricRegistry.name(base, "objects", "stored"));
      batches = appMetrics.meter(MetricRegistry.name(base, "batches"));
      final String uptimeGaugeName = MetricRegistry.name(base, "uptime");
      uptime = GAUGES.getGaugeForName(appMetrics, uptimeGaugeName);
      uptimeValue = GAUGES.getValueForName(uptimeGaugeName);
    }
  }
}
