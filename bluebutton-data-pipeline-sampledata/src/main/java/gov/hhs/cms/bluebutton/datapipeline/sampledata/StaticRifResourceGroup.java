package gov.hhs.cms.bluebutton.datapipeline.sampledata;

/**
 * Enumerates groups of related {@link StaticRifResource}s that can be processed
 * together.
 */
public enum StaticRifResourceGroup {
	SAMPLE_A(StaticRifResource.SAMPLE_A_BENES, StaticRifResource.SAMPLE_A_CARRIER, StaticRifResource.SAMPLE_A_PDE),

	SAMPLE_B(StaticRifResource.SAMPLE_B_BENES, StaticRifResource.SAMPLE_B_CARRIER, StaticRifResource.SAMPLE_B_PDE);

	private final StaticRifResource[] resources;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resources
	 *            the value to use for {@link #getResources()}
	 */
	private StaticRifResourceGroup(StaticRifResource... resources) {
		this.resources = resources;
	}

	/**
	 * @return the related {@link StaticRifResource}s grouped into this
	 *         {@link StaticRifResourceGroup}
	 */
	public StaticRifResource[] getResources() {
		return resources;
	}
}
