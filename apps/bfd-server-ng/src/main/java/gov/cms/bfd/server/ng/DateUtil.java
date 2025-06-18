package gov.cms.bfd.server.ng;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

/** Date utility methods. */
public class DateUtil {
  /** UTC time zone. UTC should be used for all datetime conversions and representations. */
  public static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

  private DateUtil() {}

  /**
   * Converts the {@link LocalDate} to a {@link Date} set to midnight UTC.
   *
   * @param localDate local date instance
   * @return date instance
   */
  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(TIME_ZONE_UTC.toZoneId()).toInstant());
  }

  /**
   * Converts the {@link LocalDateTime} to a {@link Date} object with the same date and time info.
   *
   * @param localDateTime local datetime instance
   * @return date instance
   */
  public static Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
  }

  /**
   * Converts the {@link Date} instance to a {@link LocalDateTime} instance with the same date and
   * time info.
   *
   * @param date date
   * @return local datetime
   */
  public static LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(TIME_ZONE_UTC.toZoneId()).toLocalDateTime();
  }
}
