package gov.cms.bfd.server.war.stu3.providers;

public enum ServiceProviderIdentifiers {
  NPI(TransformerConstants.CODING_NPI_US, "NPI", "National Provider Identifier"),
  UPIN(TransformerConstants.CODING_UPIN, "UPIN", "Unique Physician Identification Number"),
  NCPDP(
      TransformerConstants.CODING_NCPDP,
      "NCPDP",
      "National Council for Prescription Drug Programs"),
  SL(TransformerConstants.CODING_STATE_LICENSE, "DL", "State license number"),
  FTN(TransformerConstants.CODING_FEDERAL_TAX_NUM, "TAX", "Federal tax number");

  public final String system;
  public final String code;
  public final String display;

  private ServiceProviderIdentifiers(String system, String code, String display) {
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
