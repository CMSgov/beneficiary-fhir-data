package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import ca.uhn.fhir.rest.gclient.IClientExecutable;

/**
 * Models the results of a FHIR batch {@link IClientExecutable#execute()}
 * operatio, without keeping all of the objects that were loaded in memory.
 */
public final class FhirResult {
	private final int resourcesPushedCount;

	/**
	 * Constructs a new {@link FhirResult} instance.
	 * 
	 * @param resourcesPushedCount
	 *            the value to use for {@link #getResourcesPushedCount()}
	 */
	public FhirResult(int resourcesPushedCount) {
		this.resourcesPushedCount = resourcesPushedCount;
	}

	/**
	 * @return the number of FHIR resources that were pushed in the batch
	 *         represented by this {@link FhirResult}
	 */
	public int getResourcesPushedCount() {
		return resourcesPushedCount;
	}
}
