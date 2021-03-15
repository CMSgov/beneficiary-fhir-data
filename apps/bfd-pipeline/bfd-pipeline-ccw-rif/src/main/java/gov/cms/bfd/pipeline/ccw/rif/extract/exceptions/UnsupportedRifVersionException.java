package gov.cms.bfd.pipeline.ccw.rif.extract.exceptions;

public final class UnsupportedRifVersionException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  public UnsupportedRifVersionException() {}

  public UnsupportedRifVersionException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedRifVersionException(int version) {
    super("Unsupported record version: " + version);
  }

  public UnsupportedRifVersionException(Throwable cause) {
    super(cause);
  }
}
