package gov.cms.bfd.pipeline.rda.grpc.source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines a RDA version in the format of [major].[minor].[patch], with optional range modifier for
 * checking version compatibility.
 *
 * <p>The versioning system used here is a subset of Semantic Versioning, with support for exact
 * match, minor match, and major match. Examples of valid version definitions include:
 *
 * <ul>
 *   <li>0.10.1 - Must exactly match version 0.10.1
 *   <li>~0.10.1 - Must be a 0.10.x version equal to or higher than 0.10.1
 *   <li>^0.10.1 - Must be a 0.x.x version equal to or higher than 0.10.1
 * </ul>
 */
public class RdaVersion {

  /** Name for the type group of the RegEx. */
  private static final String TYPE_GROUP = "type";
  /** Name for the major group of the RegEx. */
  private static final String MAJOR_GROUP = "major";
  /** Name for the minor group of the RegEx. */
  private static final String MINOR_GROUP = "minor";
  /** Name for the patch group of the RegEx. */
  private static final String PATCH_GROUP = "patch";
  /** A RegEx used for matching a versioning string. */
  private static final Pattern VERSION_PATTERN =
      Pattern.compile(
          String.format(
              "^(?<%s>[~^])?(?<%s>\\d+)\\.(?<%s>\\d+)\\.(?<%s>\\d+)$",
              TYPE_GROUP, MAJOR_GROUP, MINOR_GROUP, PATCH_GROUP));

  /** The major version (first number). */
  private final int major;
  /** The minor version (middle number). */
  private final int minor;
  /** The patch version (last number). */
  private final int patch;

  /**
   * The required {@link CompatibilityLevel}, only used when comparing another {@link RdaVersion} to
   * this one.
   */
  private final CompatibilityLevel compatibility;

  /**
   * Builds an {@link RdaVersion} instance using the given version specifications.
   *
   * @param major The major version
   * @param minor The minor version
   * @param patch The patch version
   * @param compatibility The level requirement
   */
  private RdaVersion(int major, int minor, int patch, CompatibilityLevel compatibility) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.compatibility = compatibility;
  }

  /**
   * Creates a {@link RdaVersionBuilder} to be used to build an {@link RdaVersion} object.
   *
   * @return A {@link RdaVersionBuilder}.
   */
  public static RdaVersionBuilder builder() {
    return new RdaVersionBuilder();
  }

  /**
   * Determines if this {@link RdaVersion} specifies a version or range that would accept the given
   * {@link RdaVersion}.
   *
   * @param rdaVersionString The version being checked to see if it falls within range.
   * @return True if the given {@link RdaVersion} is within range of this instance's {@link
   *     RdaVersion} requirement.
   */
  public boolean allows(String rdaVersionString) {
    // When we connect to S3 or some other source, there is a prefix on the version to distinguish
    // it.
    String[] bits = rdaVersionString.split(":");
    String versionString = bits[bits.length - 1];
    // Parse the version
    RdaVersion rdaVersion = builder().versionString(versionString).build();

    boolean compatible;

    switch (compatibility) {
      case MAJOR:
        // Must be a higher or equal version, within the same major
        compatible =
            rdaVersion.major == major
                && ((rdaVersion.minor == minor && rdaVersion.patch >= patch)
                    || rdaVersion.minor > minor);
        break;
      case MINOR:
        // Must be a higher or equal version, within the same major & minor
        compatible =
            rdaVersion.major == major && rdaVersion.minor == minor && rdaVersion.patch >= patch;
        break;
      default:
        // Must be exactly the same version
        compatible =
            rdaVersion.major == major && rdaVersion.minor == minor && rdaVersion.patch == patch;
    }

    return compatible;
  }

  /** The potential compatibility levels that can be specified. */
  public enum CompatibilityLevel {
    /** Must be same major version. */
    MAJOR,
    /** Must be same Major/Minor version. */
    MINOR,
    /** Must match exact same Major, Minor, and Patch version. */
    PATCH
  }

  /** Helper class for building {@link RdaVersion} objects. */
  public static class RdaVersionBuilder {

    /** The version string to parse. */
    private String versionString;

    /** Empty private constructor to prevent direct instantiation. */
    private RdaVersionBuilder() {
      // Nothing to do
    }

    /**
     * Sets the version string to parse when building the {@link RdaVersion} object.
     *
     * @param versionString The version string to set.
     * @return A reference to this builder instance.
     */
    public RdaVersionBuilder versionString(String versionString) {
      this.versionString = versionString;
      return this;
    }

    /**
     * Builds an {@link RdaVersion} object instance using the previously given parameters.
     *
     * @return An {@link RdaVersion} object built with the previously provided parameters.
     */
    public RdaVersion build() {
      if (versionString != null) {
        Matcher matcher = VERSION_PATTERN.matcher(versionString);

        CompatibilityLevel level;
        int major;
        int minor;
        int patch;

        if (matcher.matches()) {
          String compatibility = matcher.group(TYPE_GROUP);

          if (compatibility != null) {
            level = compatibility.equals("~") ? CompatibilityLevel.MINOR : CompatibilityLevel.MAJOR;
          } else {
            level = CompatibilityLevel.PATCH;
          }

          try {
            major = Integer.parseInt(matcher.group(MAJOR_GROUP));
            minor = Integer.parseInt(matcher.group(MINOR_GROUP));
            patch = Integer.parseInt(matcher.group(PATCH_GROUP));
          } catch (NumberFormatException e) {
            // This shouldn't happen
            throw new IllegalStateException("Failed to parse RDA Version");
          }
        } else {
          throw new IllegalArgumentException(
              String.format("Invalid RdaVersion format '%s'", versionString));
        }

        return new RdaVersion(major, minor, patch, level);
      } else {
        throw new NullPointerException("versionString can not be null");
      }
    }
  }
}
