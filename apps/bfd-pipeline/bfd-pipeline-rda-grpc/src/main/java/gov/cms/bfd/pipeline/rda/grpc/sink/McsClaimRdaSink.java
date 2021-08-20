package gov.cms.bfd.pipeline.rda.grpc.sink;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.util.Optional;

public class McsClaimRdaSink extends AbstractClaimRdaSink<PreAdjMcsClaim> {
  public McsClaimRdaSink(PipelineApplicationState appState) {
    super(appState);
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    try {
      Long sequenceNumber =
          entityManager
              .createQuery(
                  String.format(
                      "select max(c.%s) from PreAdjMcsClaim c",
                      PreAdjMcsClaim.Fields.sequenceNumber),
                  Long.class)
              .getSingleResult();
      return Optional.ofNullable(sequenceNumber);
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }
}
