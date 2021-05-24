package gov.cms.bfd.pipeline.app.scheduler;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.pipeline.app.PipelineManager;
import gov.cms.bfd.pipeline.app.volunteer.VolunteerJob;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecord;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * This {@link PipelineJob} checks the schedules of other jobs and triggers their executions, per
 * those schedules.
 *
 * <p>Design Note: If we ever move to an autoscaled version of this application, it will be
 * important to ensure that this job is only running once across its environment, to avoid duplicate
 * job schedule triggers. To that end, this job will get run just like any other job (via the {@link
 * PipelineJobRecordStore} and the {@link VolunteerJob}). To ensure that it gets kicked off, the
 * {@link PipelineJobRecordStore} has a permanently uncompleted {@link PipelineJobRecord} for this:
 * the job will always be running and runs a scheduling loop internally.
 */
public final class SchedulerJob implements PipelineJob<NullPipelineJobArguments> {
  public static final PipelineJobType<NullPipelineJobArguments> JOB_TYPE =
      new PipelineJobType<NullPipelineJobArguments>(SchedulerJob.class);

  /**
   * The number of milliseconds to wait between schedule check iterations. Regardless of their
   * schedule, {@link PipelineJob}s will not be able to run more frequently than this.
   *
   * <p>Note: this "constant" is actually mutable, but should only ever be modified by tests.
   */
  public static long SCHEDULER_TICK_MILLIS = 10 * 1000;

  private final MetricRegistry appMetrics;
  private final PipelineManager pipelineManager;
  private final PipelineJobRecordStore jobRecordsStore;

  /**
   * Constructs the {@link SchedulerJob}, which should be a singleton within the application
   * environment.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param pipelineManager the {@link PipelineManager} that jobs should be run on
   * @param jobRecordsStore the {@link PipelineJobRecordStore} tracking jobs that have been
   *     submitted for execution
   */
  public SchedulerJob(
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
    boolean scheduledAJob = false;
    while (true) {
      try (Timer.Context timer =
          appMetrics
              .timer(MetricRegistry.name(getClass().getSimpleName(), "call", "iteration"))
              .time()) {
        Instant now = Instant.now();
        Set<PipelineJob<NullPipelineJobArguments>> scheduledJobs =
            pipelineManager.getScheduledJobs();
        for (PipelineJob<NullPipelineJobArguments> scheduledJob : scheduledJobs) {
          PipelineJobSchedule jobSchedule = scheduledJob.getSchedule().get();
          Optional<PipelineJobRecord<NullPipelineJobArguments>> mostRecentExecution =
              jobRecordsStore.findMostRecent(scheduledJob.getType());

          /* Calculate whether or not we should trigger an execution of the next job. */
          boolean shouldTriggerJob;
          if (!mostRecentExecution.isPresent()) {
            // If the job has never run, we'll always trigger it now, regardless of schedule.
            shouldTriggerJob = true;
          } else {
            if (!mostRecentExecution.get().isCompleted()) {
              // If the job's still pending or running, don't double-trigger it.
              shouldTriggerJob = false;
            } else {
              if (mostRecentExecution.get().isCompletedSuccessfully()) {
                // If the job's not running, check to see if it's time to trigger it again.
                // Note: This calculation is based on completion time, not submission or start time.
                Instant nextExecution =
                    mostRecentExecution
                        .get()
                        .getStartedTime()
                        .get()
                        .plus(jobSchedule.getRepeatDelay(), jobSchedule.getRepeatDelayUnit());
                shouldTriggerJob = now.equals(nextExecution) || now.isAfter(nextExecution);
              } else {
                // We don't re-run failed jobs.
                shouldTriggerJob = false;
              }
            }
          }

          // If we shouldn't trigger this job, move on to the next.
          if (!shouldTriggerJob) {
            continue;
          }

          // Trigger the job (for future execution, when VolunteerJob picks it up)!
          jobRecordsStore.submitPendingJob(scheduledJob.getType(), null);
        }
      }

      try {
        Thread.sleep(SCHEDULER_TICK_MILLIS);
      } catch (InterruptedException e) {
        /*
         * Jobs are only interrupted/cancelled as part of application shutdown, so when encountered,
         * we'll break out of our scheduling loop and close up shop here.
         */
        break;
      }
    }

    /*
     * Did we schedule at least one job? If we ever move to an autoscaled version of this
     * application, it will be important to ensure that we "collude" with the PipelineJobRecordStore
     * to ignore this PipelineJobOutcome and ensure that the record doesn't get marked as completed,
     * even when the application shuts down. (If that happened, then scheduled triggers would stop
     * firing.)
     */
    return scheduledAJob ? PipelineJobOutcome.WORK_DONE : PipelineJobOutcome.NOTHING_TO_DO;
  }
}
