package gov.cms.bfd.server.war.commons;

/**
 * Enumerates the idenitifer types of values that for different code sets and their corresponding
 * system, code, and display values.
 */
public enum IdentifierType {
  NPI(
      TransformerConstants.CODING_NPI_US,
      TransformerConstants.CODED_IDENTIFIER_TYPE_NPI,
      TransformerConstants.CODED_IDENTIFIER_TYPE_NPI_DISPLAY),
  UPIN(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_UPIN,
      TransformerConstants.CODED_IDENTIFIER_TYPE_UPIN_DISPLAY),
  NCPDP(
      TransformerConstants.CODING_SYSTEM_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_PDP,
      TransformerConstants.CODED_IDENTIFIER_TYPE_PDP_DISPLAY),
  SL(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_DL,
      TransformerConstants.CODED_IDENTIFIER_TYPE_DL_DISPLAY),
  FTN(
      TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
      TransformerConstants.CODED_IDENTIFIER_TYPE_TAX,
      TransformerConstants.CODED_IDENTIFIER_TYPE_TAX_DISPLAY);

  public final String system;
  public final String code;
  public final String display;

  private IdentifierType(String system, String code, String display) {
    this.system = system;
    this.code = code;
    this.display = display;
  }
  /** @return the string for the corresponding system for the enum value */
  public String getSystem() {
    return system;
  }
  /** @return the string for the corresponding code for the enum value */
  public String getCode() {
    return code;
  }
  /** @return the string for the corresponding display for the enum value */
  public String getDisplay() {
    return display;
  }
}
