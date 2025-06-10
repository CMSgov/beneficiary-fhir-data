package gov.cms.bfd.server.ng.claim.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClaimLineRevenueCenterCode {
  _0001("0001", "TOTAL_CHARGE");

  private String code;
  private String description;
}
