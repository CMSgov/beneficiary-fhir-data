package gov.cms.bfd.server.sharedutils;

/** Formatter for logging to the MDC class. */
public class MDCFormatter {

  /**
   * Delimiter that we want to be used to separate the parts of MDC field names; we need to replace
   * FROM_DELIMITER with this.
   */
  public static final String TO_DELIMITER = "_";

  /**
   * Delimiter that used to be used to separate the parts of MDC field names; we need to replace
   * this with TO_DELIMITER.
   */
  public static final String FROM_DELIMITER = ".";

  /**
   * Format an identifier for an MDC key. Historically, we have used "." to delimit parts of the MDC
   * keys, such as "http_access.request.header". For AWS CloudWatch Metrics, though, the "."
   * character is not supported, so we need to change these to "_" with minimal impact on the rest
   * of the project.
   *
   * @param key Fields to concatenate into a field identifier
   * @return Text of the field identifier
   */
  public static String formatMdcKey(String key) {
    return key.replace(FROM_DELIMITER, TO_DELIMITER);
  }
}
