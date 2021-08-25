package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
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
import gov.cms.mpsm.rda.v1.fiss.FissInsuredPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
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
  private static final List<FissClaimStatus> CLAIM_STATUSES = enumValues(FissClaimStatus.values());
  private static final List<FissProcessingType> PROCESSING_TYPES =
      enumValues(FissProcessingType.values());
  private static final List<FissCurrentLocation2> CURR_LOC2S =
      enumValues(FissCurrentLocation2.values());
  private static final List<FissDiagnosisPresentOnAdmissionIndicator> INDICATORS =
      enumValues(FissDiagnosisPresentOnAdmissionIndicator.values());
  private static final List<FissBillFacilityType> FACILITY_TYPES =
      enumValues(FissBillFacilityType.values());
  private static final List<FissBillClassification> BILL_CLASSIFICATIONS =
      enumValues(FissBillClassification.values());
  private static final List<FissBillClassificationForClinics> CLINIC_BILL_CLASSIFICATIONS =
      enumValues(FissBillClassificationForClinics.values());
  private static final List<FissBillClassificationForSpecialFacilities>
      SPECIAL_BILL_CLASSIFICATIONS =
          enumValues(FissBillClassificationForSpecialFacilities.values());
  private static final List<FissBillFrequency> BILL_FREQUENCIES =
      enumValues(FissBillFrequency.values());
  private static final List<FissPayersCode> PAYER_CODES = enumValues(FissPayersCode.values());
  private static final List<FissReleaseOfInformation> RELEASE_OF_INFOS =
      enumValues(FissReleaseOfInformation.values());
  private static final List<FissAssignmentOfBenefitsIndicator> ASSIGNMENT_OF_BENE_INDICATORS =
      enumValues(FissAssignmentOfBenefitsIndicator.values());
  private static final List<FissPatientRelationshipCode> PATIENT_REL_CODES =
      enumValues(FissPatientRelationshipCode.values());
  private static final List<FissBeneficiarySex> BENE_SEXES =
      enumValues(FissBeneficiarySex.values());

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
    return claim.build();
  }

  private void addRandomFieldValues(FissClaim.Builder claim) {
    claim.setDcn(randomDigit(5, 8)).setHicNo(randomDigit(12, 12));
    claim.setCurrStatusEnum(randomEnum(CLAIM_STATUSES));
    oneOf(
        () -> claim.setCurrLoc1Enum(randomEnum(PROCESSING_TYPES)),
        () -> claim.setCurrLoc1Unrecognized(randomLetter(1, 1)));
    oneOf(
        () -> claim.setCurrLoc2Enum(randomEnum(CURR_LOC2S)),
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
        () -> claim.setLobCdEnum(randomEnum(FACILITY_TYPES)),
        () -> claim.setLobCdUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setServTypeCdEnum(randomEnum(BILL_CLASSIFICATIONS)),
        () -> claim.setServTypeCdForClinicsEnum(randomEnum(CLINIC_BILL_CLASSIFICATIONS)),
        () -> claim.setServTypeCdForSpecialFacilitiesEnum(randomEnum(SPECIAL_BILL_CLASSIFICATIONS)),
        () -> claim.setServTypCdUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> claim.setFreqCdEnum(randomEnum(BILL_FREQUENCIES)),
        () -> claim.setFreqCdUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setBillTypCd(randomAlphaNumeric(3, 3)));
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
          () -> diagCode.setDiagPoaIndEnum(randomEnum(INDICATORS)),
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
        () -> payer.setPayersIdEnum(randomEnum(PAYER_CODES)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> payer.setPayersName(randomAlphaNumeric(1, 32)));
    oneOf(
        () -> payer.setRelIndEnum(randomEnum(RELEASE_OF_INFOS)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setAssignIndEnum(randomEnum(ASSIGNMENT_OF_BENE_INDICATORS)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setProviderNumber(randomAlphaNumeric(1, 13)));
    optional(() -> payer.setAdjDcnIcn(randomAlphaNumeric(1, 23)));
    optional(() -> payer.setPriorPmt(randomAmount()));
    optional(() -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        () -> payer.setBeneRelEnum(randomEnum(PATIENT_REL_CODES)),
        () -> payer.setBeneRelUnrecognized(randomDigit(2, 2)));
    optional(() -> payer.setBeneLastName(randomLetter(1, 15)));
    optional(() -> payer.setBeneFirstName(randomLetter(1, 10)));
    optional(() -> payer.setBeneMidInit(randomLetter(1, 1)));
    optional(() -> payer.setBeneSsnHic(randomAlphaNumeric(1, 19)));
    optional(() -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional(() -> payer.setBeneDob(randomDate()));
    oneOf(
        () -> payer.setBeneSexEnum(randomEnum(BENE_SEXES)),
        () -> payer.setBeneSexUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setTreatAuthCd(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredSexEnum(randomEnum(BENE_SEXES)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredRelX12Enum(randomEnum(PATIENT_REL_CODES)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));

    parent.setBeneZPayer(payer.build());
  }

  private void addInsuredPayer(FissPayer.Builder parent) {
    final FissInsuredPayer.Builder payer = FissInsuredPayer.newBuilder();

    oneOf(
        () -> payer.setPayersIdEnum(randomEnum(PAYER_CODES)),
        () -> payer.setPayersIdUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> payer.setPayersName(randomLetter(1, 32)));
    oneOf(
        () -> payer.setRelIndEnum(randomEnum(RELEASE_OF_INFOS)),
        () -> payer.setRelIndUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setAssignIndEnum(randomEnum(ASSIGNMENT_OF_BENE_INDICATORS)),
        () -> payer.setAssignIndUnrecognized(randomLetter(1, 1)));
    optional(() -> payer.setProviderNumber(randomLetter(8, 13)));
    optional(() -> payer.setAdjDcnIcn(randomLetter(23, 23)));
    optional(() -> payer.setPriorPmt(randomAmount()));
    optional(() -> payer.setEstAmtDue(randomAmount()));
    oneOf(
        () -> payer.setInsuredRelEnum(randomEnum(PATIENT_REL_CODES)),
        () -> payer.setInsuredRelUnrecognized(randomDigit(2, 2)));
    optional(() -> payer.setInsuredName(randomLetter(1, 25)));
    optional(() -> payer.setInsuredSsnHic(randomLetter(1, 19)));
    optional(() -> payer.setInsuredGroupName(randomLetter(1, 17)));
    optional(() -> payer.setInsuredGroupNbr(randomLetter(1, 20)));
    optional(() -> payer.setTreatAuthCd(randomAlphaNumeric(1, 18)));
    oneOf(
        () -> payer.setInsuredSexEnum(randomEnum(BENE_SEXES)),
        () -> payer.setInsuredSexUnrecognized(randomLetter(1, 1)));
    oneOf(
        () -> payer.setInsuredRelX12Enum(randomEnum(PATIENT_REL_CODES)),
        () -> payer.setInsuredRelX12Unrecognized(randomDigit(2, 2)));
    optional(
        () -> {
          String date = randomDate();
          payer.setInsuredDob(date);
          payer.setInsuredDobText(date.replace("-", "").substring(4) + date.substring(0, 4));
        });

    parent.setInsuredPayer(payer.build());
  }
}
