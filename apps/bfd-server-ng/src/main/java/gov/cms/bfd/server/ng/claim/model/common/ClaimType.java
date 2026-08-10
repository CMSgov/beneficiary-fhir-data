package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

@SuppressWarnings({
  "checkstyle:JavadocVariable",
  "checkstyle:MissingJavadocType",
  "checkstyle:MissingJavadocMethod"
})
@Getter
@AllArgsConstructor
public enum ClaimType {
  PHARMACY("pharmacy"),
  INSTITUTIONAL("institutional"),
  PROFESSIONAL("professional");

  private final String code;

  public Coding toFhir() {
    return new Coding().setSystem(SystemUrls.HL7_CLAIM_TYPE).setCode(code);
  }
}
