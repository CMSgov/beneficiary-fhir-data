package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForSpecialFacilities;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
import java.time.Clock;
import java.time.Instant;
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
  private final EnumStringExtractor<FissClaim, FissClaimStatus>
      PreAdjFissClaim_currStatus_Extractor;

  private final EnumStringExtractor<FissClaim, FissProcessingType>
      PreAdjFissClaim_currLoc1_Extractor;

  private final EnumStringExtractor<FissClaim, FissCurrentLocation2>
      PreAdjFissClaim_currLoc2_Extractor;

  private final EnumStringExtractor<FissClaim, FissBillFacilityType>
      PreAdjFissClaim_lobCd_Extractor;

  private final EnumStringExtractor<FissClaim, FissBillClassification>
      PreAdjFissClaim_servTypeCd_Extractor;

  private final EnumStringExtractor<FissClaim, FissBillClassificationForClinics>
      PreAdjFissClaim_servTypeCdForClinics_Extractor;

  private final EnumStringExtractor<FissClaim, FissBillClassificationForSpecialFacilities>
      PreAdjFissClaim_servTypeCdForSpecialFacilities_Extractor;

  private final EnumStringExtractor<FissClaim, FissBillFrequency> PreAdjFissClaim_freqCd_Extractor;

  private final EnumStringExtractor<FissDiagnosisCode, FissDiagnosisPresentOnAdmissionIndicator>
      PreAdjFissDiagnosisCode_diagPoaInd_Extractor;

  private final EnumStringExtractor<FissPayer, FissPayersCode>
      PreAdjFissPayer_insuredPayer_payersId_Extractor;

  private final EnumStringExtractor<FissPayer, FissReleaseOfInformation>
      PreAdjFissPayer_insuredPayer_relInd_Extractor;

  private final EnumStringExtractor<FissPayer, FissAssignmentOfBenefitsIndicator>
      PreAdjFissPayer_insuredPayer_assignInd_Extractor;

  private final EnumStringExtractor<FissPayer, FissPatientRelationshipCode>
      PreAdjFissPayer_insuredPayer_insuredRel_Extractor;

  private final EnumStringExtractor<FissPayer, FissBeneficiarySex>
      PreAdjFissPayer_insuredPayer_insuredSex_Extractor;

  private final EnumStringExtractor<FissPayer, FissPatientRelationshipCode>
      PreAdjFissPayer_insuredPayer_insuredRelX12_Extractor;

  private final EnumStringExtractor<FissPayer, FissPayersCode>
      PreAdjFissPayer_beneZPayer_payersId_Extractor;

  private final EnumStringExtractor<FissPayer, FissAssignmentOfBenefitsIndicator>
      PreAdjFissPayer_beneZPayer_relInd_Extractor;

  private final EnumStringExtractor<FissPayer, FissAssignmentOfBenefitsIndicator>
      PreAdjFissPayer_beneZPayer_assignInd_Extractor;

  private final EnumStringExtractor<FissPayer, FissPatientRelationshipCode>
      PreAdjFissPayer_beneZPayer_beneRel_Extractor;

  private final EnumStringExtractor<FissPayer, FissBeneficiarySex>
      PreAdjFissPayer_beneZPayer_beneSex_Extractor;

  private final EnumStringExtractor<FissPayer, FissBeneficiarySex>
      PreAdjFissPayer_beneZPayer_insuredSex_Extractor;

  private final EnumStringExtractor<FissPayer, FissPatientRelationshipCode>
      PreAdjFissPayer_beneZPayer_insuredRelX12_Extractor;

  private final Clock clock;
  private final IdHasher idHasher;

  public FissClaimTransformer(Clock clock, IdHasher idHasher) {
    this.clock = clock;
    this.idHasher = idHasher;
    PreAdjFissClaim_currStatus_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasCurrStatusEnum,
            FissClaim::getCurrStatusEnum,
            FissClaim::hasCurrStatusUnrecognized,
            FissClaim::getCurrStatusUnrecognized,
            FissClaimStatus.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of(EnumStringExtractor.Options.RejectUnrecognized));
    PreAdjFissClaim_currLoc1_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasCurrLoc1Enum,
            FissClaim::getCurrLoc1Enum,
            FissClaim::hasCurrLoc1Unrecognized,
            FissClaim::getCurrLoc1Unrecognized,
            FissProcessingType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_currLoc2_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasCurrLoc2Enum,
            FissClaim::getCurrLoc2Enum,
            FissClaim::hasCurrLoc2Unrecognized,
            FissClaim::getCurrLoc2Unrecognized,
            FissCurrentLocation2.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_lobCd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasLobCdEnum,
            FissClaim::getLobCdEnum,
            FissClaim::hasLobCdUnrecognized,
            FissClaim::getLobCdUnrecognized,
            FissBillFacilityType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_servTypeCd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasServTypeCdEnum,
            FissClaim::getServTypeCdEnum,
            ignored -> false,
            ignored -> null,
            FissBillClassification.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_servTypeCdForClinics_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasServTypeCdForClinicsEnum,
            FissClaim::getServTypeCdForClinicsEnum,
            ignored -> false,
            ignored -> null,
            FissBillClassificationForClinics.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_servTypeCdForSpecialFacilities_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasServTypeCdForSpecialFacilitiesEnum,
            FissClaim::getServTypeCdForSpecialFacilitiesEnum,
            ignored -> false,
            ignored -> null,
            FissBillClassificationForSpecialFacilities.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_freqCd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasFreqCdEnum,
            FissClaim::getFreqCdEnum,
            FissClaim::hasFreqCdUnrecognized,
            FissClaim::getFreqCdUnrecognized,
            FissBillFrequency.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissDiagnosisCode_diagPoaInd_Extractor =
        new EnumStringExtractor<>(
            FissDiagnosisCode::hasDiagPoaIndEnum,
            FissDiagnosisCode::getDiagPoaIndEnum,
            FissDiagnosisCode::hasDiagPoaIndUnrecognized,
            FissDiagnosisCode::getDiagPoaIndUnrecognized,
            FissDiagnosisPresentOnAdmissionIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_payersId_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasInsuredPayer() && message.getInsuredPayer().hasPayersIdEnum(),
            message -> message.getInsuredPayer().getPayersIdEnum(),
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasPayersIdUnrecognized(),
            message -> message.getInsuredPayer().getPayersIdUnrecognized(),
            FissPayersCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_relInd_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasInsuredPayer() && message.getInsuredPayer().hasRelIndEnum(),
            message -> message.getInsuredPayer().getRelIndEnum(),
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasRelIndUnrecognized(),
            message -> message.getInsuredPayer().getRelIndUnrecognized(),
            FissReleaseOfInformation.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_assignInd_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasInsuredPayer() && message.getInsuredPayer().hasAssignIndEnum(),
            message -> message.getInsuredPayer().getAssignIndEnum(),
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasAssignIndUnrecognized(),
            message -> message.getInsuredPayer().getAssignIndUnrecognized(),
            FissAssignmentOfBenefitsIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_insuredRel_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredRelEnum(),
            message -> message.getInsuredPayer().getInsuredRelEnum(),
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredRelUnrecognized(),
            message -> message.getInsuredPayer().getInsuredRelUnrecognized(),
            FissPatientRelationshipCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_insuredSex_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredSexEnum(),
            message -> message.getInsuredPayer().getInsuredSexEnum(),
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredSexUnrecognized(),
            message -> message.getInsuredPayer().getInsuredSexUnrecognized(),
            FissBeneficiarySex.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_insuredPayer_insuredRelX12_Extractor =
        new EnumStringExtractor<>(
            message ->
                message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredRelX12Enum(),
            message -> message.getInsuredPayer().getInsuredRelX12Enum(),
            message ->
                message.hasInsuredPayer()
                    && message.getInsuredPayer().hasInsuredRelX12Unrecognized(),
            message -> message.getInsuredPayer().getInsuredRelX12Unrecognized(),
            FissPatientRelationshipCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_payersId_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasPayersIdEnum(),
            message -> message.getBeneZPayer().getPayersIdEnum(),
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasPayersIdUnrecognized(),
            message -> message.getBeneZPayer().getPayersIdUnrecognized(),
            FissPayersCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_relInd_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasRelIndEnum(),
            message -> message.getBeneZPayer().getRelIndEnum(),
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasRelIndUnrecognized(),
            message -> message.getBeneZPayer().getRelIndUnrecognized(),
            FissAssignmentOfBenefitsIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_assignInd_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasAssignIndEnum(),
            message -> message.getBeneZPayer().getAssignIndEnum(),
            message ->
                message.hasBeneZPayer() && message.getBeneZPayer().hasAssignIndUnrecognized(),
            message -> message.getBeneZPayer().getAssignIndUnrecognized(),
            FissAssignmentOfBenefitsIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_beneRel_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneRelEnum(),
            message -> message.getBeneZPayer().getBeneRelEnum(),
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneRelUnrecognized(),
            message -> message.getBeneZPayer().getBeneRelUnrecognized(),
            FissPatientRelationshipCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_beneSex_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneSexEnum(),
            message -> message.getBeneZPayer().getBeneSexEnum(),
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneSexUnrecognized(),
            message -> message.getBeneZPayer().getBeneSexUnrecognized(),
            FissBeneficiarySex.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_insuredSex_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredSexEnum(),
            message -> message.getBeneZPayer().getInsuredSexEnum(),
            message ->
                message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredSexUnrecognized(),
            message -> message.getBeneZPayer().getInsuredSexUnrecognized(),
            FissBeneficiarySex.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissPayer_beneZPayer_insuredRelX12_Extractor =
        new EnumStringExtractor<>(
            message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredRelX12Enum(),
            message -> message.getBeneZPayer().getInsuredRelX12Enum(),
            message ->
                message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredRelX12Unrecognized(),
            message -> message.getBeneZPayer().getInsuredRelX12Unrecognized(),
            FissPatientRelationshipCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
  }

  public RdaChange<PreAdjFissClaim> transformClaim(FissClaimChange change) {
    FissClaim from = change.getClaim();
    final DataTransformer transformer = new DataTransformer();
    final PreAdjFissClaim to = transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());

    final List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: dcn=%s errors=%s", errors.size(), from.getDcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()));
  }

  private PreAdjFissClaim transformMessage(
      FissClaim from, DataTransformer transformer, Instant now) {
    final PreAdjFissClaim to = transformMessageImpl(from, transformer, now, "");
    transformMessageArrays(from, to, transformer, now, "");
    return to;
  }

  private PreAdjFissClaim transformMessageImpl(
      FissClaim from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjFissClaim to = new PreAdjFissClaim();
    transformer.copyString(
        namePrefix + PreAdjFissClaim.Fields.dcn, false, 1, 23, from.getDcn(), to::setDcn);
    transformer.copyString(
        namePrefix + PreAdjFissClaim.Fields.hicNo, false, 1, 12, from.getHicNo(), to::setHicNo);
    transformer.copyEnumAsCharacter(
        namePrefix + PreAdjFissClaim.Fields.currStatus,
        PreAdjFissClaim_currStatus_Extractor.getEnumString(from),
        to::setCurrStatus);
    transformer.copyEnumAsCharacter(
        namePrefix + PreAdjFissClaim.Fields.currLoc1,
        PreAdjFissClaim_currLoc1_Extractor.getEnumString(from),
        to::setCurrLoc1);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.currLoc2,
        false,
        1,
        5,
        PreAdjFissClaim_currLoc2_Extractor.getEnumString(from),
        to::setCurrLoc2);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.medaProvId,
        1,
        13,
        from::hasMedaProvId,
        from::getMedaProvId,
        to::setMedaProvId);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.medaProv_6,
        1,
        6,
        from::hasMedaProv6,
        from::getMedaProv6,
        to::setMedaProv_6);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjFissClaim.Fields.totalChargeAmount,
        from::hasTotalChargeAmount,
        from::getTotalChargeAmount,
        to::setTotalChargeAmount);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissClaim.Fields.receivedDate,
        from::hasRecdDtCymd,
        from::getRecdDtCymd,
        to::setReceivedDate);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissClaim.Fields.currTranDate,
        from::hasCurrTranDtCymd,
        from::getCurrTranDtCymd,
        to::setCurrTranDate);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.admitDiagCode,
        1,
        7,
        from::hasAdmDiagCode,
        from::getAdmDiagCode,
        to::setAdmitDiagCode);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.principleDiag,
        1,
        7,
        from::hasPrincipleDiag,
        from::getPrincipleDiag,
        to::setPrincipleDiag);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.npiNumber,
        1,
        10,
        from::hasNpiNumber,
        from::getNpiNumber,
        to::setNpiNumber);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.mbi, 1, 13, from::hasMbi, from::getMbi, to::setMbi);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.mbiHash,
        1,
        64,
        from::hasMbi,
        () -> idHasher.computeIdentifierHash(from.getMbi()),
        to::setMbiHash);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.fedTaxNumber,
        1,
        10,
        from::hasFedTaxNb,
        from::getFedTaxNb,
        to::setFedTaxNumber);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.pracLocAddr1,
        1,
        2147483647,
        from::hasPracLocAddr1,
        from::getPracLocAddr1,
        to::setPracLocAddr1);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.pracLocAddr2,
        1,
        2147483647,
        from::hasPracLocAddr2,
        from::getPracLocAddr2,
        to::setPracLocAddr2);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.pracLocCity,
        1,
        2147483647,
        from::hasPracLocCity,
        from::getPracLocCity,
        to::setPracLocCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.pracLocState,
        1,
        2,
        from::hasPracLocState,
        from::getPracLocState,
        to::setPracLocState);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.pracLocZip,
        1,
        15,
        from::hasPracLocZip,
        from::getPracLocZip,
        to::setPracLocZip);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissClaim.Fields.stmtCovFromDate,
        from::hasStmtCovFromCymd,
        from::getStmtCovFromCymd,
        to::setStmtCovFromDate);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissClaim.Fields.stmtCovToDate,
        from::hasStmtCovToCymd,
        from::getStmtCovToCymd,
        to::setStmtCovToDate);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.lobCd,
        true,
        1,
        1,
        PreAdjFissClaim_lobCd_Extractor.getEnumString(from),
        to::setLobCd);
    if (from.hasServTypeCdEnum()) {
      to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Normal);
    }
    if (from.hasServTypeCdForClinicsEnum()) {
      to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Clinic);
    }
    if (from.hasServTypeCdForSpecialFacilitiesEnum()) {
      to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.SpecialFacility);
    }
    if (from.hasServTypCdUnrecognized()) {
      to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Unrecognized);
    }
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        1,
        PreAdjFissClaim_servTypeCd_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        1,
        PreAdjFissClaim_servTypeCdForClinics_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        1,
        PreAdjFissClaim_servTypeCdForSpecialFacilities_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.servTypeCd,
        1,
        1,
        from::hasServTypCdUnrecognized,
        from::getServTypCdUnrecognized,
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissClaim.Fields.freqCd,
        true,
        1,
        1,
        PreAdjFissClaim_freqCd_Extractor.getEnumString(from),
        to::setFreqCd);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissClaim.Fields.billTypCd,
        1,
        3,
        from::hasBillTypCd,
        from::getBillTypCd,
        to::setBillTypCd);
    to.setLastUpdated(now);
    return to;
  }

  private void transformMessageArrays(
      FissClaim from,
      PreAdjFissClaim to,
      DataTransformer transformer,
      Instant now,
      String namePrefix) {
    for (short index = 0; index < from.getFissProcCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "procCode" + "-" + index + "-";
      final FissProcedureCode itemFrom = from.getFissProcCodes(index);
      final PreAdjFissProcCode itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getProcCodes().add(itemTo);
    }
    for (short index = 0; index < from.getFissDiagCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "diagCode" + "-" + index + "-";
      final FissDiagnosisCode itemFrom = from.getFissDiagCodes(index);
      final PreAdjFissDiagnosisCode itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getDiagCodes().add(itemTo);
    }
    for (short index = 0; index < from.getFissPayersCount(); ++index) {
      final String itemNamePrefix = namePrefix + "payer" + "-" + index + "-";
      final FissPayer itemFrom = from.getFissPayers(index);
      final PreAdjFissPayer itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getPayers().add(itemTo);
    }
  }

  private PreAdjFissProcCode transformMessageImpl(
      FissProcedureCode from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjFissProcCode to = new PreAdjFissProcCode();
    transformer.copyString(
        namePrefix + PreAdjFissProcCode.Fields.procCode,
        false,
        1,
        10,
        from.getProcCd(),
        to::setProcCode);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissProcCode.Fields.procFlag,
        1,
        4,
        from::hasProcFlag,
        from::getProcFlag,
        to::setProcFlag);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissProcCode.Fields.procDate,
        from::hasProcDt,
        from::getProcDt,
        to::setProcDate);
    to.setLastUpdated(now);
    return to;
  }

  private PreAdjFissDiagnosisCode transformMessageImpl(
      FissDiagnosisCode from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjFissDiagnosisCode to = new PreAdjFissDiagnosisCode();
    transformer.copyString(
        namePrefix + PreAdjFissDiagnosisCode.Fields.diagCd2,
        false,
        1,
        7,
        from.getDiagCd2(),
        to::setDiagCd2);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissDiagnosisCode.Fields.diagPoaInd,
        false,
        1,
        1,
        PreAdjFissDiagnosisCode_diagPoaInd_Extractor.getEnumString(from),
        to::setDiagPoaInd);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissDiagnosisCode.Fields.bitFlags,
        1,
        4,
        from::hasBitFlags,
        from::getBitFlags,
        to::setBitFlags);
    to.setLastUpdated(now);
    return to;
  }

  private PreAdjFissPayer transformMessageImpl(
      FissPayer from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjFissPayer to = new PreAdjFissPayer();
    if (from.hasBeneZPayer()) {
      to.setPayerType(PreAdjFissPayer.PayerType.BeneZ);
    }
    if (from.hasInsuredPayer()) {
      to.setPayerType(PreAdjFissPayer.PayerType.Insured);
    }
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.payersId,
        true,
        1,
        1,
        PreAdjFissPayer_insuredPayer_payersId_Extractor.getEnumString(from),
        to::setPayersId);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.payersName,
        1,
        32,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasPayersName(),
        () -> from.getInsuredPayer().getPayersName(),
        to::setPayersName);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.relInd,
        true,
        1,
        1,
        PreAdjFissPayer_insuredPayer_relInd_Extractor.getEnumString(from),
        to::setRelInd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.assignInd,
        true,
        1,
        1,
        PreAdjFissPayer_insuredPayer_assignInd_Extractor.getEnumString(from),
        to::setAssignInd);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.providerNumber,
        1,
        13,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasProviderNumber(),
        () -> from.getInsuredPayer().getProviderNumber(),
        to::setProviderNumber);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.adjDcnIcn,
        1,
        23,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasAdjDcnIcn(),
        () -> from.getInsuredPayer().getAdjDcnIcn(),
        to::setAdjDcnIcn);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjFissPayer.Fields.priorPmt,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasPriorPmt(),
        () -> from.getInsuredPayer().getPriorPmt(),
        to::setPriorPmt);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjFissPayer.Fields.estAmtDue,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasEstAmtDue(),
        () -> from.getInsuredPayer().getEstAmtDue(),
        to::setEstAmtDue);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.insuredRel,
        true,
        1,
        2,
        PreAdjFissPayer_insuredPayer_insuredRel_Extractor.getEnumString(from),
        to::setInsuredRel);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredName,
        1,
        25,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredName(),
        () -> from.getInsuredPayer().getInsuredName(),
        to::setInsuredName);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredSsnHic,
        1,
        19,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredSsnHic(),
        () -> from.getInsuredPayer().getInsuredSsnHic(),
        to::setInsuredSsnHic);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredGroupName,
        1,
        17,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredGroupName(),
        () -> from.getInsuredPayer().getInsuredGroupName(),
        to::setInsuredGroupName);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredGroupNbr,
        1,
        20,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredGroupNbr(),
        () -> from.getInsuredPayer().getInsuredGroupNbr(),
        to::setInsuredGroupNbr);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.treatAuthCd,
        1,
        18,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasTreatAuthCd(),
        () -> from.getInsuredPayer().getTreatAuthCd(),
        to::setTreatAuthCd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.insuredSex,
        true,
        1,
        1,
        PreAdjFissPayer_insuredPayer_insuredSex_Extractor.getEnumString(from),
        to::setInsuredSex);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.insuredRelX12,
        true,
        1,
        2,
        PreAdjFissPayer_insuredPayer_insuredRelX12_Extractor.getEnumString(from),
        to::setInsuredRelX12);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissPayer.Fields.insuredDob,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDob(),
        () -> from.getInsuredPayer().getInsuredDob(),
        to::setInsuredDob);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredDobText,
        1,
        9,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDobText(),
        () -> from.getInsuredPayer().getInsuredDobText(),
        to::setInsuredDobText);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.payersId,
        true,
        1,
        1,
        PreAdjFissPayer_beneZPayer_payersId_Extractor.getEnumString(from),
        to::setPayersId);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.payersName,
        1,
        32,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasPayersName(),
        () -> from.getBeneZPayer().getPayersName(),
        to::setPayersName);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.relInd,
        true,
        1,
        1,
        PreAdjFissPayer_beneZPayer_relInd_Extractor.getEnumString(from),
        to::setRelInd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.assignInd,
        true,
        1,
        1,
        PreAdjFissPayer_beneZPayer_assignInd_Extractor.getEnumString(from),
        to::setAssignInd);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.providerNumber,
        1,
        13,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasProviderNumber(),
        () -> from.getBeneZPayer().getProviderNumber(),
        to::setProviderNumber);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.adjDcnIcn,
        1,
        23,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasAdjDcnIcn(),
        () -> from.getBeneZPayer().getAdjDcnIcn(),
        to::setAdjDcnIcn);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjFissPayer.Fields.priorPmt,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasPriorPmt(),
        () -> from.getBeneZPayer().getPriorPmt(),
        to::setPriorPmt);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjFissPayer.Fields.estAmtDue,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasEstAmtDue(),
        () -> from.getBeneZPayer().getEstAmtDue(),
        to::setEstAmtDue);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.beneRel,
        true,
        1,
        2,
        PreAdjFissPayer_beneZPayer_beneRel_Extractor.getEnumString(from),
        to::setBeneRel);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.beneLastName,
        1,
        15,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneLastName(),
        () -> from.getBeneZPayer().getBeneLastName(),
        to::setBeneLastName);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.beneFirstName,
        1,
        10,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneFirstName(),
        () -> from.getBeneZPayer().getBeneFirstName(),
        to::setBeneFirstName);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.beneMidInit,
        1,
        1,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneMidInit(),
        () -> from.getBeneZPayer().getBeneMidInit(),
        to::setBeneMidInit);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.beneSsnHic,
        1,
        19,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneSsnHic(),
        () -> from.getBeneZPayer().getBeneSsnHic(),
        to::setBeneSsnHic);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.insuredGroupName,
        1,
        17,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasInsuredGroupName(),
        () -> from.getBeneZPayer().getInsuredGroupName(),
        to::setInsuredGroupName);
    transformer.copyOptionalDate(
        namePrefix + PreAdjFissPayer.Fields.beneDob,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneDob(),
        () -> from.getBeneZPayer().getBeneDob(),
        to::setBeneDob);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.beneSex,
        true,
        1,
        1,
        PreAdjFissPayer_beneZPayer_beneSex_Extractor.getEnumString(from),
        to::setBeneSex);
    transformer.copyOptionalString(
        namePrefix + PreAdjFissPayer.Fields.treatAuthCd,
        1,
        18,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasTreatAuthCd(),
        () -> from.getBeneZPayer().getTreatAuthCd(),
        to::setTreatAuthCd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.insuredSex,
        true,
        1,
        1,
        PreAdjFissPayer_beneZPayer_insuredSex_Extractor.getEnumString(from),
        to::setInsuredSex);
    transformer.copyEnumAsString(
        namePrefix + PreAdjFissPayer.Fields.insuredRelX12,
        true,
        1,
        2,
        PreAdjFissPayer_beneZPayer_insuredRelX12_Extractor.getEnumString(from),
        to::setInsuredRelX12);
    to.setLastUpdated(now);
    return to;
  }
}
