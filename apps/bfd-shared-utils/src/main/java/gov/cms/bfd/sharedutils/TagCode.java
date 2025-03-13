package gov.cms.bfd.sharedutils;

import lombok.Getter;

/** ENUM of TAG codes. */
@Getter
public enum TagCode {
  /** Tag Code R. */
  R("R", "Restricted"),
  /** Tag Code 42CFRPart2. */
  _42CFRPart2("42CFRPart2", "42 CFR Part 2");

  /** Name of the enum in the DB. */
  private final String dbName;

  /** Display name. */
  private final String displayName;

  /**
   * Constructor.
   *
   * @param dbName Sets the DB name of the enum.
   * @param displayName Sets display name of the enum.
   */
  TagCode(String dbName, String displayName) {
    this.dbName = dbName;
    this.displayName = displayName;
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
