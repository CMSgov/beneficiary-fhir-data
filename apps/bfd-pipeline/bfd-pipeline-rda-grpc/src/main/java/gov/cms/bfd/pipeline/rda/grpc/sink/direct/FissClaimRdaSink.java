package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import gov.cms.bfd.model.rda.AbstractJsonConverter;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.StringList;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;

/** Implementation of AbstractClaimRdaSink that adds FISS claim specific methods. */
public class FissClaimRdaSink extends AbstractClaimRdaSink<FissClaimChange, RdaFissClaim> {
  private final FissClaimTransformer transformer;

  public FissClaimRdaSink(
      PipelineApplicationState appState,
      FissClaimTransformer transformer,
      boolean autoUpdateLastSeq) {
    super(appState, RdaApiProgress.ClaimType.FISS, autoUpdateLastSeq);
    this.transformer =
        transformer.withMbiCache(transformer.getMbiCache().withDatabaseLookup(super.entityManager));
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
  RdaChange<RdaFissClaim> transformMessageImpl(String apiVersion, FissClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }

  @Override
  int getInsertCount(RdaFissClaim claim) {
    return 1 // Add one for the base claim
        + claim.getProcCodes().size()
        + claim.getDiagCodes().size()
        + claim.getPayers().size()
        + claim.getAuditTrail().size();
  }

  @Override
  RdaClaimMessageMetaData createMetaData(RdaChange<RdaFissClaim> change) {
    final RdaFissClaim claim = change.getClaim();
    return RdaClaimMessageMetaData.builder()
        .sequenceNumber(change.getSequenceNumber())
        .claimType(RdaApiProgress.ClaimType.FISS)
        .claimId(claim.getDcn())
        .mbiRecord(claim.getMbiRecord())
        .claimState(String.valueOf(claim.getCurrStatus()))
        .receivedDate(claim.getLastUpdated())
        .locations(new StringList().add(claim.getCurrLoc1()).addIfNonEmpty(claim.getCurrLoc2()))
        .transactionDate(claim.getCurrTranDate())
        .phase(change.getSource().getPhase())
        .phaseSeqNum(change.getSource().getPhaseSeqNum())
        .extractDate(change.getSource().getExtractDate())
        .transmissionTimestamp(change.getSource().getTransmissionTimestamp())
        .build();
  }

  @Override
  MessageError createMessageError(
      String apiVersion, FissClaimChange change, List<DataTransformer.ErrorMessage> errors)
      throws IOException {
    return MessageError.builder()
        .sequenceNumber(change.getSeq())
        .claimId(change.getClaim().getDcn())
        .claimType(MessageError.ClaimType.FISS)
        .apiSource(apiVersion)
        .errors(AbstractJsonConverter.convertObjectToJsonString(errors))
        .message(protobufObjectWriter.print(change))
        .obsolete(false)
        .build();
  }
}
