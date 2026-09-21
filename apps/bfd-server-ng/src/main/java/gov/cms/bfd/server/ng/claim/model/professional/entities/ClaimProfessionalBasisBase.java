package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Shared base for regular profile professional claims. */
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimProfessionalBasisBase extends ClaimProfessionalBase {

  // Basis does not contain an amount field
  @Embedded private PaymentComponent paymentComponent;

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  @Override
  List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  void addSubclassAdjudication(ExplanationOfBenefit eob) {}

  @Override
  void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {}

  // Basis does not provide adjudications.
  @Override
  Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.empty();
  }
}
