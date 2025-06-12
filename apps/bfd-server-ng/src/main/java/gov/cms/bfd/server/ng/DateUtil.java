package gov.cms.bfd.server.ng;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;

/** Date utility methods. */
public class DateUtil {

  /**
   * Converts the {@link LocalDate} to a {@link Date} set to midnight UTC.
   *
   * @param localDate local date instance
   * @return date instance
   */
  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(IdrConstants.ZONE_ID_UTC).toInstant());
  }

  /**
   * Converts the {@link ZonedDateTime} to a {@link Date} object with the same date and time info.
   *
   * @param zonedDateTime local datetime instance
   * @return date instance
   */
  public static Date toDate(ZonedDateTime zonedDateTime) {
    return Date.from(zonedDateTime.toInstant());
  }

  /**
   * Converts the {@link Date} instance to a {@link ZonedDateTime} instance with the same date and
   * time info.
   *
   * @param date date
   * @return local datetime
   */
  public static ZonedDateTime toZonedDateTime(Date date) {
    return date.toInstant().atZone(IdrConstants.ZONE_ID_UTC);
  }
}
