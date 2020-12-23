package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.Enrollment;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Contract;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.instance.model.api.IBaseResource;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Coverage} resources. */
final class CoverageTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param medicareSegment the {@link MedicareSegment} to generate a {@link Coverage} resource for
   * @param beneficiary the {@link Beneficiary} to generate a {@link Coverage} resource for
   * @return the {@link Coverage} resource that was generated
   */
  @Trace
  public static Coverage transform(
      MetricRegistry metricRegistry, MedicareSegment medicareSegment, Beneficiary beneficiary) {
    Objects.requireNonNull(medicareSegment);

    if (medicareSegment == MedicareSegment.PART_A)
      return transformPartA(metricRegistry, beneficiary);
    else if (medicareSegment == MedicareSegment.PART_B)
      return transformPartB(metricRegistry, beneficiary);
    else if (medicareSegment == MedicareSegment.PART_C)
      return transformPartC(metricRegistry, beneficiary);
    else if (medicareSegment == MedicareSegment.PART_D)
      return transformPartD(metricRegistry, beneficiary);
    else throw new BadCodeMonkeyException();
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the CCW {@link Beneficiary} to generate the {@link Coverage}s for
   * @return the FHIR {@link Coverage} resources that can be generated from the specified {@link
   *     Beneficiary}
   */
  @Trace
  public static List<IBaseResource> transform(
      MetricRegistry metricRegistry, Beneficiary beneficiary) {
    return Arrays.stream(MedicareSegment.values())
        .map(s -> transform(metricRegistry, s, beneficiary))
        .collect(Collectors.toList());
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the {@link Beneficiary} to generate a {@link MedicareSegment#PART_A} {@link
   *     Coverage} resource for
   * @return {@link MedicareSegment#PART_A} {@link Coverage} resource for the specified {@link
   *     Beneficiary}
   */
  private static Coverage transformPartA(MetricRegistry metricRegistry, Beneficiary beneficiary) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    CoverageTransformer.class.getSimpleName(), "transform", "part_a"))
            .time();

    Objects.requireNonNull(beneficiary);

    Coverage coverage = new Coverage();
    coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary));
    if (beneficiary.getPartATerminationCode().isPresent()
        && beneficiary.getPartATerminationCode().get().equals('0'))
      coverage.setStatus(CoverageStatus.ACTIVE);
    else coverage.setStatus(CoverageStatus.CANCELLED);

    if (beneficiary.getMedicareCoverageStartDate().isPresent()) {
      TransformerUtils.setPeriodStart(
          coverage.getPeriod(), beneficiary.getMedicareCoverageStartDate().get());
    }

    // deh start
    coverage.addContract().setId("ptc-contract1");

    Contract newContract = new Contract();
    LocalDate localDate = LocalDate.now();
    newContract.setIdentifier(
        new Identifier().setSystem("part C System").setValue("contract 5555"));
    newContract.setApplies(
        (new Period()
            .setStart((TransformerUtils.convertToDate(localDate)), TemporalPrecisionEnum.DAY)));
    coverage.addContained(newContract);

    coverage.addContract(
        TransformerUtils.referenceCoverage("contract1 reference", MedicareSegment.PART_A));

    coverage
        .getGrouping()
        .setSubGroup(TransformerConstants.COVERAGE_PLAN)

        // deh end
        .setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_A);
    coverage.setType(
        TransformerUtils.createCodeableConcept(
            TransformerConstants.COVERAGE_PLAN, TransformerConstants.COVERAGE_PLAN_PART_A));
    coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode()));
    }
    if (beneficiary.getEntitlementCodeOriginal().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.OREC, beneficiary.getEntitlementCodeOriginal()));
    }
    if (beneficiary.getEntitlementCodeCurrent().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.CREC, beneficiary.getEntitlementCodeCurrent()));
    }
    if (beneficiary.getEndStageRenalDiseaseCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.ESRD_IND, beneficiary.getEndStageRenalDiseaseCode()));
    }
    if (beneficiary.getPartATerminationCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.A_TRM_CD, beneficiary.getPartATerminationCode()));
    }

    // The reference year of the enrollment data

    // The reference year of the enrollment data
    List<Enrollment> enrollments = beneficiary.getEnrollments();
    if (!enrollments.isEmpty()) {
      String referenceYear = String.valueOf(enrollments.get(0).getYearMonth().getYear());
      if (!Strings.isNullOrEmpty(referenceYear)) {
        coverage.addExtension(
            TransformerUtils.createExtensionDate(CcwCodebookVariable.RFRNC_YR, referenceYear));
      }

      // I dunno if i like this..
      List<Extension> medicaidDualEligibilities = new ArrayList<Extension>();
      List<Extension> buyInIndicators = new ArrayList<Extension>();
      beneficiary
          .getEnrollments()
          .forEach(
              enrollment -> {
                int month = enrollment.getYearMonth().getMonthValue();
                TransformerUtils.transformMedicaidDualEligibility(
                    month, enrollment, coverage, medicaidDualEligibilities);
                transformEntitlementBuyInIndicators(coverage, enrollment, month, buyInIndicators);
              });

      if (medicaidDualEligibilities.size() > 0 || buyInIndicators.size() > 0) {
        List<Extension> coverageExtensions = coverage.getExtension();

        if (medicaidDualEligibilities.size() > 0)
          coverageExtensions.addAll(medicaidDualEligibilities);

        if (buyInIndicators.size() > 0) coverageExtensions.addAll(buyInIndicators);

        coverage.setExtension(coverageExtensions);
      }
    }
    TransformerUtils.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the {@link Beneficiary} to generate a {@link MedicareSegment#PART_B} {@link
   *     Coverage} resource for
   * @return {@link MedicareSegment#PART_B} {@link Coverage} resource for the specified {@link
   *     Beneficiary}
   */
  private static Coverage transformPartB(MetricRegistry metricRegistry, Beneficiary beneficiary) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    CoverageTransformer.class.getSimpleName(), "transform", "part_b"))
            .time();

    Objects.requireNonNull(beneficiary);

    Coverage coverage = new Coverage();
    coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary));
    if (beneficiary.getPartBTerminationCode().isPresent()
        && beneficiary.getPartBTerminationCode().get().equals('0'))
      coverage.setStatus(CoverageStatus.ACTIVE);
    else coverage.setStatus(CoverageStatus.CANCELLED);

    if (beneficiary.getMedicareCoverageStartDate().isPresent()) {
      TransformerUtils.setPeriodStart(
          coverage.getPeriod(), beneficiary.getMedicareCoverageStartDate().get());
    }

    coverage
        .getGrouping()
        .setSubGroup(TransformerConstants.COVERAGE_PLAN)
        .setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_B);
    coverage.setType(
        TransformerUtils.createCodeableConcept(
            TransformerConstants.COVERAGE_PLAN, TransformerConstants.COVERAGE_PLAN_PART_B));
    coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode()));
    }
    if (beneficiary.getPartBTerminationCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.B_TRM_CD, beneficiary.getPartBTerminationCode()));
    }

    // The reference year of the enrollment data
    List<Enrollment> enrollments = beneficiary.getEnrollments();
    if (!enrollments.isEmpty()) {
      String referenceYear = String.valueOf(enrollments.get(0).getYearMonth().getYear());
      if (!Strings.isNullOrEmpty(referenceYear)) {
        coverage.addExtension(
            TransformerUtils.createExtensionDate(CcwCodebookVariable.RFRNC_YR, referenceYear));
      }

      // I dunno if i like this..
      List<Extension> medicaidDualEligibilities = new ArrayList<Extension>();
      List<Extension> buyInIndicators = new ArrayList<Extension>();
      beneficiary
          .getEnrollments()
          .forEach(
              enrollment -> {
                int month = enrollment.getYearMonth().getMonthValue();
                TransformerUtils.transformMedicaidDualEligibility(
                    month, enrollment, coverage, medicaidDualEligibilities);
                transformEntitlementBuyInIndicators(coverage, enrollment, month, buyInIndicators);
              });

      if (medicaidDualEligibilities.size() > 0 || buyInIndicators.size() > 0) {
        List<Extension> coverageExtensions = coverage.getExtension();

        if (medicaidDualEligibilities.size() > 0)
          coverageExtensions.addAll(medicaidDualEligibilities);

        if (buyInIndicators.size() > 0) coverageExtensions.addAll(buyInIndicators);

        coverage.setExtension(coverageExtensions);
      }
    }
    TransformerUtils.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the {@link Beneficiary} to generate a {@link MedicareSegment#PART_C} {@link
   *     Coverage} resource for
   * @return {@link MedicareSegment#PART_C} {@link Coverage} resource for the specified {@link
   *     Beneficiary}
   */
  private static Coverage transformPartC(MetricRegistry metricRegistry, Beneficiary beneficiary) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    CoverageTransformer.class.getSimpleName(), "transform", "part_c"))
            .time();

    Objects.requireNonNull(beneficiary);

    Coverage coverage = new Coverage();
    coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_C, beneficiary));
    coverage.setStatus(CoverageStatus.ACTIVE);

    coverage
        .getGrouping()
        .setSubGroup(TransformerConstants.COVERAGE_PLAN)
        .setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_C);
    coverage.setType(
        TransformerUtils.createCodeableConcept(
            TransformerConstants.COVERAGE_PLAN, TransformerConstants.COVERAGE_PLAN_PART_C));
    coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));

    // The reference year of the enrollment data
    List<Enrollment> enrollments = beneficiary.getEnrollments();
    if (!enrollments.isEmpty()) {
      String referenceYear = String.valueOf(enrollments.get(0).getYearMonth().getYear());
      if (!Strings.isNullOrEmpty(referenceYear)) {
        coverage.addExtension(
            TransformerUtils.createExtensionDate(CcwCodebookVariable.RFRNC_YR, referenceYear));
      }

      // I dunno if i like this..
      List<Extension> partCContractNumbers = new ArrayList<Extension>();
      List<Extension> partCPbpNumbers = new ArrayList<Extension>();
      List<Extension> partCPlanTypeCodes = new ArrayList<Extension>();
      List<Extension> hmoIndicators = new ArrayList<Extension>();
      List<Extension> medicaidDualEligibilities = new ArrayList<Extension>();
      beneficiary
          .getEnrollments()
          .forEach(
              enrollment -> {
                int month = enrollment.getYearMonth().getMonthValue();
                transformPartCContractNumber(coverage, enrollment, month, partCContractNumbers);
                transformPartCPbpNumbers(coverage, enrollment, month, partCPbpNumbers);
                transformPartCPlanTypeCode(coverage, enrollment, month, partCPlanTypeCodes);
                transformHmoIndicators(coverage, enrollment, month, hmoIndicators);
                TransformerUtils.transformMedicaidDualEligibility(
                    month, enrollment, coverage, medicaidDualEligibilities);
              });

      if (partCContractNumbers.size() > 0
          || partCPbpNumbers.size() > 0
          || partCPlanTypeCodes.size() > 0
          || hmoIndicators.size() > 0
          || medicaidDualEligibilities.size() > 0) {
        List<Extension> coverageExtensions = coverage.getExtension();

        if (partCContractNumbers.size() > 0) coverageExtensions.addAll(partCContractNumbers);

        if (partCPbpNumbers.size() > 0) coverageExtensions.addAll(partCPbpNumbers);

        if (partCPlanTypeCodes.size() > 0) coverageExtensions.addAll(partCPlanTypeCodes);

        if (hmoIndicators.size() > 0) coverageExtensions.addAll(hmoIndicators);

        if (medicaidDualEligibilities.size() > 0)
          coverageExtensions.addAll(medicaidDualEligibilities);

        coverage.setExtension(coverageExtensions);
      }
    }

    TransformerUtils.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the {@link Beneficiary} to generate a {@link MedicareSegment#PART_D} {@link
   *     Coverage} resource for
   * @return {@link MedicareSegment#PART_D} {@link Coverage} resource for the specified {@link
   *     Beneficiary}
   */
  private static Coverage transformPartD(MetricRegistry metricRegistry, Beneficiary beneficiary) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    CoverageTransformer.class.getSimpleName(), "transform", "part_d"))
            .time();

    Objects.requireNonNull(beneficiary);

    Coverage coverage = new Coverage();
    coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary));
    coverage
        .getGrouping()
        .setSubGroup(TransformerConstants.COVERAGE_PLAN)
        .setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_D);
    coverage.setType(
        TransformerUtils.createCodeableConcept(
            TransformerConstants.COVERAGE_PLAN, TransformerConstants.COVERAGE_PLAN_PART_D));
    coverage.setStatus(CoverageStatus.ACTIVE);
    coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
    if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode()));
    }

    // The reference year of the enrollment data
    List<Enrollment> enrollments = beneficiary.getEnrollments();
    if (!enrollments.isEmpty()) {
      String referenceYear = String.valueOf(enrollments.get(0).getYearMonth().getYear());
      if (!Strings.isNullOrEmpty(referenceYear)) {
        coverage.addExtension(
            TransformerUtils.createExtensionDate(CcwCodebookVariable.RFRNC_YR, referenceYear));
      }

      // I dunno if i like this..
      List<Extension> partDContractNumbers = new ArrayList<Extension>();
      List<Extension> partDPbpNumbers = new ArrayList<Extension>();
      List<Extension> partDSegmentNumbers = new ArrayList<Extension>();
      List<Extension> partDLowIncomeCostShareGroups = new ArrayList<Extension>();
      List<Extension> partDRetireeDrugSubsidy = new ArrayList<Extension>();
      List<Extension> medicaidDualEligibilities = new ArrayList<Extension>();
      beneficiary
          .getEnrollments()
          .forEach(
              enrollment -> {
                int month = enrollment.getYearMonth().getMonthValue();
                transformPartDContractNumbers(coverage, enrollment, month, partDContractNumbers);
                transformPartDPbpNumbers(coverage, enrollment, month, partDPbpNumbers);
                transformPartDSegmentNumbers(coverage, enrollment, month, partDSegmentNumbers);
                transformPartDLowIncomeCostShareGroups(
                    coverage, enrollment, month, partDLowIncomeCostShareGroups);
                transformPartDRetireeDrugSubsidy(
                    coverage, enrollment, month, partDRetireeDrugSubsidy);
                TransformerUtils.transformMedicaidDualEligibility(
                    month, enrollment, coverage, medicaidDualEligibilities);
              });

      if (partDContractNumbers.size() > 0
          || partDPbpNumbers.size() > 0
          || partDSegmentNumbers.size() > 0
          || partDLowIncomeCostShareGroups.size() > 0
          || partDRetireeDrugSubsidy.size() > 0
          || medicaidDualEligibilities.size() > 0) {
        List<Extension> coverageExtensions = coverage.getExtension();

        if (partDContractNumbers.size() > 0) coverageExtensions.addAll(partDContractNumbers);

        if (partDPbpNumbers.size() > 0) coverageExtensions.addAll(partDPbpNumbers);

        if (partDSegmentNumbers.size() > 0) coverageExtensions.addAll(partDSegmentNumbers);

        if (partDLowIncomeCostShareGroups.size() > 0)
          coverageExtensions.addAll(partDLowIncomeCostShareGroups);

        if (partDRetireeDrugSubsidy.size() > 0) coverageExtensions.addAll(partDRetireeDrugSubsidy);

        if (medicaidDualEligibilities.size() > 0)
          coverageExtensions.addAll(medicaidDualEligibilities);

        coverage.setExtension(coverageExtensions);
      }
    }
    TransformerUtils.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  /**
   * @param coverage the {@link Coverage} to generate
   * @param beneficiary the {@link Beneficiary} to generate Coverage for
   * @return {@link Coverage} resource for the
   */
  private static void transformEntitlementBuyInIndicators(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> buyInIndicators) {

    // Medicare Entitlement Buy In Indicator
    if (month == 1 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN01, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 2 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN02, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 3 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN03, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 4 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN04, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 5 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN05, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 6 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN06, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 7 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN07, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 8 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN08, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 9 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN09, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 10 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN10, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 11 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN11, enrollment.getEntitlementBuyInInd().get()));
    }
    if (month == 12 && enrollment.getEntitlementBuyInInd().isPresent()) {
      buyInIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN12, enrollment.getEntitlementBuyInInd().get()));
    }
  }

  private static void transformPartCContractNumber(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partCContractNumbers) {
    // Contract Number
    if (month == 1 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_01,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 2 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_02,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 3 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_03,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 4 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_04,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 5 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_05,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 6 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_06,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 7 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_07,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 8 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_08,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 9 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_09,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 10 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_10,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 11 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_11,
              enrollment.getPartCContractNumberId().get()));
    }
    if (month == 12 && enrollment.getPartCContractNumberId().isPresent()) {
      partCContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_12,
              enrollment.getPartCContractNumberId().get()));
    }
  }

  private static void transformPartCPbpNumbers(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partCPbpNumbers) {
    // PBP
    if (month == 1 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_01, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 2 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_02, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 3 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_03, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 4 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_04, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 5 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_05, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 6 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_06, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 7 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_07, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 8 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_08, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 9 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_09, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 10 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_10, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 11 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_11, enrollment.getPartCPbpNumberId().get()));
    }
    if (month == 12 && enrollment.getPartCPbpNumberId().isPresent()) {
      partCPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_12, enrollment.getPartCPbpNumberId().get()));
    }
  }

  private static void transformPartCPlanTypeCode(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partCPlanTypes) {
    // Plan Type
    if (month == 1 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_01,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 2 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_02,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 3 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_03,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 4 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_04,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 5 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_05,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 6 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_06,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 7 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_07,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 8 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_08,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 9 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_09,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 10 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_10,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 11 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_11,
              enrollment.getPartCPlanTypeCode().get()));
    }
    if (month == 12 && enrollment.getPartCPlanTypeCode().isPresent()) {
      partCPlanTypes.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_12,
              enrollment.getPartCPlanTypeCode().get()));
    }
  }

  private static void transformHmoIndicators(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> hmoIndicators) {
    // Monthly Medicare Advantage (MA) enrollment indicators:
    if (month == 1 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_01, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 2 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_02, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 3 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_03, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 4 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_04, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 5 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_05, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 6 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_06, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 7 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_07, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 8 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_08, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 9 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_09, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 10 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_10, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 11 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_11, enrollment.getHmoIndicatorInd().get()));
    }
    if (month == 12 && enrollment.getHmoIndicatorInd().isPresent()) {
      hmoIndicators.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_12, enrollment.getHmoIndicatorInd().get()));
    }
  }

  private static void transformPartDContractNumbers(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partDContractNumbers) {
    // Contract Number
    if (month == 1 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT01,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 2 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT02,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 3 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT03,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 4 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT04,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 5 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT05,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 6 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT06,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 7 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT07,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 8 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT08,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 9 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT09,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 10 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT10,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 11 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT11,
              enrollment.getPartDContractNumberId().get()));
    }
    if (month == 12 && enrollment.getPartDContractNumberId().isPresent()) {
      partDContractNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT12,
              enrollment.getPartDContractNumberId().get()));
    }
  }

  private static void transformPartDPbpNumbers(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partDPbpNumbers) {
    // PBP
    if (month == 1 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID01, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 2 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID02, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 3 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID03, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 4 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID04, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 5 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID05, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 6 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID06, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 7 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID07, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 8 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID08, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 9 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID09, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 10 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID10, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 11 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID11, enrollment.getPartDPbpNumberId().get()));
    }
    if (month == 12 && enrollment.getPartDPbpNumberId().isPresent()) {
      partDPbpNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID12, enrollment.getPartDPbpNumberId().get()));
    }
  }

  private static void transformPartDSegmentNumbers(
      Coverage coverage, Enrollment enrollment, int month, List<Extension> partDSegmentNumbers) {

    // Segment Number
    if (month == 1 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID01, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 2 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID02, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 3 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID03, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 4 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID04, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 5 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID05, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 6 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID06, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 7 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID07, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 8 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID08, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 9 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID09, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 10 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID10, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 11 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID11, enrollment.getPartDSegmentNumberId().get()));
    }
    if (month == 12 && enrollment.getPartDSegmentNumberId().isPresent()) {
      partDSegmentNumbers.add(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID12, enrollment.getPartDSegmentNumberId().get()));
    }
  }

  private static void transformPartDLowIncomeCostShareGroups(
      Coverage coverage,
      Enrollment enrollment,
      int month,
      List<Extension> partDLowIncomeCostShareGroups) {
    // Monthly cost sharing group
    if (month == 1 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR01,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 2 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR02,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 3 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR03,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 4 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR04,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 5 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR05,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 6 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR06,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 7 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR07,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 8 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR08,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 9 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR09,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 10 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR10,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 11 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR11,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
    if (month == 12 && enrollment.getPartDLowIncomeCostShareGroupCode().isPresent()) {
      partDLowIncomeCostShareGroups.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR12,
              enrollment.getPartDLowIncomeCostShareGroupCode().get()));
    }
  }

  private static void transformPartDRetireeDrugSubsidy(
      Coverage coverage,
      Enrollment enrollment,
      int month,
      List<Extension> partDRetireeDrugSubsidy) {
    // Monthly Part D Retiree Drug Subsidy Indicators
    if (month == 1 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND01,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 2 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND02,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 3 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND03,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 4 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND04,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 5 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND05,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 6 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND06,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 7 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND07,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 8 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND08,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 9 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND09,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 10 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND10,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 11 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND11,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
    if (month == 12 && enrollment.getPartDRetireeDrugSubsidyInd().isPresent()) {
      partDRetireeDrugSubsidy.add(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND12,
              enrollment.getPartDRetireeDrugSubsidyInd().get()));
    }
  }
}
