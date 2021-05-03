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
import io.grpc.StatusRuntimeException;
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
 * class. This current implementation is a placeholder to demonstrate the structure that will be
 * used as the RDA API develops to call their RPC's to download Part A and Part B claims.
 *
 * @param <T> type of objects returned by the gRPC service
 */
public class GrpcRdaSource<T> implements RdaSource<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRdaSource.class);
  public static final String CALLS_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "calls");
  public static final String RECORDS_RECEIVED_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "recordsReceived");
  public static final String RECORDS_STORED_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "recordsStored");
  public static final String BATCHES_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "batches");

  private final GrpcStreamCaller.Factory<T> callerFactory;
  private final Meter callsMeter;
  private final Meter recordsReceivedMeter;
  private final Meter recordsStoredMeter;
  private final Meter batchesMeter;
  private ManagedChannel channel;

  public GrpcRdaSource(
      Config config, GrpcStreamCaller.Factory<T> callerFactory, MetricRegistry appMetrics) {
    this(
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .usePlaintext()
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build(),
        callerFactory,
        appMetrics);
  }

  @VisibleForTesting
  GrpcRdaSource(
      ManagedChannel channel,
      GrpcStreamCaller.Factory<T> callerFactory,
      MetricRegistry appMetrics) {
    this.callerFactory = callerFactory;
    this.channel = channel;
    callsMeter = appMetrics.meter(CALLS_METER);
    recordsReceivedMeter = appMetrics.meter(RECORDS_RECEIVED_METER);
    recordsStoredMeter = appMetrics.meter(RECORDS_STORED_METER);
    batchesMeter = appMetrics.meter(BATCHES_METER);
  }

  /**
   * Repeatedly call the service until either our max allowed run time has elapsed or our maximum
   * number of objects have been processed. Calls the service through a specific implementation of
   * GrpcStreamCaller created by the callerFactory.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return the number of objects that were successfully processed
   * @throws ProcessingException wrapper around any Exception thrown by the service
   */
  @Override
  public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<PreAdjudicatedClaim> sink)
      throws ProcessingException {
    callsMeter.mark();
    int processed = 0;
    try {
      final GrpcStreamCaller<T> caller = callerFactory.createCaller(channel);
      final List<PreAdjudicatedClaim> batch = new ArrayList<>();
      final Iterator<T> resultIterator = caller.callService();
      while (resultIterator.hasNext()) {
        final T result = resultIterator.next();
        final PreAdjudicatedClaim claim = caller.convertResultToClaim(result);
        recordsReceivedMeter.mark();
        batch.add(claim);
        if (batch.size() >= maxPerBatch) {
          processed += submitBatchToSink(sink, batch);
        }
      }
      if (batch.size() > 0) {
        processed += submitBatchToSink(sink, batch);
      }
    } catch (StatusRuntimeException ex) {
      // gRPC blocking stub will throw a StatusRuntimeException when it encounters any error.
      // Here we check to see if the underlying cause was an InterruptedException and close down
      // safely if it was since those are normal and expected for pipeline apps.  Any other cause
      // is treated as a fatal error.
      if (ex.getCause() != null && ex.getCause() instanceof InterruptedException) {
        LOGGER.warn("gRPC call was terminated by an InterruptedException");
      } else {
        throw new ProcessingException(ex, processed);
      }
    } catch (ProcessingException ex) {
      throw new ProcessingException(ex.getCause(), processed + ex.getProcessedCount());
    } catch (InterruptedException ex) {
      LOGGER.warn("non-gRPC code was terminated by an InterruptedException");
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
