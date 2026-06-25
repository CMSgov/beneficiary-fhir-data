package gov.cms.bfd.server.ng.audit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Represents an AuditEventId.
 *
 * @param beneId patient ID
 * @param timestampToken canonical UTC timestamp token (yyyyMMddHHmmssnnnnnnnnn)
 */
public record AuditEventId(Long beneId, String timestampToken) {

  private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssnnnnnnnnn";
  private static final DateTimeFormatter TIMESTAMP_TOKEN_FORMATTER =
      DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

  /**
   * Generates AuditEvent Resource ID.
   *
   * @return resource id
   */
  public String getIdAsString() {
    return beneId() + "-" + timestampToken();
  }

  /**
   * Converts this ID to an exact Dynamo sort key timestamp string.
   *
   * @return ISO-8601 timestamp string used in Dynamo sort key
   */
  public String toDynamoSortKey() {
    validateTimestampToken(timestampToken());
    return parseTimestampTokenToInstant(timestampToken()).toString();
  }

  /**
   * Creates an ID from a Dynamo timestamp key.
   *
   * @param beneId patient id
   * @param dynamoTimestamp ISO-8601 timestamp from the Dynamo sort key
   * @return audit event id
   */
  public static AuditEventId fromDynamoTimestamp(Long beneId, String dynamoTimestamp) {
    var instant = Instant.parse(dynamoTimestamp);
    var token = TIMESTAMP_TOKEN_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    return new AuditEventId(beneId, token);
  }

  /**
   * Parse an Audit id string to an AuditId object.
   *
   * @param id string
   * @return id object
   */
  public static AuditEventId parse(String id) {
    var lastIndex = id.lastIndexOf("-");
    if (lastIndex == -1) {
      throw new IllegalArgumentException("Invalid AuditEvent id format");
    }

    var beneId = Long.parseLong(id.substring(0, lastIndex));
    var token = id.substring(lastIndex + 1);
    validateTimestampToken(token);
    return new AuditEventId(beneId, token);
  }

  private static void validateTimestampToken(String timestampToken) {
    if (!timestampToken.chars().allMatch(Character::isDigit)
        || timestampToken.length() != TIMESTAMP_FORMAT.length()) {
      throw new IllegalArgumentException("Invalid timestamp token format");
    }
  }

  private static Integer parseNextChars(StringBuffer buffer, int numChars) {
    var res = Integer.parseInt(buffer.substring(0, numChars));
    buffer.delete(0, numChars);
    return res;
  }

  private static Instant parseTimestampTokenToInstant(String timestampToken) {
    try {
      var buffer = new StringBuffer(timestampToken);
      var year = parseNextChars(buffer, 4);
      var month = parseNextChars(buffer, 2);
      var day = parseNextChars(buffer, 2);
      var hour = parseNextChars(buffer, 2);
      var minute = parseNextChars(buffer, 2);
      var second = parseNextChars(buffer, 2);
      var nanos = parseNextChars(buffer, 9);
      var localDateTime = LocalDateTime.of(year, month, day, hour, minute, second, nanos);
      return localDateTime.toInstant(ZoneOffset.UTC);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid timestamp token format", e);
    }
  }
}
