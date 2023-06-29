package gov.cms.bfd.server.war.r4.providers;

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

/** Tests the {@link ClaimTypeV2} enum logic. */
public final class ClaimTypeV2Test {
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

    HospiceClaim hospiceClaim = new HospiceClaim();
    hospiceClaim.setDateFrom(start);
    hospiceClaim.setDateThrough(end);

    PartDEvent partDEvent = new PartDEvent();
    partDEvent.setPrescriptionFillDate(end);

    InpatientClaim inpatientClaim = new InpatientClaim();
    inpatientClaim.setDateFrom(start);
    inpatientClaim.setDateThrough(end);

    OutpatientClaim outpatientClaim = new OutpatientClaim();
    outpatientClaim.setDateFrom(start);
    outpatientClaim.setDateThrough(end);

    SNFClaim snfClaim = new SNFClaim();
    snfClaim.setDateFrom(start);
    snfClaim.setDateThrough(end);

    HHAClaim hhaClaim = new HHAClaim();
    hhaClaim.setDateFrom(start);
    hhaClaim.setDateThrough(end);

    ImmutableMap.Builder<ClaimTypeV2, Object> builder = ImmutableMap.builder();
    builder.put(ClaimTypeV2.CARRIER, carrierClaim);
    builder.put(ClaimTypeV2.DME, dmeClaim);
    builder.put(ClaimTypeV2.PDE, partDEvent);
    builder.put(ClaimTypeV2.INPATIENT, inpatientClaim);
    builder.put(ClaimTypeV2.OUTPATIENT, outpatientClaim);
    builder.put(ClaimTypeV2.HOSPICE, hospiceClaim);
    builder.put(ClaimTypeV2.SNF, snfClaim);
    builder.put(ClaimTypeV2.HHA, hhaClaim);

    Map<ClaimTypeV2, Object> claimTypeToClaim = builder.build();

    // Verify that we're testing all of the ClaimTypes that are defined
    EnumSet.allOf(ClaimTypeV2.class).stream()
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
   * Verifies that providing a EnumSet of {@link ClaimTypeV2} and a bit mask integer denoting claim
   * types that have data, teh results is a filtered EnumSet.
   */
  @Test
  public void verifyEnumSetFromListOfClaimTypesAndDatabaseBitmaskOfData() {
    EnumSet allClaimSet = EnumSet.allOf(ClaimTypeV2.class);

    // resultant set only includes claim types that have data.
    int testVal = QueryUtils.V_DME_HAS_DATA | QueryUtils.V_SNF_HAS_DATA | QueryUtils.V_HHA_HAS_DATA;
    EnumSet availSet = ClaimTypeV2.fetchClaimsAvailability(allClaimSet, testVal);

    assertTrue(availSet.contains(ClaimTypeV2.HHA));
    assertTrue(availSet.contains(ClaimTypeV2.SNF));
    assertTrue(availSet.contains(ClaimTypeV2.DME));
    assertFalse(availSet.contains(ClaimTypeV2.INPATIENT));

    // check efficacy of EnumSet filter vs. bit mask of data.
    EnumSet someClaimSet = EnumSet.noneOf(ClaimTypeV2.class);
    someClaimSet.add(ClaimTypeV2.CARRIER);
    someClaimSet.add(ClaimTypeV2.PDE);

    availSet = ClaimTypeV2.fetchClaimsAvailability(someClaimSet, testVal);
    assertFalse(availSet.contains(ClaimTypeV2.HHA));
    assertFalse(availSet.contains(ClaimTypeV2.SNF));
    assertFalse(availSet.contains(ClaimTypeV2.DME));
    assertFalse(availSet.contains(ClaimTypeV2.CARRIER));
    // adjust data bit mask and try again
    testVal = testVal | QueryUtils.V_CARRIER_HAS_DATA;
    assertTrue(availSet.contains(ClaimTypeV2.CARRIER));
  }
}
