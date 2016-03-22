package gov.hhs.cms.bluebutton.datapipeline.sampledata.pharmacies;

/**
 * Models a sample/generated address.
 */
public final class SamplePharmacy {
	private final int npi;

	/**
	 * Constructs a new {@link SamplePharmacy} instance.
	 * 
	 * @param npi
	 *            the value to use for {@link #getNpi()}
	 */
	public SamplePharmacy(int npi) {
		this.npi = npi;
	}

	/**
	 * @return the NPI code for this {@link SamplePharmacy}
	 */
	public int getNpi() {
		return npi;
	}
}
