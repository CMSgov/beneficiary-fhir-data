package gov.cms.bfd.pipeline.sharedutils.jobs.store;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private final Map<PipelineJobRecordId, PipelineJobRecord<?>> jobRecords;

  /** Constructs a new {@link PipelineJobRecordStore} instance. */
  public PipelineJobRecordStore() {
    this.jobRecords = new HashMap<>();
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
    // TODO Is this actually performant at our production scale? I'd guess not.
    // TODO This is almost certainly not FIFO, and should be.
    return jobRecords.values().stream()
        .filter(j -> !j.isStarted())
        .limit(maxJobRecords)
        .collect(Collectors.toSet());
  }

  /**
   * @param type the {@link PipelineJobRecord#getJobType()} to match against
   * @return the {@link PipelineJobRecord} that matches the criteria with the most recent {@link
   *     PipelineJobRecord#getCreationTime()} value
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <A extends PipelineJobArguments> Optional<PipelineJobRecord<A>> findMostRecent(
      PipelineJobType<A> type) {
    // TODO Is this actually performant at our production scale? I'd guess not.
    if (type == null) throw new IllegalArgumentException();
    Optional mostRecentRecord =
        jobRecords.values().stream()
            .filter(j -> type.equals(j.getJobType()))
            .max(Comparator.comparing(PipelineJobRecord::getCreationTime));
    return mostRecentRecord;
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
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getEnqueueTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobEnqueue(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setEnqueueTime(Instant.now());
    LOGGER.trace("recordJobEnqueue(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getStartTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobStart(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setStartTime(Instant.now());
    LOGGER.trace("recordJobStart(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getCancelTime()} value for the
   * {@link PipelineJobRecord} with the specified {@link PipelineJobRecordId}.
   *
   * @param jobRecordId the {@link PipelineJobRecord#getId()} value of the {@link PipelineJobRecord}
   *     to update
   */
  public void recordJobCancellation(PipelineJobRecordId jobRecordId) {
    PipelineJobRecord<?> jobRecord = jobRecords.get(jobRecordId);
    if (jobRecord == null) throw new IllegalStateException();

    jobRecord.setCancelTime(Instant.now());
    LOGGER.trace("recordJobCancellation(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getStopTime()} value for the
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

    jobRecord.setStopTime(Instant.now());
    jobRecord.setOutcome(jobOutcome);
    LOGGER.trace("recordJobCompletion(...) called: jobRecord='{}'", jobRecord);
  }

  /**
   * Records {@link Instant#now()} as the {@link PipelineJobRecord#getStopTime()} value for the
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

    jobRecord.setStopTime(Instant.now());
    jobRecord.setFailure(jobFailure);
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
