package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadableFhirBundle;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * This unchecked {@link RuntimeException} is used to represent that a specific
 * {@link TransformedBundle} failed to load, when pushed to a FHIR server via
 * {@link FhirLoader}.
 */
public final class FhirLoadFailure extends RuntimeException {
	private static final long serialVersionUID = 2180257204126931820L;

	private final LoadableFhirBundle failedBundle;

	/**
	 * Constructs a new {@link FhirLoadFailure} instance.
	 * 
	 * @param inputBundle
	 *            the value to use for {@link #getFailedBundle()}
	 * @param cause
	 *            the {@link Throwable} that was encountered, when the
	 *            {@link TransformedBundle} failed to load
	 */
	public FhirLoadFailure(LoadableFhirBundle inputBundle, Throwable cause) {
		super(buildMessage(inputBundle), cause);
		this.failedBundle = inputBundle;
	}

	/**
	 * @param inputBundle
	 *            the {@link TransformedBundle} that failed to load
	 * @return the value to use for {@link #getMessage()}
	 */
	private static String buildMessage(LoadableFhirBundle inputBundle) {
		return String.format("Failed to load a bundle containing '%d' resources, built from a '%s' record.",
				inputBundle.getResult().getEntry().size(), inputBundle.getSourceType());
	}

	/**
	 * @return the {@link LoadableFhirBundle} that failed to load
	 */
	public LoadableFhirBundle getFailedBundle() {
		return failedBundle;
	}
}
