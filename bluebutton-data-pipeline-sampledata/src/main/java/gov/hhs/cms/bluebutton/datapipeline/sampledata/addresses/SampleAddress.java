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

		/*
		 * Workaround for CBBD-43: Some of the sample zips have a space between
		 * their zip-5 and zip-4.
		 */
		zip = stripWhiteSpace(zip);

		/*
		 * Workaround for CBBD-43: Some of the sample zips have a dash between
		 * their zip-5 and zip-4.
		 */
		zip = stripDashes(zip);

		/*
		 * Workaround for CBBD-43: Some of the sample zips have more than nine
		 * characters (look like international addresses).
		 */
		zip = zip.substring(0, Math.min(9, zip.length()));

		this.zip = zip;
	}

	/**
	 * @param text
	 *            the {@link String} to process
	 * @return the specified {@link String}, but with all whitespace removed
	 */
	private static String stripWhiteSpace(String text) {
		return text.replaceAll("\\s", "");
	}

	/**
	 * @param text
	 *            the {@link String} to process
	 * @return the specified {@link String}, but with all '<code>-</code>'
	 *         characters removed
	 */
	private static String stripDashes(String text) {
		return text.replaceAll("-", "");
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
