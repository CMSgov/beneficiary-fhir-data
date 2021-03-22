package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public final class InpatientClaimTransformerV2Test {
  /**
   * Generates the Claim object to be used in multiple tests
   *
   * @return
   * @throws FHIRException
   */
  public InpatientClaim generateClaim() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());

    return claim;
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.InpatientClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_INPATIENT} {@link
   * InpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    InpatientClaim claim = generateClaim();

    assertMatches(claim, InpatientClaimTransformerV2.transform(new MetricRegistry(), claim));
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
        InpatientClaimTransformerV2.transform(new MetricRegistry(), generateClaim());
    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link InpatientClaim}.
   *
   * @param claim the {@link InpatientClaim} that the {@link ExplanationOfBenefit} was generated
   *     from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     InpatientClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(InpatientClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimTypeV2.INPATIENT,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // Test the common field provider NPI number is set as expected in the EOB
    // TransformerTestUtilsV2.assertProviderNPI(eob, claim.getOrganizationNpi());

    if (claim.getPatientStatusCd().isPresent()) {
      TransformerTestUtilsV2.assertInfoWithCodeEquals(
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claim.getPatientStatusCd(),
          eob);
    }

    // Test that the expected number of diagnoses are mapped
    Assert.assertEquals(8, eob.getDiagnosis().size());
  }
}
