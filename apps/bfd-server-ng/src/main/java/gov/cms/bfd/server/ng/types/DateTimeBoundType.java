package gov.cms.bfd.server.ng.types;

import ca.uhn.fhir.rest.param.ParamPrefixEnum;

public enum DateTimeBoundType {
  INCLUSIVE,
  EXLCUSIVE;

  public static DateTimeBoundType fromPrefix(ParamPrefixEnum prefix) {
    return switch (prefix) {
      case ParamPrefixEnum.LESSTHAN, ParamPrefixEnum.GREATERTHAN -> DateTimeBoundType.EXLCUSIVE;
      default -> DateTimeBoundType.INCLUSIVE;
    };
  }
}
