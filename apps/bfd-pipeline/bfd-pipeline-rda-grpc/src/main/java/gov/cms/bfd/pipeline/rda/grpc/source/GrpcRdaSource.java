package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
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
 * General RdaSource implementation that delegates actual service call and result mapping to other
 * objects. This class has no dependency on a particular RPC or data type. It does maintain the
 * ManagedChannel used to communicate with the gRPC service. The constructor creates the channel and
 * the close() method closes the channel.
 *
 * <p>This object makes the RPC call and processes the stream. Stream processing consists of
 * batching received objects and passing them to the RdaSink object for storage. Basic metrics are
 * tracked at this level.
 *
 * @param <TResponse> type of objects returned by the gRPC service
 */
public class GrpcRdaSource<TResponse> implements RdaSource<TResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRdaSource.class);

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

  /**
   * The primary constructor for this class. Constructs a GrpcRdaSource and opens a channel to the
   * gRPC service.
   *
   * @param config the configuration values used to establish the channel
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   */
  public GrpcRdaSource(
      Config config, GrpcStreamCaller<TResponse> caller, MetricRegistry appMetrics) {
    this(
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build(),
        caller,
        appMetrics);
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
   * Calls the service through the specific implementation of GrpcStreamCaller provided to our
   * constructor. Cancels the response stream if reading from the stream is interrupted.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return the number of objects that were successfully processed
   * @throws ProcessingException wrapper around any Exception thrown by the service or sink
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
      error = ex;
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

  /** This class contains the configuration settings specific to the RDA rRPC service. */
  public static class Config {
    private final String host;
    private final int port;
    private final Duration maxIdle;

    public Config(String host, int port, Duration maxIdle) {
      this.host = Preconditions.checkNotNull(host);
      this.port = port;
      this.maxIdle = maxIdle;
      Preconditions.checkArgument(host.length() >= 1, "host name is empty");
      Preconditions.checkArgument(port >= 1, "port is negative (%s)");
      Preconditions.checkArgument(maxIdle.toMillis() >= 1_000, "maxIdle less than 1 second");
    }

    /** @return the hostname or IP address of the host running the RDA API. */
    public String getHost() {
      return host;
    }

    /** @return the port on which the RDA API listens for connections. */
    public int getPort() {
      return port;
    }

    /**
     * Used to specify the maximum amount of time to wait for responses to arrive on the response
     * stream. This is an inter-message time, not an overall connection time. For example if maxIdle
     * is set for five minutes the stream would be kept open forever as long as messages arrive
     * within 5 minutes of each other.
     *
     * @return the maximum idle time for the rRPC service's response stream.
     */
    public Duration getMaxIdle() {
      return maxIdle;
    }
  }
}
