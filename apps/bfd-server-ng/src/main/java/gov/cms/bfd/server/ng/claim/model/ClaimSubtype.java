package gov.cms.bfd.server.ng.claim.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClaimSubtype {
  INPATIENT("inpatient"),
  OUTPATIENT("outpatient");

  private final String code;
}
