package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@Getter
@AllArgsConstructor
enum ClaimSubtype {
  CARRIER("carrier"),
  DME("dme"),
  HHA("hha"),
  HOSPICE("hospice"),
  INPATIENT("inpatient"),
  OUTPATIENT("outpatient"),
  PDE("pde"),
  SNF("snf");

  private final String code;

  Coding toFhir() {
    return new Coding().setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE).setCode(code);
  }

  public static ClaimSubtype fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst().get();
  }
}
