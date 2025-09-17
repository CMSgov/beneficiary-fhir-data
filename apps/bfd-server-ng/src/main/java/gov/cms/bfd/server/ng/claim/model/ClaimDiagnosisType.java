package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Diagnosis types. */
@Getter
@AllArgsConstructor
public enum ClaimDiagnosisType {
  /** Principal diagnosis. */
  PRINCIPAL("P", "principal", SystemUrls.HL7_DIAGNOSIS_TYPE),
  /** Admitting diagnosis. */
  ADMITTING("A", "admitting", SystemUrls.HL7_DIAGNOSIS_TYPE),
  /** First diagnosis. */
  FIRST("1", "externalcauseofinjury", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  /** Present on admission. */
  PRESENT_ON_ADMISSION("D", "other", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  /** E code. */
  DIAGNOSIS_E_CODE("E", "externalcauseofinjury", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  /** R code. */
  DIAGNOSIS_R_CODE("R", "patientreasonforvisit", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE);

  private final String idrCode;
  private final String fhirCode;
  private final String system;

  /**
   * Converts from a database code.
   *
   * @param idrCode database code
   * @return diagnosis type
   */
  public static Optional<ClaimDiagnosisType> tryFromIdrCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }
}
