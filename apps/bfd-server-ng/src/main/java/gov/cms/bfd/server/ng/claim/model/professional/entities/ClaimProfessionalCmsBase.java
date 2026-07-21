package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;

/** Shared base for CMS profile professional claim types (NCH and Shared Systems). */
@MappedSuperclass
public abstract class ClaimProfessionalCmsBase extends ClaimProfessionalBase {

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.ofNullable(claimIdrLoadDate);
  }
  // endregion
}
