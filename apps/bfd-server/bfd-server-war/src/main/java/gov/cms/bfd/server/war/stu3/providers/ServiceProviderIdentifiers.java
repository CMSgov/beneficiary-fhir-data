package gov.cms.bfd.server.war.stu3.providers;

public enum ServiceProviderIdentifiers {
  NPI("", "NPI", "National Provider Identifier"),
  UPIN("", "UPIN", "Unique Physician Identification Number"),
  NCPDP("", "NCPDP", "National Council for Prescription Drug Programs"),
  SL("", "DL", "State license number"),
  FTN("", "DL", "Federal tax number");

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
