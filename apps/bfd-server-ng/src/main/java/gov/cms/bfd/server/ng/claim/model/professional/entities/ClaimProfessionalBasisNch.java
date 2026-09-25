package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.NchClaim;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimProfessionalNchCore;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Leaf entity, Professional-Basis-NCH. */
@Getter
@Entity
@Table(name = "claim_professional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalBasisNch extends ClaimProfessionalBasisBase implements NchClaim {

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalBasisNch> claimItems;

  @Embedded private ClaimProfessionalNchCore nchCore;

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    return Stream.concat(
            super.getSubclassSupportingInfo().stream(),
            nchCore.getClaimQueryCode().map(c -> c.toFhir(supportingInfoFactory)).stream())
        .toList();
  }

  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    nchCore.addServiceCareTeam(eob, sequenceGenerator, getClaimTypeCode());
  }

  @Override
  public Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(nchCore.getClaimRecordType());
  }

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  public MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.NCH;
  }
}
