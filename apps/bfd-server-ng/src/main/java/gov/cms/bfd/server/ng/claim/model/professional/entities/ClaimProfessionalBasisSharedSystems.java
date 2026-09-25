package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SharedSystemsClaim;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimProfessionalSharedSystemsCore;
import gov.cms.bfd.server.ng.converter.ClaimPaidStatusCodeConverter;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The professional claim, basis profile, sourced from shared system. */
@Getter
@Entity
@Table(name = "claim_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalBasisSharedSystems extends ClaimProfessionalBasisBase
    implements SharedSystemsClaim {

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @Column(name = "clm_pd_stus_cd")
  @Convert(converter = ClaimPaidStatusCodeConverter.class)
  private ClaimPaidStatusCode claimPaidStatusCode;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalBasisSharedSystems> claimItems;

  @Embedded private ClaimProfessionalSharedSystemsCore sharedSystemsCore;

  @Override
  public Optional<ClaimPaidStatusCode> getClaimPaidStatusCode() {
    return Optional.of(claimPaidStatusCode);
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  /**
   * SS also adds the {@code otherProviderHistory} care-team member alongside the referring provider
   * that the base class handles.
   */
  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    sharedSystemsCore.addOtherCareTeam(eob, sequenceGenerator, getClaimTypeCode());
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return claimSourceId;
  }

  @Override
  public MetaSourceSk getMetaSourceSk() {
    return metaSourceSk;
  }
}
