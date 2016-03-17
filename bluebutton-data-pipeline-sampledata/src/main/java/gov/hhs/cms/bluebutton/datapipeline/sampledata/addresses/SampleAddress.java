package gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses;

/**
 * Models a sample/generated address.
 */
public final class SampleAddress {
	private final String addressExceptZip;
	private final String zip;

	/**
	 * Constructs a new {@link SampleAddress} instance.
	 * 
	 * @param addressExceptZip
	 *            the value to use for {@link #getAddressExceptZip()}
	 * @param zip
	 *            the value to use for {@link #getZip()}
	 */
	public SampleAddress(String addressExceptZip, String zip) {
		this.addressExceptZip = addressExceptZip;
		this.zip = zip;
	}

	/**
	 * @return the address as a single-line string that includes all of the
	 *         address components except for the zip code
	 */
	public String getAddressExceptZip() {
		return addressExceptZip;
	}

	/**
	 * @return the address' zip code
	 */
	public String getZip() {
		return zip;
	}
}
