package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.filter.ClaimTypeCodeFilterParam;
import gov.cms.bfd.server.ng.claim.model.common.ClaimType;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.input.ProfileSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a claim type used for executing claim searches.
 *
 * @param baseQuery base query
 * @param claimClass entity class
 * @param systemType system type which indicates a claim's source
 * @param claimType claim type of the entity class
 * @param profile the profile the entity class is rendered in
 */
public record ClaimTypeDefinition(
    String baseQuery,
    Class<? extends ClaimBase> claimClass,
    SystemType systemType,
    ClaimType claimType,
    ClaimProfile profile) {

  private static final String PROFILE_CLAIM_TYPE_CODES = "profileClaimTypeCodes";

  /**
   * Determine whether this claim type definition is compatible with any active filters. Inactive
   * filters do not exclude the claim type. search criteria.
   *
   * @param filters the filter builders to check against
   * @return boolean
   */
  public boolean matchesSystemType(List<DbFilterBuilder> filters) {
    return filters.stream().allMatch(filter -> filter.matchesSystemType(systemType));
  }

  /**
   * Determine whether this claim type definition belongs to the requested profiles.
   *
   * @param profileSelection requested profiles
   * @return boolean
   */
  public boolean matchesProfile(ProfileSelection profileSelection) {
    if (profileSelection.profiles().isEmpty()) {
      return profile == ClaimProfile.CMS;
    }
    return EobProfileCategory.fromClaimType(claimType).stream()
        .anyMatch(category -> profileSelection.isRequested(category, profile));
  }

  /**
   * Adds a claim type code filter when only some of this claim type's categories were requested.
   *
   * @param profileSelection requested profiles
   * @param filters filters from the search criteria
   * @return filters
   */
  public List<DbFilterBuilder> getProfileFilters(
      ProfileSelection profileSelection, List<DbFilterBuilder> filters) {

    var categories = EobProfileCategory.fromClaimType(claimType);
    var requested =
        categories.stream()
            .filter(category -> profileSelection.isRequested(category, profile))
            .toList();

    if (profileSelection.profiles().isEmpty() || requested.size() == categories.size()) {
      return filters;
    }

    var claimTypeCodes =
        requested.stream().flatMap(category -> category.getClaimTypeCodes().stream()).toList();
    var profileFilters = new ArrayList<>(filters);
    profileFilters.add(new ClaimTypeCodeFilterParam(PROFILE_CLAIM_TYPE_CODES, claimTypeCodes));

    return profileFilters;
  }
}
