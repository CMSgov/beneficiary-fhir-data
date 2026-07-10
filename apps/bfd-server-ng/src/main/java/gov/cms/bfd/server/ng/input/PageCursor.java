package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Base64;

/**
 * Represents an opaque, URL-safe cursor for keyset-based pagination of ExplanationOfBenefit
 * resources. The cursor encodes the last seen {@code claimUniqueId} so subsequent pages can
 * efficiently seek past previously returned results using {@code WHERE clm_uniq_id > :cursor}.
 *
 * <p>This approach eliminates the performance degradation of offset-based pagination, where the
 * database must scan and discard rows proportional to the offset value.
 *
 * @param lastClaimUniqueId the {@code clm_uniq_id} of the last claim returned in the previous page
 */
public record PageCursor(long lastClaimUniqueId) {

  /**
   * Parses an opaque cursor string back into a {@link PageCursor}.
   *
   * @param cursorStr the Base64 URL-encoded cursor string from the client
   * @return the decoded PageCursor
   * @throws InvalidRequestException if the cursor string is malformed or cannot be decoded
   */
  public static PageCursor parse(String cursorStr) {
    if (cursorStr == null || cursorStr.isBlank()) {
      throw new InvalidRequestException("Cursor parameter must not be empty");
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursorStr));
      return new PageCursor(Long.parseLong(decoded));
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("Invalid cursor format: " + cursorStr);
    }
  }

  /**
   * Encodes this cursor into a URL-safe, opaque string suitable for inclusion in FHIR Bundle next
   * links.
   *
   * @return the Base64 URL-encoded cursor string
   */
  public String encode() {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(String.valueOf(lastClaimUniqueId).getBytes());
  }

  /**
   * Creates a cursor from the last claim in a result set.
   *
   * @param lastClaimUniqueId the unique ID of the last claim in the current page
   * @return a new PageCursor
   */
  public static PageCursor of(long lastClaimUniqueId) {
    return new PageCursor(lastClaimUniqueId);
  }
}
