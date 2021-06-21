package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import java.time.Clock;
import java.util.List;

/**
 * Transforms a gRPC FissClaim object into a Hibernate PreAdjClaim object. Note that the gRPC data
 * objects are not proper java beans since their optional field getters should only be called if
 * their corresponding &quot;has&quot; methods return true. Optional fields are ignored when not
 * present. All other fields are validated and copied into a new PreAdjFissClaim object. A
 * lastUpdated time stamp is set using a Clock (for easier testing) and the MBI is hashed using an
 * IdHasher.
 */
public class FissClaimTransformer {
  private final Clock clock;
  private final IdHasher idHasher;

  public FissClaimTransformer(Clock clock, IdHasher idHasher) {
    this.clock = clock;
    this.idHasher = idHasher;
  }

  public PreAdjFissClaim transformClaim(FissClaim from) {
    final DataTransformer transformer = new DataTransformer();
    final PreAdjFissClaim to = new PreAdjFissClaim();
    transformer
        .copyString(PreAdjFissClaim.Fields.dcn, from.getDcn(), false, 1, 23, to::setDcn)
        .copyString(PreAdjFissClaim.Fields.hicNo, from.getHicNo(), false, 1, 12, to::setHicNo)
        .copyEnumAsAsciiCharacter(
            PreAdjFissClaim.Fields.currStatus,
            from.getCurrStatus(),
            FissClaimStatus.CLAIM_STATUS_UNSET,
            FissClaimStatus.UNRECOGNIZED,
            to::setCurrStatus)
        .copyEnumAsAsciiCharacter(
            PreAdjFissClaim.Fields.currLoc1,
            from.getCurrLoc1(),
            FissProcessingType.PROCESSING_TYPE_UNSET,
            FissProcessingType.UNRECOGNIZED,
            to::setCurrLoc1)
        .copyString(
            PreAdjFissClaim.Fields.currLoc2, from.getCurrLoc2(), false, 1, 5, to::setCurrLoc2);
    if (from.hasMedaProvId()) {
      transformer.copyString(
          PreAdjFissClaim.Fields.medaProvId, from.getMedaProvId(), true, 1, 13, to::setMedaProvId);
    }
    if (from.hasTotalChargeAmount()) {
      transformer.copyAmount(
          PreAdjFissClaim.Fields.totalChargeAmount,
          from.getTotalChargeAmount(),
          true,
          to::setTotalChargeAmount);
    }
    if (from.hasRecdDt()) {
      transformer.copyDate(
          PreAdjFissClaim.Fields.receivedDate, from.getRecdDt(), true, to::setReceivedDate);
    }
    if (from.hasCurrTranDate()) {
      transformer.copyDate(
          PreAdjFissClaim.Fields.currTranDate, from.getCurrTranDate(), true, to::setCurrTranDate);
    }
    if (from.hasAdmDiagCode()) {
      transformer.copyString(
          PreAdjFissClaim.Fields.admitDiagCode,
          from.getAdmDiagCode(),
          true,
          1,
          7,
          to::setAdmitDiagCode);
    }
    if (from.hasPrincipleDiag()) {
      transformer.copyString(
          PreAdjFissClaim.Fields.principleDiag,
          from.getPrincipleDiag(),
          true,
          1,
          7,
          to::setPrincipleDiag);
    }
    if (from.hasNpiNumber()) {
      transformer.copyString(
          PreAdjFissClaim.Fields.npiNumber, from.getNpiNumber(), true, 1, 10, to::setNpiNumber);
    }
    if (from.hasMbi()) {
      final String mbi = from.getMbi();
      transformer
          .copyString(PreAdjFissClaim.Fields.mbi, mbi, true, 1, 13, to::setMbi)
          .copyString(
              PreAdjFissClaim.Fields.mbiHash,
              idHasher.computeIdentifierHash(mbi),
              true,
              64,
              64,
              to::setMbiHash);
    }
    if (from.hasFedTaxNb()) {
      transformer.copyString(
          PreAdjFissClaim.Fields.fedTaxNumber,
          from.getFedTaxNb(),
          true,
          1,
          10,
          to::setFedTaxNumber);
    }
    to.setLastUpdated(clock.instant());

    short priority = 0;
    for (FissProcedureCode fromCode : from.getFissProcCodesList()) {
      String fieldPrefix = "procCode-" + priority + "-";
      PreAdjFissProcCode toCode = new PreAdjFissProcCode();
      toCode.setDcn(to.getDcn());
      toCode.setPriority(priority);
      transformer.copyString(
          fieldPrefix + PreAdjFissProcCode.Fields.procCode,
          fromCode.getProcCd(),
          false,
          1,
          10,
          toCode::setProcCode);
      if (fromCode.hasProcFlag()) {
        transformer.copyString(
            fieldPrefix + PreAdjFissProcCode.Fields.procFlag,
            fromCode.getProcFlag(),
            true,
            1,
            4,
            toCode::setProcFlag);
      }
      if (fromCode.hasProcDt()) {
        transformer.copyDate(
            fieldPrefix + PreAdjFissProcCode.Fields.procDate,
            fromCode.getProcDt(),
            true,
            toCode::setProcDate);
      }
      toCode.setLastUpdated(to.getLastUpdated());
      to.getProcCodes().add(toCode);
      priority += 1;
    }
    List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: dcn=%s errors=%s", errors.size(), from.getDcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return to;
  }
}
