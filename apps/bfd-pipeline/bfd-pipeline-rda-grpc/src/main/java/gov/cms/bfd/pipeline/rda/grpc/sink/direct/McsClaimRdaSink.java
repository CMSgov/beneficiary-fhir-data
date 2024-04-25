package gov.cms.bfd.pipeline.rda.grpc.sink.direct;

import gov.cms.bfd.model.rda.AbstractJsonConverter;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.model.rda.RdaApiProgress;
import gov.cms.bfd.model.rda.RdaClaimMessageMetaData;
import gov.cms.bfd.model.rda.StringList;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/** Implementation of AbstractClaimRdaSink that adds MCS claim specific methods. */
public class McsClaimRdaSink extends AbstractClaimRdaSink<McsClaimChange, RdaMcsClaim> {
  /** The claim transformer. */
  private final McsClaimTransformer transformer;

  /**
   * Instantiates a new Mcs claim rda sink.
   *
   * @param appState the app state
   * @param transformer the transformer
   * @param autoUpdateLastSeq whether to automatically update the sequence number
   * @param errorLimit the error limit
   */
  public McsClaimRdaSink(
      PipelineApplicationState appState,
      McsClaimTransformer transformer,
      boolean autoUpdateLastSeq,
      int errorLimit) {
    super(appState, RdaApiProgress.ClaimType.MCS, autoUpdateLastSeq, errorLimit);
    this.transformer =
        transformer.withMbiCache(transformer.getMbiCache().withDatabaseLookup(transactionManager));
  }

  /**
   * This implementation checks the {@link McsClaimChange#getChangeType}.
   *
   * <p>{@inheritDoc}
   *
   * @param mcsClaimChange message to check
   * @return true if {@link McsClaimChange#getChangeType} is {@link ChangeType#CHANGE_TYPE_DELETE}.
   */
  @Override
  public boolean isDeleteMessage(McsClaimChange mcsClaimChange) {
    return mcsClaimChange.getChangeType() == ChangeType.CHANGE_TYPE_DELETE;
  }

  @Override
  public String getClaimIdForMessage(McsClaimChange object) {
    return object.getClaim().getIdrClmHdIcn();
  }

  @Override
  public long getSequenceNumberForObject(McsClaimChange object) {
    return object.getSeq();
  }

  @Nonnull
  @Override
  RdaChange<RdaMcsClaim> transformMessageImpl(String apiVersion, McsClaimChange message) {
    var change = transformer.transformClaim(message);
    change.getClaim().setApiSource(apiVersion);
    return change;
  }

  @Override
  int getInsertCount(RdaMcsClaim claim) {
    return 1 // Add one for the base claim
        + claim.getDetails().size()
        + claim.getDiagCodes().size()
        + claim.getAdjustments().size()
        + claim.getAudits().size()
        + claim.getLocations().size();
  }

  @Override
  RdaClaimMessageMetaData createMetaData(RdaChange<RdaMcsClaim> change) {
    final RdaMcsClaim claim = change.getClaim();
    final var locations = new StringList();
    claim.getLocations().stream()
        .sorted(Comparator.comparing(RdaMcsLocation::getRdaPosition))
        .forEach(loc -> locations.addIfNonEmpty(loc.getIdrLocCode()));
    return RdaClaimMessageMetaData.builder()
        .sequenceNumber(change.getSequenceNumber())
        .claimType(RdaApiProgress.ClaimType.MCS)
        .claimId(claim.getIdrClmHdIcn())
        .mbiRecord(claim.getMbiRecord())
        .claimState(claim.getIdrStatusCode())
        .lastUpdated(claim.getLastUpdated())
        .locations(locations)
        .transactionDate(claim.getIdrStatusDate())
        .phase(change.getSource().getPhase())
        .phaseSeqNum(change.getSource().getPhaseSeqNum())
        .extractDate(change.getSource().getExtractDate())
        .transmissionTimestamp(change.getSource().getTransmissionTimestamp())
        .build();
  }

  @Override
  MessageError createMessageError(
      String apiVersion, McsClaimChange change, List<DataTransformer.ErrorMessage> errors)
      throws IOException {
    return MessageError.builder()
        .sequenceNumber(change.getSeq())
        .claimId(change.getClaim().getIdrClmHdIcn())
        .claimType(MessageError.ClaimType.MCS)
        .apiSource(apiVersion)
        .errors(AbstractJsonConverter.convertObjectToJsonString(errors))
        .message(protobufObjectWriter.print(change))
        .status(MessageError.Status.UNRESOLVED)
        .createdDate(clock.instant())
        .updatedDate(clock.instant())
        .build();
  }
}
