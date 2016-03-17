package gov.hhs.cms.bluebutton.datapipeline.sampledata;

/**
 * Models a sample/generated person name.
 */
final class SampleName {
	private final String firstName;
	private final String lastName;

	/**
	 * Constructs a new {@link SampleName} instance.
	 * 
	 * @param firstName
	 *            the value to use for {@link #getFirstName()}
	 * @param lastName
	 *            the value to use for {@link #getLastName()}
	 */
	public SampleName(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	/**
	 * @return the first/given name component of this name
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the last/family name component of this name
	 */
	public String getLastName() {
		return lastName;
	}
}
