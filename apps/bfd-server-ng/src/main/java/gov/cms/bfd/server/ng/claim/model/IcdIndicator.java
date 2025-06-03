package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum IcdIndicator {
  ICD_9(" ", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  ICD_9("9", SystemUrls.CMS_ICD_9, SystemUrls.ICD_9_CM),
  ICD_10("0", SystemUrls.CMS_ICD_10, SystemUrls.ICD_10_CM);

  public static IcdIndicator fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  private final String code;
  private final String procedureSystem;
  private final String diagnosisSytem;
}
