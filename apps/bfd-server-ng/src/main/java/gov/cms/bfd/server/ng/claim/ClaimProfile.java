package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** enum that represents the exposed profile definitions for an ExplanationOfBenefit. */
@Getter
@AllArgsConstructor
public enum ClaimProfile {
  /** Fullest profile, default. */
  CMS(SystemUrls.CMS_STRUCTURE_DEFINITION_EOB, "", ""),
  /** Some information redacted, retains CARIN fields. */
  REGULAR(SystemUrls.CARIN_STRUCTURE_DEFINITION_EOB, "", SystemUrls.CARIN_VERSION),
  /** Least information, all adjudication removed. */
  BASIS(SystemUrls.CARIN_STRUCTURE_DEFINITION_EOB, "-Basis", SystemUrls.CARIN_VERSION);

  private final String baseUrl;
  private final String suffix;
  private final String version;

  /**
   * Gets the canonical URL for the given category.
   *
   * @param category profile category
   * @return canonical URL
   */
  public String getCanonicalUrl(EobProfileCategory category) {
    return baseUrl + category.getProfileName() + suffix;
  }

  /**
   * Gets the versioned URL for the given category.
   *
   * @param category profile category
   * @return profile URL
   */
  public String toFhirProfile(EobProfileCategory category) {
    var url = getCanonicalUrl(category);
    return version.isEmpty() ? url : url + "|" + version;
  }

  /**
   * Gets the meta.profile values for the given category.
   *
   * @param category profile category
   * @return profile URLs
   */
  public List<String> toFhirProfiles(EobProfileCategory category) {
    // CMS also conforms to C4BB, list C4BB first
    return switch (this) {
      case CMS -> List.of(REGULAR.toFhirProfile(category), toFhirProfile(category));
      case REGULAR, BASIS -> List.of(toFhirProfile(category));
    };
  }
}
