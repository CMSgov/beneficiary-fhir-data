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

  /** Creates an empty {@link ProfileSelection}. */
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
    return profiles.get(category) == profile;
  }

  /**
   * Returns whether any profiles were provided.
   *
   * @return boolean
   */
  public boolean hasProfiles() {
    return !profiles.isEmpty();
  }
}
