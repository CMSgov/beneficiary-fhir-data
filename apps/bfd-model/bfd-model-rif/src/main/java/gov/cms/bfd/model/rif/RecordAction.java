package gov.cms.bfd.model.rif;

/** Used in RIF files to indicate what type of DB operation that a given row/record represents. */
public enum RecordAction {
  /** Represents an INSERT action. */
  INSERT("INSERT"),

  /** Represents an UPDATE action. */
  UPDATE("UPDATE"),

  /** Represents a DELETE action; currently unimplemented. TODO: Implement or delete. */
  DELETE("TODO2");

  /** The Text representation of the action. */
  private final String textRepresentation;

  /**
   * Enum constant constructor.
   *
   * @param textRepresentation the value used in the RIF file format to represent this {@link
   *     RecordAction}.
   */
  private RecordAction(String textRepresentation) {
    this.textRepresentation = textRepresentation;
  }

  /**
   * Provides the {@link RecordAction} that matches a string value.
   *
   * @param value the text representation of the {@link RecordAction} to be returned
   * @return the {@link RecordAction} that matches the specified text value
   */
  public static RecordAction match(String value) {
    for (RecordAction recordAction : RecordAction.values())
      if (recordAction.textRepresentation.equals(value)) return recordAction;
    throw new IllegalArgumentException("Unknown value: " + value);
  }
}
