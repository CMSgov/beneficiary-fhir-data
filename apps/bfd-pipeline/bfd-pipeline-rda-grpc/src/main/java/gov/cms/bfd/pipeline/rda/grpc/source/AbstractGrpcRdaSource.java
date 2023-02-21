package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.NumericGauges;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines common gRPC RDA Source logic, taking care of basic resource and error handling.
 *
 * @param <TMessage> The type of message object received from the source
 * @param <TClaim> The type of claim object generated from the source message object data
 */
@Slf4j
public abstract class AbstractGrpcRdaSource<TMessage, TClaim>
    implements RdaSource<TMessage, TClaim> {

  /** Holds the underlying value of our uptime gauges. */
  private static final NumericGauges GAUGES = new NumericGauges();

  /** The {@link ManagedChannel} the source messages will be streamed on. */
  protected ManagedChannel channel;
  /** Client for calling the remote RDA gRPC service. */
  protected final GrpcStreamCaller<TMessage> caller;
  /** The type of claim being read from the source (i.e. FISS/MCS). */
  protected final String claimType;
  /** Factory for creating {@link CallOptions}. */
  protected final Supplier<CallOptions> callOptionsFactory;
  /** Metrics for doing later application and processing analysis. */
  @Getter protected final DLQGrpcRdaSource.Metrics metrics;

  /**
   * Instantiates a new abstract grpc rda source.
   *
   * @param channel the channel
   * @param caller the caller
   * @param claimType the claim type
   * @param callOptionsFactory the call options factory
   * @param appMetrics the app metrics
   */
  protected AbstractGrpcRdaSource(
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      String claimType,
      Supplier<CallOptions> callOptionsFactory,
      MeterRegistry appMetrics) {
    this.channel = channel;
    this.caller = caller;
    this.claimType = claimType;
    this.callOptionsFactory = callOptionsFactory;
    this.metrics = new Metrics(getClass(), appMetrics, claimType);
  }

  /**
   * Method to perform basic error handling when executing the given {@link Processor} logic.
   *
   * @param logic The logic to execute to retrieve and process objects.
   * @return The number of objects that were processed successfully.
   * @throws ProcessingException If there was an issue processing the objects.
   */
  protected int tryRetrieveAndProcessObjects(Processor logic) throws ProcessingException {
    metrics.getCalls().increment();
    boolean interrupted = false;
    Exception error = null;
    int processed = 0;

    try {
      setUptimeToRunning();
      ProcessResult result = logic.process();
      processed += result.getCount();
      interrupted = result.isInterrupted();
      error = result.getException();
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
        metrics.getFailures().increment();
        throw new ProcessingException(error, processed);
      }
    }

    if (interrupted) {
      log.warn("{} claim processing interrupted with processedCount {}", claimType, processed);
    }

    metrics.getSuccesses().increment();
    return processed;
  }

  /** Functional interface to define logic to be executed. */
  @FunctionalInterface
  protected interface Processor {
    /**
     * Consumer defined process that executes logic and returns a {@link ProcessResult}.
     *
     * @return A {@link ProcessResult} with details of the execution result.
     * @throws Exception If the processing encounters an unexpected issue.
     */
    ProcessResult process() throws Exception;
  }

  /** Data class for holding processing results. */
  @Data
  protected static class ProcessResult {
    /** If this process is interrupted. */
    private boolean interrupted = false;
    /** The count of results. */
    private int count = 0;
    /** Holds any unexpected issue with the processing. */
    private Exception exception = null;

    /**
     * Add to the current {@link ProcessResult#count}.
     *
     * @param count The amount to add to the current count.
     */
    public void addCount(int count) {
      this.count += count;
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
        try {
          log.info("waiting for channel to terminate");
          channel.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
          log.info("caught InterruptedException while closing ManagedChannel - retrying once");

          try {
            channel.awaitTermination(1, TimeUnit.MINUTES);
          } catch (InterruptedException ex2) {
            log.info("caught second InterruptedException while closing ManagedChannel");
          }
        }
        log.info("finished waiting for channel to terminate");
      }

      channel = null;
    }
  }

  /**
   * Indicates service is running but not actively processing a new record. Called at start of job
   * and when a batch has been written.
   */
  protected void setUptimeToRunning() {
    metrics.uptime.set(10);
  }

  /** Indicates service is actively receiving a batch of data. */
  protected void setUptimeToReceiving() {
    metrics.uptime.set(20);
  }

  /** Indicates service is not running. */
  protected void setUptimeToStopped() {
    metrics.uptime.set(0);
  }

  /**
   * Submit a batch of message objects to be written to the given {@link RdaSink}.
   *
   * @param apiVersion The version of the RDA API source the message was received from.
   * @param sink The {@link RdaSink} to write the batch of messages to.
   * @param batch The batch of messages from the RDA source to write to the {@link RdaSink}.
   * @return The number of messages that were successfully written to the given {@link RdaSink}.
   * @throws ProcessingException If there was an issue processing a message in the batch or writing
   *     to the given sink.
   */
  protected int submitBatchToSink(
      String apiVersion, RdaSink<TMessage, TClaim> sink, Map<Object, TMessage> batch)
      throws ProcessingException {
    final int processed = sink.writeMessages(apiVersion, List.copyOf(batch.values()));
    log.debug(
        "submitted batch to sink: type={} size={} processed={}",
        claimType,
        batch.size(),
        processed);
    batch.clear();
    metrics.batches.increment();
    metrics.objectsStored.increment(processed);
    setUptimeToRunning();
    return processed;
  }

  /**
   * Metrics are tested in unit tests, so they need to be easily accessible from tests. Also, this
   * class is used to write both MCS and FISS claims so the metric names need to include a claim
   * type to distinguish them.
   */
  @Getter
  @VisibleForTesting
  protected static class Metrics {
    /** Number of times the source has been called to retrieve data from the RDA API. */
    private final Counter calls;
    /** Number of calls that successfully called service and stored results. */
    private final Counter successes;
    /** Number of calls that ended in some sort of failure. */
    private final Counter failures;
    /** Number of objects that have been received from the RDA API. */
    private final Counter objectsReceived;
    /**
     * Number of objects that have been successfully stored by the sink. Generally <code>
     * batches * maxPerBatch</code>
     */
    private final Counter objectsStored;
    /**
     * Number of batches/transactions used to store the objects. Generally <code>
     * objectsReceived / maxPerBatch</code>
     */
    private final Counter batches;

    /** Holds the value that is reported in the update gauge. */
    private final AtomicLong uptime;

    /**
     * Constructor to create a Metrics object.
     *
     * @param baseClass The class the {@link Metrics} object is being created for.
     * @param appMetrics The {@link MetricRegistry} used to create the needed metrics tools.
     * @param claimType The type of claim this {@link Metrics} object will gather metrics for.
     */
    private Metrics(Class<?> baseClass, MeterRegistry appMetrics, String claimType) {
      final String base = MetricRegistry.name(baseClass.getSimpleName(), claimType);
      calls = appMetrics.counter(MetricRegistry.name(base, "calls"));
      successes = appMetrics.counter(MetricRegistry.name(base, "successes"));
      failures = appMetrics.counter(MetricRegistry.name(base, "failures"));
      objectsReceived = appMetrics.counter(MetricRegistry.name(base, "objects", "received"));
      objectsStored = appMetrics.counter(MetricRegistry.name(base, "objects", "stored"));
      batches = appMetrics.counter(MetricRegistry.name(base, "batches"));
      final String uptimeGaugeName = MetricRegistry.name(base, "uptime");
      uptime = GAUGES.getGaugeForName(appMetrics, uptimeGaugeName);
    }
  }
}
