package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import jakarta.persistence.Embedded;
import java.util.Optional;
import javax.annotation.processing.Generated;

/** Shared base for CMS profile professional claim types (NCH and Shared Systems). */
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimProfessionalCmsBase extends ClaimProfessionalBase {

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.ofNullable(claimIdrLoadDate);
  }
  // endregion
}
