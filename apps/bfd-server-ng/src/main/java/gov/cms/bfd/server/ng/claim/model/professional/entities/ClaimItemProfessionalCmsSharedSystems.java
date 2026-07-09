package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineRxNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimLineProfessionalSharedSystems;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimProcedureProfessional;
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
@Table(name = "claim_item_professional_ss", schema = "idr")
public class ClaimItemProfessionalCmsSharedSystems implements ClaimItemBase {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineProfessionalSharedSystems claimLine;
  @Embedded private ClaimProcedureProfessional claimProcedure;
  @Embedded private ClaimLineRxNumber claimLineRxNum;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimProfessionalCmsSharedSystems claim;

  @Override
  public Optional<ClaimProcedureBase> getProcedure() {
    return Optional.of(claimProcedure);
  }

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.of(claimLine.getHcpcsCode());
  }
}
