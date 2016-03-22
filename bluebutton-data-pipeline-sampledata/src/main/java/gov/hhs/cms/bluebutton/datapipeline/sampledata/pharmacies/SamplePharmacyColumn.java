package gov.hhs.cms.bluebutton.datapipeline.sampledata.pharmacies;

/**
 * Enumerates some of the columns in the
 * <code>src/main/resources/pharmacy-names.csv</code> file.
 */
enum SamplePharmacyColumn {
	NPI(0, "NPI"),

	NAME_LEGAL(1, "Legal Business Name"),

	NAME_BUSINESS(2, "Doing-Business-As Name");

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
	private SamplePharmacyColumn(int columnIndex, String columnName) {
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
