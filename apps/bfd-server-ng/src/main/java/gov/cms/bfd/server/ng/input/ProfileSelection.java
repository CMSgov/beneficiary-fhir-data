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

  /** Creates a CMS default ProfileSelection. */
  public ProfileSelection() {
    this(
        Map.of(
            EobProfileCategory.PHARMACY, ClaimProfile.CMS,
            EobProfileCategory.INPATIENT_INSTITUTIONAL, ClaimProfile.CMS,
            EobProfileCategory.OUTPATIENT_INSTITUTIONAL, ClaimProfile.CMS,
            EobProfileCategory.PROFESSIONAL_NONCLINICIAN, ClaimProfile.CMS));
  }

  /**
   * Returns whether the category was requested in the given profile.
   *
   * @param category profile category
   * @param profile claim profile
   * @return boolean
   */
  public boolean isRequested(EobProfileCategory category, ClaimProfile profile) {
    return profiles.get(category) == profile;
  }

  /**
   * Gets the profile to use as a metric tag.
   *
   * @return CMS by default, MIXED if more than one, otherwise the profile name
   */
  public String getProfileTag() {
    var requested = profiles.values().stream().distinct().toList();
    return requested.size() > 1 ? "MIXED" : requested.getFirst().name();
  }
}
