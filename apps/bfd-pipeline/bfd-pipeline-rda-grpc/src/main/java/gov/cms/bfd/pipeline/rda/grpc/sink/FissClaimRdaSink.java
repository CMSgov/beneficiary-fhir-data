package gov.cms.bfd.pipeline.rda.grpc.sink;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import java.util.Optional;

public class FissClaimRdaSink extends AbstractClaimRdaSink<PreAdjFissClaim> {
  public FissClaimRdaSink(PipelineApplicationState appState) {
    super(appState);
  }

  @Override
  public Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    try {
      Long sequenceNumber =
          entityManager
              .createQuery(
                  String.format(
                      "select max(c.%s) from PreAdjFissClaim c",
                      PreAdjFissClaim.Fields.sequenceNumber),
                  Long.class)
              .getSingleResult();
      return Optional.ofNullable(sequenceNumber);
    } catch (Exception ex) {
      throw new ProcessingException(ex, 0);
    }
  }
}
