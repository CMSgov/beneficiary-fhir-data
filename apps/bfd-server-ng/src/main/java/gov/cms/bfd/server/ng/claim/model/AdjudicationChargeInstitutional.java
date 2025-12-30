package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class AdjudicationChargeInstitutional {

  @Column(name = "clm_mdcr_ip_lrd_use_cnt")
  private int lifetimeReserveDaysUsed;

  @Column(name = "clm_instnl_mdcr_coins_day_cnt")
  private int totalCoinsuranceDays;

  @Column(name = "clm_instnl_ncvrd_day_cnt")
  private int nonUtilizationDays;

  @Column(name = "clm_mdcr_hospc_prd_cnt")
  private int totalHospicePeriodCount;

  @Column(name = "clm_mdcr_hha_tot_visit_cnt")
  private int totalHHAVisits;

  @Column(name = "clm_mdcr_ip_pps_drg_wt_num")
  private double ppsDrgWeight;

  @Column(name = "clm_instnl_cvrd_day_cnt")
  private double totalCoveredDays;

  @Column(name = "clm_instnl_per_diem_amt")
  private double perDiemAmount;

  @Column(name = "clm_mdcr_ip_pps_dsprprtnt_amt")
  private double ppsDisproportionateAmount;

  @Column(name = "clm_mdcr_ip_pps_excptn_amt")
  private double ppsExceptionAmount;

  @Column(name = "clm_mdcr_ip_pps_cptl_fsp_amt")
  private double ppsCapitalFspAmount;

  @Column(name = "clm_mdcr_ip_pps_cptl_ime_amt")
  private double ppsCapitalImeAmount;

  @Column(name = "clm_mdcr_ip_pps_outlier_amt")
  private double ppsOutlierAmount;

  @Column(name = "clm_mdcr_ip_pps_cptl_hrmls_amt")
  private double ppsCapitalHarmlessAmount;

  @Column(name = "clm_mdcr_ip_pps_cptl_tot_amt")
  private double ppsCapitalTotalAmount;

  @Column(name = "clm_mdcr_instnl_prmry_pyr_amt")
  private double primaryPayerAmount;

  @Column(name = "clm_instnl_prfnl_amt")
  private double professionalAmount;

  @Column(name = "clm_instnl_drg_outlier_amt")
  private double drgOutlierAmount;

  @Column(name = "clm_hipps_uncompd_care_amt")
  private double hippsUncompensatedCareAmount;

  @Column(name = "clm_mdcr_ip_bene_ddctbl_amt")
  private double beneDeductibleAmount;

  @Column(name = "clm_mdcr_instnl_bene_pd_amt")
  private double benePaidAmount;

  @Column(name = "clm_finl_stdzd_pymt_amt")
  private double standardizedPaymentAmount;

  @Column(name = "clm_hac_rdctn_pymt_amt")
  private double hospitalAcquiredConditionReductionAmount;

  @Column(name = "clm_hipps_model_bndld_pmt_amt")
  private double blendedPaymentAmount;

  @Column(name = "clm_hipps_readmsn_rdctn_amt")
  private double readmissionReductionAmount;

  @Column(name = "clm_hipps_vbp_amt")
  private double hippsPurchasingAmount;

  @Column(name = "clm_instnl_low_vol_pmt_amt")
  private double lowVolumePaymentAmount;

  @Column(name = "clm_mdcr_ip_1st_yr_rate_amt")
  private double firstYearRateAmount;

  @Column(name = "clm_mdcr_ip_scnd_yr_rate_amt")
  private double secondYearRateAmount;

  @Column(name = "clm_pps_md_wvr_stdzd_val_amt")
  private double marylandWaiverStandardizedAmount;

  @Column(name = "clm_site_ntrl_cst_bsd_pymt_amt")
  private double siteNeutralCostBasedPaymentAmount;

  @Column(name = "clm_site_ntrl_ip_pps_pymt_amt")
  private double siteNeutralIPPSPaymentAmount;

  @Column(name = "clm_ss_outlier_std_pymt_amt")
  private double shortStayOutlierPaymentAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir(List<ClaimValue> claimValues) {
    return Stream.of(
            AdjudicationChargeClaimValue.toFhir(claimValues),
            List.of(
                AdjudicationChargeType.BENE_MEDICARE_LRD_USED_COUNT.toFhirAdjudicationUnsignedType(
                    lifetimeReserveDaysUsed),
                AdjudicationChargeType.BENE_TOTAL_COINSURANCE_DAYS_COUNT
                    .toFhirAdjudicationUnsignedType(totalCoinsuranceDays),
                AdjudicationChargeType.NON_UTILIZATION_DAYS_COUNT.toFhirAdjudicationUnsignedType(
                    nonUtilizationDays),
                AdjudicationChargeType.HOSPICE_PERIOD_COUNT.toFhirAdjudicationUnsignedType(
                    totalHospicePeriodCount),
                AdjudicationChargeType.HHA_TOTAL_VISIT_COUNT.toFhirAdjudicationUnsignedType(
                    totalHHAVisits),
                AdjudicationChargeType.PPS_CAPITAL_DRUG_WEIGHT_NUMBER.toFhirAdjudicationDecimalType(
                    ppsDrgWeight),
                AdjudicationChargeType.UTILIZATION_DAYS_COUNT.toFhirAdjudicationUnsignedType(
                    // These are always whole numbers, but IDR stores them as floats
                    Math.round(totalCoveredDays)),
                AdjudicationChargeType.PER_DIEM_AMOUNT.toFhirAdjudication(perDiemAmount),
                AdjudicationChargeType.PPS_CAPITAL_DISPROPORTIONATE_SHARE_AMOUNT.toFhirAdjudication(
                    ppsDisproportionateAmount),
                AdjudicationChargeType.PPS_CAPITAL_EXCEPTION_AMOUNT.toFhirAdjudication(
                    ppsExceptionAmount),
                AdjudicationChargeType.PPS_CAPITAL_FEDERAL_SPECIFIC_PORTION_AMOUNT
                    .toFhirAdjudication(ppsCapitalFspAmount),
                AdjudicationChargeType.PPS_CAPITAL_INDIRECT_MEDICAL_EDUCATION_AMOUNT
                    .toFhirAdjudication(ppsCapitalImeAmount),
                AdjudicationChargeType.PPS_CAPITAL_OUTLIER_AMOUNT.toFhirAdjudication(
                    ppsOutlierAmount),
                AdjudicationChargeType.PPS_OLD_CAPITAL_HOLD_HARMLESS_AMOUNT.toFhirAdjudication(
                    ppsCapitalHarmlessAmount),
                AdjudicationChargeType.PPS_CAPITAL_HOLD_TOTAL_AMOUNT.toFhirAdjudication(
                    ppsCapitalTotalAmount),
                AdjudicationChargeType.PRIMARY_PAYER_NON_MEDICARE_PAID_AMOUNT.toFhirAdjudication(
                    primaryPayerAmount),
                AdjudicationChargeType.PROFESSIONAL_COMPONENT_CHARGE_AMOUNT.toFhirAdjudication(
                    professionalAmount),
                AdjudicationChargeType.DRUG_OUTLIER_APPROVED_PAYMENT_AMOUNT.toFhirAdjudication(
                    drgOutlierAmount),
                AdjudicationChargeType.UNCOMPENSATED_CARE_PAYMENT_AMOUNT.toFhirAdjudication(
                    hippsUncompensatedCareAmount),
                AdjudicationChargeType.BENE_INPATIENT_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
                    beneDeductibleAmount),
                AdjudicationChargeType.STANDARDIZED_PAYMENT_AMOUNT.toFhirAdjudication(
                    standardizedPaymentAmount),
                AdjudicationChargeType.HOSPITAL_ACQUIRED_CONDITION_REDUCTION_AMOUNT
                    .toFhirAdjudication(hospitalAcquiredConditionReductionAmount),
                AdjudicationChargeType.BLENDED_PAYMENT_AMOUNT.toFhirAdjudication(
                    blendedPaymentAmount),
                AdjudicationChargeType.READMISSION_REDUCTION_AMOUNT.toFhirAdjudication(
                    readmissionReductionAmount),
                AdjudicationChargeType.HIPPS_VALUE_BASED_PURCHASING_AMOUNT.toFhirAdjudication(
                    hippsPurchasingAmount),
                AdjudicationChargeType.LOW_VOLUME_PAYMENT_AMOUNT.toFhirAdjudication(
                    lowVolumePaymentAmount),
                AdjudicationChargeType.FIRST_YEAR_RATE_AMOUNT.toFhirAdjudication(
                    firstYearRateAmount),
                AdjudicationChargeType.SECOND_YEAR_RATE_AMOUNT.toFhirAdjudication(
                    secondYearRateAmount),
                AdjudicationChargeType.MARYLAND_WAIVER_STANDARDIZED_AMOUNT.toFhirAdjudication(
                    marylandWaiverStandardizedAmount),
                AdjudicationChargeType.SITE_NEUTRAL_COST_BASED_PAYMENT_AMOUNT.toFhirAdjudication(
                    siteNeutralCostBasedPaymentAmount),
                AdjudicationChargeType.SITE_NEUTRAL_IPPS_PAYMENT_AMOUNT.toFhirAdjudication(
                    siteNeutralIPPSPaymentAmount),
                AdjudicationChargeType.SHORT_STAY_OUTLIER_PAYMENT_AMOUNT.toFhirAdjudication(
                    shortStayOutlierPaymentAmount)))
        .flatMap(Collection::stream)
        .toList();
  }
}
