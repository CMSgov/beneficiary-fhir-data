package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRelatedCondition;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.MetaSourceSk;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Shared base for regular profile professional claims. */
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimProfessionalRegularBase extends ClaimProfessionalBase {
  @Override
  AdjudicationChargeBase getAdjudicationCharge() {
    return null;
  }

  @Override
  List<ExplanationOfBenefit.SupportingInformationComponent> buildSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  void addSubclassAdjudication(ExplanationOfBenefit eob) {}

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
  @Embedded private ClaimPaymentComponentAmount paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion
}
