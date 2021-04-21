package gov.cms.bfd.pipeline.sharedutils.jobs.store;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
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
 *   <li>Cancelled: Jobs can be cancelled before or during execution.
 *   <li>Enqueued: Once a job has been submitted for execution.
 *   <li>Started: Once a job has started running.
 *   <li>Succeeded: A job that has successfully finished running, without exceptions.
 *   <li>Failed: A job that started running but then threw an exception, rather than completing
 *       successfully.
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
  private final Instant creationTime;
  private Optional<Instant> enqueueTime;
  private Optional<Instant> startTime;
  private Optional<Instant> stopTime;
  private Optional<Instant> cancelTime;
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
    this.creationTime = Instant.now();
    this.jobType = jobType;
    this.jobArguments = jobArguments;

    this.enqueueTime = Optional.empty();
    this.startTime = Optional.empty();
    this.stopTime = Optional.empty();
    this.cancelTime = Optional.empty();
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
  public Instant getCreationTime() {
    return creationTime;
  }

  /** @return the {@link Instant} that this job was enqueued to an executor for execution, if any */
  public Optional<Instant> getEnqueueTime() {
    return enqueueTime;
  }

  /** @param enqueueTime the value to set {@link #getEnqueueTime()} to */
  public void setEnqueueTime(Instant enqueueTime) {
    this.enqueueTime = Optional.of(enqueueTime);
  }

  /** @return the {@link Instant} that this job started running at, if any */
  public Optional<Instant> getStartTime() {
    return startTime;
  }

  /** @return <code>true</code> if this job has started running, <code>false</code> if it has not */
  public boolean isStarted() {
    return startTime.isPresent();
  }

  /** @param startTime the value to set {@link #getStartTime()} to */
  public void setStartTime(Instant startTime) {
    this.startTime = Optional.of(startTime);
  }

  /** @return the {@link Instant} that this job completed at, if any */
  public Optional<Instant> getStopTime() {
    return stopTime;
  }

  /** @param stopTime the value to set {@link #getStopTime()} to */
  public void setStopTime(Instant stopTime) {
    this.stopTime = Optional.of(stopTime);
  }

  /**
   * @return <code>true</code> if the job has completed (either successfully or with a failure),
   *     <code>false</code> if it has not
   */
  public boolean isCompleted() {
    return stopTime.isPresent();
  }

  /** @return the {@link Instant} that this job was canceled at, if any */
  public Optional<Instant> getCancelTime() {
    return cancelTime;
  }

  /** @param cancelTime the value to set {@link #getCancelTime()} to */
  public void setCancelTime(Instant cancelTime) {
    this.cancelTime = Optional.of(cancelTime);
  }

  /** @return <code>true</code> if the job has been canceled, <code>false</code> if it has not */
  public boolean isCanceled() {
    return cancelTime.isPresent();
  }

  /**
   * @return a {@link Duration} representing how long the job ran for, or {@link Optional#empty()}
   *     if it hasn't started or completed
   */
  public Optional<Duration> getDuration() {
    if (isCompleted()) {
      return Optional.of(Duration.between(startTime.get(), stopTime.get()));
    } else if (isCanceled()) {
      return Optional.of(Duration.between(startTime.get(), cancelTime.get()));
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
   * @return <code>true</code> if {@link #getOutcome()} is present, <code>false</code> if it's not
   */
  public boolean isSuccessful() {
    return outcome.isPresent();
  }

  /** @param outcome the value to set {@link #getOutcome()} to */
  public void setOutcome(PipelineJobOutcome outcome) {
    this.outcome = Optional.of(outcome);
  }

  /**
   * @return the {@link PipelineJobFailure} for this job if it has completed with a failure, or
   *     {@link Optional#empty()} if it is either as-yet-incomplete or if it succeeded (in which
   *     case, {@link #getOutcome()} will have a value)
   */
  public Optional<PipelineJobFailure> getFailure() {
    return failure;
  }

  /** @param failure the value to set {@link #getFailure()} to */
  public void setFailure(PipelineJobFailure failure) {
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
    builder.append(", creationTime=");
    builder.append(creationTime);
    builder.append(", enqueueTime=");
    builder.append(enqueueTime);
    builder.append(", startTime=");
    builder.append(startTime);
    builder.append(", stopTime=");
    builder.append(stopTime);
    builder.append(", cancelTime=");
    builder.append(cancelTime);
    builder.append(", outcome=");
    builder.append(outcome);
    builder.append(", failure=");
    builder.append(failure);
    builder.append("]");
    return builder.toString();
  }
}
