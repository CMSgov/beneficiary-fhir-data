package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IcdIndicator {
  DEFAULT("", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  ICD_9("9", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  ICD_10("0", SystemUrls.CMS_ICD_10, SystemUrls.ICD_10_CM);

  public static IcdIndicator fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  private final String code;
  private final String procedureSystem;
  private final String diagnosisSytem;
}
