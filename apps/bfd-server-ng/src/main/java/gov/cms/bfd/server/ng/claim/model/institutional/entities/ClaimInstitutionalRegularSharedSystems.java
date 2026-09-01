package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationChargeRegularSharedSystems;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** The institutional claim, regular profile, sourced from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalRegularSharedSystems extends ClaimInstitutionalRegularBase {

  @Embedded private AdjudicationChargeRegularSharedSystems adjudicationCharge;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemRegularSharedSystems> claimItems;
}
