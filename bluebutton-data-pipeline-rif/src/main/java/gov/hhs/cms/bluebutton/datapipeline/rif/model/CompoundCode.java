package gov.hhs.cms.bluebutton.datapipeline.rif.model;

/**
 * Enumerate the possibly values for Compound Code for Part D
 */
public enum CompoundCode {
	NOT_COMPOUNDED(1),

	COMPOUNDED(2);

	private final Integer code;

	/**
	 * Enum constant constructor.
	 * 
	 * @param code
	 *            the value to use for {@link #getValue()}
	 */
	private CompoundCode(Integer code) {
		this.code = code;
	}

	/**
	 * @return the 1-digit code used to represent the compound status of a drug
	 */
	public Integer getCode() {
		return code;
	}

	/**
	 * @param rifValue
	 *            the {@link CompoundCode#getCode()} to find a match for
	 * @return the {@link CompoundCode} that matches the specified
	 *         {@link CompoundCode#getCode()}
	 */
	public static CompoundCode parseRifValue(Integer rifValue) {
		for (CompoundCode compoundCode : CompoundCode.values()) {
			if (compoundCode.getCode().equals(rifValue)) {
				return compoundCode;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + rifValue);
	}
}
