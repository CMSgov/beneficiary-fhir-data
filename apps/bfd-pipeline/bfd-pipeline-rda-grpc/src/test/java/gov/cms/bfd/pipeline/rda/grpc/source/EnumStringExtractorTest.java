package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableSet;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the {@link EnumStringExtractor}. */
public class EnumStringExtractorTest {
  /** Test extractor for Fiss claims. */
  private final EnumStringExtractor<FissClaim, FissClaimStatus> fissStatusExtractor =
      new EnumStringExtractor<>(
          FissClaim::hasCurrStatusEnum,
          FissClaim::getCurrStatusEnum,
          FissClaim::hasCurrStatusUnrecognized,
          FissClaim::getCurrStatusUnrecognized,
          FissClaimStatus.UNRECOGNIZED,
          ImmutableSet.of(FissClaimStatus.CLAIM_STATUS_SUSPEND),
          ImmutableSet.of());
  /** Test extractor for MCS claims. */
  private final EnumStringExtractor<McsClaim, McsStatusCode> mcsStatusExtractor =
      new EnumStringExtractor<>(
          McsClaim::hasIdrStatusCodeEnum,
          McsClaim::getIdrStatusCodeEnum,
          McsClaim::hasIdrStatusCodeUnrecognized,
          McsClaim::getIdrStatusCodeUnrecognized,
          McsStatusCode.UNRECOGNIZED,
          ImmutableSet.of(McsStatusCode.STATUS_CODE_NOT_USED),
          ImmutableSet.of(EnumStringExtractor.Options.RejectUnrecognized));
  /** The test Fiss claim to extract from. */
  private FissClaim.Builder fissClaim;
  /** The test MCS claim to extract from. */
  private McsClaim.Builder mcsClaim;

  /**
   * Sets up the test dependencies.
   *
   * @throws Exception if there is a setup issue
   */
  @BeforeEach
  public void setUp() throws Exception {
    fissClaim = FissClaim.newBuilder();
    mcsClaim = McsClaim.newBuilder();
  }

  /**
   * Verifies that if no value is set for the status (defaults to 0), {@link
   * EnumStringExtractor.Status#NoValue} is returned.
   */
  @Test
  public void noValue() {
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
        fissStatusExtractor.getEnumString(fissClaim.build()));

    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
        mcsStatusExtractor.getEnumString(mcsClaim.build()));
  }

  /**
   * Verifies that if an invalid value is set for the status (such as -1), {@link
   * EnumStringExtractor.Status#InvalidValue} is returned.
   */
  @Test
  public void invalidValue() {
    fissClaim.setCurrStatusEnumValue(-1);
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
        fissStatusExtractor.getEnumString(fissClaim.build()));

    mcsClaim.setIdrStatusCodeEnumValue(-1);
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
        mcsStatusExtractor.getEnumString(mcsClaim.build()));
  }

  /**
   * Verifies that if an unrecognized status value is set for the status, {@link
   * EnumStringExtractor.Status#UnsupportedValue} is returned.
   */
  @Test
  public void unrecognizedValue() {
    fissClaim.setCurrStatusUnrecognized("boo!");
    assertEquals(
        new EnumStringExtractor.Result("boo!"),
        fissStatusExtractor.getEnumString(fissClaim.build()));

    mcsClaim.setIdrStatusCodeUnrecognized("boo!");
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.UnsupportedValue, "boo!"),
        mcsStatusExtractor.getEnumString(mcsClaim.build()));
  }

  /** Verifies that if a valid value is set for the status, the corresponding enum is returned. */
  @Test
  public void goodValue() {
    fissClaim.setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP);
    assertEquals(
        new EnumStringExtractor.Result("T"), fissStatusExtractor.getEnumString(fissClaim.build()));

    mcsClaim.setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A);
    assertEquals(
        new EnumStringExtractor.Result("A"), mcsStatusExtractor.getEnumString(mcsClaim.build()));
  }

  /**
   * Verifies that if an unrecognized status value is set, {@link
   * EnumStringExtractor.Status#UnsupportedValue} is returned.
   */
  @Test
  public void unsupportedEnumValue() {
    mcsClaim.setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_NOT_USED);
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.UnsupportedValue, "6"),
        mcsStatusExtractor.getEnumString(mcsClaim.build()));
  }
}
