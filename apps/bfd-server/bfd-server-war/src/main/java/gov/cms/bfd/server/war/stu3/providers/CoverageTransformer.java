package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Contract;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
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
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));

      transformMedicaidDualEligibility(coverage, beneficiary);

      transformEntitlementBuyInIndicators(coverage, beneficiary);
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
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));

      transformMedicaidDualEligibility(coverage, beneficiary);

      transformEntitlementBuyInIndicators(coverage, beneficiary);
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

    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      transformPartCContractNumber(coverage, beneficiary);

      transformPartCPbpNumber(coverage, beneficiary);

      transformPartCPlanType(coverage, beneficiary);

      transformHmoIndicator(coverage, beneficiary);

      coverage.addExtension(
          TransformerUtils.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));

      transformMedicaidDualEligibility(coverage, beneficiary);
    }

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
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      transformPartDContractNumber(coverage, beneficiary);

      // Beneficiary Monthly Data
      beneficiary
          .getBeneficiaryMonthlys()
          .forEach(
              beneMonthly -> {
                int month = beneMonthly.getYearMonth().getMonthValue();
                String yearMonth =
                    String.format(
                        "%s-%s",
                        String.valueOf(beneMonthly.getYearMonth().getYear()),
                        String.valueOf(month));

                Map<Integer, CcwCodebookVariable> mapOfMonth =
                    new HashMap<Integer, CcwCodebookVariable>() {
                      {
                        put(1, CcwCodebookVariable.PTDCNTRCT01);
                        put(2, CcwCodebookVariable.PTDCNTRCT02);
                        put(3, CcwCodebookVariable.PTDCNTRCT03);
                        put(4, CcwCodebookVariable.PTDCNTRCT04);
                        put(5, CcwCodebookVariable.PTDCNTRCT05);
                        put(6, CcwCodebookVariable.PTDCNTRCT06);
                        put(7, CcwCodebookVariable.PTDCNTRCT07);
                        put(8, CcwCodebookVariable.PTDCNTRCT08);
                        put(9, CcwCodebookVariable.PTDCNTRCT09);
                        put(10, CcwCodebookVariable.PTDCNTRCT10);
                        put(11, CcwCodebookVariable.PTDCNTRCT11);
                        put(12, CcwCodebookVariable.PTDCNTRCT12);
                      }
                    };

                if (mapOfMonth.containsKey(month)) {
                  if (!beneMonthly.getPartDContractNumberId().isPresent()
                      || beneMonthly.getPartDContractNumberId().get().isEmpty()) {
                    beneMonthly.setPartDContractNumberId(Optional.of("0"));
                  }

                  coverage.addExtension(
                      TransformerUtils.createExtensionCoding(
                          coverage,
                          mapOfMonth.get(month),
                          yearMonth,
                          beneMonthly.getPartDContractNumberId()));
                }
              });

      transformPartDPbpNumber(coverage, beneficiary);

      transformPartDSegmentNumber(coverage, beneficiary);

      transformPartDLowIncomeCostShareGroup(coverage, beneficiary);

      transformPartDRetireeDrugSubsidy(coverage, beneficiary);

      coverage.addExtension(
          TransformerUtils.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));

      transformMedicaidDualEligibility(coverage, beneficiary);
    }
    TransformerUtils.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  private static Coverage transformHmoIndicator(Coverage coverage, Beneficiary beneficiary) {
    // Monthly Medicare Advantage (MA) enrollment indicators:
    if (beneficiary.getHmoIndicatorJanInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_01, beneficiary.getHmoIndicatorJanInd()));
    }
    if (beneficiary.getHmoIndicatorFebInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_02, beneficiary.getHmoIndicatorFebInd()));
    }
    if (beneficiary.getHmoIndicatorMarInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_03, beneficiary.getHmoIndicatorMarInd()));
    }
    if (beneficiary.getHmoIndicatorAprInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_04, beneficiary.getHmoIndicatorAprInd()));
    }
    if (beneficiary.getHmoIndicatorMayInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_05, beneficiary.getHmoIndicatorMayInd()));
    }
    if (beneficiary.getHmoIndicatorJunInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_06, beneficiary.getHmoIndicatorJunInd()));
    }
    if (beneficiary.getHmoIndicatorJulInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_07, beneficiary.getHmoIndicatorJulInd()));
    }
    if (beneficiary.getHmoIndicatorAugInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_08, beneficiary.getHmoIndicatorAugInd()));
    }
    if (beneficiary.getHmoIndicatorSeptInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_09, beneficiary.getHmoIndicatorSeptInd()));
    }
    if (beneficiary.getHmoIndicatorOctInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_10, beneficiary.getHmoIndicatorOctInd()));
    }
    if (beneficiary.getHmoIndicatorNovInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_11, beneficiary.getHmoIndicatorNovInd()));
    }
    if (beneficiary.getHmoIndicatorDecInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.HMO_IND_12, beneficiary.getHmoIndicatorDecInd()));
    }

    return coverage;
  }

  private static Coverage transformPartCPbpNumber(Coverage coverage, Beneficiary beneficiary) {
    // PBP
    if (beneficiary.getPartCPbpNumberJanId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_01, beneficiary.getPartCPbpNumberJanId()));
    }
    if (beneficiary.getPartCPbpNumberFebId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_02, beneficiary.getPartCPbpNumberFebId()));
    }
    if (beneficiary.getPartCPbpNumberMarId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_03, beneficiary.getPartCPbpNumberMarId()));
    }
    if (beneficiary.getPartCPbpNumberAprId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_04, beneficiary.getPartCPbpNumberAprId()));
    }
    if (beneficiary.getPartCPbpNumberMayId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_05, beneficiary.getPartCPbpNumberMayId()));
    }
    if (beneficiary.getPartCPbpNumberJunId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_06, beneficiary.getPartCPbpNumberJunId()));
    }
    if (beneficiary.getPartCPbpNumberJulId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_07, beneficiary.getPartCPbpNumberJulId()));
    }
    if (beneficiary.getPartCPbpNumberAugId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_08, beneficiary.getPartCPbpNumberAugId()));
    }
    if (beneficiary.getPartCPbpNumberSeptId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_09, beneficiary.getPartCPbpNumberSeptId()));
    }
    if (beneficiary.getPartCPbpNumberOctId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_10, beneficiary.getPartCPbpNumberOctId()));
    }
    if (beneficiary.getPartCPbpNumberNovId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_11, beneficiary.getPartCPbpNumberNovId()));
    }
    if (beneficiary.getPartCPbpNumberDecId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTC_PBP_ID_12, beneficiary.getPartCPbpNumberDecId()));
    }
    return coverage;
  }

  private static Coverage transformPartCPlanType(Coverage coverage, Beneficiary beneficiary) {
    // Plan Type
    if (beneficiary.getPartCPlanTypeJanCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_01,
              beneficiary.getPartCPlanTypeJanCode()));
    }
    if (beneficiary.getPartCPlanTypeFebCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_02,
              beneficiary.getPartCPlanTypeFebCode()));
    }
    if (beneficiary.getPartCPlanTypeMarCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_03,
              beneficiary.getPartCPlanTypeMarCode()));
    }
    if (beneficiary.getPartCPlanTypeAprCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_04,
              beneficiary.getPartCPlanTypeAprCode()));
    }
    if (beneficiary.getPartCPlanTypeMayCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_05,
              beneficiary.getPartCPlanTypeMayCode()));
    }
    if (beneficiary.getPartCPlanTypeJunCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_06,
              beneficiary.getPartCPlanTypeJunCode()));
    }
    if (beneficiary.getPartCPlanTypeJulCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_07,
              beneficiary.getPartCPlanTypeJulCode()));
    }
    if (beneficiary.getPartCPlanTypeAugCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_08,
              beneficiary.getPartCPlanTypeAugCode()));
    }
    if (beneficiary.getPartCPlanTypeSeptCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_09,
              beneficiary.getPartCPlanTypeSeptCode()));
    }
    if (beneficiary.getPartCPlanTypeOctCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_10,
              beneficiary.getPartCPlanTypeOctCode()));
    }
    if (beneficiary.getPartCPlanTypeNovCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_11,
              beneficiary.getPartCPlanTypeNovCode()));
    }
    if (beneficiary.getPartCPlanTypeDecCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_PLAN_TYPE_CD_12,
              beneficiary.getPartCPlanTypeDecCode()));
    }
    return coverage;
  }

  private static Coverage transformPartCContractNumber(Coverage coverage, Beneficiary beneficiary) {
    // Contract Number
    if (beneficiary.getPartCContractNumberJanId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_01,
              beneficiary.getPartCContractNumberJanId()));
    }
    if (beneficiary.getPartCContractNumberFebId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_02,
              beneficiary.getPartCContractNumberFebId()));
    }
    if (beneficiary.getPartCContractNumberMarId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_03,
              beneficiary.getPartCContractNumberMarId()));
    }
    if (beneficiary.getPartCContractNumberAprId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_04,
              beneficiary.getPartCContractNumberAprId()));
    }
    if (beneficiary.getPartCContractNumberMayId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_05,
              beneficiary.getPartCContractNumberMayId()));
    }
    if (beneficiary.getPartCContractNumberJunId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_06,
              beneficiary.getPartCContractNumberJunId()));
    }
    if (beneficiary.getPartCContractNumberJulId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_07,
              beneficiary.getPartCContractNumberJulId()));
    }
    if (beneficiary.getPartCContractNumberAugId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_08,
              beneficiary.getPartCContractNumberAugId()));
    }
    if (beneficiary.getPartCContractNumberSeptId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_09,
              beneficiary.getPartCContractNumberSeptId()));
    }
    if (beneficiary.getPartCContractNumberOctId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_10,
              beneficiary.getPartCContractNumberOctId()));
    }
    if (beneficiary.getPartCContractNumberNovId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_11,
              beneficiary.getPartCContractNumberNovId()));
    }
    if (beneficiary.getPartCContractNumberDecId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTC_CNTRCT_ID_12,
              beneficiary.getPartCContractNumberDecId()));
    }
    return coverage;
  }

  /**
   * @param coverage the {@link Coverage} to generate
   * @param beneficiary the {@link Beneficiary} to generate Coverage for
   * @return {@link Coverage} resource for the
   */
  private static Coverage transformEntitlementBuyInIndicators(
      Coverage coverage, Beneficiary beneficiary) {

    // Medicare Entitlement Buy In Indicator
    if (beneficiary.getEntitlementBuyInJanInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN01, beneficiary.getEntitlementBuyInJanInd()));
    }
    if (beneficiary.getEntitlementBuyInFebInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN02, beneficiary.getEntitlementBuyInFebInd()));
    }
    if (beneficiary.getEntitlementBuyInMarInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN03, beneficiary.getEntitlementBuyInMarInd()));
    }
    if (beneficiary.getEntitlementBuyInAprInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd()));
    }
    if (beneficiary.getEntitlementBuyInMayInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN05, beneficiary.getEntitlementBuyInMayInd()));
    }
    if (beneficiary.getEntitlementBuyInJunInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN06, beneficiary.getEntitlementBuyInJunInd()));
    }
    if (beneficiary.getEntitlementBuyInJulInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN07, beneficiary.getEntitlementBuyInJulInd()));
    }
    if (beneficiary.getEntitlementBuyInAugInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN08, beneficiary.getEntitlementBuyInAugInd()));
    }
    if (beneficiary.getEntitlementBuyInSeptInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN09, beneficiary.getEntitlementBuyInSeptInd()));
    }
    if (beneficiary.getEntitlementBuyInOctInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN10, beneficiary.getEntitlementBuyInOctInd()));
    }
    if (beneficiary.getEntitlementBuyInNovInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN11, beneficiary.getEntitlementBuyInNovInd()));
    }
    if (beneficiary.getEntitlementBuyInDecInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.BUYIN12, beneficiary.getEntitlementBuyInDecInd()));
    }
    return coverage;
  }

  private static Coverage transformMedicaidDualEligibility(
      Coverage coverage, Beneficiary beneficiary) {
    // Monthly Medicare-Medicaid dual eligibility codes
    if (beneficiary.getMedicaidDualEligibilityJanCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_01,
              beneficiary.getMedicaidDualEligibilityJanCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_02,
              beneficiary.getMedicaidDualEligibilityFebCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMarCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_03,
              beneficiary.getMedicaidDualEligibilityMarCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAprCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_04,
              beneficiary.getMedicaidDualEligibilityAprCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMayCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_05,
              beneficiary.getMedicaidDualEligibilityMayCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJunCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_06,
              beneficiary.getMedicaidDualEligibilityJunCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJulCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_07,
              beneficiary.getMedicaidDualEligibilityJulCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAugCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_08,
              beneficiary.getMedicaidDualEligibilityAugCode()));
    }
    if (beneficiary.getMedicaidDualEligibilitySeptCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_09,
              beneficiary.getMedicaidDualEligibilitySeptCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityOctCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_10,
              beneficiary.getMedicaidDualEligibilityOctCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityNovCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_11,
              beneficiary.getMedicaidDualEligibilityNovCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityDecCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.DUAL_12,
              beneficiary.getMedicaidDualEligibilityDecCode()));
    }

    return coverage;
  }

  private static Coverage transformPartDContractNumber(Coverage coverage, Beneficiary beneficiary) {
    if (beneficiary.getPartDContractNumberJanId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT01,
              beneficiary.getPartDContractNumberJanId()));
    }
    if (beneficiary.getPartDContractNumberFebId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT02,
              beneficiary.getPartDContractNumberFebId()));
    }
    if (beneficiary.getPartDContractNumberMarId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT03,
              beneficiary.getPartDContractNumberMarId()));
    }
    if (beneficiary.getPartDContractNumberAprId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT04,
              beneficiary.getPartDContractNumberAprId()));
    }
    if (beneficiary.getPartDContractNumberMayId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT05,
              beneficiary.getPartDContractNumberMayId()));
    }
    if (beneficiary.getPartDContractNumberJunId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT06,
              beneficiary.getPartDContractNumberJunId()));
    }
    if (beneficiary.getPartDContractNumberJulId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT07,
              beneficiary.getPartDContractNumberJulId()));
    }
    if (beneficiary.getPartDContractNumberAugId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT08,
              beneficiary.getPartDContractNumberAugId()));
    }
    if (beneficiary.getPartDContractNumberSeptId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT09,
              beneficiary.getPartDContractNumberSeptId()));
    }
    if (beneficiary.getPartDContractNumberOctId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT10,
              beneficiary.getPartDContractNumberOctId()));
    }
    if (beneficiary.getPartDContractNumberNovId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT11,
              beneficiary.getPartDContractNumberNovId()));
    }
    if (beneficiary.getPartDContractNumberDecId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.PTDCNTRCT12,
              beneficiary.getPartDContractNumberDecId()));
    }
    return coverage;
  }

  private static Coverage transformPartDPbpNumber(Coverage coverage, Beneficiary beneficiary) {
    // PBP
    if (beneficiary.getPartDPbpNumberJanId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID01, beneficiary.getPartDPbpNumberJanId()));
    }
    if (beneficiary.getPartDPbpNumberFebId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID02, beneficiary.getPartDPbpNumberFebId()));
    }
    if (beneficiary.getPartDPbpNumberMarId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID03, beneficiary.getPartDPbpNumberMarId()));
    }
    if (beneficiary.getPartDPbpNumberAprId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID04, beneficiary.getPartDPbpNumberAprId()));
    }
    if (beneficiary.getPartDPbpNumberMayId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID05, beneficiary.getPartDPbpNumberMayId()));
    }
    if (beneficiary.getPartDPbpNumberJunId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID06, beneficiary.getPartDPbpNumberJunId()));
    }
    if (beneficiary.getPartDPbpNumberJulId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID07, beneficiary.getPartDPbpNumberJulId()));
    }
    if (beneficiary.getPartDPbpNumberAugId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID08, beneficiary.getPartDPbpNumberAugId()));
    }
    if (beneficiary.getPartDPbpNumberSeptId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID09, beneficiary.getPartDPbpNumberSeptId()));
    }
    if (beneficiary.getPartDPbpNumberOctId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID10, beneficiary.getPartDPbpNumberOctId()));
    }
    if (beneficiary.getPartDPbpNumberNovId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID11, beneficiary.getPartDPbpNumberNovId()));
    }
    if (beneficiary.getPartDPbpNumberDecId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.PTDPBPID12, beneficiary.getPartDPbpNumberDecId()));
    }
    return coverage;
  }

  private static Coverage transformPartDSegmentNumber(Coverage coverage, Beneficiary beneficiary) {
    // Segment Number
    if (beneficiary.getPartDSegmentNumberJanId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID01, beneficiary.getPartDSegmentNumberJanId()));
    }
    if (beneficiary.getPartDSegmentNumberFebId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID02, beneficiary.getPartDSegmentNumberFebId()));
    }
    if (beneficiary.getPartDSegmentNumberMarId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID03, beneficiary.getPartDSegmentNumberMarId()));
    }
    if (beneficiary.getPartDSegmentNumberAprId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID04, beneficiary.getPartDSegmentNumberAprId()));
    }
    if (beneficiary.getPartDSegmentNumberMayId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID05, beneficiary.getPartDSegmentNumberMayId()));
    }
    if (beneficiary.getPartDSegmentNumberJunId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID06, beneficiary.getPartDSegmentNumberJunId()));
    }
    if (beneficiary.getPartDSegmentNumberJulId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID07, beneficiary.getPartDSegmentNumberJulId()));
    }
    if (beneficiary.getPartDSegmentNumberAugId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID08, beneficiary.getPartDSegmentNumberAugId()));
    }
    if (beneficiary.getPartDSegmentNumberSeptId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID09, beneficiary.getPartDSegmentNumberSeptId()));
    }
    if (beneficiary.getPartDSegmentNumberOctId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID10, beneficiary.getPartDSegmentNumberOctId()));
    }
    if (beneficiary.getPartDSegmentNumberNovId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID11, beneficiary.getPartDSegmentNumberNovId()));
    }
    if (beneficiary.getPartDSegmentNumberDecId().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage, CcwCodebookVariable.SGMTID12, beneficiary.getPartDSegmentNumberDecId()));
    }
    return coverage;
  }

  private static Coverage transformPartDLowIncomeCostShareGroup(
      Coverage coverage, Beneficiary beneficiary) {
    // Monthly cost sharing group
    if (beneficiary.getPartDLowIncomeCostShareGroupJanCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR01,
              beneficiary.getPartDLowIncomeCostShareGroupJanCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupFebCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR02,
              beneficiary.getPartDLowIncomeCostShareGroupFebCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupMarCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR03,
              beneficiary.getPartDLowIncomeCostShareGroupMarCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupAprCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR04,
              beneficiary.getPartDLowIncomeCostShareGroupAprCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupMayCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR05,
              beneficiary.getPartDLowIncomeCostShareGroupMayCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupJunCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR06,
              beneficiary.getPartDLowIncomeCostShareGroupJunCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupJulCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR07,
              beneficiary.getPartDLowIncomeCostShareGroupJulCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupAugCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR08,
              beneficiary.getPartDLowIncomeCostShareGroupAugCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupSeptCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR09,
              beneficiary.getPartDLowIncomeCostShareGroupSeptCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupOctCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR10,
              beneficiary.getPartDLowIncomeCostShareGroupOctCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupNovCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR11,
              beneficiary.getPartDLowIncomeCostShareGroupNovCode()));
    }
    if (beneficiary.getPartDLowIncomeCostShareGroupDecCode().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.CSTSHR12,
              beneficiary.getPartDLowIncomeCostShareGroupDecCode()));
    }
    return coverage;
  }

  private static Coverage transformPartDRetireeDrugSubsidy(
      Coverage coverage, Beneficiary beneficiary) {
    // Monthly Part D Retiree Drug Subsidy Indicators
    if (beneficiary.getPartDRetireeDrugSubsidyJanInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND01,
              beneficiary.getPartDRetireeDrugSubsidyJanInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyFebInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND02,
              beneficiary.getPartDRetireeDrugSubsidyFebInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyMarInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND03,
              beneficiary.getPartDRetireeDrugSubsidyMarInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyAprInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND04,
              beneficiary.getPartDRetireeDrugSubsidyAprInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyMayInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND05,
              beneficiary.getPartDRetireeDrugSubsidyMayInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyJunInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND06,
              beneficiary.getPartDRetireeDrugSubsidyJunInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyJulInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND07,
              beneficiary.getPartDRetireeDrugSubsidyJulInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyAugInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND08,
              beneficiary.getPartDRetireeDrugSubsidyAugInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidySeptInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND09,
              beneficiary.getPartDRetireeDrugSubsidySeptInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyOctInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND10,
              beneficiary.getPartDRetireeDrugSubsidyOctInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyNovInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND11,
              beneficiary.getPartDRetireeDrugSubsidyNovInd()));
    }
    if (beneficiary.getPartDRetireeDrugSubsidyDecInd().isPresent()) {
      coverage.addExtension(
          TransformerUtils.createExtensionCoding(
              coverage,
              CcwCodebookVariable.RDSIND12,
              beneficiary.getPartDRetireeDrugSubsidyDecInd()));
    }
    return coverage;
  }
}
