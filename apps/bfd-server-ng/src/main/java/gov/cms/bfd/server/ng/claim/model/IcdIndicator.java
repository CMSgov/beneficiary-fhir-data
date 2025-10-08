package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ICD indicator types for ICD-9 and ICD-10, providing code system URLs and formatting logic for
 * diagnosis and procedure codes.
 */
@Getter
@AllArgsConstructor
public enum IcdIndicator {
  /** Not specified (default to ICD 9). */
  DEFAULT("", SystemUrls.CMS_ICD_9_PROCEDURE, SystemUrls.ICD_9_CM_DIAGNOSIS) {
    @Override
    public String formatDiagnosisCode(String rawCode) {
      return formatIcd9Diagnosis(rawCode);
    }

    @Override
    public String formatProcedureCode(String rawCode) {
      return formatIcd9Procedure(rawCode);
    }
  },
  /** ICD 9. */
  ICD_9("9", SystemUrls.CMS_ICD_9_PROCEDURE, SystemUrls.ICD_9_CM_DIAGNOSIS) {
    @Override
    public String formatDiagnosisCode(String rawCode) {
      return formatIcd9Diagnosis(rawCode);
    }

    @Override
    public String formatProcedureCode(String rawCode) {
      return formatIcd9Procedure(rawCode);
    }
  },
  /** ICD 10. */
  ICD_10("0", SystemUrls.CMS_ICD_10_PROCEDURE, SystemUrls.ICD_10_CM_DIAGNOSIS) {
    @Override
    public String formatDiagnosisCode(String rawCode) {
      return formatIcd10(rawCode);
    }

    @Override
    public String formatProcedureCode(String rawCode) {
      return rawCode;
    }
  };

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
   * Formats a raw diagnosis code by the rules of this ICD system.
   *
   * @param rawCode The raw diagnosis code string from the database.
   * @return Formatted diagnosis code string.
   */
  public abstract String formatDiagnosisCode(String rawCode);

  /**
   * Formats a procedure code for this ICD system.
   *
   * @param rawCode raw procedure code
   * @return formatted procedure code
   */
  public abstract String formatProcedureCode(String rawCode);

  private static String formatIcd10(String rawCode) {
    if (rawCode.contains(".")) {
      return rawCode;
    }
    return insertDot(rawCode, 3);
  }

  private static String formatIcd9Diagnosis(String rawCode) {
    if (rawCode.contains(".")) {
      return rawCode;
    }
    // Fully numeric insert dot after 3rd char when long enough.
    if (rawCode.chars().allMatch(Character::isDigit)) {
      return insertDot(rawCode, 3);
    }

    // Codes starting with 'E' . after 4th char when long enough.
    if (Character.toUpperCase(rawCode.charAt(0)) == 'E') {
      return insertDot(rawCode, 4);
    }
    // Codes starting with 'V' . after 3rd char when long enough.
    if (Character.toUpperCase(rawCode.charAt(0)) == 'V') {
      return insertDot(rawCode, 3);
    }

    return rawCode;
  }

  private static String formatIcd9Procedure(String rawCode) {
    if (rawCode.indexOf('.') >= 0) {
      return rawCode;
    }
    return insertDot(rawCode, 2);
  }

  private static String insertDot(String rawCode, int position) {
    if (rawCode.length() > position) {
      return rawCode.substring(0, position) + "." + rawCode.substring(position);
    }
    return rawCode;
  }
}
