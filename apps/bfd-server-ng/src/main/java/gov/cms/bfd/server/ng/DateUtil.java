package gov.cms.bfd.server.ng;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
   * UTC {@link ZoneId}. UTC should be used for all datetime conversions to/from an instant
   * represented by {@link java.util.Date}.
   */
  public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

  /** Minimum possible {@link ZonedDateTime}. */
  public static final ZonedDateTime MIN_DATETIME =
      ZonedDateTime.of(LocalDateTime.MIN, DateUtil.ZONE_ID_UTC);

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
   * Converts the {@link ZonedDateTime} to a {@link Date} object with the same date and time info.
   *
   * <p>Converts a LocalDate to a FHIR DateTimeType with DAY precision. The underlying instant is
   * midnight UTC of the given LocalDate.
   *
   * @param localDate The LocalDate to convert.
   * @return A FHIR DateTimeType with DAY precision, or null if input is null.
   */
  public static DateTimeType toFhirDate(LocalDate localDate) {
    if (localDate == null) {
      return null;
    }
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
}
