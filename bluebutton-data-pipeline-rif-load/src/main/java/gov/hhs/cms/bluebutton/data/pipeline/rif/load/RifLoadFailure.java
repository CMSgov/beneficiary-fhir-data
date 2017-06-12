package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;

/**
 * This unchecked {@link RuntimeException} is used to represent that a specific
 * {@link RifRecordEvent} failed to load, when pushed to a FHIR server via
 * {@link RifLoader}.
 */
public final class RifLoadFailure extends RuntimeException {
	private static final long serialVersionUID = 5268467019558996698L;

	private static final boolean LOG_SOURCE_DATA = false;

	private final RifRecordEvent<?> failedRecordEvent;

	/**
	 * Constructs a new {@link RifLoadFailure} instance.
	 * 
	 * @param failedRecordEvent
	 *            the value to use for {@link #getFailedRecordEvent()}
	 * @param cause
	 *            the {@link Throwable} that was encountered, when the
	 *            {@link RifRecordEvent} failed to load
	 */
	public RifLoadFailure(RifRecordEvent<?> failedRecordEvent, Throwable cause) {
		super(buildMessage(failedRecordEvent), cause);
		this.failedRecordEvent = failedRecordEvent;
	}

	/**
	 * @param inputBundle
	 *            the {@link TransformedBundle} that failed to load
	 * @return the value to use for {@link #getMessage()}
	 */
	private static String buildMessage(RifRecordEvent<?> failedRecordEvent) {
		if (LOG_SOURCE_DATA)
			return String.format("Failed to load a '%s' record: '%s'.",
					failedRecordEvent.getFile().getFileType().name(), failedRecordEvent.toString());
		else
			return String.format("Failed to load a '%s' record.", failedRecordEvent.getFile().getFileType().name());
	}

	/**
	 * @return the {@link RifRecordEvent} that failed to load
	 */
	public RifRecordEvent<?> getFailedRecordEvent() {
		return failedRecordEvent;
	}
}
