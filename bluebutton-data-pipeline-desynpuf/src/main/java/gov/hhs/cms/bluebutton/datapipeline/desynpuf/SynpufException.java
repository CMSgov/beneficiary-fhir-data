package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

/**
 * This unchecked exception indicates that an unrecoverable problem has occurred
 * extracting a {@link SynpufSample}.
 */
public final class SynpufException extends RuntimeException {
	private static final long serialVersionUID = -5347508989179822814L;

	/**
	 * Constructs a new {@link SynpufException}.
	 * 
	 * @param message
	 *            the value to use for {@link #getMessage()}
	 */
	public SynpufException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link SynpufException} instance.
	 * 
	 * @param cause
	 *            the value to use for {@link #getCause()}
	 */
	public SynpufException(Exception cause) {
		super(cause);
	}
}
