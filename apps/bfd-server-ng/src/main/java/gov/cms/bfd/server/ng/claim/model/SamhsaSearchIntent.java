package gov.cms.bfd.server.ng.claim.model;

/** SAMHSA search intent. */
public enum SamhsaSearchIntent {
  /** Requested only SAMHSA. */
  ONLY_SAMHSA,
  /** Requested to exclude SAMHSA. */
  EXCLUDE_SAMHSA,
  /** No security parameter specified. */
  UNSPECIFIED
}
