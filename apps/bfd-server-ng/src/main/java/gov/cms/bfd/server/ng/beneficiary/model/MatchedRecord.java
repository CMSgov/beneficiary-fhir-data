package gov.cms.bfd.server.ng.beneficiary.model;

import java.time.ZonedDateTime;

public record MatchedRecord(Long beneSk, ZonedDateTime effectiveTimestamp) {}
