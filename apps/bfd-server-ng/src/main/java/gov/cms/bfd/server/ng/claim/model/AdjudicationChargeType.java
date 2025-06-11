package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Getter
@AllArgsConstructor
public enum AdjudicationChargeType {
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
  SUBMITTED_CHARGE_AMOUNT(
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
      "Total Charge Amount");

  private final String coding1System;
  private final String coding1Code;
  private final String coding1Display;
  private final String coding2Code;
  private final String coding2Display;

  ExplanationOfBenefit.AdjudicationComponent toFhir(float value) {
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
        .setAmount(new USD().setValue(value));
  }
}
