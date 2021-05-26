package gov.cms.bfd.pipeline.sharedutils;

import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

/**
 * Represents the schedule to run an automatically-repeating {@link PipelineJob} on. Note that many
 * {@link PipelineJob} implementations don't/can't run on such a repeating schedule and are instead
 * triggered by other means.
 */
public final class PipelineJobSchedule {
  private final long repeatDelay;
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
   * @return the minimum amount of time to wait between repeated executions of the {@link
   *     PipelineJob} (note that this delay does not apply to the first run)
   */
  public long getRepeatDelay() {
    return repeatDelay;
  }

  /** @return the {@link TimeUnit} of the {@link #getRepeatDelay()} value */
  public TemporalUnit getRepeatDelayUnit() {
    return repeatDelayUnit;
  }
}
