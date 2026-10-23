package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.EobProfile;
import gov.cms.bfd.server.ng.claim.EobProfileCategory;
import gov.cms.bfd.server.ng.claim.EobProfileType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the profiles requested for each category of EoB.
 *
 * @param profiles requested profile by category
 */
public record EobProfileSelection(Set<EobProfile> profiles) {

  /** Default selection: every category as CMS. */
  public EobProfileSelection() {
    this(
        Arrays.stream(EobProfile.values())
            .filter(p -> p.getProfileType() == EobProfileType.CMS)
            .collect(Collectors.toSet()));
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
