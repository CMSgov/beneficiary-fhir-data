package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@Getter
@AllArgsConstructor
enum ClaimSubtype {
  INPATIENT("inpatient"),
  OUTPATIENT("outpatient");

  private final String code;

  Coding toFhir() {
    return new Coding().setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE).setCode(code);
  }
}
