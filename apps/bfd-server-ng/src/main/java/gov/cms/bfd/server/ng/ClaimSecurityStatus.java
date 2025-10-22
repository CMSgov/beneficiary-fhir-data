package gov.cms.bfd.server.ng;

/** Represents Claim Security status. */
public enum ClaimSecurityStatus {

  /** No special security tags are required for this claim. */
  NONE,

  /** This claim requires SAMHSA tags. */
  SAMHSA_APPLICABLE,
}
