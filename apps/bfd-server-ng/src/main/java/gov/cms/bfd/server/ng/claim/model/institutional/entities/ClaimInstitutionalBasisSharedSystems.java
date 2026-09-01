package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** the institutional claim, basis profile, from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalBasisSharedSystems extends ClaimInstitutionalBasisBase {

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemBasisSharedSystems> claimItems;

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return new TreeSet<ClaimItemBase>(getClaimItems());
  }

  @Override
  public List<ClaimValue> getClaimValues() {
    return getClaimItems().stream().map(ClaimItemBasisSharedSystems::getClaimValue).toList();
  }

  @Override
  SupportingInfoComponentBase getSupportingInfo() {
    return null;
  }

  @Override
  AdjudicationChargeBase getAdjudicationCharge() {
    return null;
  }

  @Override
  List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return List.of();
  }

  @Override
  void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {}

  @Override
  public ClaimSourceId getClaimSourceId() {
    return null;
  }

  @Override
  public MetaSourceSk getMetaSourceSk() {
    return null;
  }

  @Override
  public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
    return Optional.empty();
  }
}
