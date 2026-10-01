package gov.cms.bfd.server.ng.input;

/** enum that represents the exposed profile definitions for an ExplanationOfBenefit. */
public enum ClaimProfile {
  /** Fullest profile, default. */
  CMS,
  /** Some information redacted, retains CARIN fields. */
  REGULAR,
  /** Least information, all adjudication removed. */
  BASIS
}
