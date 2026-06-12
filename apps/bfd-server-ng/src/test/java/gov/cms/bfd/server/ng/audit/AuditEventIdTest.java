package gov.cms.bfd.server.ng.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuditEventIdTest {

  @Test
  void canonicalTimestampTokenRoundTrips() {
    var id = AuditEventId.fromDynamoTimestamp(123L, "2026-06-15T21:09:07.123456Z");

    assertEquals("123-20260615210907123456000", id.getIdAsString());
    assertEquals("2026-06-15T21:09:07.123456Z", id.toDynamoSortKey());

    var parsed = AuditEventId.parse(id.getIdAsString());
    assertEquals(id, parsed);
  }

  @Test
  void parseRejectsInvalidId() {
    assertThrows(IllegalArgumentException.class, () -> AuditEventId.parse("bad-id"));
    assertThrows(IllegalArgumentException.class, () -> AuditEventId.parse("123-not-a-timestamp"));
    assertThrows(IllegalArgumentException.class, () -> AuditEventId.parse("123-1718442007123"));
  }
}
