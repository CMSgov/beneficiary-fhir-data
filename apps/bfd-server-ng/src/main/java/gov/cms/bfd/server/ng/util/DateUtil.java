package gov.cms.bfd.server.ng.util;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.hl7.fhir.r4.model.DateTimeType;

/** Date utility methods. */
public class DateUtil {

  /**
   * UTC {@link ZoneId}. UTC should be used for all datetime conversions to/from an instant
   * represented by {@link Date}.
   */
  public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

  /**
   * Minimum {@link ZonedDateTime} used as a safe min value.
   *
   * <p>LocalDateTime.MIN is outside the representable range of {@link java.util.Date} and will
   * cause an overflow when converting to milliseconds since the epoch. Using epoch-based minimum
   * (1970-01-01T00:00Z) which fits into a java.util.Date.
   */
  public static final ZonedDateTime MIN_DATETIME =
      ZonedDateTime.of(LocalDate.of(1970, 1, 1).atStartOfDay(), DateUtil.ZONE_ID_UTC);

  private DateUtil() {}

  /**
   * Converts the {@link LocalDate} to a {@link Date} set to midnight UTC.
   *
   * @param localDate local date instance
   * @return date instance
   */
  public static Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZONE_ID_UTC).toInstant());
  }

  /**
   * Converts a {@link LocalDate} to a FHIR {@link DateTimeType} with DAY precision. The underlying
   * instant is midnight UTC of the given LocalDate.
   *
   * @param localDate The LocalDate to convert.
   * @return A FHIR DateTimeType with DAY precision, or null if input is null.
   */
  public static DateTimeType toFhirDate(LocalDate localDate) {
    Date utilDate = toDate(localDate);
    DateTimeType fhirDate = new DateTimeType(utilDate);
    fhirDate.setPrecision(TemporalPrecisionEnum.DAY);
    return fhirDate;
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
    return date.toInstant().atZone(ZONE_ID_UTC);
  }

  /**
   * <a href="https://en.wikipedia.org/wiki/Anywhere_on_Earth">"Anywhere on Earth"</a> time which
   * indicates a period that expires when the date passes everywhere on Earth.
   *
   * @return local date
   */
  public static LocalDate nowAoe() {
    return LocalDate.now(ZoneId.of("-12:00"));
  }
}
