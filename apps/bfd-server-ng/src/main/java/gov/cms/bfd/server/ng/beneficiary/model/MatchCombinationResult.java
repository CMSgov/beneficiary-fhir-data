package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;

/**
 * Represents the result of evaluating a set of patient match criteria.
 *
 * @param combination the combination index
 * @param matchType the type of match
 * @param matchedRecords the list of matched records for this combination
 */
public record MatchCombinationResult(
    String combination, String matchType, List<MatchedRecord> matchedRecords) {}
