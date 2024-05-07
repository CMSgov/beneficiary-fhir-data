package gov.cms.model.dsl.codegen.plugin.util;

/** An enum for signifying the BFD API versions. */
public enum Version {
  /** Version 1 of the BFD API. */
  V1("dstu3"),
  /** Version 2 of the BFD API. */
  V2("r4");

  /** Label of Version enum. */
  public final String label;

  /**
   * Constructor for Version enum.
   *
   * @param label label
   */
  private Version(String label) {
    this.label = label;
  }
}
