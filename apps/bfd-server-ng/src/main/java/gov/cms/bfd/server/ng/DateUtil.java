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

  /**
   * Converts the {@link LocalDate} to a {@link Date} set to midnight UTC.
   *
   * @param localDate local date instance
   * @return date instance
   */
  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(TIME_ZONE_UTC.toZoneId()).toInstant());
  }

  public static Date toDate(LocalDateTime localDate) {
    return Date.from(localDate.toInstant(ZoneOffset.UTC));
  }

  public static LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atZone(TIME_ZONE_UTC.toZoneId()).toLocalDateTime();
  }
}
