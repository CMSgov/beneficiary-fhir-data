package gov.cms.bfd.server.war.commons;

/** Enum for Institutional Claims SubTypes. */
public enum C4BBInstutionalClaimSubtypes {
  /** Inpatient subtype. */
  Inpatient("inpatient"),
  /** Outpatient subtype. */
  Outpatient("outpatient");

  /** The subtype label. */
  public final String label;

  /**
   * Assigning labels to each enum.
   *
   * @param label the label
   */
  private C4BBInstutionalClaimSubtypes(String label) {
    this.label = label;
  }
}
