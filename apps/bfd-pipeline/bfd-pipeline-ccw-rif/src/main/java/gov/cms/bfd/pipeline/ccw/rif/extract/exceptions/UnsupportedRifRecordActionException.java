package gov.cms.bfd.pipeline.ccw.rif.extract.exceptions;

import gov.cms.bfd.model.rif.RecordAction;

public final class UnsupportedRifRecordActionException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  public UnsupportedRifRecordActionException() {}

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
