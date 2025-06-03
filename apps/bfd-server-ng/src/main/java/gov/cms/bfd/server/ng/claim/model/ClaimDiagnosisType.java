package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ClaimDiagnosisType {
  PRINCIPAL("P", "principal", SystemUrls.HL7_DIAGNOSIS_TYPE),
  ADMITTING("A", "admitting", SystemUrls.HL7_DIAGNOSIS_TYPE),
  FIRST("1", "externalcauseofinjury", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  PRESENT_ON_ADMISSION("D", "other", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE),
  DIAGNOSIS_E_CODE("E", "externalcauseofinjury", SystemUrls.CARIN_CODE_SYSTEM_DIAGNOSIS_TYPE);

  private String idrCode;
  private String fhirCode;
  private String system;

  public static ClaimDiagnosisType fromIdrCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst().get();
  }
}
