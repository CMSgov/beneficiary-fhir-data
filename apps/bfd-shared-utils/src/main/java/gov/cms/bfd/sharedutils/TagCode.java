package gov.cms.bfd.sharedutils;

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

  /**
   * Converts a string from the DB to the corresponding TagCode enum.
   *
   * @param text The string value from the DB.
   * @return The corresponding TagCode enum, or null if no match is found.
   */
  public static TagCode fromString(String text) {
    if (text != null) {
      for (TagCode tag : TagCode.values()) {
        if (tag.dbName.equalsIgnoreCase(text)) {
          return tag;
        }
      }
    }
    return null;
  }
}
