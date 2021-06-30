package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General PipelineJob instance that delegates the actual ETL work to two other objects. The
 * RdaSource object handles communication with the source of incoming data. The RdaSink object
 * handles communication with the ultimate storage system. The purpose of this class is to handle
 * general PipelineJob semantics that are common to any source or sink.
 *
 * <p>Since the streaming service can run for extended periods of time this class is designed to be
 * reentrant. If multiple threads invoke the call() method at the same time only the first thread
 * will do any work. The other threads will all immediately return with an indication that they have
 * no work to do.
 */
public final class RdaLoadJob<TResponse> implements PipelineJob<NullPipelineJobArguments> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaLoadJob.class);
  public static final String CALLS_METER_NAME =
      MetricRegistry.name(RdaLoadJob.class.getSimpleName(), "calls");
  public static final String FAILURES_METER_NAME =
      MetricRegistry.name(RdaLoadJob.class.getSimpleName(), "failures");
  public static final String SUCCESSES_METER_NAME =
      MetricRegistry.name(RdaLoadJob.class.getSimpleName(), "successes");
  public static final String PROCESSED_METER_NAME =
      MetricRegistry.name(RdaLoadJob.class.getSimpleName(), "processed");

  private final Config config;
  private final Callable<RdaSource<TResponse>> sourceFactory;
  private final Callable<RdaSink<TResponse>> sinkFactory;
  private final Meter callsMeter;
  private final Meter failuresMeter;
  private final Meter successesMeter;
  private final Meter processedMeter;
  // This is used to enforce that this job can only be executed by a single thread at any given
  // time. If multiple threads call the job at the same time only the first will do any work.
  private final Semaphore runningSemaphore;

  public RdaLoadJob(
      Config config,
      Callable<RdaSource<TResponse>> sourceFactory,
      Callable<RdaSink<TResponse>> sinkFactory,
      MetricRegistry appMetrics) {
    this.config = Preconditions.checkNotNull(config);
    this.sourceFactory = Preconditions.checkNotNull(sourceFactory);
    this.sinkFactory = Preconditions.checkNotNull(sinkFactory);
    callsMeter = appMetrics.meter(CALLS_METER_NAME);
    failuresMeter = appMetrics.meter(FAILURES_METER_NAME);
    successesMeter = appMetrics.meter(SUCCESSES_METER_NAME);
    processedMeter = appMetrics.meter(PROCESSED_METER_NAME);
    runningSemaphore = new Semaphore(1);
  }

  @Override
  public PipelineJobOutcome call() throws Exception {
    // We only allow one outstanding call at a time.  If this job is already running any other
    // call to the same job exits immediately with NOTHING_TO_DO.
    if (!runningSemaphore.tryAcquire()) {
      LOGGER.warn("job is already running");
      return NOTHING_TO_DO;
    }
    try {
      final long startMillis = System.currentTimeMillis();
      int processedCount = 0;
      Exception error = null;
      try {
        callsMeter.mark();
        try (RdaSource<TResponse> source = sourceFactory.call();
            RdaSink<TResponse> sink = sinkFactory.call()) {
          processedCount = source.retrieveAndProcessObjects(config.getBatchSize(), sink);
        }
      } catch (ProcessingException ex) {
        processedCount += ex.getProcessedCount();
        error = ex;
      } catch (Exception ex) {
        error = ex;
      }
      processedMeter.mark(processedCount);
      final long stopMillis = System.currentTimeMillis();
      LOGGER.info("processed {} objects in {} ms", processedCount, stopMillis - startMillis);
      if (error != null) {
        failuresMeter.mark();
        LOGGER.error("processing aborted by an exception: message={}", error.getMessage(), error);
        throw new ProcessingException(error, processedCount);
      }
      successesMeter.mark();
      return processedCount == 0 ? NOTHING_TO_DO : PipelineJobOutcome.WORK_DONE;
    } finally {
      runningSemaphore.release();
    }
  }

  /**
   * This job will tend to run for a long time during each execution but has a schedule so that it
   * can be automatically restarted if it exits for any reason. The job detects when it's already
   * running so periodic execution is safe.
   *
   * @return the run interval as a PipelineJobSchedule.
   */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return Optional.of(
        new PipelineJobSchedule(config.getRunInterval().toMillis(), ChronoUnit.MILLIS));
  }

  @Override
  public boolean isInterruptible() {
    return true;
  }

  /** Immutable class containing configuration settings used by the DcGeoRDALoadJob class. */
  public static final class Config implements Serializable {
    private static final long serialVersionUID = 1823137784819917L;

    /**
     * runInterval specifies how often the job should be scheduled. It is used to create a return
     * value for the PipelineJob.getSchedule() method.
     */
    private final Duration runInterval;

    /**
     * batchSize specifies the number of records per batch sent to the RdaSink for processing. This
     * value will likely be tuned for a specific type of sink object and for performance tuning
     * purposes (i.e. finding most efficient transaction size for a specific database).
     */
    private final int batchSize;

    public Config(Duration runInterval, int batchSize) {
      this.runInterval = Preconditions.checkNotNull(runInterval);
      this.batchSize = batchSize;
      Preconditions.checkArgument(runInterval.toMillis() >= 1_000, "scanInterval less than 1s: %s");
      Preconditions.checkArgument(batchSize >= 1, "batchSize less than 1: %s");
    }

    /** @return the expected time between executions of the job by the scheduler */
    public Duration getRunInterval() {
      return runInterval;
    }

    /** @return the number of objects to write to the database per batch/transaction. */
    public int getBatchSize() {
      return batchSize;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Config)) {
        return false;
      }
      Config config = (Config) o;
      return batchSize == config.batchSize && Objects.equals(runInterval, config.runInterval);
    }

    @Override
    public int hashCode() {
      return Objects.hash(runInterval, batchSize);
    }
  }
}
