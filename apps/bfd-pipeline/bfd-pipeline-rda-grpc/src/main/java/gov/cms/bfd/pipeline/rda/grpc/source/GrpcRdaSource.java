package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;
import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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

  private final GrpcStreamCaller<TResponse> caller;
  private final String claimType;
  private final Optional<Long> startingSequenceNumber;
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
   * @param startingSequenceNumber optional hard coded sequence number
   */
  public GrpcRdaSource(
      Config config,
      GrpcStreamCaller<TResponse> caller,
      MetricRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber) {
    this(
        config.createChannel(),
        caller,
        config::createCallOptions,
        appMetrics,
        claimType,
        startingSequenceNumber);
  }

  /**
   * This constructor accepts a fully constructed channel instead of a configuration object. This is
   * used internally by the primary constructor but is also used by unit tests to allow a mock
   * channel to be provided.
   *
   * @param channel channel used to make RPC calls
   * @param caller the GrpcStreamCaller used to invoke a particular RPC
   * @param appMetrics the MetricRegistry used to track metrics
   * @param startingSequenceNumber optional hard coded sequence number
   */
  @VisibleForTesting
  GrpcRdaSource(
      ManagedChannel channel,
      GrpcStreamCaller<TResponse> caller,
      Supplier<CallOptions> callOptionsFactory,
      MetricRegistry appMetrics,
      String claimType,
      Optional<Long> startingSequenceNumber) {
    this.caller = Preconditions.checkNotNull(caller);
    this.claimType = Preconditions.checkNotNull(claimType);
    this.callOptionsFactory = callOptionsFactory;
    this.channel = Preconditions.checkNotNull(channel);
    this.startingSequenceNumber = Preconditions.checkNotNull(startingSequenceNumber);
    metrics = new Metrics(appMetrics, claimType);
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
    metrics.calls.mark();
    boolean interrupted = false;
    Exception error = null;
    int processed = 0;
    try {
      setUptimeToRunning();
      final long startingSequenceNumber = getStartingSequenceNumber(sink);
      LOGGER.info(
          "calling API for {} claims starting at sequence number {}",
          claimType,
          startingSequenceNumber);
      final GrpcResponseStream<TResponse> responseStream =
          caller.callService(channel, callOptionsFactory.get(), startingSequenceNumber);
      final List<TResponse> batch = new ArrayList<>();
      try {
        while (responseStream.hasNext()) {
          setUptimeToReceiving();
          final TResponse result = responseStream.next();
          metrics.objectsReceived.mark();
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
    } finally {
      setUptimeToStopped();
    }
    if (error != null) {
      // InterruptedException isn't really an error so we exit normally rather than rethrowing.
      if (isInterrupted(error)) {
        interrupted = true;
      } else {
        metrics.failures.mark();
        throw new ProcessingException(error, processed);
      }
    }
    if (interrupted) {
      LOGGER.warn("{} claim processing interrupted with processedCount {}", claimType, processed);
    }
    metrics.successes.mark();
    return processed;
  }

  /**
   * Uses the hard coded starting sequence number if one has been configured. Otherwise asks the
   * sink to provide its maximum known sequence number as our starting point. When all else fails we
   * start at the beginning.
   *
   * @param sink used to obtain the maximum known sequence number
   * @return a valid RDA change sequence number
   */
  private long getStartingSequenceNumber(RdaSink<TResponse> sink) throws ProcessingException {
    if (startingSequenceNumber.isPresent()) {
      return startingSequenceNumber.get();
    } else {
      return sink.readMaxExistingSequenceNumber().map(seqNo -> seqNo + 1).orElse(MIN_SEQUENCE_NUM);
    }
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

  private int submitBatchToSink(RdaSink<TResponse> sink, List<TResponse> batch)
      throws ProcessingException {
    LOGGER.info("submitting batch to sink: type={} size={}", claimType, batch.size());
    int processed = sink.writeBatch(batch);
    LOGGER.info(
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

  /** This class contains the configuration settings specific to the RDA rRPC service. */
  @Getter
  @EqualsAndHashCode
  public static class Config implements Serializable {
    private static final long serialVersionUID = 6667857735839524L;

    /** The type of RDA API server to connect to. */
    private final ServerType serverType;
    /** The hostname or IP address of the host running the RDA API. */
    private final String host;
    /** The port on which the RDA API listens for connections. */
    private final int port;
    /** The name used to connect to an in-process mock RDA API server. */
    private final String inProcessServerName;
    /** The maximum time the stream is allowed to remain idle before being automatically closed. */
    private final Duration maxIdle;
    /** The token to pass to the RDA API server to authenticate the client. */
    @Nullable private final String authenticationToken;

    /**
     * Specifies which type of server we want to connect to. {@code Remote} is the normal
     * configuration. {@code InProcess} is used when populating an environment with synthetic data
     * served by a mock RDA API server running as a pipeline job.
     */
    public enum ServerType {
      Remote,
      InProcess
    }

    @Builder
    private Config(
        ServerType serverType,
        String host,
        int port,
        String inProcessServerName,
        Duration maxIdle,
        String authenticationToken) {
      this.serverType = Preconditions.checkNotNull(serverType, "serverType is required");
      this.host = host;
      this.port = port;
      this.inProcessServerName = inProcessServerName;
      this.maxIdle = maxIdle;
      if (serverType == ServerType.Remote) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(host), "host name is required");
        Preconditions.checkArgument(port >= 1, "port is negative (%s)", port);
      } else {
        Preconditions.checkArgument(
            !Strings.isNullOrEmpty(inProcessServerName), "inProcessServerName is required");
      }
      this.authenticationToken =
          Strings.isNullOrEmpty(authenticationToken) ? null : authenticationToken;
      Preconditions.checkArgument(maxIdle.toMillis() >= 1_000, "maxIdle less than 1 second");
    }

    private ManagedChannel createChannel() {
      return createChannelBuilder()
          .idleTimeout(maxIdle.toMillis(), TimeUnit.MILLISECONDS)
          .enableRetry()
          .build();
    }

    private ManagedChannelBuilder<?> createChannelBuilder() {
      if (serverType == ServerType.InProcess) {
        return createInProcessChannelBuilder();
      } else {
        return createRemoteChannelBuilder();
      }
    }

    /**
     * Creates a CallOptions object for an API call. Will be {@link CallOptions.DEFAULT} if no token
     * has been defined or a BearerToken if one has been defined.
     *
     * @return a valid CallOptions object
     */
    public CallOptions createCallOptions() {
      CallOptions answer = CallOptions.DEFAULT;
      if (authenticationToken != null) {
        answer = answer.withCallCredentials(new BearerToken(authenticationToken));
      } else {
        LOGGER.warn("authenticationToken has not been set - calling server with no token");
      }
      return answer;
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

    private ManagedChannelBuilder<?> createRemoteChannelBuilder() {
      final ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
      if (host.equals("localhost")) {
        builder.usePlaintext();
      }
      return builder;
    }

    private ManagedChannelBuilder<?> createInProcessChannelBuilder() {
      return InProcessChannelBuilder.forName(inProcessServerName);
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
    private final AtomicInteger uptimeValue = new AtomicInteger();

    private Metrics(MetricRegistry appMetrics, String claimType) {
      final String base = MetricRegistry.name(GrpcRdaSource.class.getSimpleName(), claimType);
      calls = appMetrics.meter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.meter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.meter(MetricRegistry.name(base, "failures"));
      objectsReceived = appMetrics.meter(MetricRegistry.name(base, "objects", "received"));
      objectsStored = appMetrics.meter(MetricRegistry.name(base, "objects", "stored"));
      batches = appMetrics.meter(MetricRegistry.name(base, "batches"));
      uptime = appMetrics.gauge(MetricRegistry.name(base, "uptime"), () -> uptimeValue::get);
    }
  }
}
