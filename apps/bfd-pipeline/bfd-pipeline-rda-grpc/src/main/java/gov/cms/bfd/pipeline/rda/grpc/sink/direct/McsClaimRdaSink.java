package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import javax.annotation.Nonnull;

/** Implementation of AbstractClaimRdaSink that adds MCS claim specific methods. */
public class McsClaimRdaSink extends AbstractClaimRdaSink<McsClaimChange, PreAdjMcsClaim> {
  private final McsClaimTransformer transformer;

  public McsClaimRdaSink(
      PipelineApplicationState appState,
      McsClaimTransformer transformer,
      boolean autoUpdateLastSeq) {
    super(appState, RdaApiProgress.ClaimType.MCS, autoUpdateLastSeq);
    this.transformer =
        transformer.withMbiCache(transformer.getMbiCache().withDatabaseLookup(super.entityManager));
  }

  @Override
  public String getDedupKeyForMessage(McsClaimChange object) {
    return object.getClaim().getIdrClmHdIcn();
  }

  @Override
  public long getSequenceNumberForObject(McsClaimChange object) {
    return object.getSeq();
  }

  @Nonnull
  @Override
  public RdaChange<PreAdjMcsClaim> transformMessage(String apiVersion, McsClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }
}
