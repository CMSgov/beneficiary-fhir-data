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

/** claim_item data for a professional claim, regular profile, sourced from nch. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "claim_item_professional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimItemProfessionalRegularNch implements ClaimItemBase {

  @EmbeddedId private ClaimItemId claimItemId;

  @Override
  public ClaimLineBase getClaimLine() {
    return null;
  }
}
