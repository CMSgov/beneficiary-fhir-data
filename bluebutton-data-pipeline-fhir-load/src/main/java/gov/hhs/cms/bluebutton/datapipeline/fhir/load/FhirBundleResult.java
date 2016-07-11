package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import org.hl7.fhir.dstu21.model.Bundle;

import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * Helps model the results of
 * {@link FhirLoader#process(java.util.stream.Stream)} operations. Each
 * {@link FhirBundleResult} instance represents the results of a single
 * {@link TransformedBundle} entry's processing.
 */
public final class FhirBundleResult {
	private final TransformedBundle inputBundle;
	private final Bundle outputBundle;

	/**
	 * Constructs a new {@link FhirBundleResult} instance.
	 * 
	 * @param inputBundle
	 *            the value to use for {@link #getInputBundle()}
	 * @param outputBundle
	 *            the value to use for {@link #getOutputBundle()}
	 */
	public FhirBundleResult(TransformedBundle inputBundle, Bundle outputBundle) {
		this.inputBundle = inputBundle;
		this.outputBundle = outputBundle;
	}

	/**
	 * @return the input {@link TransformedBundle} that was processed
	 */
	public TransformedBundle getInputBundle() {
		return inputBundle;
	}

	/**
	 * @return the output FHIR {@link Bundle} that was returned from the
	 *         processing
	 */
	public Bundle getOutputBundle() {
		return outputBundle;
	}
}
