package gov.cms.bfd.server.ng.coverage;

import lombok.Getter;

/** Yes No Unknown Indicator. */
@Getter
public enum YesNoUnknownIndicator {
  /** Represents Y Indicator. */
  YES("Y"),
  /** Represents N Indicator. */
  NO("N"),
  /** Represents U Indicator. */
  UNKNOWN("U");

  private final String code;

  YesNoUnknownIndicator(String code) {
    this.code = code;
  }
}
