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
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/** Implementation of AbstractClaimRdaSink that adds FISS claim specific methods. */
public class FissClaimRdaSink extends AbstractClaimRdaSink<FissClaimChange, RdaFissClaim> {
  /**
   * Some FISS claim records received from the IDR are not real claims. These records can be
   * recognized by their DCN values. Any FISS claim with DCN that is all zeros (and/or spaces) or
   * has X in positions 15-17 is a claim that has been marked as deleted on FISS and should be
   * ignored. This regex is used by {@link #isValidMessage} to recognize these bad claims.
   */
  private static final Pattern InvalidDcnRegex = Pattern.compile("(^[0 ]+$)|(^.{14}XXX)");

  /** The claim transformer. */
  private final FissClaimTransformer transformer;

  /**
   * Instantiates a new Fiss claim rda sink.
   *
   * @param appState the app state
   * @param transformer the transformer
   * @param autoUpdateLastSeq whether to automatically update the sequence number
   * @param errorLimit the error limit
   */
  public FissClaimRdaSink(
      PipelineApplicationState appState,
      FissClaimTransformer transformer,
      boolean autoUpdateLastSeq,
      int errorLimit) {
    super(appState, RdaApiProgress.ClaimType.FISS, autoUpdateLastSeq, errorLimit);
    this.transformer =
        transformer.withMbiCache(transformer.getMbiCache().withDatabaseLookup(super.entityManager));
  }

  /**
   * {@inheritDoc} This implementation checks the DCN of the claim against known-bad values.
   *
   * @param fissClaimChange Message received from the RDA API
   * @return true if the claim should be stored, false otherwise
   */
  @Override
  public boolean isValidMessage(FissClaimChange fissClaimChange) {
    return !InvalidDcnRegex.matcher(fissClaimChange.getDcn()).find();
  }

  /** {@inheritDoc} */
  @Override
  public String getClaimIdForMessage(FissClaimChange object) {
    return object.getClaim().getDcn();
  }

  /** {@inheritDoc} */
  @Override
  public long getSequenceNumberForObject(FissClaimChange object) {
    return object.getSeq();
  }

  /** {@inheritDoc} */
  @Nonnull
  @Override
  RdaChange<RdaFissClaim> transformMessageImpl(String apiVersion, FissClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }

  /** {@inheritDoc} */
  @Override
  int getInsertCount(RdaFissClaim claim) {
    return 1 // Add one for the base claim
        + claim.getProcCodes().size()
        + claim.getDiagCodes().size()
        + claim.getPayers().size()
        + claim.getAuditTrail().size();
  }

  /** {@inheritDoc} */
  @Override
  RdaClaimMessageMetaData createMetaData(RdaChange<RdaFissClaim> change) {
    final RdaFissClaim claim = change.getClaim();
    return RdaClaimMessageMetaData.builder()
        .sequenceNumber(change.getSequenceNumber())
        .claimType(RdaApiProgress.ClaimType.FISS)
        .claimId(claim.getClaimId())
        .mbiRecord(claim.getMbiRecord())
        .claimState(String.valueOf(claim.getCurrStatus()))
        .lastUpdated(claim.getLastUpdated())
        .locations(new StringList().add(claim.getCurrLoc1()).addIfNonEmpty(claim.getCurrLoc2()))
        .transactionDate(claim.getCurrTranDate())
        .phase(change.getSource().getPhase())
        .phaseSeqNum(change.getSource().getPhaseSeqNum())
        .extractDate(change.getSource().getExtractDate())
        .transmissionTimestamp(change.getSource().getTransmissionTimestamp())
        .build();
  }

  /** {@inheritDoc} */
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
        .status(MessageError.Status.UNRESOLVED)
        .build();
  }
}
