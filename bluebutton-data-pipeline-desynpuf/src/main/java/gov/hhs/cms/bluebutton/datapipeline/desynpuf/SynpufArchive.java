package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

/**
 * Enumerates the DE-SynPUF archive resources used in this project.
 */
public enum SynpufArchive {
	SAMPLE_1("de-synpuf/sample-1.tar.bz2", "1");

	private final String resourceName;
	private final String id;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resourceName
	 *            the value to use for {@link #getResourceName()}
	 * @param id
	 *            the value to use for {@link #getId()}
	 */
	private SynpufArchive(String resourceName, String id) {
		this.resourceName = resourceName;
		this.id = id;
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
}
