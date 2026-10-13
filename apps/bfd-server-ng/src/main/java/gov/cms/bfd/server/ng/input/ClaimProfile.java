package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Profiles an ExplanationOfBenefit can be rendered in. */
@Getter
@AllArgsConstructor
public enum ClaimProfile {
  /** CMS Blue Button profile. Contains all available elements. This is the default. */
  CMS(SystemUrls.CMS_STRUCTURE_DEFINITION_EOB, "", ""),
  /** C4BB profile. Contains CARIN elements, including financial information. */
  REGULAR(SystemUrls.CARIN_STRUCTURE_DEFINITION_EOB, "", SystemUrls.CARIN_VERSION),
  /** C4BB Basis profile. Contains no financial information. */
  BASIS(SystemUrls.CARIN_STRUCTURE_DEFINITION_EOB, "-Basis", SystemUrls.CARIN_VERSION);

  private final String baseUrl;
  private final String suffix;
  private final String version;

  /**
   * Gets the unversioned canonical URL of this profile for a category.
   *
   * @param category profile category
   * @return canonical URL
   */
  public String getCanonicalUrl(EobProfileCategory category) {
    return baseUrl + category.getProfileName() + suffix;
  }

  /**
   * Gets the URL of this profile for a category, including the version if there is one.
   *
   * @param category profile category
   * @return profile URL
   */
  public String toFhirProfile(EobProfileCategory category) {
    var url = getCanonicalUrl(category);
    return version.isEmpty() ? url : url + "|" + version;
  }

  /**
   * Gets the meta.profile values for a resource rendered in this profile. CMS resources also
   * conform to C4BB, which is listed first.
   *
   * @param category profile category
   * @return profile URLs
   */
  public List<String> toFhirProfiles(EobProfileCategory category) {
    return switch (this) {
      case CMS -> List.of(REGULAR.toFhirProfile(category), toFhirProfile(category));
      case REGULAR, BASIS -> List.of(toFhirProfile(category));
    };
  }
}
