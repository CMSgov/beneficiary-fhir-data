package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiClaimMessageMetaData;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;

/** Implementation of AbstractClaimRdaSink that adds MCS claim specific methods. */
public class McsClaimRdaSink extends AbstractClaimRdaSink<McsClaimChange, RdaMcsClaim> {
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
  public RdaChange<RdaMcsClaim> transformMessage(String apiVersion, McsClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }

  @Override
  RdaApiClaimMessageMetaData createMetaData(RdaChange<RdaMcsClaim> change) {
    final RdaMcsClaim claim = change.getClaim();
    return RdaApiClaimMessageMetaData.builder()
        .sequenceNumber(change.getSequenceNumber())
        .claimType(RdaApiProgress.ClaimType.MCS)
        .claimId(claim.getIdrClmHdIcn())
        .mbiRecord(claim.getMbiRecord())
        .claimState(claim.getIdrStatusCode())
        .receivedDate(claim.getLastUpdated())
        .build();
  }

  @Override
  MessageError createMessageError(McsClaimChange change, List<DataTransformer.ErrorMessage> errors)
      throws IOException {
    return MessageError.builder()
        .sequenceNumber(change.getSeq())
        .claimId(change.getClaim().getIdrClmHdIcn())
        .claimType(MessageError.ClaimType.MCS)
        .errors(mapper.writeValueAsString(errors))
        .message(writer.print(change))
        .build();
  }
}
