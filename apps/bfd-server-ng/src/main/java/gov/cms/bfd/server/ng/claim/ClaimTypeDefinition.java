package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.SystemType;

/**
 * Definition of a claim type used for executing claim searches.
 *
 * @param baseQuery base query
 * @param claimClass entity class
 * @param systemType system type which indicates a claim's source
 */
public record ClaimTypeDefinition(
    String baseQuery, Class<? extends ClaimBase> claimClass, SystemType systemType) {}
