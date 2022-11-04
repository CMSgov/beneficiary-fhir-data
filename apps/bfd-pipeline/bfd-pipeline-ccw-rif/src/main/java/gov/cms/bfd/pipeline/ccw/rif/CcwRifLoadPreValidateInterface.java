package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;

/** The interface for CCW RIF Load pre-validation. */
public interface CcwRifLoadPreValidateInterface {
  /** implementations will need a PipelineApplicationState. */
  public void init(PipelineApplicationState appState);

  /**
   * Gets the CCW Codebook variable's name.
   *
   * @return {@link boolean} the validity status
   */
  public boolean isValid(Object object) throws Exception;
}
