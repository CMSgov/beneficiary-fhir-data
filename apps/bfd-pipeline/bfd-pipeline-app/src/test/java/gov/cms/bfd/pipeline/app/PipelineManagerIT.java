package gov.cms.bfd.pipeline.app;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.app.scheduler.SchedulerJob;
import gov.cms.bfd.pipeline.app.volunteer.VolunteerJob;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecord;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/** Integration tests for {@link PipelineManager}, {@link PipelineJobRecordStore}, and friends. */
public final class PipelineManagerIT {
  /**
   * Verifies that {@link PipelineManager} automatically runs {@link MockJob} and {@link
   * SchedulerJob}, as expected.
   */
  @Test
  public void runBuiltinJobs() {
    // Create the pipeline.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    new PipelineManager(new MetricRegistry(), jobRecordStore);

    // Verify that there are job records for the built-ins.
    Assert.assertEquals(2, jobRecordStore.getJobRecords().size());
    Assert.assertTrue(
        jobRecordStore.getJobRecords().stream()
            .anyMatch(j -> VolunteerJob.JOB_TYPE.equals(j.getJobType())));
    Assert.assertTrue(
        jobRecordStore.getJobRecords().stream()
            .anyMatch(j -> SchedulerJob.JOB_TYPE.equals(j.getJobType())));
  }

  /** Verifies that {@link PipelineManager} runs a successful mock one-shot job, as expected. */
  @Test
  public void runSuccessfulMockOneshotJob() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob = new MockJob(Optional.empty(), () -> PipelineJobOutcome.WORK_DONE);
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until a completed iteration of the mock job can be found.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
                    .findAny()
                    .isPresent());

    // Verify that one of the completed mock job iterations looks correct.
    Optional<PipelineJobRecord<?>> mockJobRecord =
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()))
            .findAny();
    Assert.assertEquals(
        Optional.of(PipelineJobOutcome.WORK_DONE), mockJobRecord.get().getOutcome());
  }

  /** Verifies that {@link PipelineManager} runs a failing mock one-shot job, as expected. */
  @Test
  public void runFailingMockOneshotJob() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.empty(),
            () -> {
              throw new RuntimeException("boom");
            });
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until a completed iteration of the mock job can be found.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
                    .findAny()
                    .isPresent());

    // Verify that one of the completed mock job iterations looks correct.
    Optional<PipelineJobRecord<?>> mockJobRecord =
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()))
            .findAny();
    Assert.assertEquals(RuntimeException.class, mockJobRecord.get().getFailure().get().getType());
    Assert.assertEquals("boom", mockJobRecord.get().getFailure().get().getMessage());
  }

  /** Verifies that {@link PipelineManager} runs a successful mock scheduled job, as expected. */
  @Test
  public void runSuccessfulScheduledJob() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            () -> PipelineJobOutcome.WORK_DONE);
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until a completed iteration of the mock job can be found.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
                    .findAny()
                    .isPresent());

    // Verify that one of the completed mock job iterations looks correct.
    Optional<PipelineJobRecord<?>> mockJobRecord =
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
            .findAny();
    Assert.assertEquals(
        Optional.of(PipelineJobOutcome.WORK_DONE), mockJobRecord.get().getOutcome());
  }

  /** Verifies that {@link PipelineManager} runs a failing mock scheduled job, as expected. */
  @Test
  public void runFailingScheduledJob() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            () -> {
              throw new RuntimeException("boom");
            });
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until a completed job can be found.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
                    .findAny()
                    .isPresent());

    // Verify that one of the completed mock job iterations looks correct.
    Optional<PipelineJobRecord<?>> mockJobRecord =
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isCompleted())
            .findAny();
    Assert.assertEquals(RuntimeException.class, mockJobRecord.get().getFailure().get().getType());
    Assert.assertEquals("boom", mockJobRecord.get().getFailure().get().getMessage());

    // Make sure that the job stopped trying to execute after it failed.
    Assert.assertEquals(
        1,
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()))
            .count());
  }

  /** Verifies that {@link PipelineManager#stop()} works, as expected. */
  @Test
  public void runInterruptibleJobsThenStop() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            () -> {
              // Add an artificial delay that we'll be able to measure.
              Thread.sleep(500);
              return PipelineJobOutcome.WORK_DONE;
            });
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until the mock job has started.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isStarted())
                    .findAny()
                    .isPresent());

    // Stop the pipeline and then make sure that the job was actually interrupted.
    pipelineManager.stop();
    PipelineJobRecord<NullPipelineJobArguments> mockJobRecord =
        jobRecordStore.findMostRecent(MockJob.JOB_TYPE).get();
    Assert.assertTrue(mockJobRecord.getCanceledTime().isPresent());
    Assert.assertTrue(mockJobRecord.getDuration().get().toMillis() < 500);
  }

  /** Verifies that {@link PipelineManager#stop()} works, as expected. */
  @Test
  public void runUninterruptibleJobsThenStop() {
    // Create the pipeline and have it run a mock job.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.of(new PipelineJobSchedule(1, ChronoUnit.MILLIS)),
            false,
            () -> {
              return PipelineJobOutcome.WORK_DONE;
            });
    pipelineManager.registerJob(mockJob);
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until the mock job has started.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isStarted())
                    .findAny()
                    .isPresent());

    // Stop the pipeline. If this doesn't hang, we're good.
    pipelineManager.stop();
  }

  /** Verifies that {@link PipelineManager#stop()} works, as expected. */
  @Test
  public void runThenStopAndCancelPendingJobs() {
    // Create the pipeline and a slow mock job that we can use.
    PipelineJobRecordStore jobRecordStore = new PipelineJobRecordStore();
    PipelineManager pipelineManager = new PipelineManager(new MetricRegistry(), jobRecordStore);
    MockJob mockJob =
        new MockJob(
            Optional.empty(),
            () -> {
              // Add an artificial delay that we'll be able to measure.
              Thread.sleep(500);
              return PipelineJobOutcome.WORK_DONE;
            });
    pipelineManager.registerJob(mockJob);

    /*
     * Once the VolunteerJob is running, submit enough slow mock jobs to fill up the
     * PipelineManager's executor threads/slots.
     */
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> VolunteerJob.JOB_TYPE.equals(j.getJobType()) && j.isStarted())
                    .findAny()
                    .isPresent());
    int openExecutorSlots = pipelineManager.getOpenExecutorSlots();
    for (int i = 0; i < openExecutorSlots; i++) {
      jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);
    }

    // Add one extra job that should sit as pending for a bit.
    jobRecordStore.submitPendingJob(MockJob.JOB_TYPE, null);

    // Wait until one of the mock jobs has started.
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(
            () ->
                jobRecordStore.getJobRecords().stream()
                    .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && j.isStarted())
                    .findAny()
                    .isPresent());

    // Stop the pipeline and verify that at least one job was cancelled before it started.
    pipelineManager.stop();
    Assert.assertTrue(
        jobRecordStore.getJobRecords().stream()
            .filter(j -> MockJob.JOB_TYPE.equals(j.getJobType()) && !j.isStarted())
            .findAny()
            .isPresent());
  }

  /** Reduce tick time on built-in jobs, to speed test execution. */
  @BeforeClass
  public static void configureTimers() {
    VolunteerJob.VOLUNTEER_TICK_MILLIS = 10;
    SchedulerJob.SCHEDULER_TICK_MILLIS = 10;
  }

  /** This mock {@link PipelineJob} returns a specified result. */
  private static final class MockJob implements PipelineJob<NullPipelineJobArguments> {
    public static final PipelineJobType<NullPipelineJobArguments> JOB_TYPE =
        new PipelineJobType<NullPipelineJobArguments>(MockJob.class);

    private final Optional<PipelineJobSchedule> schedule;
    private final boolean interruptible;
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

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#getSchedule() */
    @Override
    public Optional<PipelineJobSchedule> getSchedule() {
      return schedule;
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#isInterruptible() */
    @Override
    public boolean isInterruptible() {
      return interruptible;
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#call() */
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
