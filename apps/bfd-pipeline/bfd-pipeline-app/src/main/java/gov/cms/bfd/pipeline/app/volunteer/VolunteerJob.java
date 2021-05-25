package gov.cms.bfd.pipeline.app.volunteer;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.pipeline.app.PipelineManager;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecord;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link PipelineJob} watches for jobs to be submitted and then claims them: it "volunteers"
 * the system for any available work.
 */
public final class VolunteerJob implements PipelineJob<NullPipelineJobArguments> {
  private static final Logger LOGGER = LoggerFactory.getLogger(VolunteerJob.class);

  public static final PipelineJobType<NullPipelineJobArguments> JOB_TYPE =
      new PipelineJobType<NullPipelineJobArguments>(VolunteerJob.class);

  /**
   * The number of milliseconds to wait between job submission iterations. Regardless of their
   * schedule, {@link PipelineJob}s will not be able to run more frequently than this.
   *
   * <p>Note: this "constant" is actually mutable, but should only ever be modified by tests.
   */
  public static long VOLUNTEER_TICK_MILLIS = 10 * 1000;

  private final MetricRegistry appMetrics;
  private final PipelineManager pipelineManager;
  private final PipelineJobRecordStore jobRecordsStore;

  /**
   * Constructs the {@link VolunteerJob}, which should be a singleton within its JVM.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param pipelineManager the {@link PipelineManager} that jobs should be run on
   * @param jobRecordsStore the {@link PipelineJobRecordStore} tracking jobs that have been
   *     submitted for execution
   */
  public VolunteerJob(
      MetricRegistry appMetrics,
      PipelineManager pipelineManager,
      PipelineJobRecordStore jobRecordsStore) {
    this.appMetrics = appMetrics;
    this.pipelineManager = pipelineManager;
    this.jobRecordsStore = jobRecordsStore;
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#getSchedule() */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return Optional.empty();
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#isInterruptible() */
  @Override
  public boolean isInterruptible() {
    return true;
  }

  /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#call() */
  @Override
  public PipelineJobOutcome call() throws Exception {
    boolean enqueuedAJob = false;
    while (true) {
      try (Timer.Context timer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "call", "iteration"))
              .time()) {
        /*
         * We want to submit up to as many jobs for execution as we can actually work, each
         * iteration of this loop.
         */
        int executorSlotsOpen = pipelineManager.getOpenExecutorSlots();
        if (executorSlotsOpen <= 0) continue;

        Set<PipelineJobRecord<?>> jobsToStart = jobRecordsStore.findPendingJobs(executorSlotsOpen);
        LOGGER.trace(
            "call() called: executorSlotsOpen='{}', jobsToStart='{}'",
            executorSlotsOpen,
            jobsToStart);

        // Submit those jobs to start running!
        for (PipelineJobRecord<?> jobToStart : jobsToStart) {
          pipelineManager.enqueueJob(jobToStart);
          enqueuedAJob = true;
        }
      }

      try {
        Thread.sleep(VOLUNTEER_TICK_MILLIS);
      } catch (InterruptedException e) {
        /*
         * Jobs are only interrupted/cancelled as part of application shutdown, so when encountered,
         * we'll break out of our scheduling loop and close up shop here.
         */
        break;
      }
    }

    // Did we enqueue at least one job?
    return enqueuedAJob ? PipelineJobOutcome.WORK_DONE : PipelineJobOutcome.NOTHING_TO_DO;
  }
}
