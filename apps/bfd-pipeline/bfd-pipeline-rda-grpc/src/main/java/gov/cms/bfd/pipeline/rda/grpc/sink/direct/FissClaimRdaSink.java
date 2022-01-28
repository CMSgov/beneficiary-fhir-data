package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import javax.annotation.Nonnull;

/** Implementation of AbstractClaimRdaSink that adds FISS claim specific methods. */
public class FissClaimRdaSink extends AbstractClaimRdaSink<FissClaimChange, PreAdjFissClaim> {
  private final FissClaimTransformer transformer;

  public FissClaimRdaSink(
      PipelineApplicationState appState,
      FissClaimTransformer transformer,
      boolean autoUpdateLastSeq) {
    super(appState, RdaApiProgress.ClaimType.FISS, autoUpdateLastSeq);
    this.transformer =
        transformer.withIdHasher(
            new DatabaseMbiHasher(transformer.getIdHasher().getConfig(), super.entityManager));
  }

  @Override
  public String getDedupKeyForMessage(FissClaimChange object) {
    return object.getClaim().getDcn();
  }

  @Override
  public long getSequenceNumberForObject(FissClaimChange object) {
    return object.getSeq();
  }

  @Nonnull
  @Override
  public RdaChange<PreAdjFissClaim> transformMessage(String apiVersion, FissClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }
}
