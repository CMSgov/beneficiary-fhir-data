package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Enumerates the sample RIF resources available on the classpath.
 */
public enum StaticRifResource {
	BENE_1("rif-static-sample-1/beneficiary.txt", RifFileType.BENEFICIARY, 1000);

	private final String classpathName;
	private final RifFileType rifFileType;
	private final int recordCount;

	/**
	 * Enum constant constructor.
	 * 
	 * @param classpathName
	 *            the value to use for {@link #getClasspathName()}
	 * @param rifFileType
	 *            the value to use for
	 * @param recordCount
	 *            the value to use for
	 */
	private StaticRifResource(String classpathName, RifFileType rifFileType, int recordCount) {
		this.classpathName = classpathName;
		this.rifFileType = rifFileType;
		this.recordCount = recordCount;
	}

	/**
	 * @return the location of this RIF resource on the classpath, as might be
	 *         passed to {@link ClassLoader#getResource(String)}
	 */
	public String getClasspathName() {
		return classpathName;
	}

	/**
	 * @return the {@link RifFileType} of the RIF file
	 */
	public RifFileType getRifFileType() {
		return rifFileType;
	}

	/**
	 * @return the number of beneficiaries/claims/drug events in the RIF file
	 */
	public int getRecordCount() {
		return recordCount;
	}
}
