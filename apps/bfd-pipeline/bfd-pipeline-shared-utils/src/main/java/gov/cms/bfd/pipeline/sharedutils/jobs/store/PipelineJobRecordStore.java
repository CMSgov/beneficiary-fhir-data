package gov.cms.bfd.pipeline.sharedutils.jobs.store;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and manages the {@link PipelineJobRecord}s representing the history of submitted {@link
 * PipelineJob}s.
 */
public final class PipelineJobRecordStore {
  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineJobRecordStore.class);

  /** The number of milliseconds to wait between polling job dependencies' status. */
  private static final int JOB_DEPENDENCY_POLL_MILLIS = 100;

  private final MetricRegistry appMetrics;

  /**
   * The "database" for all {@link PipelineJobRecord}s in the application.
   *
   * <p>This will be read and modified from almost every thread in the application, and from the
   * perspective of any one of them, is only eventually consistent. This tends to work out okay for
   * our use cases, as you're not likely to have two separate jobs racing to create the
   * <em>same</em> new job, but care should still be taken when working with it.
   */
  private final ConcurrentMap<PipelineJobRecordId, PipelineJobRecord<?>> jobRecords;

  /**
   * Constructs a new {@link PipelineJobRecordStore} instance.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   */
  public PipelineJobRecordStore(MetricRegistry appMetrics) {
    this.appMetrics = appMetrics;
    this.jobRecords = new ConcurrentHashMap<>();
  }

  /**
   * Note: this should only be used in test code, as it's performance characteristics are terrible
   * for production use.
   *
   * @return the full {@link Collection} of {@link PipelineJobRecord}s that have been submitted
   */
  public Collection<PipelineJobRecord<?>> getJobRecords() {
    return Collections.unmodifiableCollection(jobRecords.values());
  }

  /**
   * @param maxJobRecords the maximum number of matching {@link PipelineJobRecord}s to return
   * @return up to the specified number of {@link PipelineJobRecord}s where {@link
   *     PipelineJobRecord#isStarted()} is <code>false</code>
   */
  public Set<PipelineJobRecord<?>> findPendingJobs(int maxJobRecords) {
    try (Timer.Context timer =
        appMetrics
            .timer(MetricRegistry.name(getClass().getSimpleName(), "findPendingJobs"))
            .time()) {
      /*
       * Design note: We don't garbage collect completed jobs, so this will get slower as job count
       * grows. However, in tests to simulate this behavior, it looks like it only gets up to the
       * tens of milliseconds after a week of operation, worst-case, with our current number of
       * jobs. That's acceptable, since only VolunteerJob calls this.
       */
      return jobRecords.values().stream()
          .filter(j -> !j.isStarted())
          .sorted(Comparator.comparing(PipelineJobRecord::getCreatedTime))
          .limit(maxJobRecords)
          .collect(Collectors.toSet());
    }
  }

  /**
   * @param type the {@link PipelineJobRecord#getJobType()} to match against
   * @return the {@link PipelineJobRecord} that matches the criteria with the most recent {@link
   *     PipelineJobRecord#getCreatedTime()} value
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <A extends PipelineJobArguments> Optional<PipelineJobRecord<A>> findMostRecent(
      PipelineJobType<A> type) {
    try (Timer.Context timer =
        appMetrics
            .timer(MetricRegistry.name(getClass().getSimpleName(), "findMostRecent"))
            .time()) {
      if (type == null) throw new IllegalArgumentException();

      /*
       * Design note: We don't garbage collect completed jobs, so this will get slower as job count
       * grows. However, in tests to simulate this behavior, it looks like it only gets up to the
       * tens of milliseconds after a week of operation, worst-case, with our current number of
       * jobs. That's acceptable, since only SchedulerJob calls this.
       */
      Optional mostRecentRecord =
          jobRecords.values().stream()
              .filter(j -> type.equals(j.getJobType()))
              .max(Comparator.comparing(PipelineJobRecord::getCreatedTime));
      return mostRecentRecord;
    }
  }

  /**
   * Submits the specified {@link PipelineJob} / {@link PipelineJobType} for execution, with the
   * specified {@link PipelineJobArguments}.
   *
   * @param jobType the {@link PipelineJob#getType()} of the {@link PipelineJob} to run
   * @param jobArguments the {@link PipelineJobArguments} to run the specified {@link PipelineJob}
   *     with
   * @return a new {@link PipelineJobRecord} that tracks the status of the requested job run
   */
  public <A extends PipelineJobArguments> PipelineJobRecord<A> submitPendingJob(
      PipelineJobType<A> jobType, A jobArguments) {
    PipelineJobRecord<A> jobRecord = new PipelineJobRecord<A>(jobType, jobArguments);
    this.jobRecords.put(jobRecord.getId(), jobRecord);
    LOGGER.trace(
        "submitPendingJob(...) called: jobType='{}', jobArguments='{}', jobRecord='{}'",
        jobType,
        jobArguments,
        jobRecord);

    return jobRecord;
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getEnqueuedTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobEnqueue(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setEnqueuedTime(Instant.now());

    // Record how long it took the job to go from being created to being enqueued.
    appMetrics
        .timer(MetricRegistry.name(PipelineJob.class.getSimpleName(), "createdToEnqueued"))
        .update(Duration.between(jobRecord.getCreatedTime(), jobRecord.getEnqueuedTime().get()));

    LOGGER.trace("recordJobEnqueue(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getStartedTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobStart(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setStartedTime(Instant.now());

    // Record how long it took the job to go from being enqueued to being started.
    appMetrics
        .timer(MetricRegistry.name(PipelineJob.class.getSimpleName(), "enqueuedToStarted"))
        .update(
            Duration.between(jobRecord.getEnqueuedTime().get(), jobRecord.getStartedTime().get()));

    LOGGER.trace("recordJobStart(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getCanceledTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobCancellation(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setCanceledTime(Instant.now());

    // Record how long it took the job to go from being started to being canceled.
    if (jobRecord.getStartedTime().isPresent()) {
      appMetrics
          .timer(MetricRegistry.name(PipelineJob.class.getSimpleName(), "startedToCanceled"))
          .update(
              Duration.between(
                  jobRecord.getStartedTime().get(), jobRecord.getCanceledTime().get()));
    }

    LOGGER.trace("recordJobCancellation(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getCompletedTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}, along with the other
   * data provided.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   * @param jobOutcome the {@link PipelineJobOutcome} that the job execution produced/returned
   */
  public void recordJobCompletion(PipelineJobRecordId jobRecordId, PipelineJobOutcome jobOutcome) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setCompleted(Instant.now(), jobOutcome);

    // Record how long it took the job to go from being started to being completed (succeeded).
    appMetrics
        .timer(
            MetricRegistry.name(
                PipelineJob.class.getSimpleName(), "startedToCompleted", "succeeded"))
        .update(
            Duration.between(jobRecord.getStartedTime().get(), jobRecord.getCompletedTime().get()));

    LOGGER.trace("recordJobCompletion(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getCompletedTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}, along with the other
   * data provided.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   * @param jobFailure the {@link PipelineJobFailure} that the job execution produced/resulted in
   */
  public void recordJobFailure(PipelineJobRecordId jobRecordId, PipelineJobFailure jobFailure) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setCompleted(Instant.now(), jobFailure);

    // Record how long it took the job to go from being started to being completed (failed).
    appMetrics
        .timer(
            MetricRegistry.name(PipelineJob.class.getSimpleName(), "startedToCompleted", "failed"))
        .update(
            Duration.between(jobRecord.getStartedTime().get(), jobRecord.getCompletedTime().get()));

    LOGGER.trace("recordJobFailure(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Blocks until the specified jobs have completed their most recent execution.
   *
   * @param jobsToWaitOn the {@link PipelineJobRecord}s to wait for the completion of
   * @throws InterruptedException Any {@link InterruptedException}s encountered will be bubbled up.
   * @throws IllegalStateException An {@link IllegalStateException} will be thrown if no execution a
   *     specified job can be found. This likely indicates a logic error elsewhere in the
   *     application, where a job <em>should</em> have been triggered, but wasn't.
   */
  public void waitForJobs(PipelineJobRecord<?>... jobsToWaitOn) throws InterruptedException {
    /*
     * Design Note: I'm not particularly thrilled with this design for job dependency handling, as
     * it has a couple drawbacks. First, the dependent job sits here occupying an executor slot even
     * though it really can't do anything. Second, polling for dependency status is clunky; a
     * notification-based approach would be cleaner. Nevertheless... this could be redesigned if our
     * needs warranted it and the current approach is definitely good enough for our needs right
     * now.
     */

    // Now, poll each of those jobs until they're complete.
    for (PipelineJobRecord<?> jobToWaitOn : jobsToWaitOn) {
      // Poll for completion.
      while (!jobToWaitOn.isCompleted()) {
        Thread.sleep(JOB_DEPENDENCY_POLL_MILLIS);
      }
    }
  }
}
