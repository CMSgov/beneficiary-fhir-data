package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Claim primary payer codes. */
public sealed interface ClaimPrimaryPayerCode
    permits ClaimPrimaryPayerCode.Valid, ClaimPrimaryPayerCode.Invalid {

  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim primary payer code or empty Optional if code is null or blank
   */
  static Optional<ClaimPrimaryPayerCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPrimaryPayerCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PRMRY_PYR_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PRMRY_PYR_CD, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimPrimaryPayerCode {
    /** A - WORKING AGED BENEFICIARY/SPOUSE WITH EMPLOYER GROUP HEALTH PLAN. */
    A("A", "WORKING AGED BENEFICIARY/SPOUSE WITH EMPLOYER GROUP HEALTH PLAN"),
    /** B - ESRD BENEFICIARY IN 12 MONTH PERIOD WITH EMPLOYER GROUP HEALTH PLAN. */
    B("B", "ESRD BENEFICIARY IN 12 MONTH PERIOD WITH EMPLOYER GROUP HEALTH PLAN"),
    /** C - ANY CONDITIONAL MEDICARE PAYMENT SITUATION. */
    C("C", "ANY CONDITIONAL MEDICARE PAYMENT SITUATION"),
    /** D - AUTOMOBILE NO FAULT OR ANY LIABILITY INSURANCE. */
    D("D", "AUTOMOBILE NO FAULT OR ANY LIABILITY INSURANCE"),
    /** E - WORKER'S COMPENSATION INCLUDING BLACK LUNG. */
    E("E", "WORKER'S COMPENSATION INCLUDING BLACK LUNG"),
    /** F - VETERAN'S AFFAIRS, PUBLIC HEALTH SERVICE, OR OTHER FEDERAL AGENCY. */
    F("F", "VETERAN'S AFFAIRS, PUBLIC HEALTH SERVICE, OR OTHER FEDERAL AGENCY"),
    /** G - WORKING DISABLED. */
    G("G", "WORKING DISABLED"),
    /** H - BLACK LUNG. */
    H("H", "BLACK LUNG"),
    /** I - VETERAN'S AFFAIRS. */
    I("I", "VETERAN'S AFFAIRS"),
    /** L - LIABILITY. */
    L("L", "LIABILITY"),
    /** M - OVERRIDE CODE EMPLOYEE GROUP HEALTH PLAN SERVICE INVOLVED. */
    M("M", "OVERRIDE CODE EMPLOYEE GROUP HEALTH PLAN SERVICE INVOLVED"),
    /** N - OVERRIDE CODE NON-EMPLOYEE GROUP HEALTH PLAN SERVICE INVOLVED. */
    N("N", "OVERRIDE CODE NON-EMPLOYEE GROUP HEALTH PLAN SERVICE INVOLVED"),
    /** W - WORKER'S COMPENSATION SET-ASIDE. */
    W("W", "WORKER'S COMPENSATION SET-ASIDE"),
    /** X - OVERRIDE CODE MSP COST AVOIDED. */
    X("X", "OVERRIDE CODE MSP COST AVOIDED"),
    /** Y - OTHER SECONDARY PAYER INVESTIGATION SHOWS MEDICARE PRIMARY. */
    Y("Y", "OTHER SECONDARY PAYER INVESTIGATION SHOWS MEDICARE PRIMARY"),
    /** Z - MEDICARE IS PRIMARY PAYER. */
    Z("Z", "MEDICARE IS PRIMARY PAYER");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPrimaryPayerCode {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
