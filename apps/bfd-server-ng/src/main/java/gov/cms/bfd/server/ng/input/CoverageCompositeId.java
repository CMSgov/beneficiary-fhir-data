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
  // Example: "part-a-123", "part-b--456"
  private static final Pattern STANDARD_COVERAGE_ID_PATTERN =
      Pattern.compile("(\\p{Alnum}+-?\\p{Alnum})-(-?\\d+)", Pattern.CASE_INSENSITIVE);

  // Group 1: optional prefix + coverage part (e.g., "part-c", "c4dic-part-c")
  // Group 2: The bene_sk
  // Group 3: The contract number (H1234)
  // Group 4: The pbp number (001)
  // Example: "part-c-12345-H1234-001", "c4dic-part-c-12345-H1234-001"
  private static final Pattern PART_C_AND_D_COVERAGE_ID_PATTERN =
      Pattern.compile(
          "((?:\\p{Alnum}+\\-)?part-[cd])-(\\d+)-(\\p{Alnum}{5})-(\\d{3})",
          Pattern.CASE_INSENSITIVE);

  /**
   * Parses a raw composite ID string (e.g., "part-a-12345", "{type}-{bene_sk}-{contract}-{pbp}" for
   * part C/D so "part-c-12345-H1234-001") into a {@link CoverageCompositeId}.
   *
   * @param rawCompositeId The raw composite ID string.
   * @return A {@link CoverageCompositeId} instance.
   * @throws InvalidRequestException if the ID format is invalid or the part prefix is not
   *     recognized.
   */
  public static CoverageCompositeId parse(String rawCompositeId) {

    Matcher standardMatcher = STANDARD_COVERAGE_ID_PATTERN.matcher(rawCompositeId.trim());
    if (standardMatcher.matches()) {
      var rawPartPrefix = standardMatcher.group(1);
      var beneSkStr = standardMatcher.group(2);

      // CoveragePart will find a match if rawPartPrefix is "part-a", "part-b", etc.
      // (case-insensitive)
      var part = CoveragePart.fromExactRawPrefixOrThrow(rawPartPrefix);
      try {
        return new CoverageCompositeId(part, Long.parseLong(beneSkStr));
      } catch (NumberFormatException _) {
        throw new InvalidRequestException("Invalid beneficiary SK format in Coverage ID");
      }
    }

    Matcher partCAndDMatcher = PART_C_AND_D_COVERAGE_ID_PATTERN.matcher(rawCompositeId.trim());
    if (partCAndDMatcher.matches()) {
      var rawPartPrefix = partCAndDMatcher.group(1); // "part-c", "c4dic-part-c"
      var beneSkStr = partCAndDMatcher.group(2);

      // CoveragePart will find a match if rawPartPrefix is "part-c", "part-d", etc.
      // (case-insensitive)
      var part = CoveragePart.fromExactRawPrefixOrThrow(rawPartPrefix);
      try {
        return new CoverageCompositeId(part, Long.parseLong(beneSkStr));
      } catch (NumberFormatException _) {
        throw new InvalidRequestException("Invalid beneficiary SK format in Coverage ID");
      }
    }

    throw new InvalidRequestException(
        "Invalid Coverage ID format. Expected pattern like 'part-c-12345-H1234-001'.");
  }

  /**
   * Returns the full ID as a string.
   *
   * @return ID
   */
  public String fullId() {
    return coveragePart.getStandardSystem() + "-" + beneSk;
  }
}
