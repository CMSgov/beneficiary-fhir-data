package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import java.time.Duration;
import java.util.Optional;
import lombok.Getter;

/** Models the (mostly) user-configurable options for the {@link CcwRifLoadJob}. */
@Getter
public final class CcwRifLoadOptions {

  /** The data extraction options. */
  private final ExtractionOptions extractionOptions;

  /** The application load options. */
  private final LoadAppOptions loadOptions;

  /** Time between runs of the {@link CcwRifLoadJob}. Empty means to run exactly once. */
  private final Optional<Duration> runInterval;

  /**
   * Constructs a new {@link CcwRifLoadOptions} instance.
   *
   * @param extractionOptions the value to use for {@link #extractionOptions}
   * @param loadOptions the value to use for {@link #loadOptions}
   * @param runInterval used to construct the job schedule
   */
  public CcwRifLoadOptions(
      ExtractionOptions extractionOptions,
      LoadAppOptions loadOptions,
      Optional<Duration> runInterval) {
    this.extractionOptions = extractionOptions;
    this.loadOptions = loadOptions;
    this.runInterval = runInterval;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CcwRifLoadOptions [extractionOptions=");
    builder.append(extractionOptions);
    builder.append(", loadOptions=");
    builder.append(loadOptions);
    builder.append(", runInterval=");
    builder.append(runInterval);
    builder.append("]");
    return builder.toString();
  }
}
