package gov.hhs.cms.bluebutton.datapipeline.sampledata.npi;

/**
 * Models a sample/generated address.
 */
public final class SampleProvider {
	private final int npi;

	/**
	 * Constructs a new {@link SampleProvider} instance.
	 * 
	 * @param npi
	 *            the value to use for {@link #getNpi()}
	 */
	public SampleProvider(int npi) {
		this.npi = npi;
	}

	/**
	 * @return the NPI code for this {@link SampleProvider}
	 */
	public int getNpi() {
		return npi;
	}
}
