package gov.cms.bfd.server.ng.beneficiary.model;

public record FinalDetermination(
    String combination, String matchType, MatchedRecord matchedRecord) {}
