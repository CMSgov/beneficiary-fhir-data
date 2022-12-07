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
public class RandomFissClaimGenerator extends AbstractRandomClaimGenerator<FissClaim> {
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
   * Creates an instance for use in unit tests. Setting optionalOverride to true causes all optional
   * fields to be added to the claim. This is useful in some tests.
   *
   * @param seed seed for the PRNG
   * @param optionalOverride true if all optional fields should be populated
   */
  @VisibleForTesting
  RandomFissClaimGenerator(long seed, boolean optionalOverride, Clock clock) {
    super(seed, optionalOverride, clock);
  }

  @Override
  public FissClaim createRandomClaim() {
    FissClaim.Builder claim = FissClaim.newBuilder();
    always(
        "fiss",
        () -> {
          addRandomFieldValues(claim);
          addRandomProcCodes(claim);
          addRandomDiagnosisCodes(claim);
          addRandomPayers(claim);
          addRandomAudits(claim);
        });
    return claim.build();
  }

  private void addRandomFieldValues(FissClaim.Builder claim) {
    always("dcn", () -> claim.setDcn(randomDigit(5, 8)));
    always("hicNo", () -> claim.setHicNo(randomDigit(12, 12)));
    always("currStatus", () -> claim.setCurrStatusEnum(randomEnum(FissClaimStatusEnums)));
    oneOf(
        "loc1",
        () -> claim.setCurrLoc1Enum(randomEnum(FissProcessingTypeEnums)),
        () -> claim.setCurrLoc1Unrecognized(randomLetter(1, 1)));
    oneOf(
        "loc2",
        () -> claim.setCurrLoc2Enum(randomEnum(FissCurrentLocation2Enums)),
        () -> claim.setCurrLoc2Unrecognized(randomLetter(1, 5)));
    optional("provStateCd", () -> claim.setProvStateCd(randomAlphaNumeric(2, 2)));
    optional("provTypeFacilCd", () -> claim.setProvTypFacilCd(randomAlphaNumeric(1, 1)));
    optional("provEmerInd", () -> claim.setProvEmerInd(randomAlphaNumeric(1, 1)));
    optional("provDeptId", () -> claim.setProvDeptId(randomAlphaNumeric(3, 3)));
    optional(
        "medaProvId",
        () -> {
          String medaProvId = randomAlphaNumeric(13, 13);
          claim.setMedaProvId(medaProvId);
          claim.setMedaProv6(medaProvId.substring(0, 6));
        });
    optional("totalChargeAmount", () -> claim.setTotalChargeAmount(randomAmount()));
    optional("recdDtCymd", () -> claim.setRecdDtCymd(randomDate()));
    optional("currTranDtCymd", () -> claim.setCurrTranDtCymd(randomDate()));
    optional("admDiagCode", () -> claim.setAdmDiagCode(randomLetter(1, 7)));
    optional("npiNumber", () -> claim.setNpiNumber(randomDigit(10, 10)));
    optional("mbi", () -> claim.setMbi(randomAlphaNumeric(11, 11)));
    optional("fedTaxNb", () -> claim.setFedTaxNb(randomDigit(10, 10)));
    optional("pracLocAddr1", () -> claim.setPracLocAddr1(randomAlphaNumeric(1, 100)));
    optional("pracAddr2", () -> claim.setPracLocAddr2(randomAlphaNumeric(1, 100)));
    optional("pracLocCity", () -> claim.setPracLocCity(randomAlphaNumeric(1, 100)));
    optional("pracLocState", () -> claim.setPracLocState(randomLetter(2, 2)));
    optional("pracLocZip", () -> claim.setPracLocZip(randomDigit(1, 15)));
    optional(
        "stmtCovFromCymd",
        () -> {
          String date = randomDate();
          claim.setStmtCovFromCymd(date);
          claim.setStmtCovFromCymdText(date);
        });
    optional(
        "stmtCovToCymd",
        () -> {
          String date = randomDate();
          claim.setStmtCovToCymd(date);
          claim.setStmtCovToCymdText(date);
        });
    oneOf(
        "locCd",
        () -> claim.setLobCdEnum(randomEnum(FissBillFacilityTypeEnums)),
        () -> claim.setLobCdUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        "servTypeCd",
        () -> claim.setServTypeCdEnum(randomEnum(FissBillClassificationEnums)),
        () -> claim.setServTypeCdForClinicsEnum(randomEnum(FissBillClassificationForClinicsEnums)),
        () ->
            claim.setServTypeCdForSpecialFacilitiesEnum(
                randomEnum(FissBillClassificationForSpecialFacilitiesEnums)),
        () -> claim.setServTypCdUnrecognized(randomLetter(1, 1)));
    oneOf(
        "freqCd",
        () -> claim.setFreqCdEnum(randomEnum(FissBillFrequencyEnums)),
        () -> claim.setFreqCdUnrecognized(randomLetter(1, 1)));
    optional("billTypeCd", () -> claim.setBillTypCd(randomAlphaNumeric(3, 3)));
    optional("rejectCd", () -> claim.setRejectCd(randomAlphaNumeric(1, 5)));
    optional("fullPartDenInd", () -> claim.setFullPartDenInd(randomAlphaNumeric(1, 1)));
    optional("nonPayInd", () -> claim.setNonPayInd(randomAlphaNumeric(1, 2)));
    optional("xrefDcnNbr", () -> claim.setXrefDcnNbr(randomAlphaNumeric(1, 23)));
    oneOf(
        "adjReqCd",
        () -> claim.setAdjReqCdEnum(randomEnum(FissAdjustmentRequestorCodeEnums)),
        () -> claim.setAdjReqCdUnrecognized(randomAlphaNumeric(1, 1)));
    optional("adjReasCd", () -> claim.setAdjReasCd(randomAlphaNumeric(1, 2)));
    optional("cancelXrefDcn", () -> claim.setCancelXrefDcn(randomAlphaNumeric(1, 23)));
    optional("cancelDateCymd", () -> claim.setCancelDateCymd(randomDate()));
    oneOf(
        "cancAdjCd",
        () -> claim.setCancAdjCdEnum(randomEnum(FissCancelAdjustmentCodeEnums)),
        () -> claim.setCancAdjCdUnrecognized(randomAlphaNumeric(1, 1)));
    optional("originalXrefDcn", () -> claim.setOriginalXrefDcn(randomAlphaNumeric(1, 23)));
    optional("paidDtCymd", () -> claim.setPaidDtCymd(randomDate()));
    optional(
        "admDateCymd",
        () -> {
          String date = randomDate();
          claim.setAdmDateCymd(date);
          claim.setAdmDateCymdText(date);
        });
    oneOf(
        "admSource",
        () -> claim.setAdmSourceEnum(randomEnum(FissSourceOfAdmissionEnums)),
        () -> claim.setAdmSourceUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        "primaryPayer",
        () -> claim.setPrimaryPayerCodeEnum(randomEnum(FissPayersCodeEnums)),
        () -> claim.setPrimaryPayerCodeUnrecognized(randomAlphaNumeric(1, 1)));
    optional("attendPhysId", () -> claim.setAttendPhysId(randomAlphaNumeric(1, 16)));
    optional("attendPhysLname", () -> claim.setAttendPhysLname(randomAlphaNumeric(1, 17)));
    optional("attendPhysFname", () -> claim.setAttendPhysFname(randomAlphaNumeric(1, 18)));
    optional("attendPhysMint", () -> claim.setAttendPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        "attendPhysFlag",
        () -> claim.setAttendPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setAttendPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional("operatingPhysId", () -> claim.setOperatingPhysId(randomAlphaNumeric(1, 16)));
    optional("operPhysLname", () -> claim.setOperPhysLname(randomAlphaNumeric(1, 17)));
    optional("operPhysFname", () -> claim.setOperPhysFname(randomAlphaNumeric(1, 18)));
    optional("operPhysMint", () -> claim.setOperPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        "operPhysFlag",
        () -> claim.setOperPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setOperPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional("othPhysId", () -> claim.setOthPhysId(randomAlphaNumeric(1, 16)));
    optional("othPhysLanme", () -> claim.setOthPhysLname(randomAlphaNumeric(1, 17)));
    optional("othPhysFname", () -> claim.setOthPhysFname(randomAlphaNumeric(1, 18)));
    optional("othPhysMint", () -> claim.setOthPhysMint(randomAlphaNumeric(1, 1)));
    oneOf(
        "othPhysFlag",
        () -> claim.setOthPhysFlagEnum(randomEnum(FissPhysicianFlagEnums)),
        () -> claim.setOthPhysFlagUnrecognized(randomAlphaNumeric(1, 1)));
    optional("xrefHicNbr", () -> claim.setXrefHicNbr(randomAlphaNumeric(1, 12)));
    oneOf(
        "procNewHicInd",
        () ->
            claim.setProcNewHicIndEnum(
                randomEnum(FissProcessNewHealthInsuranceClaimNumberIndicatorEnums)),
        () -> claim.setProcNewHicIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional("newHic", () -> claim.setNewHic(randomAlphaNumeric(1, 12)));
    oneOf(
        "reposInd",
        () -> claim.setReposIndEnum(randomEnum(FissRepositoryIndicatorEnums)),
        () -> claim.setReposIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional("reposHic", () -> claim.setReposHic(randomAlphaNumeric(1, 12)));
    oneOf(
        "mbiSubmBeneInd",
        () ->
            claim.setMbiSubmBeneIndEnum(
                randomEnum(FissHealthInsuranceClaimNumberOrMedicareBeneficiaryIdentifierEnums)),
        () -> claim.setMbiSubmBeneIndUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        "adjMbiInd",
        () ->
            claim.setAdjMbiIndEnum(
                randomEnum(FissAdjustmentMedicareBeneficiaryIdentifierIndicatorEnums)),
        () -> claim.setAdjMbiIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional("adjMbi", () -> claim.setAdjMbi(randomAlphaNumeric(1, 11)));
    optional("medicalRecordNo", () -> claim.setMedicalRecordNo(randomAlphaNumeric(1, 17)));
  }

  private void addRandomProcCodes(FissClaim.Builder claim) {
    always(
        "procCode",
        () -> {
          final int count = randomInt(MAX_PROC_CODES);

          if (count > 0) {
            always("principleDiag", () -> claim.setPrincipleDiag(randomLetter(1, 7)));
            final String primaryCode = claim.getPrincipleDiag();

            for (int i = 1; i <= count; ++i) {
              final int INDEX = i;
              FissProcedureCode.Builder procCode = FissProcedureCode.newBuilder();

              always(
                  String.format("[%d]", INDEX),
                  () -> {
                    always(
                        "procCd",
                        () -> procCode.setProcCd(INDEX == 1 ? primaryCode : randomLetter(1, 7)));
                    optional("procFlag", () -> procCode.setProcFlag(randomLetter(1, 4)));
                    optional("procDt", () -> procCode.setProcDt(randomDate()));
                  });

              procCode.setRdaPosition(INDEX);
              claim.addFissProcCodes(procCode);
            }
          }
        });
  }

  private void addRandomDiagnosisCodes(FissClaim.Builder claim) {
    always(
        "diagnosisCode",
        () -> {
          final int count = randomInt(MAX_DIAG_CODES);

          for (int i = 1; i <= count; ++i) {
            FissDiagnosisCode.Builder diagCode = FissDiagnosisCode.newBuilder();

            always(
                String.format("[%d]", i),
                () -> {
                  optional("diagCd2", () -> diagCode.setDiagCd2(randomLetter(1, 7)));
                  oneOf(
                      "diagPoaInd",
                      () ->
                          diagCode.setDiagPoaIndEnum(
                              randomEnum(FissDiagnosisPresentOnAdmissionIndicatorEnums)),
                      () -> diagCode.setDiagPoaIndUnrecognized(randomLetter(1, 1)));
                  optional("bitFlags", () -> diagCode.setBitFlags(randomLetter(1, 4)));
                });

            diagCode.setRdaPosition(i);
            claim.addFissDiagCodes(diagCode);
          }
        });
  }

  private void addRandomPayers(FissClaim.Builder claim) {
    always(
        "payer",
        () -> {
          final int count = 1 + randomInt(MAX_PAYERS);

          for (int i = 1; i <= count; ++i) {
            final int POSITION = i;
            FissPayer.Builder payer = FissPayer.newBuilder();

            oneOf(
                String.format("[%d]", POSITION),
                () -> addBeneZPayer(payer, POSITION),
                () -> addInsuredPayer(payer, POSITION));

            claim.addFissPayers(payer.build());
          }
        });
  }

  private void addBeneZPayer(FissPayer.Builder parent, int position) {
    final FissBeneZPayer.Builder payer = FissBeneZPayer.newBuilder();

    oneOf(
        "payersId",
        () -> payer.setPayersIdEnum(randomEnum(FissPayersCodeEnums)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional("payersName", () -> payer.setPayersName(randomAlphaNumeric(1, 32)));
    oneOf(
        "relInd",
        () -> payer.setRelIndEnum(randomEnum(FissReleaseOfInformationEnums)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        "assignInd",
        () -> payer.setAssignIndEnum(randomEnum(FissAssignmentOfBenefitsIndicatorEnums)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional("providerNumber", () -> payer.setProviderNumber(randomAlphaNumeric(1, 13)));
    optional("adjDcnIcn", () -> payer.setAdjDcnIcn(randomAlphaNumeric(1, 23)));
    optional("priorPmt", () -> payer.setPriorPmt(randomAmount()));
    optional("estAmtDue", () -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        "beneRel",
        () -> payer.setBeneRelEnum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setBeneRelUnrecognized(randomDigit(2, 2)));
    optional("beneLastName", () -> payer.setBeneLastName(randomLetter(1, 15)));
    optional("beneFirstName", () -> payer.setBeneFirstName(randomLetter(1, 10)));
    optional("beneMidInit", () -> payer.setBeneMidInit(randomLetter(1, 1)));
    optional("beneSsnHic", () -> payer.setBeneSsnHic(randomAlphaNumeric(1, 19)));
    optional("insuredGroupName", () -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional("beneDob", () -> payer.setBeneDob(randomDate()));
    oneOf(
        "beneSex",
        () -> payer.setBeneSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setBeneSexUnrecognized(randomLetter(1, 1)));
    optional("treatAuthCd", () -> payer.setTreatAuthCd(randomLetter(1, 1)));
    oneOf(
        "insuredSex",
        () -> payer.setInsuredSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        "insuredRelX12",
        () -> payer.setInsuredRelX12Enum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));
    payer.setRdaPosition(position);

    parent.setBeneZPayer(payer.build());
  }

  private void addInsuredPayer(FissPayer.Builder parent, int position) {
    final FissInsuredPayer.Builder payer = FissInsuredPayer.newBuilder();

    oneOf(
        "payersId",
        () -> payer.setPayersIdEnum(randomEnum(FissPayersCodeEnums)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional("payersName", () -> payer.setPayersName(randomLetter(1, 32)));
    oneOf(
        "relInd",
        () -> payer.setRelIndEnum(randomEnum(FissReleaseOfInformationEnums)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        "assignInd",
        () -> payer.setAssignIndEnum(randomEnum(FissAssignmentOfBenefitsIndicatorEnums)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional("providerNumber", () -> payer.setProviderNumber(randomLetter(8, 13)));
    optional("adjDcnIcn", () -> payer.setAdjDcnIcn(randomLetter(23, 23)));
    optional("priorPmt", () -> payer.setPriorPmt(randomAmount()));
    optional("estAmtDue", () -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        "insuredRel",
        () -> payer.setInsuredRelEnum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelUnrecognized(randomDigit(2, 2)));
    optional("insuredName", () -> payer.setInsuredName(randomLetter(1, 25)));
    optional("insuredSsnHic", () -> payer.setInsuredSsnHic(randomLetter(1, 19)));
    optional("insuredGroupName", () -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional("insuredGroupNbr", () -> payer.setInsuredGroupNbr(randomLetter(1, 20)));
    optional("treatAuthCd", () -> payer.setTreatAuthCd(randomAlphaNumeric(1, 18)));
    oneOf(
        "insuredSex",
        () -> payer.setInsuredSexEnum(randomEnum(FissBeneficiarySexEnums)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        "insuredRelX12",
        () -> payer.setInsuredRelX12Enum(randomEnum(FissPatientRelationshipCodeEnums)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));
    optional(
        "insureDob",
        () -> {
          String date = randomDate();
          payer.setInsuredDob(date);
          payer.setInsuredDobText(date.replace("-", "").substring(4) + date.substring(0, 4));
        });
    payer.setRdaPosition(position);

    parent.setInsuredPayer(payer.build());
  }

  private void addRandomAudits(FissClaim.Builder claim) {
    always(
        "audit",
        () -> {
          final int count = 1 + randomInt(MAX_AUDITS);

          for (int i = 1; i <= count; ++i) {
            FissAuditTrail.Builder audit = FissAuditTrail.newBuilder();

            always(
                String.format("[%d]", i),
                () -> {
                  oneOf(
                      "badtStatus",
                      () -> audit.setBadtStatusEnum(randomEnum(FissClaimStatusEnums)),
                      () -> audit.setBadtStatusUnrecognized(randomAlphaNumeric(1, 1)));
                  optional("badtLoc", () -> audit.setBadtLoc(randomAlphaNumeric(1, 5)));
                  optional("badtOperId", () -> audit.setBadtOperId(randomAlphaNumeric(1, 9)));
                  optional("badtReas", () -> audit.setBadtReas(randomAlphaNumeric(1, 5)));
                  optional("badtCurrDateCymd", () -> audit.setBadtCurrDateCymd(randomDate()));
                });

            audit.setRdaPosition(i);

            claim.addFissAuditTrail(audit.build());
          }
        });
  }
}
