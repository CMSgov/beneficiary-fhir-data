package gov.cms.bfd.server.ng.util;

import java.time.LocalDate;

/** Mapping Sanitizer. */
public class MappingSanitizer {

  private static final LocalDate MIN_VALID_DATE = LocalDate.of(1000, 1, 1);
  private static final LocalDate MAX_VALID_DATE = LocalDate.of(9999, 12, 31);

  /**
   * Sanitizes Dates. Returns null if date is a placeholder sentinel.
   *
   * @param date date
   * @return sanitized Date
   */
  public static LocalDate sanitizeDate(LocalDate date) {
    if (date == null) return null;

    if (date.isBefore(MIN_VALID_DATE) || date.isAfter(MAX_VALID_DATE)) {
      return null;
    }
    if (date.equals(MIN_VALID_DATE) || date.equals(MAX_VALID_DATE)) {
      return null;
    }

    return date;
  }
}
