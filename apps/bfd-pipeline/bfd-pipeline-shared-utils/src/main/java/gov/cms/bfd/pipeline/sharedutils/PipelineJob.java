package gov.cms.bfd.pipeline.sharedutils;

import java.util.concurrent.Callable;

/** Represents a job/task that can be scheduled and executed by the BFD Pipeline application. */
public interface PipelineJob extends Callable<PipelineJobOutcome> {
  /**
   * Each scheduled/triggered execution of this {@link PipelineJob}, the job orchestrator will run
   * this {@link #call()} method. The {@link PipelineJob} SHALL:
   *
   * <ul>
   *   <li>Attempt to find and process the data or perform the work that it's expected to.
   *   <li>Block until that processing/work is completed.
   *   <li>Return a successful outcome as a {@link PipelineJobOutcome} and report any failures by
   *       throwing an {@link Exception}.
   *   <li>
   * </ul>
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  PipelineJobOutcome call() throws Exception;
}
