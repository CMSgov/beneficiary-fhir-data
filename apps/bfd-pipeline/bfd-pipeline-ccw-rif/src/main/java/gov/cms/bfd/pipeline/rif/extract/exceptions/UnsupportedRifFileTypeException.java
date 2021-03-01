package gov.cms.bfd.pipeline.rif.extract.exceptions;

public final class UnsupportedRifFileTypeException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  public UnsupportedRifFileTypeException() {}

  public UnsupportedRifFileTypeException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedRifFileTypeException(String message) {
    super(message);
  }

  public UnsupportedRifFileTypeException(Throwable cause) {
    super(cause);
  }
}
