package gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions;

public final class UnsupportedRifVersionException extends RuntimeException {
	private static final long serialVersionUID = 6764860303725144657L;

	public UnsupportedRifVersionException() {
	}

	public UnsupportedRifVersionException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedRifVersionException(String message) {
		super(message);
	}

	public UnsupportedRifVersionException(Throwable cause) {
		super(cause);
	}
}