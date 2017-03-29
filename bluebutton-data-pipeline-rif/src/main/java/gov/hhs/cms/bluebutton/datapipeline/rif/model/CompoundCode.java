package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.util.Optional;

/**
 * Enumerate the possibly values for Compound Code for Part D
 */
public enum CompoundCode {
	NOT_SPECIFIED(0),

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
	 * @param valueToParse
	 *            the {@link CompoundCode#getCode()} to find a match for
	 * @return the {@link CompoundCode} that matches the specified
	 *         {@link CompoundCode#getCode()}
	 */
	public static CompoundCode parseRifValue(Optional<Integer> valueToParse) {
		if (!valueToParse.isPresent())
			return CompoundCode.NOT_SPECIFIED;

		for (CompoundCode compoundCode : CompoundCode.values()) {
			if (compoundCode.getCode().equals(valueToParse.get())) {
				return compoundCode;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + valueToParse.get());
	}
}
