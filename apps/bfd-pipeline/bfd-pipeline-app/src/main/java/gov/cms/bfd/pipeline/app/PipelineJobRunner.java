package gov.cms.bfd.pipeline.app;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Wrapper for a {@link PipelineJob} that runs the job on a schedule. */
@Slf4j
@AllArgsConstructor
public class PipelineJobRunner implements Callable<Void> {
  private final PipelineJob job;
  private final ThrowingFunction<Void, Long, InterruptedException> sleeper;
  private final Clock clock;
  private final Tracker tracker;

  /**
   * Runs the job according to its schedule. If the job has no schedule simply runs the job once.
   * Any uncaught exception thrown by the job terminates the loop. Our status is always updated in
   * the {@link Tracker} so it knows when job runs happen as well as when the loop terminates (and
   * for what reason).
   *
   * @return nothing
   */
  @Override
  public Void call() {
    try {
      final var repeatMillis =
          job.getSchedule()
              .map(s -> Duration.of(s.getRepeatDelay(), s.getRepeatDelayUnit()))
              .map(Duration::toMillis)
              .orElse(0L);
      while (tracker.jobsCanRun()) {
        final var outcome = runJob();
        if (repeatMillis <= 0
            || !tracker.jobsCanRun()
            || outcome == PipelineJobOutcome.INTERRUPTED) {
          break;
        }
        tracker.sleeping(job);
        sleeper.apply(repeatMillis);
      }
      tracker.stoppingNormally(job);
    } catch (InterruptedException ex) {
      tracker.stoppingDueToInterrupt(job);
    } catch (Exception ex) {
      tracker.stoppingDueToException(job, ex);
    } finally {
      tracker.stopped(job);
    }
    return null;
  }

  /**
   * Runs the job once and reports its outcome to the {@link Tracker}.
   *
   * @return the job's outcome
   * @throws Exception passed through if the job terminates with an exception
   */
  private PipelineJobOutcome runJob() throws Exception {
    final var id = tracker.beginningRun(job);
    final var startTime = clock.instant();
    PipelineJobOutcome outcome = null;
    Exception exception = null;
    try {
      outcome = job.call();
    } catch (InterruptedException ex) {
      outcome = PipelineJobOutcome.INTERRUPTED;
    } catch (Exception ex) {
      exception = ex;
    }
    final var stopTime = clock.instant();
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
            String.format("%s \\[id=.*outcome=([^,]+)", JobRunSummary.class.getSimpleName()));
    /** Used to identify log lines indicating job failure. */
    private static final Pattern FAILURE_REGEX =
        Pattern.compile(
            String.format("%s \\[id=.*failure=([^,]+)", JobRunSummary.class.getSimpleName()));

    /** Job id. */
    private final long id;
    /** The job. */
    private final PipelineJob job;
    /** When the run started. */
    private final Instant startTime;
    /** When the run stopped. */
    private final Instant stopTime;
    /** The outcome if job was successful. */
    private final Optional<PipelineJobOutcome> outcome;
    /** The exception if job failed. */
    private final Optional<Exception> exception;

    /**
     * Used by {@link PipelineManagerIT} to detect successful job run log lines.
     *
     * @param logString line from log file
     * @return true if the line indicates a job was successful
     */
    public static boolean isSuccessString(String logString) {
      var m = SUCCESS_REGEX.matcher(logString);
      return m.find() && !m.group(1).equals(".empty");
    }

    /**
     * Used by {@link PipelineManagerIT} to detect failed job run log lines.
     *
     * @param logString line from log file
     * @return true if the line indicates a job failed
     */
    public static boolean isFailureString(String logString) {
      var m = SUCCESS_REGEX.matcher(logString);
      return m.find() && !m.group(1).equals(".empty");
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

  public interface Tracker {
    boolean jobsCanRun();

    long beginningRun(PipelineJob job);

    void completedRun(JobRunSummary summary);

    void sleeping(PipelineJob job);

    void stoppingDueToInterrupt(PipelineJob job);

    void stoppingDueToException(PipelineJob job, Exception error);

    void stoppingNormally(PipelineJob job);

    void stopped(PipelineJob job);
  }
}
