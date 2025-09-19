package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.TimestampUtil;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimestampUtilTest {

  private static final ZonedDateTime ZDT =
      ZonedDateTime.of(2026, 8, 5, 2, 7, 9, 336_000_000, ZoneOffset.UTC);

  @Test
  void zonedDateTimeIsConvertedToUtcSameInstant() {
    var input = ZonedDateTime.of(2025, 9, 10, 12, 0, 0, 0, ZoneId.of("America/Los_Angeles"));
    var out = TimestampUtil.toUtcZdt(input);
    assertNotNull(out);
    // same instant
    assertEquals(input.toInstant(), out.toInstant());
    assertEquals(DateUtil.ZONE_ID_UTC, out.getZone());
  }

  @Test
  void charsequenceIsoParsesViaParseIso() {
    var text = "2025-09-01T12:34:56-07:00";
    var out = TimestampUtil.toUtcZdt(text);
    assertNotNull(out);
    assertEquals(java.time.OffsetDateTime.parse(text).toInstant(), out.toInstant());
    assertEquals(DateUtil.ZONE_ID_UTC, out.getZone());
  }

  @Test
  void nonCharSequenceToStringFallbackParsesIso() {
    Object obj =
        new Object() {
          @Override
          public String toString() {
            return "2025-09-02T00:00:00Z";
          }
        };

    var out = TimestampUtil.toUtcZdt(obj);
    assertNotNull(out);
    assertEquals(java.time.Instant.parse("2025-09-02T00:00:00Z"), out.toInstant());
    assertEquals(DateUtil.ZONE_ID_UTC, out.getZone());
  }

  @Test
  @DisplayName("Converts supported temporal types to UTC ZonedDateTime")
  void convertsSupportedTypes() {
    assertEquals(
        ZDT.toInstant(), TimestampUtil.toUtcZdt(Timestamp.from(ZDT.toInstant())).toInstant());
    assertEquals(ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT.toOffsetDateTime()).toInstant());
    assertEquals(ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT.toLocalDateTime()).toInstant());
    assertEquals(
        ZDT.toInstant(), TimestampUtil.toUtcZdt(java.util.Date.from(ZDT.toInstant())).toInstant());
    assertEquals(ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT).toInstant());
    assertEquals(ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT.toInstant()).toInstant());
  }

  @Test
  @DisplayName("Parses string / toString fallbacks")
  void parsesStrings() {
    assertEquals(
        ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT.toOffsetDateTime().toString()).toInstant());
    assertEquals(ZDT.toInstant(), TimestampUtil.toUtcZdt(ZDT.toInstant().toString()).toInstant());
  }

  @Test
  @DisplayName("Returns null on unrecognized input")
  void returnsNullOnUnknown() {
    assertNull(TimestampUtil.toUtcZdt(12345));
    assertNull(TimestampUtil.toUtcZdt(null));
  }

  @Test
  @DisplayName("Never returns pre-epoch sentinel unless input is null/invalid")
  void notSentinelUnlessInvalid() {
    assertNotEquals(DateUtil.MIN_DATETIME, TimestampUtil.toUtcZdt(ZDT));
  }
}
