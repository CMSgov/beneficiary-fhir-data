package gov.cms.bfd.server.ng.claim.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClaimType {
  PHARMACY("pharmacy"),
  INSTITUTIONAL("institutional"),
  PROFESSIONAL("professional");

  private String code;
}
