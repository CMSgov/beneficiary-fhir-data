package gov.cms.bfd.pipeline.sharedutils.jobs.store;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Models the status of a {@link PipelineJob} that has been submitted for execution. This is
 * basically a state machine data store, that models the various states and transitions that a job
 * can proceed through. Jobs can be in the following states:
 *
 * <ul>
 *   <li>Created: This is the initial state for every job.
 *   <li>Canceled: Jobs can be cancelled before or during execution.
 *   <li>Enqueued: Once a job has been submitted for execution.
 *   <li>Started: Once a job has started running.
 *   <li>Completed (Succeeded): A job that has successfully finished running, without exceptions.
 *   <li>Completed (Failed): A job that started running but then threw an exception, rather than
 *       completing successfully.
 * </ul>
 *
 * <p>Design Note: I considered coding this <em>as</em> an actual state machine, but all of the
 * libraries and patterns available for doing so in Java looked like they'd add more complexity than
 * they removed. Also: if we ever decided to scale job execution across multiple nodes, this
 * data/class would need to become a JPA entity, and none of the state machine patterns seemed
 * suited to that. For example, <a href="https://spring.io/projects/spring-statemachine">Spring
 * Statemachine</a> will persist all of the state machine's data as a combined serialized blob,
 * which is not what we need.
 */
public final class PipelineJobRecord<A extends PipelineJobArguments> {
  private final PipelineJobRecordId id;
  private final PipelineJobType<A> jobType;
  private final A jobArguments;
  private final Instant createdTime;
  private Optional<Instant> canceledTime;
  private Optional<Instant> enqueuedTime;
  private Optional<Instant> startedTime;
  private Optional<Instant> completedTime;
  private Optional<PipelineJobOutcome> outcome;
  private Optional<PipelineJobFailure> failure;

  /**
   * Constructs a new {@link PipelineJobRecord} instance.
   *
   * @param jobType the value to use for {@link #getJobType()}
   * @param jobArguments the value to use for {@link #getJobArguments()}
   */
  public PipelineJobRecord(PipelineJobType<A> jobType, A jobArguments) {
    this.id = new PipelineJobRecordId();
    this.jobType = jobType;
    this.jobArguments = jobArguments;
    this.createdTime = Instant.now();

    this.canceledTime = Optional.empty();
    this.enqueuedTime = Optional.empty();
    this.startedTime = Optional.empty();
    this.completedTime = Optional.empty();
    this.outcome = Optional.empty();
    this.failure = Optional.empty();
  }

  /**
   * @return the {@link PipelineJobRecordId} that uniquely identifies this {@link PipelineJobRecord}
   */
  public PipelineJobRecordId getId() {
    return id;
  }

  /** @return the {@link PipelineJobType} that this {@link PipelineJobRecord} is for */
  public PipelineJobType<A> getJobType() {
    return jobType;
  }

  /**
   * @return the {@link PipelineJobArguments} that the job should be run with, if any (<code>null
   *     </code> if there are none)
   */
  public A getJobArguments() {
    return jobArguments;
  }

  /** @return the {@link Instant} that this {@link PipelineJobRecord} was created at */
  public Instant getCreatedTime() {
    return createdTime;
  }

  /** @return the {@link Instant} that this job was canceled at, if any */
  public Optional<Instant> getCanceledTime() {
    return canceledTime;
  }

  /** @return <code>true</code> if the job has been canceled, <code>false</code> if it has not */
  public boolean isCanceled() {
    return canceledTime.isPresent();
  }

  /** @param canceledTime the value to set {@link #getCanceledTime()} to */
  public void setCanceledTime(Instant canceledTime) {
    if (this.canceledTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.completedTime.isPresent()) throw new BadCodeMonkeyException();

    this.canceledTime = Optional.of(canceledTime);
  }

  /** @return the {@link Instant} that this job was enqueued to an executor for execution, if any */
  public Optional<Instant> getEnqueuedTime() {
    return enqueuedTime;
  }

  /** @param enqueuedTime the value to set {@link #getEnqueuedTime()} to */
  public void setEnqueuedTime(Instant enqueuedTime) {
    // Validate the state transition.
    if (this.enqueuedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.canceledTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.startedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.completedTime.isPresent()) throw new BadCodeMonkeyException();

    this.enqueuedTime = Optional.of(enqueuedTime);
  }

  /** @return the {@link Instant} that this job started running at, if any */
  public Optional<Instant> getStartedTime() {
    return startedTime;
  }

  /** @return <code>true</code> if this job has started running, <code>false</code> if it has not */
  public boolean isStarted() {
    return startedTime.isPresent();
  }

  /** @param startedTime the value to set {@link #getStartedTime()} to */
  public void setStartedTime(Instant startedTime) {
    if (!this.enqueuedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.canceledTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.startedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.completedTime.isPresent()) throw new BadCodeMonkeyException();

    this.startedTime = Optional.of(startedTime);
  }

  /** @return the {@link Instant} that this job completed at, if any */
  public Optional<Instant> getCompletedTime() {
    return completedTime;
  }

  /**
   * @return <code>true</code> if the job has completed (either successfully or with a failure),
   *     <code>false</code> if it has not
   */
  public boolean isCompleted() {
    return completedTime.isPresent();
  }

  /**
   * @return a {@link Duration} representing how long the job ran for, or {@link Optional#empty()}
   *     if it hasn't started or completed
   */
  public Optional<Duration> getDuration() {
    if (isCompleted()) {
      return Optional.of(Duration.between(startedTime.get(), completedTime.get()));
    } else if (isCanceled()) {
      return Optional.of(Duration.between(startedTime.get(), canceledTime.get()));
    } else {
      return Optional.empty();
    }
  }

  /**
   * @return the {@link PipelineJobOutcome} for this job if it has completed successfully, or {@link
   *     Optional#empty()} if it is either as-yet-incomplete or if it failed (in which case, {@link
   *     #getFailure()} will have a value)
   */
  public Optional<PipelineJobOutcome> getOutcome() {
    return outcome;
  }

  /**
   * @return the {@link PipelineJobFailure} for this job if it has completed with a failure, or
   *     {@link Optional#empty()} if it is either as-yet-incomplete or if it succeeded (in which
   *     case, {@link #getOutcome()} will have a value)
   */
  public Optional<PipelineJobFailure> getFailure() {
    return failure;
  }

  /**
   * @return <code>true</code> if {@link #getOutcome()} is present, <code>false</code> if it's not
   */
  public boolean isCompletedSuccessfully() {
    return outcome.isPresent();
  }

  /**
   * Marks the {@link PipelineJob} as having completed successfully.
   *
   * @param completedTime the value to use for {@link #getCompletedTime()}
   * @param outcome the value to use for {@link #getOutcome()}
   */
  public void setCompleted(Instant completedTime, PipelineJobOutcome outcome) {
    if (!this.enqueuedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.canceledTime.isPresent()) throw new BadCodeMonkeyException();
    if (!this.startedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.completedTime.isPresent()) throw new BadCodeMonkeyException();

    this.completedTime = Optional.of(completedTime);
    this.outcome = Optional.of(outcome);
  }

  /**
   * Marks the {@link PipelineJob} as having completed with an exception.
   *
   * @param completedTime the value to use for {@link #getCompletedTime()}
   * @param failure the value to use for {@link #getFailure()}
   */
  public void setCompleted(Instant completedTime, PipelineJobFailure failure) {
    if (!this.enqueuedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.canceledTime.isPresent()) throw new BadCodeMonkeyException();
    if (!this.startedTime.isPresent()) throw new BadCodeMonkeyException();
    if (this.completedTime.isPresent()) throw new BadCodeMonkeyException();

    this.completedTime = Optional.of(completedTime);
    this.failure = Optional.of(failure);
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PipelineJobRecord [id=");
    builder.append(id);
    builder.append(", jobType=");
    builder.append(jobType);
    builder.append(", jobArguments=");
    builder.append(jobArguments);
    builder.append(", createdTime=");
    builder.append(createdTime);
    builder.append(", canceledTime=");
    builder.append(canceledTime);
    builder.append(", enqueuedTime=");
    builder.append(enqueuedTime);
    builder.append(", startedTime=");
    builder.append(startedTime);
    builder.append(", completedTime=");
    builder.append(completedTime);
    builder.append(", outcome=");
    builder.append(outcome);
    builder.append(", failure=");
    builder.append(failure);
    builder.append("]");
    return builder.toString();
  }
}
