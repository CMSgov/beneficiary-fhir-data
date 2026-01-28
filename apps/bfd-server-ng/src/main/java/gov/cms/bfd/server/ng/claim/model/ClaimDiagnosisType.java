package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
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
  /** E code. */
  DIAGNOSIS_E_CODE("E", "externalcauseofinjury", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  /** R code. */
  DIAGNOSIS_R_CODE("R", "patientreasonforvisit", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  /** Other. */
  OTHER("D", "other", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE);

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

  /**
   * Returns FHIR code based on claim context. Professional context overrides only one case (idrCode
   * "D").
   *
   * @param claimContext context enum
   * @return override value for professional PRESENT_ON_ADMISSION value
   */
  public String getFhirCode(ClaimContext claimContext) {
    if (claimContext == ClaimContext.PROFESSIONAL && this == OTHER) {
      return "secondary"; // override
    }
    return fhirCode;
  }

  /**
   * Determines if the current type is relevant for the diagnosis group. Irrelevant types should not
   * be added to the final mapping.
   *
   * @param allDiagnosisTypes all diagnosis types present in the group
   * @return boolean
   */
  public boolean shouldAdd(Set<ClaimDiagnosisType> allDiagnosisTypes) {
    var hasTypesNotOther = this == OTHER && allDiagnosisTypes.size() > 1;
    var isECodeWithFirst = this == DIAGNOSIS_E_CODE && allDiagnosisTypes.contains(FIRST);
    return !hasTypesNotOther && !isECodeWithFirst;
  }
}
