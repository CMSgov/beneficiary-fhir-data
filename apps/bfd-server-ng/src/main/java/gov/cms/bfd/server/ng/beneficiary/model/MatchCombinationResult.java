package gov.cms.bfd.server.ng.beneficiary.model;

import java.util.List;

public record MatchCombinationResult(
    String combination, String matchType, List<MatchedRecord> matchedRecords) {}
