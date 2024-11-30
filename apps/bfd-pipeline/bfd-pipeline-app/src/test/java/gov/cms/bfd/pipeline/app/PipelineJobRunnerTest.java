package gov.cms.bfd.pipeline.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import gov.cms.bfd.pipeline.app.PipelineJobRunner.JobRunSummary;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link PipelineJobRunner}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PipelineJobRunnerTest {
  /** Mock tracker. */
  @Mock private PipelineJobRunner.Tracker tracker;

  /** Mock job. */
  @Mock private PipelineJob job;

  /** Mock clock. */
  @Mock private Clock clock;

  /** Mock sleep function. */
  @Mock private ThrowingConsumer<Long, InterruptedException> sleeper;

  /** Collects the summaries. */
  private List<JobRunSummary> summaries;

  /** The runner we are testing. */
  private PipelineJobRunner runner;

  /**
   * Sets up common behavior for mocks.
   *
   * @throws InterruptedException just a pass through because of a method being mocked
   */
  @BeforeEach
  void setUp() throws InterruptedException {
    // By default the job is not interruptable.
    doReturn(false).when(job).isInterruptible();

    // The clock used in tests increments time by 1 ms per call.
    final var timestampMillis = new AtomicLong();
    doAnswer(invocation -> Instant.ofEpochMilli(timestampMillis.incrementAndGet()))
        .when(clock)
        .instant();

    // Mock tracker assigns job run ids starting at 1.
    final var runId = new AtomicLong();
    doAnswer(invocation -> runId.incrementAndGet()).when(tracker).beginningRun(job);

    // Sleeping does nothing at all but the mock will allow us to verify if it was called.
    doNothing().when(sleeper).accept(anyLong());

    // Mock tracker collects job run summaries into a list.
    summaries = new ArrayList<>();
    doAnswer(invocation -> summaries.add(invocation.getArgument(0, JobRunSummary.class)))
        .when(tracker)
        .completedRun(any());

    // The runner that we'll be testing.
    runner = new PipelineJobRunner(tracker, job, sleeper, clock);
  }

  /**
   * Verifies that a one-time job that finishes successfully works as expected.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void noScheduleJobRunsSuccessfully() throws Exception {
    // Job has no schedule so it should only run once.
    doReturn(Optional.empty()).when(job).getSchedule();

    // Job will be allowed to run as often as it likes.  It just won't want to
    // because it has no schedule.
    doReturn(true).when(tracker).jobsCanRun();

    // Any time the job runs it will indicate success.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker).stoppingNormally(job);
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(eq(job), any());
    verifyNoInteractions(sleeper);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummary =
        new JobRunSummary(
            1L,
            job,
            Instant.ofEpochMilli(1),
            Instant.ofEpochMilli(2),
            Optional.of(PipelineJobOutcome.WORK_DONE),
            Optional.empty());
    assertEquals(List.of(expectedSummary), summaries);
  }

  /**
   * Verifies that a one-time job that is interrupted while running works as expected.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void noScheduleInterruptedWhileRunningJob() throws Exception {
    // Job has no schedule so it should only run once.
    doReturn(Optional.empty()).when(job).getSchedule();

    // Job will be allowed to run as often as it likes.  However, in this test
    // it will be stopped by the InterruptedException we configure below.
    doReturn(true).when(tracker).jobsCanRun();

    // When the job runs it will throw an InterruptedException.
    final var interrupt = new InterruptedException();
    doThrow(interrupt).when(job).call();

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(job);
    verify(tracker).stoppingDueToInterrupt(job);
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(eq(job), any());
    verifyNoInteractions(sleeper);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummary =
        new JobRunSummary(
            1L,
            job,
            Instant.ofEpochMilli(1),
            Instant.ofEpochMilli(2),
            Optional.empty(),
            Optional.of(interrupt));
    assertEquals(List.of(expectedSummary), summaries);
  }

  /**
   * Verifies that a one-time job that throws an exception works as expected.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void noScheduleJobThrowsExceptionWhileRunning() throws Exception {
    // Job has no schedule so it should only run once.
    doReturn(Optional.empty()).when(job).getSchedule();

    // Job will be allowed to run as often as it likes.  However, in this test
    // it will be stopped by the IOException we configure below.
    doReturn(true).when(tracker).jobsCanRun();

    // Any time the job runs it will thrown an IOException.
    final var error = new IOException("boom!");
    doThrow(error).when(job).call();

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker).stoppingDueToException(job, error);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker).stopped(eq(job), any());
    verifyNoInteractions(sleeper);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummary =
        new JobRunSummary(
            1L,
            job,
            Instant.ofEpochMilli(1),
            Instant.ofEpochMilli(2),
            Optional.empty(),
            Optional.of(error));
    assertEquals(List.of(expectedSummary), summaries);
  }

  /**
   * Verifies that a scheduled job runs and sleeps as expected until told to stop by {@link
   * PipelineJobRunner.Tracker#jobsCanRun}.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void runsUntilStopped() throws Exception {
    // Job has a 5 second schedule.  We'll verify this value was passed to sleeper below.
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();

    // Job can run twice before being told to stop.  Each run of the job calls jobsCanRun
    // twice (at top of loop and in mid-loop if statement) so we mock two runs (allowed by true
    // being returned four times) and a prevented third run (disallowed by false being returned).
    doReturn(true, true, true, true, false).when(tracker).jobsCanRun();

    // Any time the job runs it will indicate success.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker, times(5)).jobsCanRun();
    verify(tracker, times(2)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker).stoppingNormally(job);
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(eq(job), any());
    verify(sleeper, times(2)).accept(repeatMills);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummaries =
        List.of(
            new JobRunSummary(
                1L,
                job,
                Instant.ofEpochMilli(1),
                Instant.ofEpochMilli(2),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()),
            new JobRunSummary(
                2L,
                job,
                Instant.ofEpochMilli(3),
                Instant.ofEpochMilli(4),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()));
    assertEquals(expectedSummaries, summaries);
  }

  /**
   * Verifies that a scheduled job runs and sleeps as expected until it catches an {@link
   * InterruptedException} while sleeping between runs.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void runsUntilInterruptedBetweenRuns() throws Exception {
    // Job has a 5 second schedule.  We'll verify this value was passed to sleeper below.
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();

    // Job will always be allowed to run, but it will be stopped by an InterruptedException
    // thrown by sleeper, which we configure below.
    doReturn(true).when(tracker).jobsCanRun();

    // Any time the job runs it will indicate success.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();

    // First call to sleep will work normally (doNothing) but the second call will
    // throw an InterruptedException.
    doNothing().doThrow(InterruptedException.class).when(sleeper).accept(any());

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker, times(4)).jobsCanRun();
    verify(tracker, times(2)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker).stoppingDueToInterrupt(job);
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(eq(job), any());
    verify(sleeper, times(2)).accept(repeatMills);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummaries =
        List.of(
            new JobRunSummary(
                1L,
                job,
                Instant.ofEpochMilli(1),
                Instant.ofEpochMilli(2),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()),
            new JobRunSummary(
                2L,
                job,
                Instant.ofEpochMilli(3),
                Instant.ofEpochMilli(4),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()));
    assertEquals(expectedSummaries, summaries);
  }

  /**
   * Verifies that a scheduled job runs and sleeps as expected until it throws an exception while
   * running.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void runsUntilThrows() throws Exception {
    // Job has a 5 second schedule.  We'll verify this value was passed to sleeper below.
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();

    // Job will always be allowed to run, but it will be stopped by an IOException
    // thrown by itself, which we configure below.
    doReturn(true).when(tracker).jobsCanRun();

    // First two calls to the job will return success but the third time will throw
    // an IOException.
    final var error = new IOException("boom!");
    doReturn(PipelineJobOutcome.WORK_DONE, PipelineJobOutcome.WORK_DONE)
        .doThrow(error)
        .when(job)
        .call();

    // Mocks all set up - now run the job.
    runner.run();

    // Verify expected calls were made to the tracker.
    verify(tracker, times(5)).jobsCanRun();
    verify(tracker, times(3)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker).stoppingDueToException(job, error);
    verify(tracker).stopped(eq(job), any());
    verify(sleeper, times(2)).accept(repeatMills);
    verify(job).close();

    // Verify that the job summary matches expectations
    var expectedSummaries =
        List.of(
            new JobRunSummary(
                1L,
                job,
                Instant.ofEpochMilli(1),
                Instant.ofEpochMilli(2),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()),
            new JobRunSummary(
                2L,
                job,
                Instant.ofEpochMilli(3),
                Instant.ofEpochMilli(4),
                Optional.of(PipelineJobOutcome.WORK_DONE),
                Optional.empty()),
            new JobRunSummary(
                3L,
                job,
                Instant.ofEpochMilli(5),
                Instant.ofEpochMilli(6),
                Optional.empty(),
                Optional.of(error)));
    assertEquals(expectedSummaries, summaries);
  }

  /** Verify that job run summaries can be recognized in log lines. */
  @Test
  void logMessagesParsedCorrectly() {
    final var successSummary =
        new JobRunSummary(
            1L,
            job,
            Instant.ofEpochMilli(1),
            Instant.ofEpochMilli(2),
            Optional.of(PipelineJobOutcome.WORK_DONE),
            Optional.empty());

    final var error = new IOException("boom!");
    final var failureSummary =
        new JobRunSummary(
            3L,
            job,
            Instant.ofEpochMilli(5),
            Instant.ofEpochMilli(6),
            Optional.empty(),
            Optional.of(error));

    var successMessage = "some prefix" + successSummary + "some suffix";
    assertTrue(JobRunSummary.isSuccessString(successMessage));
    assertFalse(JobRunSummary.isFailureString(successMessage));

    var failureMessage = "some prefix" + failureSummary + "some suffix";
    assertFalse(JobRunSummary.isSuccessString(failureMessage));
    assertTrue(JobRunSummary.isFailureString(failureMessage));
  }
}
