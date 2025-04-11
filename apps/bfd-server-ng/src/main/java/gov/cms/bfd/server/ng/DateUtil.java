package gov.cms.bfd.server.ng;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateUtil {
  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
  }
}
