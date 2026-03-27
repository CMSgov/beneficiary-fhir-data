package gov.cms.bfd.server.ng.beneficiary.model;

import java.time.ZonedDateTime;

/**
 * Represents a single matched patient record.
 *
 * @param beneSk the beneficiary surrogate key
 * @param effectiveTimestamp the time assigned by IDR identifying when a bene record was loaded
 */
public record MatchedRecord(Long beneSk, ZonedDateTime effectiveTimestamp) {}
