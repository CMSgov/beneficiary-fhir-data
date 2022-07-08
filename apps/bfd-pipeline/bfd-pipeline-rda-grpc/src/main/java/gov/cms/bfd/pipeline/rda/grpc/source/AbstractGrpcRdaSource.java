package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.ProcessingException.isInterrupted;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.NumericGauges;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.RdaSource;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractGrpcRdaSource<TMessage, TClaim>
    implements RdaSource<TMessage, TClaim> {

  /** Holds the underlying value of our uptime gauges. */
  private static final NumericGauges GAUGES = new NumericGauges();

  protected ManagedChannel channel;
  protected final GrpcStreamCaller<TMessage> caller;
  protected final String claimType;
  protected final Supplier<CallOptions> callOptionsFactory;
  @Getter protected final DLQGrpcRdaSource.Metrics metrics;

  protected AbstractGrpcRdaSource(
      ManagedChannel channel,
      GrpcStreamCaller<TMessage> caller,
      String claimType,
      Supplier<CallOptions> callOptionsFactory,
      MetricRegistry appMetrics) {
    this.channel = channel;
    this.caller = caller;
    this.claimType = claimType;
    this.callOptionsFactory = callOptionsFactory;
    this.metrics = new Metrics(getClass(), appMetrics, claimType);
  }

  protected int tryRetrieveAndProcessObjects(SomeInterface logic) throws ProcessingException {
    metrics.getCalls().mark();
    boolean interrupted = false;
    Exception error = null;
    int processed = 0;

    try {
      setUptimeToRunning();
      ProcessResult result = logic.process();
      processed += result.getCount();
      interrupted = result.isWasInterrupted();
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
        metrics.getFailures().mark();
        throw new ProcessingException(error, processed);
      }
    }

    if (interrupted) {
      log.warn("{} claim processing interrupted with processedCount {}", claimType, processed);
    }

    metrics.getSuccesses().mark();
    return processed;
  }

  protected interface SomeInterface {
    ProcessResult process() throws Exception;
  }

  @Data
  protected static class ProcessResult {
    private boolean wasInterrupted = false;
    private int count = 0;
    private Exception exception = null;
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
          channel.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
          log.info("caught InterruptedException while closing ManagedChannel - retrying once");

          try {
            channel.awaitTermination(1, TimeUnit.MINUTES);
          } catch (InterruptedException ex2) {
            log.info(
                "caught second InterruptedException while closing ManagedChannel - calling shutdownNow");
            channel.shutdownNow();
          }
        }
      }

      channel = null;
    }
  }

  /**
   * Indicates service is running but not actively processing a new record. Called at start of job
   * and when a batch has been written.
   */
  protected void setUptimeToRunning() {
    metrics.uptimeValue.set(10);
  }

  /** Indicates service is actively receiving a batch of data. */
  protected void setUptimeToReceiving() {
    metrics.uptimeValue.set(20);
  }

  /** Indicates service is not running. */
  protected void setUptimeToStopped() {
    metrics.uptimeValue.set(0);
  }

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
    metrics.batches.mark();
    metrics.objectsStored.mark(processed);
    setUptimeToRunning();
    return processed;
  }

  /**
   * Metrics are tested in unit tests so they need to be easily accessible from tests. Also this
   * class is used to write both MCS and FISS claims so the metric names need to include a claim
   * type to distinguish them.
   */
  @Getter
  @VisibleForTesting
  protected static class Metrics {
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

    private Metrics(Class<?> baseClass, MetricRegistry appMetrics, String claimType) {
      final String base = MetricRegistry.name(baseClass.getSimpleName(), claimType);
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
