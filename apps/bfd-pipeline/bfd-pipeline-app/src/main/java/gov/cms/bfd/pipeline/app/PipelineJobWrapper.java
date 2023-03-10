package gov.cms.bfd.pipeline.app;

import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobFailure;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecord;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link PipelineJob} implementation wraps a delegate {@link PipelineJob}, providing data to
 * {@link PipelineJobRecordStore} about that job's execution and status.
 *
 * @param <A> the {@link PipelineJobArguments} type associated with this {@link PipelineJob}
 *     implementation (see {@link NullPipelineJobArguments} for those {@link PipelineJob}
 *     implementations which do not need arguments)
 */
public final class PipelineJobWrapper<A extends PipelineJobArguments> implements PipelineJob<A> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineJobWrapper.class);

  /** The {@link PipelineJob} to wrap and monitor. */
  private final PipelineJob<A> wrappedJob;
  /** The {@link PipelineJobRecord} for the job to wrap and monitor. */
  private final PipelineJobRecord<A> jobRecord;

  /**
   * The {@link PipelineJobRecordStore} service that tracks job submissions, executions, and
   * outcomes.
   */
  private final PipelineJobRecordStore jobRecordStore;

  /**
   * See PipelineManager's jobsEnqueuedHandles; should be handled in a thread-safe way when
   * accessing.
   */
  private final ConcurrentMap<PipelineJobRecordId, PipelineManager.PipelineJobHandle<?>>
      jobsEnqueuedHandles;

  /**
   * Constructs a new {@link PipelineJobWrapper} for the specified {@link PipelineJob}.
   *
   * @param wrappedJob the {@link PipelineJob} to wrap and monitor
   * @param jobRecord the {@link PipelineJobRecord} for the job to wrap and monitor
   * @param jobRecordStore the job record store
   * @param jobsEnqueuedHandles the jobs enqueued handles
   */
  public PipelineJobWrapper(
      PipelineJob<A> wrappedJob,
      PipelineJobRecord<A> jobRecord,
      PipelineJobRecordStore jobRecordStore,
      ConcurrentMap<PipelineJobRecordId, PipelineManager.PipelineJobHandle<?>>
          jobsEnqueuedHandles) {
    this.wrappedJob = wrappedJob;
    this.jobRecord = jobRecord;
    this.jobRecordStore = jobRecordStore;
    this.jobsEnqueuedHandles = jobsEnqueuedHandles;
  }

  /**
   * Gets the {@link #jobRecord}.
   *
   * @return the {@link PipelineJobRecord} for this {@link PipelineJobWrapper}
   */
  public PipelineJobRecord<A> getJobRecord() {
    return jobRecord;
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobType<A> getType() {
    return wrappedJob.getType();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return wrappedJob.getSchedule();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isInterruptible() {
    return wrappedJob.isInterruptible();
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobOutcome call() throws Exception {
    jobRecordStore.recordJobStart(jobRecord.getId());

    try {
      PipelineJobOutcome jobOutcome = wrappedJob.call();
      handleJobCompletion(jobRecord.getId(), jobOutcome);
      return jobOutcome;
    } catch (InterruptedException e) {
      /*
       * This indicates that someone has successfully interrupted the job, which should only have
       * happened when we're trying to shut down. Whether or not PipelineJob.isInterruptible() for
       * this job, it's now been stopped, so we should record the cancellation.
       */
      handleJobCancellation(jobRecord.getId());

      // Restore the interrupt so things can get back to shutting down.
      Thread.currentThread().interrupt();
      LOGGER.error(
          "PipeLineJobOutcome interrupt failed with the the following: " + e.getMessage(), e);
      throw new InterruptedException("Re-firing job interrupt.");
    } catch (Exception e) {
      // This will print the job failure + exception stacktrace
      handleJobFailure(jobRecord.getId(), e);
      // If we've blown up without explicit recovery possible, stop the pipeline
      // stop();
      // Ideally this would be caught by the exception handler?
      throw e;
    }
    // return PipelineJobOutcome.WORK_DONE;
  }

  /**
   * Handle normal job completion by de-queueing and recording completion.
   *
   * @param jobRecordId the {@link PipelineJobRecord} of the job
   * @param jobOutcome the outcome of the job to record
   */
  public void handleJobCompletion(PipelineJobRecordId jobRecordId, PipelineJobOutcome jobOutcome) {
    synchronized (jobsEnqueuedHandles) {
      if (jobsEnqueuedHandles.containsKey(jobRecordId)) {
        jobRecordStore.recordJobCompletion(jobRecordId, jobOutcome);
        jobsEnqueuedHandles.remove(jobRecordId);
      }
    }
  }

  /**
   * Handle job failure by de-queueing and recording the failure.
   *
   * @param jobRecordId the {@link PipelineJobRecord} of the job
   * @param exception The exception from the job failure
   */
  public void handleJobFailure(PipelineJobRecordId jobRecordId, Exception exception) {
    synchronized (jobsEnqueuedHandles) {
      if (jobsEnqueuedHandles.containsKey(jobRecordId)) {
        jobRecordStore.recordJobFailure(jobRecordId, new PipelineJobFailure(exception));
        jobsEnqueuedHandles.remove(jobRecordId);
      }
      LOGGER.error("Job failure in Pipeline: ", exception);
    }
  }

  /**
   * Handle job cancellation by de-queueing and recording cancellation.
   *
   * @param jobRecordId the {@link PipelineJobRecord} of the job
   */
  public void handleJobCancellation(PipelineJobRecordId jobRecordId) {
    synchronized (jobsEnqueuedHandles) {
      if (jobsEnqueuedHandles.containsKey(jobRecordId)) {
        jobRecordStore.recordJobCancellation(jobRecordId);
        jobsEnqueuedHandles.remove(jobRecordId);
      }
    }
  }
}
