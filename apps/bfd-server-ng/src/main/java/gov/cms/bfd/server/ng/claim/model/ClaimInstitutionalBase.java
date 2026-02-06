package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/** Institutional claims table. */
@Getter
@MappedSuperclass
public abstract class ClaimInstitutionalBase {
  @Embedded private ClaimInstitutionalSupportingInfo supportingInfo;
  @Embedded private AdjudicationChargeInstitutional adjudicationChargeInstitutional;
}
