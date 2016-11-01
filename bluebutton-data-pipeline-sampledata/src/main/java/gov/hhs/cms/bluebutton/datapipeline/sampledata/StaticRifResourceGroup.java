package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;

/**
 * Enumerates groups of related {@link StaticRifResource}s that can be processed
 * together.
 */
public enum StaticRifResourceGroup {
	SAMPLE_A(StaticRifResource.SAMPLE_A_BENES, StaticRifResource.SAMPLE_A_CARRIER, StaticRifResource.SAMPLE_A_PDE),

	SAMPLE_B(StaticRifResource.SAMPLE_B_BENES, StaticRifResource.SAMPLE_B_CARRIER, StaticRifResource.SAMPLE_B_PDE),

	SAMPLE_C(StaticRifResource.SAMPLE_C_BENES, StaticRifResource.SAMPLE_C_CARRIER, StaticRifResource.SAMPLE_C_PDE);

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

	/**
	 * @return a {@link Set} of {@link RifFile}s based on
	 *         {@link #getResources()}
	 */
	public Set<RifFile> toRifFiles() {
		return Arrays.stream(resources).map(resource -> resource.toRifFile()).collect(Collectors.toSet());
	}
}
