package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Enumerates the sample RIF resources available on the classpath.
 */
public enum StaticRifResource {
	BENES_1("rif-static-samples/beneficiaries-1.txt", RifFileType.BENEFICIARY, 1),

	BENES_1000("rif-static-samples/beneficiaries-1000.txt", RifFileType.BENEFICIARY, 1000),

	PDE_1("rif-static-samples/pde-1.txt", RifFileType.PDE, 1),

	PDE_1195("rif-static-samples/pde-1195.txt", RifFileType.PDE, 1195),

	CARRIER_1("rif-static-samples/sample-a-bcarrier-1.txt", RifFileType.CARRIER, 1),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ awk -F '|' '{print $4}' bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-b-bcarrier-1440.txt | sort | uniq -c | wc -l</code>
	 * .
	 */
	CARRIER_1091("rif-static-samples/sample-b-bcarrier-1091.txt", RifFileType.CARRIER, 1091);

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
