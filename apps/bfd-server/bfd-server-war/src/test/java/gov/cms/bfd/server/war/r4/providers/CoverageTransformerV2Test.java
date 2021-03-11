package gov.cms.bfd.server.war.r4.providers;

import static org.hamcrest.CoreMatchers.*;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Coverage.CoverageStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2}. */
public final class CoverageTransformerV2Test {

  private static final FhirContext fhirContext = FhirContext.forR4();
  private static Beneficiary beneficiary = null;

  @Before
  public void setup() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // Pull out the base Beneficiary record and fix its HICN and MBI-HASH fields.
    beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    beneficiary.setLastUpdated(new Date());
  }

  @After
  public void tearDown() {
    beneficiary = null;
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCoveragePartA() throws FHIRException {
    Coverage coverage =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    // System.out.println(fhirContext.newJsonParser().encodeResourceToString(partACoverage));
    assertPartAMatches(beneficiary, coverage);

    // Test with null lastUpdated
    beneficiary.setLastUpdated(null);
    Coverage partACoverageNullLastUpdated =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    assertPartAMatches(beneficiary, partACoverageNullLastUpdated);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_B_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCoveragePartB() throws FHIRException {
    Coverage coverage =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_B, beneficiary);
    // System.out.println(fhirContext.newJsonParser().encodeResourceToString(partBCoverage));
    assertPartBMatches(beneficiary, coverage);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_C_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCoveragePartC() throws FHIRException {
    Coverage coverage =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_C, beneficiary);
    // System.out.println(fhirContext.newJsonParser().encodeResourceToString(partCCoverage));
    assertPartCMatches(beneficiary, coverage);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_D_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCoveragePartD() throws FHIRException {
    Coverage coverage =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_D, beneficiary);
    // System.out.println(fhirContext.newJsonParser().encodeResourceToString(partCCoverage));
    assertPartDMatches(beneficiary, coverage);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformerV2#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void testCoveragePartA_NoDate() throws FHIRException {
    // Test with null lastUpdated
    beneficiary.setLastUpdated(null);
    Coverage coverage =
        CoverageTransformerV2.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    assertPartAMatches(beneficiary, coverage);
  }

  /**
   * Verifies that the specified {@link
   * gov.cms.bfd.server.war.stu3.providers.MedicareSegment#PART_A} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param coverage the {@link Coverage} to verify
   */
  static void assertPartAMatches(Beneficiary beneficiary, Coverage coverage) {
    TransformerTestUtilsV2.assertNoEncodedOptionals(coverage);

    Assert.assertNotNull(coverage);
    Assert.assertEquals(
        TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_A, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());

    TransformerTestUtilsV2.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);
    if (beneficiary.getMedicareCoverageStartDate().isPresent())
      TransformerTestUtilsV2.assertPeriodEquals(
          beneficiary.getMedicareCoverageStartDate(), Optional.empty(), coverage.getPeriod());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    if (beneficiary.getEntitlementCodeOriginal().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.OREC, beneficiary.getEntitlementCodeOriginal(), coverage);
    if (beneficiary.getEntitlementCodeCurrent().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.CREC, beneficiary.getEntitlementCodeCurrent(), coverage);
    if (beneficiary.getEndStageRenalDiseaseCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.ESRD_IND, beneficiary.getEndStageRenalDiseaseCode(), coverage);
    if (beneficiary.getPartATerminationCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.A_TRM_CD, beneficiary.getPartATerminationCode(), coverage);
    if (beneficiary.getEntitlementBuyInAprInd().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd(), coverage);

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent())
      TransformerTestUtilsV2.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
  }

  /**
   * Verifies that the specified {@link
   * gov.cms.bfd.server.war.stu3.providers.MedicareSegment#PART_B} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param coverage the {@link Coverage} to verify
   */
  static void assertPartBMatches(Beneficiary beneficiary, Coverage coverage) {
    Assert.assertNotNull(coverage);

    Assert.assertEquals(
        TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_B, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());

    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtilsV2.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getMedicareCoverageStartDate().isPresent())
      TransformerTestUtilsV2.assertPeriodEquals(
          beneficiary.getMedicareCoverageStartDate(), Optional.empty(), coverage.getPeriod());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    if (beneficiary.getPartBTerminationCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.B_TRM_CD, beneficiary.getPartBTerminationCode(), coverage);

    if (beneficiary.getEntitlementBuyInAprInd().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd(), coverage);

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent())
      TransformerTestUtilsV2.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
  }

  /**
   * Verifies that the specified {@link
   * gov.cms.bfd.server.war.stu3.providers.MedicareSegment#PART_C} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param coverage the {@link Coverage} to verify
   */
  static void assertPartCMatches(Beneficiary beneficiary, Coverage coverage) {
    Assert.assertNotNull(coverage);

    Assert.assertEquals(
        TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_C, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());

    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtilsV2.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getPartCContractNumberAugId().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.PTC_CNTRCT_ID_08,
          beneficiary.getPartCContractNumberAugId(),
          coverage);
    if (beneficiary.getPartCPbpNumberAugId().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.PTC_PBP_ID_08, beneficiary.getPartCPbpNumberAugId(), coverage);
    if (beneficiary.getPartCPlanTypeAugCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.PTC_PLAN_TYPE_CD_08, beneficiary.getPartCPlanTypeAugCode(), coverage);

    if (beneficiary.getHmoIndicatorFebInd().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.HMO_IND_02, beneficiary.getHmoIndicatorFebInd(), coverage);

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent())
      TransformerTestUtilsV2.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
  }

  /**
   * Verifies that the specified {@link
   * gov.cms.bfd.server.war.stu3.providers.MedicareSegment#PART_D} {@link Coverage} "looks like" it
   * should, if it were produced from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} that the specified {@link Coverage} should match
   * @param coverage the {@link Coverage} to verify
   */
  static void assertPartDMatches(Beneficiary beneficiary, Coverage coverage) {
    Assert.assertNotNull(coverage);

    Assert.assertEquals(
        TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_D, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtilsV2.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getPartDContractNumberAugId().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.PTDCNTRCT08, beneficiary.getPartDContractNumberAugId(), coverage);
    if (beneficiary.getPartDPbpNumberAugId().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.PTDPBPID08, beneficiary.getPartDPbpNumberAugId(), coverage);
    if (beneficiary.getPartDSegmentNumberAugId().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.SGMTID08, beneficiary.getPartDSegmentNumberAugId(), coverage);

    if (beneficiary.getPartDLowIncomeCostShareGroupFebCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.CSTSHR02,
          beneficiary.getPartDLowIncomeCostShareGroupFebCode(),
          coverage);
    if (beneficiary.getPartDRetireeDrugSubsidyJanInd().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.RDSIND01, beneficiary.getPartDRetireeDrugSubsidyJanInd(), coverage);

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
      TransformerTestUtilsV2.assertExtensionCodingEquals(
          CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent())
      TransformerTestUtilsV2.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
  }
}
