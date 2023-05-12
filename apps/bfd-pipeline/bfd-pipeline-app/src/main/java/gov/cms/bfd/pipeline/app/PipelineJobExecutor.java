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
public class PipelineJobExecutor implements Callable<Void> {
  private final PipelineJob job;
  private final ThrowingFunction<Void, Long, InterruptedException> sleeper;
  private final Clock clock;
  private final Tracker tracker;

  @Override
  public Void call() throws Exception {
    try {
      final var repeatMillis =
          job.getSchedule()
              .map(s -> Duration.of(s.getRepeatDelay(), s.getRepeatDelayUnit()))
              .map(Duration::toMillis)
              .orElse(0L);
      while (tracker.isRunning()) {
        runJob();
        if (repeatMillis <= 0 || !tracker.isRunning()) {
          break;
        }
        tracker.sleeping(job);
        sleeper.apply(repeatMillis);
      }
      tracker.stoppingNormally(job);
    } catch (InterruptedException ex) {
      tracker.stoppingDueToInterrupt(job);
    } catch (Exception ex) {
      tracker.stoppingDueToExecption(job, ex);
    } finally {
      tracker.stopped(job);
    }
    return null;
  }

  private void runJob() throws Exception {
    final var id = tracker.beginningRun(job);
    final var startTime = clock.instant();
    PipelineJobOutcome outcome = null;
    Exception exception = null;
    try {
      outcome = job.call();
    } catch (Exception ex) {
      exception = ex;
    } finally {
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
    }
    // rethrow so that loop can perform local error handling
    if (exception != null) {
      throw exception;
    }
  }

  @Data
  public static class JobRunSummary {
    private static final Pattern SUCCESS_REGEX =
        Pattern.compile(
            String.format("%s \\[id=.*outcome=([^,]+)", JobRunSummary.class.getSimpleName()));
    private static final Pattern FAILURE_REGEX =
        Pattern.compile(
            String.format("%s \\[id=.*failure=([^,]+)", JobRunSummary.class.getSimpleName()));

    private final long id;
    private final PipelineJob job;
    private final Instant startTime;
    private final Instant stopTime;
    private final Optional<PipelineJobOutcome> outcome;
    private final Optional<Exception> exception;

    public static boolean isSuccessString(String logString) {
      var m = SUCCESS_REGEX.matcher(logString);
      return m.find() && !m.group(1).equals(".empty");
    }

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
    boolean isRunning();

    long beginningRun(PipelineJob job);

    void completedRun(JobRunSummary summary);

    void sleeping(PipelineJob job);

    void stoppingDueToInterrupt(PipelineJob job);

    void stoppingDueToExecption(PipelineJob job, Exception error);

    void stoppingNormally(PipelineJob job);

    void stopped(PipelineJob job);
  }
}
