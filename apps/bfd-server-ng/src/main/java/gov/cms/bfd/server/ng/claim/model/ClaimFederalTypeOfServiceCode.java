package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Claim federal type of service codes. Suppress SonarQube warning that constant names should comply
 * with * naming conventions.
 */
@AllArgsConstructor
@Getter
public enum ClaimFederalTypeOfServiceCode {
  /** 1 - Medical care. */
  _1("1", "Medical care"),
  /** 2 - Surgery. */
  _2("2", "Surgery"),
  /** 3 - Consultation. */
  _3("3", "Consultation"),
  /** 4 - Diagnostic radiology. */
  _4("4", "Diagnostic radiology"),
  /** 5 - Diagnostic laboratory. */
  _5("5", "Diagnostic laboratory"),
  /** 6 - Therapeutic radiology. */
  _6("6", "Therapeutic radiology"),
  /** 7 - Anesthesia. */
  _7("7", "Anesthesia"),
  /** 8 - Assistant at surgery. */
  _8("8", "Assistant at surgery"),
  /** 9 - Other medical items or services. */
  _9("9", "Other medical items or services"),
  /** 0 - Whole blood only eff 01/96, whole blood or packed red cells before 01/96. */
  _0("0", "Whole blood only eff 01/96, whole blood or packed red cells before 01/96"),
  /** A - Used durable medical equipment (DME). */
  A("A", "Used durable medical equipment (DME)"),
  /** B - High risk screening mammography (obsolete 1/1/98). */
  B("B", "High risk screening mammography (obsolete 1/1/98)"),
  /** C - Low risk screening mammography (obsolete 1/1/98). */
  C("C", "Low risk screening mammography (obsolete 1/1/98)"),
  /** D - Ambulance (eff 04/95). */
  D("D", "Ambulance (eff 04/95)"),
  /** E - Enteral/parenteral nutrients/supplies (eff 04/95). */
  E("E", "Enteral/parenteral nutrients/supplies (eff 04/95)"),
  /** F - Ambulatory surgical center (facility usage for surgical services). */
  F("F", "Ambulatory surgical center (facility usage for surgical services)"),
  /** G - Immunosuppressive drugs. */
  G("G", "Immunosuppressive drugs"),
  /** H - Hospice services (discontinued 01/95). */
  H("H", "Hospice services (discontinued 01/95)"),
  /** I - Purchase of DME (installment basis) (discontinued 04/95). */
  I("I", "Purchase of DME (installment basis) (discontinued 04/95)"),
  /** J - Diabetic shoes (eff 04/95). */
  J("J", "Diabetic shoes (eff 04/95)"),
  /** K - Hearing items and services (eff 04/95). */
  K("K", "Hearing items and services (eff 04/95)"),
  /** L - ESRD supplies (eff 04/95) (renal supplier in the home before 04/95). */
  L("L", "ESRD supplies (eff 04/95) (renal supplier in the home before 04/95)"),
  /** M - Monthly capitation payment for dialysis. */
  M("M", "Monthly capitation payment for dialysis"),
  /** N - Kidney donor. */
  N("N", "Kidney donor"),
  /** P - Lump sum purchase of DME, prosthetics orthotics. */
  P("P", "Lump sum purchase of DME, prosthetics orthotics"),
  /** Q - Vision items or services. */
  Q("Q", "Vision items or services"),
  /** R - Rental of DME. */
  R("R", "Rental of DME"),
  /** S - Surgical dressings or other medical supplies (eff 04/95). */
  S("S", "Surgical dressings or other medical supplies (eff 04/95)"),
  /**
   * T - Psychological therapy (term. 12/31/97) outpatient mental health limitation (eff. 1/1/98).
   */
  T(
      "T",
      "Psychological therapy (term. 12/31/97) outpatient mental health limitation (eff. 1/1/98)"),
  /** U - Occupational therapy. */
  U("U", "Occupational therapy"),
  /**
   * V - Pneumococcal/flu vaccine (eff 01/96), Pneumococcal/flu/hepatitis B vaccine (eff
   * 04/95-12/95), Pneumococcal only before 04/95.
   */
  V(
      "V",
      "Pneumococcal/flu vaccine (eff 01/96), Pneumococcal/flu/hepatitis B vaccine (eff 04/95-12/95), Pneumococcal only before 04/95"),
  /** W - Physical therapy. */
  W("W", "Physical therapy"),
  /** Y - Second opinion on elective surgery (obsoleted 1/97). */
  Y("Y", "Second opinion on elective surgery (obsoleted 1/97)"),
  /** Z - Third opinion on elective surgery (obsoleted 1/97). */
  Z("Z", "Third opinion on elective surgery (obsoleted 1/97)");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim federal type of service code
   */
  public static Optional<ClaimFederalTypeOfServiceCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_FED_TYPE_SRVC_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_FED_TYPE_SRVC_CD, code, display));
  }
}
