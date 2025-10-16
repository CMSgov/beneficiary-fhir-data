package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;

/** The interface for CCW RIF Load pre-validation. */
public interface CcwRifLoadPreValidateInterface {
  /**
   * All interfaces will need to know about application state.
   *
   * @param appState will need a {@link PipelineApplicationState}.
   */
  public void init(PipelineApplicationState appState);

  /**
   * Validity will be what the implementation decides.
   *
   * @param dataSetManifest a {@link DataSetManifest}
   * @return {@code boolean} the validity status
   * @throws Exception if there is an issue during validation
   */
  public boolean isValid(DataSetManifest dataSetManifest) throws Exception;
}
