package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.Enrollment;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.CoverageTransformer}. */
public final class CoverageTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformer#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
   * {@link Beneficiary}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    beneficiary.setLastUpdated(new Date());

    Coverage partACoverage =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    assertPartAMatches(beneficiary, partACoverage);

    Coverage partBCoverage =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_B, beneficiary);
    assertPartBMatches(beneficiary, partBCoverage);

    Coverage partCCoverage =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_C, beneficiary);
    assertPartCMatches(beneficiary, partCCoverage);

    Coverage partDCoverage =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_D, beneficiary);
    assertPartDMatches(beneficiary, partDCoverage);

    // Test with null lastUpdated
    beneficiary.setLastUpdated(null);
    Coverage partACoverageNullLastUpdated =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    assertPartAMatches(beneficiary, partACoverageNullLastUpdated);
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
    TransformerTestUtils.assertNoEncodedOptionals(coverage);

    Assert.assertNotNull(coverage);

    Assert.assertEquals(
        TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    Assert.assertEquals(
        TransformerConstants.COVERAGE_PLAN_PART_A, coverage.getGrouping().getSubPlan());
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getMedicareCoverageStartDate().isPresent())
      TransformerTestUtils.assertPeriodEquals(
          beneficiary.getMedicareCoverageStartDate(), Optional.empty(), coverage.getPeriod());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    if (beneficiary.getEntitlementCodeOriginal().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.OREC, beneficiary.getEntitlementCodeOriginal(), coverage);
    if (beneficiary.getEntitlementCodeCurrent().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.CREC, beneficiary.getEntitlementCodeCurrent(), coverage);
    if (beneficiary.getEndStageRenalDiseaseCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.ESRD_IND, beneficiary.getEndStageRenalDiseaseCode(), coverage);
    if (beneficiary.getPartATerminationCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.A_TRM_CD, beneficiary.getPartATerminationCode(), coverage);

    if (beneficiary.getEntitlementBuyInAprInd().isPresent()) {
      Optional<Enrollment> aprEnrollment =
          beneficiary.getEnrollments().stream()
              .filter(f -> f.getYearMonth().getMonthValue() == 4)
              .findFirst();
      if (aprEnrollment.isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.BUYIN04,
            aprEnrollment.get().getEntitlementBuyInInd().get(),
            coverage);
    }

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      Optional<Enrollment> febEnrollment =
          beneficiary.getEnrollments().stream()
              .filter(f -> f.getYearMonth().getMonthValue() == 2)
              .findFirst();
      if (febEnrollment.isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02,
            febEnrollment.get().getMedicaidDualEligibilityCode().get(),
            coverage);
    }
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
        TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    Assert.assertEquals(
        TransformerConstants.COVERAGE_PLAN_PART_B, coverage.getGrouping().getSubPlan());
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getMedicareCoverageStartDate().isPresent())
      TransformerTestUtils.assertPeriodEquals(
          beneficiary.getMedicareCoverageStartDate(), Optional.empty(), coverage.getPeriod());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    if (beneficiary.getPartBTerminationCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.B_TRM_CD, beneficiary.getPartBTerminationCode(), coverage);

    if (beneficiary.getEntitlementBuyInAprInd().isPresent()) {
      Optional<Enrollment> aprEnrollment =
          beneficiary.getEnrollments().stream()
              .filter(f -> f.getYearMonth().getMonthValue() == 4)
              .findFirst();
      if (aprEnrollment.isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.BUYIN04,
            aprEnrollment.get().getEntitlementBuyInInd().get(),
            coverage);
    }

    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      Optional<Enrollment> febEnrollment =
          beneficiary.getEnrollments().stream()
              .filter(f -> f.getYearMonth().getMonthValue() == 2)
              .findFirst();
      if (febEnrollment.isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02,
            febEnrollment.get().getMedicaidDualEligibilityCode().get(),
            coverage);
    }
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
        TransformerUtils.buildCoverageId(MedicareSegment.PART_C, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    Assert.assertEquals(
        TransformerConstants.COVERAGE_PLAN_PART_C, coverage.getGrouping().getSubPlan());
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    Optional<Enrollment> augEnrollment =
        beneficiary.getEnrollments().stream()
            .filter(f -> f.getYearMonth().getMonthValue() == 8)
            .findFirst();

    if (augEnrollment.isPresent()) {

      if (beneficiary.getPartCContractNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_CNTRCT_ID_08,
            augEnrollment.get().getPartCContractNumberId().get(),
            coverage);
      if (beneficiary.getPartCPbpNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_PBP_ID_08,
            augEnrollment.get().getPartCPbpNumberId().get(),
            coverage);
      if (beneficiary.getPartCPlanTypeAugCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_PLAN_TYPE_CD_08,
            augEnrollment.get().getPartCPlanTypeCode().get(),
            coverage);
    }

    Optional<Enrollment> febEnrollment =
        beneficiary.getEnrollments().stream()
            .filter(f -> f.getYearMonth().getMonthValue() == 2)
            .findFirst();

    if (febEnrollment.isPresent()) {
      if (beneficiary.getHmoIndicatorFebInd().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.HMO_IND_02,
            febEnrollment.get().getHmoIndicatorInd().get(),
            coverage);

      if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02,
            febEnrollment.get().getMedicaidDualEligibilityCode().get(),
            coverage);
    }
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
        TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    Assert.assertEquals(
        TransformerConstants.COVERAGE_PLAN_PART_D, coverage.getGrouping().getSubPlan());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    Optional<Enrollment> augEnrollment =
        beneficiary.getEnrollments().stream()
            .filter(f -> f.getYearMonth().getMonthValue() == 8)
            .findFirst();

    if (augEnrollment.isPresent()) {
      if (beneficiary.getPartDContractNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTDCNTRCT08,
            augEnrollment.get().getPartDContractNumberId().get(),
            coverage);
      if (beneficiary.getPartDPbpNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTDPBPID08,
            augEnrollment.get().getPartDPbpNumberId().get(),
            coverage);
      if (beneficiary.getPartDSegmentNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.SGMTID08,
            augEnrollment.get().getPartDSegmentNumberId().get(),
            coverage);
    }

    Optional<Enrollment> febEnrollment =
        beneficiary.getEnrollments().stream()
            .filter(f -> f.getYearMonth().getMonthValue() == 2)
            .findFirst();

    if (febEnrollment.isPresent()) {

      if (beneficiary.getPartDLowIncomeCostShareGroupFebCode().isPresent()) {
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.CSTSHR02,
            febEnrollment.get().getPartDLowIncomeCostShareGroupCode().get(),
            coverage);
      }

      if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02,
            febEnrollment.get().getMedicaidDualEligibilityCode().get(),
            coverage);
      }
    }
    Optional<Enrollment> janEnrollment =
        beneficiary.getEnrollments().stream()
            .filter(f -> f.getYearMonth().getMonthValue() == 1)
            .findFirst();

    if (janEnrollment.isPresent()) {
      if (beneficiary.getPartDRetireeDrugSubsidyJanInd().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.RDSIND01,
            janEnrollment.get().getPartDRetireeDrugSubsidyInd().get(),
            coverage);
    }
  }
}
