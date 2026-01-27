package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

@AllArgsConstructor
enum BlueButtonSupportingInfoCategory {
  /** CLM_NCH_WKLY_PROC_DT - Weekly Process Date. */
  CLM_NCH_WKLY_PROC_DT("CLM_NCH_WKLY_PROC_DT", "Weekly Process Date"),
  /** CLM_BLOOD_PT_FRNSH_QTY - Blood Pints Furnished Quantity. */
  CLM_BLOOD_PT_FRNSH_QTY("CLM_BLOOD_PT_FRNSH_QTY", "Blood Pints Furnished Quantity"),
  /** CLM_MDCR_INSTNL_MCO_PD_SW - MCO Paid Switch. */
  CLM_MDCR_INSTNL_MCO_PD_SW("CLM_MDCR_INSTNL_MCO_PD_SW", "MCO Paid Switch"),
  /** CLM_MDCR_NCH_PTNT_STUS_IND_CD - Patient Status Code. */
  CLM_MDCR_NCH_PTNT_STUS_IND_CD("CLM_MDCR_NCH_PTNT_STUS_IND_CD", "Patient Status Code"),
  /** CLM_ACTV_CARE_THRU_DT - Covered Care Through Date. */
  CLM_ACTV_CARE_THRU_DT("CLM_ACTV_CARE_THRU_DT", "Covered Care Through Date"),
  /** CLM_NCVRD_FROM_DT - Noncovered Stay From Date. */
  CLM_NCVRD_FROM_DT("CLM_NCVRD_FROM_DT", "Noncovered Stay From Date"),
  /** CLM_NCVRD_THRU_DT - Noncovered Stay Through Date. */
  CLM_NCVRD_THRU_DT("CLM_NCVRD_THRU_DT", "Noncovered Stay Through Date"),
  /** CLM_MDCR_EXHSTD_DT - Medicare Benefits Exhausted Date. */
  CLM_MDCR_EXHSTD_DT("CLM_MDCR_EXHSTD_DT", "Medicare Benefits Exhausted Date"),
  /** CLM_PPS_IND_CD - Claim PPS Indicator Code. */
  CLM_PPS_IND_CD("CLM_PPS_IND_CD", "Claim PPS Indicator Code"),
  /** CLM_NCH_PRMRY_PYR_CD - NCH Primary Payer Code. */
  CLM_NCH_PRMRY_PYR_CD("CLM_NCH_PRMRY_PYR_CD", "NCH Primary Payer Code"),
  /** CLM_QLFY_STAY_FROM_DT - Qualified Stay From Date. */
  CLM_QLFY_STAY_FROM_DT("CLM_QLFY_STAY_FROM_DT", "Qualified Stay From Date"),
  /** CLM_QLFY_STAY_THRU_DT - Qualified Stay Thru Date. */
  CLM_QLFY_STAY_THRU_DT("CLM_QLFY_STAY_THRU_DT", "Qualified Stay Thru Date"),
  /** CLM_HHA_LUP_IND_CD - Claim Lupa Indicator Code. */
  CLM_HHA_LUP_IND_CD("CLM_HHA_LUP_IND_CD", "Claim Lupa Indicator Code"),
  /** CLM_HHA_RFRL_CD - Claim Referral Code. */
  CLM_HHA_RFRL_CD("CLM_HHA_RFRL_CD", "Claim Referral Code"),
  /** CLM_NRLN_RIC_CD - Near Line Record Identification Code. */
  CLM_NRLN_RIC_CD("CLM_NRLN_RIC_CD", "Near Line Record Identification Code"),
  /** CLM_CTSTRPHC_CVRG_IND_CD - Catastrophic Coverage Code. */
  CLM_CTSTRPHC_CVRG_IND_CD("CLM_CTSTRPHC_CVRG_IND_CD", "Catastrophic Coverage Code"),
  /** CLM_DRUG_CVRG_STUS_CD - Drug Coverage Status Code. */
  CLM_DRUG_CVRG_STUS_CD("CLM_DRUG_CVRG_STUS_CD", "Drug Coverage Status Code"),
  /** CLM_DSPNSNG_STUS_CD - Claim Dispensing Status Code. */
  CLM_DSPNSNG_STUS_CD("CLM_DSPNSNG_STUS_CD", "Claim Dispensing Status Code"),
  /** CLM_LTC_DSPNSNG_MTHD_CD - Submission clarification code. */
  CLM_LTC_DSPNSNG_MTHD_CD("CLM_LTC_DSPNSNG_MTHD_CD", "Submission clarification code"),
  /** CLM_PHRMCY_SRVC_TYPE_CD - Pharmacy service type code. */
  CLM_PHRMCY_SRVC_TYPE_CD("CLM_PHRMCY_SRVC_TYPE_CD", "Pharmacy service type code"),
  /** CLM_PTNT_RSDNC_CD - Patient Residence Code. */
  CLM_PTNT_RSDNC_CD("CLM_PTNT_RSDNC_CD", "Patient Residence Code"),
  /** CLM_LINE_RX_NUM - Claim Line Prescription Service Reference Number. */
  CLM_LINE_RX_NUM("CLM_LINE_RX_NUM", "Claim Line Prescription Service Reference Number"),
  /** CLM_RLT_COND_CD - Claim Related Condition Code. */
  CLM_RLT_COND_CD("CLM_RLT_COND_CD", "Claim Related Condition Code"),
  /** CLM_IDR_LD_DT - Claim IDR Load Date. */
  CLM_IDR_LD_DT("CLM_IDR_LD_DT", "Claim IDR Load Date"),
  /** CLM_CNTRCTR_NUM - Claim Contractor Number. */
  CLM_CNTRCTR_NUM("CLM_CNTRCTR_NUM", "Claim Contractor Number"),
  /** CLM_ADJSTMT_TYPE_CD - Claim Adjustment Type Code. */
  CLM_ADJSTMT_TYPE_CD("CLM_ADJSTMT_TYPE_CD", "Claim Adjustment Type Code"),
  /** CLM_DISP_CD - Claim Disposition Code. */
  CLM_DISP_CD("CLM_DISP_CD", "Claim Disposition Code"),
  /** CLM_CMS_PROC_DT - FI Claim Process Date. */
  CLM_CMS_PROC_DT("CLM_CMS_PROC_DT", "FI Claim Process Date"),
  /** CLM_CARR_PMT_DNL_CD - Claim Payment Denial Code. */
  CLM_CARR_PMT_DNL_CD("CLM_CARR_PMT_DNL_CD", "Claim Payment Denial Code"),
  /** CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW - Provider Assignment Indicator. */
  CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW("CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW", "Provider Assignment Indicator"),
  /** CLM_CLNCL_TRIL_NUM - Clinical Trial Number. */
  CLM_CLNCL_TRIL_NUM("CLM_CLNCL_TRIL_NUM", "Clinical Trial Number"),
  /** CLM_MDCR_NPMT_RSN_CD - Claim Non Payment Reason Code. */
  CLM_MDCR_NPMT_RSN_CD("CLM_MDCR_NPMT_RSN_CD", "Claim Non Payment Reason Code"),
  /** CLM_FI_ACTN_CD - Claim Fiscal Intermediary Action Code. */
  CLM_FI_ACTN_CD("CLM_FI_ACTN_CD", "Claim Fiscal Intermediary Action Code"),
  /** CLM_OP_SRVC_TYPE_CD - Claim Outpatient Service Type Code. */
  CLM_OP_SRVC_TYPE_CD("CLM_OP_SRVC_TYPE_CD", "Claim Outpatient Service Type Code"),
  /** CLM_QUERY_CD - Claim Query Code. */
  CLM_QUERY_CD("CLM_QUERY_CD", "Claim Query Code"),
  /** CLM_SBMT_FRMT_CD - Claim Submission Format Code. */
  CLM_SBMT_FRMT_CD("CLM_SBMT_FRMT_CD", "Claim Submission Format Code"),
  /** CLM_SBMTR_CNTRCT_NUM - Submitter Contract Number. */
  CLM_SBMTR_CNTRCT_NUM("CLM_SBMTR_CNTRCT_NUM", "Submitter Contract Number"),
  /** CLM_SBMTR_CNTRCT_PBP_NUM - Submitter Contract PBP Number. */
  CLM_SBMTR_CNTRCT_PBP_NUM("CLM_SBMTR_CNTRCT_PBP_NUM", "Submitter Contract PBP Number"),
  /** CLM_AUDT_TRL_STUS_CD - Claim Audit Trail Status Code. */
  CLM_AUDT_TRL_STUS_CD("CLM_AUDT_TRL_STUS_CD", "Claim Status Code");

  private final String code;
  private final String display;

  CodeableConcept toFhir() {
    return new CodeableConcept()
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_SUPPORTING_INFORMATION)
                .setCode(code)
                .setDisplay(display))
        .addCoding(
            new Coding()
                .setSystem(SystemUrls.HL7_CLAIM_INFORMATION)
                .setCode("info")
                .setDisplay("Information"));
  }
}
