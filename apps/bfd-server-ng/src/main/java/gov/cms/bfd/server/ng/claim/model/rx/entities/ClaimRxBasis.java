package gov.cms.bfd.server.ng.claim.model.rx.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.rx.ClaimItemRx;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** The basis profile of a Rx-Basis. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimRxBasis extends ClaimRxBase {

  @Embedded private ClaimItemRx claimItem;

  /**
   * Returns the system type.
   *
   * @return system type
   */
  public static SystemType getSystemType() {
    return SystemType.DDPS;
  }

  @Override
  protected ClaimItemBase getClaimItem() {
    return claimItem;
  }
}
