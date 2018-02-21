package gov.hhs.cms.bluebutton.data.codebook.extractor;

import java.io.InputStream;

/**
 * Enumerates the codebooks that are expected to be converted.
 */
public enum SupportedCodebook {
	BENEFICIARY_SUMMARY("codebook-mbsf-abd.pdf", "Master Beneficiary Summary File - Base With Medicare Part A/B/D",
			"May 2017, Version 1.0"),

	FFS_CLAIMS("codebook-ffs-claims.pdf", "Medicare Fee-For-Service Claims (for Version K)",
			"December 2017, Version 1.4"),

	PARTD_EVENTS("codebook-pde.pdf", "Medicare Part D Event (PDE) / Drug Characteristics", "May 2017, Version 1.0");

	private final String codebookPdfResourceName;
	private final String displayName;
	private final String version;

	/**
	 * Enum constant constructor.
	 * 
	 * @param codebookPdfResourceName
	 *            the name of the input/unparsed codebook PDF resource on this
	 *            project's classpath
	 * @param displayName
	 *            the value to use for {@link #getDisplayName()}
	 * @param version
	 *            the value to use for {@link #getVersion()}
	 */
	private SupportedCodebook(String codebookPdfResourceName, String displayName, String version) {
		this.codebookPdfResourceName = codebookPdfResourceName;
		this.displayName = displayName;
		this.version = version;
	}

	/**
	 * @return the file/resource name of the {@link SupportedCodebook} XML resource
	 *         produced by {@link CodebookPdfToXmlApp}
	 */
	public String getCodebookXmlResourceName() {
		return codebookPdfResourceName.replace(".pdf", ".xml");
	}

	/**
	 * @return an {@link InputStream} for the {@link SupportedCodebook} PDF resource
	 *         to be converted
	 */
	public InputStream getCodebookPdfInputStream() {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(codebookPdfResourceName);
	}

	/**
	 * @return the descriptive English name for this {@link SupportedCodebook}
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return a human-readable {@link String} that identifies which version of the
	 *         data is represented by this {@link SupportedCodebook}
	 */
	public String getVersion() {
		return version;
	}
}
