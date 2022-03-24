package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.server.war.commons.CoverageClass;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.SubscriberPolicyRelationship;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Contract;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Coverage.CoverageStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Coverage} resources. */
final class CoverageTransformerV2 {
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
    Objects.requireNonNull(beneficiary);

    switch (medicareSegment) {
      case PART_A:
        return transformPartA(metricRegistry, beneficiary);
      case PART_B:
        return transformPartB(metricRegistry, beneficiary);
      case PART_C:
        return transformPartC(metricRegistry, beneficiary);
      case PART_D:
        return transformPartD(metricRegistry, beneficiary);
      default:
        throw new BadCodeMonkeyException();
    }
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
    Timer.Context timer = getTimerContext(metricRegistry, "part_a");
    Coverage coverage = new Coverage();

    coverage.getMeta().addProfile(ProfileConstants.C4BB_COVERAGE_URL);

    coverage.setId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_A, beneficiary));

    setCoverageStatus(coverage, beneficiary.getPartATerminationCode());

    beneficiary
        .getMedicareCoverageStartDate()
        .ifPresent(value -> TransformerUtilsV2.setPeriodStart(coverage.getPeriod(), value));

    // deh start
    coverage.addContract().setId("contract1");

    Contract newContract = new Contract();
    newContract
        .addIdentifier(new Identifier().setSystem("part C System").setValue("contract 5555"))
        .setApplies(
            (new Period()
                .setStart(
                    (TransformerUtilsV2.convertToDate(LocalDate.now())),
                    TemporalPrecisionEnum.DAY)));
    coverage.addContained(newContract);

    coverage.addContract(TransformerUtilsV2.referenceCoverage("contract1", MedicareSegment.PART_A));

    beneficiary.getMedicareBeneficiaryId().ifPresent(value -> coverage.setSubscriberId(value));

    setTypeAndIssuer(coverage);

    setCoverageRelationship(coverage, SubscriberPolicyRelationship.SELF);

    createCoverageClass(
        coverage, CoverageClass.GROUP, TransformerConstants.COVERAGE_PLAN, Optional.empty());

    createCoverageClass(
        coverage, CoverageClass.PLAN, TransformerConstants.COVERAGE_PLAN_PART_A, Optional.empty());

    coverage.setBeneficiary(TransformerUtilsV2.referencePatient(beneficiary));

    addCoverageExtension(
        coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.OREC, beneficiary.getEntitlementCodeOriginal());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.CREC, beneficiary.getEntitlementCodeCurrent());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.ESRD_IND, beneficiary.getEndStageRenalDiseaseCode());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.A_TRM_CD, beneficiary.getPartATerminationCode());
    addCoverageDecimalExtension(
        coverage, CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear());

    // Monthly Medicare-Medicaid dual eligibility codes
    transformEntitlementDualEligibility(coverage, beneficiary);

    // Medicare Entitlement Buy In Indicator
    transformEntitlementBuyInIndicators(coverage, beneficiary);

    // update Coverage.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(coverage, beneficiary.getLastUpdated());

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
    Timer.Context timer = getTimerContext(metricRegistry, "part_b");
    Coverage coverage = new Coverage();

    coverage.getMeta().addProfile(ProfileConstants.C4BB_COVERAGE_URL);
    coverage.setId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_B, beneficiary));
    setCoverageStatus(coverage, beneficiary.getPartBTerminationCode());

    TransformerUtilsV2.setPeriodStart(
        coverage.getPeriod(), beneficiary.getMedicareCoverageStartDate());

    beneficiary.getMedicareBeneficiaryId().ifPresent(value -> coverage.setSubscriberId(value));

    setTypeAndIssuer(coverage);

    setCoverageRelationship(coverage, SubscriberPolicyRelationship.SELF);

    createCoverageClass(
        coverage, CoverageClass.GROUP, TransformerConstants.COVERAGE_PLAN, Optional.empty());

    createCoverageClass(
        coverage, CoverageClass.PLAN, TransformerConstants.COVERAGE_PLAN_PART_B, Optional.empty());

    coverage.setBeneficiary(TransformerUtilsV2.referencePatient(beneficiary));

    addCoverageExtension(
        coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode());

    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.B_TRM_CD, beneficiary.getPartBTerminationCode());

    addCoverageDecimalExtension(
        coverage, CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear());

    // Monthly Medicare-Medicaid dual eligibility codes
    transformEntitlementDualEligibility(coverage, beneficiary);

    // Medicare Entitlement Buy In Indicator
    transformEntitlementBuyInIndicators(coverage, beneficiary);

    // update Coverage.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(coverage, beneficiary.getLastUpdated());

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
    Timer.Context timer = getTimerContext(metricRegistry, "part_c");
    Coverage coverage = new Coverage();

    coverage.getMeta().addProfile(ProfileConstants.C4BB_COVERAGE_URL);
    coverage.setId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_C, beneficiary));
    coverage.setStatus(CoverageStatus.ACTIVE);

    beneficiary.getMedicareBeneficiaryId().ifPresent(value -> coverage.setSubscriberId(value));

    setTypeAndIssuer(coverage);

    setCoverageRelationship(coverage, SubscriberPolicyRelationship.SELF);

    createCoverageClass(
        coverage, CoverageClass.GROUP, TransformerConstants.COVERAGE_PLAN, Optional.empty());

    createCoverageClass(
        coverage, CoverageClass.PLAN, TransformerConstants.COVERAGE_PLAN_PART_C, Optional.empty());

    coverage.setBeneficiary(TransformerUtilsV2.referencePatient(beneficiary));

    // Contract Number
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_01, beneficiary.getPartCContractNumberJanId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_02, beneficiary.getPartCContractNumberFebId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_03, beneficiary.getPartCContractNumberMarId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_04, beneficiary.getPartCContractNumberAprId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_05, beneficiary.getPartCContractNumberMayId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_06, beneficiary.getPartCContractNumberJunId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_07, beneficiary.getPartCContractNumberJulId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_08, beneficiary.getPartCContractNumberAugId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_09, beneficiary.getPartCContractNumberSeptId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_10, beneficiary.getPartCContractNumberOctId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_11, beneficiary.getPartCContractNumberNovId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_CNTRCT_ID_12, beneficiary.getPartCContractNumberDecId());

    // PBP
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_01, beneficiary.getPartCPbpNumberJanId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_02, beneficiary.getPartCPbpNumberFebId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_03, beneficiary.getPartCPbpNumberMarId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_04, beneficiary.getPartCPbpNumberAprId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_05, beneficiary.getPartCPbpNumberMayId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_06, beneficiary.getPartCPbpNumberJunId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_07, beneficiary.getPartCPbpNumberJulId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_08, beneficiary.getPartCPbpNumberAugId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_09, beneficiary.getPartCPbpNumberSeptId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_10, beneficiary.getPartCPbpNumberOctId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_11, beneficiary.getPartCPbpNumberNovId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PBP_ID_12, beneficiary.getPartCPbpNumberDecId());

    // Plan Type
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_01, beneficiary.getPartCPlanTypeJanCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_02, beneficiary.getPartCPlanTypeFebCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_03, beneficiary.getPartCPlanTypeMarCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_04, beneficiary.getPartCPlanTypeAprCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_05, beneficiary.getPartCPlanTypeMayCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_06, beneficiary.getPartCPlanTypeJunCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_07, beneficiary.getPartCPlanTypeJulCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_08, beneficiary.getPartCPlanTypeAugCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_09, beneficiary.getPartCPlanTypeSeptCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_10, beneficiary.getPartCPlanTypeOctCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_11, beneficiary.getPartCPlanTypeNovCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTC_PLAN_TYPE_CD_12, beneficiary.getPartCPlanTypeDecCode());

    // Monthly Medicare Advantage (MA) enrollment indicators:
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_01, beneficiary.getHmoIndicatorJanInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_02, beneficiary.getHmoIndicatorFebInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_03, beneficiary.getHmoIndicatorMarInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_04, beneficiary.getHmoIndicatorAprInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_05, beneficiary.getHmoIndicatorMayInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_06, beneficiary.getHmoIndicatorJunInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_07, beneficiary.getHmoIndicatorJulInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_08, beneficiary.getHmoIndicatorAugInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_09, beneficiary.getHmoIndicatorSeptInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_10, beneficiary.getHmoIndicatorOctInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_11, beneficiary.getHmoIndicatorNovInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.HMO_IND_12, beneficiary.getHmoIndicatorDecInd());

    // The reference year of the enrollment data
    addCoverageDecimalExtension(
        coverage, CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear());

    // Monthly Medicare-Medicaid dual eligibility codes
    transformEntitlementDualEligibility(coverage, beneficiary);

    // update Coverage.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(coverage, beneficiary.getLastUpdated());

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
    Timer.Context timer = getTimerContext(metricRegistry, "part_d");
    Coverage coverage = new Coverage();

    coverage.getMeta().addProfile(ProfileConstants.C4BB_COVERAGE_URL);
    coverage.setId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_D, beneficiary));

    beneficiary.getMedicareBeneficiaryId().ifPresent(value -> coverage.setSubscriberId(value));

    setTypeAndIssuer(coverage);

    setCoverageRelationship(coverage, SubscriberPolicyRelationship.SELF);

    createCoverageClass(
        coverage, CoverageClass.GROUP, TransformerConstants.COVERAGE_PLAN, Optional.empty());

    createCoverageClass(
        coverage, CoverageClass.PLAN, TransformerConstants.COVERAGE_PLAN_PART_D, Optional.empty());

    coverage.setStatus(CoverageStatus.ACTIVE);

    coverage.setBeneficiary(TransformerUtilsV2.referencePatient(beneficiary));

    addCoverageExtension(
        coverage, CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode());

    // Contract Number
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT01, beneficiary.getPartDContractNumberJanId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT02, beneficiary.getPartDContractNumberFebId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT03, beneficiary.getPartDContractNumberMarId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT04, beneficiary.getPartDContractNumberAprId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT05, beneficiary.getPartDContractNumberMayId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT06, beneficiary.getPartDContractNumberJunId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT07, beneficiary.getPartDContractNumberJulId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT08, beneficiary.getPartDContractNumberAugId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT09, beneficiary.getPartDContractNumberSeptId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT10, beneficiary.getPartDContractNumberOctId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT11, beneficiary.getPartDContractNumberNovId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDCNTRCT12, beneficiary.getPartDContractNumberDecId());

    // Beneficiary Monthly Data
    beneficiary
        .getBeneficiaryMonthlys()
        .forEach(
            beneMonthly -> {
              int month = beneMonthly.getYearMonth().getMonthValue();
              String yearMonth =
                  String.format(
                      "%s-%s",
                      String.valueOf(beneMonthly.getYearMonth().getYear()), String.valueOf(month));

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
                    TransformerUtilsV2.createExtensionCoding(
                        coverage,
                        mapOfMonth.get(month),
                        yearMonth,
                        beneMonthly.getPartDContractNumberId()));
              }
            });

    // PBP
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID01, beneficiary.getPartDPbpNumberJanId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID02, beneficiary.getPartDPbpNumberFebId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID03, beneficiary.getPartDPbpNumberMarId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID04, beneficiary.getPartDPbpNumberAprId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID05, beneficiary.getPartDPbpNumberMayId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID06, beneficiary.getPartDPbpNumberJunId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID07, beneficiary.getPartDPbpNumberJulId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID08, beneficiary.getPartDPbpNumberAugId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID09, beneficiary.getPartDPbpNumberSeptId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID10, beneficiary.getPartDPbpNumberOctId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID11, beneficiary.getPartDPbpNumberNovId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.PTDPBPID12, beneficiary.getPartDPbpNumberDecId());

    // Segment Number
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID01, beneficiary.getPartDSegmentNumberJanId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID02, beneficiary.getPartDSegmentNumberFebId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID03, beneficiary.getPartDSegmentNumberMarId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID04, beneficiary.getPartDSegmentNumberAprId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID05, beneficiary.getPartDSegmentNumberMayId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID06, beneficiary.getPartDSegmentNumberJunId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID07, beneficiary.getPartDSegmentNumberJulId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID08, beneficiary.getPartDSegmentNumberAugId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID09, beneficiary.getPartDSegmentNumberSeptId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID10, beneficiary.getPartDSegmentNumberOctId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID11, beneficiary.getPartDSegmentNumberNovId());
    addCoverageExtension(
        coverage, CcwCodebookVariable.SGMTID12, beneficiary.getPartDSegmentNumberDecId());

    // Monthly cost sharing group
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR01,
        beneficiary.getPartDLowIncomeCostShareGroupJanCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR02,
        beneficiary.getPartDLowIncomeCostShareGroupFebCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR03,
        beneficiary.getPartDLowIncomeCostShareGroupMarCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR04,
        beneficiary.getPartDLowIncomeCostShareGroupAprCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR05,
        beneficiary.getPartDLowIncomeCostShareGroupMayCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR06,
        beneficiary.getPartDLowIncomeCostShareGroupJunCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR07,
        beneficiary.getPartDLowIncomeCostShareGroupJulCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR08,
        beneficiary.getPartDLowIncomeCostShareGroupAugCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR09,
        beneficiary.getPartDLowIncomeCostShareGroupSeptCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR10,
        beneficiary.getPartDLowIncomeCostShareGroupOctCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR11,
        beneficiary.getPartDLowIncomeCostShareGroupNovCode());
    addCoverageExtension(
        coverage,
        CcwCodebookVariable.CSTSHR12,
        beneficiary.getPartDLowIncomeCostShareGroupDecCode());

    // Monthly Part D Retiree Drug Subsidy Indicators
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND01, beneficiary.getPartDRetireeDrugSubsidyJanInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND02, beneficiary.getPartDRetireeDrugSubsidyFebInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND03, beneficiary.getPartDRetireeDrugSubsidyMarInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND04, beneficiary.getPartDRetireeDrugSubsidyAprInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND05, beneficiary.getPartDRetireeDrugSubsidyMayInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND06, beneficiary.getPartDRetireeDrugSubsidyJunInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND07, beneficiary.getPartDRetireeDrugSubsidyJulInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND08, beneficiary.getPartDRetireeDrugSubsidyAugInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND09, beneficiary.getPartDRetireeDrugSubsidySeptInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND10, beneficiary.getPartDRetireeDrugSubsidyOctInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND11, beneficiary.getPartDRetireeDrugSubsidyNovInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.RDSIND12, beneficiary.getPartDRetireeDrugSubsidyDecInd());

    // The reference year of the enrollment data
    addCoverageDecimalExtension(
        coverage, CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear());

    // Monthly Medicare-Medicaid dual eligibility codes
    transformEntitlementDualEligibility(coverage, beneficiary);

    // update Coverage.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(coverage, beneficiary.getLastUpdated());

    timer.stop();
    return coverage;
  }

  /**
   * @param coverage the {@link Coverage} to generate
   * @param beneficiary the {@link Beneficiary} to generate Coverage for
   */
  private static void transformEntitlementBuyInIndicators(
      Coverage coverage, Beneficiary beneficiary) {

    // Medicare Entitlement Buy In Indicator
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN01, beneficiary.getEntitlementBuyInJanInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN02, beneficiary.getEntitlementBuyInFebInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN03, beneficiary.getEntitlementBuyInMarInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN04, beneficiary.getEntitlementBuyInAprInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN05, beneficiary.getEntitlementBuyInMayInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN06, beneficiary.getEntitlementBuyInJunInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN07, beneficiary.getEntitlementBuyInJulInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN08, beneficiary.getEntitlementBuyInAugInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN09, beneficiary.getEntitlementBuyInSeptInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN10, beneficiary.getEntitlementBuyInOctInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN11, beneficiary.getEntitlementBuyInNovInd());
    addCoverageCodeExtension(
        coverage, CcwCodebookVariable.BUYIN12, beneficiary.getEntitlementBuyInDecInd());
  }

  /**
   * @param coverage the {@link Coverage} to generate
   * @param beneficiary the {@link Beneficiary} to generate Coverage for
   */
  private static void transformEntitlementDualEligibility(
      Coverage coverage, Beneficiary beneficiary) {

    // Monthly Medicare-Medicaid dual eligibility codes
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_01, beneficiary.getMedicaidDualEligibilityJanCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_03, beneficiary.getMedicaidDualEligibilityMarCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_04, beneficiary.getMedicaidDualEligibilityAprCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_05, beneficiary.getMedicaidDualEligibilityMayCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_06, beneficiary.getMedicaidDualEligibilityJunCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_07, beneficiary.getMedicaidDualEligibilityJulCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_08, beneficiary.getMedicaidDualEligibilityAugCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_09, beneficiary.getMedicaidDualEligibilitySeptCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_10, beneficiary.getMedicaidDualEligibilityOctCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_11, beneficiary.getMedicaidDualEligibilityNovCode());
    addCoverageExtension(
        coverage, CcwCodebookVariable.DUAL_12, beneficiary.getMedicaidDualEligibilityDecCode());
  }

  /**
   * Sets the Coverage.status Looks up or adds a contained {@link Identifier} object to the current
   * {@link Patient}. This is used to store Identifier slices related to the Provider organization.
   *
   * @param coverage The {@link Coverage} to Coverage details
   * @param terminationCode The {@link Character} that denotes if Part is active
   */
  static void setCoverageStatus(Coverage coverage, Optional<Character> terminationCode) {
    if (terminationCode.isPresent() && terminationCode.get().equals('0')) {
      coverage.setStatus(CoverageStatus.ACTIVE);
    } else {
      coverage.setStatus(CoverageStatus.CANCELLED);
    }
  }

  /**
   * Sets the Coverage.type creates {@link CodeableConcept} object and sets the Coverage {@link
   * Coverage} type.
   *
   * @param coverage The {@link Coverage} to Coverage details
   */
  static void setTypeAndIssuer(Coverage coverage) {
    coverage.setType(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setCode("SUBSIDIZ")
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")));
    coverage
        .addPayor()
        .setIdentifier(new Identifier().setValue(TransformerConstants.COVERAGE_ISSUER));
  }

  /**
   * Looks up or adds a contained {@link Identifier} object to the current {@link Patient}. This is
   * used to store Identifier slices related to the Provider organization.
   *
   * @param coverage The {@link Coverage} to Coverage details to
   * @param coverageClass The {@link CoverageClass} of the type
   * @param value The value associated with the {@link CoverageClass}
   * @param name The name associated with the {@link CoverageClass}
   */
  static void createCoverageClass(
      Coverage coverage, CoverageClass coverageClass, String value, Optional<String> name) {

    if (value == null || value.isEmpty()) {
      return;
    }

    if (name.isPresent()) {
      coverage
          .addClass_()
          .setValue(value)
          .setName(name.get())
          .getType()
          .addCoding()
          .setSystem(coverageClass.getSystem())
          .setCode(coverageClass.toCode())
          .setDisplay(coverageClass.getDisplay());
    } else {
      coverage
          .addClass_()
          .setValue(value)
          .getType()
          .addCoding()
          .setSystem(coverageClass.getSystem())
          .setCode(coverageClass.toCode())
          .setDisplay(coverageClass.getDisplay());
    }
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param coverage The {@link Coverage} to Coverage details
   * @param policyRelationship The {@link SubscriberPolicyRelationship} associated with the Coverage
   */
  static void setCoverageRelationship(
      Coverage coverage, SubscriberPolicyRelationship policyRelationship) {
    coverage.setRelationship(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setCode(policyRelationship.toCode())
                    .setSystem(policyRelationship.getSystem())
                    .setDisplay(policyRelationship.getDisplay())));
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param coverage The {@link Coverage} to Coverage details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the Coverage
   * @param optVal The {@link String} value associated with the Coverage
   */
  static void addCoverageExtension(
      Coverage coverage, CcwCodebookVariable ccwVariable, Optional<String> optVal) {
    optVal.ifPresent(
        value ->
            coverage.addExtension(
                TransformerUtilsV2.createExtensionCoding(coverage, ccwVariable, value)));
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param coverage The {@link Coverage} to Coverage details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the Coverage
   * @param optVal The {@link Character} value associated with the Coverage
   */
  static void addCoverageCodeExtension(
      Coverage coverage, CcwCodebookVariable ccwVariable, Optional<Character> optVal) {
    optVal.ifPresent(
        value ->
            coverage.addExtension(
                TransformerUtilsV2.createExtensionCoding(coverage, ccwVariable, value)));
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param coverage The {@link Coverage} to Coverage details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the Coverage
   * @param optVal The {@link BigDecimal} value associated with the Coverage
   */
  static void addCoverageDecimalExtension(
      Coverage coverage, CcwCodebookVariable ccwVariable, Optional<BigDecimal> optVal) {
    coverage.addExtension(TransformerUtilsV2.createExtensionDate(ccwVariable, optVal));
  }

  /**
   * Constructs a Timer context {@link Timer.Context} suitable for measuring compute duration
   *
   * @param metricRegistry The EtricRegistry passed into the transformer {@link MetricRegistry}
   * @param partId The context string {@link String}
   */
  static Timer.Context getTimerContext(MetricRegistry metricRegistry, String partId) {
    return metricRegistry
        .timer(
            MetricRegistry.name(CoverageTransformerV2.class.getSimpleName(), "transform", partId))
        .time();
  }
}
