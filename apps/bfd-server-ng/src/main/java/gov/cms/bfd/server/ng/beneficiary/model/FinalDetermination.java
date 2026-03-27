package gov.cms.bfd.server.ng.beneficiary.model;

/**
 * Represents the final determination of a patient match attempt.
 *
 * @param combination the combination index
 * @param matchType the type of match
 * @param matchedRecord the matched record
 */
public record FinalDetermination(
    String combination, String matchType, MatchedRecord matchedRecord) {}
