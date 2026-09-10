package gov.cms.bfd.server.ng.claim.model.rx.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** The basis profile of a Rx Claim. */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimRxBasis extends ClaimRxBase {

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
    return null;
  }
}
