package gov.hhs.cms.bluebutton.datapipeline.sampledata.prescribers;

/**
 * Enumerates some of the columns in the
 * <code>src/main/resources/prescriber-names.csv</code> file.
 */
enum SamplePrescriberColumn {
	NPI(0, "NPI"),

	NAME_FIRST(1, "NPPES Provider First Name"),

	NAME_MIDDLE(2, "NPPES Provider Middle Initial"),

	NAME_LAST_OR_ORG(3, "NPPES Provider Last Name / Organization Name");

	private final int columnIndex;
	private final String columnName;

	/**
	 * Enum constant constructor.
	 * 
	 * @param columnIndex
	 *            the value to use for {@link #getColumnIndex()}
	 * @param columnName
	 *            the value to use for {@link #getColumnName()}
	 */
	private SamplePrescriberColumn(int columnIndex, String columnName) {
		this.columnIndex = columnIndex;
		this.columnName = columnName;
	}

	/**
	 * @return the index of the column in the source CSV file (zero-indexed)
	 */
	public int getColumnIndex() {
		return columnIndex;
	}

	/**
	 * @return the name of the column in the CSV file
	 */
	public String getColumnName() {
		return columnName;
	}
}
