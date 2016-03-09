package gov.hhs.cms.bluebutton.datapipeline.sampledata;

/**
 * This unchecked exception indicates that an unrecoverable problem has occurred
 * in {@link SampleDataLoader}.
 */
public final class SampleDataException extends RuntimeException {
	private static final long serialVersionUID = 7812088488193767670L;

	/**
	 * Constructs a new {@link SampleDataException} instance.
	 * 
	 * @param cause
	 *            the value to use for {@link #getCause()}
	 */
	public SampleDataException(Exception cause) {
		super(cause);
	}
}
