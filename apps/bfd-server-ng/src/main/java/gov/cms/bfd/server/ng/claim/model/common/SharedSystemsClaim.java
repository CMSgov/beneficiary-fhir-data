package gov.cms.bfd.server.ng.claim.model.common;

import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Marked interface to remove instanceof chain in ClaimBase. */
public interface SharedSystemsClaim {

  /**
   * Hook method to return the claim paid status code if applicable to this claim source type.
   * Defaults to empty for base claims (like NCH/DDPS) that do not track this field.
   *
   * @return an optional containing the claim paid status code
   */
  Optional<ClaimPaidStatusCode> getClaimPaidStatusCode();

  /**
   * Shared Systems claims use CLM_PD_STUS_CD to determine outcome, no longer using audit-trail
   * logic. Standard base claims with no status code will ignore this, default implementation is to
   * return empty, and only Shared Systems wil override getClaimPaidStatusCode().
   *
   * @param eob the EOB being built
   */
  default void resolveSharedSystemsOutcome(ExplanationOfBenefit eob) {
    ClaimPaidStatusCode.resolveOutcome(getClaimPaidStatusCode()).ifPresent(eob::setOutcome);
  }
}
