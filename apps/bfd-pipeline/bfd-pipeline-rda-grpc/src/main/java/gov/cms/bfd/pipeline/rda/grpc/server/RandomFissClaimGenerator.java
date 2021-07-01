package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Objects of this class create populated FissClaim objects using random data. The purpose is simply
 * to rapidly produce objects for pipeline testing to try out different scenarios for
 * transformation. The purpose is NOT to produce realistic/valid data. The random number seed is
 * settable in the constructor to allow for for predictable unit tests. Every optional field has a
 * 50% chance of being present in each claim. Arrays have randomly assigned variable length
 * (including zero).
 */
public class RandomFissClaimGenerator {
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  private static final String DIGIT = "1234567890";
  private static final String ALNUM = ALPHA + DIGIT;
  private static final int MAX_DAYS_AGO = 180;
  private static final int MAX_PROC_CODES = 7;
  private static final List<FissClaimStatus> CLAIM_STATUSES = valuesOf(FissClaimStatus.values());
  private static final List<FissProcessingType> PROCESSING_TYPES =
      valuesOf(FissProcessingType.values());
  private static final List<FissCurrentLocation2> CURR_LOC2S =
      valuesOf(FissCurrentLocation2.values());

  private final Random random;

  private static <T extends Enum<T>> List<T> valuesOf(T[] values) {
    return Arrays.stream(values)
        .filter(v -> !v.name().equals("UNRECOGNIZED"))
        .collect(ImmutableList.toImmutableList());
  }

  public RandomFissClaimGenerator(long seed) {
    this.random = new Random(seed);
  }

  public FissClaim randomClaim() {
    FissClaim.Builder claim = FissClaim.newBuilder();
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
    final int procCodeCount = random.nextInt(MAX_PROC_CODES);
    if (procCodeCount > 0) {
      final String primaryCode = randomString(ALPHA, 1, 7);
      claim.setPrincipleDiag(primaryCode);
      for (int i = 1; i <= procCodeCount; ++i) {
        FissProcedureCode.Builder procCode =
            FissProcedureCode.newBuilder()
                .setProcCd(i == 1 ? primaryCode : randomString(ALPHA, 1, 7));
        optional(() -> procCode.setProcFlag(randomString(ALPHA, 1, 4)));
        optional(() -> procCode.setProcDt(randomDate()));
        claim.addFissProcCodes(procCode);
      }
    }
    return claim.build();
  }

  private char randomChar(String characters) {
    return characters.charAt(random.nextInt(characters.length()));
  }

  private String randomString(String characters, int minLength, int maxLength) {
    final StringBuilder sb = new StringBuilder();
    final int len = minLength + random.nextInt(maxLength - minLength + 1);
    while (sb.length() < len) {
      sb.append(randomChar(characters));
    }
    return sb.toString();
  }

  private String randomDate() {
    final LocalDate date = LocalDate.now().minusDays(random.nextInt(MAX_DAYS_AGO));
    return date.toString();
  }

  private String randomAmount() {
    final int dollarDigits = 1 + random.nextInt(5);
    final StringBuilder amount = new StringBuilder(randomString(DIGIT, dollarDigits, dollarDigits));
    while (amount.length() > 1 && amount.charAt(0) == '0') {
      amount.setCharAt(0, randomChar(DIGIT));
    }
    amount.append(".").append(randomString(DIGIT, 2, 2));
    return amount.toString();
  }

  private <T> T randomEnum(List<T> values) {
    return values.get(random.nextInt(values.size()));
  }

  private void optional(Runnable action) {
    if (random.nextBoolean()) {
      action.run();
    }
  }

  private void either(Runnable action1, Runnable action2) {
    if (random.nextBoolean()) {
      action1.run();
    } else {
      action2.run();
    }
  }
}
