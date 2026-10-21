package gov.cms.bfd.server.ng.claim;

/** Profiles an ExplanationOfBenefit (only) can be rendered in. */
public enum EobProfileType {
  /** CMS BBAPI, fullest profile, default. */
  CMS,
  /** C4BB, some information redacted, retains CARIN fields. */
  REGULAR,
  /** C4BB, least information, all payment info removed. */
  BASIS
}
