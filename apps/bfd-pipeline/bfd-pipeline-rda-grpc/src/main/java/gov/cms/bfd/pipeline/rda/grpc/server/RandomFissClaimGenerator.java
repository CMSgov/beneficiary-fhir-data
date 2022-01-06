package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentMedicareBeneficiaryIdentifierIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAdjustmentRequestorCode;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissAuditTrail;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
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
import gov.cms.mpsm.rda.v1.fiss.FissInsuredPayer;
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
import java.util.List;

/**
 * Objects of this class create populated FissClaim objects using random data. The purpose is simply
 * to rapidly produce objects for pipeline testing to try out different scenarios for
 * transformation. The purpose is NOT to produce realistic/valid data. The random number seed is
 * settable in the constructor to allow for for predictable unit tests. Every optional field has a
 * 50% chance of being present in each claim. Arrays have randomly assigned variable length
 * (including zero).
 */
public class RandomFissClaimGenerator extends AbstractRandomClaimGenerator {
  private static final int MAX_PROC_CODES = 7;
  private static final int MAX_DIAG_CODES = 7;
  private static final int MAX_PAYERS = 5;
  private static final int MAX_AUDITS = 20;
  private static final List<FissClaimStatus> FissClaimStatusEnums =
      enumValues(FissClaimStatus.values());
  private static final List<FissProcessingType> FissProcessingTypeEnums =
      enumValues(FissProcessingType.values());
  private static final List<FissCurrentLocation2> FissCurrentLocation2Enums =
      enumValues(FissCurrentLocation2.values());
  private static final List<FissDiagnosisPresentOnAdmissionIndicator>
      FissDiagnosisPresentOnAdmissionIndicatorEnums =
          enumValues(FissDiagnosisPresentOnAdmissionIndicator.values());
  private static final List<FissBillFacilityType> FissBillFacilityTypeEnums =
      enumValues(FissBillFacilityType.values());
  private static final List<FissBillClassification> FissBillClassificationEnums =
      enumValues(FissBillClassification.values());
  private static final List<FissBillClassificationForClinics>
      FissBillClassificationForClinicsEnums = enumValues(FissBillClassificationForClinics.values());
  private static final List<FissBillClassificationForSpecialFacilities>
      FissBillClassificationForSpecialFacilitiesEnums =
          enumValues(FissBillClassificationForSpecialFacilities.values());
  private static final List<FissBillFrequency> FissBillFrequencyEnums =
      enumValues(FissBillFrequency.values());
  private static final List<FissPayersCode> FissPayersCodeEnums =
      enumValues(FissPayersCode.values());
  private static final List<FissReleaseOfInformation> FissReleaseOfInformationEnums =
      enumValues(FissReleaseOfInformation.values());
  private static final List<FissAssignmentOfBenefitsIndicator>
      FissAssignmentOfBenefitsIndicatorEnums =
          enumValues(FissAssignmentOfBenefitsIndicator.values());
  private static final List<FissPatientRelationshipCode> FissPatientRelationshipCodeEnums =
      enumValues(FissPatientRelationshipCode.values());
  private static final List<FissBeneficiarySex> FissBeneficiarySexEnums =
      enumValues(FissBeneficiarySex.values());
  private static final List<FissAdjustmentRequestorCode> FissAdjustmentRequestorCodeEnums =
      enumValues(FissAdjustmentRequestorCode.values());
  private static final List<FissCancelAdjustmentCode> FissCancelAdjustmentCodeEnums =
      enumValues(FissCancelAdjustmentCode.values());
  private static final List<FissSourceOfAdmission> FissSourceOfAdmissionEnums =
      enumValues(FissSourceOfAdmission.values());
  private static final List<FissPhysicianFlag> FissPhysicianFlagEnums =
      enumValues(FissPhysicianFlag.values());
  private static final List<FissProcessNewHealthInsuranceClaimNumberIndicator>
      FissProcessNewHealthInsuranceClaimNumberIndicatorEnums =
          enumValues(FissProcessNewHealthInsuranceClaimNumberIndicator.values());
  private static final List<FissRepositoryIndicator> FissRepositoryIndicatorEnums =
      enumValues(FissRepositoryIndicator.values());
  private static final List<FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier>
      FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifierEnums =
          enumValues(FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifier.values());
  private static final List<FissAdjustmentMedicareBeneficiaryIdentifierIndicator>
      FissAdjustmentMedicareBeneficiaryIdentifierIndicatorEnums =
          enumValues(FissAdjustmentMedicareBeneficiaryIdentifierIndicator.values());

  /**
   * Creates an instance with the specified seed.
   *
   * @param seed seed for the PRNG
   */
  public RandomFissClaimGenerator(long seed) {
    super(seed, false, Clock.systemUTC());
  }

  /**
   * Creates an instance for use in unit tests. Setting optionalTrue to true causes all optional
   * fields to be added to the claim. This is useful in some tests.
   *
   * @param seed seed for the PRNG
   * @param optionalTrue true if all optional fields should be populated
   */
  @VisibleForTesting
  RandomFissClaimGenerator(long seed, boolean optionalTrue, Clock clock) {
    super(seed, optionalTrue, clock);
  }

  public FissClaim randomClaim() {
    FissClaim.Builder claim = FissClaim.newBuilder();
    addRandomFieldValues(claim);
    addRandomProcCodes(claim);
    addRandomDiagnosisCodes(claim);
    addRandomPayers(claim);
    addRandomAudits(claim);
    return claim.build();
  }

  private void addRandomFieldValues(FissClaim.Builder claim) {
    claim.setDcn(randomDigit(5, 8)).setHicNo(randomDigit(12, 12));
    claim.setCurrStatusEnum(randomEnum(FissClaimStatusEnums));
    oneOf(
        () -> claim.setCurrLoc1Enum(randomEnum(FissProcessingTypeEnums)),
        () -> claim.setCurrLoc1Unrecognized(randomLetter(1, 1)));
    oneOf(
        () -> claim.setCurrLoc2Enum(randomEnum(FissCurrentLocation2Enums)),
        () -> claim.setCurrLoc2Unrecognized(randomLetter(1, 5)));
    optional(() -> claim.setMedaProvId(randomAlphaNumeric(13, 13)));
    optional(() -> claim.setTotalChargeAmount(randomAmount()));
    optional(() -> claim.setRecdDtCymd(randomDate()));
    optional(() -> claim.setCurrTranDtCymd(randomDate()));
    optional(() -> claim.setAdmDiagCode(randomLetter(1, 7)));
    optional(() -> claim.setNpiNumber(randomDigit(10, 10)));
    optional(() -> claim.setMbi(randomAlphaNumeric(13, 13)));
    optional(() -> claim.setFedTaxNb(randomDigit(10, 10)));
    optional(() -> claim.setPracLocAddr1(randomAlphaNumeric(1, 100)));
    optional(() -> claim.setPracLocAddr2(randomAlphaNumeric(1, 100)));
    optional(() -> claim.setPracLocCity(randomAlphaNumeric(1, 100)));
    optional(() -> claim.setPracLocState(randomLetter(2, 2)));
    optional(() -> claim.setPracLocZip(randomDigit(1, 15)));
    optional(() -> claim.setStmtCovFromCymd(randomDate()));
    optional(() -> claim.setStmtCovToCymd(randomDate()));
    oneOf(
        () -> claim.setLobCdEnum(randomEnum(FissBillFacilityTypeEnums)),
        () -> claim.setLobCdUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setServTypeCdEnum(randomEnum(FissBillClassificationEnums)),
        () -> claim.setServTypeCdForClinicsEnum(randomEnum(FissBillClassificationForClinicsEnums)),
        () ->
            claim.setServTypeCdForSpecialFacilitiesEnum(
                randomEnum(FissBillClassificationForSpecialFacilitiesEnums)),
        () -> claim.setServTypCdUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> claim.setFreqCdEnum(randomEnum(FissBillFrequencyEnums)),
        () -> claim.setFreqCdUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setBillTypCd(randomAlphaNumeric(3, 3)));
    optional(() -> claim.setRejectCd(randomAlphaNumeric(1, 5)));
    optional(() -> claim.setFullPartDenInd(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setNonPayInd(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setXrefDcnNbr(randomAlphaNumeric(1, 23)));
    oneOf(
        () -> claim.setAdjReqCdEnum(randomEnum(FissAdjustmentRequestorCodeEnums)),
        () -> claim.setAdjReqCdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setAdjReasCd(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setCancelXrefDcn(randomAlphaNumeric(1, 23)));
    optional(() -> claim.setCancelDateCymd(randomDate()));
    oneOf(
        () -> claim.setCancAdjCdEnum(randomEnum(FissCancelAdjustmentCodeEnums)),
        () -> claim.setCancAdjCdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setOriginalXrefDcn(randomAlphaNumeric(1, 23)));
    optional(() -> claim.setPaidDtCymd(randomDate()));
    optional(() -> claim.setAdmDateCymd(randomDate()));
    oneOf(
        () -> claim.setAdmSourceEnum(randomEnum(FissSourceOfAdmissionEnums)),
        () -> claim.setAdmSourceUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setPrimaryPayerCodeEnum(randomEnum(FissPayersCodeEnums)),
        () -> claim.setPrimaryPayerCodeUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setAttendPhysId(randomAlphaNumeric(1, 16)));
    optional(() -> claim.setAttendPhysLname(randomAlphaNumeric(1, 17)));
    optional(() -> claim.setAttendPhysFname(randomAlphaNumeric(1, 18)));
    optional(() -> claim.setAttendPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setAttendPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setAttendPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setOperatingPhysId(randomAlphaNumeric(1, 16)));
    optional(() -> claim.setOperPhysLname(randomAlphaNumeric(1, 17)));
    optional(() -> claim.setOperPhysFname(randomAlphaNumeric(1, 18)));
    optional(() -> claim.setOperPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setOperPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setOperPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setOthPhysId(randomAlphaNumeric(1, 16)));
    optional(() -> claim.setOthPhysLname(randomAlphaNumeric(1, 17)));
    optional(() -> claim.setOthPhysFname(randomAlphaNumeric(1, 18)));
    optional(() -> claim.setOthPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setOthPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setOthPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setXrefHicNbr(randomAlphaNumeric(1, 12)));
    oneOf(
        () ->
            claim.setProcNewHicIndEnum(
                randomEnum(FissProcessNewHealthInsuranceClaimNumberIndicatorEnums)),
        () -> claim.setProcNewHicIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setNewHic(randomAlphaNumeric(1, 12)));
    oneOf(
        () -> claim.setReposIndEnum(randomEnum(FissRepositoryIndicatorEnums)),
        () -> claim.setReposIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setReposHic(randomAlphaNumeric(1, 12)));
    oneOf(
        () ->
            claim.setMbiSubmBeneIndEnum(
                randomEnum(FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifierEnums)),
        () -> claim.setMbiSubmBeneIndUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () ->
            claim.setAdjMbiIndEnum(
                randomEnum(FissAdjustmentMedicareBeneficiaryIdentifierIndicatorEnums)),
        () -> claim.setAdjMbiIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setAdjMbi(randomAlphaNumeric(1, 11)));
    optional(() -> claim.setMedicalRecordNo(randomAlphaNumeric(1, 17)));
  }

  private void addRandomProcCodes(FissClaim.Builder claim) {
    final int count = randomInt(MAX_PROC_CODES);
    if (count > 0) {
      final String primaryCode = randomLetter(1, 7);
      claim.setPrincipleDiag(primaryCode);
      for (int i = 1; i <= count; ++i) {
        FissProcedureCode.Builder procCode =
            FissProcedureCode.newBuilder().setProcCd(i == 1 ? primaryCode : randomLetter(1, 7));
        optional(() -> procCode.setProcFlag(randomLetter(1, 4)));
        optional(() -> procCode.setProcDt(randomDate()));
        claim.addFissProcCodes(procCode);
      }
    }
  }

  private void addRandomDiagnosisCodes(FissClaim.Builder claim) {
    final int count = randomInt(MAX_DIAG_CODES);
    for (int i = 1; i <= count; ++i) {
      FissDiagnosisCode.Builder diagCode =
          FissDiagnosisCode.newBuilder().setDiagCd2(randomLetter(1, 7));
      oneOf(
          () ->
              diagCode.setDiagPoaIndEnum(randomEnum(FissDiagnosisPresentOnAdmissionIndicatorEnums)),
          () -> diagCode.setDiagPoaIndUnrecognized(randomLetter(1, 1)));
      optional(() -> diagCode.setBitFlags(randomLetter(1, 4)));
      claim.addFissDiagCodes(diagCode);
    }
  }

  private void addRandomPayers(FissClaim.Builder claim) {
    final int count = 1 + randomInt(MAX_PAYERS);
    for (int i = 1; i <= count; ++i) {
      FissPayer.Builder payer = FissPayer.newBuilder();
      oneOf(() -> addBeneZPayer(payer), () -> addInsuredPayer(payer));
      claim.addFissPayers(payer.build());
    }
  }

  private void addBeneZPayer(FissPayer.Builder parent) {
    final FissBeneZPayer.Builder payer = FissBeneZPayer.newBuilder();

    oneOf(
        () -> payer.setPayersIdEnum(randomEnum(FissPayersCodeEnums)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> payer.setPayersName(randomAlphaNumeric(1, 32)));
    oneOf(
        () -> payer.setRelIndEnum(randomEnum(FissReleaseOfInformationEnums)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setAssignIndEnum(randomEnum(FissAssignmentOfBenefitsIndicatorEnums)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setProviderNumber(randomAlphaNumeric(1, 13)));
    optional(() -> payer.setAdjDcnIcn(randomAlphaNumeric(1, 23)));
    optional(() -> payer.setPriorPmt(randomAmount()));
    optional(() -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        () -> payer.setBeneRelEnum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setBeneRelUnrecognized(randomDigit(2, 2)));
    optional(() -> payer.setBeneLastName(randomLetter(1, 15)));
    optional(() -> payer.setBeneFirstName(randomLetter(1, 10)));
    optional(() -> payer.setBeneMidInit(randomLetter(1, 1)));
    optional(() -> payer.setBeneSsnHic(randomAlphaNumeric(1, 19)));
    optional(() -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional(() -> payer.setBeneDob(randomDate()));
    oneOf(
        () -> payer.setBeneSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setBeneSexUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setTreatAuthCd(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredRelX12Enum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));

    parent.setBeneZPayer(payer.build());
  }

  private void addInsuredPayer(FissPayer.Builder parent) {
    final FissInsuredPayer.Builder payer = FissInsuredPayer.newBuilder();

    oneOf(
        () -> payer.setPayersIdEnum(randomEnum(FissPayersCodeEnums)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> payer.setPayersName(randomLetter(1, 32)));
    oneOf(
        () -> payer.setRelIndEnum(randomEnum(FissReleaseOfInformationEnums)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setAssignIndEnum(randomEnum(FissAssignmentOfBenefitsIndicatorEnums)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setProviderNumber(randomLetter(8, 13)));
    optional(() -> payer.setAdjDcnIcn(randomLetter(23, 23)));
    optional(() -> payer.setPriorPmt(randomAmount()));
    optional(() -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        () -> payer.setInsuredRelEnum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelUnrecognized(randomDigit(2, 2)));
    optional(() -> payer.setInsuredName(randomLetter(1, 25)));
    optional(() -> payer.setInsuredSsnHic(randomLetter(1, 19)));
    optional(() -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional(() -> payer.setInsuredGroupNbr(randomLetter(1, 20)));
    optional(() -> payer.setTreatAuthCd(randomAlphaNumeric(1, 18)));
    oneOf(
        () -> payer.setInsuredSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredRelX12Enum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));
    optional(
        () -> {
          String date = randomDate();
          payer.setInsuredDob(date);
          payer.setInsuredDobText(date.replace("-", "").substring(4) + date.substring(0, 4));
        });

    parent.setInsuredPayer(payer.build());
  }

  private void addRandomAudits(FissClaim.Builder claim) {
    final int count = 1 + randomInt(MAX_AUDITS);
    for (int i = 1; i <= count; ++i) {
      FissAuditTrail.Builder payer = FissAuditTrail.newBuilder();

      oneOf(
          () -> payer.setBadtStatusEnum(randomEnum(FissClaimStatusEnums)),
          () -> payer.setBadtStatusUnrecognized(randomAlphaNumeric(1, 1)));
      optional(() -> payer.setBadtLoc(randomAlphaNumeric(1, 5)));
      optional(() -> payer.setBadtOperId(randomAlphaNumeric(1, 9)));
      optional(() -> payer.setBadtReas(randomAlphaNumeric(1, 5)));
      optional(() -> payer.setBadtCurrDateCymd(randomDate()));

      claim.addFissAuditTrail(payer.build());
    }
  }
}
