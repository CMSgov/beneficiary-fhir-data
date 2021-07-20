package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.assertEquals;

import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import org.junit.Before;
import org.junit.Test;

public class EnumStringExtractorTest {
  private final EnumStringExtractor<FissClaim, FissClaimStatus> currStatusExtractor =
      new EnumStringExtractor<>(
          FissClaim::hasCurrStatusEnum,
          FissClaim::getCurrStatusEnum,
          FissClaim::hasCurrStatusUnrecognized,
          FissClaim::getCurrStatusUnrecognized,
          FissClaimStatus.UNRECOGNIZED);
  private FissClaim.Builder claim;

  @Before
  public void setUp() throws Exception {
    claim = FissClaim.newBuilder();
  }

  @Test
  public void noValue() {
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.NoValue),
        currStatusExtractor.getEnumString(claim.build()));
  }

  @Test
  public void invalidValue() {
    claim.setCurrStatusEnumValue(-1);
    assertEquals(
        new EnumStringExtractor.Result(EnumStringExtractor.Status.InvalidValue),
        currStatusExtractor.getEnumString(claim.build()));
  }

  @Test
  public void unrecognizedValue() {
    claim.setCurrStatusUnrecognized("boo!");
    assertEquals(
        new EnumStringExtractor.Result("boo!"), currStatusExtractor.getEnumString(claim.build()));
  }

  @Test
  public void goodValue() {
    claim.setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_RTP);
    assertEquals(
        new EnumStringExtractor.Result("T"), currStatusExtractor.getEnumString(claim.build()));
  }
}
