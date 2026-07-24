package gov.cms.bfd.server.ng.claim.model;

import static gov.cms.bfd.server.ng.claim.model.BlueButtonSupportingInfoCategory.CLM_AUDT_TRL_STUS_CD;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Claim audit trail status codes mapped from CLM_AUDT_TRL_STUS_CD. NOTE: This enum derives a
 * composite code using source, audit trail status code, and for VMS's case, audit trail location
 * code. This is because there exists some overlap between MCS and FISS in which descriptions
 * differ. Also, VMS is dependent upon location code so descriptions differ based off status code
 * and location code combinations here.
 */
@Getter
@AllArgsConstructor
@SuppressWarnings("java:S1192")
public enum ClaimAuditTrailStatusCode {

  /** FISS - A - Accept. */
  FISS_A(MetaSourceSk.FISS, "A", ClaimAuditTrailLocationCode.NA, "Accept"),
  /**
   * FISS - F - Suspended online; the system adds the claim record to the file with active errors.
   */
  FISS_F(
      MetaSourceSk.FISS,
      "F",
      ClaimAuditTrailLocationCode.NA,
      "Suspended online; the system adds the claim record to the file with active errors."),
  /** FISS - S - A manual update is needed before the claim processing can continue. */
  S(
      MetaSourceSk.FISS,
      "S",
      ClaimAuditTrailLocationCode.NA,
      "A manual update is needed before the claim processing can continue."),
  /**
   * FISS - M - Designates a manual claim move to either another department, employee, desk, etc.
   * Note: Once an 'M' value is inserted in the Status field, the system changes the status to an
   * 'S' for suspense after the move.
   */
  FISS_M(
      MetaSourceSk.FISS,
      "M",
      ClaimAuditTrailLocationCode.NA,
      "Designates a manual claim move to either another department, employee, desk, etc. Note: Once an 'M' value is inserted in the Status field, the system changes the status to an 'S' for suspense after the move."),
  /** FISS - D - Claim has reached final disposition with no reimbursement (medical denial). */
  FISS_D(
      MetaSourceSk.FISS,
      "D",
      ClaimAuditTrailLocationCode.NA,
      "Claim has reached final disposition with no reimbursement (medical denial)."),
  /** FISS - P - Claim has reached final disposition with reimbursement. */
  FISS_P(
      MetaSourceSk.FISS,
      "P",
      ClaimAuditTrailLocationCode.NA,
      "Claim has reached final disposition with reimbursement."),
  /** FISS - R - Claim has reached final disposition with no reimbursement (non-medical reject). */
  FISS_R(
      MetaSourceSk.FISS,
      "R",
      ClaimAuditTrailLocationCode.NA,
      "Claim has reached final disposition with no reimbursement (non-medical reject)."),
  /**
   * FISS - T - Claim has reached final disposition with no reimbursement and has been returned to
   * the provider with billing errors.
   */
  T(
      MetaSourceSk.FISS,
      "T",
      ClaimAuditTrailLocationCode.NA,
      "Claim has reached final disposition with no reimbursement and has been returned to the provider with billing errors."),
  /** FISS - I - Claim moves from the active processing file to the inactive file. */
  I(
      MetaSourceSk.FISS,
      "I",
      ClaimAuditTrailLocationCode.NA,
      "Claim moves from the active processing file to the inactive file."),
  /**
   * FISS - U - Claim has reached final disposition and has been returned to the Peer Review
   * Organization for corrections.
   */
  FISS_U(
      MetaSourceSk.FISS,
      "U",
      ClaimAuditTrailLocationCode.NA,
      "Claim has reached final disposition and has been returned to the Peer Review Organization for corrections."),
  /**
   * MCS - A - Current active claim. This is an internal MCS MPAP status, and will only display
   * online in related history.
   */
  A(
      MetaSourceSk.MCS,
      "A",
      ClaimAuditTrailLocationCode.NA,
      "Current active claim. This is an internal MCS MPAP status, and will only display online in related history."),
  /**
   * MCS - B - Suspended. All pending claims will show this status when they are viewed online. All
   * other pending claim statuses are used internally by MCS MPAP only.
   */
  B(
      MetaSourceSk.MCS,
      "B",
      ClaimAuditTrailLocationCode.NA,
      "Suspended. All pending claims will show this status when they are viewed online. All other pending claim statuses are used internally by MCS MPAP only."),
  /**
   * MCS - C - Approved awaiting CWF response through MPAP, claim processed with no outstanding
   * edits/audits through MPAP and queried.
   */
  C(
      MetaSourceSk.MCS,
      "C",
      ClaimAuditTrailLocationCode.NA,
      "Approved awaiting CWF response through MPAP, claim processed with no outstanding edits/audits through MPAP and queried."),
  /** MCS - D - Approved and paid; CAP physician no pay detail lines. */
  MCS_D(
      MetaSourceSk.MCS,
      "D",
      ClaimAuditTrailLocationCode.NA,
      "Approved and paid; CAP physician no pay detail lines."),
  /**
   * MCS - E - Denied; set based on the history usage indicator on the AA segment of a denial edit
   * or audit.
   */
  E(
      MetaSourceSk.MCS,
      "E",
      ClaimAuditTrailLocationCode.NA,
      "Denied; set based on the history usage indicator on the AA segment of a denial edit or audit."),
  /**
   * MCS - F - Full claim refund - EGHP, used only when an EGHP accounts receivable has been
   * satisfied (RG type AR). This status is for display purposes only, internally the claim status
   * would be a ‘y’.
   */
  F(
      MetaSourceSk.MCS,
      "F",
      ClaimAuditTrailLocationCode.NA,
      "Full claim refund - EGHP, used only when an EGHP accounts receivable has been satisfied (RG type AR). This status is for display purposes only, internally the claim status would be a ‘y’."),
  /**
   * MCS - G refund applied, partial refund was calculated for the claim but was applied to
   * something else outstanding, such as an AR.
   */
  G(
      MetaSourceSk.MCS,
      "G",
      ClaimAuditTrailLocationCode.NA,
      "Partial refund applied, partial refund was calculated for the claim but was applied to something else outstanding, such as an AR."),
  /**
   * MCS - J - Claim still active. This is an internal MCS MPAP status, and will never display
   * online.
   */
  J(
      MetaSourceSk.MCS,
      "J",
      ClaimAuditTrailLocationCode.NA,
      "Claim still active. This is an internal MCS MPAP status, and will never display online."),
  /**
   * MCS - K - Claim in pending suspense, used as history for duplicate audits but not MPAP
   * (relationship or negative relationship audit). This is an internal MCS MPAP status, and does
   * not display online except as related history or on a bene research document (BRD). The status
   * can be set several ways: claim has no significant claim-level audit failed, but has claim-level
   * edit suspense (does not look at history usage on edit) claim or detail suspends for a post-CWF
   * audit detail suspends with ‘f’ disposition audit that has a history usage of ‘3’.
   */
  K(
      MetaSourceSk.MCS,
      "K",
      ClaimAuditTrailLocationCode.NA,
      "Claim in pending suspense, used as history for duplicate audits but not MPAP (relationship or negative relationship audit). This is an internal MCS MPAP status, and does not display online except as related history or on a bene research document (BRD). The status can be set several ways: claim has no significant claim-level audit failed, but has claim-level edit suspense (does not look at history usage on edit) claim or detail suspends for a post-CWF audit detail suspends with ‘f’ disposition audit that has a history usage of ‘3’"),
  /**
   * MCS - L - CWF suspense, no MPAP, the HIC change trailer on the claim has a different
   * cross-reference HIC than the ‘h’ trailer on eligibility. Note: the ‘L’ status is an internal
   * status and will not appear on a claim. While the HIC is being changed, the claim will have an
   * ‘L’ status until the change is complete.
   */
  L(
      MetaSourceSk.MCS,
      "L",
      ClaimAuditTrailLocationCode.NA,
      "CWF suspense, no MPAP, the HIC change trailer on the claim has a different cross-reference HIC than the ‘h’ trailer on eligibility. Note: the ‘L’ status is an internal status and will not appear on a claim. While the HIC is being changed, the claim will have an ‘L’ status until the change is complete."),
  /** MCS - M - Approved and paid (includes all deductible) - currently not used. */
  M(
      MetaSourceSk.MCS,
      "M",
      ClaimAuditTrailLocationCode.NA,
      "Approved and paid (includes all deductible) - currently not used."),
  /**
   * MCS - N - Denied for payment (excludes deductible), set based on the history usage (AA segment)
   * indicator on a denial edit or audit.
   */
  N(
      MetaSourceSk.MCS,
      "N",
      ClaimAuditTrailLocationCode.NA,
      "Denied for payment (excludes deductible), set based on the history usage (AA segment) indicator on a denial edit or audit."),
  /**
   * MCS - P claim refund - EGHP, used only when an EGHP accounts receivable has been satisfied (RG
   * type AR). This status is for display purposes only, internally the claim status would be a ‘g’.
   */
  MCS_P(
      MetaSourceSk.MCS,
      "P",
      ClaimAuditTrailLocationCode.NA,
      "Partial claim refund - EGHP, used only when an EGHP accounts receivable has been satisfied (RG type AR). This status is for display purposes only, internally the claim status would be a ‘g’."),
  /** MCS - Q - Adjusted - claim has been replaced by a full claim adjustment. */
  Q(
      MetaSourceSk.MCS,
      "Q",
      ClaimAuditTrailLocationCode.NA,
      "Adjusted - claim has been replaced by a full claim adjustment."),
  /**
   * MCS - R - Claim has been deleted from the system. When a claim is deleted (transferred to
   * location 090), the claim status and the detail status are both set to ‘R’.
   */
  MCS_R(
      MetaSourceSk.MCS,
      "R",
      ClaimAuditTrailLocationCode.NA,
      "Claim has been deleted from the system. When a claim is deleted (transferred to location 090), the claim status and the detail status are both set to ‘R’."),
  /** MCS - U - Paid but not for dup use - currently not used. */
  U(
      MetaSourceSk.MCS,
      "U",
      ClaimAuditTrailLocationCode.NA,
      "Paid but not for dup use - currently not used."),
  /**
   * MCS - V - Denied, but not for dup use, set based on the history usage indicator on the AA
   * segment of a denial edit or audit.
   */
  V(
      MetaSourceSk.MCS,
      "V",
      ClaimAuditTrailLocationCode.NA,
      "Denied, but not for dup use, set based on the history usage indicator on the AA segment of a denial edit or audit."),
  /**
   * MCS - w - Rejected. This status is set for Assigned and Non-Assigned claims, based on the
   * receipt date, the bene submission form, and the reject indicator which is MSG ACTION = RJ or R2
   * on the Narrative Message Usage file (NA). If all details are rejected (status ‘W’) then the
   * claim status is set to rejected (‘W’).
   */
  W(
      MetaSourceSk.MCS,
      "W",
      ClaimAuditTrailLocationCode.NA,
      "Rejected. This status is set for Assigned and Non-Assigned claims, based on the receipt date, the bene submission form, and the reject indicator which is MSG ACTION = RJ or R2 on the Narrative Message Usage file (NA). If all details are rejected (status ‘W’) then the claim status is set to rejected (‘W’)."),
  /** MCS - x refund, claim that is a partial void and a split pay. */
  X(
      MetaSourceSk.MCS,
      "X",
      ClaimAuditTrailLocationCode.NA,
      "Partial refund, claim that is a partial void and a split pay."),
  /** MCS - Y - Full refund, full amount of claim payment was returned. */
  Y(
      MetaSourceSk.MCS,
      "Y",
      ClaimAuditTrailLocationCode.NA,
      "Full refund, full amount of claim payment was returned."),
  /** MCS - Z - Voided, full void has been issued for the claim. */
  Z(
      MetaSourceSk.MCS,
      "Z",
      ClaimAuditTrailLocationCode.NA,
      "Voided, full void has been issued for the claim."),
  /**
   * MCS - 1 - Current active claim, separate history. This is an internal MCS MPAP status, and will
   * only display online in related history. This status applies to the header of claims that
   * contain a demonstration number that has been flagged for separate history in the HXXTDEMO
   * table.
   */
  _1(
      MetaSourceSk.MCS,
      "1",
      ClaimAuditTrailLocationCode.NA,
      "Current active claim, separate history. This is an internal MCS MPAP status, and will only display online in related history. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),
  /**
   * MCS - 2 - Suspended, separate history. All pending claims will show this status when they are
   * viewed online. All other pending claim statuses are used internally by MCS MPAP only. This
   * status applies to the header of claims that contain a demonstration number that has been
   * flagged for separate history in the HXXTDEMO table.
   */
  _2(
      MetaSourceSk.MCS,
      "2",
      ClaimAuditTrailLocationCode.NA,
      "Suspended, separate history. All pending claims will show this status when they are viewed online. All other pending claim statuses are used internally by MCS MPAP only. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),
  /**
   * MCS - 3 - Approved awaiting CWF response, separate history, through MPAP, claim processed with
   * no outstanding edits/audits through MPAP and queried. This status applies to the header of
   * claims that contain a demonstration number that has been flagged for separate history in the
   * HXXTDEMO table.
   */
  _3(
      MetaSourceSk.MCS,
      "3",
      ClaimAuditTrailLocationCode.NA,
      "Approved awaiting CWF response, separate history, through MPAP, claim processed with no outstanding edits/audits through MPAP and queried. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),
  /**
   * MCS - 4 - Approved and paid, separate history. This status applies to the header of claims that
   * contain a demonstration number that has been flagged for separate history in the HXXTDEMO
   * table.
   */
  _4(
      MetaSourceSk.MCS,
      "4",
      ClaimAuditTrailLocationCode.NA,
      "Approved and paid, separate history. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),
  /**
   * MCS - 5 - Denied; separate history, set based on the history usage indicator on the AA segment
   * of a denial edit or audit. This status applies to the header of claims that contain a
   * demonstration number that has been flagged for separate history in the HXXTDEMO table.
   */
  _5(
      MetaSourceSk.MCS,
      "5",
      ClaimAuditTrailLocationCode.NA,
      "Denied; separate history, set based on the history usage indicator on the AA segment of a denial edit or audit. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),
  /** MCS - 6 - ERROR - Not Used. */
  _6(MetaSourceSk.MCS, "6", ClaimAuditTrailLocationCode.NA, "Not Used"),
  /**
   * MCS - 8 - Claim moved to another HIC. Claim was submitted and finalized for a HIC prior to the
   * HIC being changed. (This status is internal to MCS only and will not display online.)
   */
  _8(
      MetaSourceSk.MCS,
      "8",
      ClaimAuditTrailLocationCode.NA,
      "Claim moved to another HIC. Claim was submitted and finalized for a HIC prior to the HIC being changed. (This status is internal to MCS only and will not display online.)"),
  /**
   * MCS - 9 - Claim deleted from system. Claim deleted from MPAP due to rework, the ICN is a
   * duplicate of another ICN in the system. (This status is internal to MCS only and will not
   * display online.)
   */
  _9(
      MetaSourceSk.MCS,
      "9",
      ClaimAuditTrailLocationCode.NA,
      "Claim deleted from system. Claim deleted from MPAP due to rework, the ICN is a duplicate of another ICN in the system. (This status is internal to MCS only and will not display online.)"),
  /** VMS - 00 - 08 - Void/Entry Code 3 Claim. */
  _00(MetaSourceSk.VMS, "00", ClaimAuditTrailLocationCode.QUERY, "Void/Entry Code 3 Claim"),
  /**
   * VMS - 01 - 05 - outcome determined by v - TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims
   * suspend to 09/27).
   */
  _01_05(
      MetaSourceSk.VMS,
      "01",
      ClaimAuditTrailLocationCode.EDIT,
      "TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims suspend to 09/27)"),
  /** VMS - 01 - 09 - TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims suspend to 09/27). */
  _01_09(
      MetaSourceSk.VMS,
      "01",
      ClaimAuditTrailLocationCode.REPLY,
      "TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims suspend to 09/27)"),
  /** VMS - 01 - 07 - MSP Cost Avoid. */
  _01_07(
      MetaSourceSk.VMS,
      "01",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "MSP Cost Avoid"),
  /** VMS - 02 - 07 - MSP Denied Lines. */
  _02_07(
      MetaSourceSk.VMS,
      "02",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "MSP Denied Lines"),
  /**
   * VMS - 02 - 09 - MSP claims that received CWF edit 6819 and had non-GHP MSP prior to querying
   * CWF.
   */
  _02_09(
      MetaSourceSk.VMS,
      "02",
      ClaimAuditTrailLocationCode.REPLY,
      "MSP claims that received CWF edit 6819 and had non-GHP MSP prior to querying CWF"),
  /** VMS - 03 - 06 - Purged. */
  _03_06(MetaSourceSk.VMS, "03", ClaimAuditTrailLocationCode.REASONABLE_CHARGE, "Purged"),
  /** VMS - 03 - 07 - MSP Split Claims. */
  _03_07(
      MetaSourceSk.VMS,
      "03",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "MSP Split Claims"),
  /**
   * VMS - 03 - 09 - MSP claims that received CWF edit 6819 and did not have non-GHP MSP prior to
   * querying CWF.
   */
  _03_09(
      MetaSourceSk.VMS,
      "03",
      ClaimAuditTrailLocationCode.REPLY,
      "MSP claims that received CWF edit 6819 and did not have non-GHP MSP prior to querying CWF"),
  /** VMS - 04 - 05 - Clean claim (ready to adjudicate). */
  _04_05(
      MetaSourceSk.VMS,
      "04",
      ClaimAuditTrailLocationCode.EDIT,
      "Clean claim (ready to adjudicate)"),
  /** VMS - 05 - 05 - Line item error. */
  _05_05(MetaSourceSk.VMS, "05", ClaimAuditTrailLocationCode.EDIT, "Line item error"),
  /** VMS - 05 - 06 - Line item error. */
  _05_06(MetaSourceSk.VMS, "05", ClaimAuditTrailLocationCode.REASONABLE_CHARGE, "Line item error"),
  /** VMS - 05 - 09 - VMS Action Code review. */
  _05_09(
      MetaSourceSk.VMS,
      "05",
      ClaimAuditTrailLocationCode.REPLY,
      """
        If the system cannot identify a VMS Action Code; the claim suspends to this location/status for review. You need to verify that the FPS Model Number on each claim line appears on the VMAP/4C/ACFPWALK table. If the FPS Model Number/Action Code combination is not on the table, update the table according to the TDL issued by CMS that introduced the FPS Model. After updating the table, deny the claim line or lines as follows:
        • Type the Action Code for the FPS Model Number on the claim line or lines.
        • Ensure that the Allowed Amount on the claim is zero.
        • Type R in the Claim Review Code field.
        Refer to entries for FPSD and FPSH in the APEX Reference Manual in the chapter on “Common Working File (CWF) Codes” for additional information.
        """),
  /** VMS - 06 - 06 - Provider problem. */
  _06_06(MetaSourceSk.VMS, "06", ClaimAuditTrailLocationCode.REASONABLE_CHARGE, "Provider problem"),
  /** VMS - 07 - 06 - Medical consultation. */
  _07_06(
      MetaSourceSk.VMS,
      "07",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      "Medical consultation"),
  /** VMS - 08 - 05 - Edit error. */
  _08_05(MetaSourceSk.VMS, "08", ClaimAuditTrailLocationCode.EDIT, "Edit error"),
  /** VMS - 08 - 06 - Edit error. */
  _08_06(MetaSourceSk.VMS, "08", ClaimAuditTrailLocationCode.REASONABLE_CHARGE, "Edit error"),
  /** VMS - 09 - 05 - Specialty examination. */
  _09_05(MetaSourceSk.VMS, "09", ClaimAuditTrailLocationCode.EDIT, "Specialty examination"),
  /** VMS - 09 - 06 - MSP with a primary paid amount from the primary payer. */
  _09_06(
      MetaSourceSk.VMS,
      "09",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      "MSP with a primary paid amount from the primary payer"),
  /** VMS - 10 - 04 - Delete. */
  _10_04(MetaSourceSk.VMS, "10", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Delete"),
  /** VMS - 11 - 05 - Claim referred to supervisor. */
  _11_05(MetaSourceSk.VMS, "11", ClaimAuditTrailLocationCode.EDIT, "Claim referred to supervisor"),
  /** VMS - 12 - 02 - MSP first letter initiated. */
  _12_02(
      MetaSourceSk.VMS,
      "12",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      "MSP first letter initiated"),
  /** VMS - 13 - 02 - Suspense – Other. */
  _13_02(MetaSourceSk.VMS, "13", ClaimAuditTrailLocationCode.DEVELOPMENT, "Suspense – Other"),
  /** VMS - 14 - 04 - Suspense – DME. */
  _14_04(MetaSourceSk.VMS, "14", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Suspense – DME"),
  /** VMS - 14 - 05 - Suspense – DME. */
  _14_05(MetaSourceSk.VMS, "14", ClaimAuditTrailLocationCode.EDIT, "Suspense – DME"),
  /** VMS - 15 - 05 - Chiropractor claim. */
  _15_05(MetaSourceSk.VMS, "15", ClaimAuditTrailLocationCode.EDIT, "Chiropractor claim"),
  /** VMS - 16 - 02 - MSP first letter sent. */
  _16_02(MetaSourceSk.VMS, "16", ClaimAuditTrailLocationCode.DEVELOPMENT, "MSP first letter sent"),
  /** VMS - 17 - 01 - Activated; not entered. */
  _17_01(
      MetaSourceSk.VMS, "17", ClaimAuditTrailLocationCode.PRE_COMPUTER, "Activated; not entered"),
  /** VMS - 18 - 02 - Utilization review. */
  _18_02(MetaSourceSk.VMS, "18", ClaimAuditTrailLocationCode.DEVELOPMENT, "Utilization review"),
  /** VMS - 18 - 07 - Utilization review. */
  _18_07(
      MetaSourceSk.VMS,
      "18",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Utilization review"),
  /** VMS - 18 - 09 - Utilization review. */
  _18_09(MetaSourceSk.VMS, "18", ClaimAuditTrailLocationCode.REPLY, "Utilization review"),
  /** VMS - 19 - 07 - Third level review (prior history review). */
  _19_07(
      MetaSourceSk.VMS,
      "19",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Third level review (prior history review)"),
  /** VMS - 20 - 05 - Reject name/sex. */
  _20_05(MetaSourceSk.VMS, "20", ClaimAuditTrailLocationCode.EDIT, "Reject name/sex"),
  /** VMS - 21 - 04 - Adjustment. */
  _21_04(MetaSourceSk.VMS, "21", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Adjustment"),
  /** VMS - 22 - 05 - Entitlement termination; quality control. */
  _22_05(
      MetaSourceSk.VMS,
      "22",
      ClaimAuditTrailLocationCode.EDIT,
      "Entitlement termination; quality control"),
  /** VMS - 23 - 05 - No beneficiary address. */
  _23_05(MetaSourceSk.VMS, "23", ClaimAuditTrailLocationCode.EDIT, "No beneficiary address"),
  /** VMS - 24 - 04 - Beneficiary BUDS01 record closed. */
  _24_04(
      MetaSourceSk.VMS,
      "24",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      "Beneficiary BUDS01 record closed"),
  /** VMS - 24 - 05 - Beneficiary BUDS01 record closed. */
  _24_05(
      MetaSourceSk.VMS, "24", ClaimAuditTrailLocationCode.EDIT, "Beneficiary BUDS01 record closed"),
  /** VMS - 24 - 09 - Beneficiary BUDS01 record closed. */
  _24_09(
      MetaSourceSk.VMS,
      "24",
      ClaimAuditTrailLocationCode.REPLY,
      "Beneficiary BUDS01 record closed"),
  /** VMS - 25 - 08 - Representative payee. */
  _25_08(MetaSourceSk.VMS, "25", ClaimAuditTrailLocationCode.QUERY, "Representative payee"),
  /** VMS - 25 - 09 - Representative payee. */
  _25_09(MetaSourceSk.VMS, "25", ClaimAuditTrailLocationCode.REPLY, "Representative payee"),
  /** VMS - 26 - 08 - Welfare; Disposition Code 42. */
  _26_08(MetaSourceSk.VMS, "26", ClaimAuditTrailLocationCode.QUERY, "Welfare; Disposition Code 42"),
  /** VMS - 26 - 09 - Welfare; Disposition Code 42. */
  _26_09(MetaSourceSk.VMS, "26", ClaimAuditTrailLocationCode.REPLY, "Welfare; Disposition Code 42"),
  /** VMS - 27 - 08 - Services prior to entitlement (HMO for Jurisdiction D only). */
  _27_08(
      MetaSourceSk.VMS,
      "27",
      ClaimAuditTrailLocationCode.QUERY,
      "Services prior to entitlement (HMO for Jurisdiction D only)"),
  /** VMS - 27 - 09 - Services prior to entitlement (HMO for Jurisdiction D only). */
  _27_09(
      MetaSourceSk.VMS,
      "27",
      ClaimAuditTrailLocationCode.REPLY,
      "Services prior to entitlement (HMO for Jurisdiction D only)"),
  /** VMS - 28 - 04 - Mass adjustment suspensions. */
  _28_04(
      MetaSourceSk.VMS,
      "28",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      "Mass adjustment suspensions"),
  /** VMS - 29 - 06 - Missing data. */
  _29_06(MetaSourceSk.VMS, "29", ClaimAuditTrailLocationCode.REASONABLE_CHARGE, "Missing data"),
  /** VMS - 30 - 04 - Estimated interest errors. */
  _30_04(
      MetaSourceSk.VMS,
      "30",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      "Location/status 04/30 is for estimated interest errors. Batch adjudication program VMSCW273 generates this location/status prior to sending the claim to CWF, based on the absence of valid data in certain fields on the claim. These fields include: the date of receipt, the estimated mail date, the amount paid to the provider, the amount paid to the beneficiary, the provider participation indicator, and the provider specialty."),
  /** VMS - 30 - 05 - Estimated interest errors. */
  _30_05(
      MetaSourceSk.VMS,
      "30",
      ClaimAuditTrailLocationCode.EDIT,
      "Location/status 04/30 is for estimated interest errors. Batch adjudication program VMSCW273 generates this location/status prior to sending the claim to CWF, based on the absence of valid data in certain fields on the claim. These fields include: the date of receipt, the estimated mail date, the amount paid to the provider, the amount paid to the beneficiary, the provider participation indicator, and the provider specialty."),
  /** VMS - 31 - 05 - "". */
  _31_05(MetaSourceSk.VMS, "31", ClaimAuditTrailLocationCode.EDIT, ""),
  /** VMS - 33 - 05 - Reasonable charge. */
  _33_05(MetaSourceSk.VMS, "33", ClaimAuditTrailLocationCode.EDIT, "Reasonable charge"),
  /** VMS - 34 - 05 - Physician inactive/missing. */
  _34_05(MetaSourceSk.VMS, "34", ClaimAuditTrailLocationCode.EDIT, "Physician inactive/missing"),
  /** VMS - 35 - 05 - Physician utilization. */
  _35_05(MetaSourceSk.VMS, "35", ClaimAuditTrailLocationCode.EDIT, "Physician utilization"),
  /** VMS - 36 - 02 - MSP first letter follow-up. */
  _36_02(
      MetaSourceSk.VMS,
      "36",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      "MSP first letter follow-up"),
  /** VMS - 37 - 07 - Duplicate suspect. */
  _37_07(
      MetaSourceSk.VMS,
      "37",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Duplicate suspect"),
  /** VMS - 38 - 07 - Beneficiary utilization - mandatory assignment for drugs/biologicals. */
  _38_07(
      MetaSourceSk.VMS,
      "38",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Beneficiary utilization - mandatory assignment for drugs/biologicals"),
  /** VMS - 39 - 02 - Beneficiary information. */
  _39_02(
      MetaSourceSk.VMS, "39", ClaimAuditTrailLocationCode.DEVELOPMENT, "Beneficiary information"),
  /** VMS - 39 - 07 - Rebundled claims (Jurisdictions A, B, &amp; C). */
  _39_07(
      MetaSourceSk.VMS,
      "39",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Rebundled claims (Jurisdictions A, B, & C)"),
  /** VMS - 40 - 09 - Premium arrearage; V trailer. */
  _40_09(MetaSourceSk.VMS, "40", ClaimAuditTrailLocationCode.REPLY, "Premium arrearage; V trailer"),
  /** VMS - 41 - 08 - New jurisdiction; E trailer; Disposition Code 40. */
  _41_08(
      MetaSourceSk.VMS,
      "41",
      ClaimAuditTrailLocationCode.QUERY,
      "New jurisdiction; E trailer; Disposition Code 40"),
  /** VMS - 41 - 09 - New jurisdiction; E trailer; Disposition Code 40. */
  _41_09(
      MetaSourceSk.VMS,
      "41",
      ClaimAuditTrailLocationCode.REPLY,
      "New jurisdiction; E trailer; Disposition Code 40"),
  /** VMS - 42 - 08 - Unique for CWF resubmits – deny after 4 or 20 days, as appropriate. */
  _42_08(
      MetaSourceSk.VMS,
      "42",
      ClaimAuditTrailLocationCode.QUERY,
      "Unique for CWF resubmits – deny after 4 or 20 days, as appropriate"),
  /** VMS - 42 - 09 - Unique for CWF resubmits – deny after 4 or 20 days, as appropriate. */
  _42_09(
      MetaSourceSk.VMS,
      "42",
      ClaimAuditTrailLocationCode.REPLY,
      "Unique for CWF resubmits – deny after 4 or 20 days, as appropriate"),
  /** VMS - 43 - 08 - Reply Disposition Code 43. */
  _43_08(MetaSourceSk.VMS, "43", ClaimAuditTrailLocationCode.QUERY, "Reply Disposition Code 43"),
  /** VMS - 43 - 09 - Reply Disposition Code 43. */
  _43_09(MetaSourceSk.VMS, "43", ClaimAuditTrailLocationCode.REPLY, "Reply Disposition Code 43"),
  /** VMS - 44 - 02 - MSP automated development. */
  _44_02(
      MetaSourceSk.VMS, "44", ClaimAuditTrailLocationCode.DEVELOPMENT, "MSP automated development"),
  /** VMS - 44 - 08 - MSP automated development. */
  _44_08(MetaSourceSk.VMS, "44", ClaimAuditTrailLocationCode.QUERY, "MSP automated development"),
  /** VMS - 45 - 08 - Name error. */
  _45_08(MetaSourceSk.VMS, "45", ClaimAuditTrailLocationCode.QUERY, "Name error"),
  /** VMS - 45 - 09 - Name error. */
  _45_09(MetaSourceSk.VMS, "45", ClaimAuditTrailLocationCode.REPLY, "Name error"),
  /** VMS - 46 - 03 - Normal DME record (Cert). */
  _46_03(MetaSourceSk.VMS, "46", ClaimAuditTrailLocationCode.DME_OQC, "Normal DME record (Cert)"),
  /**
   * VMS - 47 - 07 - Re-suspend the claim from a UR LL/SS; AC operator did not type review code U
   * showing UR review is complete.
   */
  _47_07(
      MetaSourceSk.VMS,
      "47",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Re-suspend the claim from a UR LL/SS; AC operator did not type review code U showing UR review is complete"),
  /** VMS - 47 - 09 - New HICN; C trailer. */
  _47_09(MetaSourceSk.VMS, "47", ClaimAuditTrailLocationCode.REPLY, "New HICN; C trailer"),
  /**
   * VMS - 48 - 07 - Re-suspend the claim from a UR LL/SS; the PSC/ZPIC operator did not type review
   * code U showing UR review is complete.
   */
  _48_07(
      MetaSourceSk.VMS,
      "48",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Re-suspend the claim from a UR LL/SS; the PSC/ZPIC operator did not type review code U showing UR review is complete"),
  /** VMS - 48 - 09 - Worker’s Compensation; Y trailer. */
  _48_09(
      MetaSourceSk.VMS,
      "48",
      ClaimAuditTrailLocationCode.REPLY,
      "Worker’s Compensation; Y trailer"),
  /** VMS - 49 - 07 - Rebundling (Jurisdiction D only). */
  _49_07(
      MetaSourceSk.VMS,
      "49",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Rebundling (Jurisdiction D only)"),
  /** VMS - 49 - 09 - Reject Travelers, RRB, or UMW. */
  _49_09(
      MetaSourceSk.VMS, "49", ClaimAuditTrailLocationCode.REPLY, "Reject Travelers, RRB, or UMW"),
  /** VMS - 50 - 03 - Stale cert – automated DME. */
  _50_03(MetaSourceSk.VMS, "50", ClaimAuditTrailLocationCode.DME_OQC, "Stale cert – automated DME"),
  /** VMS - 50 - 05 - Stale cert – automated DME. */
  _50_05(MetaSourceSk.VMS, "50", ClaimAuditTrailLocationCode.EDIT, "Stale cert – automated DME"),
  /** VMS - 51 - 03 - Stop cert – automated DME. */
  _51_03(MetaSourceSk.VMS, "51", ClaimAuditTrailLocationCode.DME_OQC, "Stop cert – automated DME"),
  /** VMS - 52 - 05 - Reasonable charge error. */
  _52_05(MetaSourceSk.VMS, "52", ClaimAuditTrailLocationCode.EDIT, "Reasonable charge error"),
  /** VMS - 52 - 06 - Reasonable charge error. */
  _52_06(
      MetaSourceSk.VMS,
      "52",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      "Reasonable charge error"),
  /** VMS - 53 - 05 - No cert on file – automated DME. */
  _53_05(
      MetaSourceSk.VMS, "53", ClaimAuditTrailLocationCode.EDIT, "No cert on file – automated DME"),
  /** VMS - 54 - 08 - Alien no pay. */
  _54_08(MetaSourceSk.VMS, "54", ClaimAuditTrailLocationCode.QUERY, "Alien no pay"),
  /** VMS - 55 - 09 - Hospice involvement. */
  _55_09(MetaSourceSk.VMS, "55", ClaimAuditTrailLocationCode.REPLY, "Hospice involvement"),
  /** VMS - 56 - 08 - Adjustment claim error/09 entry code. */
  _56_08(
      MetaSourceSk.VMS,
      "56",
      ClaimAuditTrailLocationCode.QUERY,
      "Adjustment claim error/09 entry code"),
  /** VMS - 56 - 09 - Adjustment claim error/09 entry code. */
  _56_09(
      MetaSourceSk.VMS,
      "56",
      ClaimAuditTrailLocationCode.REPLY,
      "Adjustment claim error/09 entry code"),
  /** VMS - 57 - 02 - Initiate MSP development. */
  _57_02(
      MetaSourceSk.VMS, "57", ClaimAuditTrailLocationCode.DEVELOPMENT, "Initiate MSP development"),
  /** VMS - 57 - 05 - Initiate MSP development. */
  _57_05(MetaSourceSk.VMS, "57", ClaimAuditTrailLocationCode.EDIT, "Initiate MSP development"),
  /** VMS - 57 - 07 - LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers. */
  _57_07(
      MetaSourceSk.VMS,
      "57",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers"),
  /** VMS - 57 - 09 - LMRP/NCD denial by CWF that is missing LMRP/NCD numbers. */
  _57_09(
      MetaSourceSk.VMS,
      "57",
      ClaimAuditTrailLocationCode.REPLY,
      "LMRP/NCD denial by CWF that is missing LMRP/NCD numbers"),
  /** VMS - 58 - 07 - LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers. */
  _58_07(
      MetaSourceSk.VMS,
      "58",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers"),
  /** VMS - 58 - 02 - Generate the MSP letter. */
  _58_02(
      MetaSourceSk.VMS, "58", ClaimAuditTrailLocationCode.DEVELOPMENT, "Generate the MSP letter"),
  /** VMS - 59 - 02 - Eligible for denial for MSP. */
  _59_02(
      MetaSourceSk.VMS,
      "59",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      "Eligible for denial for MSP"),
  /** VMS - 59 - 05 - Eligible for denial for MSP. */
  _59_05(MetaSourceSk.VMS, "59", ClaimAuditTrailLocationCode.EDIT, "Eligible for denial for MSP"),
  /** VMS - 60 - 04 - Excess history. */
  _60_04(MetaSourceSk.VMS, "60", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Excess history"),
  /** VMS - 61 - 04 - Beneficiary paid. */
  _61_04(MetaSourceSk.VMS, "61", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Beneficiary paid"),
  /** VMS - 62 - 04 - System error. */
  _62_04(MetaSourceSk.VMS, "62", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "System error"),
  /** VMS - 62 - 09 - Address formatting error from CWF. */
  _62_09(
      MetaSourceSk.VMS,
      "62",
      ClaimAuditTrailLocationCode.REPLY,
      "When a claim/CMN has an address that CWF cannot format, CWF returns it with Trailer 12. If the claim/CMN has a 01 disposition, VMS suspends it to this location/status. VMS prints UNFORMATTED in the CITY field of the CW4101-SSA BENE ADDRESS ERROR LISTING REPORT. You must resolve the address problem on the BUDS01 record and type address flag AR in the AF field so that VMS does not update the record with subsequent address information from CWF. After you correct the address, VMS resends the claim/CMN to CWF with Entry Code 05. CWF returns the claim/CMN with an 01 disposition. If applicable, VMS updates the claim/CMN with the correct payment information and processes it to location 10."),
  /** VMS - 63 - 04 - Excess splits. */
  _63_04(MetaSourceSk.VMS, "63", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Excess splits"),
  /** VMS - 64 - 02 - Development non-response. */
  _64_02(
      MetaSourceSk.VMS, "64", ClaimAuditTrailLocationCode.DEVELOPMENT, "Development non-response"),
  /** VMS - 65 - 02 - Development initiated. */
  _65_02(MetaSourceSk.VMS, "65", ClaimAuditTrailLocationCode.DEVELOPMENT, "Development initiated"),
  /** VMS - 66 - 02 - Development sent. */
  _66_02(MetaSourceSk.VMS, "66", ClaimAuditTrailLocationCode.DEVELOPMENT, "Development sent"),
  /** VMS - 67 - 02 - Development follow-up sent. */
  _67_02(
      MetaSourceSk.VMS,
      "67",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      "Development follow-up sent"),
  /** VMS - 68 - 02 - Referral initiated. */
  _68_02(MetaSourceSk.VMS, "68", ClaimAuditTrailLocationCode.DEVELOPMENT, "Referral initiated"),
  /** VMS - 69 - 02 - Referral generated. */
  _69_02(MetaSourceSk.VMS, "69", ClaimAuditTrailLocationCode.DEVELOPMENT, "Referral generated"),
  /** VMS - 70 - 02 - Referral sent. */
  _70_02(MetaSourceSk.VMS, "70", ClaimAuditTrailLocationCode.DEVELOPMENT, "Referral sent"),
  /** VMS - 71 - 02 - Follow-up referral generated. */
  _71_02(
      MetaSourceSk.VMS,
      "71",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      "Follow-up referral generated"),
  /** VMS - 72 - 02 - Follow-up referral sent. */
  _72_02(
      MetaSourceSk.VMS, "72", ClaimAuditTrailLocationCode.DEVELOPMENT, "Follow-up referral sent"),
  /** VMS - 73 - 02 - Referral non-response. */
  _73_02(MetaSourceSk.VMS, "73", ClaimAuditTrailLocationCode.DEVELOPMENT, "Referral non-response"),
  /** VMS - 74 - 02 - ADS manual status. */
  _74_02(MetaSourceSk.VMS, "74", ClaimAuditTrailLocationCode.DEVELOPMENT, "ADS manual status"),
  /** VMS - 75 - 00 - Paid. */
  _75_00(MetaSourceSk.VMS, "75", ClaimAuditTrailLocationCode.COMPLETED, "Paid"),
  /** VMS - 75 - 08 - Claim failed the CARC/RARC/Group Code validation program. */
  _75_08(
      MetaSourceSk.VMS,
      "75",
      ClaimAuditTrailLocationCode.QUERY,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),
  /** VMS - 76 - 00 - Not paid, all or partially paid to deductible. */
  _76_00(
      MetaSourceSk.VMS,
      "76",
      ClaimAuditTrailLocationCode.COMPLETED,
      "Not paid, all or partially paid to deductible; also use for MSP claims where the Medicare amount to be paid is zero and none of the claim lines are denied"),
  /** VMS - 76 - 08 - Claim failed the CARC/RARC/Group Code validation program. */
  _76_08(
      MetaSourceSk.VMS,
      "76",
      ClaimAuditTrailLocationCode.QUERY,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),
  /** VMS - 77 - 00 - Denied. */
  _77_00(MetaSourceSk.VMS, "77", ClaimAuditTrailLocationCode.COMPLETED, "Denied"),
  /** VMS - 77 - 08 - Claim failed the CARC/RARC/Group Code validation program. */
  _77_08(
      MetaSourceSk.VMS,
      "77",
      ClaimAuditTrailLocationCode.QUERY,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),
  /** VMS - 80 - 09 - A/B crossover edits. */
  _80_09(MetaSourceSk.VMS, "80", ClaimAuditTrailLocationCode.REPLY, "A/B crossover edits"),
  /** VMS - 81 - 09 - Mammography, pap smear, or cataract lens claims adjusted with Entry Code 3. */
  _81_09(
      MetaSourceSk.VMS,
      "81",
      ClaimAuditTrailLocationCode.REPLY,
      "Mammography, pap smear, or cataract lens claims adjusted with Entry Code 3"),
  /** VMS - 83 - 09 - Oxygen Equipment rental maximum reached. */
  _83_09(
      MetaSourceSk.VMS,
      "83",
      ClaimAuditTrailLocationCode.REPLY,
      """
          An Oxygen Equipment claim processing against a CMN with the maximum number of rentals in an open status causes the claim to suspend to this location/status. The CMN remains in an open status and the system makes no changes to the rental count of the CMN. Until corrective action is performed, claims continue to suspend to this location/status.
          Options for corrective action on the CMN are:
          • Close the CMN
          • Manually reduce the number of rental payments
          """),
  /** VMS - 84 - 07 - Global surgery. */
  _84_07(
      MetaSourceSk.VMS,
      "84",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Global surgery"),
  /** VMS - 85 - 00 - Claim received back from HIGLAS with check number. */
  _85_00(
      MetaSourceSk.VMS,
      "85",
      ClaimAuditTrailLocationCode.COMPLETED,
      "Claim received back from HIGLAS with check number"),
  /** VMS - 85 - 07 - Multiple surgery with UT error, auto denial. */
  _85_07(
      MetaSourceSk.VMS,
      "85",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Multiple surgery with UT error, auto denial"),
  /** VMS - 85 - 09 - DOD/REP. */
  _85_09(MetaSourceSk.VMS, "85", ClaimAuditTrailLocationCode.REPLY, "DOD/REP"),
  /** VMS - 86 - 07 - E/M location. */
  _86_07(
      MetaSourceSk.VMS, "86", ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION, "E/M location"),
  /** VMS - 86 - 09 - UR 11 rejects. */
  _86_09(MetaSourceSk.VMS, "86", ClaimAuditTrailLocationCode.REPLY, "UR 11 rejects"),
  /** VMS - 87 - 00 - Claim sent to HIGLAS on the 837 Interface file. */
  _87_00(
      MetaSourceSk.VMS,
      "87",
      ClaimAuditTrailLocationCode.COMPLETED,
      "Claim sent to HIGLAS on the 837 Interface file"),
  /** VMS - 87 - 09 - UR 08 rejects. */
  _87_09(MetaSourceSk.VMS, "87", ClaimAuditTrailLocationCode.REPLY, "UR 08 rejects"),
  /** VMS - 88 - 09 - Name incorrect. */
  _88_09(MetaSourceSk.VMS, "88", ClaimAuditTrailLocationCode.REPLY, "Name incorrect"),
  /** VMS - 89 - 08 - Acknowledgment (Disposition Code 09). */
  _89_08(
      MetaSourceSk.VMS,
      "89",
      ClaimAuditTrailLocationCode.QUERY,
      "Acknowledgment (Disposition Code 09)"),
  /** VMS - 90 - 03 - OQC. */
  _90_03(MetaSourceSk.VMS, "90", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 90 - 07 - Batch repricing process. */
  _90_07(
      MetaSourceSk.VMS,
      "90",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      "Batch repricing process"),
  /** VMS - 90 - 08 - Reply Disposition Code 90. */
  _90_08(MetaSourceSk.VMS, "90", ClaimAuditTrailLocationCode.QUERY, "Reply Disposition Code 90"),
  /** VMS - 91 - 03 - OQC. */
  _91_03(MetaSourceSk.VMS, "91", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 92 - 03 - OQC. */
  _92_03(MetaSourceSk.VMS, "92", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 94 - 03 - OQC. */
  _94_03(MetaSourceSk.VMS, "94", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 95 - 03 - OQC. */
  _95_03(MetaSourceSk.VMS, "95", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 95 - 08 - S, M, G query. */
  _95_08(MetaSourceSk.VMS, "95", ClaimAuditTrailLocationCode.QUERY, "S, M, G query"),
  /** VMS - 96 - 03 - OQC. */
  _96_03(MetaSourceSk.VMS, "96", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 96 - 08 - T, N, H query. */
  _96_08(MetaSourceSk.VMS, "96", ClaimAuditTrailLocationCode.QUERY, "T, N, H query"),
  /** VMS - 96 - 09 - CWF Error. */
  _96_09(MetaSourceSk.VMS, "96", ClaimAuditTrailLocationCode.REPLY, "CWF Error"),
  /** VMS - 97 - 03 - OQC. */
  _97_03(MetaSourceSk.VMS, "97", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 97 - 08 - R, L, I query. */
  _97_08(MetaSourceSk.VMS, "97", ClaimAuditTrailLocationCode.QUERY, "R, L, I query"),
  /** VMS - 98 - 03 - OQC. */
  _98_03(MetaSourceSk.VMS, "98", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 98 - 08 - Resend to CWF. */
  _98_08(MetaSourceSk.VMS, "98", ClaimAuditTrailLocationCode.QUERY, "Resend to CWF"),
  /** VMS - 99 - 03 - OQC. */
  _99_03(MetaSourceSk.VMS, "99", ClaimAuditTrailLocationCode.DME_OQC, "OQC"),
  /** VMS - 99 - 04 - Duplicate claim. */
  _99_04(MetaSourceSk.VMS, "99", ClaimAuditTrailLocationCode.SYSTEM_REJECT, "Duplicate claim"),
  /** VMS - 99 - 08 - Regular – Entry Code 1 and follow-up claims to CWF. */
  _99_08(
      MetaSourceSk.VMS,
      "99",
      ClaimAuditTrailLocationCode.QUERY,
      "Regular – Entry Code 1 and follow-up claims to CWF");

  private final MetaSourceSk source;
  private final String statusCode;
  private final ClaimAuditTrailLocationCode locationCode;
  private final String display;

  private static final Map<Key, ClaimAuditTrailStatusCode> CLAIM_STATUS_LOOKUP =
      Arrays.stream(values())
          .collect(
              Collectors.toMap(
                  c -> new Key(c.source, c.statusCode, c.locationCode), Function.identity()));

  private record Key(
      MetaSourceSk source, String statusCode, ClaimAuditTrailLocationCode locationCode) {}

  /**
   * Convert using the meta source id, the raw database status code, and the claim audit trail
   * location code.
   *
   * @param source meta source id
   * @param statusCode raw claim audit trail status code
   * @param locationCode claim audit trail location code
   * @return matching enum constant (if any)
   */
  public static Optional<ClaimAuditTrailStatusCode> tryFromCode(
      MetaSourceSk source, String statusCode, ClaimAuditTrailLocationCode locationCode) {
    return Optional.ofNullable(CLAIM_STATUS_LOOKUP.get(new Key(source, statusCode, locationCode)));
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CLM_AUDT_TRL_STUS_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_AUDIT_TRAIL_STATUS_CODE)
                    .setDisplay(getDisplay())
                    .setCode(getStatusCode())));
  }
}
