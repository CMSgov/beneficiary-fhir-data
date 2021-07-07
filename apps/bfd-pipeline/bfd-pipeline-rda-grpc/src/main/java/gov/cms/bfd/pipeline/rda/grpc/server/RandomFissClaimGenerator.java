package gov.cms.bfd.pipeline.rda.grpc.server;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Objects of this class create populated FissClaim objects using random data. The purpose is simply
 * to rapidly produce objects for pipeline testing to try out different scenarios for
 * transformation. The purpose is NOT to produce realistic/valid data. The random number seed is
 * settable in the constructor to allow for for predictable unit tests. Every optional field has a
 * 50% chance of being present in each claim. Arrays have randomly assigned variable length
 * (including zero).
 */
public class RandomFissClaimGenerator {
  private static final EnumSet<FissClaimStatus> SKIPPED_CLAIM_STATUS_SET =
      EnumSet.of(FissClaimStatus.UNRECOGNIZED, FissClaimStatus.CLAIM_STATUS_UNSET);
  private static final EnumSet<FissProcessingType> SKIPPED_PROC_TYPE_SET =
      EnumSet.of(FissProcessingType.UNRECOGNIZED, FissProcessingType.PROCESSING_TYPE_UNSET);
  private static final String ALPHA = "bcdfghjkmnpqrstvwxz";
  private static final String DIGIT = "1234567890";
  private static final String ALNUM = ALPHA + DIGIT;
  private static final int MAX_DAYS_AGO = 180;
  private static final int MAX_PROC_CODES = 7;
  private static final List<FissClaimStatus> CLAIM_STATUSES =
      Arrays.stream(FissClaimStatus.values())
          .filter(x -> !SKIPPED_CLAIM_STATUS_SET.contains(x))
          .collect(Collectors.collectingAndThen(toList(), ImmutableList::copyOf));
  private static final List<FissProcessingType> PROCESSING_TYPES =
      Arrays.stream(FissProcessingType.values())
          .filter(x -> !SKIPPED_PROC_TYPE_SET.contains(x))
          .collect(Collectors.collectingAndThen(toList(), ImmutableList::copyOf));

  private final Random random;

  public RandomFissClaimGenerator(long seed) {
    this.random = new Random(seed);
  }

  public FissClaim randomClaim() {
    FissClaim.Builder claim = FissClaim.newBuilder();
    claim
        .setDcn(randomString(DIGIT, 5, 8))
        .setHicNo(randomString(DIGIT, 12, 12))
        .setCurrStatus(randomEnum(CLAIM_STATUSES))
        .setCurrLoc1(randomEnum(PROCESSING_TYPES))
        .setCurrLoc2(randomString(ALNUM, 1, 5));
    optional(() -> claim.setMedaProvId(randomString(ALNUM, 13, 13)));
    optional(() -> claim.setTotalChargeAmount(randomAmount()));
    optional(() -> claim.setRecdDt(randomDate()));
    optional(() -> claim.setCurrTranDate(randomDate()));
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
}
