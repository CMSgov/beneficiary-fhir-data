package gov.hhs.cms.bluebutton.datapipeline.sampledata.prescribers;

/**
 * Models a sample/generated prescriber.
 */
public final class SamplePresciber {
	private final int npi;

	/**
	 * Constructs a new {@link SamplePresciber} instance.
	 * 
	 * @param npi
	 *            the value to use for {@link #getNpi()}
	 */
	public SamplePresciber(int npi) {
		this.npi = npi;
	}

	/**
	 * @return the NPI code for this {@link SamplePresciber}
	 */
	public int getNpi() {
		return npi;
	}
}
