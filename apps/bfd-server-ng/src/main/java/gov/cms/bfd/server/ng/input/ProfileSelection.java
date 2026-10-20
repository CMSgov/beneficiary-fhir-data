package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.ClaimProfile;
import gov.cms.bfd.server.ng.claim.EobProfileCategory;
import java.util.Map;

/**
 * Represents the profiles requested for each category.
 *
 * @param profiles requested profile by category
 */
public record ProfileSelection(Map<EobProfileCategory, ClaimProfile> profiles) {

  /** Creates an empty ProfileSelection, until populated later. */
  public ProfileSelection() {
    this(Map.of());
  }

  /**
   * Returns whether the category was requested in the given profile.
   *
   * @param category profile category
   * @param profile claim profile
   * @return boolean
   */
  public boolean isRequested(EobProfileCategory category, ClaimProfile profile) {
    if (profiles.isEmpty()) {
      return profile == ClaimProfile.CMS;
    }
    return profiles.get(category) == profile;
  }

  /**
   * Gets the profile to use as a metric tag.
   *
   * @return CMS by default, MIXED if more than one, otherwise the profile name
   */
  public String getProfileCategory() {
    var requested = profiles.values().stream().distinct().toList();
    return switch (requested.size()) {
      case 0 -> ClaimProfile.CMS.name();
      case 1 -> requested.getFirst().name();
      default -> "MIXED";
    };
  }
}
