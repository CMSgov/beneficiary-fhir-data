package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.ParamPrefixEnum;

/** Denotes if the bound is inclusive or exclusive. */
public enum DateTimeBoundType {
  /** Inclusive bound. */
  INCLUSIVE,
  /** Exclusive bound. */
  EXCLUSIVE;

  /**
   * Creates a {@link DateTimeBoundType} from a FHIR prefix.
   *
   * @param prefix FHIR prefix
   * @return bound type
   */
  public static DateTimeBoundType fromPrefix(ParamPrefixEnum prefix) {
    // Note that not all prefix values are valid for a datetime filter.
    return switch (prefix) {
      case ParamPrefixEnum.LESSTHAN, ParamPrefixEnum.GREATERTHAN -> DateTimeBoundType.EXCLUSIVE;

      default -> DateTimeBoundType.INCLUSIVE;
    };
  }
}
