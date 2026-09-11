package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import javax.annotation.processing.Generated;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** LineItem for a professional claim, regular profile, sourced from shared systems. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "claim_item_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimItemProfessionalRegularSharedSystems implements ClaimItemBase {

  @EmbeddedId private ClaimItemId claimItemId;

  @Override
  public ClaimLineBase getClaimLine() {
    return null;
  }
}
