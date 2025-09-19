package gov.cms.bfd.server.ng.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/** Utility helpers for converting common time representations into UTC ZonedDateTime. */
public final class TimestampUtil {
  private TimestampUtil() {}

  /**
   * Try to turn the given value into a UTC {@link ZonedDateTime}.
   *
   * <p>It accepts several common time types or a text representation (ISO-8601). If the value can't
   * be understood, the method returns {@code null}.
   *
   * @param raw the value to convert
   * @return the same instant expressed as a {@link ZonedDateTime} in UTC, or {@code null} if it
   *     can't be parsed
   */
  public static ZonedDateTime toUtcZdt(Object raw) {
    if (raw == null) return null;
    try {
      if (raw instanceof java.sql.Timestamp ts) return ts.toInstant().atZone(DateUtil.ZONE_ID_UTC);
      if (raw instanceof OffsetDateTime odt) return odt.toInstant().atZone(DateUtil.ZONE_ID_UTC);
      if (raw instanceof LocalDateTime ldt) return ldt.atZone(DateUtil.ZONE_ID_UTC);
      if (raw instanceof Date d) return d.toInstant().atZone(DateUtil.ZONE_ID_UTC);
      if (raw instanceof Instant inst) return inst.atZone(DateUtil.ZONE_ID_UTC);
      if (raw instanceof ZonedDateTime zdt) return zdt.withZoneSameInstant(DateUtil.ZONE_ID_UTC);
      if (raw instanceof CharSequence cs) {
        var z = parseIso(cs.toString());
        if (z != null) return z;
      }
      // last attempt using toString() output.
      var s2 = raw.toString();
      return parseIso(s2);
    } catch (Exception ignoredOuter) {
      return null;
    }
  }

  private static ZonedDateTime parseIso(String text) {
    if (text == null || text.isEmpty()) return null;
    try {
      return OffsetDateTime.parse(text).toInstant().atZone(DateUtil.ZONE_ID_UTC);
    } catch (Exception ignored) {
      try {
        return Instant.parse(text).atZone(DateUtil.ZONE_ID_UTC);
      } catch (Exception ignored2) {
        return null;
      }
    }
  }
}
