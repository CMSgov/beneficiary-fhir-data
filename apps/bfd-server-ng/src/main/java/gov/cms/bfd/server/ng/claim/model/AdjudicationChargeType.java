package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

// Suppress warnings for duplicate string literals.
// There's too many values here - creating constants for all of these adds too much noise.
@SuppressWarnings("java:S1192")
@AllArgsConstructor
@Getter
enum AdjudicationChargeType {
  LINE_NONCOVERED_CHARGE_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "noncovered",
      "Noncovered",
      "CLM_LINE_NCVRD_CHRG_AMT",
      "Non-Covered Charge Amount"),
  LINE_ALLOWED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "eligible",
      "Eligible Amount",
      "CLM_LINE_ALOWD_CHRG_AMT",
      "Line Allowed Charge Amount"),
  LINE_SUBMITTED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "CLM_LINE_SBMT_CHRG_AMT",
      "Line Submitted Charge Amount"),
  LINE_PROVIDER_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtoprovider",
      "Paid to provider",
      "CLM_LINE_PRVDR_PMT_AMT",
      "Line Provider Payment Amount"),
  LINE_BENE_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidbypatient",
      "Paid by patient",
      "CLM_LINE_BENE_PMT_AMT",
      "Beneficiary Paid Amount"),
  LINE_BENE_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtopatient",
      "Paid to patient",
      "CLM_LINE_BENE_PD_AMT",
      "Payment Amount to Beneficiary"),
  LINE_COVERED_PAID_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "benefit",
      "Benefit Amount",
      "CLM_LINE_CVRD_PD_AMT",
      "Payment Amount"),
  LINE_BLOOD_DEDUCTIBLE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "deductible",
      "Deductible",
      "CLM_LINE_BLOOD_DDCTBL_AMT",
      "Revenue Center Blood Deductible Amount"),
  LINE_MEDICARE_DEDUCTIBLE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "deductible",
      "Deductible",
      "CLM_LINE_MDCR_DDCTBL_AMT",
      "Cash Deductible Amount"),
  LINE_INSTITUTIONAL_ADJUSTED_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_INSTNL_ADJSTD_AMT",
      "Revenue Center Coinsurance/Wage Adjusted Coinsurance Amount"),
  LINE_INSTITUTIONAL_REDUCED_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_INSTNL_RDCD_AMT",
      "Revenue Center Reduced Coinsurance Amount"),
  LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_LINE_INSTNL_MSP1_PD_AMT",
      "Revenue Center 1st MSP Paid Amount"),
  LINE_INSTITUTIONAL_2ND_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_LINE_INSTNL_MSP2_PD_AMT",
      "Revenue Center 2nd MSP Paid Amount"),
  LINE_INSTITUTIONAL_RATE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_INSTNL_RATE_AMT",
      "Revenue Center Rate Amount"),
  ALLOWED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "eligible",
      "Eligible Amount",
      "CLM_ALOWD_CHRG_AMT",
      "Allowed Charge Amount"),
  SUBMITTED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "CLM_SBMT_CHRG_AMT",
      "Total Charge Amount"),
  BENE_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidbypatient",
      "Paid by patient",
      "CLM_BENE_PMT_AMT",
      "Patient Responsibility Payment Amount"),
  PROVIDER_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtoprovider",
      "Paid to provider",
      "CLM_PRVDR_PMT_AMT",
      "Provider Payment Amount"),
  PAYER_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_PRFNL_PRMRY_PYR_AMT",
      "Primary Payer Paid Amount"),
  PATIENT_LIABILITY_REDUCT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_PLRO_AMT",
      "Patient Liability Reduction Other Paid Amount"),
  LOW_INCOME_COST_SHARE_SUB_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_LIS_AMT",
      "Low Income Cost Sharing Subsidy Amount"),
  GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_GRS_BLW_THRSHLD_AMT",
      "Gross Drug Cost Below Out Of Pocket Threshold"),
  GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_GRS_ABOVE_THRSHLD_AMT",
      "Gross Drug Cost Above Out Of Pocket Threshold"),
  BENE_MEDICARE_LRD_USED_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_LRD_USE_CNT",
      "Beneficiary Medicare Lifetime Reserve Days (LRD) Used Count"),
  BENE_TOTAL_COINSURANCE_DAYS_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_MDCR_COINS_DAY_CNT",
      "Beneficiary Total Coinsurance Days Count"),
  NON_UTILIZATION_DAYS_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_NCVRD_DAY_CNT",
      "Claim Medicare Non Utilization Days Count"),
  HOSPICE_PERIOD_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_HOSPC_PRD_CNT",
      "Hospice Period Count"),
  HHA_TOTAL_VISIT_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_HHA_TOT_VISIT_CNT",
      "Claim HHA Visit Count"),
  PPS_CAPITAL_DRUG_WEIGHT_NUMBER(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_DRG_WT_NUM",
      "PPS DRG Weight Number"),
  UTILIZATION_DAYS_COUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_CVRD_DAY_CNT",
      "Claim Medicare Utilization Day Count"),
  PER_DIEM_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_PER_DIEM_AMT",
      "Claim Pass Thru Per Diem Amount"),
  PPS_CAPITAL_DISPROPORTIONATE_SHARE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_DSPRPRTNT_AMT",
      "Claim PPS Capital Disproportionate Share Amount"),
  PPS_CAPITAL_EXCEPTION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_EXCPTN_AMT",
      "Claim PPS Capital Exception Amount"),
  PPS_CAPITAL_FEDERAL_SPECIFIC_PORTION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_CPTL_FSP_AMT",
      "Claim PPS Capital Federal Specific Portion (FSP) Amount"),
  PPS_CAPITAL_INDIRECT_MEDICAL_EDUCATION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_CPTL_IME_AMT",
      "Claim PPS Capital Indirect Medical Education (IME) Amount"),
  PPS_CAPITAL_OUTLIER_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_OUTLIER_AMT",
      "Claim PPS Capital Outlier Amount"),
  PPS_OLD_CAPITAL_HOLD_HARMLESS_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT",
      "Claim PPS Old Capital Hold Harmless Amount"),
  PPS_CAPITAL_HOLD_TOTAL_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_PPS_CPTL_TOT_AMT",
      "Claim Total PPS Capital Amount"),
  PRIMARY_PAYER_NON_MEDICARE_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_INSTNL_PRMRY_PYR_AMT",
      "Primary Payer (if not Medicare) Claim Paid Amount"),
  PROFESSIONAL_COMPONENT_CHARGE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_PRFNL_AMT",
      "Professional Component Charge Amount"),
  DRUG_OUTLIER_APPROVED_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_DRG_OUTLIER_AMT",
      "DRG Outlier Approved Payment Amount"),
  UNCOMPENSATED_CARE_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_HIPPS_UNCOMPD_CARE_AMT",
      "Claim Uncompensated Care Payment Amount"),
  BENE_INPATIENT_DEDUCTIBLE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_BENE_DDCTBL_AMT",
      "Beneficiary Inpatient (or other Part A) Deductible Amount"),
  BENE_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtopatient",
      "Paid to patient",
      "CLM_MDCR_INSTNL_BENE_PD_AMT",
      "Institutional Beneficiary Paid Amount"),
  STANDARDIZED_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_FINL_STDZD_PYMT_AMT",
      "Standardized Payment Amount"),
  HOSPITAL_ACQUIRED_CONDITION_REDUCTION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_HAC_RDCTN_PYMT_AMT",
      "Hospital Acquired Condition Reduction Amount"),
  BLENDED_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_HIPPS_MODEL_BNDLD_PMT_AMT",
      "Blended Payment Amount"),
  READMISSION_REDUCTION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_HIPPS_READMSN_RDCTN_AMT",
      "Readmission Reduction Amount"),
  HIPPS_VALUE_BASED_PURCHASING_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_HIPPS_VBP_AMT",
      "HIPPS Value Based Purchasing Amount"),
  LOW_VOLUME_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_INSTNL_LOW_VOL_PMT_AMT",
      "Low Volume Payment Amount"),
  FIRST_YEAR_RATE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_1ST_YR_RATE_AMT",
      "First Year Rate Amount"),
  SECOND_YEAR_RATE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_IP_SCND_YR_RATE_AMT",
      "Second Year Rate Amount"),
  MARYLAND_WAIVER_STANDARDIZED_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_PPS_MD_WVR_STDZD_VAL_AMT",
      "Maryland Waiver Standardized Amount"),
  SITE_NEUTRAL_COST_BASED_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_SITE_NTRL_CST_BSD_PYMT_AMT",
      "Site-Neutral Cost-Based Payment Amount"),
  SITE_NEUTRAL_IPPS_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_SITE_NTRL_IP_PPS_PYMT_AMT",
      "Site-Neutral IPPS Payment Amount"),
  SHORT_STAY_OUTLIER_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_SS_OUTLIER_STD_PYMT_AMT",
      "Short Stay Outlier Payment Amount"),
  OPERATING_DISPROPORTIONATE_SHARE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_OPRTNL_DSPRTNT_AMT",
      "Operating Disproportionate Share Amount"),
  OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_OPRTNL_IME_AMT",
      "Operating Indirect Medical Education Amount"),
  BENE_PART_B_DEDUCTIBLE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "deductible",
      "Deductible",
      "CLM_MDCR_DDCTBL_AMT",
      "Beneficiary Deductible Amount"),
  BENE_PART_A_COINSURANCE_LIABILITY_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
      "CLM_MDCR_COINSRNC_AMT",
      "Beneficiary Coinsurance Liability Amount"),
  BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_BLOOD_LBLTY_AMT",
      "Beneficiary Blood Deductible Liability Amount"),
  INPATIENT_NON_COVERED_CHARGE_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "noncovered",
      "Noncovered",
      "CLM_NCVRD_CHRG_AMT",
      "Inpatient(or other Part A) Non-covered Charge Amount"),
  BENE_INTEREST_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_BENE_INTRST_PD_AMT",
      "Beneficiary Interest Paid Amount"),
  BENE_COINSURANCE_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
      "CLM_BENE_PMT_COINSRNC_AMT",
      "Beneficiary Coinsurance Amount"),
  BLOOD_CHARGE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION, "CLM_BLOOD_CHRG_AMT", "Blood Charge Amount"),
  BLOOD_NONCOVERED_CHARGE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_BLOOD_NCVRD_CHRG_AMT",
      "Blood Noncovered Charge Amount"),
  COB_PATIENT_RESPONSIBILITY_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_COB_PTNT_RESP_AMT",
      "Coordination of Benefits Patient Responsibility Amount"),
  OTHER_THIRD_PARTY_PAYER_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_OTHR_TP_PD_AMT",
      "Other Third Party Payer Paid Amount"),
  PROVIDER_INTEREST_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_PRVDR_INTRST_PD_AMT",
      "Provider Interest Paid Amount"),
  PROVIDER_OBLIGATION_TO_ACCEPT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_PRVDR_OTAF_AMT",
      "Provider Obligation To Accept as Full Amount"),
  REMAINING_AMOUNT_TO_PROVIDER(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_PRVDR_RMNG_DUE_AMT",
      "Remaining Amount to Provider"),
  TOTAL_CONTRACTUAL_AMOUNT_DISCREPANCY(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_TOT_CNTRCTL_AMT",
      "Total Contractual Amount Discrepancy"),
  PROVIDER_OFFSET_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_PRVDR_ACNT_RCVBL_OFST_AMT",
      "Provider Offset Amount"),
  LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_ADD_ON_PYMT_AMT",
      "Add On Payment Amount"),
  LINE_INSTITUTIONAL_NON_EHR_REDUCTION_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_NON_EHR_RDCTN_AMT",
      "Non-EHR Reduction Amount"),
  LINE_PROFESSIONAL_CARRIER_CLINICAL_CHARGE_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_CARR_CLNCL_CHRG_AMT",
      "Carrier Clinical Charge Amount"),
  LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_CARR_PSYCH_OT_LMT_AMT",
      "Therapy Amount Applied to Limit"),
  LINE_PROFESSIONAL_INTEREST_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_PRFNL_INTRST_AMT",
      "Professional Interest Amount"),
  LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_MDCR_PRMRY_PYR_ALOWD_AMT",
      "Line Primary Payer Allowed Amount"),
  LINE_INSTITUTIONAL_TRANSITIONAL_DRG_ADD_ON_PAYMENT_ADJUSTMENT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_REV_CNTR_TDAPA_AMT",
      "Transitional Drug Add-On Payment Adjustment"),
  LINE_NONCOVERED_PRODUCT_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_NCVRD_PD_AMT",
      "Amount Paid for Noncovered Product or Service"),
  LINE_OTHER_THIRD_PARTY_PAID_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_OTHR_TP_PD_AMT",
      "Other Third Party Paid Amount"),
  LINE_PROVIDER_OBLIGATION_FULL_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_OTAF_AMT",
      "Provider Obligation To Accept as Full Amount"),
  LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_BENE_PRMRY_PYR_PD_AMT",
      "Line Primary Payer Paid Amount"),
  LINE_PROFESSIONAL_SCREEN_SAVINGS_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "discount",
      "Discount",
      "CLM_LINE_DMERC_SCRN_SVGS_AMT",
      "Screen Savings Amount"),
  LINE_PROFESSIONAL_PURCHASE_PRICE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "eligible",
      "Eligible Amount",
      "CLM_LINE_PRFNL_DME_PRICE_AMT",
      "Purchase Price Amount"),
  LINE_RX_REPORTED_GAP_DISCOUNT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_RPTD_GAP_DSCNT_AMT",
      "Claim Line Reported Gap Discount Amount"),
  TOTAL_DRUG_COST_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "TOT_RX_CST_AMT",
      "Total drug cost (Part D)");

  private final String coding1System;
  private final String coding1Code;
  private final String coding1Display;
  private final String coding2Code;
  private final String coding2Display;

  AdjudicationChargeType(String coding1System, String coding1Code, String coding1Display) {
    this(coding1System, coding1Code, coding1Display, "", "");
  }

  private CodeableConcept buildCategory() {
    var category =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(coding1System)
                    .setCode(coding1Code)
                    .setDisplay(coding1Display));
    if (!coding2Code.isBlank()) {
      category.addCoding(
          new Coding()
              .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION)
              .setCode(coding2Code)
              .setDisplay(coding2Display));
    }
    return category;
  }

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudication(BigDecimal value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(buildCategory())
        .setAmount(USD.toFhir(value));
  }

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudicationUnsignedType(int value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(buildCategory())
        .setValue(Integer.toUnsignedLong(value));
  }

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudicationUnsignedType(long value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(buildCategory())
        .setValue(value);
  }

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudicationDecimalType(BigDecimal value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(buildCategory())
        .setValue(value);
  }

  ExplanationOfBenefit.TotalComponent toFhirTotal(BigDecimal value) {
    return new ExplanationOfBenefit.TotalComponent()
        .setCategory(buildCategory())
        .setAmount(USD.toFhir(value));
  }
}
