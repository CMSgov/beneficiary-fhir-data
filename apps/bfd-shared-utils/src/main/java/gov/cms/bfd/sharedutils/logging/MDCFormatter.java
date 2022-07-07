package gov.cms.bfd.sharedutils.logging;

/** Formatter for logging to the MDC class. */
public class MDCFormatter {

  /** Delimiter to be used to separate the parts of MDC field names. */
  public static final String TO_FIELD_DELIMITER = "_";

  /** Delimiter to be used to separate the parts of MDC field names. */
  public static final String FROM_FIELD_DELIMITER = ".";

  // /**
  //  * Format an identifier for an MDC field.
  //  *
  //  * @param fields Fields to concatenate into a field identifier
  //  * @param appends Fields to concatenate into a field identifier
  //  * @return Text of the field identifier
  //  */
  // public static String formatMdcFieldArrays(String[] fields, String[] appends) {
  //   String[] result = new String[fields.length + appends.length];
  //   System.arraycopy(fields, 0, result, 0, fields.length);
  //   System.arraycopy(appends, 0, result, fields.length, appends.length);

  //   return formatMdcField(result);
  // }

  /**
   * Format an identifier for an MDC field.
   *
   * @param key Fields to concatenate into a field identifier
   * @return Text of the field identifier
   */
  public static String formatMdcField(String key) {
    return key.replace(FROM_FIELD_DELIMITER, TO_FIELD_DELIMITER);
  }
}
