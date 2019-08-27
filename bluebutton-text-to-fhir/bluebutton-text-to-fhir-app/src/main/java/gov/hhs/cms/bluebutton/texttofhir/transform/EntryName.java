package gov.hhs.cms.bluebutton.texttofhir.transform;

import gov.hhs.cms.bluebutton.texttofhir.parsing.Entry;

/**
 * Enumerates the known {@link Entry#getName()} values.
 */
public enum EntryName {
	DEMOGRAPHICS_SOURCE("Source"), 
	
	DEMOGRAPHICS_NAME("Name"),
	
	DEMOGRAPHICS_DOB("Date of Birth"), 
	
	DEMOGRAPHICS_ADDRESS_LINE_1("Address Line 1"), 
	
	DEMOGRAPHICS_ADDRESS_LINE_2("Address Line 2"), 
	
	DEMOGRAPHICS_CITY("City"), 
	
	DEMOGRAPHICS_STATE("State"), 
	
	DEMOGRAPHICS_ZIP("Zip"), 
	
	DEMOGRAPHICS_PHONE("Phone Number"), 
	
	DEMOGRAPHICS_EMAIL("Email"), 
	
	DEMOGRAPHICS_PART_A_DATE("Part A Effective Date"), 
	
	DEMOGRAPHICS_PART_B_DATE("Part B Effective Date"), 
	
	PLANS_SOURCE("Source"),
	
	PLANS_ID("Contract ID/Plan ID"),
	
	PLANS_PERIOD("Plan Period"),
	
	PLANS_NAME("Plan Name"),
	
	PLANS_MARKETING("Marketing Name"),
	
	PLANS_ADDRESS("Plan Address"),
	
	PLANS_TYPE("Plan Type"), 
	
	CLAIM_SUMMARY_SOURCE("Source"), 
	
	CLAIM_SUMMARY_NUMBER("Claim Number"), 
	
	CLAIM_SUMMARY_PROVIDER("Provider"), 
	
	CLAIM_SUMMARY_PROVIDER_ADDRESS("Provider Billing Address"), 
	
	CLAIM_SUMMARY_START("Service Start Date"), 
	
	CLAIM_SUMMARY_END("Service End Date"), 
	
	CLAIM_SUMMARY_CHARGED("Amount Charged"), 
	
	CLAIM_SUMMARY_PROVIDER_PAID("Provider Paid"), 
	
	CLAIM_SUMMARY_BENEFICIARY_BILLED("You May be Billed"), 
	
	CLAIM_SUMMARY_TYPE("Claim Type"), 
	
	CLAIM_SUMMARY_DIAGNOSIS_CODE_1("Diagnosis Code 1"), 
	
	CLAIM_LINES_NUMBER("Line number"), 
	
	CLAIM_LINES_DATE_FROM("Date of Service From"), 
	
	CLAIM_LINES_DATE_TO("Date of Service To"), 
	
	CLAIM_LINES_PROCEDURE("Procedure Code/Description"), 
	
	CLAIM_LINES_MODIFIER_1("Modifier 1/Description"), 
	
	CLAIM_LINES_MODIFIER_2("Modifier 2/Description"), 
	
	CLAIM_LINES_MODIFIER_3("Modifier 3/Description"), 
	
	CLAIM_LINES_MODIFIER_4("Modifier 4/Description"), 
	
	CLAIM_LINES_QUANTITY("Quantity Billed/Units"), 
	
	CLAIM_LINES_SUBMITTED("Submitted Amount/Charges"), 
	
	CLAIM_LINES_ALLOWED("Allowed Amount"), 
	
	CLAIM_LINES_UNCOVERED("Non-Covered"), 
	
	CLAIM_LINES_PLACE("Place of Service/Description"), 
	
	CLAIM_LINES_TYPE("Type of Service/Description"), 
	
	CLAIM_LINES_RENDERER_NUMBER("Rendering Provider No"), 
	
	CLAIM_LINES_RENDERER_NPI("Rendering Provider NPI"); 

	private final String name;

	/**
	 * Enum constant construtor.
	 * 
	 * @param name
	 *            the value to use for {@link #getName()}
	 */
	private EntryName(String name) {
		this.name = name;
	}

	/**
	 * @return the {@link Entry#getName()} value represented by this
	 *         {@link EntryName} constant
	 */
	public String getName() {
		return name;
	}
}
