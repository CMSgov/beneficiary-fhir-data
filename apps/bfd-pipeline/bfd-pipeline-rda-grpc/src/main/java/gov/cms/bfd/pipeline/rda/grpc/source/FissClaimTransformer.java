package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import java.time.Clock;
import java.util.List;

/**
 * Transforms a gRPC FissClaim object into a Hibernate PreAdjFissClaim object. Note that the gRPC
 * data objects are not proper java beans since their optional field getters should only be called
 * if their corresponding &quot;has&quot; methods return true. Optional fields are ignored when not
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
    final PreAdjFissClaim to = transformClaim(from, transformer);

    transformProcCodes(from, transformer, to);
    transformDiagCodes(from, transformer, to);
    List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: dcn=%s errors=%s", errors.size(), from.getDcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return to;
  }

  private PreAdjFissClaim transformClaim(FissClaim from, DataTransformer transformer) {
    final PreAdjFissClaim to = new PreAdjFissClaim();
    transformer
        .copyString(PreAdjFissClaim.Fields.dcn, false, 1, 23, from.getDcn(), to::setDcn)
        .copyString(PreAdjFissClaim.Fields.hicNo, false, 1, 12, from.getHicNo(), to::setHicNo)
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
            PreAdjFissClaim.Fields.currLoc2, false, 1, 5, from.getCurrLoc2(), to::setCurrLoc2)
        .copyOptionalString(
            PreAdjFissClaim.Fields.medaProvId,
            1,
            13,
            from::hasMedaProvId,
            from::getMedaProvId,
            to::setMedaProvId)
        .copyOptionalAmount(
            PreAdjFissClaim.Fields.totalChargeAmount,
            from::hasTotalChargeAmount,
            from::getTotalChargeAmount,
            to::setTotalChargeAmount)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.receivedDate,
            from::hasRecdDt,
            from::getRecdDt,
            to::setReceivedDate)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.currTranDate,
            from::hasCurrTranDate,
            from::getCurrTranDate,
            to::setCurrTranDate)
        .copyOptionalString(
            PreAdjFissClaim.Fields.admitDiagCode,
            1,
            7,
            from::hasAdmDiagCode,
            from::getAdmDiagCode,
            to::setAdmitDiagCode)
        .copyOptionalString(
            PreAdjFissClaim.Fields.principleDiag,
            1,
            7,
            from::hasPrincipleDiag,
            from::getPrincipleDiag,
            to::setPrincipleDiag)
        .copyOptionalString(
            PreAdjFissClaim.Fields.npiNumber,
            1,
            10,
            from::hasNpiNumber,
            from::getNpiNumber,
            to::setNpiNumber)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocAddr1,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocAddr1,
            from::getPracLocAddr1,
            to::setPracLocAddr1)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocAddr2,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocAddr2,
            from::getPracLocAddr2,
            to::setPracLocAddr2)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocCity,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocCity,
            from::getPracLocCity,
            to::setPracLocCity)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocState,
            1,
            2,
            from::hasPracLocState,
            from::getPracLocState,
            to::setPracLocState)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocZip,
            1,
            15,
            from::hasPracLocZip,
            from::getPracLocZip,
            to::setPracLocZip);
    if (from.hasMbi()) {
      final String mbi = from.getMbi();
      transformer
          .copyString(PreAdjFissClaim.Fields.mbi, true, 1, 13, mbi, to::setMbi)
          .copyString(
              PreAdjFissClaim.Fields.mbiHash,
              true,
              64,
              64,
              idHasher.computeIdentifierHash(mbi),
              to::setMbiHash);
    }
    transformer.copyOptionalString(
        PreAdjFissClaim.Fields.fedTaxNumber,
        1,
        10,
        from::hasFedTaxNb,
        from::getFedTaxNb,
        to::setFedTaxNumber);
    to.setLastUpdated(clock.instant());
    return to;
  }

  private void transformProcCodes(FissClaim from, DataTransformer transformer, PreAdjFissClaim to) {
    short priority = 0;
    for (FissProcedureCode fromCode : from.getFissProcCodesList()) {
      String fieldPrefix = "procCode-" + priority + "-";
      PreAdjFissProcCode toCode =
          transformProcCode(transformer, to, priority, fromCode, fieldPrefix);
      to.getProcCodes().add(toCode);
      priority += 1;
    }
  }

  private void transformDiagCodes(FissClaim from, DataTransformer transformer, PreAdjFissClaim to) {
    short priority = 0;
    for (FissDiagnosisCode fromCode : from.getFissDiagCodesList()) {
      String fieldPrefix = "diagCode-" + priority + "-";
      PreAdjFissDiagnosisCode toCode =
          transformDiagCode(transformer, to, priority, fromCode, fieldPrefix);
      to.getDiagCodes().add(toCode);
      priority += 1;
    }
  }

  private PreAdjFissProcCode transformProcCode(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissProcedureCode fromCode,
      String fieldPrefix) {
    PreAdjFissProcCode toCode = new PreAdjFissProcCode();
    toCode.setDcn(to.getDcn());
    toCode.setPriority(priority);
    transformer
        .copyString(
            fieldPrefix + PreAdjFissProcCode.Fields.procCode,
            false,
            1,
            10,
            fromCode.getProcCd(),
            toCode::setProcCode)
        .copyOptionalString(
            fieldPrefix + PreAdjFissProcCode.Fields.procFlag,
            1,
            4,
            fromCode::hasProcFlag,
            fromCode::getProcFlag,
            toCode::setProcFlag)
        .copyOptionalDate(
            fieldPrefix + PreAdjFissProcCode.Fields.procDate,
            fromCode::hasProcDt,
            fromCode::getProcDt,
            toCode::setProcDate);
    toCode.setLastUpdated(to.getLastUpdated());
    return toCode;
  }

  private PreAdjFissDiagnosisCode transformDiagCode(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissDiagnosisCode fromCode,
      String fieldPrefix) {
    PreAdjFissDiagnosisCode toCode = new PreAdjFissDiagnosisCode();
    toCode.setDcn(to.getDcn());
    toCode.setPriority(priority);
    transformer
        .copyString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.diagCd2,
            false,
            1,
            7,
            fromCode.getDiagCd2(),
            toCode::setDiagCd2)
        .copyEnumAsAsciiCharacterString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.diagPoaInd,
            fromCode.getDiagPoaInd(),
            FissDiagnosisPresentOnAdmissionIndicator.DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_UNSET,
            FissDiagnosisPresentOnAdmissionIndicator.UNRECOGNIZED,
            toCode::setDiagPoaInd)
        .copyOptionalString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.bitFlags,
            1,
            4,
            fromCode::hasBitFlags,
            fromCode::getBitFlags,
            toCode::setBitFlags);
    toCode.setLastUpdated(to.getLastUpdated());
    return toCode;
  }
}
