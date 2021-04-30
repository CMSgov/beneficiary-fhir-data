package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SNFClaimTransformerV2Test {
  SNFClaim claim;
  ExplanationOfBenefit eob;

  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public SNFClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    SNFClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();

    return claim;
  }

  @Before
  public void before() {
    claim = generateClaim();
    eob = SNFClaimTransformerV2.transform(new MetricRegistry(), claim, Optional.empty());
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  @Test
  public void shouldSetID() {
    Assert.assertEquals("snf-" + claim.getClaimId(), eob.getId());
  }

  @Test
  public void shouldSetLastUpdated() {
    Assert.assertNotNull(eob.getMeta().getLastUpdated());
  }

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void serializeSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob =
        SNFClaimTransformerV2.transform(new MetricRegistry(), generateClaim(), Optional.of(false));
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link SNFClaim}.
   *
   * @param claim the {@link SNFClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     SNFClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(SNFClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimTypeV2.SNF,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());
  }
}
