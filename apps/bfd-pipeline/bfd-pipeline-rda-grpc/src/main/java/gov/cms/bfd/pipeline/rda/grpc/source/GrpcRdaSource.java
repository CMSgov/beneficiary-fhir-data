package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.rda.grpc.ConfigUtils;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General RDASource implementation that delegates actual service call and result mapping to another
 * class. This current implementation is a placeholder to demonstrate the structure that will be
 * used as the RDA API develops to call their RPC's to download Part A and Part B claims.
 *
 * @param <TResponse> type of objects returned by the gRPC service
 */
public class GrpcRdaSource<TResponse> implements RdaSource<TResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRdaSource.class);
  public static final String HOST_PROPERTY = "DCGeoRDAFissClaimsServiceHost";
  public static final String HOST_DEFAULT = "localhost";
  public static final String PORT_PROPERTY = "DCGeoRDAFissClaimsServicePort";
  public static final int PORT_DEFAULT = 443;
  public static final String MAX_IDLE_SECONDS_PROPERTY = "DCGeoRDAFissClaimsServiceMaxIdleSeconds";
  public static final long MAX_IDLE_SECONDS_DEFAULT = Long.MAX_VALUE;

  public static final String CALLS_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "calls");
  public static final String RECORDS_RECEIVED_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "recordsReceived");
  public static final String RECORDS_STORED_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "recordsStored");
  public static final String BATCHES_METER =
      MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), "batches");

  private final GrpcStreamCaller<TResponse> caller;
  private final Meter callsMeter;
  private final Meter recordsReceivedMeter;
  private final Meter recordsStoredMeter;
  private final Meter batchesMeter;
  private ManagedChannel channel;

  public GrpcRdaSource(
      Config config, GrpcStreamCaller<TResponse> caller, MetricRegistry appMetrics) {
    this(
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build(),
        caller,
        appMetrics);
  }

  @VisibleForTesting
  GrpcRdaSource(
      ManagedChannel channel, GrpcStreamCaller<TResponse> caller, MetricRegistry appMetrics) {
    this.caller = Preconditions.checkNotNull(caller);
    this.channel = Preconditions.checkNotNull(channel);
    callsMeter = appMetrics.meter(CALLS_METER);
    recordsReceivedMeter = appMetrics.meter(RECORDS_RECEIVED_METER);
    recordsStoredMeter = appMetrics.meter(RECORDS_STORED_METER);
    batchesMeter = appMetrics.meter(BATCHES_METER);
  }

  /**
   * Calls the service through a specific implementation of GrpcStreamCaller provided to our
   * constructor. Cancels the response stream if reading from the stream is interrupted.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return the number of objects that were successfully processed
   * @throws ProcessingException wrapper around any Exception thrown by the service
   */
  @Override
  public int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TResponse> sink)
      throws ProcessingException {
    callsMeter.mark();
    boolean interrupted = false;
    Exception error = null;
    int processed = 0;
    try {
      final GrpcResponseStream<TResponse> responseStream = caller.callService(channel);
      final List<TResponse> batch = new ArrayList<>();
      try {
        while (responseStream.hasNext()) {
          final TResponse result = responseStream.next();
          recordsReceivedMeter.mark();
          batch.add(result);
          if (batch.size() >= maxPerBatch) {
            processed += submitBatchToSink(sink, batch);
          }
        }
        if (batch.size() > 0) {
          processed += submitBatchToSink(sink, batch);
        }
      } catch (GrpcResponseStream.StreamInterruptedException ex) {
        // If our thread is interrupted we cancel the stream so the server knows we're done
        // and then shut down normally.
        responseStream.cancelStream("shutting down due to InterruptedException");
        interrupted = true;
      }
    } catch (ProcessingException ex) {
      processed += ex.getProcessedCount();
      error = ex.getCause();
    } catch (Exception ex) {
      error = ex;
    }
    if (error != null) {
      // InterruptedException isn't really an error so we exit normally rather than rethrowing.
      if (isInterrupted(error)) {
        interrupted = true;
      } else {
        throw new ProcessingException(error, processed);
      }
    }
    if (interrupted) {
      LOGGER.warn("interrupted with processedCount {}", processed);
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

  private int submitBatchToSink(RdaSink<TResponse> sink, List<TResponse> batch)
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

    public Config() {
      this(
          ConfigUtils.getString(HOST_PROPERTY, HOST_DEFAULT),
          ConfigUtils.getInt(PORT_PROPERTY, PORT_DEFAULT),
          Duration.ofSeconds(
              ConfigUtils.getLong(MAX_IDLE_SECONDS_PROPERTY, MAX_IDLE_SECONDS_DEFAULT)));
    }

    public Config(String host, int port, Duration maxIdle) {
      this.host = Preconditions.checkNotNull(host);
      this.port = port;
      this.maxIdle = maxIdle;
      Preconditions.checkArgument(host.length() >= 1, "host name is empty");
      Preconditions.checkArgument(port >= 1, "port is negative (%s)");
      Preconditions.checkArgument(maxIdle.toMillis() >= 1_000, "maxIdle less than 1 second");
    }
  }
}
