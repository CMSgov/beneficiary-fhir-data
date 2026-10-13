package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.filter.ClaimTypeCodeFilterParam;
import gov.cms.bfd.server.ng.claim.model.common.ClaimType;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.common.entities.ClaimBase;
import gov.cms.bfd.server.ng.input.ClaimProfile;
import gov.cms.bfd.server.ng.input.EobProfileCategory;
import gov.cms.bfd.server.ng.input.ProfileSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
   * Gets the filters to query this claim type with for the requested profiles.
   *
   * @param profileSelection requested profiles
   * @param filters filters from the search criteria
   * @return the filters, or empty if this claim type should not be queried
   */
  public Optional<List<DbFilterBuilder>> getProfileFilters(
      ProfileSelection profileSelection, List<DbFilterBuilder> filters) {
    if (profileSelection.isDefault()) {
      return profile == ClaimProfile.CMS ? Optional.of(filters) : Optional.empty();
    }
    var categories = EobProfileCategory.fromClaimType(claimType);
    var requested =
        categories.stream()
            .filter(category -> profileSelection.isRequested(category, profile))
            .toList();
    if (requested.isEmpty()) {
      return Optional.empty();
    }
    if (requested.size() == categories.size()) {
      return Optional.of(filters);
    }
    // Only some of this claim type's categories were requested, so narrow it by claim type code
    var claimTypeCodes =
        requested.stream().flatMap(category -> category.getClaimTypeCodes().stream()).toList();
    var profileFilters = new ArrayList<>(filters);
    profileFilters.add(new ClaimTypeCodeFilterParam(PROFILE_CLAIM_TYPE_CODES, claimTypeCodes));
    return Optional.of(profileFilters);
  }
}
