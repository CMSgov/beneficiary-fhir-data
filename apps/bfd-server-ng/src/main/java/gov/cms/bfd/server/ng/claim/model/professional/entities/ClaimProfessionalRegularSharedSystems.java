package gov.cms.bfd.server.ng.claim.model.professional.entities;

import static gov.cms.bfd.server.ng.claim.model.common.ClaimSubtype.PDE;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionFormatCode;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SharedSystemsClaim;
import gov.cms.bfd.server.ng.claim.model.professional.AdjudicationProfessionalSharedSystems;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The professional claim, regular profile, sourced from shared systems. */
@Getter
@Entity
@Table(name = "claim_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalRegularSharedSystems extends ClaimProfessionalRegularBase
    implements SharedSystemsClaim {

  @Column(name = "clm_pd_stus_cd")
  @Convert(converter = ClaimPaidStatusCodeConverter.class)
  private ClaimPaidStatusCode claimPaidStatusCode;

  @Column(name = "clm_src_id")
  private ClaimSourceId claimSourceId;

  @Column(name = "meta_src_sk")
  private MetaSourceSk metaSourceSk;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalRegularSharedSystems> claimItems;

  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<ClaimSubmissionFormatCode> claimFormatCode;

  @Embedded AdjudicationProfessionalSharedSystems adjudicationCharge;
  @Embedded ClaimProfessionalSharedSystemsCore sharedSystemsCore;

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    var result = new ArrayList<>(super.getSubclassSupportingInfo());
    claimFormatCode
        .filter(_ -> getClaimTypeCode().isClaimSubtype(PDE))
        .map(c -> c.toFhir(supportingInfoFactory))
        .ifPresent(result::add);
    return result;
  }

  @Override
  public Optional<ClaimPaidStatusCode> getClaimPaidStatusCode() {
    return Optional.of(claimPaidStatusCode);
  }

  @Override
  public Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
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
