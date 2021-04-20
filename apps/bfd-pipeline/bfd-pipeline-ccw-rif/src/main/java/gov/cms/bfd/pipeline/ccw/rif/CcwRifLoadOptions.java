package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;

/** Models the (mostly) user-configurable options for the {@link CcwRifLoadJob}. */
public final class CcwRifLoadOptions {
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
}
