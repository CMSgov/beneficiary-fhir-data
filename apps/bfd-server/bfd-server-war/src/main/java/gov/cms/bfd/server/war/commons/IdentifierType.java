package gov.cms.bfd.server.war.commons;

/**
 * Enumerates the identifier types of values that for different code sets and their corresponding
 * system, code, and display values.
 */
public enum IdentifierType {
  /** Represents the NPI code set. */
  NPI(
      TransformerConstants.CODING_NPI_US,
      TransformerConstants.CODED_IDENTIFIER_TYPE_NPI,
      TransformerConstants.CODED_IDENTIFIER_TYPE_NPI_DISPLAY),
  /** Represents the UPIN code set. */
  UPIN(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_UPIN,
      TransformerConstants.CODED_IDENTIFIER_TYPE_UPIN_DISPLAY),
  /** Represents the NCPDP code set. */
  NCPDP(
      TransformerConstants.CODING_SYSTEM_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_PDP,
      TransformerConstants.CODED_IDENTIFIER_TYPE_PDP_DISPLAY),
  /** Represents the SL code set. */
  SL(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_DL,
      TransformerConstants.CODED_IDENTIFIER_TYPE_DL_DISPLAY),
  /** Represents the TAX code set. */
  TAX(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_TAX,
      TransformerConstants.CODED_IDENTIFIER_TYPE_TAX_DISPLAY),
  /** Represents the MC code set. */
  MC(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_MC,
      TransformerConstants.CODED_IDENTIFIER_TYPE_MC_DISPLAY);

  /** The system for this code set. */
  public final String system;
  /** The code for this code set. */
  public final String code;
  /** The display for this code set. */
  public final String display;

  /**
   * Instantiates a new Identifier type.
   *
   * @param system the system
   * @param code the code
   * @param display the display
   */
  IdentifierType(String system, String code, String display) {
    this.system = system;
    this.code = code;
    this.display = display;
  }

  /**
   * Gets the {@link #system}.
   *
   * @return the string for the corresponding system for the enum value
   */
  public String getSystem() {
    return system;
  }

  /**
   * Gets the {@link #code}.
   *
   * @return the string for the corresponding code for the enum value
   */
  public String getCode() {
    return code;
  }

  /**
   * Gets the {@link #display}.
   *
   * @return the string for the corresponding display for the enum value
   */
  public String getDisplay() {
    return display;
  }
}
