package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import java.io.Serializable;

/** Models the (mostly) user-configurable options for the {@link CcwRifLoadJob}. */
public final class CcwRifLoadOptions implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ExtractionOptions extractionOptions;
  private final LoadAppOptions loadOptions;

  /**
   * Constructs a new {@link CcwRifLoadOptions} instance.
   *
   * @param extractionOptions the value to use for {@link #getExtractionOptions()}
   * @param loadOptions the value to use for {@link #getLoadOptions()}
   */
  public CcwRifLoadOptions(ExtractionOptions extractionOptions, LoadAppOptions loadOptions) {
    this.extractionOptions = extractionOptions;
    this.loadOptions = loadOptions;
  }

  /** @return the options related to finding and extracting data sets from S3 */
  public ExtractionOptions getExtractionOptions() {
    return extractionOptions;
  }

  /** @return the options related to loading data into the BFD database */
  public LoadAppOptions getLoadOptions() {
    return loadOptions;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CcwRifLoadOptions [extractionOptions=");
    builder.append(extractionOptions);
    builder.append(", loadOptions=");
    builder.append(loadOptions);
    builder.append("]");
    return builder.toString();
  }
}
