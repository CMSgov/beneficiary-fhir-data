package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** the institutional claim basis profile base. */
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimInstitutionalBasisBase extends ClaimInstitutionalBase {
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

  // region PaymentComponent

  // Basis does not have a payment amount, just the date
  @Embedded private ClaimPaymentComponent paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion
}
