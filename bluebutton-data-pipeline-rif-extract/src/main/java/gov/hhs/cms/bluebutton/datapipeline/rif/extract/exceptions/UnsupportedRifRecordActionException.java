package gov.hhs.cms.bluebutton.datapipeline.rif.extract.exceptions;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;

public final class UnsupportedRifRecordActionException extends RuntimeException {
	private static final long serialVersionUID = 6764860303725144657L;

	public UnsupportedRifRecordActionException() {
	}

	public UnsupportedRifRecordActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedRifRecordActionException(RecordAction recordAction) {
		super("Invalid RIF record action code: " + recordAction);
	}

	public UnsupportedRifRecordActionException(Throwable cause) {
		super(cause);
	}
}