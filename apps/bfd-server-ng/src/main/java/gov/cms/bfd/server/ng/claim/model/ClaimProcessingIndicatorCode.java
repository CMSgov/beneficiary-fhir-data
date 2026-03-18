package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim processing indicator codes. Suppress SonarQube warning that constant names should comply
 * with naming conventions.
 */
@AllArgsConstructor
@Getter
public enum ClaimProcessingIndicatorCode {
  /** A - Allowed. */
  A("A", "Allowed"),
  /** B - Benefits exhausted. */
  B("B", "Benefits exhausted"),
  /** C - Non-covered care. */
  C("C", "Non-covered care"),
  /** D - Denied (from BMAD). */
  D("D", "Denied (from BMAD)"),
  /** G - MSP cost avoided â€” Secondary Claims Investigation. */
  G("G", "MSP cost avoided â€” Secondary Claims Investigation"),
  /** H - MSP cost avoided â€” Self Reports. */
  H("H", "MSP cost avoided â€” Self Reports"),
  /** I - Invalid data. */
  I("I", "Invalid data"),
  /** J - MSP cost avoided â€” 411.25. */
  J("J", "MSP cost avoided â€” 411.25"),
  /** K - MSP cost avoided â€” Insurer Voluntary Reporting. */
  K("K", "MSP cost avoided â€” Insurer Voluntary Reporting"),
  /** L - CLIA. */
  L("L", "CLIA"),
  /** M - Multiple submittal-duplicate line item. */
  M("M", "Multiple submittal-duplicate line item"),
  /** N - Medically unnecessary. */
  N("N", "Medically unnecessary"),
  /** O - Other. */
  O("O", "Other"),
  /** P - Physician ownership denial. */
  P("P", "Physician ownership denial"),
  /** Q - MSP cost avoided (contractor #88888) â€” voluntary agreement. */
  Q("Q", "MSP cost avoided (contractor #88888) â€” voluntary agreement"),
  /** R - Reprocessed adjustments based on subsequent reprocessing of claim. */
  R("R", "Reprocessed adjustments based on subsequent reprocessing of claim"),
  /** S - Secondary payer. */
  S("S", "Secondary payer"),
  /** T - MSP cost avoided â€” IEQ contractor. */
  T("T", "MSP cost avoided â€” IEQ contractor"),
  /** U - MSP cost avoided â€” HMO rate cell adjustment. */
  U("U", "MSP cost avoided â€” HMO rate cell adjustment"),
  /** V - MSP cost avoided â€” litigation settlement. */
  V("V", "MSP cost avoided â€” litigation settlement"),
  /** X - MSP cost avoided â€” generic. */
  X("X", "MSP cost avoided â€” generic"),
  /** Y - MSP cost avoided â€” IRS/SSA data match project. */
  Y("Y", "MSP cost avoided â€” IRS/SSA data match project"),
  /** Z - Bundled test, no payment. */
  Z("Z", "Bundled test, no payment"),
  /** 00 - MSP cost avoided â€” COB Contractor. */
  _00("00", "MSP cost avoided â€” COB Contractor"),
  /** 12 - MSP cost avoided â€” BC/BS Voluntary Agreements. */
  _12("12", "MSP cost avoided â€” BC/BS Voluntary Agreements"),
  /** 13 - MSP cost avoided â€” Office of Personnel Management. */
  _13("13", "MSP cost avoided â€” Office of Personnel Management"),
  /** 14 - MSP cost avoided â€” Workman's Compensation (WC) Datamatch. */
  _14("14", "MSP cost avoided â€” Workman's Compensation (WC) Datamatch"),
  /**
   * 15 - MSP cost avoided â€” Workman's Compensation Insurer Voluntary Data Sharing Agreements (WC
   * VDSA) (eff. 4/2006).
   */
  _15(
      "15",
      "MSP cost avoided â€” Workman's Compensation Insurer Voluntary Data Sharing Agreements (WC VDSA) (eff. 4/2006)"),
  /** 16 - MSP cost avoided â€” Liability Insurer VDSA (eff.4/2006). */
  _16("16", "MSP cost avoided â€” Liability Insurer VDSA (eff.4/2006)"),
  /** 17 - MSP cost avoided â€” No-Fault Insurer VDSA (eff.4/2006). */
  _17("17", "MSP cost avoided â€” No-Fault Insurer VDSA (eff.4/2006)"),
  /** 18 - MSP cost avoided â€” Pharmacy Benefit Manager Data Sharing Agreement (eff.4/2006). */
  _18("18", "MSP cost avoided â€” Pharmacy Benefit Manager Data Sharing Agreement (eff.4/2006)"),
  /** 21 - MSP cost avoided â€” MIR Group Health Plan (eff.1/2009). */
  _21("21", "MSP cost avoided â€” MIR Group Health Plan (eff.1/2009)"),
  /** 22 - MSP cost avoided â€” MIR non-Group Health Plan (eff.1/2009). */
  _22("22", "MSP cost avoided â€” MIR non-Group Health Plan (eff.1/2009)"),
  /** 25 - MSP cost avoided â€” Recovery Audit Contractor â€” California (eff.10/2005). */
  _25("25", "MSP cost avoided â€” Recovery Audit Contractor â€” California (eff.10/2005)"),
  /** 26 - MSP cost avoided â€” Recovery Audit Contractor â€” Florida (eff.10/2005). */
  _26("26", "MSP cost avoided â€” Recovery Audit Contractor â€” Florida (eff.10/2005)");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim processing indicator code
   */
  public static Optional<ClaimProcessingIndicatorCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PRCSG_IND_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PRCSG_IND_CD, code, display));
  }
}
