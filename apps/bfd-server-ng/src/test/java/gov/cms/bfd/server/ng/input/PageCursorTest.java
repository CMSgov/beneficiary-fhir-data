package gov.cms.bfd.server.ng.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link PageCursor}. */
class PageCursorTest {

  @Test
  void encodeAndDecodeRoundTrip() {
    long claimId = 102435L;
    var cursor = PageCursor.of(claimId);
    String encoded = cursor.encode();

    assertNotNull(encoded);

    var decoded = PageCursor.parse(encoded);
    assertEquals(claimId, decoded.lastClaimUniqueId());
  }

  @Test
  void encodeProducesUrlSafeBase64() {
    var cursor = PageCursor.of(999999999999L);
    String encoded = cursor.encode();

    // URL-safe Base64 should not contain +, /, or = characters
    assertEquals(-1, encoded.indexOf('+'));
    assertEquals(-1, encoded.indexOf('/'));
    assertEquals(-1, encoded.indexOf('='));
  }

  @Test
  void parseValidCursor() {
    long expected = 54321L;
    String encoded =
        Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(expected).getBytes());

    var cursor = PageCursor.parse(encoded);
    assertEquals(expected, cursor.lastClaimUniqueId());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseNullOrEmptyCursorThrows(String input) {
    assertThrows(InvalidRequestException.class, () -> PageCursor.parse(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-valid-base64!!!", "YWJj"})
  void parseInvalidCursorThrows(String input) {
    // "YWJj" decodes to "abc" which is not a valid long
    assertThrows(InvalidRequestException.class, () -> PageCursor.parse(input));
  }

  @Test
  void ofCreatesCorrectCursor() {
    var cursor = PageCursor.of(12345L);
    assertEquals(12345L, cursor.lastClaimUniqueId());
  }

  @Test
  void encodeDeterministic() {
    var cursor1 = PageCursor.of(77777L);
    var cursor2 = PageCursor.of(77777L);
    assertEquals(cursor1.encode(), cursor2.encode());
  }

  @Test
  void differentIdsProduceDifferentCursors() {
    var encoded1 = PageCursor.of(100L).encode();
    var encoded2 = PageCursor.of(200L).encode();
    assertNotNull(encoded1);
    assertNotNull(encoded2);
    // Different IDs must produce different cursor strings
    assertEquals(false, encoded1.equals(encoded2));
  }
}
