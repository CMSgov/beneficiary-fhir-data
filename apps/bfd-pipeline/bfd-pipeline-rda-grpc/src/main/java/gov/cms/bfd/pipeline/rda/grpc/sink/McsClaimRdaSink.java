package gov.cms.bfd.pipeline.rda.grpc.sink;

import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.util.Optional;

/**
 * Implementation of AbstractClaimRdaSink that adds the appropriate query to obtain maximum sequence
 * number for MCS claims.
 */
public class McsClaimRdaSink extends AbstractClaimRdaSink<PartAdjMcsClaim> {
  public McsClaimRdaSink(PipelineApplicationState appState) {
    super(appState);
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return readMaxExistingSequenceNumber(
        String.format(
            "select max(c.%s) from PartAdjMcsClaim c", PartAdjMcsClaim.Fields.sequenceNumber));
  }
}
