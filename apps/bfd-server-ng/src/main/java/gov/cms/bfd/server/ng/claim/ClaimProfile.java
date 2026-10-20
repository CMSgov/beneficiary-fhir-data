package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** enum that represents the exposed profile definitions for an ExplanationOfBenefit. */
@Getter
@AllArgsConstructor
public enum ClaimProfile {
  /** Fullest profile, default. */
  CMS(SystemUrls.PROFILE_CMS_EOB_PREFIX, "", ""),
  /** Some information redacted, retains CARIN fields. */
  REGULAR(SystemUrls.PROFILE_C4BB_EOB_PREFIX, "", SystemUrls.PROFILE_C4BB_VERSION),
  /** Least information, all adjudication removed. */
  BASIS(SystemUrls.PROFILE_C4BB_EOB_PREFIX, "-Basis", SystemUrls.PROFILE_C4BB_VERSION);

  private final String baseUrl;
  private final String suffix;
  private final String version;

  /**
   * Gets the profile URL for the given category.
   *
   * @param category profile category
   * @return profile URL
   */
  public String getCanonicalUrl(EobProfileCategory category) {
    return baseUrl + category.getProfileName() + suffix;
  }

  /**
   * Gets the profile URLs for all categories.
   *
   * @return profile URLs
   */
  public List<String> getCanonicalUrls() {
    return Arrays.stream(EobProfileCategory.values()).map(this::getCanonicalUrl).toList();
  }

  /**
   * Gets the meta.profile values for the given category.
   *
   * @param category profile category
   * @return meta.profile references
   */
  public List<String> getMetaProfiles(EobProfileCategory category) {
    // CMS also conforms to C4BB (because it is a superset of C4BB)
    return switch (this) {
      case CMS ->
          List.of(REGULAR.getVersionedCanonicalUrl(category), getVersionedCanonicalUrl(category));
      case REGULAR, BASIS -> List.of(getVersionedCanonicalUrl(category));
    };
  }

  /**
   * Gets the _profile values that match this profile for the given category.
   *
   * @param category profile category
   * @return accepted URIs
   */
  public List<String> getAllowedUris(EobProfileCategory category) {
    // Distinct because CMS doesn't currently have a version
    return Stream.of(getCanonicalUrl(category), getVersionedCanonicalUrl(category))
        .distinct()
        .toList();
  }

  /**
   * Returns the canonical URL with a version attached to it, primarily for testing.
   *
   * @param category profile category
   * @return the versioned profile URL
   */
  public String getVersionedCanonicalUrl(EobProfileCategory category) {
    var url = getCanonicalUrl(category);
    return version.isEmpty() ? url : url + "|" + version;
  }
}
