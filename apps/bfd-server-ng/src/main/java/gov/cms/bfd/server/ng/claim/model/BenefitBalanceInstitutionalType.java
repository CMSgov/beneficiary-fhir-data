package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.UnsignedIntType;

@AllArgsConstructor
enum BenefitBalanceInstitutionalType {
  /** CLM_MDCR_INSTNL_BENE_PD_AMT - */
  CLM_MDCR_INSTNL_BENE_PD_AMT(
      "CLM_MDCR_INSTNL_BENE_PD_AMT",
      "The amount paid, from the Medicare trust fund, to the beneficiary for the services reported on the claim."),
  /** CLM_MDCR_IP_LRD_USE_CNT - Beneficiary Medicare Lifetime Reserve Days (LRD) Used Count. */
  CLM_MDCR_IP_LRD_USE_CNT(
      "CLM_MDCR_IP_LRD_USE_CNT", "Beneficiary Medicare Lifetime Reserve Days (LRD) Used Count"),
  /** CLM_INSTNL_MDCR_COINS_DAY_CNT - Beneficiary Total Coinsurance Days Count. */
  CLM_INSTNL_MDCR_COINS_DAY_CNT(
      "CLM_INSTNL_MDCR_COINS_DAY_CNT", "Beneficiary Total Coinsurance Days Count"),
  /** CLM_INSTNL_NCVRD_DAY_CNT - Claim Medicare Non Utilization Days Count. */
  CLM_INSTNL_NCVRD_DAY_CNT("CLM_INSTNL_NCVRD_DAY_CNT", "Claim Medicare Non Utilization Days Count"),
  /** CLM_INSTNL_PER_DIEM_AMT - Claim Pass Thru Per Diem Amount. */
  CLM_INSTNL_PER_DIEM_AMT("CLM_INSTNL_PER_DIEM_AMT", "Claim Pass Thru Per Diem Amount"),
  /** CLM_INSTNL_CVRD_DAY_CNT - Claim Medicare Utilization Day Count. */
  CLM_INSTNL_CVRD_DAY_CNT("CLM_INSTNL_CVRD_DAY_CNT", "Claim Medicare Utilization Day Count"),
  /** CLM_MDCR_HHA_TOT_VISIT_CNT - Claim HHA Total Visit Count. */
  CLM_MDCR_HHA_TOT_VISIT_CNT("CLM_MDCR_HHA_TOT_VISIT_CNT", "Claim HHA Total Visit Count."),
  /** CLM_MDCR_HOSPC_PRD_CNT - Beneficiary's Hospice Period Count. */
  CLM_MDCR_HOSPC_PRD_CNT("CLM_MDCR_HOSPC_PRD_CNT", "Beneficiary's Hospice Period Count."),
  /** CLM_MDCR_IP_PPS_DSPRPRTNT_AMT - Claim PPS Capital Disproportionate Share Amount. */
  CLM_MDCR_IP_PPS_DSPRPRTNT_AMT(
      "CLM_MDCR_IP_PPS_DSPRPRTNT_AMT", "Claim PPS Capital Disproportionate Share Amount"),
  /** CLM_MDCR_IP_PPS_EXCPTN_AMT - Claim PPS Capital Exception Amount. */
  CLM_MDCR_IP_PPS_EXCPTN_AMT("CLM_MDCR_IP_PPS_EXCPTN_AMT", "Claim PPS Capital Exception Amount"),
  /** CLM_MDCR_IP_PPS_CPTL_FSP_AMT - Claim PPS Capital Federal Specific Portion (FSP) Amount. */
  CLM_MDCR_IP_PPS_CPTL_FSP_AMT(
      "CLM_MDCR_IP_PPS_CPTL_FSP_AMT", "Claim PPS Capital Federal Specific Portion (FSP) Amount"),
  /** CLM_MDCR_IP_PPS_CPTL_IME_AMT - Claim PPS Capital Indirect Medical Education (IME) Amount. */
  CLM_MDCR_IP_PPS_CPTL_IME_AMT(
      "CLM_MDCR_IP_PPS_CPTL_IME_AMT", "Claim PPS Capital Indirect Medical Education (IME) Amount"),
  /** CLM_MDCR_IP_PPS_OUTLIER_AMT - Claim PPS Capital Outlier Amount. */
  CLM_MDCR_IP_PPS_OUTLIER_AMT("CLM_MDCR_IP_PPS_OUTLIER_AMT", "Claim PPS Capital Outlier Amount"),
  /** CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT - Claim PPS Old Capital Hold Harmless Amount. */
  CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT(
      "CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT", "Claim PPS Old Capital Hold Harmless Amount"),
  /** CLM_MDCR_IP_PPS_CPTL_TOT_AMT - Claim Total PPS Capital Amount. */
  CLM_MDCR_IP_PPS_CPTL_TOT_AMT("CLM_MDCR_IP_PPS_CPTL_TOT_AMT", "Claim Total PPS Capital Amount"),
  /** CLM_MDCR_INSTNL_PRMRY_PYR_AMT - Primary Payer (if not Medicare) Claim Paid Amount. */
  CLM_MDCR_INSTNL_PRMRY_PYR_AMT(
      "CLM_MDCR_INSTNL_PRMRY_PYR_AMT", "Primary Payer (if not Medicare) Claim Paid Amount"),
  /** CLM_INSTNL_PRFNL_AMT - Professional Component Charge Amount. */
  CLM_INSTNL_PRFNL_AMT("CLM_INSTNL_PRFNL_AMT", "Professional Component Charge Amount"),
  /** CLM_INSTNL_DRG_OUTLIER_AMT - DRG Outlier Approved Payment Amount. */
  CLM_INSTNL_DRG_OUTLIER_AMT("CLM_INSTNL_DRG_OUTLIER_AMT", "DRG Outlier Approved Payment Amount"),
  /** CLM_MDCR_IP_BENE_DDCTBL_AMT - Beneficiary Inpatient (or other Part A) Deductible Amount. */
  CLM_MDCR_IP_BENE_DDCTBL_AMT(
      "CLM_MDCR_IP_BENE_DDCTBL_AMT", "Beneficiary Inpatient (or other Part A) Deductible Amount"),
  /** CLM_MDCR_COINSRNC_AMT - Beneficiary Part A Coinsurance Liability Amount. */
  CLM_MDCR_COINSRNC_AMT("CLM_MDCR_COINSRNC_AMT", "Beneficiary Part A Coinsurance Liability Amount"),
  /** CLM_BLOOD_LBLTY_AMT - Beneficiary Blood Deductible Liability Amount. */
  CLM_BLOOD_LBLTY_AMT("CLM_BLOOD_LBLTY_AMT", "Beneficiary Blood Deductible Liability Amount"),
  /** CLM_NCVRD_CHRG_AMT - Inpatient(or other Part A) Non-covered Charge Amount. */
  CLM_NCVRD_CHRG_AMT("CLM_NCVRD_CHRG_AMT", "Inpatient(or other Part A) Non-covered Charge Amount"),
  /** CLM_MDCR_DDCTBL_AMT - Beneficiary Part B Deductible Amount. */
  CLM_MDCR_DDCTBL_AMT("CLM_MDCR_DDCTBL_AMT", "Beneficiary Part B Deductible Amount"),
  /** CLM_OPRTNL_IME_AMT - Operating Indirect Medical Education Amount. */
  CLM_OPRTNL_IME_AMT("CLM_OPRTNL_IME_AMT", "Operating Indirect Medical Education Amount"),
  /** CLM_OPRTNL_DSPRTNT_AMT - Operating Disproportionate Share Amount. */
  CLM_OPRTNL_DSPRTNT_AMT("CLM_OPRTNL_DSPRTNT_AMT", "Operating Disproportionate Share Amount"),
  /** CLM_HIPPS_UNCOMPD_CARE_AMT - Claim Uncompensated Care Payment Amount. */
  CLM_HIPPS_UNCOMPD_CARE_AMT(
      "CLM_HIPPS_UNCOMPD_CARE_AMT", "Claim Uncompensated Care Payment Amount");

  private final String code;
  private final String display;

  ExplanationOfBenefit.BenefitComponent toFhirIntType(int val) {
    return new ExplanationOfBenefit.BenefitComponent()
        .setType(toFhirType())
        .setUsed(new UnsignedIntType(val));
  }

  ExplanationOfBenefit.BenefitComponent toFhirIntType(long val) {
    return new ExplanationOfBenefit.BenefitComponent()
        .setType(toFhirType())
        .setUsed(new UnsignedIntType(val));
  }

  ExplanationOfBenefit.BenefitComponent toFhirMoney(double val) {
    return new ExplanationOfBenefit.BenefitComponent()
        .setType(toFhirType())
        .setUsed(USD.toFhir(val));
  }

  private CodeableConcept toFhirType() {
    return new CodeableConcept(
        new Coding()
            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BENEFIT_BALANCE)
            .setCode(code)
            .setDisplay(display));
  }
}
