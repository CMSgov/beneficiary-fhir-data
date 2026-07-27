package gov.cms.bfd.server.ng.claim.model.rx.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** The basis profile of a Rx Claim. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimRxBasis extends ClaimRxBase {
  @Override
  protected ClaimItemBase getClaimItem() {
    return null;
  }
}
