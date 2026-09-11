package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ProcedureBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimLineInstitutionalCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.claim.model.institutional.ProcedureInstitutional;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item_institutional_ss", schema = "idr")
public class ClaimItemInstitutionalCmsSharedSystems implements ClaimItemBase {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineInstitutionalCmsSharedSystems claimLine;
  @Embedded private ProcedureInstitutional claimProcedure;
  @Embedded private ClaimValue claimValue;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimInstitutionalCmsSharedSystems claim;

  @Override
  public Optional<ProcedureBase> getProcedureOptional() {
    return Optional.of(claimProcedure);
  }
}
