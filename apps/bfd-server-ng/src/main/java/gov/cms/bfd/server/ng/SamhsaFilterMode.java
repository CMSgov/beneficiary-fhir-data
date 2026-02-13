package gov.cms.bfd.server.ng;

/** Whether to include SAMHSA data. */
public enum SamhsaFilterMode {
  /** Include SAMHSA. */
  INCLUDE,
  /** Exclude SAMHSA. */
  EXCLUDE,
  /** Only SAMHSA. */
  ONLY_SAMHSA,
  /** Exclude all claims. */
  RETURN_NONE
}
