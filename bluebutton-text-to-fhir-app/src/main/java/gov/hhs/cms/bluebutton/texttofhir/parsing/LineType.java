package gov.hhs.cms.bluebutton.texttofhir.parsing;

/**
 * Enumerates the various types of lines that are known to appear in the
 * BlueButton text file format.
 */
public enum LineType {
	/**
	 * A "<code>--------------------------------</code>" section boundary
	 * marker, used before and after a {@link #SECTION_TITLE}.
	 */
	SECTION_BOUNDARY,

	/**
	 * Appears at the start of a new section (between {@link #SECTION_BOUNDARY}
	 * lines) and names the section.
	 */
	SECTION_TITLE,

	/**
	 * Contains a colon-separated field name and value.
	 */
	FIELD_AND_VALUE,

	/**
	 * A whitespace-only line.
	 */
	BLANK,

	/**
	 * Some unknown type of line.
	 */
	UNKNOWN;
}
