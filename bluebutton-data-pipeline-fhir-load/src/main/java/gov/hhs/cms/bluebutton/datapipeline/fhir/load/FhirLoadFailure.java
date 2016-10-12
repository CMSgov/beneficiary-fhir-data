package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * This unchecked {@link RuntimeException} is used to represent that a specific
 * {@link TransformedBundle} failed to load, when pushed to a FHIR server via
 * {@link FhirLoader}.
 */
public final class FhirLoadFailure extends RuntimeException {
	private static final long serialVersionUID = 2180257204126931820L;

	private final TransformedBundle failedBundle;

	/**
	 * Constructs a new {@link FhirLoadFailure} instance.
	 * 
	 * @param failedBundle
	 *            the value to use for {@link #getFailedBundle()}
	 * @param cause
	 *            the {@link Throwable} that was encountered, when the
	 *            {@link TransformedBundle} failed to load
	 */
	public FhirLoadFailure(TransformedBundle failedBundle, Throwable cause) {
		super(buildMessage(failedBundle), cause);
		this.failedBundle = failedBundle;
	}

	/**
	 * @param failedBundle
	 *            the {@link TransformedBundle} that failed to load
	 * @return the value to use for {@link #getMessage()}
	 */
	private static String buildMessage(TransformedBundle failedBundle) {
		return String.format("Failed to load a bundle containing '%d' resources, built from a '%s' record.",
				failedBundle.getResult().getEntry().size(),
				failedBundle.getSource().getRecord().getClass().getSimpleName());
	}

	/**
	 * @return the {@link TransformedBundle} that failed to load
	 */
	public TransformedBundle getFailedBundle() {
		return failedBundle;
	}
}
