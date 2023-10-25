package gov.cms.bfd.pipeline.sharedutils;

import java.time.temporal.TemporalUnit;

/**
 * Represents the schedule to run an automatically-repeating {@link PipelineJob} on. Note that many
 * {@link PipelineJob} implementations don't/can't run on such a repeating schedule and are instead
 * triggered by other means.
 */
public final class PipelineJobSchedule {
  /**
   * The minimum amount of time to wait between repeated executions of the {@link PipelineJob} (note
   * that this delay does not apply to the first run).
   */
  private final long repeatDelay;

  /** The {@link TemporalUnit} to use for {@link #repeatDelay}. */
  private final TemporalUnit repeatDelayUnit;

  /**
   * Constructs a new {@link PipelineJobSchedule}.
   *
   * @param repeatDelay the value to use for {@link #getRepeatDelay()}
   * @param repeatDelayUnit the value to use for {@link #getRepeatDelayUnit()}
   */
  public PipelineJobSchedule(long repeatDelay, TemporalUnit repeatDelayUnit) {
    if (repeatDelay < 0) throw new IllegalArgumentException();
    if (repeatDelayUnit == null) throw new IllegalArgumentException();

    this.repeatDelay = repeatDelay;
    this.repeatDelayUnit = repeatDelayUnit;
  }

  /**
   * Gets the {@link #repeatDelay}.
   *
   * @return the minimum amount of time to wait between repeated executions of the {@link
   *     PipelineJob} (note that this delay does not apply to the first run)
   */
  public long getRepeatDelay() {
    return repeatDelay;
  }

  /**
   * Gets the {@link #repeatDelayUnit}.
   *
   * @return the repeat delay unit
   */
  public TemporalUnit getRepeatDelayUnit() {
    return repeatDelayUnit;
  }
}
