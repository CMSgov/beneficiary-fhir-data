package gov.cms.bfd.server.war.r4.providers;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public final class ClaimTypeTest {
  /**
   * Verifies that our service end date function is working as expected. Since we are type casting
   * our claim object, we need to verify that every ClaimType is tested.
   */
  @Test
  public void verifyServiceEndAttributeFunc() {
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(10);

    PartDEvent partDEvent = new PartDEvent();
    partDEvent.setPrescriptionFillDate(end);

    InpatientClaim inpatientClaim = new InpatientClaim();
    inpatientClaim.setDateFrom(start);
    inpatientClaim.setDateThrough(end);

    OutpatientClaim outpatientClaim = new OutpatientClaim();
    outpatientClaim.setDateFrom(start);
    outpatientClaim.setDateThrough(end);

    ImmutableMap.Builder<ClaimType, Object> builder = ImmutableMap.builder();
    builder.put(ClaimType.PDE, partDEvent);
    builder.put(ClaimType.INPATIENT, inpatientClaim);
    builder.put(ClaimType.OUTPATIENT, outpatientClaim);

    Map<ClaimType, Object> claimTypeToClaim = builder.build();

    // Verify that we're testing all of the ClaimTypes that are defined
    EnumSet.allOf(ClaimType.class).stream()
        .forEach(
            claimType ->
                Assert.assertTrue(
                    String.format("ClaimType %s not tested", claimType.name()),
                    claimTypeToClaim.containsKey(claimType)));

    claimTypeToClaim
        .entrySet()
        .forEach(
            entry ->
                Assert.assertEquals(
                    String.format(
                        "Claim type %s does not match expectations", entry.getKey().name()),
                    end,
                    entry.getKey().getServiceEndAttributeFunction().apply(entry.getValue())));
  }
}
