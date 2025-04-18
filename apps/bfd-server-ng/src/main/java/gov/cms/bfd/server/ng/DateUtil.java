package gov.cms.bfd.server.ng;

import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
  public static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(TIME_ZONE_UTC.toZoneId()).toInstant());
  }
}
