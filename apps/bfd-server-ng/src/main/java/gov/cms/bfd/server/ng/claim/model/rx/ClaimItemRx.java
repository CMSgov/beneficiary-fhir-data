package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

/** Claim item table. */
@Getter
@Embeddable
public class ClaimItemRx implements ClaimItemBase {
  @Embedded private ClaimLineRx claimLine;

  @Override
  public ClaimItemId getClaimItemId() {
    return new ClaimItemId();
  }
}
