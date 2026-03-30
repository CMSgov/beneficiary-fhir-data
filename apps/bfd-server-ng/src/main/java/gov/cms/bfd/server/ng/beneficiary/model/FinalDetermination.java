package gov.cms.bfd.server.ng.beneficiary.model;

/**
 * Represents the final determination of a patient match attempt.
 *
 * @param combination the combination index
 * @param matchedRecord the matched record
 */
public record FinalDetermination(String combination, MatchedRecord matchedRecord) {}
