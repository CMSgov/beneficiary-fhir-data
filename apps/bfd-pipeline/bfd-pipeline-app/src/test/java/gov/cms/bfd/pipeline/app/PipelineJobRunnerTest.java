package gov.cms.bfd.pipeline.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import gov.cms.bfd.pipeline.app.PipelineJobRunner.JobRunSummary;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
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
  @Mock private ThrowingFunction<Void, Long, InterruptedException> sleeper;
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
    doReturn(false).when(job).isInterruptible();

    // Time for our tests increases by 1 ms on every call
    final var timestampMillis = new AtomicLong();
    doAnswer(invocation -> Instant.ofEpochMilli(timestampMillis.incrementAndGet()))
        .when(clock)
        .instant();

    // Run ids just start at 1 and increment
    final var runId = new AtomicLong();
    doAnswer(invocation -> runId.incrementAndGet()).when(tracker).beginningRun(job);

    // Sleeping does nothing at all but we can verify it it was called.
    doReturn(null).when(sleeper).apply(anyLong());

    // Collect our summaries into a list
    summaries = new ArrayList<>();
    doAnswer(invocation -> summaries.add(invocation.getArgument(0, JobRunSummary.class)))
        .when(tracker)
        .completedRun(any());

    runner = new PipelineJobRunner(tracker, job, sleeper, clock);
  }

  /**
   * Verifies that a one-time job that finishes successfully works as expected.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void noScheduleJobRunsSuccessfully() throws Exception {
    doReturn(Optional.empty()).when(job).getSchedule();
    doReturn(true).when(tracker).jobsCanRun();

    // Execute the runner.  This should successfully run the job and stop.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker).stoppingNormally(job);
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(job);
    verifyNoInteractions(sleeper);

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
    doReturn(Optional.empty()).when(job).getSchedule();
    doReturn(true).when(tracker).jobsCanRun();

    // Execute the runner.  Simulates job being interrupted.
    doThrow(InterruptedException.class).when(job).call();
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(job);
    verify(tracker).stoppingDueToInterrupt(job);
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(job);
    verifyNoInteractions(sleeper);

    // Verify that the job summary matches expectations
    var expectedSummary =
        new JobRunSummary(
            1L,
            job,
            Instant.ofEpochMilli(1),
            Instant.ofEpochMilli(2),
            Optional.of(PipelineJobOutcome.INTERRUPTED),
            Optional.empty());
    assertEquals(List.of(expectedSummary), summaries);
  }

  /**
   * Verifies that a one-time job that throws an exception works as expected.
   *
   * @throws InterruptedException just a pass through because of a method being mocked or called
   */
  @Test
  void noScheduleJobThrowsExceptionWhileRunning() throws Exception {
    doReturn(Optional.empty()).when(job).getSchedule();
    doReturn(true).when(tracker).jobsCanRun();

    // Execute the runner.  Simulates job throwing an exception.
    final var error = new IOException("boom!");
    doThrow(error).when(job).call();
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker).jobsCanRun();
    verify(tracker).beginningRun(job);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker).stoppingDueToException(job, error);
    verify(tracker, times(0)).sleeping(any());
    verify(tracker).stopped(job);
    verifyNoInteractions(sleeper);

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
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();

    // job can run twice before being told to stop
    doReturn(true, true, true, true, false).when(tracker).jobsCanRun();

    // Execute the runner.  This should successfully run the job and stop.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker, times(5)).jobsCanRun();
    verify(tracker, times(2)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker).stoppingNormally(job);
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(job);
    verify(sleeper, times(2)).apply(repeatMills);

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
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();

    // job can run twice before being told to stop
    doReturn(true).when(tracker).jobsCanRun();

    // Execute the runner.  This should successfully run the job and stop.
    doReturn(PipelineJobOutcome.WORK_DONE).when(job).call();
    doReturn(null).doThrow(InterruptedException.class).when(sleeper).apply(any());
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker, times(4)).jobsCanRun();
    verify(tracker, times(2)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker).stoppingDueToInterrupt(job);
    verify(tracker, times(0)).stoppingDueToException(any(), any());
    verify(tracker).stopped(job);
    verify(sleeper, times(2)).apply(repeatMills);

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
    final var repeatMills = 5_000L;
    doReturn(Optional.of(new PipelineJobSchedule(repeatMills, ChronoUnit.MILLIS)))
        .when(job)
        .getSchedule();
    doReturn(true).when(tracker).jobsCanRun();

    // Execute the runner.  Simulates job throwing an exception on third run.
    final var error = new IOException("boom!");
    doReturn(PipelineJobOutcome.WORK_DONE, PipelineJobOutcome.WORK_DONE)
        .doThrow(error)
        .when(job)
        .call();
    assertNull(runner.call());

    // Verify expected calls were made to the tracker.
    verify(tracker, times(5)).jobsCanRun();
    verify(tracker, times(3)).beginningRun(job);
    verify(tracker, times(2)).sleeping(any());
    verify(tracker, times(0)).stoppingNormally(any());
    verify(tracker, times(0)).stoppingDueToInterrupt(any());
    verify(tracker).stoppingDueToException(job, error);
    verify(tracker).stopped(job);
    verify(sleeper, times(2)).apply(repeatMills);

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
}
