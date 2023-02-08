package gov.cms.bfd.pipeline.ccw.rif.extract.exceptions;

import gov.cms.bfd.model.rif.RecordAction;

/** Represents an exception thrown when the RIF action is not supported by the pipeline. */
public final class UnsupportedRifRecordActionException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  /** Instantiates a new unsupported rif record action exception. */
  public UnsupportedRifRecordActionException() {}

  /**
   * Instantiates a new unsupported rif record action exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public UnsupportedRifRecordActionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new Unsupported rif record action exception.
   *
   * @param recordAction the record action
   */
  public UnsupportedRifRecordActionException(RecordAction recordAction) {
    super("Invalid RIF record action code: " + recordAction);
  }

  /**
   * Instantiates a new Unsupported rif record action exception.
   *
   * @param cause the cause
   */
  public UnsupportedRifRecordActionException(Throwable cause) {
    super(cause);
  }
}
