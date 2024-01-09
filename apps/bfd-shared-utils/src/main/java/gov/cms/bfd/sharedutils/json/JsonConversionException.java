package gov.cms.bfd.sharedutils.json;

/** Exception thrown by {@link JsonConverter} if conversion fails. */
public class JsonConversionException extends RuntimeException {
  /**
   * Initializes an instance.
   *
   * @param cause real reason for conversion failure
   */
  public JsonConversionException(Throwable cause) {
    super(cause);
  }
}
