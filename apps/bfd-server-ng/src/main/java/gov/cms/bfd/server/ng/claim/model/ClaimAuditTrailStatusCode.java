package gov.cms.bfd.server.ng.claim.model;

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
@SuppressWarnings({"java:S115", "java:S1192"})
public enum ClaimAuditTrailStatusCode {

  /** FISS - A - QUEUED - Accept. */
  FISS_A(
      MetaSourceId.FISS,
      "A",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Accept"),
  /**
   * FISS - F - PARTIAL - Suspended online; the system adds the claim record to the file with active
   * errors.
   */
  FISS_F(
      MetaSourceId.FISS,
      "F",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Suspended online; the system adds the claim record to the file with active errors."),
  /** FISS - S - QUEUED - A manual update is needed before the claim processing can continue. */
  S(
      MetaSourceId.FISS,
      "S",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "A manual update is needed before the claim processing can continue."),
  /**
   * FISS - M - PARTIAL - Designates a manual claim move to either another department, employee,
   * desk, etc. Note: Once an 'M' value is inserted in the Status field, the system changes the
   * status to an 'S' for suspense after the move.
   */
  FISS_M(
      MetaSourceId.FISS,
      "M",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Designates a manual claim move to either another department, employee, desk, etc. Note: Once an 'M' value is inserted in the Status field, the system changes the status to an 'S' for suspense after the move."),
  /**
   * FISS - D - COMPLETE - Claim has reached final disposition with no reimbursement (medical
   * denial).
   */
  FISS_D(
      MetaSourceId.FISS,
      "D",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim has reached final disposition with no reimbursement (medical denial)."),
  /** FISS - P - COMPLETE - Claim has reached final disposition with reimbursement. */
  FISS_P(
      MetaSourceId.FISS,
      "P",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim has reached final disposition with reimbursement."),
  /**
   * FISS - R - COMPLETE - Claim has reached final disposition with no reimbursement (non-medical
   * reject).
   */
  FISS_R(
      MetaSourceId.FISS,
      "R",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim has reached final disposition with no reimbursement (non-medical reject)."),
  /**
   * FISS - T - PARTIAL - Claim has reached final disposition with no reimbursement and has been
   * returned to the provider with billing errors.
   */
  T(
      MetaSourceId.FISS,
      "T",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Claim has reached final disposition with no reimbursement and has been returned to the provider with billing errors."),
  /** FISS - I - PARTIAL - Claim moves from the active processing file to the inactive file. */
  I(
      MetaSourceId.FISS,
      "I",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Claim moves from the active processing file to the inactive file."),
  /**
   * FISS - U - COMPLETE - Claim has reached final disposition and has been returned to the Peer
   * Review Organization for corrections.
   */
  FISS_U(
      MetaSourceId.FISS,
      "U",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim has reached final disposition and has been returned to the Peer Review Organization for corrections."),
  /**
   * MCS - A - QUEUED - Current active claim. This is an internal MCS MPAP status, and will only
   * display online in related history.
   */
  A(
      MetaSourceId.MCS,
      "A",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Current active claim. This is an internal MCS MPAP status, and will only display online in related history."),

  /**
   * MCS - B - QUEUED - Suspended. All pending claims will show this status when they are viewed
   * online. All other pending claim statuses are used internally by MCS MPAP only.
   */
  B(
      MetaSourceId.MCS,
      "B",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Suspended. All pending claims will show this status when they are viewed online. All other pending claim statuses are used internally by MCS MPAP only."),

  /**
   * MCS - C - PARTIAL - Approved awaiting CWF response through MPAP, claim processed with no
   * outstanding edits/audits through MPAP and queried.
   */
  C(
      MetaSourceId.MCS,
      "C",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Approved awaiting CWF response through MPAP, claim processed with no outstanding edits/audits through MPAP and queried."),
  /** MCS - D - COMPLETE - Approved and paid; CAP physician no pay detail lines. */
  MCS_D(
      MetaSourceId.MCS,
      "D",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Approved and paid; CAP physician no pay detail lines."),

  /**
   * MCS - E - COMPLETE - Denied; set based on the history usage indicator on the AA segment of a
   * denial edit or audit.
   */
  E(
      MetaSourceId.MCS,
      "E",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Denied; set based on the history usage indicator on the AA segment of a denial edit or audit."),

  /**
   * MCS - F - COMPLETE - Full claim refund - EGHP, used only when an EGHP accounts receivable has
   * been satisfied (RG type AR). This status is for display purposes only, internally the claim
   * status would be a ‘y’.
   */
  F(
      MetaSourceId.MCS,
      "F",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Full claim refund - EGHP, used only when an EGHP accounts receivable has been satisfied (RG type AR). This status is for display purposes only, internally the claim status would be a ‘y’."),

  /**
   * MCS - G - COMPLETE - Partial refund applied, partial refund was calculated for the claim but
   * was applied to something else outstanding, such as an AR.
   */
  G(
      MetaSourceId.MCS,
      "G",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Partial refund applied, partial refund was calculated for the claim but was applied to something else outstanding, such as an AR."),

  /**
   * MCS - J - QUEUED - Claim still active. This is an internal MCS MPAP status, and will never
   * display online.
   */
  J(
      MetaSourceId.MCS,
      "J",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Claim still active. This is an internal MCS MPAP status, and will never display online."),

  /**
   * MCS - K - QUEUED - Claim in pending suspense, used as history for duplicate audits but not MPAP
   * (relationship or negative relationship audit). This is an internal MCS MPAP status, and does
   * not display online except as related history or on a bene research document (BRD). The status
   * can be set several ways: claim has no significant claim-level audit failed, but has claim-level
   * edit suspense (does not look at history usage on edit) claim or detail suspends for a post-CWF
   * audit detail suspends with ‘f’ disposition audit that has a history usage of ‘3’.
   */
  K(
      MetaSourceId.MCS,
      "K",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Claim in pending suspense, used as history for duplicate audits but not MPAP (relationship or negative relationship audit). This is an internal MCS MPAP status, and does not display online except as related history or on a bene research document (BRD). The status can be set several ways: claim has no significant claim-level audit failed, but has claim-level edit suspense (does not look at history usage on edit) claim or detail suspends for a post-CWF audit detail suspends with ‘f’ disposition audit that has a history usage of ‘3’"),

  /**
   * MCS - L - PARTIAL - CWF suspense, no MPAP, the HIC change trailer on the claim has a different
   * cross-reference HIC than the ‘h’ trailer on eligibility. Note: the ‘L’ status is an internal
   * status and will not appear on a claim. While the HIC is being changed, the claim will have an
   * ‘L’ status until the change is complete.
   */
  L(
      MetaSourceId.MCS,
      "L",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "CWF suspense, no MPAP, the HIC change trailer on the claim has a different cross-reference HIC than the ‘h’ trailer on eligibility. Note: the ‘L’ status is an internal status and will not appear on a claim. While the HIC is being changed, the claim will have an ‘L’ status until the change is complete."),

  /** MCS - M - COMPLETE - Approved and paid (includes all deductible) - currently not used. */
  M(
      MetaSourceId.MCS,
      "M",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Approved and paid (includes all deductible) - currently not used."),

  /**
   * MCS - N - COMPLETE - Denied for payment (excludes deductible), set based on the history usage
   * (AA segment) indicator on a denial edit or audit.
   */
  N(
      MetaSourceId.MCS,
      "N",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Denied for payment (excludes deductible), set based on the history usage (AA segment) indicator on a denial edit or audit."),

  /**
   * MCS - P - COMPLETE - Partial claim refund - EGHP, used only when an EGHP accounts receivable
   * has been satisfied (RG type AR). This status is for display purposes only, internally the claim
   * status would be a ‘g’.
   */
  MCS_P(
      MetaSourceId.MCS,
      "P",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Partial claim refund - EGHP, used only when an EGHP accounts receivable has been satisfied (RG type AR). This status is for display purposes only, internally the claim status would be a ‘g’."),

  /** MCS - Q - COMPLETE - Adjusted - claim has been replaced by a full claim adjustment. */
  Q(
      MetaSourceId.MCS,
      "Q",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Adjusted - claim has been replaced by a full claim adjustment."),

  /**
   * MCS - R - COMPLETE - Claim has been deleted from the system. When a claim is deleted
   * (transferred to location 090), the claim status and the detail status are both set to ‘R’.
   */
  MCS_R(
      MetaSourceId.MCS,
      "R",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim has been deleted from the system. When a claim is deleted (transferred to location 090), the claim status and the detail status are both set to ‘R’."),

  /** MCS - U - COMPLETE - Paid but not for dup use - currently not used. */
  U(
      MetaSourceId.MCS,
      "U",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Paid but not for dup use - currently not used."),

  /**
   * MCS - V - COMPLETE - Denied, but not for dup use, set based on the history usage indicator on
   * the AA segment of a denial edit or audit.
   */
  V(
      MetaSourceId.MCS,
      "V",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Denied, but not for dup use, set based on the history usage indicator on the AA segment of a denial edit or audit."),

  /**
   * MCS - w - COMPLETE - Rejected. This status is set for Assigned and Non-Assigned claims, based
   * on the receipt date, the bene submission form, and the reject indicator which is MSG ACTION =
   * RJ or R2 on the Narrative Message Usage file (NA). If all details are rejected (status ‘W’)
   * then the claim status is set to rejected (‘W’).
   */
  W(
      MetaSourceId.MCS,
      "W",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Rejected. This status is set for Assigned and Non-Assigned claims, based on the receipt date, the bene submission form, and the reject indicator which is MSG ACTION = RJ or R2 on the Narrative Message Usage file (NA). If all details are rejected (status ‘W’) then the claim status is set to rejected (‘W’)."),

  /** MCS - x - COMPLETE - Partial refund, claim that is a partial void and a split pay. */
  X(
      MetaSourceId.MCS,
      "X",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Partial refund, claim that is a partial void and a split pay."),

  /** MCS - Y - COMPLETE - Full refund, full amount of claim payment was returned. */
  Y(
      MetaSourceId.MCS,
      "Y",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Full refund, full amount of claim payment was returned."),

  /** MCS - Z - COMPLETE - Voided, full void has been issued for the claim. */
  Z(
      MetaSourceId.MCS,
      "Z",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Voided, full void has been issued for the claim."),

  /**
   * MCS - 1 - QUEUED - Current active claim, separate history. This is an internal MCS MPAP status,
   * and will only display online in related history. This status applies to the header of claims
   * that contain a demonstration number that has been flagged for separate history in the HXXTDEMO
   * table.
   */
  _1(
      MetaSourceId.MCS,
      "1",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Current active claim, separate history. This is an internal MCS MPAP status, and will only display online in related history. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),

  /**
   * MCS - 2 - QUEUED - Suspended, separate history. All pending claims will show this status when
   * they are viewed online. All other pending claim statuses are used internally by MCS MPAP only.
   * This status applies to the header of claims that contain a demonstration number that has been
   * flagged for separate history in the HXXTDEMO table.
   */
  _2(
      MetaSourceId.MCS,
      "2",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.QUEUED,
      "Suspended, separate history. All pending claims will show this status when they are viewed online. All other pending claim statuses are used internally by MCS MPAP only. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),

  /**
   * MCS - 3 - PARTIAL - Approved awaiting CWF response, separate history, through MPAP, claim
   * processed with no outstanding edits/audits through MPAP and queried. This status applies to the
   * header of claims that contain a demonstration number that has been flagged for separate history
   * in the HXXTDEMO table.
   */
  _3(
      MetaSourceId.MCS,
      "3",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.PARTIAL,
      "Approved awaiting CWF response, separate history, through MPAP, claim processed with no outstanding edits/audits through MPAP and queried. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),

  /**
   * MCS - 4 - COMPLETE - Approved and paid, separate history. This status applies to the header of
   * claims that contain a demonstration number that has been flagged for separate history in the
   * HXXTDEMO table.
   */
  _4(
      MetaSourceId.MCS,
      "4",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Approved and paid, separate history. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),

  /**
   * MCS - 5 - COMPLETE - Denied; separate history, set based on the history usage indicator on the
   * AA segment of a denial edit or audit. This status applies to the header of claims that contain
   * a demonstration number that has been flagged for separate history in the HXXTDEMO table.
   */
  _5(
      MetaSourceId.MCS,
      "5",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Denied; separate history, set based on the history usage indicator on the AA segment of a denial edit or audit. This status applies to the header of claims that contain a demonstration number that has been flagged for separate history in the HXXTDEMO table."),

  /** MCS - 6 - ERROR - Not Used. */
  _6(
      MetaSourceId.MCS,
      "6",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Not Used"),

  /**
   * MCS - 8 - COMPLETE - Claim moved to another HIC. Claim was submitted and finalized for a HIC
   * prior to the HIC being changed. (This status is internal to MCS only and will not display
   * online.)
   */
  _8(
      MetaSourceId.MCS,
      "8",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim moved to another HIC. Claim was submitted and finalized for a HIC prior to the HIC being changed. (This status is internal to MCS only and will not display online.)"),

  /**
   * MCS - 9 - COMPLETE - Claim deleted from system. Claim deleted from MPAP due to rework, the ICN
   * is a duplicate of another ICN in the system. (This status is internal to MCS only and will not
   * display online.)
   */
  _9(
      MetaSourceId.MCS,
      "9",
      ClaimAuditTrailLocationCode.NA,
      ExplanationOfBenefit.RemittanceOutcome.COMPLETE,
      "Claim deleted from system. Claim deleted from MPAP due to rework, the ICN is a duplicate of another ICN in the system. (This status is internal to MCS only and will not display online.)"),

  /** VMS - 00 - 08 - outcome determined by CLM_FINAL_ACTION - Void/Entry Code 3 Claim. */
  _00(
      MetaSourceId.VMS,
      "00",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Void/Entry Code 3 Claim"),

  /**
   * VMS - 01 - 05 - outcome determined by CLM_FINAL_ACTION - TPL Suspense/MSP/HMO (Jurisdiction D’s
   * HMO claims suspend to 09/27).
   */
  _01_05(
      MetaSourceId.VMS,
      "01",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims suspend to 09/27)"),

  /**
   * VMS - 01 - 09 - outcome determined by CLM_FINAL_ACTION - TPL Suspense/MSP/HMO (Jurisdiction D’s
   * HMO claims suspend to 09/27).
   */
  _01_09(
      MetaSourceId.VMS,
      "01",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "TPL Suspense/MSP/HMO (Jurisdiction D’s HMO claims suspend to 09/27)"),

  /** VMS - 01 - 07 - outcome determined by CLM_FINAL_ACTION - MSP Cost Avoid. */
  _01_07(
      MetaSourceId.VMS,
      "01",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP Cost Avoid"),

  /** VMS - 02 - 07 - outcome determined by CLM_FINAL_ACTION - MSP Denied Lines. */
  _02_07(
      MetaSourceId.VMS,
      "02",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP Denied Lines"),

  /**
   * VMS - 02 - 09 - outcome determined by CLM_FINAL_ACTION - MSP claims that received CWF edit 6819
   * and had non-GHP MSP prior to querying CWF.
   */
  _02_09(
      MetaSourceId.VMS,
      "02",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP claims that received CWF edit 6819 and had non-GHP MSP prior to querying CWF"),

  /** VMS - 03 - 06 - outcome determined by CLM_FINAL_ACTION - Purged. */
  _03_06(
      MetaSourceId.VMS,
      "03",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Purged"),

  /** VMS - 03 - 07 - outcome determined by CLM_FINAL_ACTION - MSP Split Claims. */
  _03_07(
      MetaSourceId.VMS,
      "03",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP Split Claims"),

  /**
   * VMS - 03 - 09 - outcome determined by CLM_FINAL_ACTION - MSP claims that received CWF edit 6819
   * and did not have non-GHP MSP prior to querying CWF.
   */
  _03_09(
      MetaSourceId.VMS,
      "03",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP claims that received CWF edit 6819 and did not have non-GHP MSP prior to querying CWF"),

  /** VMS - 04 - 05 - outcome determined by CLM_FINAL_ACTION - Clean claim (ready to adjudicate). */
  _04_05(
      MetaSourceId.VMS,
      "04",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Clean claim (ready to adjudicate)"),

  /** VMS - 05 - 05 - outcome determined by CLM_FINAL_ACTION - Line item error. */
  _05_05(
      MetaSourceId.VMS,
      "05",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Line item error"),

  /** VMS - 05 - 06 - outcome determined by CLM_FINAL_ACTION - Line item error. */
  _05_06(
      MetaSourceId.VMS,
      "05",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Line item error"),

  /** VMS - 05 - 09 - outcome determined by CLM_FINAL_ACTION - VMS Action Code review. */
  _05_09(
      MetaSourceId.VMS,
      "05",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      """
        If the system cannot identify a VMS Action Code; the claim suspends to this location/status for review. You need to verify that the FPS Model Number on each claim line appears on the VMAP/4C/ACFPWALK table. If the FPS Model Number/Action Code combination is not on the table, update the table according to the TDL issued by CMS that introduced the FPS Model. After updating the table, deny the claim line or lines as follows:
        • Type the Action Code for the FPS Model Number on the claim line or lines.
        • Ensure that the Allowed Amount on the claim is zero.
        • Type R in the Claim Review Code field.
        Refer to entries for FPSD and FPSH in the APEX Reference Manual in the chapter on “Common Working File (CWF) Codes” for additional information.
        """),

  /** VMS - 06 - 06 - outcome determined by CLM_FINAL_ACTION - Provider problem. */
  _06_06(
      MetaSourceId.VMS,
      "06",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Provider problem"),

  /** VMS - 07 - 06 - outcome determined by CLM_FINAL_ACTION - Medical consultation. */
  _07_06(
      MetaSourceId.VMS,
      "07",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Medical consultation"),

  /** VMS - 08 - 05 - outcome determined by CLM_FINAL_ACTION - Edit error. */
  _08_05(
      MetaSourceId.VMS,
      "08",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Edit error"),

  /** VMS - 08 - 06 - outcome determined by CLM_FINAL_ACTION - Edit error. */
  _08_06(
      MetaSourceId.VMS,
      "08",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Edit error"),

  /** VMS - 09 - 05 - outcome determined by CLM_FINAL_ACTION - Specialty examination. */
  _09_05(
      MetaSourceId.VMS,
      "09",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Specialty examination"),

  /**
   * VMS - 09 - 06 - outcome determined by CLM_FINAL_ACTION - MSP with a primary paid amount from
   * the primary payer.
   */
  _09_06(
      MetaSourceId.VMS,
      "09",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP with a primary paid amount from the primary payer"),

  /** VMS - 10 - 04 - outcome determined by CLM_FINAL_ACTION - Delete. */
  _10_04(
      MetaSourceId.VMS,
      "10",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Delete"),

  /** VMS - 11 - 05 - outcome determined by CLM_FINAL_ACTION - Claim referred to supervisor. */
  _11_05(
      MetaSourceId.VMS,
      "11",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim referred to supervisor"),

  /** VMS - 12 - 02 - outcome determined by CLM_FINAL_ACTION - MSP first letter initiated. */
  _12_02(
      MetaSourceId.VMS,
      "12",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP first letter initiated"),

  /** VMS - 13 - 02 - outcome determined by CLM_FINAL_ACTION - Suspense – Other. */
  _13_02(
      MetaSourceId.VMS,
      "13",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Suspense – Other"),

  /** VMS - 14 - 04 - outcome determined by CLM_FINAL_ACTION - Suspense – DME. */
  _14_04(
      MetaSourceId.VMS,
      "14",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Suspense – DME"),

  /** VMS - 14 - 05 - outcome determined by CLM_FINAL_ACTION - Suspense – DME. */
  _14_05(
      MetaSourceId.VMS,
      "14",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Suspense – DME"),

  /** VMS - 15 - 05 - outcome determined by CLM_FINAL_ACTION - Chiropractor claim. */
  _15_05(
      MetaSourceId.VMS,
      "15",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Chiropractor claim"),

  /** VMS - 16 - 02 - outcome determined by CLM_FINAL_ACTION - MSP first letter sent. */
  _16_02(
      MetaSourceId.VMS,
      "16",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP first letter sent"),

  /** VMS - 17 - 01 - outcome determined by CLM_FINAL_ACTION - Activated; not entered. */
  _17_01(
      MetaSourceId.VMS,
      "17",
      ClaimAuditTrailLocationCode.PRE_COMPUTER,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Activated; not entered"),

  /** VMS - 18 - 02 - outcome determined by CLM_FINAL_ACTION - Utilization review. */
  _18_02(
      MetaSourceId.VMS,
      "18",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Utilization review"),

  /** VMS - 18 - 07 - outcome determined by CLM_FINAL_ACTION - Utilization review. */
  _18_07(
      MetaSourceId.VMS,
      "18",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Utilization review"),

  /** VMS - 18 - 09 - outcome determined by CLM_FINAL_ACTION - Utilization review. */
  _18_09(
      MetaSourceId.VMS,
      "18",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Utilization review"),

  /**
   * VMS - 19 - 07 - outcome determined by CLM_FINAL_ACTION - Third level review (prior history
   * review).
   */
  _19_07(
      MetaSourceId.VMS,
      "19",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Third level review (prior history review)"),

  /** VMS - 20 - 05 - outcome determined by CLM_FINAL_ACTION - Reject name/sex. */
  _20_05(
      MetaSourceId.VMS,
      "20",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Reject name/sex"),

  /** VMS - 21 - 04 - outcome determined by CLM_FINAL_ACTION - Adjustment. */
  _21_04(
      MetaSourceId.VMS,
      "21",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Adjustment"),

  /**
   * VMS - 22 - 05 - outcome determined by CLM_FINAL_ACTION - Entitlement termination; quality
   * control.
   */
  _22_05(
      MetaSourceId.VMS,
      "22",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Entitlement termination; quality control"),

  /** VMS - 23 - 05 - outcome determined by CLM_FINAL_ACTION - No beneficiary address. */
  _23_05(
      MetaSourceId.VMS,
      "23",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "No beneficiary address"),

  /** VMS - 24 - 04 - outcome determined by CLM_FINAL_ACTION - Beneficiary BUDS01 record closed. */
  _24_04(
      MetaSourceId.VMS,
      "24",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary BUDS01 record closed"),

  /** VMS - 24 - 05 - outcome determined by CLM_FINAL_ACTION - Beneficiary BUDS01 record closed. */
  _24_05(
      MetaSourceId.VMS,
      "24",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary BUDS01 record closed"),

  /** VMS - 24 - 09 - outcome determined by CLM_FINAL_ACTION - Beneficiary BUDS01 record closed. */
  _24_09(
      MetaSourceId.VMS,
      "24",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary BUDS01 record closed"),

  /** VMS - 25 - 08 - outcome determined by CLM_FINAL_ACTION - Representative payee. */
  _25_08(
      MetaSourceId.VMS,
      "25",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Representative payee"),

  /** VMS - 25 - 09 - outcome determined by CLM_FINAL_ACTION - Representative payee. */
  _25_09(
      MetaSourceId.VMS,
      "25",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Representative payee"),

  /** VMS - 26 - 08 - outcome determined by CLM_FINAL_ACTION - Welfare; Disposition Code 42. */
  _26_08(
      MetaSourceId.VMS,
      "26",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Welfare; Disposition Code 42"),

  /** VMS - 26 - 09 - outcome determined by CLM_FINAL_ACTION - Welfare; Disposition Code 42. */
  _26_09(
      MetaSourceId.VMS,
      "26",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Welfare; Disposition Code 42"),

  /**
   * VMS - 27 - 08 - outcome determined by CLM_FINAL_ACTION - Services prior to entitlement (HMO for
   * Jurisdiction D only).
   */
  _27_08(
      MetaSourceId.VMS,
      "27",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Services prior to entitlement (HMO for Jurisdiction D only)"),

  /**
   * VMS - 27 - 09 - outcome determined by CLM_FINAL_ACTION - Services prior to entitlement (HMO for
   * Jurisdiction D only).
   */
  _27_09(
      MetaSourceId.VMS,
      "27",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Services prior to entitlement (HMO for Jurisdiction D only)"),

  /** VMS - 28 - 04 - outcome determined by CLM_FINAL_ACTION - Mass adjustment suspensions. */
  _28_04(
      MetaSourceId.VMS,
      "28",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Mass adjustment suspensions"),

  /** VMS - 29 - 06 - outcome determined by CLM_FINAL_ACTION - Missing data. */
  _29_06(
      MetaSourceId.VMS,
      "29",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Missing data"),

  /** VMS - 30 - 04 - outcome determined by CLM_FINAL_ACTION - Estimated interest errors. */
  _30_04(
      MetaSourceId.VMS,
      "30",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Location/status 04/30 is for estimated interest errors. Batch adjudication program VMSCW273 generates this location/status prior to sending the claim to CWF, based on the absence of valid data in certain fields on the claim. These fields include: the date of receipt, the estimated mail date, the amount paid to the provider, the amount paid to the beneficiary, the provider participation indicator, and the provider specialty."),

  /** VMS - 30 - 05 - outcome determined by CLM_FINAL_ACTION - Estimated interest errors. */
  _30_05(
      MetaSourceId.VMS,
      "30",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Location/status 04/30 is for estimated interest errors. Batch adjudication program VMSCW273 generates this location/status prior to sending the claim to CWF, based on the absence of valid data in certain fields on the claim. These fields include: the date of receipt, the estimated mail date, the amount paid to the provider, the amount paid to the beneficiary, the provider participation indicator, and the provider specialty."),

  /** VMS - 31 - 05 - outcome determined by CLM_FINAL_ACTION - "". */
  _31_05(
      MetaSourceId.VMS,
      "31",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      ""),

  /** VMS - 33 - 05 - outcome determined by CLM_FINAL_ACTION - Reasonable charge. */
  _33_05(
      MetaSourceId.VMS,
      "33",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Reasonable charge"),

  /** VMS - 34 - 05 - outcome determined by CLM_FINAL_ACTION - Physician inactive/missing. */
  _34_05(
      MetaSourceId.VMS,
      "34",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Physician inactive/missing"),

  /** VMS - 35 - 05 - outcome determined by CLM_FINAL_ACTION - Physician utilization. */
  _35_05(
      MetaSourceId.VMS,
      "35",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Physician utilization"),

  /** VMS - 36 - 02 - outcome determined by CLM_FINAL_ACTION - MSP first letter follow-up. */
  _36_02(
      MetaSourceId.VMS,
      "36",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP first letter follow-up"),

  /** VMS - 37 - 07 - outcome determined by CLM_FINAL_ACTION - Duplicate suspect. */
  _37_07(
      MetaSourceId.VMS,
      "37",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Duplicate suspect"),

  /**
   * VMS - 38 - 07 - outcome determined by CLM_FINAL_ACTION - Beneficiary utilization - mandatory
   * assignment for drugs/biologicals.
   */
  _38_07(
      MetaSourceId.VMS,
      "38",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary utilization - mandatory assignment for drugs/biologicals"),

  /** VMS - 39 - 02 - outcome determined by CLM_FINAL_ACTION - Beneficiary information. */
  _39_02(
      MetaSourceId.VMS,
      "39",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary information"),

  /**
   * VMS - 39 - 07 - outcome determined by CLM_FINAL_ACTION - Rebundled claims (Jurisdictions A, B,
   * &amp; C).
   */
  _39_07(
      MetaSourceId.VMS,
      "39",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Rebundled claims (Jurisdictions A, B, & C)"),

  /** VMS - 40 - 09 - outcome determined by CLM_FINAL_ACTION - Premium arrearage; V trailer. */
  _40_09(
      MetaSourceId.VMS,
      "40",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Premium arrearage; V trailer"),

  /**
   * VMS - 41 - 08 - outcome determined by CLM_FINAL_ACTION - New jurisdiction; E trailer;
   * Disposition Code 40.
   */
  _41_08(
      MetaSourceId.VMS,
      "41",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "New jurisdiction; E trailer; Disposition Code 40"),

  /**
   * VMS - 41 - 09 - outcome determined by CLM_FINAL_ACTION - New jurisdiction; E trailer;
   * Disposition Code 40.
   */
  _41_09(
      MetaSourceId.VMS,
      "41",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "New jurisdiction; E trailer; Disposition Code 40"),

  /**
   * VMS - 42 - 08 - outcome determined by CLM_FINAL_ACTION - Unique for CWF resubmits – deny after
   * 4 or 20 days, as appropriate.
   */
  _42_08(
      MetaSourceId.VMS,
      "42",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Unique for CWF resubmits – deny after 4 or 20 days, as appropriate"),

  /**
   * VMS - 42 - 09 - outcome determined by CLM_FINAL_ACTION - Unique for CWF resubmits – deny after
   * 4 or 20 days, as appropriate.
   */
  _42_09(
      MetaSourceId.VMS,
      "42",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Unique for CWF resubmits – deny after 4 or 20 days, as appropriate"),

  /** VMS - 43 - 08 - outcome determined by CLM_FINAL_ACTION - Reply Disposition Code 43. */
  _43_08(
      MetaSourceId.VMS,
      "43",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Reply Disposition Code 43"),

  /** VMS - 43 - 09 - outcome determined by CLM_FINAL_ACTION - Reply Disposition Code 43. */
  _43_09(
      MetaSourceId.VMS,
      "43",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Reply Disposition Code 43"),

  /** VMS - 44 - 02 - outcome determined by CLM_FINAL_ACTION - MSP automated development. */
  _44_02(
      MetaSourceId.VMS,
      "44",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP automated development"),

  /** VMS - 44 - 08 - outcome determined by CLM_FINAL_ACTION - MSP automated development. */
  _44_08(
      MetaSourceId.VMS,
      "44",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "MSP automated development"),

  /** VMS - 45 - 08 - outcome determined by CLM_FINAL_ACTION - Name error. */
  _45_08(
      MetaSourceId.VMS,
      "45",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Name error"),

  /** VMS - 45 - 09 - outcome determined by CLM_FINAL_ACTION - Name error. */
  _45_09(
      MetaSourceId.VMS,
      "45",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Name error"),

  /** VMS - 46 - 03 - outcome determined by CLM_FINAL_ACTION - Normal DME record (Cert). */
  _46_03(
      MetaSourceId.VMS,
      "46",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Normal DME record (Cert)"),

  /**
   * VMS - 47 - 07 - outcome determined by CLM_FINAL_ACTION - Re-suspend the claim from a UR LL/SS;
   * AC operator did not type review code U showing UR review is complete.
   */
  _47_07(
      MetaSourceId.VMS,
      "47",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Re-suspend the claim from a UR LL/SS; AC operator did not type review code U showing UR review is complete"),

  /** VMS - 47 - 09 - outcome determined by CLM_FINAL_ACTION - New HICN; C trailer. */
  _47_09(
      MetaSourceId.VMS,
      "47",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "New HICN; C trailer"),

  /**
   * VMS - 48 - 07 - outcome determined by CLM_FINAL_ACTION - Re-suspend the claim from a UR LL/SS;
   * the PSC/ZPIC operator did not type review code U showing UR review is complete.
   */
  _48_07(
      MetaSourceId.VMS,
      "48",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Re-suspend the claim from a UR LL/SS; the PSC/ZPIC operator did not type review code U showing UR review is complete"),

  /** VMS - 48 - 09 - outcome determined by CLM_FINAL_ACTION - Worker’s Compensation; Y trailer. */
  _48_09(
      MetaSourceId.VMS,
      "48",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Worker’s Compensation; Y trailer"),

  /** VMS - 49 - 07 - outcome determined by CLM_FINAL_ACTION - Rebundling (Jurisdiction D only). */
  _49_07(
      MetaSourceId.VMS,
      "49",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Rebundling (Jurisdiction D only)"),

  /** VMS - 49 - 09 - outcome determined by CLM_FINAL_ACTION - Reject Travelers, RRB, or UMW. */
  _49_09(
      MetaSourceId.VMS,
      "49",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Reject Travelers, RRB, or UMW"),

  /** VMS - 50 - 03 - outcome determined by CLM_FINAL_ACTION - Stale cert – automated DME. */
  _50_03(
      MetaSourceId.VMS,
      "50",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Stale cert – automated DME"),

  /** VMS - 50 - 05 - outcome determined by CLM_FINAL_ACTION - Stale cert – automated DME. */
  _50_05(
      MetaSourceId.VMS,
      "50",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Stale cert – automated DME"),

  /** VMS - 51 - 03 - outcome determined by CLM_FINAL_ACTION - Stop cert – automated DME. */
  _51_03(
      MetaSourceId.VMS,
      "51",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Stop cert – automated DME"),

  /** VMS - 52 - 05 - outcome determined by CLM_FINAL_ACTION - Reasonable charge error. */
  _52_05(
      MetaSourceId.VMS,
      "52",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Reasonable charge error"),

  /** VMS - 52 - 06 - outcome determined by CLM_FINAL_ACTION - Reasonable charge error. */
  _52_06(
      MetaSourceId.VMS,
      "52",
      ClaimAuditTrailLocationCode.REASONABLE_CHARGE,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Reasonable charge error"),

  /** VMS - 53 - 05 - outcome determined by CLM_FINAL_ACTION - No cert on file – automated DME. */
  _53_05(
      MetaSourceId.VMS,
      "53",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "No cert on file – automated DME"),

  /** VMS - 54 - 08 - outcome determined by CLM_FINAL_ACTION - Alien no pay. */
  _54_08(
      MetaSourceId.VMS,
      "54",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Alien no pay"),

  /** VMS - 55 - 09 - outcome determined by CLM_FINAL_ACTION - Hospice involvement. */
  _55_09(
      MetaSourceId.VMS,
      "55",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Hospice involvement"),
  /**
   * VMS - 56 - 08 - outcome determined by CLM_FINAL_ACTION - Adjustment claim error/09 entry code.
   */
  _56_08(
      MetaSourceId.VMS,
      "56",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Adjustment claim error/09 entry code"),
  /**
   * VMS - 56 - 09 - outcome determined by CLM_FINAL_ACTION - Adjustment claim error/09 entry code.
   */
  _56_09(
      MetaSourceId.VMS,
      "56",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Adjustment claim error/09 entry code"),
  /** VMS - 57 - 02 - outcome determined by CLM_FINAL_ACTION - Initiate MSP development. */
  _57_02(
      MetaSourceId.VMS,
      "57",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Initiate MSP development"),
  /** VMS - 57 - 05 - outcome determined by CLM_FINAL_ACTION - Initiate MSP development. */
  _57_05(
      MetaSourceId.VMS,
      "57",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Initiate MSP development"),
  /**
   * VMS - 57 - 07 - outcome determined by CLM_FINAL_ACTION - LMRP/NCD denial by a non-MR edit that
   * is missing LMRP/NCD numbers.
   */
  _57_07(
      MetaSourceId.VMS,
      "57",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers"),
  /**
   * VMS - 57 - 09 - outcome determined by CLM_FINAL_ACTION - LMRP/NCD denial by CWF that is missing
   * LMRP/NCD numbers.
   */
  _57_09(
      MetaSourceId.VMS,
      "57",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "LMRP/NCD denial by CWF that is missing LMRP/NCD numbers"),
  /**
   * VMS - 58 - 07 - outcome determined by CLM_FINAL_ACTION - LMRP/NCD denial by a non-MR edit that
   * is missing LMRP/NCD numbers.
   */
  _58_07(
      MetaSourceId.VMS,
      "58",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "LMRP/NCD denial by a non-MR edit that is missing LMRP/NCD numbers"),
  /** VMS - 58 - 02 - outcome determined by CLM_FINAL_ACTION - Generate the MSP letter. */
  _58_02(
      MetaSourceId.VMS,
      "58",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Generate the MSP letter"),
  /** VMS - 59 - 02 - outcome determined by CLM_FINAL_ACTION - Eligible for denial for MSP. */
  _59_02(
      MetaSourceId.VMS,
      "59",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Eligible for denial for MSP"),
  /** VMS - 59 - 05 - outcome determined by CLM_FINAL_ACTION - Eligible for denial for MSP. */
  _59_05(
      MetaSourceId.VMS,
      "59",
      ClaimAuditTrailLocationCode.EDIT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Eligible for denial for MSP"),
  /** VMS - 60 - 04 - outcome determined by CLM_FINAL_ACTION - Excess history. */
  _60_04(
      MetaSourceId.VMS,
      "60",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "Excess history"),

  /** VMS - 61 - 04 - outcome determined by CLM_FINAL_ACTION - Beneficiary paid. */
  _61_04(
      MetaSourceId.VMS,
      "61",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Beneficiary paid"),

  /** VMS - 62 - 04 - outcome determined by CLM_FINAL_ACTION - System error. */
  _62_04(
      MetaSourceId.VMS,
      "62",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "System error"),
  /** VMS - 62 - 09 - outcome determined by CLM_FINAL_ACTION - Address formatting error from CWF. */
  _62_09(
      MetaSourceId.VMS,
      "62",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.ERROR,
      "When a claim/CMN has an address that CWF cannot format, CWF returns it with Trailer 12. If the claim/CMN has a 01 disposition, VMS suspends it to this location/status. VMS prints UNFORMATTED in the CITY field of the CW4101-SSA BENE ADDRESS ERROR LISTING REPORT. You must resolve the address problem on the BUDS01 record and type address flag AR in the AF field so that VMS does not update the record with subsequent address information from CWF. After you correct the address, VMS resends the claim/CMN to CWF with Entry Code 05. CWF returns the claim/CMN with an 01 disposition. If applicable, VMS updates the claim/CMN with the correct payment information and processes it to location 10."),

  /** VMS - 63 - 04 - outcome determined by CLM_FINAL_ACTION - Excess splits. */
  _63_04(
      MetaSourceId.VMS,
      "63",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Excess splits"),

  /** VMS - 64 - 02 - outcome determined by CLM_FINAL_ACTION - Development non-response. */
  _64_02(
      MetaSourceId.VMS,
      "64",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Development non-response"),

  /** VMS - 65 - 02 - outcome determined by CLM_FINAL_ACTION - Development initiated. */
  _65_02(
      MetaSourceId.VMS,
      "65",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Development initiated"),

  /** VMS - 66 - 02 - outcome determined by CLM_FINAL_ACTION - Development sent. */
  _66_02(
      MetaSourceId.VMS,
      "66",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Development sent"),

  /** VMS - 67 - 02 - outcome determined by CLM_FINAL_ACTION - Development follow-up sent. */
  _67_02(
      MetaSourceId.VMS,
      "67",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Development follow-up sent"),

  /** VMS - 68 - 02 - outcome determined by CLM_FINAL_ACTION - Referral initiated. */
  _68_02(
      MetaSourceId.VMS,
      "68",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Referral initiated"),

  /** VMS - 69 - 02 - outcome determined by CLM_FINAL_ACTION - Referral generated. */
  _69_02(
      MetaSourceId.VMS,
      "69",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Referral generated"),

  /** VMS - 70 - 02 - outcome determined by CLM_FINAL_ACTION - Referral sent. */
  _70_02(
      MetaSourceId.VMS,
      "70",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Referral sent"),

  /** VMS - 71 - 02 - outcome determined by CLM_FINAL_ACTION - Follow-up referral generated. */
  _71_02(
      MetaSourceId.VMS,
      "71",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Follow-up referral generated"),

  /** VMS - 72 - 02 - outcome determined by CLM_FINAL_ACTION - Follow-up referral sent. */
  _72_02(
      MetaSourceId.VMS,
      "72",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Follow-up referral sent"),

  /** VMS - 73 - 02 - outcome determined by CLM_FINAL_ACTION - Referral non-response. */
  _73_02(
      MetaSourceId.VMS,
      "73",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Referral non-response"),

  /** VMS - 74 - 02 - outcome determined by CLM_FINAL_ACTION - ADS manual status. */
  _74_02(
      MetaSourceId.VMS,
      "74",
      ClaimAuditTrailLocationCode.DEVELOPMENT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "ADS manual status"),

  /** VMS - 75 - 00 - outcome determined by CLM_FINAL_ACTION - Paid. */
  _75_00(
      MetaSourceId.VMS,
      "75",
      ClaimAuditTrailLocationCode.COMPLETED,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Paid"),

  /**
   * VMS - 75 - 08 - outcome determined by CLM_FINAL_ACTION - Claim failed the CARC/RARC/Group Code
   * validation program.
   */
  _75_08(
      MetaSourceId.VMS,
      "75",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),

  /**
   * VMS - 76 - 00 - outcome determined by CLM_FINAL_ACTION - Not paid, all or partially paid to
   * deductible.
   */
  _76_00(
      MetaSourceId.VMS,
      "76",
      ClaimAuditTrailLocationCode.COMPLETED,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Not paid, all or partially paid to deductible; also use for MSP claims where the Medicare amount to be paid is zero and none of the claim lines are denied"),

  /**
   * VMS - 76 - 08 - outcome determined by CLM_FINAL_ACTION - Claim failed the CARC/RARC/Group Code
   * validation program.
   */
  _76_08(
      MetaSourceId.VMS,
      "76",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),

  /** VMS - 77 - 00 - outcome determined by CLM_FINAL_ACTION - Denied. */
  _77_00(
      MetaSourceId.VMS,
      "77",
      ClaimAuditTrailLocationCode.COMPLETED,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Denied"),

  /**
   * VMS - 77 - 08 - outcome determined by CLM_FINAL_ACTION - Claim failed the CARC/RARC/Group Code
   * validation program.
   */
  _77_08(
      MetaSourceId.VMS,
      "77",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim failed the CARC/RARC/Group Code validation program Claim reprocesses daily through the CARC/RARC/Group Code validation program and moves to location 10 after the validation program makes a successful validation of the claim’s CARCs, RARCs, and Group Codes."),

  /** VMS - 80 - 09 - outcome determined by CLM_FINAL_ACTION - A/B crossover edits. */
  _80_09(
      MetaSourceId.VMS,
      "80",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "A/B crossover edits"),
  /**
   * VMS - 81 - 09 - outcome determined by CLM_FINAL_ACTION - Mammography, pap smear, or cataract
   * lens claims adjusted with Entry Code 3.
   */
  _81_09(
      MetaSourceId.VMS,
      "81",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Mammography, pap smear, or cataract lens claims adjusted with Entry Code 3"),

  /**
   * VMS - 83 - 09 - outcome determined by CLM_FINAL_ACTION - Oxygen Equipment rental maximum
   * reached.
   */
  _83_09(
      MetaSourceId.VMS,
      "83",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      """
          An Oxygen Equipment claim processing against a CMN with the maximum number of rentals in an open status causes the claim to suspend to this location/status. The CMN remains in an open status and the system makes no changes to the rental count of the CMN. Until corrective action is performed, claims continue to suspend to this location/status.
          Options for corrective action on the CMN are:
          • Close the CMN
          • Manually reduce the number of rental payments
          """),

  /** VMS - 84 - 07 - outcome determined by CLM_FINAL_ACTION - Global surgery. */
  _84_07(
      MetaSourceId.VMS,
      "84",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Global surgery"),

  /**
   * VMS - 85 - 00 - outcome determined by CLM_FINAL_ACTION - Claim received back from HIGLAS with
   * check number.
   */
  _85_00(
      MetaSourceId.VMS,
      "85",
      ClaimAuditTrailLocationCode.COMPLETED,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim received back from HIGLAS with check number"),

  /**
   * VMS - 85 - 07 - outcome determined by CLM_FINAL_ACTION - Multiple surgery with UT error, auto
   * denial.
   */
  _85_07(
      MetaSourceId.VMS,
      "85",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Multiple surgery with UT error, auto denial"),

  /** VMS - 85 - 09 - outcome determined by CLM_FINAL_ACTION - DOD/REP. */
  _85_09(
      MetaSourceId.VMS,
      "85",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "DOD/REP"),

  /** VMS - 86 - 07 - outcome determined by CLM_FINAL_ACTION - E/M location. */
  _86_07(
      MetaSourceId.VMS,
      "86",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "E/M location"),

  /** VMS - 86 - 09 - outcome determined by CLM_FINAL_ACTION - UR 11 rejects. */
  _86_09(
      MetaSourceId.VMS,
      "86",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "UR 11 rejects"),

  /**
   * VMS - 87 - 00 - outcome determined by CLM_FINAL_ACTION - Claim sent to HIGLAS on the 837
   * Interface file.
   */
  _87_00(
      MetaSourceId.VMS,
      "87",
      ClaimAuditTrailLocationCode.COMPLETED,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Claim sent to HIGLAS on the 837 Interface file"),

  /** VMS - 87 - 09 - outcome determined by CLM_FINAL_ACTION - UR 08 rejects. */
  _87_09(
      MetaSourceId.VMS,
      "87",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "UR 08 rejects"),

  /** VMS - 88 - 09 - outcome determined by CLM_FINAL_ACTION - Name incorrect. */
  _88_09(
      MetaSourceId.VMS,
      "88",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Name incorrect"),

  /**
   * VMS - 89 - 08 - outcome determined by CLM_FINAL_ACTION - Acknowledgment (Disposition Code 09).
   */
  _89_08(
      MetaSourceId.VMS,
      "89",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Acknowledgment (Disposition Code 09)"),

  /** VMS - 90 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _90_03(
      MetaSourceId.VMS,
      "90",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 90 - 07 - outcome determined by CLM_FINAL_ACTION - Batch repricing process. */
  _90_07(
      MetaSourceId.VMS,
      "90",
      ClaimAuditTrailLocationCode.UTILIZATION_DUPLICATION,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Batch repricing process"),

  /** VMS - 90 - 08 - outcome determined by CLM_FINAL_ACTION - Reply Disposition Code 90. */
  _90_08(
      MetaSourceId.VMS,
      "90",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Reply Disposition Code 90"),

  /** VMS - 91 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _91_03(
      MetaSourceId.VMS,
      "91",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 92 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _92_03(
      MetaSourceId.VMS,
      "92",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 94 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _94_03(
      MetaSourceId.VMS,
      "94",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 95 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _95_03(
      MetaSourceId.VMS,
      "95",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 95 - 08 - outcome determined by CLM_FINAL_ACTION - S, M, G query. */
  _95_08(
      MetaSourceId.VMS,
      "95",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "S, M, G query"),

  /** VMS - 96 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _96_03(
      MetaSourceId.VMS,
      "96",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 96 - 08 - outcome determined by CLM_FINAL_ACTION - T, N, H query. */
  _96_08(
      MetaSourceId.VMS,
      "96",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "T, N, H query"),

  /** VMS - 96 - 09 - outcome determined by CLM_FINAL_ACTION - CWF Error. */
  _96_09(
      MetaSourceId.VMS,
      "96",
      ClaimAuditTrailLocationCode.REPLY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "CWF Error"),

  /** VMS - 97 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _97_03(
      MetaSourceId.VMS,
      "97",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 97 - 08 - outcome determined by CLM_FINAL_ACTION - R, L, I query. */
  _97_08(
      MetaSourceId.VMS,
      "97",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "R, L, I query"),

  /** VMS - 98 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _98_03(
      MetaSourceId.VMS,
      "98",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 98 - 08 - outcome determined by CLM_FINAL_ACTION - Resend to CWF. */
  _98_08(
      MetaSourceId.VMS,
      "98",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Resend to CWF"),

  /** VMS - 99 - 03 - outcome determined by CLM_FINAL_ACTION - OQC. */
  _99_03(
      MetaSourceId.VMS,
      "99",
      ClaimAuditTrailLocationCode.DME_OQC,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "OQC"),

  /** VMS - 99 - 04 - outcome determined by CLM_FINAL_ACTION - Duplicate claim. */
  _99_04(
      MetaSourceId.VMS,
      "99",
      ClaimAuditTrailLocationCode.SYSTEM_REJECT,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Duplicate claim"),

  /**
   * VMS - 99 - 08 - outcome determined by CLM_FINAL_ACTION - Regular – Entry Code 1 and follow-up
   * claims to CWF.
   */
  _99_08(
      MetaSourceId.VMS,
      "99",
      ClaimAuditTrailLocationCode.QUERY,
      ExplanationOfBenefit.RemittanceOutcome.NULL,
      "Regular – Entry Code 1 and follow-up claims to CWF");

  private final MetaSourceId source;
  private final String statusCode;
  private final ClaimAuditTrailLocationCode locationCode;
  private final ExplanationOfBenefit.RemittanceOutcome outcome;
  private final String display;

  private String getCompositeStatusCode() {
    return source.getPrefix() + locationCode.getCode() + statusCode;
  }

  /**
   * Currently, we cannot confidently determine outcome for VMS since VMS is dependent on claim
   * audit trail status code and claim audit trail location code. Instead, we use claim final action
   * to determine the outcome for VMS.
   *
   * @param finalAction the claim final action code
   * @return the EOB outcome
   */
  public ExplanationOfBenefit.RemittanceOutcome getOutcome(ClaimFinalAction finalAction) {
    if (source.equals(MetaSourceId.VMS)
        && outcome.equals(ExplanationOfBenefit.RemittanceOutcome.NULL)) {
      if (finalAction.equals(ClaimFinalAction.YES)) {
        return ExplanationOfBenefit.RemittanceOutcome.COMPLETE;
      } else if (finalAction.equals(ClaimFinalAction.NO)) {
        return ExplanationOfBenefit.RemittanceOutcome.PARTIAL;
      }
    }
    return outcome;
  }

  private static final Map<Key, ClaimAuditTrailStatusCode> CLAIM_STATUS_LOOKUP =
      Arrays.stream(values())
          .collect(
              Collectors.toMap(
                  c -> new Key(c.source, c.statusCode, c.locationCode), Function.identity()));

  private record Key(
      MetaSourceId source, String statusCode, ClaimAuditTrailLocationCode locationCode) {}

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
      MetaSourceId source, String statusCode, ClaimAuditTrailLocationCode locationCode) {
    return Optional.ofNullable(CLAIM_STATUS_LOOKUP.get(new Key(source, statusCode, locationCode)));
  }

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_AUDT_TRL_STUS_CD.toFhir())
        .setCode(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_AUDIT_TRAIL_STATUS_CODE)
                    .setDisplay(display)
                    .setCode(getCompositeStatusCode())));
  }
}
