package gov.cms.bfd.pipeline.app;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper for a {@link PipelineJob} that runs the job on a schedule. Implements {@link Runnable} so
 * it can be submitted to an {@link java.util.concurrent.ExecutorService}.
 */
@Slf4j
@RequiredArgsConstructor
public class PipelineJobRunner implements Runnable {
  /** Object that tracks the status of all job runs. */
  private final Tracker tracker;

  /** The job we run. */
  private final PipelineJob job;

  /** Function used to sleep. Parameterized for use by unit tests. */
  private final ThrowingConsumer<Long, InterruptedException> sleeper;

  /** Used to get timestamps. Parameterized for use by unit tests. */
  private final Clock clock;

  /** Terminate requested. */
  private boolean terminateRequested = false;

  /**
   * Runs the job according to its schedule. If the job has no schedule simply runs the job once.
   * Any uncaught exception thrown by the job terminates the loop. Our status is always updated in
   * the {@link Tracker} so it knows when job runs happen as well as when the loop terminates (and
   * for what reason).
   */
  @Override
  public void run() {
    PipelineJobOutcome outcome = null;
    try {
      // This try-with-resources guarantees job's close method is called.
      // Nested within the outer try because we don't want stopping normally to be
      // called if closing the job throws an exception.
      try (job) {
        final Long repeatMillis =
            job.getSchedule()
                .map(s -> Duration.of(s.getRepeatDelay(), s.getRepeatDelayUnit()))
                .map(Duration::toMillis)
                .orElse(0L);
        while (tracker.jobsCanRun()) {
          outcome = runJob();
          boolean shouldTerminate = outcome == PipelineJobOutcome.SHOULD_TERMINATE;
          if (shouldTerminate) {
            this.terminateRequested = true;
          }
          if (this.terminateRequested || repeatMillis <= 0 || !tracker.jobsCanRun()) {
            break;
          }
          tracker.sleeping(job);
          sleeper.accept(repeatMillis);
        }
      }
      tracker.stoppingNormally(job);
    } catch (InterruptedException ex) {
      tracker.stoppingDueToInterrupt(job);
    } catch (Exception ex) {
      tracker.stoppingDueToException(job, ex);
    } finally {
      tracker.stopped(job, outcome);
    }
  }

  /**
   * Runs the job once and reports its outcome to the {@link Tracker}.
   *
   * @return PipelineJobOutcome outcome
   * @throws Exception passed through if the job terminates with an exception
   */
  private PipelineJobOutcome runJob() throws Exception {
    final long id = tracker.beginningRun(job);
    final Instant startTime = clock.instant();
    PipelineJobOutcome outcome = null;
    Exception exception = null;
    try {
      outcome = job.call();
    } catch (Exception ex) {
      exception = ex;
    }
    final Instant stopTime = clock.instant();
    // one must be null and the other not null
    assert (outcome == null) != (exception == null);
    tracker.completedRun(
        new JobRunSummary(
            id,
            job,
            startTime,
            stopTime,
            Optional.ofNullable(outcome),
            Optional.ofNullable(exception)));

    // rethrow so that loop can perform local error handling
    if (exception != null) {
      throw exception;
    }

    return outcome;
  }

  /** Summarizes the results of a job run. */
  @Data
  public static class JobRunSummary {
    /** Used to identify log lines indicating job success. */
    private static final Pattern SUCCESS_REGEX =
        Pattern.compile(
            String.format("%s \\[id=\\d+,.*outcome=([^,]+)", JobRunSummary.class.getSimpleName()));

    /** Used to identify log lines indicating job failure. */
    private static final Pattern FAILURE_REGEX =
        Pattern.compile(
            String.format(
                "%s \\[id=\\d+,.*failure=([^\\]]+)", JobRunSummary.class.getSimpleName()));

    /** Id for this job run. Assigned by {@link Tracker#beginningRun}. */
    private final long id;

    /** The job. */
    private final PipelineJob job;

    /** When the run started. */
    private final Instant startTime;

    /** When the run stopped. */
    private final Instant stopTime;

    /** The outcome if run was successful. */
    private final Optional<PipelineJobOutcome> outcome;

    /** The exception if run failed. */
    private final Optional<Exception> exception;

    /**
     * Used by integration tests to detect successful job run log lines.
     *
     * @param logString line from log file to check
     * @return true if the line indicates a job was successful
     */
    public static boolean isSuccessString(String logString) {
      var m = SUCCESS_REGEX.matcher(logString);
      return m.find() && !m.group(1).equals("Optional.empty");
    }

    /**
     * Used by integration tests to detect failed job run log lines.
     *
     * @param logString line from log file to check
     * @return true if the line indicates a job failed
     */
    public static boolean isFailureString(String logString) {
      var m = FAILURE_REGEX.matcher(logString);
      return m.find() && !m.group(1).equals("Optional.empty");
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append(getClass().getSimpleName())
          .append(" [id=")
          .append(id)
          .append(", jobType=")
          .append(job.getType())
          .append(", startedTime=")
          .append(startTime)
          .append(", completedTime=")
          .append(stopTime)
          .append(", outcome=")
          .append(outcome)
          .append(", failure=")
          .append(exception)
          .append("]")
          .toString();
    }
  }

  /** Interface for objects that manage {@link PipelineJobRunner} instances. */
  public interface Tracker {
    /**
     * Callable to determine if it is ok to run the job again. Used to allow jobs to shutdown
     * cleanly when any job fails or the pipeline app is shutting down.
     *
     * @return true if it's ok to run again
     */
    boolean jobsCanRun();

    /**
     * Notifies the tracker that a new job run is starting. Return value is a unique id assigned to
     * this job run.
     *
     * @param job the job that is starting
     * @return unique id for this run
     */
    long beginningRun(PipelineJob job);

    /**
     * Notifies the tracker that a job has completed and the outcome of the run.
     *
     * @param summary summaries the outcome of the run
     */
    void completedRun(JobRunSummary summary);

    /**
     * Notifies the tracker that a job is sleeping between runs.
     *
     * @param job the job that is sleeping
     */
    void sleeping(PipelineJob job);

    /**
     * Notifies the tracker that a job is stopping because it caught an {@link InterruptedException}
     * while waiting to run again or while the job was running.
     *
     * @param job the job that is stopping
     */
    void stoppingDueToInterrupt(PipelineJob job);

    /**
     * Notifies the tracker that a job is stopping because it threw an exception during a run.
     *
     * @param job the job that is stopping
     * @param error the exception that was thrown
     */
    void stoppingDueToException(PipelineJob job, Exception error);

    /**
     * Notifies the tracker that a job is stopping because it has completed a run and doesn't have a
     * schedule for running multiple times or was told not to run again by {@link #jobsCanRun}.
     *
     * @param job the job that is stopping
     */
    void stoppingNormally(PipelineJob job);

    /**
     * Notifies the tracker that a job has stopped.
     *
     * @param job the job that has stopped
     * @param outcome job outcome
     */
    void stopped(PipelineJob job, PipelineJobOutcome outcome);
  }
}
