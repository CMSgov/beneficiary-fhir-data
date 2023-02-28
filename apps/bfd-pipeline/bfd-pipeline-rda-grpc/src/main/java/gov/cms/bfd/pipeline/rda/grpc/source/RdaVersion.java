package gov.cms.bfd.pipeline.rda.grpc.source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A utility class for comparing version numbers. */
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
  private final CompatibilityLevel compatability;

  /**
   * Builds an {@link RdaVersion} instance using the given version specifications.
   *
   * @param major The major version
   * @param minor The minor version
   * @param patch The patch version
   * @param compatability The level requirement
   */
  private RdaVersion(int major, int minor, int patch, CompatibilityLevel compatability) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.compatability = compatability;
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

    boolean compatable;

    switch (compatability) {
      case MAJOR:
        compatable =
            rdaVersion.major == major
                && ((rdaVersion.minor == minor && rdaVersion.patch >= patch)
                    || rdaVersion.minor > minor);
        break;
      case MINOR:
        compatable =
            rdaVersion.major == major && rdaVersion.minor == minor && rdaVersion.patch >= patch;
        break;
      default:
        compatable =
            rdaVersion.major == major && rdaVersion.minor == minor && rdaVersion.patch == patch;
    }

    return compatable;
  }

  /** The potential compatability levels that can be specified. */
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
