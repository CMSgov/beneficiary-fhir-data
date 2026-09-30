package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ProcedureBase;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimLineProfessionalBasisSharedSystems;
import gov.cms.bfd.server.ng.claim.model.professional.ProcedureProfessional;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import javax.annotation.processing.Generated;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** claim_item data for a professional claim, basis profile, sourced from shared systems. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "claim_item_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimItemProfessionalBasisSharedSystems implements ClaimItemBase {

  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineProfessionalBasisSharedSystems claimLine;
  @Embedded private ProcedureProfessional claimProcedure;

  @Override
  public Optional<ProcedureBase> getProcedureOptional() {
    return Optional.of(claimProcedure);
  }
}
