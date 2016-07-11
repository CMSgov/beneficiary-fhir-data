package gov.hhs.cms.bluebutton.datapipeline.rif.model;

/**
 * Enumerate the possibly values for Drug Coverage Status for Part D
 */
public enum DrugCoverageStatus {
	/* Covered by Part D */
	COVERED("C"),

	/* Covered by enhanced coverage for supplemental drugs */
	SUPPLEMENTAL("E"),

	/* Covered by enhanced coverage for over-the-counter Drugs */
	OVER_THE_COUNTER("O");

	private final String code;

	/**
	 * Enum constant constructor.
	 * 
	 * @param code
	 *            the value to use for {@link #getValue()}
	 */
	private DrugCoverageStatus(String code) {
		this.code = code;
	}

	/**
	 * @return the 1-digit code used to represent the compound status of a drug
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param rifValue
	 *            the {@link DrugCoverageStatus#getCode()} to find a match for
	 * @return the {@link DrugCoverageStatus} that matches the specified
	 *         {@link DrugCoverageStatus#getCode()}
	 */
	public static DrugCoverageStatus parseRifValue(String rifValue) {
		if (rifValue.length() != 1) {
			throw new IllegalArgumentException("Code should only have one character: " + rifValue);
		}
		for (DrugCoverageStatus drugCoveredStatus : DrugCoverageStatus.values()) {
			if (drugCoveredStatus.getCode().equals(rifValue)) {
				return drugCoveredStatus;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + rifValue);
	}
}
