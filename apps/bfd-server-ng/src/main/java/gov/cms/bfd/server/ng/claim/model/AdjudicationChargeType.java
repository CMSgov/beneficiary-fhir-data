package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

// Suppress warnings for duplicate string literals.
// There's too many values here - creating constants for all of these adds too much noise.
@SuppressWarnings("java:S1192")
@Getter
@AllArgsConstructor
enum AdjudicationChargeType {
  LINE_NONCOVERED_CHARGE_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "noncovered",
      "Noncovered",
      "CLM_LINE_NCVRD_CHRG_AMT",
      "Revenue Center Non-Covered Charge Amount"),
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
      "Revenue Center Patient Responsibility Payment Amount"),
  LINE_BENE_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtopatient",
      "Paid to patient",
      "CLM_LINE_BENE_PD_AMT",
      "Revenue Center Payment Amount to Beneficiary"),
  LINE_COVERED_PAID_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "benefit",
      "Benefit Amount",
      "CLM_LINE_CVRD_PD_AMT",
      "Revenue Center Payment Amount"),
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
      "Revenue Center Cash Deductible Amount"),
  LINE_INSTITUTIONAL_ADJUSTED_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
      "CLM_LINE_INSTNL_ADJSTD_AMT",
      "Revenue Center Coinsurance/Wage Adjusted Coinsurance Amount"),
  LINE_INSTITUTIONAL_REDUCED_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
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
      "Allowed Amount",
      "CLM_ALOWD_CHRG_AMT",
      "Total Allowed Charge Amount"),
  SUBMITTED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "CLM_SBMT_CHRG_AMT",
      "Total Submitted Charge Amount"),
  BENE_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidbypatient",
      "Paid by patient",
      "CLM_BENE_PMT_AMT",
      "Revenue Center Patient Responsibility Payment Amount"),
  PROVIDER_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtoprovider",
      "Paid to provider",
      "CLM_PRVDR_PMT_AMT",
      "Provider Payment Amount"),
  PAYER_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtoprovider",
      "Paid to provider",
      "CLM_MDCR_PRFNL_PRMRY_PYR_AMT",
      "Primary Payer Paid Amount"),
  GAP_DISCOUNT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_RPTD_MFTR_DSCNT_AMT",
      "Gap Discount Amount"),
  VACCINATION_ADMIN_FEE(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_VCCN_ADMIN_FEE_AMT",
      "Vaccination Administration Fee"),
  OTHER_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_TROOP_TOT_AMT",
      "Other True Out Of Pocket Paid Amount"),
  DISPENSING_FEE(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION, "CLM_LINE_SRVC_CST_AMT", "Dispensing Fee"),
  SALES_TAX_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION, "CLM_LINE_SLS_TAX_AMT", "Sales Tax Amount"),
  PATIENT_LIABILITY_REDUCT_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION, "CLM_LINE_PLRO_AMT", "Sales Tax Amount"),
  LOW_INCOME_COST_SHARE_SUB_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_LIS_AMT",
      "Low Income Cost Sharing Subsidy Amount"),
  INGREDIENT_COST_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_INGRDNT_CST_AMT",
      "Ingredient Cost Amount"),
  GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_GRS_BLW_THRSHLD_AMT",
      "Gross Drug Cost Below Out Of Pocket Threshold"),
  GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT(
      SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION,
      "CLM_LINE_GRS_ABOVE_THRSHLD_AMT",
      "Gross Drug Cost Above Out Of Pocket Threshold");

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

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudication(double value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(buildCategory())
        .setAmount(USD.toFhir(value));
  }

  ExplanationOfBenefit.TotalComponent toFhirTotal(double value) {
    return new ExplanationOfBenefit.TotalComponent()
        .setCategory(buildCategory())
        .setAmount(USD.toFhir(value));
  }
}
