package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
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
import org.junit.Test;

public class OutpatientClaimTransformerV2Test {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.OutpatientClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_INPATIENT} {@link
   * InpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    OutpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());

    ExplanationOfBenefit eob = OutpatientClaimTransformerV2.transform(new MetricRegistry(), claim);

    assertMatches(claim, eob);
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link InpatientClaim}.
   *
   * @param claim the {@link OutpatientClaim} that the {@link ExplanationOfBenefit} was generated
   *     from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     InpatientClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(OutpatientClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtilsV2.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // TODO: Double check the assumed value
    Assert.assertEquals(5, eob.getDiagnosis().size());

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
