package gov.hhs.cms.bluebutton.data.model.rif.samples;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import gov.hhs.cms.bluebutton.data.model.rif.RifFile;

/**
 * Enumerates groups of related {@link StaticRifResource}s that can be processed
 * together.
 */
public enum StaticRifResourceGroup {
	SAMPLE_A(StaticRifResource.SAMPLE_A_BENES, StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
			StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY,
			StaticRifResource.SAMPLE_A_CARRIER, StaticRifResource.SAMPLE_A_PDE, StaticRifResource.SAMPLE_A_INPATIENT,
			StaticRifResource.SAMPLE_A_OUTPATIENT, StaticRifResource.SAMPLE_A_HHA, StaticRifResource.SAMPLE_A_HOSPICE,
			StaticRifResource.SAMPLE_A_SNF, StaticRifResource.SAMPLE_A_DME),

	SAMPLE_B(StaticRifResource.SAMPLE_B_BENES, StaticRifResource.SAMPLE_B_CARRIER, StaticRifResource.SAMPLE_B_INPATIENT,
			StaticRifResource.SAMPLE_B_OUTPATIENT, StaticRifResource.SAMPLE_B_SNF, StaticRifResource.SAMPLE_B_HOSPICE,
			StaticRifResource.SAMPLE_B_HHA, StaticRifResource.SAMPLE_B_DME, StaticRifResource.SAMPLE_B_PDE),

	SAMPLE_C(StaticRifResource.SAMPLE_C_BENES, StaticRifResource.SAMPLE_C_CARRIER, StaticRifResource.SAMPLE_C_INPATIENT,
			StaticRifResource.SAMPLE_C_OUTPATIENT, StaticRifResource.SAMPLE_C_SNF, StaticRifResource.SAMPLE_C_HOSPICE,
			StaticRifResource.SAMPLE_C_HHA, StaticRifResource.SAMPLE_C_DME, StaticRifResource.SAMPLE_C_PDE),

	SAMPLE_U(StaticRifResource.SAMPLE_U_BENES, StaticRifResource.SAMPLE_U_CARRIER),

	SYNTHETIC_DATA(StaticRifResource.SYNTHETIC_BENEFICIARY_1999, StaticRifResource.SYNTHETIC_BENEFICIARY_2000,
			StaticRifResource.SYNTHETIC_BENEFICIARY_2014, StaticRifResource.SYNTHETIC_CARRIER_1999_1999,
			StaticRifResource.SYNTHETIC_CARRIER_1999_2000, StaticRifResource.SYNTHETIC_CARRIER_1999_2001,
			StaticRifResource.SYNTHETIC_CARRIER_2000_2000, StaticRifResource.SYNTHETIC_CARRIER_2000_2001,
			StaticRifResource.SYNTHETIC_CARRIER_2000_2002, StaticRifResource.SYNTHETIC_CARRIER_2014_2014,
			StaticRifResource.SYNTHETIC_CARRIER_2014_2015, StaticRifResource.SYNTHETIC_CARRIER_2014_2016,
			StaticRifResource.SYNTHETIC_INPATIENT_1999_1999, StaticRifResource.SYNTHETIC_INPATIENT_1999_2000,
			StaticRifResource.SYNTHETIC_INPATIENT_1999_2001, StaticRifResource.SYNTHETIC_INPATIENT_2000_2000,
			StaticRifResource.SYNTHETIC_INPATIENT_2000_2001, StaticRifResource.SYNTHETIC_INPATIENT_2000_2002,
			StaticRifResource.SYNTHETIC_INPATIENT_2014_2014, StaticRifResource.SYNTHETIC_INPATIENT_2014_2015,
			StaticRifResource.SYNTHETIC_INPATIENT_2014_2016, StaticRifResource.SYNTHETIC_PDE_2014,
			StaticRifResource.SYNTHETIC_PDE_2015, StaticRifResource.SYNTHETIC_PDE_2016);

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
