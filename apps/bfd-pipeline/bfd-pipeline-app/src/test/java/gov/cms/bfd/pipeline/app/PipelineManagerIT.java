package gov.cms.bfd.pipeline.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.ec2.AwsEc2Client;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.interfaces.ThrowingConsumer;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** Integration tests for {@link PipelineManager}. */
public final class PipelineManagerIT {
  /** Sleep function that keeps the sleep time short for testing. */
  private static final ThrowingConsumer<Long, InterruptedException> SLEEPER =
      millis -> Thread.sleep(Math.min(5, millis));

  /** We don't care about timestamps in these tests so we can just use system clock. */
  private final Clock clock = Clock.systemUTC();

  /** Mock EC2 client. */
  @Mock private AwsEc2Client ec2Client;

  /** Verifies that {@link PipelineManager} runs a successful mock one-shot job, as expected. */
  @Test
  public void runSuccessfulMockOneshotJob() {
    // Since this has no schedule it will run once and then exit.
    final var mockJob = new MockJob(Optional.empty(), true, () -> PipelineJobOutcome.WORK_DONE);

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
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
    final var mockJob =
        new MockJob(
            Optional.empty(),
            true,
            () -> {
              throw error;
            });

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
    pipelineManager.start();
    pipelineManager.awaitCompletion();

    assertEquals(1, pipelineManager.getCompletedJobs().size());
    var jobSummary = pipelineManager.getCompletedJobs().get(0);
    assertEquals(Optional.empty(), jobSummary.getOutcome());
    assertEquals(Optional.of(error), jobSummary.getException());
    assertEquals(error, pipelineManager.getError());
  }

  /** Verifies that {@link PipelineManager} runs a successful mock scheduled job, as expected. */
  @Test
  public void runSuccessfulScheduledJob() {
    final var mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(10, ChronoUnit.MILLIS)),
            true,
            () -> PipelineJobOutcome.WORK_DONE);

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
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
    assertNull(pipelineManager.getError());
  }

  /**
   * Verifies that {@link PipelineManager} runs a mock scheduled job until it fails and them shuts
   * down, as expected.
   */
  @Test
  public void runFailingScheduledJob() {
    final var error = new RuntimeException("boom");

    final var runCount = new AtomicInteger();
    final var mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(10, ChronoUnit.MILLIS)),
            true,
            () -> {
              if (runCount.incrementAndGet() >= 3) {
                throw error;
              } else {
                return PipelineJobOutcome.WORK_DONE;
              }
            });

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
    pipelineManager.start();
    pipelineManager.awaitCompletion();

    final var summaries = pipelineManager.getCompletedJobs();
    assertEquals(3, summaries.size());

    var jobSummary = summaries.get(0);
    assertEquals(Optional.of(PipelineJobOutcome.WORK_DONE), jobSummary.getOutcome());
    assertEquals(Optional.empty(), jobSummary.getException());

    jobSummary = summaries.get(1);
    assertEquals(Optional.of(PipelineJobOutcome.WORK_DONE), jobSummary.getOutcome());
    assertEquals(Optional.empty(), jobSummary.getException());

    jobSummary = summaries.get(2);
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
    // lets the main thread know the job has started
    final var latch = new CountDownLatch(1);

    final var mockJob =
        new MockJob(
            Optional.empty(),
            true,
            () -> {
              // sync up with the test thread
              latch.countDown();

              // Sleep that will be interrupted
              Thread.sleep(10_000);
              return PipelineJobOutcome.WORK_DONE;
            });

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
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
    assertEquals(Optional.empty(), jobSummary.getOutcome());
    assertEquals(
        Optional.of(InterruptedException.class), jobSummary.getException().map(Object::getClass));
    assertNull(pipelineManager.getError());
  }

  /** Verifies that {@link PipelineManager#stop()} works with uninterruptible jobs. */
  @Test
  public void runUninterruptibleJobsThenStop() {
    final var mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            false,
            () -> PipelineJobOutcome.WORK_DONE);

    final var pipelineManager = new PipelineManager(SLEEPER, clock, List.of(mockJob), ec2Client);
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
  @AllArgsConstructor
  private static class MockJob implements PipelineJob {
    /** Represents the job type for this mock job. */
    public static final PipelineJobType JOB_TYPE = new PipelineJobType(MockJob.class);

    /** The pipeline job schedule for the mock job. */
    private final Optional<PipelineJobSchedule> schedule;

    /** Represents if this job can be interrupted. */
    private final boolean interruptible;

    /** The {@link Callable} that will create the values to use for {@link #call()}. */
    private final Callable<Object> jobResultProducer;

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
