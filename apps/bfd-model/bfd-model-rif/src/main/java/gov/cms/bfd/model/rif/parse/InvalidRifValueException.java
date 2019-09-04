package gov.cms.bfd.model.rif.parse;

public final class InvalidRifValueException extends RuntimeException {
	private static final long serialVersionUID = 6764860303725144657L;

	public InvalidRifValueException() {
	}

	public InvalidRifValueException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidRifValueException(String message) {
		super(message);
	}

	public InvalidRifValueException(Throwable cause) {
		super(cause);
	}
}
