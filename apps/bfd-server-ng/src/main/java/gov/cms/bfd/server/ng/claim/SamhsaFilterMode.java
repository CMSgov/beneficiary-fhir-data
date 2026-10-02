package gov.cms.bfd.server.ng.claim;

/** Whether to include SAMHSA data. */
public enum SamhsaFilterMode {
  /** Include SAMHSA. */
  INCLUDE,
  /** Exclude SAMHSA. */
  EXCLUDE,
  /** Only SAMHSA. */
  ONLY_SAMHSA
}
