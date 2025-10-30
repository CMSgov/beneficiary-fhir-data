package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Getter
@AllArgsConstructor
enum AdjudicationChargeType {
  NONCOVERED_CHARGE_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "noncovered",
      "Noncovered",
      "CLM_LINE_NCVRD_CHRG_AMT",
      "Revenue Center Non-Covered Charge Amount"),
  ALLOWED_CHARGE_AMOUNT(
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
  PROVIDER_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtoprovider",
      "Paid to provider",
      "CLM_LINE_PRVDR_PMT_AMT",
      "Line Provider Payment Amount"),
  BENE_PAYMENT_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidbypatient",
      "Paid by patient",
      "CLM_LINE_BENE_PMT_AMT",
      "Revenue Center Patient Responsibility Payment Amount"),
  BENE_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "paidtopatient",
      "Paid to patient",
      "CLM_LINE_BENE_PD_AMT",
      "Revenue Center Payment Amount to Beneficiary"),
  COVERED_PAID_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "benefit",
      "Benefit Amount",
      "CLM_LINE_CVRD_PD_AMT",
      "Revenue Center Payment Amount"),
  BLOOD_DEDUCTIBLE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "deductible",
      "Deductible",
      "CLM_LINE_BLOOD_DDCTBL_AMT",
      "Revenue Center Blood Deductible Amount"),
  MEDICARE_DEDUCTIBLE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "deductible",
      "Deductible",
      "CLM_LINE_MDCR_DDCTBL_AMT",
      "Revenue Center Cash Deductible Amount"),
  INSTITUTIONAL_ADJUSTED_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
      "CLM_LINE_INSTNL_ADJSTD_AMT",
      "Revenue Center Coinsurance/Wage Adjusted Coinsurance Amount"),
  INSTITUTIONAL_REDUCED_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "coinsurance",
      "Co-insurance",
      "CLM_LINE_INSTNL_RDCD_AMT",
      "Revenue Center Reduced Coinsurance Amount"),
  INSTITUTIONAL_1ST_MSP_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_LINE_INSTNL_MSP1_PD_AMT",
      "Revenue Center 1st MSP Paid Amount"),
  INSTITUTIONAL_2ND_PAID_AMOUNT(
      SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION,
      "priorpayerpaid",
      "Prior payer paid",
      "CLM_LINE_INSTNL_MSP2_PD_AMT",
      "Revenue Center 2nd MSP Paid Amount"),
  INSTITUTIONAL_RATE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "CLM_LINE_INSTNL_RATE_AMT",
      "Revenue Center Rate Amount"),
  SUBMITTED_CHARGE_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "submitted",
      "Submitted Amount",
      "CLM_SBMT_CHRG_AMT",
      "Total Charge Amount"),
  GAP_DISCOUNT_AMOUNT(
      SystemUrls.HL7_ADJUDICATION, "CLM_RPTD_MFTR_DSCNT_AMT", "Gap Discount Amount"),
  VACCINATION_ADMIN_FEE(
      SystemUrls.HL7_ADJUDICATION, "CLM_LINE_VCCN_ADMIN_FEE_AMT", "Vaccination Administration Fee"),
  OTHER_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "CLM_LINE_TROOP_TOT_AMT",
      "Other True Out Of Pocket Paid Amount"),
  DISPENSING_FEE(SystemUrls.HL7_ADJUDICATION, "CLM_LINE_SRVC_CST_AMT", "Dispensing Fee"),
  SALES_TAX_AMOUNT(SystemUrls.HL7_ADJUDICATION, "CLM_LINE_SLS_TAX_AMT", "Sales Tax Amount"),
  PATIENT_LIABILITY_REDUCT_AMOUNT(
      SystemUrls.HL7_ADJUDICATION, "CLM_LINE_PLRO_AMT", "Sales Tax Amount"),
  LOW_INCOME_COST_SHARE_SUB_AMOUNT(
      SystemUrls.HL7_ADJUDICATION, "CLM_LINE_LIS_AMT", "Low Income Cost Sharing Subsidy Amount"),
  INGREDIENT_COST_AMOUNT(
      SystemUrls.HL7_ADJUDICATION, "CLM_LINE_INGRDNT_CST_AMT", "Ingredient Cost Amount"),
  GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
      "CLM_LINE_GRS_BLW_THRSHLD_AMT",
      "Gross Drug Cost Below Out Of Pocket Threshold"),
  GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT(
      SystemUrls.HL7_ADJUDICATION,
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

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudication(double value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(coding1System)
                        .setCode(coding1Code)
                        .setDisplay(coding1Display))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION)
                        .setCode(coding2Code)
                        .setDisplay(coding2Display)))
        .setAmount(USD.toFhir(value));
  }

  ExplanationOfBenefit.AdjudicationComponent toFhirAdjudicationRx(double value) {
    return new ExplanationOfBenefit.AdjudicationComponent()
        .setCategory(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION)
                        .setCode(coding1Code)
                        .setDisplay(coding1Display)))
        .setAmount(USD.toFhir(value));
  }

  ExplanationOfBenefit.TotalComponent toFhirTotal(double value) {
    return new ExplanationOfBenefit.TotalComponent()
        .setCategory(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(coding1System)
                        .setCode(coding1Code)
                        .setDisplay(coding1Display))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_ADJUDICATION)
                        .setCode(coding2Code)
                        .setDisplay(coding2Display)))
        .setAmount(USD.toFhir(value));
  }
}
