package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Data;

/** Immutable object containing the numeric portion of a semantic version number. */
@Data
public class SemanticVersion implements Comparable<SemanticVersion> {
  /** Regex used to parse version strings into their component parts. */
  private static final Pattern VERSION_REGEX = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?");

  /** Shared instance to use when needing a default, invalid value. */
  public static SemanticVersion ZERO = new SemanticVersion(0, 0, 0);

  /** {@link Comparator} used to compare two versions in ascending order. */
  private static final Comparator<SemanticVersion> NaturalOrder =
      Comparator.comparing(SemanticVersion::getMajor)
          .thenComparing(SemanticVersion::getMinor)
          .thenComparing(SemanticVersion::getPatch);

  /** Major version number. */
  private final int major;
  /** Minor version number. */
  private final int minor;
  /** Patch version number. */
  private final int patch;

  /**
   * Checks to see if our fields have valid values. No field can have a negative value and at least
   * one field must have a positive value.
   *
   * @return true if the fields all have valid values
   */
  public boolean isValid() {
    return major >= 0 && minor >= 0 && patch >= 0 && (major > 0 || minor > 0 || patch > 0);
  }

  /**
   * Parse the provided string to produce a valid {@link SemanticVersion} object.
   *
   * @param versionString the string to parse
   * @return an {@link Optional} containing the valid {@link SemanticVersion} or empty if the string
   *     was invalid
   */
  public static Optional<SemanticVersion> parse(String versionString) {
    var version = Optional.<SemanticVersion>empty();
    var matcher = VERSION_REGEX.matcher(versionString);
    if (matcher.matches()) {
      version =
          Optional.of(
                  new SemanticVersion(
                      parseGroup(matcher.group(1)),
                      parseGroup(matcher.group(3)),
                      parseGroup(matcher.group(5))))
              .filter(v -> v.isValid());
    }
    return version;
  }

  /**
   * Combine the provided components of the version and construct a {@link SemanticVersion}.
   * Validate that the components are valid and return either an {@link Optional} containing a valid
   * version or an empty {@link Optional} if they are invalid.
   *
   * @param major Major version number
   * @param minor Minor version number
   * @param patch Patch version number
   * @return Filled {@link Optional} if the components are valid or an empty one if they are not.
   */
  public static Optional<SemanticVersion> fromComponents(int major, int minor, int patch) {
    var version = new SemanticVersion(major, minor, patch);
    return version.isValid() ? Optional.of(version) : Optional.empty();
  }

  @Override
  public int compareTo(@Nonnull SemanticVersion other) {
    return NaturalOrder.compare(this, other);
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch;
  }

  /**
   * Helper method to parse one of the numeric groups from the version string. If the group was not
   * present in the string its value will be zero. Otherwise it will be parsed into an integer
   * value.
   *
   * @param groupString matching group string or null if not present in the string
   * @return parsed integer value for the group string
   */
  private static int parseGroup(@Nullable String groupString) {
    return Strings.isNullOrEmpty(groupString) ? 0 : Integer.parseInt(groupString);
  }
}
