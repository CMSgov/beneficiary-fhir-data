package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Strings;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Immutable object that can match version strings to determine if they lie within acceptable
 * bounds. Syntax is based on maven rules.
 *
 * <p>A range string can either be a single version or a range starting with either {@code [}
 * (inclusive) or {@code (} (exclusive), then two versions (lower bound and upper bound) separated
 * by a comma, then either {@code ]} (inclusive) or {@code )} (exclusive). The lower and/or upper
 * bounds in the middle can be empty to indicate that any version is allowed for that position.
 * {@code 1.2} would only match {@code 1.2} version whereas {@code [1.0,2.0)} would match any
 * version greater than or equal to {@code 1.0} and less than {@code 2.0}. {@code [,2.0]} would
 * match any version less than or equal to {@code 2.0}.
 */
@AllArgsConstructor
public class SemanticVersionRange {
  /** Shared value for a bound that accepts any version. */
  private static final Bound ANY = new Bound(BoundType.ANY, SemanticVersion.ZERO);

  /**
   * Shared value for an all inclusive {@link SemanticVersionRange} suitable for use as a default.
   */
  public static final SemanticVersionRange AcceptAll = new SemanticVersionRange(ANY, ANY);

  /** Regular expression used to parse valid range strings. */
  private static final Pattern RANGE_REGEX = Pattern.compile("([(\\[])([^,]*),([^,]*)([)\\]])");

  /** Lower bound of the range. */
  private final Bound lowBound;
  /** Upper bound of the range. */
  private final Bound highBound;

  /**
   * Attempt to parse a range string. Any errors in the format result in an empty value being
   * returned. Example valid ranges are {@code [1.0,2.0]}, {@code [1.0,2.0)}.
   *
   * @param rangeString string to parse
   * @return {@link Optional} containing a valid {@link SemanticVersionRange} if range string is
   *     valid, otherwise empty
   */
  public static Optional<SemanticVersionRange> parse(String rangeString) {
    final var version = SemanticVersion.parse(rangeString);
    if (version.isPresent()) {
      return version.map(v -> new SemanticVersionRange(new Bound(BoundType.EQ, v), ANY));
    }

    final var matcher = RANGE_REGEX.matcher(rangeString);
    if (matcher.matches()) {
      final var lowBound = parseBound(matcher.group(1).charAt(0), matcher.group(2));
      final var highBound = parseBound(matcher.group(4).charAt(0), matcher.group(3));
      return lowBound.flatMap(low -> highBound.map(high -> new SemanticVersionRange(low, high)));
    }

    return Optional.empty();
  }

  /**
   * Determine if the specified version is contained within this range.
   *
   * @param version {@link SemanticVersion} to check
   * @return true if within the range, false otherwise
   */
  public boolean contains(SemanticVersion version) {
    return lowBound.matches(version) && highBound.matches(version);
  }

  /**
   * Matches a character from the beginning or end of the range string to its assigned {@link
   * BoundType}. Must only be passed the character matched by the regex.
   *
   * @param boundChar first or last character of range string
   * @return the correct {@link BoundType}
   * @throws BadCodeMonkeyException if passed an invalid character
   */
  private static BoundType parseBoundType(char boundChar) {
    switch (boundChar) {
      case '(':
        return BoundType.GT;
      case '[':
        return BoundType.GE;
      case ')':
        return BoundType.LT;
      case ']':
        return BoundType.LE;
      default:
        throw new BadCodeMonkeyException("not possible unless regex is broken");
    }
  }

  /**
   * Parses a boundary character and version string from the range string to produce a {@link
   * Bound}. {@code versionString} can be empty to indicate any version is OK.
   *
   * @param boundChar first or last character of range string
   * @param versionString one of the two version strings within the version string
   * @return a valid {@link Bound} or empty if the {@code versionString} is not a valid version
   */
  private static Optional<Bound> parseBound(char boundChar, String versionString) {
    if (Strings.isNullOrEmpty(versionString)) {
      return Optional.of(new Bound(BoundType.ANY, SemanticVersion.ZERO));
    }
    final var type = parseBoundType(boundChar);
    final var value = SemanticVersion.parse(versionString);
    return value.map(version -> new Bound(type, version));
  }

  /** Controls how a version is compared to the boundary value. */
  private enum BoundType {
    /** Accept any value. */
    ANY,
    /** Accept only exactly the same value. */
    EQ,
    /** Accept only less than or equal values. */
    LE,
    /** Accept only less than values. */
    LT,
    /** Accept only greater than or equal values. */
    GE,
    /** Accept only greater than values. */
    GT
  }

  /** An upper or lower bound for the range. */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class Bound {
    private final BoundType type;
    private final SemanticVersion value;

    /**
     * Compare the {@link SemanticVersion} to our value using appropriate comparison for our type.
     *
     * @param version {@link SemanticVersion} to check
     * @return true if the value is matched
     */
    private boolean matches(SemanticVersion version) {
      final var compared = version.compareTo(value);
      switch (type) {
        case ANY:
          return true;
        case EQ:
          return compared == 0;
        case LE:
          return compared <= 0;
        case LT:
          return compared < 0;
        case GE:
          return compared >= 0;
        case GT:
          return compared > 0;
        default:
          throw new BadCodeMonkeyException("not possible if all enum values covered by switch");
      }
    }
  }
}
