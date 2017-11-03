package gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions;

/**
 * Indicates that a checksum failure has occurred after downloading the files
 * from Amazon Web Services S3...
 */
public final class ChecksumException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link ChecksumException}.
	 * 
	 * @param cause
	 *            the value/description to use for {@link #getCause()}
	 */
	public ChecksumException(String cause) {
		super(cause);
	}
}