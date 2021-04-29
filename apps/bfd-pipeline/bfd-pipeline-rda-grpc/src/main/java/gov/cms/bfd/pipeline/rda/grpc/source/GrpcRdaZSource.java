package gov.cms.bfd.pipeline.rda.grpc.source;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General RDASource implementation that delegates actual service call and result mapping to another
 * class.
 *
 * @param <T> type of objects returned by the gRPC service
 */
public class GrpcRdaZSource<T> implements RdaSource<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRdaZSource.class);
  public static final String CALLS_METER =
      MetricRegistry.name(GrpcRdaZSource.class.getSimpleName(), "calls");
  public static final String RECORDS_RECEIVED_METER =
      MetricRegistry.name(GrpcRdaZSource.class.getSimpleName(), "recordsReceived");
  public static final String RECORDS_STORED_METER =
      MetricRegistry.name(GrpcRdaZSource.class.getSimpleName(), "recordsStored");
  public static final String BATCHES_METER =
      MetricRegistry.name(GrpcRdaZSource.class.getSimpleName(), "batches");

  private final GrpcStreamCaller.Factory<T> callerFactory;
  private final Clock clock;
  private final Meter callsMeter;
  private final Meter recordsReceivedMeter;
  private final Meter recordsStoredMeter;
  private final Meter batchesMeter;
  private ManagedChannel channel;

  public GrpcRdaZSource(
      Config config, GrpcStreamCaller.Factory<T> callerFactory, MetricRegistry appMetrics) {
    this(
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .usePlaintext()
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build(),
        callerFactory,
        Clock.systemDefaultZone(),
        appMetrics);
  }

  @VisibleForTesting
  GrpcRdaZSource(
      ManagedChannel channel,
      GrpcStreamCaller.Factory<T> callerFactory,
      Clock clock,
      MetricRegistry appMetrics) {
    this.callerFactory = callerFactory;
    this.channel = channel;
    this.clock = clock;
    callsMeter = appMetrics.meter(CALLS_METER);
    recordsReceivedMeter = appMetrics.meter(RECORDS_RECEIVED_METER);
    recordsStoredMeter = appMetrics.meter(RECORDS_STORED_METER);
    batchesMeter = appMetrics.meter(BATCHES_METER);
  }

  @Override
  public int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RdaSink<PreAdjudicatedClaim> sink)
      throws ProcessingException {
    callsMeter.mark();
    int processed = 0;
    try {
      final GrpcStreamCaller<T> caller = callerFactory.createCaller(channel);
      final List<PreAdjudicatedClaim> batch = new ArrayList<>();
      Instant now = clock.instant();
      final Instant stopTime = now.plus(maxRunTime);
      while (shouldContinue(processed, maxToProcess, now, stopTime)) {
        final Iterator<T> resultIterator = caller.callService(Duration.between(now, stopTime));
        now = clock.instant();
        while (resultIterator.hasNext() && shouldContinue(processed, maxToProcess, now, stopTime)) {
          final T result = resultIterator.next();
          final PreAdjudicatedClaim claim = caller.convertResultToClaim(result);
          recordsReceivedMeter.mark();
          batch.add(claim);
          if (batch.size() >= maxPerBatch) {
            processed += submitBatchToSink(sink, batch);
          }
          now = clock.instant();
        }
        if (batch.size() > 0) {
          processed += submitBatchToSink(sink, batch);
        }
      }
    } catch (ProcessingException ex) {
      throw new ProcessingException(ex.getCause(), processed + ex.getProcessedCount());
    } catch (Exception ex) {
      throw new ProcessingException(ex, processed);
    }
    return processed;
  }

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

  private boolean shouldContinue(int processed, int maxToProcess, Instant now, Instant stopTime) {
    if (processed >= maxToProcess) {
      LOGGER.info("exiting loop after processing max number of records: processed={}", processed);
      return false;
    }
    if (now.compareTo(stopTime) >= 0) {
      LOGGER.info("exiting loop after reaching max runtime");
      return false;
    }
    return true;
  }

  private int submitBatchToSink(RdaSink<PreAdjudicatedClaim> sink, List<PreAdjudicatedClaim> batch)
      throws ProcessingException {
    LOGGER.info("submitting batch to sink: size={}", batch.size());
    int processed = sink.writeBatch(batch);
    LOGGER.info("submitted batch to sink: size={} processed={}", batch.size(), processed);
    batch.clear();
    batchesMeter.mark();
    recordsStoredMeter.mark(processed);
    return processed;
  }

  public static class Config {
    private final String host;
    private final int port;
    private final Duration maxIdle;

    public Config(String host, int port, Duration maxIdle) {
      this.host = host;
      this.port = port;
      this.maxIdle = maxIdle;
    }
  }
}
