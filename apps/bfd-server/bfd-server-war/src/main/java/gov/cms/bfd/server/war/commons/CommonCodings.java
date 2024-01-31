package gov.cms.bfd.server.war.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** An enum encapsulating a variety of common codings. */
@Getter
@AllArgsConstructor
public enum CommonCodings {

  /** The medicare number. */
  MC(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MC", "Patient's Medicare Number");

  /** The coding's system. */
  private final String system;

  /** The coding's code. */
  private final String code;

  /** The coding's display. */
  private final String display;
}
