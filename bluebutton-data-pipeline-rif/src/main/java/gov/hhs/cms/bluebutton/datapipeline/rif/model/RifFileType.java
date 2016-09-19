package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.util.regex.Pattern;

/**
 * Enumerates the various types of RIF files.
 */
public enum RifFileType {
	BENEFICIARY(Pattern.compile(".*beneficiar(y|ies).*")),

	CARRIER(Pattern.compile(".*bcarrier.*")),

	// TODO
	DME(Pattern.compile("TODO")),

	// TODO
	HHA(Pattern.compile("TODO")),

	HOSPICE(Pattern.compile(".*hospice.*")),

	INPATIENT(Pattern.compile(".*inpatient.*")),

	OUTPATIENT(Pattern.compile(".*outpatient.*")),

	PDE(Pattern.compile(".*pde.*")),

	SNF(Pattern.compile(".*snf.*"));

	private final Pattern filenameRegex;

	/**
	 * Enum constant constructor.
	 * 
	 * @param filenameRegex
	 *            the {@link Pattern} that {@link #filenameMatches(String)}
	 *            should use
	 */
	private RifFileType(Pattern filenameRegex) {
		this.filenameRegex = filenameRegex;
	}

	/**
	 * @param filename
	 *            the filename (or S3 object key) to check
	 * @return <code>true</code> if the specified value matches the pattern
	 *         expected for this {@link RifFileType}, <code>false</code> if not
	 */
	private boolean filenameMatches(String filename) {
		return filenameRegex.matcher(filename).matches();
	}

	/**
	 * @param filename
	 *            the filename (or S3 object key) to find a matching
	 *            {@link RifFileType} for
	 * @return the {@link RifFileType} that the specified value matches the
	 *         expected pattern for
	 */
	public static RifFileType selectTypeForFilename(String filename) {
		for (RifFileType rifFileType : RifFileType.values())
			if (rifFileType.filenameMatches(filename))
				return rifFileType;

		throw new IllegalArgumentException("Unable to match filename: " + filename);
	}
}
