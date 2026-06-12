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

  private static final DateTimeFormatter TIMESTAMP_TOKEN_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssnnnnnnnnn");

  private static final int TIMESTAMP_TOKEN_LENGTH = 23;

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
    var isNegative = id.startsWith("-");
    if (isNegative) {
      id = id.replaceFirst("-", "");
    }
    String[] split = id.split("-", 2);
    if (split.length != 2) {
      throw new IllegalArgumentException("Invalid AuditEvent id format");
    }

    var beneId = Long.parseLong(isNegative ? "-" + split[0] : split[0]);
    var token = split[1];
    validateTimestampToken(token);
    return new AuditEventId(beneId, token);
  }

  private static void validateTimestampToken(String timestampToken) {
    if (!timestampToken.chars().allMatch(Character::isDigit)
        || timestampToken.length() != TIMESTAMP_TOKEN_LENGTH) {
      throw new IllegalArgumentException("Invalid timestamp token format");
    }
  }

  private static Instant parseTimestampTokenToInstant(String timestampToken) {
    try {
      var year = Integer.parseInt(timestampToken.substring(0, 4));
      var month = Integer.parseInt(timestampToken.substring(4, 6));
      var day = Integer.parseInt(timestampToken.substring(6, 8));
      var hour = Integer.parseInt(timestampToken.substring(8, 10));
      var minute = Integer.parseInt(timestampToken.substring(10, 12));
      var second = Integer.parseInt(timestampToken.substring(12, 14));
      var nanos = Integer.parseInt(timestampToken.substring(14, 23));
      var localDateTime = LocalDateTime.of(year, month, day, hour, minute, second, nanos);
      return localDateTime.toInstant(ZoneOffset.UTC);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid timestamp token format", e);
    }
  }
}
