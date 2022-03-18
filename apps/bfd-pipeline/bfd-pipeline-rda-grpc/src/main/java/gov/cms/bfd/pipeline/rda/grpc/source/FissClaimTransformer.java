package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.model.rda.PartAdjFissAuditTrail;
import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.bfd.model.rda.PartAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PartAdjFissPayer;
import gov.cms.bfd.model.rda.PartAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentMedicareBeneficiaryIdentifierIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentRequestorCode;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAuditTrail;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForSpecialFacilities;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissCancelAdjustmentCode;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissPhysicianFlag;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessNewHealthInsuranceClaimNumberIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
import gov.cms.mpsm.rda.v1.fiss.FissRepositoryIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissSourceOfAdmission;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.Getter;

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

  private final EnumStringExtractor<FissClaim, FissAdjustmentRequestorCode>
      PreAdjFissClaim_adjReqCd_Extractor;

  private final EnumStringExtractor<FissClaim, FissCancelAdjustmentCode>
      PreAdjFissClaim_cancAdjCd_Extractor;

  private final EnumStringExtractor<FissClaim, FissSourceOfAdmission>
      PreAdjFissClaim_admSource_Extractor;

  private final EnumStringExtractor<FissClaim, FissPayersCode>
      PreAdjFissClaim_primaryPayerCode_Extractor;

  private final EnumStringExtractor<FissClaim, FissPhysicianFlag>
      PreAdjFissClaim_attendPhysFlag_Extractor;

  private final EnumStringExtractor<FissClaim, FissPhysicianFlag>
      PreAdjFissClaim_operPhysFlag_Extractor;

  private final EnumStringExtractor<FissClaim, FissPhysicianFlag>
      PreAdjFissClaim_othPhysFlag_Extractor;

  private final EnumStringExtractor<FissClaim, FissProcessNewHealthInsuranceClaimNumberIndicator>
      PreAdjFissClaim_procNewHicInd_Extractor;

  private final EnumStringExtractor<FissClaim, FissRepositoryIndicator>
      PreAdjFissClaim_reposInd_Extractor;

  private final EnumStringExtractor<
          FissClaim, FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier>
      PreAdjFissClaim_mbiSubmBeneInd_Extractor;

  private final EnumStringExtractor<FissClaim, FissAdjustmentMedicareBeneficiaryIdentifierIndicator>
      PreAdjFissClaim_adjMbiInd_Extractor;

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

  private final EnumStringExtractor<FissAuditTrail, FissClaimStatus>
      PreAdjFissAuditTrail_badtStatus_Extractor;

  private final Clock clock;
  @Getter private final MbiCache mbiCache;

  public FissClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
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
    PreAdjFissClaim_adjReqCd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasAdjReqCdEnum,
            FissClaim::getAdjReqCdEnum,
            FissClaim::hasAdjReqCdUnrecognized,
            FissClaim::getAdjReqCdUnrecognized,
            FissAdjustmentRequestorCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_cancAdjCd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasCancAdjCdEnum,
            FissClaim::getCancAdjCdEnum,
            FissClaim::hasCancAdjCdUnrecognized,
            FissClaim::getCancAdjCdUnrecognized,
            FissCancelAdjustmentCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_admSource_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasAdmSourceEnum,
            FissClaim::getAdmSourceEnum,
            FissClaim::hasAdmSourceUnrecognized,
            FissClaim::getAdmSourceUnrecognized,
            FissSourceOfAdmission.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_primaryPayerCode_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasPrimaryPayerCodeEnum,
            FissClaim::getPrimaryPayerCodeEnum,
            FissClaim::hasPrimaryPayerCodeUnrecognized,
            FissClaim::getPrimaryPayerCodeUnrecognized,
            FissPayersCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_attendPhysFlag_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasAttendPhysFlagEnum,
            FissClaim::getAttendPhysFlagEnum,
            FissClaim::hasAttendPhysFlagUnrecognized,
            FissClaim::getAttendPhysFlagUnrecognized,
            FissPhysicianFlag.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_operPhysFlag_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasOperPhysFlagEnum,
            FissClaim::getOperPhysFlagEnum,
            FissClaim::hasOperPhysFlagUnrecognized,
            FissClaim::getOperPhysFlagUnrecognized,
            FissPhysicianFlag.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_othPhysFlag_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasOthPhysFlagEnum,
            FissClaim::getOthPhysFlagEnum,
            FissClaim::hasOthPhysFlagUnrecognized,
            FissClaim::getOthPhysFlagUnrecognized,
            FissPhysicianFlag.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_procNewHicInd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasProcNewHicIndEnum,
            FissClaim::getProcNewHicIndEnum,
            FissClaim::hasProcNewHicIndUnrecognized,
            FissClaim::getProcNewHicIndUnrecognized,
            FissProcessNewHealthInsuranceClaimNumberIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_reposInd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasReposIndEnum,
            FissClaim::getReposIndEnum,
            FissClaim::hasReposIndUnrecognized,
            FissClaim::getReposIndUnrecognized,
            FissRepositoryIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_mbiSubmBeneInd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasMbiSubmBeneIndEnum,
            FissClaim::getMbiSubmBeneIndEnum,
            FissClaim::hasMbiSubmBeneIndUnrecognized,
            FissClaim::getMbiSubmBeneIndUnrecognized,
            FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjFissClaim_adjMbiInd_Extractor =
        new EnumStringExtractor<>(
            FissClaim::hasAdjMbiIndEnum,
            FissClaim::getAdjMbiIndEnum,
            FissClaim::hasAdjMbiIndUnrecognized,
            FissClaim::getAdjMbiIndUnrecognized,
            FissAdjustmentMedicareBeneficiaryIdentifierIndicator.UNRECOGNIZED,
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
    PreAdjFissAuditTrail_badtStatus_Extractor =
        new EnumStringExtractor<>(
            FissAuditTrail::hasBadtStatusEnum,
            FissAuditTrail::getBadtStatusEnum,
            FissAuditTrail::hasBadtStatusUnrecognized,
            FissAuditTrail::getBadtStatusUnrecognized,
            FissClaimStatus.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
  }

  /**
   * Hook to allow the FissClaimRdaSink to install an alternative MbiCache implementation that
   * supports caching MBI values in a database table.
   *
   * @param mbiCache alternative MbiCache to use for obtaining Mbi instances
   * @return a new transformer with the same clock but alternative MbiCache
   */
  public FissClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new FissClaimTransformer(clock, mbiCache);
  }

  public RdaChange<PartAdjFissClaim> transformClaim(FissClaimChange change) {
    FissClaim from = change.getClaim();
    final DataTransformer transformer = new DataTransformer();
    final PartAdjFissClaim to = transformMessage(from, transformer, clock.instant());
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

  private PartAdjFissClaim transformMessage(
      FissClaim from, DataTransformer transformer, Instant now) {
    final PartAdjFissClaim to = transformMessageImpl(from, transformer, now, "");
    transformMessageArrays(from, to, transformer, now, "");
    return to;
  }

  private PartAdjFissClaim transformMessageImpl(
      FissClaim from, DataTransformer transformer, Instant now, String namePrefix) {
    final PartAdjFissClaim to = new PartAdjFissClaim();
    transformer.copyString(
        namePrefix + PartAdjFissClaim.Fields.dcn, false, 1, 23, from.getDcn(), to::setDcn);
    transformer.copyString(
        namePrefix + PartAdjFissClaim.Fields.hicNo, false, 1, 12, from.getHicNo(), to::setHicNo);
    transformer.copyEnumAsCharacter(
        namePrefix + PartAdjFissClaim.Fields.currStatus,
        PreAdjFissClaim_currStatus_Extractor.getEnumString(from),
        to::setCurrStatus);
    transformer.copyEnumAsCharacter(
        namePrefix + PartAdjFissClaim.Fields.currLoc1,
        PreAdjFissClaim_currLoc1_Extractor.getEnumString(from),
        to::setCurrLoc1);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.currLoc2,
        false,
        5,
        PreAdjFissClaim_currLoc2_Extractor.getEnumString(from),
        to::setCurrLoc2);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.medaProvId,
        1,
        13,
        from::hasMedaProvId,
        from::getMedaProvId,
        to::setMedaProvId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.medaProv_6,
        1,
        6,
        from::hasMedaProv6,
        from::getMedaProv6,
        to::setMedaProv_6);
    transformer.copyOptionalAmount(
        namePrefix + PartAdjFissClaim.Fields.totalChargeAmount,
        from::hasTotalChargeAmount,
        from::getTotalChargeAmount,
        to::setTotalChargeAmount);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.receivedDate,
        from::hasRecdDtCymd,
        from::getRecdDtCymd,
        to::setReceivedDate);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.currTranDate,
        from::hasCurrTranDtCymd,
        from::getCurrTranDtCymd,
        to::setCurrTranDate);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.admitDiagCode,
        1,
        7,
        from::hasAdmDiagCode,
        from::getAdmDiagCode,
        to::setAdmitDiagCode);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.principleDiag,
        1,
        7,
        from::hasPrincipleDiag,
        from::getPrincipleDiag,
        to::setPrincipleDiag);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.npiNumber,
        1,
        10,
        from::hasNpiNumber,
        from::getNpiNumber,
        to::setNpiNumber);
    if (from.hasMbi()) {
      final var mbi = from.getMbi();
      if (transformer.validateString(namePrefix + PartAdjFissClaim.Fields.mbi, false, 1, 11, mbi)) {
        to.setMbiRecord(mbiCache.lookupMbi(mbi));
      }
    }
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.fedTaxNumber,
        1,
        10,
        from::hasFedTaxNb,
        from::getFedTaxNb,
        to::setFedTaxNumber);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.pracLocAddr1,
        1,
        2147483647,
        from::hasPracLocAddr1,
        from::getPracLocAddr1,
        to::setPracLocAddr1);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.pracLocAddr2,
        1,
        2147483647,
        from::hasPracLocAddr2,
        from::getPracLocAddr2,
        to::setPracLocAddr2);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.pracLocCity,
        1,
        2147483647,
        from::hasPracLocCity,
        from::getPracLocCity,
        to::setPracLocCity);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.pracLocState,
        1,
        2,
        from::hasPracLocState,
        from::getPracLocState,
        to::setPracLocState);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.pracLocZip,
        1,
        15,
        from::hasPracLocZip,
        from::getPracLocZip,
        to::setPracLocZip);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.stmtCovFromDate,
        from::hasStmtCovFromCymd,
        from::getStmtCovFromCymd,
        to::setStmtCovFromDate);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.stmtCovToDate,
        from::hasStmtCovToCymd,
        from::getStmtCovToCymd,
        to::setStmtCovToDate);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.lobCd,
        true,
        1,
        PreAdjFissClaim_lobCd_Extractor.getEnumString(from),
        to::setLobCd);
    if (from.hasServTypeCdEnum()) {
      to.setServTypeCdMapping(PartAdjFissClaim.ServTypeCdMapping.Normal);
    }
    if (from.hasServTypeCdForClinicsEnum()) {
      to.setServTypeCdMapping(PartAdjFissClaim.ServTypeCdMapping.Clinic);
    }
    if (from.hasServTypeCdForSpecialFacilitiesEnum()) {
      to.setServTypeCdMapping(PartAdjFissClaim.ServTypeCdMapping.SpecialFacility);
    }
    if (from.hasServTypCdUnrecognized()) {
      to.setServTypeCdMapping(PartAdjFissClaim.ServTypeCdMapping.Unrecognized);
    }
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        PreAdjFissClaim_servTypeCd_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        PreAdjFissClaim_servTypeCdForClinics_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.servTypeCd,
        true,
        1,
        PreAdjFissClaim_servTypeCdForSpecialFacilities_Extractor.getEnumString(from),
        to::setServTypeCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.servTypeCd,
        1,
        1,
        from::hasServTypCdUnrecognized,
        from::getServTypCdUnrecognized,
        to::setServTypeCd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.freqCd,
        true,
        1,
        PreAdjFissClaim_freqCd_Extractor.getEnumString(from),
        to::setFreqCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.billTypCd,
        1,
        3,
        from::hasBillTypCd,
        from::getBillTypCd,
        to::setBillTypCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.rejectCd,
        1,
        5,
        from::hasRejectCd,
        from::getRejectCd,
        to::setRejectCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.fullPartDenInd,
        1,
        1,
        from::hasFullPartDenInd,
        from::getFullPartDenInd,
        to::setFullPartDenInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.nonPayInd,
        1,
        2,
        from::hasNonPayInd,
        from::getNonPayInd,
        to::setNonPayInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.xrefDcnNbr,
        1,
        23,
        from::hasXrefDcnNbr,
        from::getXrefDcnNbr,
        to::setXrefDcnNbr);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.adjReqCd,
        true,
        1,
        PreAdjFissClaim_adjReqCd_Extractor.getEnumString(from),
        to::setAdjReqCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.adjReasCd,
        1,
        2,
        from::hasAdjReasCd,
        from::getAdjReasCd,
        to::setAdjReasCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.cancelXrefDcn,
        1,
        23,
        from::hasCancelXrefDcn,
        from::getCancelXrefDcn,
        to::setCancelXrefDcn);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.cancelDate,
        from::hasCancelDateCymd,
        from::getCancelDateCymd,
        to::setCancelDate);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.cancAdjCd,
        true,
        1,
        PreAdjFissClaim_cancAdjCd_Extractor.getEnumString(from),
        to::setCancAdjCd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.originalXrefDcn,
        1,
        23,
        from::hasOriginalXrefDcn,
        from::getOriginalXrefDcn,
        to::setOriginalXrefDcn);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.paidDt,
        from::hasPaidDtCymd,
        from::getPaidDtCymd,
        to::setPaidDt);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissClaim.Fields.admDate,
        from::hasAdmDateCymd,
        from::getAdmDateCymd,
        to::setAdmDate);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.admSource,
        true,
        1,
        PreAdjFissClaim_admSource_Extractor.getEnumString(from),
        to::setAdmSource);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.primaryPayerCode,
        true,
        1,
        PreAdjFissClaim_primaryPayerCode_Extractor.getEnumString(from),
        to::setPrimaryPayerCode);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.attendPhysId,
        1,
        16,
        from::hasAttendPhysId,
        from::getAttendPhysId,
        to::setAttendPhysId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.attendPhysLname,
        1,
        17,
        from::hasAttendPhysLname,
        from::getAttendPhysLname,
        to::setAttendPhysLname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.attendPhysFname,
        1,
        18,
        from::hasAttendPhysFname,
        from::getAttendPhysFname,
        to::setAttendPhysFname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.attendPhysMint,
        1,
        1,
        from::hasAttendPhysMint,
        from::getAttendPhysMint,
        to::setAttendPhysMint);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.attendPhysFlag,
        true,
        1,
        PreAdjFissClaim_attendPhysFlag_Extractor.getEnumString(from),
        to::setAttendPhysFlag);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.operatingPhysId,
        1,
        16,
        from::hasOperatingPhysId,
        from::getOperatingPhysId,
        to::setOperatingPhysId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.operPhysLname,
        1,
        17,
        from::hasOperPhysLname,
        from::getOperPhysLname,
        to::setOperPhysLname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.operPhysFname,
        1,
        18,
        from::hasOperPhysFname,
        from::getOperPhysFname,
        to::setOperPhysFname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.operPhysMint,
        1,
        1,
        from::hasOperPhysMint,
        from::getOperPhysMint,
        to::setOperPhysMint);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.operPhysFlag,
        true,
        1,
        PreAdjFissClaim_operPhysFlag_Extractor.getEnumString(from),
        to::setOperPhysFlag);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.othPhysId,
        1,
        16,
        from::hasOthPhysId,
        from::getOthPhysId,
        to::setOthPhysId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.othPhysLname,
        1,
        17,
        from::hasOthPhysLname,
        from::getOthPhysLname,
        to::setOthPhysLname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.othPhysFname,
        1,
        18,
        from::hasOthPhysFname,
        from::getOthPhysFname,
        to::setOthPhysFname);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.othPhysMint,
        1,
        1,
        from::hasOthPhysMint,
        from::getOthPhysMint,
        to::setOthPhysMint);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.othPhysFlag,
        true,
        1,
        PreAdjFissClaim_othPhysFlag_Extractor.getEnumString(from),
        to::setOthPhysFlag);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.xrefHicNbr,
        1,
        12,
        from::hasXrefHicNbr,
        from::getXrefHicNbr,
        to::setXrefHicNbr);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.procNewHicInd,
        true,
        1,
        PreAdjFissClaim_procNewHicInd_Extractor.getEnumString(from),
        to::setProcNewHicInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.newHic,
        1,
        12,
        from::hasNewHic,
        from::getNewHic,
        to::setNewHic);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.reposInd,
        true,
        1,
        PreAdjFissClaim_reposInd_Extractor.getEnumString(from),
        to::setReposInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.reposHic,
        1,
        12,
        from::hasReposHic,
        from::getReposHic,
        to::setReposHic);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.mbiSubmBeneInd,
        true,
        1,
        PreAdjFissClaim_mbiSubmBeneInd_Extractor.getEnumString(from),
        to::setMbiSubmBeneInd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissClaim.Fields.adjMbiInd,
        true,
        1,
        PreAdjFissClaim_adjMbiInd_Extractor.getEnumString(from),
        to::setAdjMbiInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.adjMbi,
        1,
        11,
        from::hasAdjMbi,
        from::getAdjMbi,
        to::setAdjMbi);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissClaim.Fields.medicalRecordNo,
        1,
        17,
        from::hasMedicalRecordNo,
        from::getMedicalRecordNo,
        to::setMedicalRecordNo);
    to.setLastUpdated(now);
    return to;
  }

  private void transformMessageArrays(
      FissClaim from,
      PartAdjFissClaim to,
      DataTransformer transformer,
      Instant now,
      String namePrefix) {
    for (short index = 0; index < from.getFissProcCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "procCode" + "-" + index + "-";
      final FissProcedureCode itemFrom = from.getFissProcCodes(index);
      final PartAdjFissProcCode itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getProcCodes().add(itemTo);
    }
    for (short index = 0; index < from.getFissDiagCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "diagCode" + "-" + index + "-";
      final FissDiagnosisCode itemFrom = from.getFissDiagCodes(index);
      final PartAdjFissDiagnosisCode itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getDiagCodes().add(itemTo);
    }
    for (short index = 0; index < from.getFissPayersCount(); ++index) {
      final String itemNamePrefix = namePrefix + "payer" + "-" + index + "-";
      final FissPayer itemFrom = from.getFissPayers(index);
      final PartAdjFissPayer itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getPayers().add(itemTo);
    }
    for (short index = 0; index < from.getFissAuditTrailCount(); ++index) {
      final String itemNamePrefix = namePrefix + "auditTrail" + "-" + index + "-";
      final FissAuditTrail itemFrom = from.getFissAuditTrail(index);
      final PartAdjFissAuditTrail itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getAuditTrail().add(itemTo);
    }
  }

  private PartAdjFissProcCode transformMessageImpl(
      FissProcedureCode from, DataTransformer transformer, Instant now, String namePrefix) {
    final PartAdjFissProcCode to = new PartAdjFissProcCode();
    transformer.copyString(
        namePrefix + PartAdjFissProcCode.Fields.procCode,
        false,
        1,
        10,
        from.getProcCd(),
        to::setProcCode);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissProcCode.Fields.procFlag,
        1,
        4,
        from::hasProcFlag,
        from::getProcFlag,
        to::setProcFlag);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissProcCode.Fields.procDate,
        from::hasProcDt,
        from::getProcDt,
        to::setProcDate);
    to.setLastUpdated(now);
    return to;
  }

  private PartAdjFissDiagnosisCode transformMessageImpl(
      FissDiagnosisCode from, DataTransformer transformer, Instant now, String namePrefix) {
    final PartAdjFissDiagnosisCode to = new PartAdjFissDiagnosisCode();
    transformer.copyString(
        namePrefix + PartAdjFissDiagnosisCode.Fields.diagCd2,
        false,
        1,
        7,
        from.getDiagCd2(),
        to::setDiagCd2);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissDiagnosisCode.Fields.diagPoaInd,
        true,
        1,
        PreAdjFissDiagnosisCode_diagPoaInd_Extractor.getEnumString(from),
        to::setDiagPoaInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissDiagnosisCode.Fields.bitFlags,
        1,
        4,
        from::hasBitFlags,
        from::getBitFlags,
        to::setBitFlags);
    to.setLastUpdated(now);
    return to;
  }

  private PartAdjFissPayer transformMessageImpl(
      FissPayer from, DataTransformer transformer, Instant now, String namePrefix) {
    final PartAdjFissPayer to = new PartAdjFissPayer();
    if (from.hasBeneZPayer()) {
      to.setPayerType(PartAdjFissPayer.PayerType.BeneZ);
    }
    if (from.hasInsuredPayer()) {
      to.setPayerType(PartAdjFissPayer.PayerType.Insured);
    }
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.payersId,
        true,
        1,
        PreAdjFissPayer_insuredPayer_payersId_Extractor.getEnumString(from),
        to::setPayersId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.payersName,
        1,
        32,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasPayersName(),
        () -> from.getInsuredPayer().getPayersName(),
        to::setPayersName);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.relInd,
        true,
        1,
        PreAdjFissPayer_insuredPayer_relInd_Extractor.getEnumString(from),
        to::setRelInd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.assignInd,
        true,
        1,
        PreAdjFissPayer_insuredPayer_assignInd_Extractor.getEnumString(from),
        to::setAssignInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.providerNumber,
        1,
        13,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasProviderNumber(),
        () -> from.getInsuredPayer().getProviderNumber(),
        to::setProviderNumber);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.adjDcnIcn,
        1,
        23,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasAdjDcnIcn(),
        () -> from.getInsuredPayer().getAdjDcnIcn(),
        to::setAdjDcnIcn);
    transformer.copyOptionalAmount(
        namePrefix + PartAdjFissPayer.Fields.priorPmt,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasPriorPmt(),
        () -> from.getInsuredPayer().getPriorPmt(),
        to::setPriorPmt);
    transformer.copyOptionalAmount(
        namePrefix + PartAdjFissPayer.Fields.estAmtDue,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasEstAmtDue(),
        () -> from.getInsuredPayer().getEstAmtDue(),
        to::setEstAmtDue);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.insuredRel,
        true,
        2,
        PreAdjFissPayer_insuredPayer_insuredRel_Extractor.getEnumString(from),
        to::setInsuredRel);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredName,
        1,
        25,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredName(),
        () -> from.getInsuredPayer().getInsuredName(),
        to::setInsuredName);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredSsnHic,
        1,
        19,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredSsnHic(),
        () -> from.getInsuredPayer().getInsuredSsnHic(),
        to::setInsuredSsnHic);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredGroupName,
        1,
        17,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredGroupName(),
        () -> from.getInsuredPayer().getInsuredGroupName(),
        to::setInsuredGroupName);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredGroupNbr,
        1,
        20,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredGroupNbr(),
        () -> from.getInsuredPayer().getInsuredGroupNbr(),
        to::setInsuredGroupNbr);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.treatAuthCd,
        1,
        18,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasTreatAuthCd(),
        () -> from.getInsuredPayer().getTreatAuthCd(),
        to::setTreatAuthCd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.insuredSex,
        true,
        1,
        PreAdjFissPayer_insuredPayer_insuredSex_Extractor.getEnumString(from),
        to::setInsuredSex);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.insuredRelX12,
        true,
        2,
        PreAdjFissPayer_insuredPayer_insuredRelX12_Extractor.getEnumString(from),
        to::setInsuredRelX12);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissPayer.Fields.insuredDob,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDob(),
        () -> from.getInsuredPayer().getInsuredDob(),
        to::setInsuredDob);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredDobText,
        1,
        9,
        () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDobText(),
        () -> from.getInsuredPayer().getInsuredDobText(),
        to::setInsuredDobText);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.payersId,
        true,
        1,
        PreAdjFissPayer_beneZPayer_payersId_Extractor.getEnumString(from),
        to::setPayersId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.payersName,
        1,
        32,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasPayersName(),
        () -> from.getBeneZPayer().getPayersName(),
        to::setPayersName);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.relInd,
        true,
        1,
        PreAdjFissPayer_beneZPayer_relInd_Extractor.getEnumString(from),
        to::setRelInd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.assignInd,
        true,
        1,
        PreAdjFissPayer_beneZPayer_assignInd_Extractor.getEnumString(from),
        to::setAssignInd);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.providerNumber,
        1,
        13,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasProviderNumber(),
        () -> from.getBeneZPayer().getProviderNumber(),
        to::setProviderNumber);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.adjDcnIcn,
        1,
        23,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasAdjDcnIcn(),
        () -> from.getBeneZPayer().getAdjDcnIcn(),
        to::setAdjDcnIcn);
    transformer.copyOptionalAmount(
        namePrefix + PartAdjFissPayer.Fields.priorPmt,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasPriorPmt(),
        () -> from.getBeneZPayer().getPriorPmt(),
        to::setPriorPmt);
    transformer.copyOptionalAmount(
        namePrefix + PartAdjFissPayer.Fields.estAmtDue,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasEstAmtDue(),
        () -> from.getBeneZPayer().getEstAmtDue(),
        to::setEstAmtDue);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.beneRel,
        true,
        2,
        PreAdjFissPayer_beneZPayer_beneRel_Extractor.getEnumString(from),
        to::setBeneRel);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.beneLastName,
        1,
        15,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneLastName(),
        () -> from.getBeneZPayer().getBeneLastName(),
        to::setBeneLastName);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.beneFirstName,
        1,
        10,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneFirstName(),
        () -> from.getBeneZPayer().getBeneFirstName(),
        to::setBeneFirstName);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.beneMidInit,
        1,
        1,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneMidInit(),
        () -> from.getBeneZPayer().getBeneMidInit(),
        to::setBeneMidInit);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.beneSsnHic,
        1,
        19,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneSsnHic(),
        () -> from.getBeneZPayer().getBeneSsnHic(),
        to::setBeneSsnHic);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.insuredGroupName,
        1,
        17,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasInsuredGroupName(),
        () -> from.getBeneZPayer().getInsuredGroupName(),
        to::setInsuredGroupName);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissPayer.Fields.beneDob,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasBeneDob(),
        () -> from.getBeneZPayer().getBeneDob(),
        to::setBeneDob);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.beneSex,
        true,
        1,
        PreAdjFissPayer_beneZPayer_beneSex_Extractor.getEnumString(from),
        to::setBeneSex);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissPayer.Fields.treatAuthCd,
        1,
        18,
        () -> from.hasBeneZPayer() && from.getBeneZPayer().hasTreatAuthCd(),
        () -> from.getBeneZPayer().getTreatAuthCd(),
        to::setTreatAuthCd);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.insuredSex,
        true,
        1,
        PreAdjFissPayer_beneZPayer_insuredSex_Extractor.getEnumString(from),
        to::setInsuredSex);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissPayer.Fields.insuredRelX12,
        true,
        2,
        PreAdjFissPayer_beneZPayer_insuredRelX12_Extractor.getEnumString(from),
        to::setInsuredRelX12);
    to.setLastUpdated(now);
    return to;
  }

  private PartAdjFissAuditTrail transformMessageImpl(
      FissAuditTrail from, DataTransformer transformer, Instant now, String namePrefix) {
    final PartAdjFissAuditTrail to = new PartAdjFissAuditTrail();
    to.setLastUpdated(now);
    transformer.copyEnumAsString(
        namePrefix + PartAdjFissAuditTrail.Fields.badtStatus,
        true,
        1,
        PreAdjFissAuditTrail_badtStatus_Extractor.getEnumString(from),
        to::setBadtStatus);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissAuditTrail.Fields.badtLoc,
        1,
        5,
        from::hasBadtLoc,
        from::getBadtLoc,
        to::setBadtLoc);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissAuditTrail.Fields.badtOperId,
        1,
        9,
        from::hasBadtOperId,
        from::getBadtOperId,
        to::setBadtOperId);
    transformer.copyOptionalString(
        namePrefix + PartAdjFissAuditTrail.Fields.badtReas,
        1,
        5,
        from::hasBadtReas,
        from::getBadtReas,
        to::setBadtReas);
    transformer.copyOptionalDate(
        namePrefix + PartAdjFissAuditTrail.Fields.badtCurrDate,
        from::hasBadtCurrDateCymd,
        from::getBadtCurrDateCymd,
        to::setBadtCurrDate);
    return to;
  }
}
