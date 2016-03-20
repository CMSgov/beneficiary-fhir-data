package gov.hhs.cms.bluebutton.datapipeline.sampledata.npi;

import java.util.Arrays;

/**
 * Enumerates some of the columns in the
 * <code>src/main/resources/npi/npidata-20050523-20160313-subset-10000.csv</code>
 * file.
 */
enum SampleProviderColumn {
	NPI(0, "NPI"),

	ENTITY_TYPE_CODE(1, "Entity Type Code"),

	NPI_REPLACEMENT(2, "Replacement NPI"),

	NAME_ORG(4, "Provider Organization Name (Legal Business Name)"),

	NAME_LAST(5, "Provider Last Name (Legal Name)"),

	NAME_FIRST(6, "Provider First Name"),

	NAME_MIDDLE(7, "Provider Middle Name"),

	NAME_PREFIX(8, "Provider Name Prefix Text"),

	NAME_SUFFIX(9, "Provider Name Suffix Text"),

	NAME_CREDENTIAL(10, "Provider Credential Text"),

	ADDRESS_BUSINESS_MAILING_LINE_1(21, "Provider First Line Business Mailing Address"),

	ADDRESS_BUSINESS_MAILING_LINE_2(22, "Provider Second Line Business Mailing Address"),

	ADDRESS_BUSINESS_MAILING_CITY(23, "Provider Business Mailing Address City Name"),

	ADDRESS_BUSINESS_MAILING_STATE(24, "Provider Business Mailing Address State Name"),

	ADDRESS_BUSINESS_MAILING_POSTAL(25, "Provider Business Mailing Address Postal Code"),

	ADDRESS_BUSINESS_MAILING_COUNTRY(26, "Provider Business Mailing Address Country Code (If outside U.S.)"),

	PHONE_BUSINEES_MAILING(27, "Provider Business Mailing Address Telephone Number"),

	NPI_DEACTIVATION_CODE(39, "NPI Deactivation Reason Code"),

	NPI_DEACTIVATION_DATE(40, "NPI Deactivation Date");

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
	private SampleProviderColumn(int columnIndex, String columnName) {
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
