package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.NchClaim;
import gov.cms.bfd.server.ng.claim.model.professional.AdjudicationProfessionalNch;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;

/** The professional claim, regular profile, sourced from nch. */
@Entity
@Table(name = "claim_professional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalRegularNch extends ClaimProfessionalRegularBase implements NchClaim {

  @Embedded private AdjudicationProfessionalNch adjudicationCharge;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalRegularNch> claimItems;

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  public Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
  }
}
