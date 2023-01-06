package gov.cms.bfd.server.war.commons;

/** Enum for Institutional Claims SubTypes. */
public enum C4BBInstutionalClaimSubtypes {
  Inpatient("inpatient"),
  Outpatient("outpatient");

  public final String label;

  /** Assigning labels to each enum. */
  private C4BBInstutionalClaimSubtypes(String label) {
    this.label = label;
  }
}
