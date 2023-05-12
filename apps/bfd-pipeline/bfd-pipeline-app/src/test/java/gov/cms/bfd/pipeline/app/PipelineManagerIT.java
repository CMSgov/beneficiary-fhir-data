package gov.cms.bfd.pipeline.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link PipelineManager}. */
public final class PipelineManagerIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineManagerIT.class);

  private static final ThrowingFunction<Void, Long, InterruptedException> SLEEPER =
      millis -> {
        Thread.sleep(Math.min(10, millis));
        return null;
      };

  private final Clock clock = Clock.systemUTC();

  /** Verifies that {@link PipelineManager} runs a successful mock one-shot job, as expected. */
  @Test
  public void runSuccessfulMockOneshotJob() {
    // Since this has no schedule it will run once and then exit.
    MockJob mockJob = new MockJob(Optional.empty(), () -> PipelineJobOutcome.WORK_DONE);

    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();
    pipelineManager.awaitCompletion();

    assertEquals(1, pipelineManager.getCompletedJobs().size());
    var jobSummary = pipelineManager.getCompletedJobs().get(0);
    assertEquals(Optional.of(PipelineJobOutcome.WORK_DONE), jobSummary.getOutcome());
    assertEquals(Optional.empty(), jobSummary.getException());
  }

  /** Verifies that {@link PipelineManager} runs a failing mock one-shot job, as expected. */
  @Test
  public void runFailingMockOneshotJob() {
    final var error = new RuntimeException("boom");

    // Since this has no schedule it will run once and then exit.
    MockJob mockJob =
        new MockJob(
            Optional.empty(),
            () -> {
              throw error;
            });

    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();
    pipelineManager.awaitCompletion();

    assertEquals(1, pipelineManager.getCompletedJobs().size());
    var jobSummary = pipelineManager.getCompletedJobs().get(0);
    assertEquals(Optional.empty(), jobSummary.getOutcome());
    assertEquals(Optional.of(error), jobSummary.getException());
  }

  /**
   * Verifies that {@link PipelineManager} runs a successful mock scheduled job, as expected.
   *
   * @throws Exception Any unhandled {@link Exception}s will cause this test case to fail.
   */
  @Test
  public void runSuccessfulScheduledJob() {
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(10, ChronoUnit.MILLIS)),
            () -> PipelineJobOutcome.WORK_DONE);
    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();

    // Wait until a completed iteration of the mock job can be found.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                pipelineManager.getCompletedJobs().stream()
                    .anyMatch(j -> MockJob.JOB_TYPE.equals(j.getJob().getType())));

    pipelineManager.stop();
    pipelineManager.awaitCompletion();

    // Verify that one of the completed mock job iterations looks correct.
    Optional<PipelineJobRunner.JobRunSummary> mockJobSummary =
        pipelineManager.getCompletedJobs().stream()
            .filter(
                j -> MockJob.JOB_TYPE.equals(j.getJob().getType()) && j.getOutcome().isPresent())
            .findAny();

    assertTrue(mockJobSummary.isPresent());
    assertEquals(Optional.of(PipelineJobOutcome.WORK_DONE), mockJobSummary.get().getOutcome());
  }

  /** Verifies that {@link PipelineManager} runs a failing mock scheduled job, as expected. */
  @Test
  public void runFailingScheduledJob() {
    final var error = new RuntimeException("boom");

    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(10, ChronoUnit.MILLIS)),
            () -> {
              throw error;
            });

    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();
    pipelineManager.awaitCompletion();

    assertEquals(1, pipelineManager.getCompletedJobs().size());
    var jobSummary = pipelineManager.getCompletedJobs().get(0);
    assertEquals(Optional.empty(), jobSummary.getOutcome());
    assertEquals(Optional.of(error), jobSummary.getException());
    assertEquals(error, pipelineManager.getError());
  }

  /**
   * Verifies that {@link PipelineManager#stop()} works, as expected.
   *
   * @throws Exception pass through if test throws
   */
  @Test
  public void runInterruptableJobsThenStop() throws Exception {
    final var latch = new CountDownLatch(1);

    MockJob mockJob =
        new MockJob(
            Optional.empty(),
            () -> {
              // sync up with the test thread
              latch.countDown();

              // Sleep that will be interrupted
              Thread.sleep(10_000);
              return PipelineJobOutcome.WORK_DONE;
            });

    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();

    // wait until we know the job has started
    latch.await();

    // stop the job before it can finish
    pipelineManager.stop();

    // wait for the job to finish
    pipelineManager.awaitCompletion();

    // verify the job was interrupted
    assertEquals(1, pipelineManager.getCompletedJobs().size());
    var jobSummary = pipelineManager.getCompletedJobs().get(0);
    assertEquals(Optional.of(PipelineJobOutcome.INTERRUPTED), jobSummary.getOutcome());
    assertEquals(Optional.empty(), jobSummary.getException());
    assertNull(pipelineManager.getError());
  }

  /** Verifies that {@link PipelineManager#stop()} works with uninterruptible jobs. */
  @Test
  public void runUninterruptibleJobsThenStop() {
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            false,
            () -> PipelineJobOutcome.WORK_DONE);
    PipelineManager pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob));
    pipelineManager.start();

    // Wait until the mock job has started.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                pipelineManager.getCompletedJobs().stream()
                    .anyMatch(j -> MockJob.JOB_TYPE.equals(j.getJob().getType())));

    // Stop the pipeline. If this doesn't hang, we're good.
    pipelineManager.stop();
    pipelineManager.awaitCompletion();
  }

  /** This mock {@link PipelineJob} returns a specified result. */
  private static class MockJob implements PipelineJob {
    /** Represents the job type for this mock job. */
    public static final PipelineJobType JOB_TYPE = new PipelineJobType(MockJob.class);

    /** The pipeline job schedule for the mock job. */
    private final Optional<PipelineJobSchedule> schedule;
    /** Represents if this job can be interrupted. */
    private final boolean interruptible;
    /** The {@link Callable} that will create the values to use for {@link #call()}. */
    private final Callable<Object> jobResultProducer;

    /**
     * Constructs a new {@link MockJob} instance.
     *
     * @param schedule the value to use for {@link #getSchedule()}
     * @param interruptible the value to use for {@link #isInterruptible()}
     * @param jobResultProducer the {@link Callable} that will create the values to use for {@link
     *     #call()}
     */
    public MockJob(
        Optional<PipelineJobSchedule> schedule,
        boolean interruptible,
        Callable<Object> jobResultProducer) {
      this.schedule = schedule;
      this.interruptible = interruptible;
      this.jobResultProducer = jobResultProducer;
    }

    /**
     * Constructs a new {@link MockJob} instance.
     *
     * @param schedule the value to use for {@link #getSchedule()}
     * @param jobResultProducer the {@link Callable} that will create the values to use for {@link
     *     #call()}
     */
    public MockJob(Optional<PipelineJobSchedule> schedule, Callable<Object> jobResultProducer) {
      this(schedule, true, jobResultProducer);
    }

    @Override
    public Optional<PipelineJobSchedule> getSchedule() {
      return schedule;
    }

    @Override
    public boolean isInterruptible() {
      return interruptible;
    }

    @Override
    public PipelineJobOutcome call() throws Exception {
      Object result = jobResultProducer.call();
      if (result instanceof PipelineJobOutcome) {
        return (PipelineJobOutcome) result;
      } else {
        throw new BadCodeMonkeyException();
      }
    }
  }
}
