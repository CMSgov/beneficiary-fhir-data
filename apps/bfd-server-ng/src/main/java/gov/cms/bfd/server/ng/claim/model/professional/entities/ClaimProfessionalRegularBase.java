package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Shared base for regular profile professional claims. */
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimProfessionalRegularBase extends ClaimProfessionalBase {

  @Override
  List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  void addSubclassAdjudication(ExplanationOfBenefit eob) {}

  @Override
  void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {}

  @Override
  public SortedSet<ClaimItemBase> getItems() {
    return Collections.emptySortedSet();
  }

  // region PaymentComponent
  @Embedded private PaymentComponentAmount paymentComponent;

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion
}
