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
import org.junit.Test;

public class InpatientClaimTransformerV2Test {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.InpatientClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_INPATIENT} {@link
   * InpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(new Date());

    ExplanationOfBenefit eob = InpatientClaimTransformerV2.transform(new MetricRegistry(), claim);

    assertMatches(claim, eob);
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

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
        ClaimType.INPATIENT,
        claim.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // Test the common field provider NPI number is set as expected in the EOB
    TransformerTestUtilsV2.assertProviderNPI(eob, claim.getOrganizationNpi());

    if (claim.getPatientStatusCd().isPresent()) {
      TransformerTestUtilsV2.assertInfoWithCodeEquals(
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claim.getPatientStatusCd(),
          eob);
    }

    /*
    TODO: These are moved now

    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_PASS_THRU_PER_DIEM_AMT, claim.getPassThruPerDiemAmount(), eob);
    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT, claim.getProfessionalComponentCharge(), eob);
    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_TOT_PPS_CPTL_AMT, claim.getClaimTotalPPSCapitalAmount(), eob);
    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.IME_OP_CLM_VAL_AMT, claim.getIndirectMedicalEducationAmount(), eob);
    TransformerTestUtilsV2.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.DSH_OP_CLM_VAL_AMT, claim.getDisproportionateShareAmount(), eob);
        */

    // Test that the expected number of diagnoses are mapped
    Assert.assertEquals(9, eob.getDiagnosis().size());

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
