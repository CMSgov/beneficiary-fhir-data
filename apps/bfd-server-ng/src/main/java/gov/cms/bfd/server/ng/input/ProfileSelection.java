package gov.cms.bfd.server.ng.input;

import java.util.Map;

/**
 * The profiles requested with _profile, by category. Empty when _profile is not provided.
 *
 * @param profiles requested profile for each category
 */
public record ProfileSelection(Map<EobProfileCategory, ClaimProfile> profiles) {

  /** Used when _profile is not provided. */
  public static final ProfileSelection DEFAULT = new ProfileSelection(Map.of());

  /**
   * Creates a profile selection.
   *
   * @param profiles requested profile for each category
   */
  public ProfileSelection {
    profiles = Map.copyOf(profiles);
  }

  /**
   * Returns whether _profile was not provided.
   *
   * @return boolean
   */
  public boolean isDefault() {
    return profiles.isEmpty();
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
}
