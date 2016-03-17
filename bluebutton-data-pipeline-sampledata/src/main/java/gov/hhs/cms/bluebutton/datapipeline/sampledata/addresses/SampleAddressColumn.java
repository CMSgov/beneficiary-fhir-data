package gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses;

import java.util.Arrays;

/**
 * Enumerates the columns in the
 * <code>src/main/resources/prescriber-addresses.csv</code> file.
 */
enum SampleAddressColumn {
	STREET_1("NPPES Provider Street Address 1"),

	STREET_2("NPPES Provider Street Address 2"),

	CITY("NPPES Provider City"),

	STATE("NPPES Provider State"),

	ZIP("NPPES Provider Zip Code"),

	COUNTRY("NPPES Provider Country");

	private final String columnName;

	/**
	 * Enum constant constructor.
	 * 
	 * @param columnName
	 *            the value to use for {@link #getColumnName()}
	 */
	private SampleAddressColumn(String columnName) {
		this.columnName = columnName;
	}

	/**
	 * @return the name of the column in the CSV file
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * @return a <code>String[]</code> containing all of the
	 *         {@link #getColumnName()}s
	 */
	public static String[] getAllColumnNames() {
		return Arrays.stream(SampleAddressColumn.values()).map(v -> v.getColumnName())
				.toArray(size -> new String[size]);
	}
}
