package gov.cms.bfd.server.war.stu3.providers;

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

  public String bySystem() {
    return system;
  }

  public String byCode() {
    return code;
  }

  public String byDisplay() {
    return display;
  }
}
