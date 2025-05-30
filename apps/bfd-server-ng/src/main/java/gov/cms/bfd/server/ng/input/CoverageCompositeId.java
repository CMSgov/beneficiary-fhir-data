package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parsed Coverage composite ID, in the format "{part_prefix}-{bene_sk}". Example:
 * "part-a-12345".
 *
 * @param coveragePart The standardized {@link CoveragePart}.
 * @param beneSk The beneficiary surrogate key.
 */
public record CoverageCompositeId(CoveragePart coveragePart, long beneSk) {

  // Group 1: The entire part prefix (e.g., "part-a", "c4dic-part-b")
  // Group 2: The bene_sk
  private static final Pattern STANDARD_COVERAGE_ID_PATTERN =
      Pattern.compile("(\\p{Alnum}+-?\\p{Alnum})-(-?\\p{Digit}+)", Pattern.CASE_INSENSITIVE);

  // Example: "part-a-123", "partb--456"

  /**
   * Parses a raw composite ID string (e.g., "part-a-12345") into a {@link CoverageCompositeId}.
   *
   * @param rawCompositeId The raw composite ID string.
   * @return A {@link CoverageCompositeId} instance.
   * @throws InvalidRequestException if the ID format is invalid or the part prefix is not
   *     recognized.
   */
  public static CoverageCompositeId parse(String rawCompositeId) {

    Matcher standardMatcher = STANDARD_COVERAGE_ID_PATTERN.matcher(rawCompositeId.trim());
    if (standardMatcher.matches()) {
      String rawPartPrefix = standardMatcher.group(1);
      String beneSkStr = standardMatcher.group(2);

      // CoveragePart will find a match if rawPartPrefix is "part-a", "part-b", etc.
      // (case-insensitive)
      CoveragePart part = CoveragePart.fromExactRawPrefixOrThrow(rawPartPrefix);
      try {
        return new CoverageCompositeId(part, Long.parseLong(beneSkStr));
      } catch (NumberFormatException e) {
        throw new InvalidRequestException(
            "Invalid beneficiary SK format in Coverage ID: '" + beneSkStr + "'");
      }
    }

    // If the pattern did not match
    throw new InvalidRequestException(
        "Invalid Coverage ID format. Expected pattern like 'part-a-123'. Got: '"
            + rawCompositeId
            + "'");
  }
}
