package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
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
  private static final List<FissClaimStatus> CLAIM_STATUSES = enumValues(FissClaimStatus.values());
  private static final List<FissProcessingType> PROCESSING_TYPES =
      enumValues(FissProcessingType.values());
  private static final List<FissCurrentLocation2> CURR_LOC2S =
      enumValues(FissCurrentLocation2.values());
  private static final List<FissDiagnosisPresentOnAdmissionIndicator> INDICATORS =
      enumValues(FissDiagnosisPresentOnAdmissionIndicator.values());

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
    return claim.build();
  }

  private void addRandomFieldValues(FissClaim.Builder claim) {
    claim.setDcn(randomDigit(5, 8)).setHicNo(randomDigit(12, 12));
    oneOf(
        () -> claim.setCurrStatusEnum(randomEnum(CLAIM_STATUSES)),
        () -> claim.setCurrStatusUnrecognized(randomLetter(1, 1)));
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
}
