package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import javax.annotation.processing.Generated;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** LineItem for a professional claim, basis profile, sourced from nch. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "claim_item_professional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimItemProfessionalBasisNch implements ClaimItemBase {

  @EmbeddedId private ClaimItemId claimItemId;

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.empty();
  }

  @Override
  public ClaimLineBase getClaimLine() {
    return null;
  }
}
