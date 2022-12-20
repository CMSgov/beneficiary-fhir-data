package gov.cms.bfd.server.war.commons;

public enum CommonCodings {
  MC(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MC", "Patient's Medicare Number");

  private final String system;
  private final String code;
  private final String display;

  CommonCodings(String system, String code, String display) {
    this.system = system;
    this.code = code;
    this.display = display;
  }

  public String getSystem() {
    return system;
  }

  public String getCode() {
    return code;
  }

  public String getDisplay() {
    return display;
  }
}
