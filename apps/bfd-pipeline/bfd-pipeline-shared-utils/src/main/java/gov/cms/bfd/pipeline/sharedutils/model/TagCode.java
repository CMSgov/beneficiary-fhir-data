package gov.cms.bfd.pipeline.sharedutils.model;

/** ENUM of TAG codes. */
public enum TagCode {
  /** Tag Code R. */
  R("R"),
  /** Tag Code 42CFRPart2. */
  _42CFRPart2("42CFRPart2");

  /** Name of the enum in the DB. */
  private String dbName;

  /**
   * Constructor.
   *
   * @param dbName Sets the DB name of the enum.
   */
  TagCode(String dbName) {
    this.dbName = dbName;
  }

  @Override
  public String toString() {
    return this.dbName;
  }
}
