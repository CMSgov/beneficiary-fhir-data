package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.EobProfile;
import gov.cms.bfd.server.ng.claim.EobProfileCategory;
import gov.cms.bfd.server.ng.claim.EobProfileType;
import java.util.Set;

/**
 * Represents the profiles requested for each category of EoB.
 *
 * @param profiles requested profile by category
 */
public record EobProfileSelection(Set<EobProfile> profiles) {

  /** Default selection: every category as CMS. */
  public EobProfileSelection() {
    this(
        Set.of(
            EobProfile.PHARMACY_CMS,
            EobProfile.INPATIENT_INSTITUTIONAL_CMS,
            EobProfile.OUTPATIENT_INSTITUTIONAL_CMS,
            EobProfile.PROFESSIONAL_NONCLINICIAN_CMS));
  }

  /**
   * Returns whether the category was requested in the given profile.
   *
   * @param category profile category
   * @param profileType claim profile
   * @return boolean
   */
  public boolean isRequested(EobProfileCategory category, EobProfileType profileType) {
    return profiles.contains(EobProfile.of(category, profileType));
  }

  /**
   * Gets the profile to use as a metric tag.
   *
   * @return CMS by default, MIXED if more than one, otherwise the profile name
   */
  public String getProfileTag() {
    var requested = profiles.stream().map(EobProfile::getProfileType).distinct().toList();
    return requested.size() == 1 ? requested.getFirst().name() : "MIXED";
  }
}
