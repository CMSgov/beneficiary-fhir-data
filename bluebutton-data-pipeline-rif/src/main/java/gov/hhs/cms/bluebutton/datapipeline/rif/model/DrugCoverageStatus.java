package gov.hhs.cms.bluebutton.datapipeline.rif.model;

/**
 * Enumerate the possibly values for Drug Coverage Status for Part D
 */
public enum DrugCoverageStatus {
	COVERED('C', "Covered by Part D"), SUPPLEMENTAL('E',
			"Covered by enhanced coverage for supplemental drugs"), OVER_THE_COUNTER('O',
					"Covered by enhanced coverage for over-the-counter Drugs");

	private final Character code;
	private final String description;

	/**
	 * Enum constant constructor.
	 * 
	 * @param code
	 *            the value to use for {@link #getValue()}
	 * @param description
	 *            the value to use for {@link #getDescription()}
	 */
	private DrugCoverageStatus(Character code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * @return the 1-digit code used to represent the compound status of a drug
	 */
	public Character getCode() {
		return code;
	}

	/**
	 * @return a friendly description for the specified code
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param rifValue
	 *            the {@link DrugCoverageStatus#getCode()} to find a match for
	 * @return the {@link DrugCoverageStatus} that matches the specified
	 *         {@link DrugCoverageStatus#getCode()}
	 */
	public static DrugCoverageStatus parseRifValue(Character rifValue) {
		for (DrugCoverageStatus drugCoveredStatus : DrugCoverageStatus.values()) {
			if (drugCoveredStatus.getCode().equals(rifValue)) {
				return drugCoveredStatus;
			}
		}
		throw new IllegalArgumentException("Unknown code: " + rifValue);
	}
}
