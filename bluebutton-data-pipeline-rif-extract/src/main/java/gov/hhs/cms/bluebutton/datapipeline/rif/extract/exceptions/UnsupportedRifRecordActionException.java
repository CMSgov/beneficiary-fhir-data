package gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions;

public final class UnsupportedRifRecordActionException extends RuntimeException {
	private static final long serialVersionUID = 6764860303725144657L;

	public UnsupportedRifRecordActionException() {
	}

	public UnsupportedRifRecordActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedRifRecordActionException(String message) {
		super(message);
	}

	public UnsupportedRifRecordActionException(Throwable cause) {
		super(cause);
	}
}