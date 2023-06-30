package gov.cms.bfd.pipeline.sharedutils;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Represents a job/task that can be scheduled and executed by the BFD Pipeline application.
 *
 * <p>{@link PipelineJob} implementations that are meant to be triggered by other jobs
 * <strong>SHALL</strong> also provide a {@code JOB_TYPE} constant for other jobs to reference,
 * which must return the same value as {@link #getType()}.
 */
public interface PipelineJob extends Callable<PipelineJobOutcome> {
  /**
   * Gets the {@link PipelineJobType} that uniquely identifies this {@link PipelineJob}
   * implementation.
   *
   * @return the pipeline job type
   */
  default PipelineJobType getType() {
    return new PipelineJobType(this);
  }

  /**
   * Gets the schedule to run this {@link PipelineJob} on.
   *
   * @return the schedule if any, or {@link Optional#empty()} if the {@link PipelineJob} should not
   *     be run on a schedule
   */
  Optional<PipelineJobSchedule> getSchedule();

  /**
   * Indicates whether or not this {@link PipelineJob} implementation is designed to be safely
   * interruptible (via {@link Thread#interrupt()}). This indicates to the job execution framework
   * that it is safe to stop the job in-progress (and presumably, to run it again later). This
   * feature allows the application to shut down in a timely fashion, when requested. Conversely,
   * the application has to wait for all currently-executing {@link PipelineJob}s that return <code>
   * false</code> here to complete running before terminating. All new {@link PipelineJob}
   * implementations should strive to design themselves such that they can return <code>true</code>
   * here.
   *
   * <p>Java threaded applications can use {@link Thread#interrupt()} to signal to async tasks that
   * "hey, someone wants you to stop running". When called, this will cause most synchronous
   * blocking operations (e.g. I/O) to fail early with a thrown {@link InterruptedException}. In
   * addition, threads can also poll the status of {@link Thread#isInterrupted()} to see if someone
   * has asked them to stop, and then return early or throw an exception to do so, if they so wish.
   *
   * @return <code>true</code> if this {@link PipelineJob} is designed to be safely interruptible
   *     (via {@link Thread#interrupt()}, <code>false</code> if it's not
   */
  boolean isInterruptible();

  /**
   * Perform a job-specific smoke test and returns true if the test was successful. A failed test
   * indicates that the job cannot proceed and should not be scheduled. The default implementation
   * simply returns true.
   *
   * @return true if the test passed, false otherwise
   * @throws Exception test can pass through uncaught exceptions for reporting by the pipeline app
   */
  default boolean isSmokeTestSuccessful() throws Exception {
    return true;
  }

  /**
   * Each scheduled/triggered execution of this {@link PipelineJob}, the job orchestrator will run
   * this {@link #call()} method. The {@link PipelineJob} SHALL:
   *
   * <ul>
   *   <li>Attempt to find and process the data or perform the work that it's expected to.
   *   <li>Block until that processing/work is completed.
   *   <li>Return a successful outcome as a {@link PipelineJobOutcome} and report any failures by
   *       throwing an {@link Exception}.
   * </ul>
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  PipelineJobOutcome call() throws Exception;
}
