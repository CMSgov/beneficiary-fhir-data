package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

/**
 * Enumerates the DE-SynPUF archive resources used in this project.
 */
public enum SynpufArchive {
	SAMPLE_1("de-synpuf/sample-1.tar.bz2", "1", 116352), 
	
	SAMPLE_TEST_A("de-synpuf/sample-test-a.tar.bz2", "TestA", 10), 
	
	SAMPLE_TEST_B("de-synpuf/sample-test-b.tar.bz2", "TestB", 500), 
	
	SAMPLE_TEST_C("de-synpuf/sample-test-c.tar.bz2", "TestC", 200), 
	
	SAMPLE_TEST_D("de-synpuf/sample-test-d.tar.bz2", "TestD", 10000);

	private final String resourceName;
	private final String id;
	private final int beneficiaryCount;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resourceName
	 *            the value to use for {@link #getResourceName()}
	 * @param id
	 *            the value to use for {@link #getId()}
	 * @param beneficiaryCount
	 *            the value to use for {@link #getBeneficiaryCount()}
	 */
	private SynpufArchive(String resourceName, String id, int beneficiaryCount) {
		this.resourceName = resourceName;
		this.id = id;
		this.beneficiaryCount = beneficiaryCount;
	}

	/**
	 * @return the name of the resource, as expected by
	 *         {@link ClassLoader#getResource(String)}
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * @return the ID of this {@link SynpufArchive}
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the number of unique beneficiaries stored in this
	 *         {@link SynpufArchive}
	 */
	public long getBeneficiaryCount() {
		return beneficiaryCount;
	}
}
