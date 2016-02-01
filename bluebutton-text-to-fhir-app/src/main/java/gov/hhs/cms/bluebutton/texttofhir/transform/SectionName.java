package gov.hhs.cms.bluebutton.texttofhir.transform;

import gov.hhs.cms.bluebutton.texttofhir.parsing.Section;

/**
 * Enumerates the known {@link Section#getName()} values.
 */
public enum SectionName {
	DEMOGRAPHIC("Demographic"), 
	
	PLANS("Plans"), 
	
	CLAIM_SUMMARY("Claim Summary"), 
	
	CLAIM_LINES_PREFIX("Claim Lines for Claim Number: ");

	private final String name;

	/**
	 * Enum constant construtor.
	 * 
	 * @param name
	 *            the value to use for {@link #getName()}
	 */
	private SectionName(String name) {
		this.name = name;
	}

	/**
	 * @return the {@link Section#getName()} value represented by this
	 *         {@link SectionName} constant
	 */
	public String getName() {
		return name;
	}
}
