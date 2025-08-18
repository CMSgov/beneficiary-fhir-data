package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
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
  private final String diagnosisSytem;

  /**
   * Formats a raw diagnosis code string according to the rules for this specific ICD system. For
   * ICD-10, it inserts a dot after the first 3 characters if the code is long enough. For all other
   * systems (e.g., ICD-9), it returns the code as-is.
   *
   * @param rawCode The raw diagnosis code string from the database.
   * @return The correctly formatted diagnosis code string for use in FHIR.
   */
  public String formatCode(String rawCode) {
    if (this == ICD_10 && rawCode.length() > 3) {
      return rawCode.substring(0, 3) + "." + rawCode.substring(3);
    }
    return rawCode;
  }
}
