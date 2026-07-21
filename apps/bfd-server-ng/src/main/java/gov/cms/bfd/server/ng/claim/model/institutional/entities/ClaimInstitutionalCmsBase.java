package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import javax.annotation.processing.Generated;

/** The institutional claim, full (CMS) profile base class. */
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimInstitutionalCmsBase extends ClaimInstitutionalBase {

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.ofNullable(claimIdrLoadDate);
  }
  // endregion
}
