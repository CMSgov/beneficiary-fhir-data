package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadPreValidateInterface;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;

public class MockRifLoadPreValidationSynthea implements CcwRifLoadPreValidateInterface {
  /** our 'fake' validity value; used to create either true or false results */
  private boolean validity = false;

  /**
   * Initializes the {@link CcwRifLoadPreValidateInterface} prior to pre-validating Synthea data.
   *
   * @param appState the {@link PipelineApplicationState} for the overall application
   */
  @Override
  public void init(PipelineApplicationState appState) {}

  /**
   * Override of {@link CcwRifLoadPreValidateInterface#isValid(DataSetManifest)} which preforms
   * assorted checks to assert that Synthea load can proceed.
   *
   * @param manifest the {@link DataSetManifest} which will provide the various Synthea end-state
   *     property values that can be used to perform the pre-validation.
   * @return {@link boolean} asserting that the pre-validation succeeded (true) or failed (false).
   */
  @Override
  public boolean isValid(DataSetManifest manifest) throws Exception {
    return validity;
  }

  /**
   * Sets the {@link #validity}.
   *
   * @param value allowed object is {@link boolean }
   */
  public void setValidity(boolean value) {
    validity = value;
  }
}
