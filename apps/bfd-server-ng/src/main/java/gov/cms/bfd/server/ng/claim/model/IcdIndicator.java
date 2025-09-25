package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Indicates the ICD code system used. */
@Getter
@AllArgsConstructor
public enum IcdIndicator {
  /** Not specified (default to ICD 9). */
  DEFAULT("", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  /** ICD 9. */
  ICD_9("9", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  /** ICD 10. */
  ICD_10("0", SystemUrls.CMS_ICD_10, SystemUrls.ICD_10_CM);

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return ICD indicator
   */
  public static Optional<IcdIndicator> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  private final String code;
  private final String procedureSystem;
  private final String diagnosisSystem;

  /**
   * Formats a raw diagnosis code string according to the rules for this specific ICD system. For
   * ICD-10, it inserts a dot after the first 3 characters if the code is long enough. For ICD-9, If
   * the code contains a '.' returns code the unchanged. If fully numeric and length > 3, inserts
   * '.' after the 3rd char. If starts with 'E' and length > 4, insert '.' after the 4th char. If
   * starts with 'V' and length > 3, insert '.' after the 3rd char. All other systems it returns the
   * code as-is.
   *
   * @param rawCode The raw diagnosis code string from the database.
   * @return The correctly formatted diagnosis code string for use in FHIR.
   */
  public String formatCode(String rawCode) {
    if (rawCode == null || rawCode.isEmpty()) return rawCode;

    // If the code contains a dot, it's already formatted.
    if (rawCode.indexOf('.') >= 0) return rawCode;

    if (this == ICD_10) return formatIcd10(rawCode);
    if (this == ICD_9) return formatIcd9Diagnosis(rawCode);

    return rawCode;
  }

  private String formatIcd10(String rawCode) {
    if (rawCode.length() > 3) return rawCode.substring(0, 3) + "." + rawCode.substring(3);
    return rawCode;
  }

  private String formatIcd9Diagnosis(String rawCode) {
    // Fully numeric insert dot after 3rd char when long enough.
    if (rawCode.matches("\\d+") && rawCode.length() > 3) {
      return rawCode.substring(0, 3) + "." + rawCode.substring(3);
    }

    // Codes starting with 'E' . after 4th char when long enough.
    if (!rawCode.isEmpty()
        && Character.toUpperCase(rawCode.charAt(0)) == 'E'
        && rawCode.length() > 4) {
      return rawCode.substring(0, 4) + "." + rawCode.substring(4);
    }

    // Codes starting with 'V' . after 3rd char when long enough.
    if (!rawCode.isEmpty()
        && Character.toUpperCase(rawCode.charAt(0)) == 'V'
        && rawCode.length() > 3) {
      return rawCode.substring(0, 3) + "." + rawCode.substring(3);
    }

    return rawCode;
  }

  /**
   * Formats a procedure code for this ICD system.
   *
   * @param rawCode raw procedure code
   * @return formatted procedure code
   */
  public String formatProcedureCode(String rawCode) {
    if (rawCode == null || rawCode.isEmpty()) return rawCode;
    if (rawCode.indexOf('.') >= 0) return rawCode;

    if (this == ICD_9) {
      if (rawCode.length() > 2) {
        return rawCode.substring(0, 2) + "." + rawCode.substring(2);
      }
      return rawCode;
    }

    return rawCode;
  }
}
