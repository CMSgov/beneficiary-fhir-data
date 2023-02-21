package gov.cms.bfd.pipeline.rda.grpc.source;

/** A utility class for comparing version numbers. */
public class RdaVersion {

  /** The major version (first number). */
  private final int major;
  /** The minor version (middle number). */
  private final int minor;
  /** The patch version (last number). */
  private final int patch;

  /**
   * The required {@link CompatabilityLevel}, only used when comparing another {@link RdaVersion} to
   * this one.
   */
  private final CompatabilityLevel compatability;

  /**
   * Builds an {@link RdaVersion} instance using the given version specifications.
   *
   * @param major The major version
   * @param minor The minor version
   * @param patch The patch version
   * @param compatability The level requirement
   */
  private RdaVersion(int major, int minor, int patch, CompatabilityLevel compatability) {
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
        compatable = rdaVersion.major == major;
        break;
      case MINOR:
        compatable = rdaVersion.major == major && rdaVersion.minor == minor;
        break;
      default:
        compatable =
            rdaVersion.major == major && rdaVersion.minor == minor && rdaVersion.patch == patch;
    }

    return compatable;
  }

  /** The potential compatability levels that can be specified. */
  public enum CompatabilityLevel {
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
        CompatabilityLevel level = CompatabilityLevel.PATCH;

        // Array includes: { Level, Major, Minor, Patch }
        Integer[] versionParts = new Integer[] {0, 0, 0, 0};
        int stage = 0;

        // Go through each character and parse out the version parts
        for (int i = 0; i < versionString.length() && stage < versionParts.length; ) {
          char c = versionString.charAt(i);

          // If we're at first stage, look if there is a modifier (~ or ^) to specify compatability
          // level
          if (stage == 0) {
            switch (c) {
              case '^':
                level = CompatabilityLevel.MAJOR;
                // Character found, move to the next character
                ++i;
                break;
              case '~':
                level = CompatabilityLevel.MINOR;
                // Character found, move to the next character
                ++i;
                break;
              default:
                // No special character found, don't advance, so we can process it as a version
                // number
                level = CompatabilityLevel.PATCH;
            }

            // Advance to the next stage
            ++stage;
          } else {
            // Check if it's a digit
            if (Character.isDigit(c)) {
              // Add it to the current version part we're on
              versionParts[stage] *= 10;
              versionParts[stage] += (c - '0');
            } else if (c == '.') {
              // If we find a dot, move to the next stage
              ++stage;
            } else {
              // If it's not a digit or a dot, it's illegal
              throw new IllegalArgumentException(
                  String.format(
                      "Unexpected character found in version string '%s'", versionString));
            }

            // Move to the next character after we've processed it
            ++i;
          }
        }

        // Grab our version parts (index 0 was compatability level, but we ignored it, it was only
        // to make
        // advancing the stages simpler)
        int major = versionParts[1];
        int minor = versionParts[2];
        int patch = versionParts[3];

        return new RdaVersion(major, minor, patch, level);
      } else {
        throw new NullPointerException("versionString can not be null");
      }
    }
  }
}
