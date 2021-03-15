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
import org.junit.Ignore;
import org.junit.Test;

public class SNFClaimTransformerV2Test {

  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public SNFClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    return parsedRecords.stream()
        .filter(r -> r instanceof SNFClaim)
        .map(r -> (SNFClaim) r)
        .findFirst()
        .get();
  }
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SNFClaimTransformer#transform(Object)} works as expected
   * when run against the {@link StaticRifResource#SAMPLE_A_SNF} {@link SNFClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    SNFClaim claim = generateClaim();

    assertMatches(claim, SNFClaimTransformerV2.transform(new MetricRegistry(), claim));
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  /**
   * Serializes the EOB and prints to the command line
   *
   * @throws FHIRException
   */
  @Ignore
  @Test
  public void serializeSampleARecord() throws FHIRException {
    ExplanationOfBenefit eob =
        SNFClaimTransformerV2.transform(new MetricRegistry(), generateClaim());
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

    // TODO: finish tests based off V1

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
