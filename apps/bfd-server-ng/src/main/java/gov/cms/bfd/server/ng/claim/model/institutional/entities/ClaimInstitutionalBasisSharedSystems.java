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
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** the institutional claim, basis profile, from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalBasisSharedSystems extends ClaimInstitutionalBasisBase {
  @Override
  SupportingInfoComponentBase getClaimDateSupportingInfo() {
    return null;
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
  List<ClaimValue> getClaimValues() {
    return List.of();
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
  public SortedSet<ClaimItemBase> getItems() {
    return null;
  }

  @Override
  public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
    return Optional.empty();
  }
}
