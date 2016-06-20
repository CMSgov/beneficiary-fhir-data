package gov.hhs.cms.bluebutton.datapipeline.rif.model;

/**
 * Used in RIF files to indicate what type of DB operation that a given
 * row/record represents.
 */
public enum RecordAction {
	INSERT("INSERT"),

	/*
	 * TODO
	 */
	UPDATE("TODO1"),

	/*
	 * TODO
	 */
	DELETE("TODO2");

	private final String textRepresentation;

	/**
	 * Enum constant constructor.
	 * 
	 * @param textRepresentation
	 *            the value used in the RIF file format to represent this
	 *            {@link RecordAction}.
	 */
	private RecordAction(String textRepresentation) {
		this.textRepresentation = textRepresentation;
	}

	/**
	 * @param value
	 *            the text representation of the {@link RecordAction} to be
	 *            returned
	 * @return the {@link RecordAction} that matches the specified text value
	 */
	public static RecordAction match(String value) {
		for (RecordAction recordAction : RecordAction.values())
			if (recordAction.textRepresentation.equals(value))
				return recordAction;
		throw new IllegalArgumentException("Unknown value: " + value);
	}
}
