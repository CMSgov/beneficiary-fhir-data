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
    CLM_NRLN_RIC_CD("CLM_NRLN_RIC_CD", "Near Line Record Identification Code");

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
