package gov.cms.bfd.server.ng.testUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SamhsaCertType {
  NO_CERT(null),
  SAMHSA_ALLOWED_CERT("samhsa_allowed"),
  SAMHSA_NOT_ALLOWED_CERT("samhsa_not_allowed");

  private final String certValue;
}
