package gov.cms.bfd.sharedutils.logging;

import java.util.Iterator;

/** Formatter for logging to the MDC class. */
public class MDCFormatter {

  /** Delimiter to be used to separate the parts of MDC field names. */
  public static final String FIELD_DELIMITER = "-";

  /**
   * Format an identifier for an MDC field.
   *
   * @param fields Fields to concatenate into a field identifier
   * @return Text of the field identifier
   */
  public static String formatMDCField(String[] fields) {
    return String.join(FIELD_DELIMITER, fields);
  }

  /**
   * Format an identifier for an MDC field.
   *
   * @param fields Fields to concatenate into a field identifier
   * @return Text of the field identifier
   */
  public static String formatMCDField(Iterator<String> fields) {
    String fieldName = "";

    while (fields.hasNext()) {
      if (fieldName != "") {
        fieldName.concat(FIELD_DELIMITER);
      }
      fieldName.concat(fields.next());
    }

    return fieldName;
  }
}
