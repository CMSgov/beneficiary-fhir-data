package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
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
  public static IcdIndicator fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  private final String code;
  private final String procedureSystem;
  private final String diagnosisSytem;
}
