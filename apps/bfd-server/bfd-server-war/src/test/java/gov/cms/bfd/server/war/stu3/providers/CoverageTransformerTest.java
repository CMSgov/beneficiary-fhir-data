package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Test;

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
    beneficiary.setLastUpdated(Instant.now());

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
    beneficiary.setLastUpdated(Optional.empty());
    Coverage partACoverageNullLastUpdated =
        CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_A, beneficiary);
    assertPartAMatches(beneficiary, partACoverageNullLastUpdated);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageTransformer#transform(MedicareSegment,
   * Beneficiary)} works as expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
   * {@link Beneficiary} with a null reference year.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithNullReferenceYear() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    beneficiary.setLastUpdated(Instant.now());
    beneficiary.setBeneEnrollmentReferenceYear(Optional.empty());

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

    // Test with empty lastUpdated
    beneficiary.setLastUpdated(Optional.empty());
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

    assertNotNull(coverage);

    assertEquals(
        TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    assertEquals(TransformerConstants.COVERAGE_PLAN_PART_A, coverage.getGrouping().getSubPlan());
    assertEquals(CoverageStatus.CANCELLED, coverage.getStatus());
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

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      if (beneficiary.getEntitlementBuyInAprInd().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd(), coverage);

      if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

      TransformerTestUtils.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
    } else {
      checkForNoYearlyData(beneficiary, coverage);
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
    assertNotNull(coverage);

    assertEquals(
        TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    assertEquals(TransformerConstants.COVERAGE_PLAN_PART_B, coverage.getGrouping().getSubPlan());
    assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
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

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      if (beneficiary.getEntitlementBuyInAprInd().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd(), coverage);

      if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

      TransformerTestUtils.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
    } else {
      checkForNoYearlyData(beneficiary, coverage);
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
    assertNotNull(coverage);

    assertEquals(
        TransformerUtils.buildCoverageId(MedicareSegment.PART_C, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    assertEquals(TransformerConstants.COVERAGE_PLAN_PART_C, coverage.getGrouping().getSubPlan());
    assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      if (beneficiary.getPartCContractNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_CNTRCT_ID_08,
            beneficiary.getPartCContractNumberAugId(),
            coverage);
      if (beneficiary.getPartCPbpNumberAugId().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_PBP_ID_08, beneficiary.getPartCPbpNumberAugId(), coverage);
      if (beneficiary.getPartCPlanTypeAugCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.PTC_PLAN_TYPE_CD_08,
            beneficiary.getPartCPlanTypeAugCode(),
            coverage);

      if (beneficiary.getHmoIndicatorFebInd().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.HMO_IND_02, beneficiary.getHmoIndicatorFebInd(), coverage);

      if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent())
        TransformerTestUtils.assertExtensionCodingEquals(
            CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);

      TransformerTestUtils.assertExtensionDateYearEquals(
          CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);
    } else {
      checkForNoYearlyData(beneficiary, coverage);
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
    assertNotNull(coverage);

    assertEquals(
        TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary).getIdPart(),
        coverage.getIdElement().getIdPart());
    assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
    assertEquals(TransformerConstants.COVERAGE_PLAN_PART_D, coverage.getGrouping().getSubPlan());

    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
      TransformerTestUtils.assertExtensionCodingEquals(
          CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(), coverage);
    assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
    TransformerTestUtils.assertLastUpdatedEquals(beneficiary.getLastUpdated(), coverage);

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {

    } else {
      checkForNoYearlyData(beneficiary, coverage);
    }
  }

  static void checkForNoYearlyData(Beneficiary beneficiary, Coverage coverage) {
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_01, beneficiary.getPartCContractNumberJanId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_02, beneficiary.getPartCContractNumberFebId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_03, beneficiary.getPartCContractNumberMarId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_04, beneficiary.getPartCContractNumberAprId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_05, beneficiary.getPartCContractNumberMayId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_06, beneficiary.getPartCContractNumberJunId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_07, beneficiary.getPartCContractNumberJulId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_08, beneficiary.getPartCContractNumberAugId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_09, beneficiary.getPartCContractNumberSeptId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_10, beneficiary.getPartCContractNumberOctId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_11, beneficiary.getPartCContractNumberNovId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_CNTRCT_ID_12, beneficiary.getPartCContractNumberDecId(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_01, beneficiary.getPartCPbpNumberJanId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_02, beneficiary.getPartCPbpNumberFebId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_03, beneficiary.getPartCPbpNumberMarId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_04, beneficiary.getPartCPbpNumberAprId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_05, beneficiary.getPartCPbpNumberMayId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_06, beneficiary.getPartCPbpNumberJunId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_07, beneficiary.getPartCPbpNumberJulId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_08, beneficiary.getPartCPbpNumberAugId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_09, beneficiary.getPartCPbpNumberSeptId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_10, beneficiary.getPartCPbpNumberOctId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_11, beneficiary.getPartCPbpNumberNovId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PBP_ID_12, beneficiary.getPartCPbpNumberDecId(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_01, beneficiary.getPartCPlanTypeJanCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_02, beneficiary.getPartCPlanTypeFebCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_03, beneficiary.getPartCPlanTypeMarCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_04, beneficiary.getPartCPlanTypeAprCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_05, beneficiary.getPartCPlanTypeMayCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_06, beneficiary.getPartCPlanTypeJunCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_07, beneficiary.getPartCPlanTypeJulCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_08, beneficiary.getPartCPlanTypeAugCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_09, beneficiary.getPartCPlanTypeSeptCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_10, beneficiary.getPartCPlanTypeOctCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_11, beneficiary.getPartCPlanTypeNovCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTC_PLAN_TYPE_CD_12, beneficiary.getPartCPlanTypeDecCode(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT01, beneficiary.getPartDContractNumberJanId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT02, beneficiary.getPartDContractNumberFebId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT03, beneficiary.getPartDContractNumberMarId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT04, beneficiary.getPartDContractNumberAprId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT05, beneficiary.getPartDContractNumberMayId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT06, beneficiary.getPartDContractNumberJunId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT07, beneficiary.getPartDContractNumberJulId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT08, beneficiary.getPartDContractNumberAugId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT09, beneficiary.getPartDContractNumberSeptId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT10, beneficiary.getPartDContractNumberOctId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT11, beneficiary.getPartDContractNumberNovId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDCNTRCT12, beneficiary.getPartDContractNumberDecId(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID01, beneficiary.getPartDPbpNumberJanId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID02, beneficiary.getPartDPbpNumberFebId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID03, beneficiary.getPartDPbpNumberMarId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID04, beneficiary.getPartDPbpNumberAprId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID05, beneficiary.getPartDPbpNumberMayId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID06, beneficiary.getPartDPbpNumberJunId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID07, beneficiary.getPartDPbpNumberJulId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID08, beneficiary.getPartDPbpNumberAugId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID09, beneficiary.getPartDPbpNumberSeptId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID10, beneficiary.getPartDPbpNumberOctId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID11, beneficiary.getPartDPbpNumberNovId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.PTDPBPID12, beneficiary.getPartDPbpNumberDecId(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID01, beneficiary.getPartDSegmentNumberJanId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID02, beneficiary.getPartDSegmentNumberFebId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID03, beneficiary.getPartDSegmentNumberMarId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID04, beneficiary.getPartDSegmentNumberAprId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID05, beneficiary.getPartDSegmentNumberMayId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID06, beneficiary.getPartDSegmentNumberJunId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID07, beneficiary.getPartDSegmentNumberJulId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID08, beneficiary.getPartDSegmentNumberAugId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID09, beneficiary.getPartDSegmentNumberSeptId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID10, beneficiary.getPartDSegmentNumberOctId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID11, beneficiary.getPartDSegmentNumberNovId(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.SGMTID12, beneficiary.getPartDSegmentNumberDecId(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR01,
        beneficiary.getPartDLowIncomeCostShareGroupJanCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR02,
        beneficiary.getPartDLowIncomeCostShareGroupFebCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR03,
        beneficiary.getPartDLowIncomeCostShareGroupMarCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR04,
        beneficiary.getPartDLowIncomeCostShareGroupAprCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR05,
        beneficiary.getPartDLowIncomeCostShareGroupMayCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR06,
        beneficiary.getPartDLowIncomeCostShareGroupJunCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR07,
        beneficiary.getPartDLowIncomeCostShareGroupJulCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR08,
        beneficiary.getPartDLowIncomeCostShareGroupAugCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR09,
        beneficiary.getPartDLowIncomeCostShareGroupSeptCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR10,
        beneficiary.getPartDLowIncomeCostShareGroupOctCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR11,
        beneficiary.getPartDLowIncomeCostShareGroupNovCode(),
        coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.CSTSHR12,
        beneficiary.getPartDLowIncomeCostShareGroupDecCode(),
        coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND01, beneficiary.getPartDRetireeDrugSubsidyJanInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND02, beneficiary.getPartDRetireeDrugSubsidyFebInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND03, beneficiary.getPartDRetireeDrugSubsidyMarInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND04, beneficiary.getPartDRetireeDrugSubsidyAprInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND05, beneficiary.getPartDRetireeDrugSubsidyMayInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND06, beneficiary.getPartDRetireeDrugSubsidyJunInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND07, beneficiary.getPartDRetireeDrugSubsidyJulInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND08, beneficiary.getPartDRetireeDrugSubsidyAugInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND09, beneficiary.getPartDRetireeDrugSubsidySeptInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND10, beneficiary.getPartDRetireeDrugSubsidyOctInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND11, beneficiary.getPartDRetireeDrugSubsidyNovInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.RDSIND12, beneficiary.getPartDRetireeDrugSubsidyDecInd(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_01, beneficiary.getHmoIndicatorJanInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_02, beneficiary.getHmoIndicatorFebInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_03, beneficiary.getHmoIndicatorMarInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_04, beneficiary.getHmoIndicatorAprInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_05, beneficiary.getHmoIndicatorMayInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_06, beneficiary.getHmoIndicatorJunInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_07, beneficiary.getHmoIndicatorJulInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_08, beneficiary.getHmoIndicatorAugInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_09, beneficiary.getHmoIndicatorSeptInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_10, beneficiary.getHmoIndicatorOctInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_11, beneficiary.getHmoIndicatorNovInd(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.HMO_IND_12, beneficiary.getHmoIndicatorDecInd(), coverage);

    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_01, beneficiary.getMedicaidDualEligibilityJanCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_03, beneficiary.getMedicaidDualEligibilityMarCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_04, beneficiary.getMedicaidDualEligibilityAprCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_05, beneficiary.getMedicaidDualEligibilityMayCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_06, beneficiary.getMedicaidDualEligibilityJunCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_07, beneficiary.getMedicaidDualEligibilityJulCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_08, beneficiary.getMedicaidDualEligibilityAugCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_09, beneficiary.getMedicaidDualEligibilitySeptCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_10, beneficiary.getMedicaidDualEligibilityOctCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_11, beneficiary.getMedicaidDualEligibilityNovCode(), coverage);
    TransformerTestUtils.assertExtensionCodingDoesNotExist(
        CcwCodebookVariable.DUAL_12, beneficiary.getMedicaidDualEligibilityDecCode(), coverage);
  }
}
