package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
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
  private static final List<FissClaimStatus> CLAIM_STATUSES = enumValues(FissClaimStatus.values());
  private static final List<FissProcessingType> PROCESSING_TYPES =
      enumValues(FissProcessingType.values());
  private static final List<FissCurrentLocation2> CURR_LOC2S =
      enumValues(FissCurrentLocation2.values());
  private static final List<FissDiagnosisPresentOnAdmissionIndicator> INDICATORS =
      enumValues(FissDiagnosisPresentOnAdmissionIndicator.values());

  public RandomFissClaimGenerator(long seed) {
    super(seed);
  }

  public FissClaim randomClaim() {
    FissClaim.Builder claim = FissClaim.newBuilder();
    addRandomFieldValues(claim);
    addRandomProcCodes(claim);
    addRandomDiagnosisCodes(claim);
    return claim.build();
  }

  private void addRandomFieldValues(FissClaim.Builder claim) {
    claim.setDcn(randomString(DIGIT, 5, 8)).setHicNo(randomString(DIGIT, 12, 12));
    either(
        () -> claim.setCurrStatusEnum(randomEnum(CLAIM_STATUSES)),
        () -> claim.setCurrStatusUnrecognized(randomString(ALPHA, 1, 1)));
    either(
        () -> claim.setCurrLoc1Enum(randomEnum(PROCESSING_TYPES)),
        () -> claim.setCurrLoc1Unrecognized(randomString(ALPHA, 1, 1)));
    either(
        () -> claim.setCurrLoc2Enum(randomEnum(CURR_LOC2S)),
        () -> claim.setCurrStatusUnrecognized(randomString(ALPHA, 1, 5)));
    optional(() -> claim.setMedaProvId(randomString(ALNUM, 13, 13)));
    optional(() -> claim.setTotalChargeAmount(randomAmount()));
    optional(() -> claim.setRecdDtCymd(randomDate()));
    optional(() -> claim.setCurrTranDtCymd(randomDate()));
    optional(() -> claim.setAdmDiagCode(randomString(ALPHA, 1, 7)));
    optional(() -> claim.setNpiNumber(randomString(DIGIT, 10, 10)));
    optional(() -> claim.setMbi(randomString(ALNUM, 13, 13)));
    optional(() -> claim.setFedTaxNb(randomString(DIGIT, 10, 10)));
  }

  private void addRandomProcCodes(FissClaim.Builder claim) {
    final int count = random.nextInt(MAX_PROC_CODES);
    if (count > 0) {
      final String primaryCode = randomString(ALPHA, 1, 7);
      claim.setPrincipleDiag(primaryCode);
      for (int i = 1; i <= count; ++i) {
        FissProcedureCode.Builder procCode =
            FissProcedureCode.newBuilder()
                .setProcCd(i == 1 ? primaryCode : randomString(ALPHA, 1, 7));
        optional(() -> procCode.setProcFlag(randomString(ALPHA, 1, 4)));
        optional(() -> procCode.setProcDt(randomDate()));
        claim.addFissProcCodes(procCode);
      }
    }
  }

  private void addRandomDiagnosisCodes(FissClaim.Builder claim) {
    final int count = random.nextInt(MAX_DIAG_CODES);
    for (int i = 1; i <= count; ++i) {
      FissDiagnosisCode.Builder diagCode =
          FissDiagnosisCode.newBuilder().setDiagCd2(randomString(ALPHA, 1, 7));
      either(
          () -> diagCode.setDiagPoaIndEnum(randomEnum(INDICATORS)),
          () -> diagCode.setDiagPoaIndUnrecognized(randomString(ALPHA, 1, 1)));
      optional(() -> diagCode.setBitFlags(randomString(ALPHA, 1, 4)));
      claim.addFissDiagCodes(diagCode);
    }
  }
}
