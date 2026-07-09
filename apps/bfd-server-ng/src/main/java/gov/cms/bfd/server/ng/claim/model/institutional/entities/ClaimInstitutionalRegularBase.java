package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional claim, regular profile base class. */
public abstract class ClaimInstitutionalRegularBase extends ClaimInstitutionalBase {
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
  List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.empty();
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildRecordTypeSupportingInfo() {
    return List.of();
  }

  @Override
  public List<ClaimValue> getClaimValues() {
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
    return Collections.emptySortedSet();
  }

  @Override
  public Optional<ClaimRelatedCondition> getClaimRelatedCondition() {
    return Optional.empty();
  }
}
