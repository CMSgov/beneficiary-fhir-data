package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link ClaimType}. */
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
                assertTrue(
                    claimTypeToClaim.containsKey(claimType),
                    String.format("ClaimType %s not tested", claimType.name())));

    claimTypeToClaim
        .entrySet()
        .forEach(
            entry ->
                assertEquals(
                    end,
                    entry.getKey().getServiceEndAttributeFunction().apply(entry.getValue()),
                    String.format(
                        "Claim type %s does not match expectations", entry.getKey().name())));
  }

  /**
   * Verifies that providing a EnumSet of {@link ClaimType} and a bit mask integer denoting claim
   * types that have data, teh results is a filtered EnumSet.
   */
  @Test
  public void verifyEnumSetFromListOfClaimTypesAndDatabaseBitmaskOfData() {
    EnumSet allClaimSet = EnumSet.allOf(ClaimType.class);

    // resultant set only includes claim types that have data.
    int testVal = QueryUtils.V_DME_HAS_DATA | QueryUtils.V_SNF_HAS_DATA | QueryUtils.V_HHA_HAS_DATA;
    EnumSet availSet = ClaimType.fetchClaimsAvailability(allClaimSet, testVal);

    assertTrue(availSet.contains(ClaimType.HHA));
    assertTrue(availSet.contains(ClaimType.SNF));
    assertTrue(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.INPATIENT));

    // check efficacy of EnumSet filter vs. bit mask of data.
    EnumSet someClaimSet = EnumSet.noneOf(ClaimType.class);
    someClaimSet.add(ClaimType.CARRIER);
    someClaimSet.add(ClaimType.PDE);

    availSet = ClaimType.fetchClaimsAvailability(someClaimSet, testVal);
    assertFalse(availSet.contains(ClaimType.HHA));
    assertFalse(availSet.contains(ClaimType.SNF));
    assertFalse(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.CARRIER));
    // adjust data bit mask and try again
    testVal = testVal | QueryUtils.V_CARRIER_HAS_DATA;
    availSet = ClaimType.fetchClaimsAvailability(someClaimSet, testVal);
    assertTrue(availSet.contains(ClaimType.CARRIER));
  }
}
