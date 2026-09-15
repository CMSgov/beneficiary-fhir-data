package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SharedSystemsClaim;
import gov.cms.bfd.server.ng.claim.model.professional.AdjudicationProfessionalSharedSystems;
import gov.cms.bfd.server.ng.converter.ClaimPaidStatusCodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Optional;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** The professional claim, regular profile, sourced from shared systems. */
@Getter
@Entity
@Table(name = "claim_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalRegularSharedSystems extends ClaimProfessionalRegularBase
    implements SharedSystemsClaim {

  @Embedded AdjudicationProfessionalSharedSystems adjudicationCharge;

  @Column(name = "clm_pd_stus_cd")
  @Convert(converter = ClaimPaidStatusCodeConverter.class)
  private ClaimPaidStatusCode claimPaidStatusCode;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @Override
  public Optional<ClaimPaidStatusCode> getClaimPaidStatusCode() {
    return Optional.of(claimPaidStatusCode);
  }

  @Override
  public Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
  }
}
