package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class BenefitBalanceInstitutional {
  @Column(name = "clm_mdcr_ip_lrd_use_cnt")
  private int lifetimeReserveDaysUsed;

  @Column(name = "clm_mdcr_instnl_bene_pd_amt")
  private double benePaidAmount;

  @Column(name = "clm_instnl_mdcr_coins_day_cnt")
  private int totalCoinsuranceDays;

  @Column(name = "clm_instnl_cvrd_day_cnt")
  private double totalCoveredDays;

  @Column(name = "clm_instnl_per_diem_amt")
  private double perDiemAmount;

  @Column(name = "clm_mdcr_hha_tot_visit_cnt")
  private int totalHHAVisits;

  @Column(name = "clm_mdcr_hospc_prd_cnt")
  private int totalHospicePeriodTrailers;

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

  @Column(name = "clm_mdcr_ip_bene_ddctbl_amt")
  private double beneDeductibleAmount;

  @Column(name = "clm_hipps_uncompd_care_amt")
  private double hippsUncompensatedCareAmount;

  List<ExplanationOfBenefit.BenefitComponent> toFhir() {
    return List.of(
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_LRD_USE_CNT.toFhirIntType(
            lifetimeReserveDaysUsed),
        BenefitBalanceInstitutionalType.CLM_MDCR_HHA_TOT_VISIT_CNT.toFhirIntType(totalHHAVisits),
        BenefitBalanceInstitutionalType.CLM_MDCR_HOSPC_PRD_CNT.toFhirIntType(totalHospicePeriodTrailers),
        BenefitBalanceInstitutionalType.CLM_INSTNL_MDCR_COINS_DAY_CNT.toFhirIntType(
            totalCoinsuranceDays),
        BenefitBalanceInstitutionalType.CLM_INSTNL_CVRD_DAY_CNT.toFhirIntType(
            // These are always whole numbers, but IDR stores them as floats
            Math.round(totalCoveredDays)),
        BenefitBalanceInstitutionalType.CLM_INSTNL_PER_DIEM_AMT.toFhirMoney(perDiemAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_INSTNL_BENE_PD_AMT.toFhirMoney(benePaidAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_DSPRPRTNT_AMT.toFhirMoney(
            ppsDisproportionateAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_EXCPTN_AMT.toFhirMoney(ppsExceptionAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_CPTL_FSP_AMT.toFhirMoney(
            ppsCapitalFspAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_CPTL_IME_AMT.toFhirMoney(
            ppsCapitalImeAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_OUTLIER_AMT.toFhirMoney(ppsOutlierAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT.toFhirMoney(
            ppsCapitalHarmlessAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_PPS_CPTL_TOT_AMT.toFhirMoney(
            ppsCapitalTotalAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_INSTNL_PRMRY_PYR_AMT.toFhirMoney(
            primaryPayerAmount),
        BenefitBalanceInstitutionalType.CLM_INSTNL_PRFNL_AMT.toFhirMoney(professionalAmount),
        BenefitBalanceInstitutionalType.CLM_INSTNL_DRG_OUTLIER_AMT.toFhirMoney(drgOutlierAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_IP_BENE_DDCTBL_AMT.toFhirMoney(
            beneDeductibleAmount),
        BenefitBalanceInstitutionalType.CLM_HIPPS_UNCOMPD_CARE_AMT.toFhirMoney(
            hippsUncompensatedCareAmount));
  }
}
