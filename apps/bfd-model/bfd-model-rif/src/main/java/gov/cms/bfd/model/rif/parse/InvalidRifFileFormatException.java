package gov.cms.bfd.model.rif.parse;

public final class InvalidRifFileFormatException extends RuntimeException {
  private static final long serialVersionUID = 6764860303725144657L;

  public InvalidRifFileFormatException() {}

  public InvalidRifFileFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidRifFileFormatException(String message) {
    super(message);
  }

  public InvalidRifFileFormatException(Throwable cause) {
    super(cause);
  }
}
