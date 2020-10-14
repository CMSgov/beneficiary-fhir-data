package gov.cms.bfd.server.war.stu3.providers;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.SNFClaim;
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

    CarrierClaim carrierClaim = new CarrierClaim();
    carrierClaim.setDateFrom(start);
    carrierClaim.setDateThrough(end);

    DMEClaim dmeClaim = new DMEClaim();
    dmeClaim.setDateFrom(start);
    dmeClaim.setDateThrough(end);

    HHAClaim hhaClaim = new HHAClaim();
    hhaClaim.setDateFrom(start);
    hhaClaim.setDateThrough(end);

    HospiceClaim hospiceClaim = new HospiceClaim();
    hospiceClaim.setDateFrom(start);
    hospiceClaim.setDateThrough(end);

    InpatientClaim inpatientClaim = new InpatientClaim();
    inpatientClaim.setDateFrom(start);
    inpatientClaim.setDateThrough(end);

    OutpatientClaim outpatientClaim = new OutpatientClaim();
    outpatientClaim.setDateFrom(start);
    outpatientClaim.setDateThrough(end);

    PartDEvent partDEvent = new PartDEvent();
    partDEvent.setPrescriptionFillDate(end);

    SNFClaim snfClaim = new SNFClaim();
    snfClaim.setDateFrom(start);
    snfClaim.setDateThrough(end);

    ImmutableMap.Builder<ClaimType, Object> builder = ImmutableMap.builder();
    builder.put(ClaimType.CARRIER, carrierClaim);
    builder.put(ClaimType.DME, dmeClaim);
    builder.put(ClaimType.HHA, hhaClaim);
    builder.put(ClaimType.HOSPICE, hospiceClaim);
    builder.put(ClaimType.INPATIENT, inpatientClaim);
    builder.put(ClaimType.OUTPATIENT, outpatientClaim);
    builder.put(ClaimType.PDE, partDEvent);
    builder.put(ClaimType.SNF, snfClaim);

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
