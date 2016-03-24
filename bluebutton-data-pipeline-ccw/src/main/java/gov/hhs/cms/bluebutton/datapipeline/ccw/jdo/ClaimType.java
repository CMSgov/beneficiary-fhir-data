package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

/**
 * Enumerates the various CCW claim types.
 */
public enum ClaimType {
	OUTPATIENT_CLAIM("40", "Outpatient claim"),

	INPATIENT_CLAIM("60", "Inpatient claim"),

	CARRIER_NON_DME_CLAIM("71", "RIC O local carrier DMEPOS claim");

	private final String code;
	private final String description;

	/**
	 * Enum constant constructor.
	 * 
	 * @param code
	 *            the value to use for {@link #getValue()}
	 * @param description
	 *            the value to use for {@link #getDescription()}
	 */
	private ClaimType(String code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * @return the two-digit code that is used to represent this
	 *         {@link ClaimType} in the CCW
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return a bit of descriptive text that explains what this
	 *         {@link ClaimType} is
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param code
	 *            the {@link ClaimType#getCode()} to find a match for
	 * @return the {@link ClaimType} that matches the specified
	 *         {@link ClaimType#getCode()}
	 */
	public static ClaimType getClaimTypeByCode(String code) {
		for (ClaimType claimType : ClaimType.values())
			if (claimType.getCode().equals(code))
				return claimType;
		throw new IllegalArgumentException("Unknown code: " + code);
	}
}
